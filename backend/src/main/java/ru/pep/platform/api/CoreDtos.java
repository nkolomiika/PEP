package ru.pep.platform.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
import ru.pep.platform.domain.PentestTaskArchiveFailedStage;
import ru.pep.platform.domain.PentestTaskArchiveStatus;
import ru.pep.platform.domain.PentestTaskBuildStatus;
import ru.pep.platform.domain.PentestTaskInstanceStatus;
import ru.pep.platform.domain.PentestTaskSourceKind;
import ru.pep.platform.domain.PentestTaskStatus;
import ru.pep.platform.domain.UserPentestStandFailedStage;
import ru.pep.platform.domain.ReportStatus;
import ru.pep.platform.domain.ReportType;
import ru.pep.platform.domain.ReviewDecision;
import ru.pep.platform.domain.Role;
import ru.pep.platform.domain.StudentStreamMembershipStatus;
import ru.pep.platform.domain.StudentStreamStatus;
import ru.pep.platform.domain.SubmissionStatus;
import ru.pep.platform.domain.SubmissionSourceType;
import ru.pep.platform.domain.UserPentestStandInstanceStatus;
import ru.pep.platform.domain.UserPentestStandReviewStatus;
import ru.pep.platform.domain.UserPentestStandStatus;
import ru.pep.platform.domain.UserStatus;
import ru.pep.platform.domain.ValidationJobStatus;

public final class CoreDtos {

    public static final int REPORT_TITLE_MAX_LENGTH = 180;
    public static final int REPORT_MARKDOWN_MAX_LENGTH = 20_000;
    public static final int REVIEW_COMMENT_MAX_LENGTH = 8_000;
    public static final int LOGIN_EMAIL_MAX_LENGTH = 320;
    public static final int LOGIN_PASSWORD_MAX_LENGTH = 256;
    public static final String PENTEST_FLAG_REGEX = "^pep\\{[a-zA-Z0-9]{20}\\}$";

    private CoreDtos() {
    }

    public record CourseResponse(
            UUID id,
            String title,
            String description,
            CourseStatus status,
            List<ModuleResponse> modules) {
    }

    public record ModuleResponse(
            UUID id,
            String title,
            String vulnerabilityTopic,
            ModuleStatus status,
            OffsetDateTime startsAt,
            OffsetDateTime submissionDeadline,
            OffsetDateTime blackBoxStartsAt,
            OffsetDateTime blackBoxDeadline) {
    }

    public record CurrentUserResponse(String email, String displayName, Role role, String avatarUrl) {
    }

