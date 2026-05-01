package ru.pep.platform.api;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.pep.platform.service.AuthSessionService;
import ru.pep.platform.service.CorePlatformService;

@RestController
@RequestMapping("/api")
public class CorePlatformController {

    private final CorePlatformService platform;
    private final AuthSessionService sessions;

    public CorePlatformController(CorePlatformService platform, AuthSessionService sessions) {
        this.platform = platform;
        this.sessions = sessions;
    }

    @GetMapping("/auth/csrf")
    public CoreDtos.CsrfTokenResponse csrf(CsrfToken token) {
        return new CoreDtos.CsrfTokenResponse(token.getToken(), token.getHeaderName(), token.getParameterName());
    }

    @PostMapping("/auth/login")
    public ResponseEntity<CoreDtos.CurrentUserResponse> login(
            @Valid @RequestBody CoreDtos.LoginRequest request,
            HttpServletRequest servletRequest) {
        AuthSessionService.LoginResult result = sessions.login(
                request.email(),
                request.password(),
                clientAddress(servletRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.cookie().toString())
                .body(new CoreDtos.CurrentUserResponse(
                        result.user().getEmail(),
                        result.user().getDisplayName(),
                        result.user().getRole()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = sessions.tokenFrom(request.getCookies());
        if (token != null) {
            sessions.logout(token);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessions.clearSessionCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public CoreDtos.CurrentUserResponse currentUser(Principal principal) {
        return platform.currentUserProfile(principal.getName());
    }

    private String clientAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/courses")
    public List<CoreDtos.CourseResponse> listCourses() {
        return platform.listCourses();
    }

    @PostMapping("/admin/courses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CoreDtos.CourseResponse createCourse(@Valid @RequestBody CoreDtos.CreateCourseRequest request) {
        return platform.createCourse(request);
    }

    @PostMapping("/admin/modules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CoreDtos.ModuleResponse createModule(@Valid @RequestBody CoreDtos.CreateModuleRequest request) {
        return platform.createModule(request);
    }

    @PostMapping("/admin/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CoreDtos.LessonResponse upsertLesson(@Valid @RequestBody CoreDtos.UpsertLessonRequest request) {
        return platform.upsertLesson(request);
    }

    @PostMapping("/admin/lessons/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CoreDtos.LessonResponse updateLesson(
            @PathVariable UUID lessonId,
            @Valid @RequestBody CoreDtos.UpdateLessonRequest request) {
        return platform.updateLesson(lessonId, request);
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CoreDtos.AdminUserResponse> listUsers() {
        return platform.listUsers();
    }

    @PostMapping("/admin/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CoreDtos.AdminUserResponse createUser(@Valid @RequestBody CoreDtos.CreateUserRequest request) {
        return platform.createUser(request);
    }

    @PostMapping("/admin/users/{userId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void disableUser(@PathVariable UUID userId) {
        platform.disableUser(userId);
    }

    @DeleteMapping("/admin/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable UUID userId) {
        platform.disableUser(userId);
    }

    @GetMapping("/admin/online-users")
    @PreAuthorize("hasRole('ADMIN')")
    public CoreDtos.OnlineUsersResponse onlineUsers() {
        return platform.onlineUsers();
    }

    @GetMapping("/modules/{moduleId}/lessons")
    public List<CoreDtos.LessonSummaryResponse> listLessons(@PathVariable UUID moduleId) {
        return platform.listLessons(moduleId);
    }

    @GetMapping("/lessons/{lessonId}")
    public CoreDtos.LessonResponse getLesson(@PathVariable UUID lessonId) {
        return platform.getLesson(lessonId);
    }

    @GetMapping("/modules/{moduleId}/lesson-progress")
    @PreAuthorize("hasRole('STUDENT')")
    public List<CoreDtos.LessonProgressResponse> listLessonProgress(Principal principal, @PathVariable UUID moduleId) {
        return platform.listLessonProgress(principal.getName(), moduleId);
    }

    @PostMapping("/lessons/{lessonId}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public CoreDtos.LessonProgressResponse completeLesson(Principal principal, @PathVariable UUID lessonId) {
        return platform.completeLesson(principal.getName(), lessonId);
    }

    @GetMapping("/modules/{moduleId}/result")
    @PreAuthorize("hasRole('STUDENT')")
    public CoreDtos.ModuleResultResponse getModuleResult(Principal principal, @PathVariable UUID moduleId) {
        return platform.getModuleResult(principal.getName(), moduleId);
    }

    @GetMapping("/modules/{moduleId}/grades/export")
    @PreAuthorize("hasAnyRole('CURATOR','ADMIN')")
    public ResponseEntity<String> exportModuleGrades(Principal principal, @PathVariable UUID moduleId) {
        String csv = platform.exportModuleGradesCsv(principal.getName(), moduleId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"module-" + moduleId + "-grades.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public CoreDtos.SubmissionResponse createSubmission(
            Principal principal,
            @Valid @RequestBody CoreDtos.CreateSubmissionRequest request) {
        return platform.createSubmission(principal.getName(), request);
    }

    @PostMapping(path = "/submissions/archive", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public CoreDtos.SubmissionResponse createArchiveSubmission(
            Principal principal,
            @RequestParam("moduleId") UUID moduleId,
            @RequestParam("applicationPort") Integer applicationPort,
            @RequestParam(value = "healthPath", required = false) String healthPath,
            @RequestParam(value = "composeService", required = false) String composeService,
            @RequestParam("archive") MultipartFile archive) {
        return platform.createArchiveSubmission(
                principal.getName(),
                moduleId,
                applicationPort,
                healthPath,
                composeService,
                archive);
    }

    @GetMapping("/submissions")
    @PreAuthorize("hasAnyRole('STUDENT','CURATOR','ADMIN')")
    public List<CoreDtos.SubmissionResponse> listSubmissions(Principal principal) {
        return platform.listSubmissions(principal.getName());
    }

    @GetMapping("/validation-jobs/{jobId}")
    @PreAuthorize("hasAnyRole('STUDENT','CURATOR','ADMIN')")
    public CoreDtos.ValidationJobResponse getValidationJob(Principal principal, @PathVariable UUID jobId) {
        return platform.getValidationJob(principal.getName(), jobId);
    }

    @GetMapping("/validation-jobs")
    @PreAuthorize("hasAnyRole('STUDENT','CURATOR','ADMIN')")
    public List<CoreDtos.ValidationJobResponse> listValidationJobs(Principal principal) {
        return platform.listValidationJobs(principal.getName());
    }

    @PostMapping("/validation-jobs/{jobId}/complete")
    @PreAuthorize("hasAnyRole('CURATOR','ADMIN')")
    public CoreDtos.ValidationJobResponse completeValidation(
            Principal principal,
            @PathVariable UUID jobId,
            @RequestBody CoreDtos.CompleteValidationRequest request) {
        return platform.completeValidation(principal.getName(), jobId, request);
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public CoreDtos.ReportResponse createReport(
            Principal principal,
            @Valid @RequestBody CoreDtos.CreateReportRequest request) {
        return platform.createReport(principal.getName(), request);
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('STUDENT','CURATOR','ADMIN')")
    public List<CoreDtos.ReportResponse> listReports(Principal principal) {
        return platform.listReports(principal.getName());
    }

    @PostMapping("/reports/{reportId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public CoreDtos.ReportAttachmentResponse uploadReportAttachment(
            Principal principal,
            @PathVariable UUID reportId,
            @RequestParam("file") MultipartFile file) {
        return platform.uploadReportAttachment(principal.getName(), reportId, file);
    }

    @GetMapping("/report-attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('STUDENT','CURATOR','ADMIN')")
    public ResponseEntity<FileSystemResource> downloadReportAttachment(
            Principal principal,
            @PathVariable UUID attachmentId) {
        CorePlatformService.AttachmentDownload attachment = platform.downloadReportAttachment(principal.getName(), attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.contentType()))
                .contentLength(attachment.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(attachment.filename())
                        .build()
                        .toString())
                .body(new FileSystemResource(attachment.path()));
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CURATOR','ADMIN')")
    public CoreDtos.ReviewResponse createReview(
            Principal principal,
            @Valid @RequestBody CoreDtos.CreateReviewRequest request) {
        return platform.createReview(principal.getName(), request);
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasAnyRole('STUDENT','CURATOR','ADMIN')")
    public List<CoreDtos.ReviewResponse> listReviews(Principal principal) {
        return platform.listReviews(principal.getName());
    }

    @PostMapping("/labs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CoreDtos.LabResponse createLab(
            Principal principal,
            @Valid @RequestBody CoreDtos.CreateLabRequest request) {
        return platform.createLab(principal.getName(), request);
    }

    @GetMapping("/labs")
    @PreAuthorize("hasAnyRole('CURATOR','ADMIN')")
    public List<CoreDtos.LabResponse> listLabs() {
        return platform.listLabs();
    }

    @PostMapping("/modules/{moduleId}/black-box-assignments/distribute")
    @PreAuthorize("hasRole('ADMIN')")
    public CoreDtos.DistributionResponse distributeBlackBox(Principal principal, @PathVariable UUID moduleId) {
        return platform.distributeBlackBox(principal.getName(), moduleId);
    }

    @GetMapping("/black-box-assignments/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<CoreDtos.BlackBoxAssignmentResponse> listMyAssignments(Principal principal) {
        return platform.listMyAssignments(principal.getName());
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CoreDtos.AuditEventResponse> latestAuditEvents() {
        return platform.latestAuditEvents();
    }

    @GetMapping("/live/status")
    @PreAuthorize("hasAnyRole('STUDENT','CURATOR','ADMIN')")
    public CoreDtos.LiveStatusResponse liveStatus(Principal principal) {
        return platform.liveStatus(principal.getName());
    }

    @GetMapping(path = "/live/status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT','CURATOR','ADMIN')")
    public SseEmitter liveStatusStream(Principal principal) {
        SseEmitter emitter = new SseEmitter(60_000L);
        String email = principal.getName();
        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < 30; i++) {
                    emitter.send(SseEmitter.event()
                            .name("status")
                            .data(platform.liveStatus(email)));
                    Thread.sleep(2_000L);
                }
                emitter.complete();
            } catch (IOException exception) {
                emitter.completeWithError(exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }
}
