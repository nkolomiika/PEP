package ru.pep.platform.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.Lesson;
import ru.pep.platform.domain.LessonProgress;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    Optional<LessonProgress> findByStudentAndLesson(AppUser student, Lesson lesson);

    @Query("""
            select progress
            from LessonProgress progress
            join fetch progress.lesson lesson
            where progress.student.id = :studentId
              and lesson.module.id = :moduleId
            order by lesson.position asc
            """)
    List<LessonProgress> findModuleProgress(
            @Param("studentId") UUID studentId,
            @Param("moduleId") UUID moduleId);
}
