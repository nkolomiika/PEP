package ru.pep.platform.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import ru.pep.platform.domain.CourseStatus;
import ru.pep.platform.domain.BlackBoxAssignmentStatus;
import ru.pep.platform.domain.DependencyScanStatus;
import ru.pep.platform.domain.ImageScanStatus;
import ru.pep.platform.domain.LabStatus;
import ru.pep.platform.domain.ModuleStatus;
import ru.pep.platform.domain.PentestTaskBuildStatus;
import ru.pep.platform.domain.PentestTaskInstanceStatus;
import ru.pep.platform.domain.PentestTaskStatus;
import ru.pep.platform.domain.ReportStatus;
import ru.pep.platform.domain.ReportType;
import ru.pep.platform.domain.ReviewDecision;
import ru.pep.platform.domain.Role;
import ru.pep.platform.domain.SubmissionStatus;
import ru.pep.platform.domain.SubmissionSourceType;
import ru.pep.platform.domain.UserStatus;
import ru.pep.platform.domain.ValidationJobStatus;

public final class CoreDtos {

    public static final int REPORT_TITLE_MAX_LENGTH = 180;
    public static final int REPORT_MARKDOWN_MAX_LENGTH = 20_000;
    public static final int REVIEW_COMMENT_MAX_LENGTH = 8_000;
    public static final int LOGIN_EMAIL_MAX_LENGTH = 320;
    public static final int LOGIN_PASSWORD_MAX_LENGTH = 256;

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

    public record CurrentUserResponse(String email, String displayName, Role role) {
    }

    public record AdminUserResponse(UUID id, String email, String displayName, Role role, UserStatus status) {
    }

    public record CreateUserRequest(
            @NotBlank @Email @Size(max = LOGIN_EMAIL_MAX_LENGTH) String email,
            @NotBlank @Size(max = LOGIN_PASSWORD_MAX_LENGTH) String password,
            @NotBlank @Size(max = 160) String displayName,
            @NotNull Role role) {
    }

    public record OnlineUsersResponse(long activeUsers, long activeSessions) {
    }

    public record PentestTaskResponse(
            UUID id,
            String title,
            String slug,
            String category,
            String difficulty,
            Integer durationMinutes,
            Integer entrypointPort,
            String healthPath,
            String composeService,
            String descriptionMarkdown,
            String repositoryUrl,
            String branchName,
            String commitSha,
            PentestTaskStatus status,
            PentestTaskBuildStatus buildStatus,
            String runtimeImageReference) {
    }

    public record PentestTaskInstanceResponse(
            UUID id,
            UUID taskId,
            String taskTitle,
            String category,
            String namespace,
            String runtimeImageReference,
            String publicUrl,
            String localHostUrl,
            String deployCommand,
            PentestTaskInstanceStatus status,
            OffsetDateTime expiresAt) {
    }

    public record GitLabSyncResponse(int scannedProjects, int syncedTasks) {
    }

    public record CreateCourseRequest(@NotBlank @Size(max = 200) String title, @NotBlank String description) {
    }

    public record CreateModuleRequest(
            @NotNull UUID courseId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 120) String vulnerabilityTopic) {
    }

    public record UpsertLessonRequest(
            @NotNull UUID moduleId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank String contentMarkdown,
            @NotNull @Min(1) Integer position) {
    }

    public record UpdateLessonRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String contentMarkdown,
            @NotNull @Min(1) Integer position) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = LOGIN_EMAIL_MAX_LENGTH) String email,
            @NotBlank @Size(max = LOGIN_PASSWORD_MAX_LENGTH) String password) {
    }

    public record CsrfTokenResponse(String token, String headerName, String parameterName) {
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
            SubmissionSourceType sourceType,
            String archiveFilename,
            String composeService,
            String buildContext,
            String runtimeImageReference,
            String publicUrl,
            String localHostUrl,
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
            @NotBlank @Size(max = REPORT_TITLE_MAX_LENGTH) String title,
            @NotBlank @Size(max = REPORT_MARKDOWN_MAX_LENGTH) String contentMarkdown) {
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
            @NotBlank @Size(max = REVIEW_COMMENT_MAX_LENGTH) String commentMarkdown) {
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

    public record LiveStatusResponse(
            String role,
            Integer submissions,
            Integer validationJobs,
            Integer runningLabs,
            Integer reports,
            Integer assignments,
            OffsetDateTime updatedAt) {
    }

    public record CreateLabRequest(@NotNull UUID submissionId) {
    }

    public record LabResponse(
            UUID id,
            UUID submissionId,
            String studentEmail,
            String imageReference,
            SubmissionSourceType sourceType,
            String runtimeImageReference,
            String publicUrl,
            String localHostUrl,
            String namespace,
            String deploymentName,
            String serviceName,
            String routeUrl,
            String ingressUrl,
            String deployCommand,
            String ingressInstallCommand,
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
