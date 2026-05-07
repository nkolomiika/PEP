package ru.pep.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pep.platform.api.CoreDtos;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.Course;
import ru.pep.platform.domain.LearningModule;
import ru.pep.platform.domain.ModuleStatus;
import ru.pep.platform.domain.Role;
import ru.pep.platform.domain.StudentStream;
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

class StudentStreamServiceTest {

    private StudentStreamRepository streams;
    private StudentStreamCourseRepository streamCourses;
    private StudentStreamMembershipRepository memberships;
    private StudentStreamModuleScheduleRepository schedules;
    private CourseRepository courses;
    private LearningModuleRepository modules;
    private AppUserRepository users;
    private AuditService audit;
    private StudentStreamService service;

    @BeforeEach
    void setUp() {
        streams = mock(StudentStreamRepository.class);
        streamCourses = mock(StudentStreamCourseRepository.class);
        memberships = mock(StudentStreamMembershipRepository.class);
        schedules = mock(StudentStreamModuleScheduleRepository.class);
        courses = mock(CourseRepository.class);
        modules = mock(LearningModuleRepository.class);
        users = mock(AppUserRepository.class);
        audit = mock(AuditService.class);
        service = new StudentStreamService(
                streams, streamCourses, memberships, schedules, courses, modules, users, audit);
    }

    @Test
    void createStreamRequiresCuratorOrAdmin() {
        AppUser student = newStudent("s1@pep.local");
        when(users.findByEmail("s1@pep.local")).thenReturn(Optional.of(student));
        assertThatThrownBy(() ->
                service.createStream("s1@pep.local",
                        new CoreDtos.CreateStudentStreamRequest("demo", "desc", StudentStreamStatus.ACTIVE)))
                .isInstanceOf(CorePlatformService.AccessDeniedException.class);
    }

    @Test
    void createStreamRejectsDuplicateName() {
        AppUser admin = newUser("admin@pep.local", Role.ADMIN);
        when(users.findByEmail("admin@pep.local")).thenReturn(Optional.of(admin));
        when(streams.findByName("demo")).thenReturn(Optional.of(streamWithName("demo")));
        assertThatThrownBy(() ->
                service.createStream("admin@pep.local",
                        new CoreDtos.CreateStudentStreamRequest("demo", null, StudentStreamStatus.ACTIVE)))
                .isInstanceOf(CorePlatformService.ValidationException.class);
    }

    @Test
    void activeStreamIdsReturnsEmptyForNull() {
        assertThat(service.activeStreamIds(null)).isEmpty();
    }

    @Test
    void activeStreamIdsUsesRepository() {
        AppUser student = newStudent("s@pep.local");
        UUID stream1 = UUID.randomUUID();
        UUID stream2 = UUID.randomUUID();
        when(memberships.findActiveStreamIdsForUser(eq(student.getId()),
                eq(StudentStreamMembershipStatus.ACTIVE)))
                .thenReturn(List.of(stream1, stream2));
        assertThat(service.activeStreamIds(student)).containsExactlyInAnyOrder(stream1, stream2);
    }

    @Test
    void accessibleCourseIdsEmptyWhenNoStreams() {
        AppUser student = newStudent("s@pep.local");
        when(memberships.findActiveStreamIdsForUser(eq(student.getId()),
                eq(StudentStreamMembershipStatus.ACTIVE))).thenReturn(List.of());
        assertThat(service.accessibleCourseIds(student)).isEmpty();
    }

    @Test
    void accessibleCourseIdsUnionsStreams() {
        AppUser student = newStudent("s@pep.local");
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        when(memberships.findActiveStreamIdsForUser(eq(student.getId()),
                eq(StudentStreamMembershipStatus.ACTIVE))).thenReturn(List.of(s1, s2));
        when(streamCourses.findCourseIdsForStreams(anyCollection())).thenReturn(List.of(c1, c2));
        assertThat(service.accessibleCourseIds(student)).containsExactlyInAnyOrder(c1, c2);
    }

    @Test
    void shareActiveStreamAlwaysTrueForAdmin() {
        AppUser admin = newUser("a@pep.local", Role.ADMIN);
        AppUser student = newStudent("s@pep.local");
        assertThat(service.shareActiveStream(admin, student)).isTrue();
    }

    @Test
    void shareActiveStreamDetectsOverlap() {
        AppUser a = newStudent("a@pep.local");
        AppUser b = newStudent("b@pep.local");
        UUID stream = UUID.randomUUID();
        when(memberships.findActiveStreamIdsForUser(eq(a.getId()),
                eq(StudentStreamMembershipStatus.ACTIVE))).thenReturn(List.of(stream));
        when(memberships.findActiveStreamIdsForUser(eq(b.getId()),
                eq(StudentStreamMembershipStatus.ACTIVE))).thenReturn(List.of(stream));
        assertThat(service.shareActiveStream(a, b)).isTrue();
    }

