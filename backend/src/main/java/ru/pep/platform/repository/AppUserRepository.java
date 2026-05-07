package ru.pep.platform.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.pep.platform.domain.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select u from AppUser u
            where lower(u.email) like lower(concat('%', :query, '%'))
               or lower(u.displayName) like lower(concat('%', :query, '%'))
            order by u.createdAt desc
            """)
    Page<AppUser> search(@Param("query") String query, Pageable pageable);
}
