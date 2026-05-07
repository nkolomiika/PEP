package ru.pep.platform.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.LearningModule;
import ru.pep.platform.domain.StudentStream;
import ru.pep.platform.domain.StudentStreamModuleSchedule;

public interface StudentStreamModuleScheduleRepository
        extends JpaRepository<StudentStreamModuleSchedule, UUID> {

    Optional<StudentStreamModuleSchedule> findByStreamAndModule(StudentStream stream, LearningModule module);

    List<StudentStreamModuleSchedule> findByStream(StudentStream stream);

    List<StudentStreamModuleSchedule> findByStreamIdInAndModuleId(
            Collection<UUID> streamIds, UUID moduleId);
}