    @Test
    void shareActiveStreamFalseWhenDifferentStreams() {
        AppUser a = newStudent("a@pep.local");
        AppUser b = newStudent("b@pep.local");
        when(memberships.findActiveStreamIdsForUser(eq(a.getId()),
                eq(StudentStreamMembershipStatus.ACTIVE))).thenReturn(List.of(UUID.randomUUID()));
        when(memberships.findActiveStreamIdsForUser(eq(b.getId()),
                eq(StudentStreamMembershipStatus.ACTIVE))).thenReturn(List.of(UUID.randomUUID()));
        assertThat(service.shareActiveStream(a, b)).isFalse();
    }

    @Test
    void peerUserIdsFiltersByStreamAndCourse() {
        AppUser actor = newStudent("a@pep.local");
        AppUser peerSame = newStudent("b@pep.local");
        UUID streamShared = UUID.randomUUID();
        UUID streamOther = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        Course course = newCourse(courseId);
        LearningModule module = newModule(moduleId, course);
        when(modules.findById(moduleId)).thenReturn(Optional.of(module));
        when(memberships.findActiveStreamIdsForUser(eq(actor.getId()),
                eq(StudentStreamMembershipStatus.ACTIVE))).thenReturn(List.of(streamShared, streamOther));
        when(streamCourses.findStreamIdsForCourse(courseId)).thenReturn(List.of(streamShared));
        when(memberships.findMemberIdsForStreams(anyCollection(),
                eq(StudentStreamMembershipStatus.ACTIVE)))
                .thenReturn(List.of(actor.getId(), peerSame.getId()));
        Set<UUID> peers = service.peerUserIdsForModule(actor, moduleId);
        assertThat(peers).containsExactly(peerSame.getId()).doesNotContain(actor.getId());
    }

    @Test
    void effectiveScheduleReturnsEmptyWhenNoOverride() {
        AppUser student = newStudent("s@pep.local");
        UUID moduleId = UUID.randomUUID();
        when(memberships.findActiveStreamIdsForUser(eq(student.getId()),
                eq(StudentStreamMembershipStatus.ACTIVE))).thenReturn(List.of(UUID.randomUUID()));
        when(schedules.findByStreamIdInAndModuleId(anyCollection(), eq(moduleId)))
                .thenReturn(List.of());
        assertThat(service.effectiveScheduleFor(student, moduleId).isEmpty()).isTrue();
    }

