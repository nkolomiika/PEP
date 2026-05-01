package ru.pep.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_login_throttle")
public class AuthLoginThrottle {

    @Id
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 128)
    private String remoteAddress;

    @Column(nullable = false)
    private int failedCount;

    @Column(nullable = false)
    private OffsetDateTime firstFailedAt;

    private OffsetDateTime lockedUntil;

    protected AuthLoginThrottle() {
    }

    public AuthLoginThrottle(String email, String remoteAddress, OffsetDateTime now) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.remoteAddress = remoteAddress;
        this.failedCount = 0;
        this.firstFailedAt = now;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public OffsetDateTime getFirstFailedAt() {
        return firstFailedAt;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public boolean isLocked(OffsetDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void resetWindow(OffsetDateTime now) {
        this.failedCount = 1;
        this.firstFailedAt = now;
        this.lockedUntil = null;
    }

    public void registerFailure(OffsetDateTime now) {
        this.failedCount++;
        this.lockedUntil = null;
    }

    public void lockUntil(OffsetDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
}
