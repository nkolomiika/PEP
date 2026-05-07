package ru.pep.platform.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.pep.platform.domain.Course;
import ru.pep.platform.domain.StudentStream;
import ru.pep.platform.domain.StudentStreamCourse;

public interface StudentStreamCourseRepository extends JpaRepository<StudentStreamCourse, UUID> {

    List<StudentStreamCourse> findByStreamOrderByPositionAscCreatedAtAsc(StudentStream stream);

    Optional<StudentStreamCourse> findByStreamAndCourse(StudentStream stream, Course course);

    void deleteByStreamAndCourse(StudentStream stream, Course course);

    @Query("select distinct sc.course.id from StudentStreamCourse sc where sc.stream.id in :streamIds")
    List<UUID> findCourseIdsForStreams(@Param("streamIds") Collection<UUID> streamIds);

    @Query("select distinct sc.stream.id from StudentStreamCourse sc where sc.course.id = :courseId")
    List<UUID> findStreamIdsForCourse(@Param("courseId") UUID courseId);
}