    @Test
    void effectiveScheduleChoosesEarliestStartAndLatestDeadline() {
        AppUser student = newStudent("s@pep.local");
        UUID moduleId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        when(memberships.findActiveStreamIdsForUser(eq(student.getId()),
                eq(StudentStreamMembershipStatus.ACTIVE))).thenReturn(List.of(s1, s2));
        LearningModule module = newModule(moduleId, newCourse(UUID.randomUUID()));
        StudentStream streamA = streamWithName("a");
        setId(streamA, s1);
        StudentStream streamB = streamWithName("b");
        setId(streamB, s2);

        StudentStreamModuleSchedule early = new StudentStreamModuleSchedule(streamA, module);
        early.applySchedule(
                OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                OffsetDateTime.parse("2026-02-01T00:00:00Z"),
                null, null);
        StudentStreamModuleSchedule late = new StudentStreamModuleSchedule(streamB, module);
        late.applySchedule(
                OffsetDateTime.parse("2026-01-10T00:00:00Z"),
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                null, null);
        when(schedules.findByStreamIdInAndModuleId(anyCollection(), eq(moduleId)))
                .thenReturn(List.of(early, late));
        StudentStreamService.EffectiveSchedule effective = service.effectiveScheduleFor(student, moduleId);
        assertThat(effective.startsAt()).isEqualTo(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        assertThat(effective.submissionDeadline()).isEqualTo(OffsetDateTime.parse("2026-03-01T00:00:00Z"));
    }

    @Test
    void deleteStreamBlockedIfMembersExist() {
        AppUser admin = newUser("admin@pep.local", Role.ADMIN);
        StudentStream stream = streamWithName("demo");
        when(users.findByEmail("admin@pep.local")).thenReturn(Optional.of(admin));
        when(streams.findById(any(UUID.class))).thenReturn(Optional.of(stream));
        when(memberships.countByStream(stream)).thenReturn(1L);
        assertThatThrownBy(() -> service.deleteStream("admin@pep.local", UUID.randomUUID()))
                .isInstanceOf(CorePlatformService.ValidationException.class);
        verify(streams, never()).delete(any());
    }

    @Test
    void addMembersRejectsAdminUsers() {
        AppUser admin = newUser("admin@pep.local", Role.ADMIN);
        AppUser otherAdmin = newUser("admin2@pep.local", Role.ADMIN);
        StudentStream stream = streamWithName("demo");
        when(users.findByEmail("admin@pep.local")).thenReturn(Optional.of(admin));
        when(streams.findById(any(UUID.class))).thenReturn(Optional.of(stream));
        when(users.findById(otherAdmin.getId())).thenReturn(Optional.of(otherAdmin));
        CoreDtos.AddStreamMembersRequest req = new CoreDtos.AddStreamMembersRequest(List.of(otherAdmin.getId()));
        assertThatThrownBy(() -> service.addMembers("admin@pep.local", UUID.randomUUID(), req))
                .isInstanceOf(CorePlatformService.ValidationException.class);
    }

    @Test
    void addMembersAllowsCuratorEnrollment() {
        AppUser admin = newUser("admin@pep.local", Role.ADMIN);
        AppUser curator = newUser("c@pep.local", Role.CURATOR);
        StudentStream stream = streamWithName("demo");
        when(users.findByEmail("admin@pep.local")).thenReturn(Optional.of(admin));
        when(streams.findById(any(UUID.class))).thenReturn(Optional.of(stream));
        when(users.findById(curator.getId())).thenReturn(Optional.of(curator));
        when(memberships.findByStreamAndUser(stream, curator)).thenReturn(Optional.empty());
        lenient().when(streamCourses.findByStreamOrderByPositionAscCreatedAtAsc(stream)).thenReturn(List.of());
        lenient().when(memberships.findByStream(stream)).thenReturn(List.of());
        lenient().when(schedules.findByStream(stream)).thenReturn(List.of());
        lenient().when(memberships.findByStreamAndStatus(stream, StudentStreamMembershipStatus.ACTIVE))
                .thenReturn(List.of());
        CoreDtos.AddStreamMembersRequest req = new CoreDtos.AddStreamMembersRequest(List.of(curator.getId()));
        service.addMembers("admin@pep.local", UUID.randomUUID(), req);
        verify(memberships, times(1)).save(any(StudentStreamMembership.class));
    }

    @Test
    void addMembersActivatesExistingRemovedMembership() {
        AppUser admin = newUser("admin@pep.local", Role.ADMIN);
        AppUser student = newStudent("s@pep.local");
        StudentStream stream = streamWithName("demo");
        StudentStreamMembership existing = new StudentStreamMembership(stream, student);
        existing.remove();
        when(users.findByEmail("admin@pep.local")).thenReturn(Optional.of(admin));
        when(streams.findById(any(UUID.class))).thenReturn(Optional.of(stream));
        when(users.findById(student.getId())).thenReturn(Optional.of(student));
        when(memberships.findByStreamAndUser(stream, student)).thenReturn(Optional.of(existing));
        CoreDtos.AddStreamMembersRequest req = new CoreDtos.AddStreamMembersRequest(List.of(student.getId()));
        lenient().when(streamCourses.findByStreamOrderByPositionAscCreatedAtAsc(stream)).thenReturn(List.of());
        lenient().when(memberships.findByStream(stream)).thenReturn(List.of(existing));
        lenient().when(schedules.findByStream(stream)).thenReturn(List.of());
        lenient().when(memberships.findByStreamAndStatus(stream, StudentStreamMembershipStatus.ACTIVE))
                .thenReturn(List.of(existing));
        service.addMembers("admin@pep.local", UUID.randomUUID(), req);
        assertThat(existing.getStatus()).isEqualTo(StudentStreamMembershipStatus.ACTIVE);
        verify(memberships, times(1)).save(existing);
    }

    // ---------- helpers ----------

    private AppUser newUser(String email, Role role) {
        AppUser user = new AppUser(email, "hash", "display", role);
        setId(user, UUID.randomUUID());
        return user;
    }

    private AppUser newStudent(String email) {
        return newUser(email, Role.STUDENT);
    }

    private StudentStream streamWithName(String name) {
        StudentStream stream = new StudentStream(name, null, StudentStreamStatus.ACTIVE, null);
        setId(stream, UUID.randomUUID());
        return stream;
    }

    private Course newCourse(UUID id) {
        Course course = new Course("Course " + id, "desc", ru.pep.platform.domain.CourseStatus.PUBLISHED);
        setId(course, id);
        return course;
    }

    private LearningModule newModule(UUID id, Course course) {
        LearningModule module = new LearningModule(course, "Module " + id, "topic", ModuleStatus.ACTIVE);
        setId(module, id);
        return module;
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field field = findField(entity.getClass(), "id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
