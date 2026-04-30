package ru.pep.platform.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findByModuleIdAndPublishedTrueOrderByPositionAsc(UUID moduleId);

    boolean existsByModuleId(UUID moduleId);
}
