package ru.pep.platform.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByStudent(AppUser student);

    List<Submission> findByStudentAndModuleId(AppUser student, UUID moduleId);
}
