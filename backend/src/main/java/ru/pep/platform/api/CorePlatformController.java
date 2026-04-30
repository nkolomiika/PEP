package ru.pep.platform.api;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.pep.platform.service.CorePlatformService;

@RestController
@RequestMapping("/api")
public class CorePlatformController {

    private final CorePlatformService platform;

    public CorePlatformController(CorePlatformService platform) {
        this.platform = platform;
    }

    @GetMapping("/courses")
    public List<CoreDtos.CourseResponse> listCourses() {
        return platform.listCourses();
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

    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public CoreDtos.SubmissionResponse createSubmission(
            Principal principal,
            @Valid @RequestBody CoreDtos.CreateSubmissionRequest request) {
        return platform.createSubmission(principal.getName(), request);
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
    @PreAuthorize("hasAnyRole('STUDENT','CURATOR','ADMIN')")
    public CoreDtos.ReportAttachmentResponse uploadReportAttachment(
            Principal principal,
            @PathVariable UUID reportId,
            @RequestParam("file") MultipartFile file) {
        return platform.uploadReportAttachment(principal.getName(), reportId, file);
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
}
