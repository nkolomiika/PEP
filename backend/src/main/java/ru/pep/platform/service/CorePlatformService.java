package ru.pep.platform.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pep.platform.api.CoreDtos;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.Course;
import ru.pep.platform.domain.LearningModule;
import ru.pep.platform.domain.Report;
import ru.pep.platform.domain.Review;
import ru.pep.platform.domain.Role;
import ru.pep.platform.domain.Submission;
import ru.pep.platform.domain.ValidationJob;
import ru.pep.platform.repository.AppUserRepository;
import ru.pep.platform.repository.CourseRepository;
import ru.pep.platform.repository.LearningModuleRepository;
import ru.pep.platform.repository.ReportRepository;
import ru.pep.platform.repository.ReviewRepository;
import ru.pep.platform.repository.SubmissionRepository;
import ru.pep.platform.repository.ValidationJobRepository;

@Service
public class CorePlatformService {

    private final AppUserRepository users;
    private final CourseRepository courses;
    private final LearningModuleRepository modules;
    private final SubmissionRepository submissions;
    private final ValidationJobRepository validationJobs;
    private final ReportRepository reports;
    private final ReviewRepository reviews;
    private final AuditService audit;

    public CorePlatformService(
            AppUserRepository users,
            CourseRepository courses,
            LearningModuleRepository modules,
            SubmissionRepository submissions,
            ValidationJobRepository validationJobs,
            ReportRepository reports,
            ReviewRepository reviews,
            AuditService audit) {
        this.users = users;
        this.courses = courses;
        this.modules = modules;
        this.submissions = submissions;
        this.validationJobs = validationJobs;
        this.reports = reports;
        this.reviews = reviews;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.CourseResponse> listCourses() {
        return courses.findAll().stream().map(this::toCourseResponse).toList();
    }

    @Transactional
    public CoreDtos.SubmissionResponse createSubmission(String email, CoreDtos.CreateSubmissionRequest request) {
        AppUser student = currentUser(email);
        if (student.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Только студент может отправить приложение");
        }
        LearningModule module = modules.findById(request.moduleId())
                .orElseThrow(() -> new NotFoundException("Модуль не найден"));
        Submission submission = submissions.save(new Submission(
                module,
                student,
                request.imageReference(),
                request.applicationPort(),
                normalizeHealthPath(request.healthPath())));
        ValidationJob job = validationJobs.save(new ValidationJob(submission));
        audit.record(student, "SUBMISSION_CREATED", "Submission", submission.getId(), "{\"validationJobId\":\"" + job.getId() + "\"}");
        return toSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.SubmissionResponse> listSubmissions(String email) {
        AppUser user = currentUser(email);
        List<Submission> result = user.getRole() == Role.STUDENT ? submissions.findByStudent(user) : submissions.findAll();
        return result.stream().map(this::toSubmissionResponse).toList();
    }

    @Transactional(readOnly = true)
    public CoreDtos.ValidationJobResponse getValidationJob(String email, UUID id) {
        AppUser user = currentUser(email);
        ValidationJob job = validationJobs.findById(id)
                .orElseThrow(() -> new NotFoundException("Validation job не найден"));
        assertCanReadSubmission(user, job.getSubmission());
        return toValidationJobResponse(job);
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.ValidationJobResponse> listValidationJobs(String email) {
        AppUser user = currentUser(email);
        return validationJobs.findAll().stream()
                .filter(job -> user.getRole() != Role.STUDENT || job.getSubmission().getStudent().getId().equals(user.getId()))
                .map(this::toValidationJobResponse)
                .toList();
    }

    @Transactional
    public CoreDtos.ValidationJobResponse completeValidation(
            String email,
            UUID id,
            CoreDtos.CompleteValidationRequest request) {
        AppUser actor = currentUser(email);
        ValidationJob job = validationJobs.findById(id)
                .orElseThrow(() -> new NotFoundException("Validation job не найден"));
        if (request.passed()) {
            job.pass(request.logsUri());
            audit.record(actor, "VALIDATION_JOB_PASSED", "ValidationJob", job.getId(), "{}");
        } else {
            job.fail(request.errorMessage() == null ? "Техническая проверка не пройдена" : request.errorMessage());
            audit.record(actor, "VALIDATION_JOB_FAILED", "ValidationJob", job.getId(), "{}");
        }
        return toValidationJobResponse(job);
    }

    @Transactional
    public CoreDtos.ReportResponse createReport(String email, CoreDtos.CreateReportRequest request) {
        AppUser author = currentUser(email);
        LearningModule module = modules.findById(request.moduleId())
                .orElseThrow(() -> new NotFoundException("Модуль не найден"));
        Submission submission = null;
        if (request.submissionId() != null) {
            submission = submissions.findById(request.submissionId())
                    .orElseThrow(() -> new NotFoundException("Submission не найден"));
            assertCanReadSubmission(author, submission);
        }
        Report report = reports.save(new Report(
                author,
                module,
                submission,
                request.type(),
                request.title(),
                request.contentMarkdown()));
        audit.record(author, "REPORT_SUBMITTED", "Report", report.getId(), "{\"type\":\"" + report.getType() + "\"}");
        return toReportResponse(report);
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.ReportResponse> listReports(String email) {
        AppUser user = currentUser(email);
        List<Report> result = user.getRole() == Role.STUDENT ? reports.findByAuthor(user) : reports.findAll();
        return result.stream().map(this::toReportResponse).toList();
    }

    @Transactional
    public CoreDtos.ReviewResponse createReview(String email, CoreDtos.CreateReviewRequest request) {
        AppUser curator = currentUser(email);
        Report report = reports.findById(request.reportId())
                .orElseThrow(() -> new NotFoundException("Отчет не найден"));
        Review review = reviews.save(new Review(report, curator, request.decision(), request.score(), request.commentMarkdown()));
        audit.record(curator, "REVIEW_COMPLETED", "Report", report.getId(), "{\"decision\":\"" + request.decision() + "\"}");
        return toReviewResponse(review);
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.AuditEventResponse> latestAuditEvents() {
        return audit.latest().stream()
                .map(event -> new CoreDtos.AuditEventResponse(
                        event.getId(),
                        event.getActor() == null ? null : event.getActor().getEmail(),
                        event.getAction(),
                        event.getTargetType(),
                        event.getTargetId(),
                        event.getMetadataJson(),
                        event.getCreatedAt()))
                .toList();
    }

    private AppUser currentUser(String email) {
        return users.findByEmail(email).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private void assertCanReadSubmission(AppUser user, Submission submission) {
        if (user.getRole() == Role.STUDENT && !submission.getStudent().getId().equals(user.getId())) {
            throw new AccessDeniedException("Нет доступа к чужой работе");
        }
    }

    private String normalizeHealthPath(String healthPath) {
        if (healthPath == null || healthPath.isBlank()) {
            return "/health";
        }
        return healthPath.startsWith("/") ? healthPath : "/" + healthPath;
    }

    private CoreDtos.CourseResponse toCourseResponse(Course course) {
        return new CoreDtos.CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getStatus(),
                course.getModules().stream()
                        .map(module -> new CoreDtos.ModuleResponse(
                                module.getId(),
                                module.getTitle(),
                                module.getVulnerabilityTopic(),
                                module.getStatus()))
                        .toList());
    }

    private CoreDtos.SubmissionResponse toSubmissionResponse(Submission submission) {
        return new CoreDtos.SubmissionResponse(
                submission.getId(),
                submission.getModule().getId(),
                submission.getStudent().getEmail(),
                submission.getImageReference(),
                submission.getApplicationPort(),
                submission.getHealthPath(),
                submission.getStatus());
    }

    private CoreDtos.ValidationJobResponse toValidationJobResponse(ValidationJob job) {
        return new CoreDtos.ValidationJobResponse(
                job.getId(),
                job.getSubmission().getId(),
                job.getImageReference(),
                job.getStatus(),
                job.getLogsUri(),
                job.getErrorMessage());
    }

    private CoreDtos.ReportResponse toReportResponse(Report report) {
        return new CoreDtos.ReportResponse(
                report.getId(),
                report.getAuthor().getEmail(),
                report.getModule().getId(),
                report.getSubmission() == null ? null : report.getSubmission().getId(),
                report.getType(),
                report.getTitle(),
                report.getContentMarkdown(),
                report.getStatus());
    }

    private CoreDtos.ReviewResponse toReviewResponse(Review review) {
        return new CoreDtos.ReviewResponse(
                review.getId(),
                review.getReport().getId(),
                review.getCurator().getEmail(),
                review.getDecision(),
                review.getScore(),
                review.getCommentMarkdown());
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }
}
