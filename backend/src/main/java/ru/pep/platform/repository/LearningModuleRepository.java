package ru.pep.platform.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.LearningModule;

public interface LearningModuleRepository extends JpaRepository<LearningModule, UUID> {

    Optional<LearningModule> findByCourseIdAndTitle(UUID courseId, String title);

    List<LearningModule> findByCourseId(UUID courseId);
}
