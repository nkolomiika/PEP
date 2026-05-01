package ru.pep.platform.service;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.AuthLoginThrottle;
import ru.pep.platform.domain.AuthSession;
import ru.pep.platform.domain.UserStatus;
import ru.pep.platform.repository.AuthLoginThrottleRepository;
import ru.pep.platform.repository.AppUserRepository;
import ru.pep.platform.repository.AuthSessionRepository;

@Service
public class AuthSessionService {

    public static final String SESSION_COOKIE = "PEP_SESSION";

    private final AppUserRepository users;
    private final AuthSessionRepository sessions;
    private final AuthLoginThrottleRepository throttles;
    private final AuditService audit;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration ttl;
    private final boolean secureCookie;
    private final int maxFailedAttempts;
    private final Duration failedWindow;
    private final Duration lockDuration;

    public AuthSessionService(
            AppUserRepository users,
            AuthSessionRepository sessions,
            AuthLoginThrottleRepository throttles,
            AuditService audit,
            PasswordEncoder passwordEncoder,
            @Value("${pep.auth.session-ttl-hours:8}") long ttlHours,
            @Value("${pep.auth.cookie-secure:true}") boolean secureCookie,
            @Value("${pep.auth.login.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${pep.auth.login.failed-window-minutes:15}") long failedWindowMinutes,
            @Value("${pep.auth.login.lock-minutes:15}") long lockMinutes) {
        this.users = users;
        this.sessions = sessions;
        this.throttles = throttles;
        this.audit = audit;
        this.passwordEncoder = passwordEncoder;
        this.ttl = Duration.ofHours(ttlHours);
        this.secureCookie = secureCookie;
        this.maxFailedAttempts = maxFailedAttempts;
        this.failedWindow = Duration.ofMinutes(failedWindowMinutes);
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    @Transactional(noRollbackFor = {
            CorePlatformService.AccessDeniedException.class,
            TooManyLoginAttemptsException.class
    })
    public LoginResult login(String email, String password, String remoteAddress) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedRemoteAddress = normalizeRemoteAddress(remoteAddress);
        OffsetDateTime now = OffsetDateTime.now();
        assertLoginAllowed(normalizedEmail, normalizedRemoteAddress, now);

        AppUser user = users.findByEmail(normalizedEmail)
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElse(null);
        if (user == null) {
            registerFailedLogin(normalizedEmail, normalizedRemoteAddress, now);
            throw new CorePlatformService.AccessDeniedException("Неверный email или пароль");
        }
        throttles.deleteByEmailAndRemoteAddress(normalizedEmail, normalizedRemoteAddress);
        String rawToken = generateToken();
        AuthSession session = sessions.save(new AuthSession(user, hashToken(rawToken), OffsetDateTime.now().plus(ttl)));
        audit.record(user, "AUTH_LOGIN_SUCCESS", "AuthSession", session.getId(),
                "{\"remoteAddress\":\"" + jsonEscape(normalizedRemoteAddress) + "\"}");
        return new LoginResult(user, createSessionCookie(rawToken, ttl));
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> authenticate(String rawToken) {
        return sessions.findByTokenHashAndExpiresAtAfterAndRevokedAtIsNull(hashToken(rawToken), OffsetDateTime.now())
                .filter(session -> session.isActive(OffsetDateTime.now()))
                .map(session -> {
                    AppUser user = session.getUser();
                    user.getEmail();
                    user.getRole();
                    return user;
                });
    }

    @Transactional
    public void logout(String rawToken) {
        sessions.findByTokenHashAndExpiresAtAfterAndRevokedAtIsNull(hashToken(rawToken), OffsetDateTime.now())
                .ifPresent(session -> {
                    AppUser user = session.getUser();
                    user.getEmail();
                    session.revoke();
                    audit.record(user, "AUTH_LOGOUT", "AuthSession", session.getId(), "{}");
                });
    }

    public ResponseCookie clearSessionCookie() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie createSessionCookie(String token, Duration maxAge) {
        return baseCookie(token)
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(SESSION_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/");
    }

    private String generateToken() {
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    public String tokenFrom(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void assertLoginAllowed(String email, String remoteAddress, OffsetDateTime now) {
        throttles.findByEmailAndRemoteAddress(email, remoteAddress)
                .filter(throttle -> throttle.isLocked(now))
                .ifPresent(throttle -> {
                    audit.record(null, "AUTH_LOGIN_RATE_LIMITED", "Auth", null, authMetadata(email, remoteAddress, true));
                    throw new TooManyLoginAttemptsException("Слишком много попыток входа. Повторите позже.");
                });
    }

    private void registerFailedLogin(String email, String remoteAddress, OffsetDateTime now) {
        AuthLoginThrottle throttle = throttles.findByEmailAndRemoteAddress(email, remoteAddress)
                .orElseGet(() -> new AuthLoginThrottle(email, remoteAddress, now));
        if (throttle.getFirstFailedAt().plus(failedWindow).isBefore(now)) {
            throttle.resetWindow(now);
        } else if (throttle.getFailedCount() + 1 >= maxFailedAttempts) {
            throttle.registerFailure(now);
            throttle.lockUntil(now.plus(lockDuration));
        } else {
            throttle.registerFailure(now);
        }
        throttles.save(throttle);
        audit.record(null, "AUTH_LOGIN_FAILED", "Auth", null, authMetadata(email, remoteAddress, throttle.isLocked(now)));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRemoteAddress(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "unknown";
        }
        return remoteAddress.trim();
    }

    private String authMetadata(String email, String remoteAddress, boolean locked) {
        return "{\"emailHash\":\"" + hashText(email)
                + "\",\"remoteAddress\":\"" + jsonEscape(remoteAddress)
                + "\",\"locked\":" + locked + "}";
    }

    private String hashText(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record LoginResult(AppUser user, ResponseCookie cookie) {
    }

    public static class TooManyLoginAttemptsException extends RuntimeException {
        public TooManyLoginAttemptsException(String message) {
            super(message);
        }
    }
}
