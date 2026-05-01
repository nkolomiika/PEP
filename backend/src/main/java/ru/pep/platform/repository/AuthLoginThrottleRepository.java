package ru.pep.platform.repository;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.pep.platform.domain.AuthLoginThrottle;

public interface AuthLoginThrottleRepository extends JpaRepository<AuthLoginThrottle, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuthLoginThrottle> findByEmailAndRemoteAddress(String email, String remoteAddress);

    void deleteByEmailAndRemoteAddress(String email, String remoteAddress);

    @Modifying
    @Query("""
            delete from AuthLoginThrottle throttle
            where throttle.firstFailedAt < :failedBefore
              and (throttle.lockedUntil is null or throttle.lockedUntil < :lockedBefore)
            """)
    int deleteInactiveBefore(OffsetDateTime failedBefore, OffsetDateTime lockedBefore);
}
