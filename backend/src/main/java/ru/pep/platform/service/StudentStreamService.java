package ru.pep.platform.service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pep.platform.api.CoreDtos;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.Course;
import ru.pep.platform.domain.LearningModule;
import ru.pep.platform.domain.Role;
import ru.pep.platform.domain.StudentStream;
import ru.pep.platform.domain.StudentStreamCourse;
import ru.pep.platform.domain.StudentStreamMembership;
import ru.pep.platform.domain.StudentStreamMembershipStatus;
import ru.pep.platform.domain.StudentStreamModuleSchedule;
import ru.pep.platform.domain.StudentStreamStatus;
import ru.pep.platform.repository.AppUserRepository;
import ru.pep.platform.repository.CourseRepository;
import ru.pep.platform.repository.LearningModuleRepository;
import ru.pep.platform.repository.StudentStreamCourseRepository;
import ru.pep.platform.repository.StudentStreamMembershipRepository;
import ru.pep.platform.repository.StudentStreamModuleScheduleRepository;
import ru.pep.platform.repository.StudentStreamRepository;

@Service
public class StudentStreamService {

    private final StudentStreamRepository streams;
    private final StudentStreamCourseRepository streamCourses;
    private final StudentStreamMembershipRepository memberships;
    private final StudentStreamModuleScheduleRepository schedules;
    private final CourseRepository courses;
    private final LearningModuleRepository modules;
    private final AppUserRepository users;
    private final AuditService audit;

