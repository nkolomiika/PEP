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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
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

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CURATOR','ADMIN')")
    public CoreDtos.ReviewResponse createReview(
            Principal principal,
            @Valid @RequestBody CoreDtos.CreateReviewRequest request) {
        return platform.createReview(principal.getName(), request);
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CoreDtos.AuditEventResponse> latestAuditEvents() {
        return platform.latestAuditEvents();
    }
}
