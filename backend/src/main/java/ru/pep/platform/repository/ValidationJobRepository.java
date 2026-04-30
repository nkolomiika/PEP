package ru.pep.platform.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.Submission;
import ru.pep.platform.domain.ValidationJob;
import ru.pep.platform.domain.ValidationJobStatus;

public interface ValidationJobRepository extends JpaRepository<ValidationJob, UUID> {

    List<ValidationJob> findBySubmission(Submission submission);

    Optional<ValidationJob> findFirstByStatusOrderByCreatedAtAsc(ValidationJobStatus status);
}