    public StudentStreamService(
            StudentStreamRepository streams,
            StudentStreamCourseRepository streamCourses,
            StudentStreamMembershipRepository memberships,
            StudentStreamModuleScheduleRepository schedules,
            CourseRepository courses,
            LearningModuleRepository modules,
            AppUserRepository users,
            AuditService audit) {
        this.streams = streams;
        this.streamCourses = streamCourses;
        this.memberships = memberships;
        this.schedules = schedules;
        this.courses = courses;
        this.modules = modules;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public CoreDtos.StudentStreamResponse createStream(String actorEmail, CoreDtos.CreateStudentStreamRequest request) {
        AppUser actor = curatorOrAdmin(actorEmail);
        String name = request.name().trim();
        if (streams.findByName(name).isPresent()) {
            throw new CorePlatformService.ValidationException(
                    "Поток с таким названием уже существует");
        }
        StudentStream stream = streams.save(new StudentStream(
                name,
                request.description() == null ? null : request.description().trim(),
                request.status() == null ? StudentStreamStatus.ACTIVE : request.status(),
                actor));
        audit.record(actor, "STUDENT_STREAM_CREATED", "StudentStream", stream.getId(), "{}");
        return toStreamResponse(stream);
    }

    @Transactional
    public CoreDtos.StudentStreamResponse updateStream(
            String actorEmail, UUID streamId, CoreDtos.UpdateStudentStreamRequest request) {
        AppUser actor = curatorOrAdmin(actorEmail);
        StudentStream stream = streamOrThrow(streamId);
        String name = request.name() == null ? stream.getName() : request.name().trim();
        if (!name.equals(stream.getName())) {
            streams.findByName(name).ifPresent(existing -> {
                if (!existing.getId().equals(stream.getId())) {
                    throw new CorePlatformService.ValidationException(
                            "Поток с таким названием уже существует");
                }
            });
        }
        stream.updateDetails(name, request.description(), request.status());
        streams.save(stream);
        audit.record(actor, "STUDENT_STREAM_UPDATED", "StudentStream", stream.getId(), "{}");
        return toStreamResponse(stream);
    }

    @Transactional
    public void deleteStream(String actorEmail, UUID streamId) {
        AppUser actor = curatorOrAdmin(actorEmail);
        StudentStream stream = streamOrThrow(streamId);
        if (memberships.countByStream(stream) > 0) {
            throw new CorePlatformService.ValidationException(
                    "Нельзя удалить поток с активными участниками");
        }
        streams.delete(stream);
        audit.record(actor, "STUDENT_STREAM_DELETED", "StudentStream", streamId, "{}");
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.StudentStreamSummaryResponse> listStreams(String actorEmail) {
        curatorOrAdmin(actorEmail);
        return streams.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public CoreDtos.PageResponse<CoreDtos.StudentStreamSummaryResponse> listStreams(
            String actorEmail, String query, int page, int size) {
        curatorOrAdmin(actorEmail);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        String normalizedQuery = query == null || query.isBlank() ? "" : query.trim();
        Page<CoreDtos.StudentStreamSummaryResponse> result =
                streams.search(normalizedQuery, PageRequest.of(safePage, safeSize)).map(this::toSummary);
        return new CoreDtos.PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CoreDtos.StudentStreamResponse getStream(String actorEmail, UUID streamId) {
        curatorOrAdmin(actorEmail);
        return toStreamResponse(streamOrThrow(streamId));
    }

    @Transactional
    public CoreDtos.StudentStreamResponse addCourse(
            String actorEmail, UUID streamId, CoreDtos.AddStreamCourseRequest request) {
        AppUser actor = curatorOrAdmin(actorEmail);
        StudentStream stream = streamOrThrow(streamId);
        Course course = courses.findById(request.courseId())
                .orElseThrow(() -> new CorePlatformService.NotFoundException("Курс не найден"));
        streamCourses.findByStreamAndCourse(stream, course).ifPresent(existing -> {
            throw new CorePlatformService.ValidationException("Курс уже привязан к потоку");
        });
        int position = request.position() == null ? nextCoursePosition(stream) : request.position();
        streamCourses.save(new StudentStreamCourse(stream, course, position));
        audit.record(actor, "STUDENT_STREAM_COURSE_ADDED", "StudentStream", stream.getId(),
                "{\"courseId\":\"" + course.getId() + "\"}");
        return toStreamResponse(stream);
    }

    @Transactional
    public CoreDtos.StudentStreamResponse removeCourse(String actorEmail, UUID streamId, UUID courseId) {
        AppUser actor = curatorOrAdmin(actorEmail);
        StudentStream stream = streamOrThrow(streamId);
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new CorePlatformService.NotFoundException("Курс не найден"));
        StudentStreamCourse link = streamCourses.findByStreamAndCourse(stream, course)
                .orElseThrow(() -> new CorePlatformService.NotFoundException(
                        "Курс не привязан к потоку"));
        streamCourses.delete(link);
        audit.record(actor, "STUDENT_STREAM_COURSE_REMOVED", "StudentStream", stream.getId(),
                "{\"courseId\":\"" + course.getId() + "\"}");
        return toStreamResponse(stream);
    }

    @Transactional
    public CoreDtos.StudentStreamResponse upsertSchedule(
            String actorEmail,
            UUID streamId,
            UUID moduleId,
            CoreDtos.UpsertStreamScheduleRequest request) {
        AppUser actor = curatorOrAdmin(actorEmail);
        StudentStream stream = streamOrThrow(streamId);
        LearningModule module = modules.findById(moduleId)
                .orElseThrow(() -> new CorePlatformService.NotFoundException("Модуль не найден"));
        Course course = module.getCourse();
        if (course == null || streamCourses.findByStreamAndCourse(stream, course).isEmpty()) {
            throw new CorePlatformService.ValidationException(
                    "Модуль принадлежит курсу, который не привязан к потоку");
        }
        StudentStreamModuleSchedule schedule = schedules.findByStreamAndModule(stream, module)
                .orElseGet(() -> new StudentStreamModuleSchedule(stream, module));
        schedule.applySchedule(
                request.startsAt(),
                request.submissionDeadline(),
                request.blackBoxStartsAt(),
                request.blackBoxDeadline());
        schedules.save(schedule);
        audit.record(actor, "STUDENT_STREAM_SCHEDULE_UPSERTED", "StudentStream", stream.getId(),
                "{\"moduleId\":\"" + module.getId() + "\"}");
        return toStreamResponse(stream);
    }

    @Transactional
    public CoreDtos.StudentStreamResponse addMembers(
            String actorEmail, UUID streamId, CoreDtos.AddStreamMembersRequest request) {
        AppUser actor = curatorOrAdmin(actorEmail);
        StudentStream stream = streamOrThrow(streamId);
        if (request.userIds() == null || request.userIds().isEmpty()) {
            throw new CorePlatformService.ValidationException("Не указаны пользователи для зачисления");
        }
        for (UUID userId : request.userIds()) {
            AppUser member = users.findById(userId)
                    .orElseThrow(() -> new CorePlatformService.NotFoundException(
                            "Пользователь не найден: " + userId));
            if (member.getRole() != Role.STUDENT && member.getRole() != Role.CURATOR) {
                throw new CorePlatformService.ValidationException(
                        "В поток можно зачислять только студентов и кураторов: " + member.getEmail());
            }
            Optional<StudentStreamMembership> existing = memberships.findByStreamAndUser(stream, member);
            if (existing.isPresent()) {
                StudentStreamMembership membership = existing.get();
                if (membership.getStatus() != StudentStreamMembershipStatus.ACTIVE) {
                    membership.activate();
                    memberships.save(membership);
                }
            } else {
                memberships.save(new StudentStreamMembership(stream, member));
            }
        }
        audit.record(actor, "STUDENT_STREAM_MEMBERS_ADDED", "StudentStream", stream.getId(),
                "{\"count\":" + request.userIds().size() + "}");
        return toStreamResponse(stream);
    }

    @Transactional
    public CoreDtos.StudentStreamResponse removeMember(String actorEmail, UUID streamId, UUID userId) {
        AppUser actor = curatorOrAdmin(actorEmail);
        StudentStream stream = streamOrThrow(streamId);
        AppUser student = users.findById(userId)
                .orElseThrow(() -> new CorePlatformService.NotFoundException("Пользователь не найден"));
        StudentStreamMembership membership = memberships.findByStreamAndUser(stream, student)
                .orElseThrow(() -> new CorePlatformService.NotFoundException(
                        "Студент не состоит в потоке"));
        membership.remove();
        memberships.save(membership);
        audit.record(actor, "STUDENT_STREAM_MEMBER_REMOVED", "StudentStream", stream.getId(),
                "{\"userId\":\"" + userId + "\"}");
        return toStreamResponse(stream);
    }

    @Transactional(readOnly = true)
    public List<CoreDtos.StudentStreamSummaryResponse> listActiveStreamsForStudent(String studentEmail) {
        AppUser student = users.findByEmail(studentEmail)
                .orElseThrow(() -> new CorePlatformService.NotFoundException("Пользователь не найден"));
        return activeStreamsForUser(student).stream()
                .map(this::toSummary)
                .toList();
    }

    // ---------- Helpers for other services ----------

    /** Returns the set of active stream ids where the given student is an ACTIVE member. */
    @Transactional(readOnly = true)
    public Set<UUID> activeStreamIds(AppUser student) {
        if (student == null) {
            return Set.of();
        }
        return new HashSet<>(memberships.findActiveStreamIdsForUser(
                student.getId(), StudentStreamMembershipStatus.ACTIVE));
    }

    /** Courses that the student can access through at least one active stream. */
    @Transactional(readOnly = true)
    public Set<UUID> accessibleCourseIds(AppUser student) {
        Set<UUID> streamIds = activeStreamIds(student);
        if (streamIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(streamCourses.findCourseIdsForStreams(streamIds));
    }

    /** Module ids that belong to any accessible course for the student. */
    @Transactional(readOnly = true)
    public Set<UUID> accessibleModuleIds(AppUser student) {
        Set<UUID> courseIds = accessibleCourseIds(student);
        if (courseIds.isEmpty()) {
            return Set.of();
        }
        Set<UUID> moduleIds = new HashSet<>();
        for (UUID courseId : courseIds) {
            courses.findById(courseId).ifPresent(course -> {
                for (LearningModule module : course.getModules()) {
                    moduleIds.add(module.getId());
                }
            });
        }
        return moduleIds;
    }

    /** Whether the student can access the given module via an active stream. */
    @Transactional(readOnly = true)
    public boolean canStudentAccessModule(AppUser student, UUID moduleId) {
        if (student == null || moduleId == null) {
            return false;
        }
        return accessibleModuleIds(student).contains(moduleId);
    }

    /** Whether two users share at least one active stream. Curators/admins are treated as sharing. */
    @Transactional(readOnly = true)
    public boolean shareActiveStream(AppUser a, AppUser b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getRole() != Role.STUDENT || b.getRole() != Role.STUDENT) {
            return true;
        }
        Set<UUID> aStreams = activeStreamIds(a);
        if (aStreams.isEmpty()) {
            return false;
        }
        Set<UUID> bStreams = activeStreamIds(b);
        for (UUID id : aStreams) {
            if (bStreams.contains(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns ids of STUDENT users who share at least one active stream
     * with the given actor AND whose shared streams include the module.
     */
    @Transactional(readOnly = true)
    public Set<UUID> peerUserIdsForModule(AppUser actor, UUID moduleId) {
        if (actor == null || moduleId == null) {
            return Set.of();
        }
        Set<UUID> actorStreams = activeStreamIds(actor);
        if (actorStreams.isEmpty()) {
            return Set.of();
        }
        LearningModule module = modules.findById(moduleId).orElse(null);
        if (module == null || module.getCourse() == null) {
            return Set.of();
        }
        UUID courseId = module.getCourse().getId();
        Set<UUID> streamsWithCourse = new HashSet<>(streamCourses.findStreamIdsForCourse(courseId));
        Set<UUID> relevant = new HashSet<>(actorStreams);
        relevant.retainAll(streamsWithCourse);
        if (relevant.isEmpty()) {
            return Set.of();
        }
        Set<UUID> memberIds = new HashSet<>(memberships.findMemberIdsForStreams(
                relevant, StudentStreamMembershipStatus.ACTIVE));
        memberIds.remove(actor.getId());
        return memberIds;
    }

    /**
     * Returns the effective schedule for the module given the student's active streams.
     * If there are multiple overrides, we pick the earliest {@code startsAt} and the latest
     * {@code submissionDeadline} (most permissive window).
     */
    @Transactional(readOnly = true)
    public EffectiveSchedule effectiveScheduleFor(AppUser student, UUID moduleId) {
        if (student == null || moduleId == null) {
            return EffectiveSchedule.EMPTY;
        }
        Set<UUID> streamIds = activeStreamIds(student);
        if (streamIds.isEmpty()) {
            return EffectiveSchedule.EMPTY;
        }
        List<StudentStreamModuleSchedule> overrides =
                schedules.findByStreamIdInAndModuleId(streamIds, moduleId);
        if (overrides.isEmpty()) {
            return EffectiveSchedule.EMPTY;
        }
        OffsetDateTime startsAt = null;
        OffsetDateTime submissionDeadline = null;
        OffsetDateTime blackBoxStartsAt = null;
        OffsetDateTime blackBoxDeadline = null;
        for (StudentStreamModuleSchedule override : overrides) {
            startsAt = earliest(startsAt, override.getStartsAt());
            submissionDeadline = latest(submissionDeadline, override.getSubmissionDeadline());
            blackBoxStartsAt = earliest(blackBoxStartsAt, override.getBlackBoxStartsAt());
            blackBoxDeadline = latest(blackBoxDeadline, override.getBlackBoxDeadline());
        }
        return new EffectiveSchedule(startsAt, submissionDeadline, blackBoxStartsAt, blackBoxDeadline);
    }

    private List<StudentStream> activeStreamsForUser(AppUser user) {
        return memberships
                .findByUserAndStatus(user, StudentStreamMembershipStatus.ACTIVE)
                .stream()
                .map(StudentStreamMembership::getStream)
                .filter(stream -> stream.getStatus() == StudentStreamStatus.ACTIVE)
                .sorted(Comparator.comparing(StudentStream::getCreatedAt))
                .toList();
    }

    private int nextCoursePosition(StudentStream stream) {
        return streamCourses.findByStreamOrderByPositionAscCreatedAtAsc(stream).stream()
                .mapToInt(link -> link.getPosition() == null ? 0 : link.getPosition())
                .max()
                .orElse(0) + 1;
    }

    private CoreDtos.StudentStreamSummaryResponse toSummary(StudentStream stream) {
        long courseCount = streamCourses.findByStreamOrderByPositionAscCreatedAtAsc(stream).size();
        long memberCount = memberships
                .findByStreamAndStatus(stream, StudentStreamMembershipStatus.ACTIVE)
                .size();
        return new CoreDtos.StudentStreamSummaryResponse(
                stream.getId(),
                stream.getName(),
                stream.getDescription(),
                stream.getStatus(),
                (int) courseCount,
                (int) memberCount,
                stream.getCreatedAt(),
                stream.getUpdatedAt());
    }

    private CoreDtos.StudentStreamResponse toStreamResponse(StudentStream stream) {
        List<CoreDtos.StudentStreamCourseResponse> courseResponses = streamCourses
                .findByStreamOrderByPositionAscCreatedAtAsc(stream)
                .stream()
                .map(link -> new CoreDtos.StudentStreamCourseResponse(
                        link.getCourse().getId(),
                        link.getCourse().getTitle(),
                        link.getCourse().getStatus(),
                        link.getPosition()))
                .toList();
        List<CoreDtos.StudentStreamMemberResponse> memberResponses = memberships.findByStream(stream)
                .stream()
                .sorted(Comparator.comparing(StudentStreamMembership::getEnrolledAt))
                .map(m -> new CoreDtos.StudentStreamMemberResponse(
                        m.getUser().getId(),
                        m.getUser().getEmail(),
                        m.getUser().getDisplayName(),
                        m.getUser().getRole(),
                        m.getStatus(),
                        m.getEnrolledAt()))
                .toList();
        List<CoreDtos.StudentStreamModuleScheduleResponse> scheduleResponses = schedules.findByStream(stream)
                .stream()
                .sorted(Comparator.comparing(s -> s.getModule().getTitle()))
                .map(s -> new CoreDtos.StudentStreamModuleScheduleResponse(
                        s.getModule().getId(),
                        s.getModule().getTitle(),
                        s.getStartsAt(),
                        s.getSubmissionDeadline(),
                        s.getBlackBoxStartsAt(),
                        s.getBlackBoxDeadline()))
                .toList();
        return new CoreDtos.StudentStreamResponse(
                stream.getId(),
                stream.getName(),
                stream.getDescription(),
                stream.getStatus(),
                stream.getCreatedAt(),
                stream.getUpdatedAt(),
                courseResponses,
                memberResponses,
                scheduleResponses);
    }

    private StudentStream streamOrThrow(UUID id) {
        return streams.findById(id)
                .orElseThrow(() -> new CorePlatformService.NotFoundException("Поток не найден"));
    }

    private AppUser curatorOrAdmin(String email) {
        AppUser actor = users.findByEmail(email)
                .orElseThrow(() -> new CorePlatformService.NotFoundException("Пользователь не найден"));
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.CURATOR) {
            throw new CorePlatformService.AccessDeniedException(
                    "Управлять потоками может только куратор или администратор");
        }
        return actor;
    }

    private static OffsetDateTime earliest(OffsetDateTime current, OffsetDateTime candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.isBefore(current)) {
            return candidate;
        }
        return current;
    }

    private static OffsetDateTime latest(OffsetDateTime current, OffsetDateTime candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.isAfter(current)) {
            return candidate;
        }
        return current;
    }

    public record EffectiveSchedule(
            OffsetDateTime startsAt,
            OffsetDateTime submissionDeadline,
            OffsetDateTime blackBoxStartsAt,
            OffsetDateTime blackBoxDeadline) {
        public static final EffectiveSchedule EMPTY = new EffectiveSchedule(null, null, null, null);

        public boolean isEmpty() {
            return startsAt == null
                    && submissionDeadline == null
                    && blackBoxStartsAt == null
                    && blackBoxDeadline == null;
        }
    }
}
