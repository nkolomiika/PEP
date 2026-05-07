package ru.pep.platform.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.pep.platform.api.CoreDtos;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.BlackBoxAssignment;
import ru.pep.platform.domain.LabInstance;
import ru.pep.platform.domain.Course;
import ru.pep.platform.domain.CourseStatus;
import ru.pep.platform.domain.LearningModule;
import ru.pep.platform.domain.Lesson;
import ru.pep.platform.domain.LabStatus;
import ru.pep.platform.domain.ModuleStatus;
import ru.pep.platform.domain.Report;
import ru.pep.platform.domain.ReportAttachment;
import ru.pep.platform.domain.Review;
import ru.pep.platform.domain.ReportType;
import ru.pep.platform.domain.Role;
import ru.pep.platform.domain.ReviewDecision;
import ru.pep.platform.domain.Submission;
import ru.pep.platform.domain.SubmissionSourceType;
import ru.pep.platform.domain.SubmissionStatus;
import ru.pep.platform.domain.UserStatus;
import ru.pep.platform.domain.ValidationJob;
import ru.pep.platform.domain.ValidationJobStatus;
import ru.pep.platform.repository.AppUserRepository;
import ru.pep.platform.repository.AuthSessionRepository;
import ru.pep.platform.repository.BlackBoxAssignmentRepository;
import ru.pep.platform.repository.CourseRepository;
import ru.pep.platform.repository.LabInstanceRepository;
import ru.pep.platform.repository.LearningModuleRepository;
import ru.pep.platform.repository.LessonRepository;
import ru.pep.platform.repository.ReportAttachmentRepository;
import ru.pep.platform.repository.ReportRepository;
import ru.pep.platform.repository.ReviewRepository;
import ru.pep.platform.repository.SubmissionRepository;
import ru.pep.platform.repository.ValidationJobRepository;

@Service
public class CorePlatformService {

