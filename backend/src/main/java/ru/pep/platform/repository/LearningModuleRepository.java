package ru.pep.platform.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.LearningModule;

public interface LearningModuleRepository extends JpaRepository<LearningModule, UUID> {
}
