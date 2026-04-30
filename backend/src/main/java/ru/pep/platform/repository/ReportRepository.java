package ru.pep.platform.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.Report;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByAuthor(AppUser author);
}