    private static final int MAX_BLACK_BOX_TARGETS_PER_STUDENT = 3;
    private static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS = Set.of(
            ".txt",
            ".md",
            ".pdf",
            ".png",
            ".jpg",
            ".jpeg",
            ".webp");
    private static final Set<String> ALLOWED_ATTACHMENT_CONTENT_TYPES = Set.of(
            "text/plain",
            "text/markdown",
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/webp");
    private static final Set<String> ALLOWED_ARCHIVE_EXTENSIONS = Set.of(".zip", ".tar", ".tar.gz", ".tgz");
    private static final long MAX_ARCHIVE_SIZE_BYTES = 50L * 1024L * 1024L;

    private final AppUserRepository users;
    private final AuthSessionRepository authSessions;
    private final CourseRepository courses;
    private final LearningModuleRepository modules;
    private final SubmissionRepository submissions;
    private final ValidationJobRepository validationJobs;
    private final ReportRepository reports;
    private final ReviewRepository reviews;
    private final LabInstanceRepository labs;
    private final BlackBoxAssignmentRepository assignments;
    private final LessonRepository lessons;
    private final ReportAttachmentRepository reportAttachments;
    private final AuditService audit;
    private final StudentStreamService streams;
    private final PasswordEncoder passwordEncoder;
    private final Path attachmentStorageDirectory;
    private final Path archiveStorageDirectory;
    private final Path avatarStorageDirectory;

    public CorePlatformService(
            AppUserRepository users,
            AuthSessionRepository authSessions,
            CourseRepository courses,
            LearningModuleRepository modules,
            SubmissionRepository submissions,
            ValidationJobRepository validationJobs,
            ReportRepository reports,
            ReviewRepository reviews,
            LabInstanceRepository labs,
            BlackBoxAssignmentRepository assignments,
            LessonRepository lessons,
            ReportAttachmentRepository reportAttachments,
            AuditService audit,
            StudentStreamService streams,
            PasswordEncoder passwordEncoder,
            @Value("${pep.attachments.storage-dir:var/report-attachments}") String attachmentStorageDirectory,
            @Value("${pep.submissions.archive-storage-dir:var/submission-archives}") String archiveStorageDirectory,
            @Value("${pep.avatar.storage-dir:var/avatars}") String avatarStorageDirectory) {
        this.users = users;
        this.authSessions = authSessions;
        this.courses = courses;
        this.modules = modules;
        this.submissions = submissions;
        this.validationJobs = validationJobs;
        this.reports = reports;
        this.reviews = reviews;
        this.labs = labs;
        this.assignments = assignments;
        this.lessons = lessons;
        this.reportAttachments = reportAttachments;
        this.audit = audit;
        this.streams = streams;
        this.passwordEncoder = passwordEncoder;
        this.attachmentStorageDirectory = Path.of(attachmentStorageDirectory).toAbsolutePath().normalize();
        this.archiveStorageDirectory = Path.of(archiveStorageDirectory).toAbsolutePath().normalize();
        this.avatarStorageDirectory = Path.of(avatarStorageDirectory).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.CourseResponse> listCourses(String principalEmail) {
        AppUser actor = principalEmail == null ? null : users.findByEmail(principalEmail).orElse(null);
        boolean isStudent = actor != null && actor.getRole() == Role.STUDENT;
        java.util.Set<UUID> accessibleCourseIds = isStudent
                ? streams.accessibleCourseIds(actor)
                : null;
        AppUser viewerForSchedule = isStudent ? actor : null;
        return courses.findAll().stream()
                .filter(course -> course.getStatus() == ru.pep.platform.domain.CourseStatus.PUBLISHED)
                .filter(course -> accessibleCourseIds == null || accessibleCourseIds.contains(course.getId()))
                .map(course -> toCourseResponse(course, viewerForSchedule))
                .toList();
    }

    @Transactional(readOnly = true)
    public CoreDtos.CurrentUserResponse currentUserProfile(String email) {
        AppUser user = currentUser(email);
        return toUserResponse(user);
    }

    public CoreDtos.CurrentUserResponse toUserResponse(AppUser user) {
        String avatarUrl = (user.getAvatarStorageKey() == null || user.getAvatarStorageKey().isBlank())
                ? null
                : "/api/me/avatar?v=" + user.getUpdatedAt().toInstant().toEpochMilli();
        return new CoreDtos.CurrentUserResponse(
                user.getEmail(), user.getDisplayName(), user.getRole(), avatarUrl);
    }

    @Transactional
    public CoreDtos.CurrentUserResponse updateProfile(String principalEmail, CoreDtos.UpdateProfileRequest request) {
        AppUser user = currentUser(principalEmail);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.applyDisplayName(request.displayName().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            if (!email.equals(user.getEmail())) {
                Optional<AppUser> other = users.findByEmail(email);
                if (other.isPresent() && !other.get().getId().equals(user.getId())) {
                    throw new ValidationException("Пользователь с таким email уже существует");
                }
            }
            user.applyEmail(email);
        }
        audit.record(user, "PROFILE_UPDATED", "AppUser", user.getId(), "{}");
        return toUserResponse(users.save(user));
    }

    @Transactional
    public void changePassword(String principalEmail, CoreDtos.ChangePasswordRequest request) {
        AppUser user = currentUser(principalEmail);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Неверный текущий пароль");
        }
        user.applyPasswordHash(passwordEncoder.encode(request.newPassword()));
        audit.record(user, "PASSWORD_CHANGED", "AppUser", user.getId(), "{}");
    }

    @Transactional
    public CoreDtos.CurrentUserResponse uploadAvatar(String principalEmail, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Файл не передан");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new ValidationException("Разрешены только изображения");
        }
        if (file.getSize() > 512 * 1024) {
            throw new ValidationException("Файл больше 512 КБ");
        }
        String ext = switch (ct) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/jpeg" -> ".jpg";
            default -> throw new ValidationException("Формат: PNG, JPEG или WebP");
        };
        AppUser user = currentUser(principalEmail);
        String key = user.getId() + ext;
        try {
            Files.createDirectories(avatarStorageDirectory);
            Path target = avatarStorageDirectory.resolve(key);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сохранить аватар", exception);
        }
        user.setAvatarStorageKey(key);
        AppUser saved = users.save(user);
        audit.record(user, "AVATAR_UPDATED", "AppUser", user.getId(), "{}");
        return toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> avatarResource(String principalEmail) {
        AppUser user = currentUser(principalEmail);
        String key = user.getAvatarStorageKey();
        if (key == null || key.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Path path = avatarStorageDirectory.resolve(key).normalize();
        if (!path.startsWith(avatarStorageDirectory) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        Resource body = new FileSystemResource(path);
        MediaType mediaType = key.endsWith(".png")
                ? MediaType.IMAGE_PNG
                : key.endsWith(".webp") ? MediaType.valueOf("image/webp") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .body(body);
    }

    @Transactional(readOnly = true)
    public CoreDtos.PageResponse<CoreDtos.AdminUserResponse> listUsers(String query, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        String normalizedQuery = query == null || query.isBlank() ? "" : query.trim();
        Page<CoreDtos.AdminUserResponse> result = users.search(normalizedQuery, PageRequest.of(safePage, safeSize))
                .map(this::toAdminUserResponse);
        return new CoreDtos.PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public CoreDtos.AdminUserResponse createUser(CoreDtos.CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ValidationException("Пользователь с таким email уже существует");
        }
        AppUser user = users.save(new AppUser(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                request.role()));
        audit.record(user, "ADMIN_USER_CREATED", "AppUser", user.getId(), "{}");
        return toAdminUserResponse(user);
    }

    @Transactional
    public CoreDtos.AdminUserResponse updateUser(UUID userId, CoreDtos.UpdateUserRequest request) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        String email = request.email() == null || request.email().isBlank()
                ? user.getEmail()
                : request.email().trim().toLowerCase(Locale.ROOT);
        if (!email.equals(user.getEmail())) {
            users.findByEmail(email).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new ValidationException("Пользователь с таким email уже существует");
                }
            });
        }
        String displayName = request.displayName() == null || request.displayName().isBlank()
                ? user.getDisplayName()
                : request.displayName().trim();
        Role role = request.role() == null ? user.getRole() : request.role();
        UserStatus status = request.status() == null ? user.getStatus() : request.status();
        user.updateAdminProfile(email, displayName, role, status);
        audit.record(user, "ADMIN_USER_UPDATED", "AppUser", user.getId(), "{}");
        return toAdminUserResponse(users.save(user));
    }

    @Transactional
    public void disableUser(UUID userId) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        user.disable();
        audit.record(user, "ADMIN_USER_DISABLED", "AppUser", user.getId(), "{}");
    }

    @Transactional
    public void disableUsers(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new ValidationException("Не выбраны пользователи");
        }
        userIds.forEach(this::disableUser);
    }

    @Transactional
    public void deleteUsers(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new ValidationException("Не выбраны пользователи");
        }
        for (UUID userId : userIds) {
            AppUser user = users.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
            audit.record(user, "ADMIN_USER_DELETED", "AppUser", user.getId(), "{}");
            users.delete(user);
        }
    }

    @Transactional(readOnly = true)
    public CoreDtos.OnlineUsersResponse onlineUsers() {
        OffsetDateTime now = OffsetDateTime.now();
        return new CoreDtos.OnlineUsersResponse(
                authSessions.countActiveUsers(now),
                authSessions.countActiveSessions(now));
    }

    @Transactional
    public CoreDtos.CourseResponse createCourse(CoreDtos.CreateCourseRequest request) {
        Course course = courses.findByTitle(request.title().trim())
                .map(existing -> {
                    existing.updateDetails(request.description(), CourseStatus.PUBLISHED);
                    return courses.save(existing);
                })
                .orElseGet(() -> courses.save(new Course(
                        request.title().trim(),
                        request.description(),
                        CourseStatus.PUBLISHED)));
        audit.record(null, "COURSE_UPSERTED", "Course", course.getId(), "{}");
        return toCourseResponse(course);
    }

    @Transactional
    public CoreDtos.ModuleResponse createModule(CoreDtos.CreateModuleRequest request) {
        Course course = courses.findById(request.courseId())
                .orElseThrow(() -> new NotFoundException("Курс не найден"));
        LearningModule module = modules.findByCourseIdAndTitle(course.getId(), request.title().trim())
                .map(existing -> {
                    existing.updateDetails(request.title().trim(), request.vulnerabilityTopic().trim(), ModuleStatus.ACTIVE);
                    return modules.save(existing);
                })
                .orElseGet(() -> modules.save(new LearningModule(
                        course,
                        request.title().trim(),
                        request.vulnerabilityTopic().trim(),
                        ModuleStatus.ACTIVE)));
        audit.record(null, "MODULE_UPSERTED", "LearningModule", module.getId(), "{}");
        return toModuleResponse(module);
    }

    @Transactional
    public CoreDtos.LessonResponse upsertLesson(CoreDtos.UpsertLessonRequest request) {
        LearningModule module = modules.findById(request.moduleId())
                .orElseThrow(() -> new NotFoundException("Модуль не найден"));
        Lesson lesson = lessons.findByModuleIdOrderByPositionAsc(module.getId()).stream()
                .filter(item -> item.getPosition().equals(request.position()))
                .findFirst()
                .orElseGet(() -> lessons.save(new Lesson(module, request.title(), request.contentMarkdown(), request.position())));
        lesson.updateContent(request.title(), request.contentMarkdown(), request.position());
        lesson = lessons.save(lesson);
        audit.record(null, "LESSON_UPSERTED", "Lesson", lesson.getId(), "{}");
        return toLessonResponse(lesson);
    }

    @Transactional
    public CoreDtos.LessonResponse updateLesson(UUID lessonId, CoreDtos.UpdateLessonRequest request) {
        Lesson lesson = lessons.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Урок не найден"));
        lesson.updateContent(request.title(), request.contentMarkdown(), request.position());
        lesson = lessons.save(lesson);
        audit.record(null, "LESSON_UPDATED", "Lesson", lesson.getId(), "{}");
        return toLessonResponse(lesson);
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
        return toLessonResponse(lesson);
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

    @Transactional
    public CoreDtos.SubmissionResponse createArchiveSubmission(
            String email,
            UUID moduleId,
            Integer applicationPort,
            String healthPath,
            String composeService,
            MultipartFile archive) {
        AppUser student = currentUser(email);
        if (student.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Только студент может отправить стенд");
        }
        if (applicationPort == null || applicationPort < 1 || applicationPort > 65535) {
            throw new ValidationException("Некорректный порт приложения");
        }
        if (archive == null || archive.isEmpty()) {
            throw new ValidationException("Архив стенда не выбран");
        }
        if (archive.getSize() > MAX_ARCHIVE_SIZE_BYTES) {
            throw new ValidationException("Архив стенда превышает лимит 50 MB");
        }
        LearningModule module = modules.findById(moduleId)
                .orElseThrow(() -> new NotFoundException("Модуль не найден"));
        String originalFilename = normalizeFilename(archive.getOriginalFilename());
        String archiveExtension = archiveExtensionOf(originalFilename);
        if (!ALLOWED_ARCHIVE_EXTENSIONS.contains(archiveExtension)) {
            throw new ValidationException("Разрешены только архивы .zip, .tar, .tar.gz и .tgz");
        }
        String storedFilename = UUID.randomUUID() + archiveExtension;
        Path destination = archiveStorageDirectory.resolve(storedFilename).normalize();
        try {
            Files.createDirectories(archiveStorageDirectory);
            try (InputStream input = archive.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new StorageException("Не удалось сохранить архив стенда", exception);
        }

        Submission submission = submissions.save(Submission.archive(
                module,
                student,
                originalFilename,
                destination.toString(),
                normalizeOptionalName(composeService),
                applicationPort,
                normalizeHealthPath(healthPath)));
        ValidationJob job = validationJobs.save(new ValidationJob(submission));
        audit.record(student, "SUBMISSION_ARCHIVE_UPLOADED", "Submission", submission.getId(),
                "{\"validationJobId\":\"" + job.getId() + "\",\"archiveFilename\":\"" + jsonEscape(originalFilename) + "\"}");
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
        if (job.getStatus() != ValidationJobStatus.QUEUED) {
            throw new ValidationException("Validation job уже обрабатывается или завершен");
        }
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
        validateReportScope(request, module, submission, assignment);
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
    public CoreDtos.ReportAttachmentResponse uploadReportAttachment(String email, UUID reportId, MultipartFile file) {
        AppUser user = currentUser(email);
        Report report = reports.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Отчет не найден"));
        assertCanUploadReportAttachment(user, report);
        if (file.isEmpty()) {
            throw new ValidationException("Файл вложения пустой");
        }

        String originalFilename = normalizeFilename(file.getOriginalFilename());
        String extension = extensionOf(originalFilename);
        String contentType = normalizeContentType(file.getContentType());
        validateAttachmentType(extension, contentType);
        String storedFilename = report.getId() + "-" + UUID.randomUUID() + extension;
        Path destination = attachmentStorageDirectory.resolve(storedFilename).normalize();

        try {
            Files.createDirectories(attachmentStorageDirectory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new StorageException("Не удалось сохранить вложение", exception);
        }

        ReportAttachment attachment = reportAttachments.save(new ReportAttachment(
                report,
                originalFilename,
                destination.toString(),
                contentType,
                file.getSize()));
        audit.record(user, "REPORT_ATTACHMENT_UPLOADED", "Report", report.getId(), "{\"attachmentId\":\"" + attachment.getId() + "\"}");
        return toReportAttachmentResponse(attachment);
    }

    @Transactional
    public AttachmentDownload downloadReportAttachment(String email, UUID attachmentId) {
        AppUser user = currentUser(email);
        ReportAttachment attachment = reportAttachments.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Вложение не найдено"));
        Report report = attachment.getReport();
        assertCanReadReport(user, report);

        Path storedPath = Path.of(attachment.getStoragePath()).toAbsolutePath().normalize();
        if (!storedPath.startsWith(attachmentStorageDirectory) || !Files.isRegularFile(storedPath)) {
            throw new NotFoundException("Файл вложения не найден");
        }
        audit.record(user, "REPORT_ATTACHMENT_DOWNLOADED", "ReportAttachment", attachment.getId(),
                "{\"reportId\":\"" + report.getId() + "\"}");
        return new AttachmentDownload(
                storedPath,
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes());
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
    public String exportModuleGradesCsv(String email, UUID moduleId) {
        AppUser actor = currentUser(email);
        if (actor.getRole() == Role.STUDENT) {
            throw new AccessDeniedException("Экспорт оценок доступен только куратору или администратору");
        }
        if (!modules.existsById(moduleId)) {
            throw new NotFoundException("Модуль не найден");
        }

        StringBuilder csv = new StringBuilder("studentEmail,displayName,dockerPassed,whiteBoxScore,blackBoxScore,finalScore,status\n");
        users.findAll().stream()
                .filter(user -> user.getRole() == Role.STUDENT)
                .forEach(studentItem -> {
                    ModuleResult result = calculateModuleResult(studentItem, moduleId);
                    csv.append(csvValue(studentItem.getEmail())).append(',')
                            .append(csvValue(studentItem.getDisplayName())).append(',')
                            .append(result.dockerPassed()).append(',')
                            .append(nullableCsvValue(result.whiteBoxScore())).append(',')
                            .append(nullableCsvValue(result.blackBoxScore())).append(',')
                            .append(nullableCsvValue(result.finalScore())).append(',')
                            .append(csvValue(result.status()))
                            .append('\n');
                });
        return csv.toString();
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
                        publicLabUrl(submission.getId()))));
        submission.setLabUrls(publicLabUrl(submission.getId()), localHostLabUrl(submission.getId()));
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
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .filter(user -> streams.canStudentAccessModule(user, moduleId)
                        || streams.activeStreamIds(user).isEmpty())
                .toList();
        int created = 0;
        for (AppUser student : students) {
            long assignedTargets = assignments.countByModuleAndStudent(module, student);
            if (assignedTargets >= MAX_BLACK_BOX_TARGETS_PER_STUDENT) {
                continue;
            }
            for (LabInstance lab : moduleLabs) {
                if (lab.getSubmission().getStudent().getId().equals(student.getId())) {
                    continue;
                }
                if (assignments.existsByModuleAndStudentAndTargetLabInstance(module, student, lab)) {
                    continue;
                }
                assignments.save(new BlackBoxAssignment(module, student, lab));
                created++;
                assignedTargets++;
                if (assignedTargets >= MAX_BLACK_BOX_TARGETS_PER_STUDENT) {
                    break;
                }
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

    @Transactional(readOnly = true)
    public CoreDtos.LiveStatusResponse liveStatus(String email) {
        AppUser user = currentUser(email);
        int submissionCount = user.getRole() == Role.STUDENT ? submissions.findByStudent(user).size() : submissions.findAll().size();
        int validationJobCount = validationJobs.findAll().stream()
                .filter(job -> user.getRole() != Role.STUDENT || job.getSubmission().getStudent().getId().equals(user.getId()))
                .toList()
                .size();
        int reportCount = user.getRole() == Role.STUDENT ? reports.findByAuthor(user).size() : reports.findAll().size();
        int assignmentCount = user.getRole() == Role.STUDENT ? assignments.findByStudent(user).size() : 0;
        int runningLabCount = user.getRole() == Role.STUDENT
                ? 0
                : (int) labs.findAll().stream().filter(lab -> lab.getStatus() == LabStatus.RUNNING).count();
        return new CoreDtos.LiveStatusResponse(
                user.getRole().name(),
                submissionCount,
                validationJobCount,
                runningLabCount,
                reportCount,
                assignmentCount,
                OffsetDateTime.now());
    }

    private void validateReportScope(
            CoreDtos.CreateReportRequest request,
            LearningModule module,
            Submission submission,
            BlackBoxAssignment assignment) {
        if (request.type() == ReportType.WHITE_BOX) {
            if (submission == null || assignment != null) {
                throw new ValidationException("White box отчет должен ссылаться только на свою отправку image");
            }
            if (!submission.getModule().getId().equals(module.getId())) {
                throw new ValidationException("Модуль отчета не совпадает с модулем отправки");
            }
        } else if (request.type() == ReportType.BLACK_BOX) {
            if (assignment == null || submission != null) {
                throw new ValidationException("Black box отчет должен ссылаться только на назначенную цель");
            }
            if (!assignment.getModule().getId().equals(module.getId())) {
                throw new ValidationException("Модуль отчета не совпадает с модулем black box задания");
            }
        }
    }

    private AppUser currentUser(String email) {
        return users.findByEmail(email).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private void assertCanReadSubmission(AppUser user, Submission submission) {
        if (user.getRole() == Role.STUDENT && !submission.getStudent().getId().equals(user.getId())) {
            throw new AccessDeniedException("Нет доступа к чужой работе");
        }
    }

    private void assertCanReadReport(AppUser user, Report report) {
        if (user.getRole() == Role.STUDENT && !report.getAuthor().getId().equals(user.getId())) {
            throw new AccessDeniedException("Нет доступа к чужому отчету");
        }
    }

    private void assertCanUploadReportAttachment(AppUser user, Report report) {
        if (!report.getAuthor().getId().equals(user.getId())) {
            throw new AccessDeniedException("Вложения может добавлять только автор отчета");
        }
    }

    private String normalizeHealthPath(String healthPath) {
        if (healthPath == null || healthPath.isBlank()) {
            return "/health";
        }
        return healthPath.startsWith("/") ? healthPath : "/" + healthPath;
    }

    private String normalizeFilename(String filename) {
        String normalized = filename == null || filename.isBlank() ? "attachment" : Path.of(filename).getFileName().toString();
        return normalized.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String normalizeOptionalName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replaceAll("[^A-Za-z0-9._-]", "-");
        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
    }

    private String archiveExtensionOf(String filename) {
        String normalized = filename.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".tar.gz")) {
            return ".tar.gz";
        }
        if (normalized.endsWith(".tgz")) {
            return ".tgz";
        }
        return extensionOf(normalized);
    }

    private String extensionOf(String filename) {
        int extensionStart = filename.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == filename.length() - 1) {
            return "";
        }
        return filename.substring(extensionStart).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    }

    private void validateAttachmentType(String extension, String contentType) {
        if (!ALLOWED_ATTACHMENT_EXTENSIONS.contains(extension) || !ALLOWED_ATTACHMENT_CONTENT_TYPES.contains(contentType)) {
            throw new ValidationException("Тип файла вложения не разрешен");
        }
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private String publicLabUrl(UUID submissionId) {
        return "http://lab-" + shortId(submissionId) + ".127.0.0.1.nip.io:8088";
    }

    private String localHostLabUrl(UUID submissionId) {
        return "http://lab-" + shortId(submissionId) + ".local.host";
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Integer bestScore(List<Review> moduleReviews, ReportType reportType) {
        return moduleReviews.stream()
                .filter(review -> review.getReport().getType() == reportType)
                .filter(review -> review.getDecision() == ReviewDecision.APPROVED)
                .map(Review::getScore)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private ModuleResult calculateModuleResult(AppUser student, UUID moduleId) {
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
        return new ModuleResult(dockerPassed, whiteBoxScore, blackBoxScore, finalScore, status);
    }

    private String nullableCsvValue(Integer value) {
        return value == null ? "" : value.toString();
    }

    private String csvValue(String value) {
        String escaped = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private record ModuleResult(
            boolean dockerPassed,
            Integer whiteBoxScore,
            Integer blackBoxScore,
            Integer finalScore,
            String status) {
    }

    private CoreDtos.CourseResponse toCourseResponse(Course course) {
        return toCourseResponse(course, null);
    }

    private CoreDtos.CourseResponse toCourseResponse(Course course, AppUser viewerForSchedule) {
        return new CoreDtos.CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getStatus(),
                course.getModules().stream()
                        .map(module -> toModuleResponse(module, viewerForSchedule))
                        .toList());
    }

    private CoreDtos.ModuleResponse toModuleResponse(LearningModule module) {
        return toModuleResponse(module, null);
    }

    private CoreDtos.ModuleResponse toModuleResponse(LearningModule module, AppUser viewerForSchedule) {
        StudentStreamService.EffectiveSchedule schedule = viewerForSchedule == null
                ? StudentStreamService.EffectiveSchedule.EMPTY
                : streams.effectiveScheduleFor(viewerForSchedule, module.getId());
        return new CoreDtos.ModuleResponse(
                module.getId(),
                module.getTitle(),
                module.getVulnerabilityTopic(),
                module.getStatus(),
                schedule.startsAt(),
                schedule.submissionDeadline(),
                schedule.blackBoxStartsAt(),
                schedule.blackBoxDeadline());
    }

    private CoreDtos.LessonResponse toLessonResponse(Lesson lesson) {
        return new CoreDtos.LessonResponse(
                lesson.getId(),
                lesson.getModule().getId(),
                lesson.getTitle(),
                lesson.getContentMarkdown(),
                lesson.getPosition());
    }

    private CoreDtos.AdminUserResponse toAdminUserResponse(AppUser user) {
        return new CoreDtos.AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus());
    }

    private CoreDtos.SubmissionResponse toSubmissionResponse(Submission submission) {
        return new CoreDtos.SubmissionResponse(
                submission.getId(),
                submission.getModule().getId(),
                submission.getStudent().getEmail(),
                submission.getImageReference(),
                submission.getSourceType(),
                submission.getArchiveFilename(),
                submission.getComposeService(),
                submission.getBuildContext(),
                submission.getRuntimeImageReference(),
                submission.getPublicUrl(),
                submission.getLocalHostUrl(),
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
                job.getErrorMessage(),
                job.getImageScanStatus(),
                job.getImageScanSummary(),
                job.getImageScanReport(),
                job.getDependencyScanStatus(),
                job.getDependencyScanSummary(),
                job.getDependencyScanReport());
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
                report.getStatus(),
                reportAttachments.findByReportOrderByUploadedAtAsc(report).stream()
                        .map(this::toReportAttachmentResponse)
                        .toList());
    }

    private CoreDtos.ReportAttachmentResponse toReportAttachmentResponse(ReportAttachment attachment) {
        return new CoreDtos.ReportAttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getUploadedAt());
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
        Submission submission = lab.getSubmission();
        String ingressUrl = publicLabUrl(submission.getId());
        String deployCommand = "docker compose exec k8s-toolbox pep-lab-deploy "
                + submission.getId() + " " + submission.getRuntimeImageReference() + " " + submission.getApplicationPort();
        String ingressInstallCommand = "docker compose exec k8s-toolbox pep-ingress-install";
        String portForwardCommand = "docker compose exec k8s-toolbox pep-lab-forward "
                + submission.getId() + " " + submission.getApplicationPort() + " 18080";
        return new CoreDtos.LabResponse(
                lab.getId(),
                submission.getId(),
                submission.getStudent().getEmail(),
                submission.getImageReference(),
                submission.getSourceType(),
                submission.getRuntimeImageReference(),
                publicLabUrl(submission.getId()),
                localHostLabUrl(submission.getId()),
                lab.getNamespace(),
                lab.getDeploymentName(),
                lab.getServiceName(),
                lab.getRouteUrl(),
                ingressUrl,
                deployCommand,
                ingressInstallCommand,
                portForwardCommand,
                lab.getStatus(),
                lab.getExpiresAt());
    }

    private CoreDtos.BlackBoxAssignmentResponse toAssignmentResponse(BlackBoxAssignment assignment) {
        LabInstance lab = assignment.getTargetLabInstance();
        return new CoreDtos.BlackBoxAssignmentResponse(
                assignment.getId(),
                assignment.getModule().getId(),
                lab.getId(),
                lab.getSubmission().getPublicUrl() == null ? lab.getRouteUrl() : lab.getSubmission().getPublicUrl(),
                lab.getSubmission().getRuntimeImageReference(),
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

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public record AttachmentDownload(Path path, String filename, String contentType, Long sizeBytes) {
    }
}
