package ru.pep.platform.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.StudentStream;
import ru.pep.platform.domain.StudentStreamMembership;
import ru.pep.platform.domain.StudentStreamMembershipStatus;

public interface StudentStreamMembershipRepository
        extends JpaRepository<StudentStreamMembership, UUID> {

    Optional<StudentStreamMembership> findByStreamAndUser(StudentStream stream, AppUser user);

    List<StudentStreamMembership> findByStream(StudentStream stream);

    List<StudentStreamMembership> findByStreamAndStatus(
            StudentStream stream, StudentStreamMembershipStatus status);

    List<StudentStreamMembership> findByUserAndStatus(
            AppUser user, StudentStreamMembershipStatus status);

    @Query("select m.stream.id from StudentStreamMembership m "
            + "where m.user.id = :userId and m.status = :status and m.stream.status = "
            + "ru.pep.platform.domain.StudentStreamStatus.ACTIVE")
    List<UUID> findActiveStreamIdsForUser(
            @Param("userId") UUID userId,
            @Param("status") StudentStreamMembershipStatus status);

    @Query("select distinct m.user.id from StudentStreamMembership m "
            + "where m.stream.id in :streamIds and m.status = :status")
    List<UUID> findMemberIdsForStreams(
            @Param("streamIds") Collection<UUID> streamIds,
            @Param("status") StudentStreamMembershipStatus status);

    long countByStream(StudentStream stream);
}
