package ru.pep.platform.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import ru.pep.platform.domain.CourseStatus;
import ru.pep.platform.domain.BlackBoxAssignmentStatus;
import ru.pep.platform.domain.DependencyScanStatus;
import ru.pep.platform.domain.ImageScanStatus;
import ru.pep.platform.domain.LabStatus;
import ru.pep.platform.domain.ModuleStatus;
import ru.pep.platform.domain.ReportStatus;
import ru.pep.platform.domain.ReportType;
import ru.pep.platform.domain.ReviewDecision;
import ru.pep.platform.domain.SubmissionStatus;
import ru.pep.platform.domain.ValidationJobStatus;

public final class CoreDtos {

    private CoreDtos() {
    }

    public record CourseResponse(
            UUID id,
            String title,
            String description,
            CourseStatus status,
            List<ModuleResponse> modules) {
    }

    public record ModuleResponse(UUID id, String title, String vulnerabilityTopic, ModuleStatus status) {
    }

    public record LessonSummaryResponse(UUID id, UUID moduleId, String title, Integer position) {
    }

    public record LessonResponse(UUID id, UUID moduleId, String title, String contentMarkdown, Integer position) {
    }

    public record LessonProgressResponse(UUID lessonId, boolean completed, OffsetDateTime completedAt) {
    }

    public record CreateSubmissionRequest(
            @NotNull UUID moduleId,
            @NotBlank String imageReference,
            @NotNull @Min(1) @Max(65535) Integer applicationPort,
            String healthPath) {
    }

    public record SubmissionResponse(
            UUID id,
            UUID moduleId,
            String studentEmail,
            String imageReference,
            Integer applicationPort,
            String healthPath,
            SubmissionStatus status) {
    }

    public record ValidationJobResponse(
            UUID id,
            UUID submissionId,
            String imageReference,
            ValidationJobStatus status,
            String logsUri,
            String errorMessage,
            ImageScanStatus imageScanStatus,
            String imageScanSummary,
            String imageScanReport,
            DependencyScanStatus dependencyScanStatus,
            String dependencyScanSummary,
            String dependencyScanReport) {
    }

    public record CompleteValidationRequest(boolean passed, String logsUri, String errorMessage) {
    }

    public record CreateReportRequest(
            @NotNull UUID moduleId,
            UUID submissionId,
            UUID blackBoxAssignmentId,
            @NotNull ReportType type,
            @NotBlank String title,
            @NotBlank String contentMarkdown) {
    }

    public record ReportResponse(
            UUID id,
            String authorEmail,
            UUID moduleId,
            UUID submissionId,
            UUID blackBoxAssignmentId,
            ReportType type,
            String title,
            String contentMarkdown,
            ReportStatus status,
            List<ReportAttachmentResponse> attachments) {
    }

    public record ReportAttachmentResponse(
            UUID id,
            String originalFilename,
            String contentType,
            Long sizeBytes,
            OffsetDateTime uploadedAt) {
    }

    public record CreateReviewRequest(
            @NotNull UUID reportId,
            @NotNull ReviewDecision decision,
            @NotNull @Min(0) @Max(100) Integer score,
            @NotBlank String commentMarkdown) {
    }

    public record ReviewResponse(
            UUID id,
            UUID reportId,
            String curatorEmail,
            ReviewDecision decision,
            Integer score,
            String commentMarkdown) {
    }

    public record ModuleResultResponse(
            UUID moduleId,
            boolean dockerPassed,
            Integer whiteBoxScore,
            Integer blackBoxScore,
            Integer finalScore,
            String status) {
    }

    public record AuditEventResponse(
            UUID id,
            String actorEmail,
            String action,
            String targetType,
            UUID targetId,
            String metadataJson,
            OffsetDateTime createdAt) {
    }

    public record CreateLabRequest(@NotNull UUID submissionId) {
    }

    public record LabResponse(
            UUID id,
            UUID submissionId,
            String studentEmail,
            String imageReference,
            String namespace,
            String deploymentName,
            String serviceName,
            String routeUrl,
            String deployCommand,
            String portForwardCommand,
            LabStatus status,
            OffsetDateTime expiresAt) {
    }

    public record DistributionResponse(UUID moduleId, int createdAssignments) {
    }

    public record BlackBoxAssignmentResponse(
            UUID id,
            UUID moduleId,
            UUID targetLabId,
            String targetUrl,
            String targetImageReference,
            BlackBoxAssignmentStatus status,
            OffsetDateTime assignedAt) {
    }
}
