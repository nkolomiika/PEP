package ru.pep.platform.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.BlackBoxAssignment;
import ru.pep.platform.domain.LearningModule;
import ru.pep.platform.domain.LabInstance;

public interface BlackBoxAssignmentRepository extends JpaRepository<BlackBoxAssignment, UUID> {

    List<BlackBoxAssignment> findByStudent(AppUser student);

    boolean existsByModuleAndStudentAndTargetLabInstance(LearningModule module, AppUser student, LabInstance targetLabInstance);
}
