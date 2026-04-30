package ru.pep.platform.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.Submission;
import ru.pep.platform.domain.ValidationJob;

public interface ValidationJobRepository extends JpaRepository<ValidationJob, UUID> {

    List<ValidationJob> findBySubmission(Submission submission);
}
