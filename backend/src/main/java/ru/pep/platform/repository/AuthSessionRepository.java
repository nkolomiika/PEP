package ru.pep.platform.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.pep.platform.domain.AuthSession;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    Optional<AuthSession> findByTokenHashAndExpiresAtAfterAndRevokedAtIsNull(String tokenHash, OffsetDateTime now);

    @Query("""
            select count(distinct session.user.id)
            from AuthSession session
            where session.expiresAt > :now
              and session.revokedAt is null
            """)
    long countActiveUsers(OffsetDateTime now);

    @Query("""
            select count(session.id)
            from AuthSession session
            where session.expiresAt > :now
              and session.revokedAt is null
            """)
    long countActiveSessions(OffsetDateTime now);

    @Modifying
    @Query("""
            delete from AuthSession session
            where session.expiresAt < :expiredBefore
               or (session.revokedAt is not null and session.revokedAt < :revokedBefore)
            """)
    int deleteInactiveBefore(OffsetDateTime expiredBefore, OffsetDateTime revokedBefore);
}
