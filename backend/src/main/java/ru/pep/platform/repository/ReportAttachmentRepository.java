package ru.pep.platform.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.Report;
import ru.pep.platform.domain.ReportAttachment;

public interface ReportAttachmentRepository extends JpaRepository<ReportAttachment, UUID> {

    List<ReportAttachment> findByReportOrderByUploadedAtAsc(Report report);
}
