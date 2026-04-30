package ru.pep.platform.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pep.platform.api.CoreDtos;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.BlackBoxAssignment;
import ru.pep.platform.domain.LabInstance;
import ru.pep.platform.domain.Course;
import ru.pep.platform.domain.LearningModule;
import ru.pep.platform.domain.Lesson;
import ru.pep.platform.domain.LessonProgress;
import ru.pep.platform.domain.Report;
import ru.pep.platform.domain.Review;
import ru.pep.platform.domain.ReportType;
import ru.pep.platform.domain.Role;
import ru.pep.platform.domain.ReviewDecision;
import ru.pep.platform.domain.Submission;
import ru.pep.platform.domain.SubmissionStatus;
import ru.pep.platform.domain.ValidationJob;
import ru.pep.platform.repository.AppUserRepository;
import ru.pep.platform.repository.BlackBoxAssignmentRepository;
import ru.pep.platform.repository.CourseRepository;
import ru.pep.platform.repository.LabInstanceRepository;
import ru.pep.platform.repository.LearningModuleRepository;
import ru.pep.platform.repository.LessonProgressRepository;
import ru.pep.platform.repository.LessonRepository;
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
    private final LabInstanceRepository labs;
    private final BlackBoxAssignmentRepository assignments;
    private final LessonRepository lessons;
    private final LessonProgressRepository lessonProgress;
    private final AuditService audit;

    public CorePlatformService(
            AppUserRepository users,
            CourseRepository courses,
            LearningModuleRepository modules,
            SubmissionRepository submissions,
            ValidationJobRepository validationJobs,
            ReportRepository reports,
            ReviewRepository reviews,
            LabInstanceRepository labs,
            BlackBoxAssignmentRepository assignments,
            LessonRepository lessons,
            LessonProgressRepository lessonProgress,
            AuditService audit) {
        this.users = users;
        this.courses = courses;
        this.modules = modules;
        this.submissions = submissions;
        this.validationJobs = validationJobs;
        this.reports = reports;
        this.reviews = reviews;
        this.labs = labs;
        this.assignments = assignments;
        this.lessons = lessons;
        this.lessonProgress = lessonProgress;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.CourseResponse> listCourses() {
        return courses.findAll().stream().map(this::toCourseResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.LessonSummaryResponse> listLessons(UUID moduleId) {
        if (!modules.existsById(moduleId)) {
            throw new NotFoundException("Модуль не найден");
        }
        return lessons.findByModuleIdAndPublishedTrueOrderByPositionAsc(moduleId).stream()
                .map(lesson -> new CoreDtos.LessonSummaryResponse(
                        lesson.getId(),
                        lesson.getModule().getId(),
                        lesson.getTitle(),
                        lesson.getPosition()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CoreDtos.LessonResponse getLesson(UUID lessonId) {
        Lesson lesson = lessons.findById(lessonId)
                .filter(Lesson::getPublished)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));
        return new CoreDtos.LessonResponse(
                lesson.getId(),
                lesson.getModule().getId(),
                lesson.getTitle(),
                lesson.getContentMarkdown(),
                lesson.getPosition());
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.LessonProgressResponse> listLessonProgress(String email, UUID moduleId) {
        AppUser student = currentUser(email);
        if (student.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Progress доступен только студенту");
        }
        if (!modules.existsById(moduleId)) {
            throw new NotFoundException("Модуль не найден");
        }
        return lessonProgress.findModuleProgress(student.getId(), moduleId).stream()
                .map(this::toLessonProgressResponse)
                .toList();
    }

    @Transactional
    public CoreDtos.LessonProgressResponse completeLesson(String email, UUID lessonId) {
        AppUser student = currentUser(email);
        if (student.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Только студент может отмечать уроки");
        }
        Lesson lesson = lessons.findById(lessonId)
                .filter(Lesson::getPublished)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));
        LessonProgress progress = lessonProgress.findByStudentAndLesson(student, lesson)
                .orElseGet(() -> lessonProgress.save(new LessonProgress(student, lesson)));
        audit.record(student, "LESSON_COMPLETED", "Lesson", lesson.getId(), "{}");
        return toLessonProgressResponse(progress);
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
        BlackBoxAssignment assignment = null;
        if (request.blackBoxAssignmentId() != null) {
            assignment = assignments.findById(request.blackBoxAssignmentId())
                    .orElseThrow(() -> new NotFoundException("Black box assignment не найден"));
            if (!assignment.getStudent().getId().equals(author.getId())) {
                throw new AccessDeniedException("Нет доступа к чужому black box заданию");
            }
        }
        Report report = reports.save(new Report(
                author,
                module,
                submission,
                assignment,
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
    public List<CoreDtos.ReviewResponse> listReviews(String email) {
        AppUser user = currentUser(email);
        List<Review> result = user.getRole() == Role.STUDENT ? reviews.findByReportAuthor(user) : reviews.findAll();
        return result.stream().map(this::toReviewResponse).toList();
    }

    @Transactional(readOnly = true)
    public CoreDtos.ModuleResultResponse getModuleResult(String email, UUID moduleId) {
        AppUser student = currentUser(email);
        if (student.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Итог модуля доступен только студенту");
        }
        if (!modules.existsById(moduleId)) {
            throw new NotFoundException("Модуль не найден");
        }
        boolean dockerPassed = submissions.findByStudentAndModuleId(student, moduleId).stream()
                .anyMatch(submission -> submission.getStatus() == SubmissionStatus.READY_FOR_REVIEW
                        || submission.getStatus() == SubmissionStatus.APPROVED
                        || submission.getStatus() == SubmissionStatus.PUBLISHED_FOR_BLACK_BOX);
        List<Review> moduleReviews = reviews.findByReportAuthorAndReportModuleId(student, moduleId);
        Integer whiteBoxScore = bestScore(moduleReviews, ReportType.WHITE_BOX);
        Integer blackBoxScore = bestScore(moduleReviews, ReportType.BLACK_BOX);
        Integer finalScore = dockerPassed && whiteBoxScore != null && blackBoxScore != null
                ? Math.round((float) (whiteBoxScore * 0.45 + blackBoxScore * 0.55))
                : null;
        String status;
        if (!dockerPassed) {
            status = "DOCKER_REQUIRED";
        } else if (whiteBoxScore == null || blackBoxScore == null) {
            status = "IN_PROGRESS";
        } else {
            status = "COMPLETED";
        }
        return new CoreDtos.ModuleResultResponse(moduleId, dockerPassed, whiteBoxScore, blackBoxScore, finalScore, status);
    }

    @Transactional
    public CoreDtos.LabResponse createLab(String email, CoreDtos.CreateLabRequest request) {
        AppUser actor = currentUser(email);
        Submission submission = submissions.findById(request.submissionId())
                .orElseThrow(() -> new NotFoundException("Submission не найден"));
        if (submission.getStatus() != SubmissionStatus.APPROVED) {
            throw new AccessDeniedException("Lab можно создать только для принятой работы");
        }
        LabInstance lab = labs.findBySubmission(submission)
                .orElseGet(() -> labs.save(new LabInstance(
                        submission,
                        "pep-lab-" + shortId(submission.getId()),
                        "lab-" + shortId(submission.getId()),
                        "svc-" + shortId(submission.getId()),
                        "http://localhost:18080")));
        audit.record(actor, "LAB_CREATED", "LabInstance", lab.getId(), "{\"submissionId\":\"" + submission.getId() + "\"}");
        return toLabResponse(lab);
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.LabResponse> listLabs() {
        return labs.findAll().stream().map(this::toLabResponse).toList();
    }

    @Transactional
    public CoreDtos.DistributionResponse distributeBlackBox(String email, UUID moduleId) {
        AppUser actor = currentUser(email);
        LearningModule module = modules.findById(moduleId)
                .orElseThrow(() -> new NotFoundException("Модуль не найден"));
        List<LabInstance> moduleLabs = labs.findBySubmissionModuleId(moduleId);
        List<AppUser> students = users.findAll().stream()
                .filter(user -> user.getRole() == Role.STUDENT)
                .toList();
        int created = 0;
        for (AppUser student : students) {
            for (LabInstance lab : moduleLabs) {
                if (lab.getSubmission().getStudent().getId().equals(student.getId())) {
                    continue;
                }
                if (assignments.existsByModuleAndStudentAndTargetLabInstance(module, student, lab)) {
                    continue;
                }
                assignments.save(new BlackBoxAssignment(module, student, lab));
                created++;
                break;
            }
        }
        audit.record(actor, "BLACK_BOX_DISTRIBUTION_COMPLETED", "Module", moduleId, "{\"createdAssignments\":" + created + "}");
        return new CoreDtos.DistributionResponse(moduleId, created);
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.BlackBoxAssignmentResponse> listMyAssignments(String email) {
        AppUser student = currentUser(email);
        return assignments.findByStudent(student).stream().map(this::toAssignmentResponse).toList();
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

    private String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private Integer bestScore(List<Review> moduleReviews, ReportType reportType) {
        return moduleReviews.stream()
                .filter(review -> review.getReport().getType() == reportType)
                .filter(review -> review.getDecision() == ReviewDecision.APPROVED)
                .map(Review::getScore)
                .max(Integer::compareTo)
                .orElse(null);
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

    private CoreDtos.LessonProgressResponse toLessonProgressResponse(LessonProgress progress) {
        return new CoreDtos.LessonProgressResponse(
                progress.getLesson().getId(),
                true,
                progress.getCompletedAt());
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
                report.getBlackBoxAssignment() == null ? null : report.getBlackBoxAssignment().getId(),
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

    private CoreDtos.LabResponse toLabResponse(LabInstance lab) {
        return new CoreDtos.LabResponse(
                lab.getId(),
                lab.getSubmission().getId(),
                lab.getSubmission().getStudent().getEmail(),
                lab.getSubmission().getImageReference(),
                lab.getNamespace(),
                lab.getDeploymentName(),
                lab.getServiceName(),
                lab.getRouteUrl(),
                lab.getStatus(),
                lab.getExpiresAt());
    }

    private CoreDtos.BlackBoxAssignmentResponse toAssignmentResponse(BlackBoxAssignment assignment) {
        LabInstance lab = assignment.getTargetLabInstance();
        return new CoreDtos.BlackBoxAssignmentResponse(
                assignment.getId(),
                assignment.getModule().getId(),
                lab.getId(),
                lab.getRouteUrl(),
                lab.getSubmission().getImageReference(),
                assignment.getStatus(),
                assignment.getAssignedAt());
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
