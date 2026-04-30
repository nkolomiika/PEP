package ru.pep.platform.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.LabInstance;
import ru.pep.platform.domain.Submission;

public interface LabInstanceRepository extends JpaRepository<LabInstance, UUID> {

    Optional<LabInstance> findBySubmission(Submission submission);

    List<LabInstance> findBySubmissionModuleId(UUID moduleId);
}