    public record AdminUserResponse(UUID id, String email, String displayName, Role role, UserStatus status) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }

    public record CreateUserRequest(
            @NotBlank @Email @Size(max = LOGIN_EMAIL_MAX_LENGTH) String email,
            @NotBlank @Size(max = LOGIN_PASSWORD_MAX_LENGTH) String password,
            @NotBlank @Size(max = 160) String displayName,
            @NotNull Role role) {
    }

    public record UpdateUserRequest(
            @Email @Size(max = LOGIN_EMAIL_MAX_LENGTH) String email,
            @Size(max = 160) String displayName,
            Role role,
            UserStatus status) {
    }

    public record BulkUserActionRequest(@NotNull List<UUID> userIds) {
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
            String runtimeImageReference,
            String placementAfterHeading,
            UUID lessonId,
            UUID moduleId,
            PentestTaskSourceKind sourceKind,
            PentestTaskArchiveStatus archiveStatus,
            PentestTaskArchiveFailedStage archiveFailedStage,
            String archiveFilename,
            Long archiveSizeBytes,
            String archiveBuildLog,
            UUID createdByUserId) {
    }

    public record CreatePentestTaskFromArchiveRequest(
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 100) String category,
            @NotBlank @Size(max = 40) String difficulty,
            @Min(1) @Max(720) Integer durationMinutes,
            @Size(max = 4_000) String descriptionMarkdown,
            @Pattern(regexp = PENTEST_FLAG_REGEX) String flag,
            UUID lessonId,
            @Size(max = 200) String placementAfterHeading) {
    }

    public record PromoteStandToTaskRequest(
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 100) String category,
            @NotBlank @Size(max = 40) String difficulty,
            @Min(1) @Max(720) Integer durationMinutes,
            UUID lessonId,
            @Size(max = 200) String placementAfterHeading) {
    }

    public record AttachTaskToLessonRequest(@Size(max = 200) String placementAfterHeading) {
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
            Integer flagAttempts,
            boolean flagSolved,
            OffsetDateTime flagSolvedAt,
            OffsetDateTime expiresAt) {
    }

    public record PentestTaskFlagSubmitRequest(
            @NotBlank
            @Pattern(regexp = PENTEST_FLAG_REGEX, message = "Флаг должен быть в формате pep{[a-zA-Z0-9]{20}}")
            String flag) {
    }

    public record PentestTaskFlagSubmitResponse(
            UUID instanceId,
            boolean accepted,
            boolean solved,
            Integer attempts,
            PentestTaskInstanceStatus status,
            OffsetDateTime solvedAt,
            String message) {
    }

    public record UserPentestStandResponse(
            UUID id,
            UUID moduleId,
            String moduleTitle,
            UUID ownerId,
            String ownerEmail,
            String ownerDisplayName,
            String displayName,
            String description,
            String originalFilename,
            Long archiveSizeBytes,
            Integer applicationPort,
            String composeService,
            String composeServices,
            boolean authorSolved,
            OffsetDateTime authorSolvedAt,
            Long imageSizeBytes,
            UserPentestStandStatus status,
            UserPentestStandFailedStage failedStage,
            UserPentestStandReviewStatus reviewStatus,
            String reviewComment,
            OffsetDateTime reviewedAt,
            String reviewedByEmail,
            OffsetDateTime submittedForReviewAt,
            String runtimeImageReference,
            String s3Bucket,
            String s3Key,
            String lastError,
            String buildLog,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime builtAt) {
    }

    public record CreateUserStandRequest(
            @NotNull UUID moduleId,
            @Size(max = 200) String displayName,
            @Size(max = 2000) String description) {
    }

    public record SubmitStandFlagRequest(@NotBlank String flag) {
    }

    public record SubmitStandFlagResponse(
            boolean accepted,
            boolean solved,
            Integer attempts,
            String message) {
    }

    public record StandReviewDecisionRequest(@Size(max = 500) String comment) {
    }

    public record UserPentestStandInstanceResponse(
            UUID id,
            UUID standId,
            String standDisplayName,
            String namespace,
            String runtimeImageReference,
            String publicUrl,
            String deployCommand,
            UserPentestStandInstanceStatus status,
            boolean flagSolved,
            OffsetDateTime flagSolvedAt,
            Integer flagAttempts,
            OffsetDateTime expiresAt,
            OffsetDateTime stoppedAt,
            OffsetDateTime createdAt) {
    }

    public record PeerStandAssignmentResponse(
            UUID assignmentId,
            UUID standId,
            String standDisplayName,
            String standDescription,
            UUID moduleId,
            String authorDisplayName,
            boolean solved,
            OffsetDateTime solvedAt,
            OffsetDateTime assignedAt) {
    }

    public record UpdateProfileRequest(
            @Size(max = 160) String displayName, @Email @Size(max = LOGIN_EMAIL_MAX_LENGTH) String email) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = LOGIN_PASSWORD_MAX_LENGTH) String newPassword) {
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

    public record StudentStreamSummaryResponse(
            UUID id,
            String name,
            String description,
            StudentStreamStatus status,
            int courseCount,
            int memberCount,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record StudentStreamCourseResponse(
            UUID courseId,
            String title,
            CourseStatus status,
            Integer position) {
    }

    public record StudentStreamMemberResponse(
            UUID userId,
            String email,
            String displayName,
            Role role,
            StudentStreamMembershipStatus status,
            OffsetDateTime enrolledAt) {
    }

    public record StudentStreamModuleScheduleResponse(
            UUID moduleId,
            String moduleTitle,
            OffsetDateTime startsAt,
            OffsetDateTime submissionDeadline,
            OffsetDateTime blackBoxStartsAt,
            OffsetDateTime blackBoxDeadline) {
    }

    public record StudentStreamResponse(
            UUID id,
            String name,
            String description,
            StudentStreamStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            List<StudentStreamCourseResponse> courses,
            List<StudentStreamMemberResponse> members,
            List<StudentStreamModuleScheduleResponse> moduleSchedules) {
    }

    public record CreateStudentStreamRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2_000) String description,
            StudentStreamStatus status) {
    }

    public record UpdateStudentStreamRequest(
            @Size(max = 200) String name,
            @Size(max = 2_000) String description,
            StudentStreamStatus status) {
    }

    public record AddStreamCourseRequest(@NotNull UUID courseId, Integer position) {
    }

    public record UpsertStreamScheduleRequest(
            OffsetDateTime startsAt,
            OffsetDateTime submissionDeadline,
            OffsetDateTime blackBoxStartsAt,
            OffsetDateTime blackBoxDeadline) {
    }

    public record AddStreamMembersRequest(@NotNull List<UUID> userIds) {
    }

    public record AssignPeerTasksRequest(
            @NotNull UUID moduleId,
            @NotNull List<UUID> userIds,
            @Min(1) @Max(20) Integer count) {
    }
}
