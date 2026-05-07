package ru.pep.platform.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.pep.platform.domain.StudentStream;
import ru.pep.platform.domain.StudentStreamStatus;

public interface StudentStreamRepository extends JpaRepository<StudentStream, UUID> {

    Optional<StudentStream> findByName(String name);

    List<StudentStream> findAllByOrderByCreatedAtDesc();

    List<StudentStream> findByStatusOrderByCreatedAtDesc(StudentStreamStatus status);

    @Query("""
            select s from StudentStream s
            where lower(s.name) like lower(concat('%', :query, '%'))
            order by s.createdAt desc
            """)
    Page<StudentStream> search(@Param("query") String query, Pageable pageable);
}
