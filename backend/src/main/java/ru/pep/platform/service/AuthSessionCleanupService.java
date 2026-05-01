package ru.pep.platform.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pep.platform.repository.AuthLoginThrottleRepository;
import ru.pep.platform.repository.AuthSessionRepository;

@Service
public class AuthSessionCleanupService {

    private final AuthSessionRepository sessions;
    private final AuthLoginThrottleRepository throttles;
    private final Duration expiredRetention;
    private final Duration revokedRetention;
    private final Duration throttleRetention;

    public AuthSessionCleanupService(
            AuthSessionRepository sessions,
            AuthLoginThrottleRepository throttles,
            @Value("${pep.auth.cleanup.expired-retention-hours:24}") long expiredRetentionHours,
            @Value("${pep.auth.cleanup.revoked-retention-hours:24}") long revokedRetentionHours,
            @Value("${pep.auth.cleanup.throttle-retention-hours:24}") long throttleRetentionHours) {
        this.sessions = sessions;
        this.throttles = throttles;
        this.expiredRetention = Duration.ofHours(expiredRetentionHours);
        this.revokedRetention = Duration.ofHours(revokedRetentionHours);
        this.throttleRetention = Duration.ofHours(throttleRetentionHours);
    }

    @Scheduled(
            fixedDelayString = "${pep.auth.cleanup.interval-ms:3600000}",
            initialDelayString = "${pep.auth.cleanup.initial-delay-ms:60000}")
    @Transactional
    public int cleanupInactiveSessions() {
        OffsetDateTime now = OffsetDateTime.now();
        return sessions.deleteInactiveBefore(now.minus(expiredRetention), now.minus(revokedRetention));
    }

    @Scheduled(
            fixedDelayString = "${pep.auth.cleanup.interval-ms:3600000}",
            initialDelayString = "${pep.auth.cleanup.initial-delay-ms:60000}")
    @Transactional
    public int cleanupInactiveLoginThrottles() {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(throttleRetention);
        return throttles.deleteInactiveBefore(cutoff, cutoff);
    }
}
