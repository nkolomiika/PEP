package ru.pep.platform.api;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.pep.platform.service.StudentStreamService;

@RestController
@RequestMapping("/api/admin/streams")
@PreAuthorize("hasAnyRole('ADMIN','CURATOR')")
public class AdminStreamController {

    private final StudentStreamService streamService;

    public AdminStreamController(StudentStreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping
    public CoreDtos.PageResponse<CoreDtos.StudentStreamSummaryResponse> list(
            Principal principal,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        return streamService.listStreams(principal.getName(), query, page, size);
    }

    @GetMapping("/{streamId}")
    public CoreDtos.StudentStreamResponse get(Principal principal, @PathVariable UUID streamId) {
        return streamService.getStream(principal.getName(), streamId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoreDtos.StudentStreamResponse create(
            Principal principal,
            @Valid @RequestBody CoreDtos.CreateStudentStreamRequest request) {
        return streamService.createStream(principal.getName(), request);
    }

    @PatchMapping("/{streamId}")
    public CoreDtos.StudentStreamResponse update(
            Principal principal,
            @PathVariable UUID streamId,
            @Valid @RequestBody CoreDtos.UpdateStudentStreamRequest request) {
        return streamService.updateStream(principal.getName(), streamId, request);
    }

    @DeleteMapping("/{streamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable UUID streamId) {
        streamService.deleteStream(principal.getName(), streamId);
    }

    @PostMapping("/{streamId}/courses")
    public CoreDtos.StudentStreamResponse addCourse(
            Principal principal,
            @PathVariable UUID streamId,
            @Valid @RequestBody CoreDtos.AddStreamCourseRequest request) {
        return streamService.addCourse(principal.getName(), streamId, request);
    }

    @DeleteMapping("/{streamId}/courses/{courseId}")
    public CoreDtos.StudentStreamResponse removeCourse(
            Principal principal,
            @PathVariable UUID streamId,
            @PathVariable UUID courseId) {
        return streamService.removeCourse(principal.getName(), streamId, courseId);
    }

    @PutMapping("/{streamId}/modules/{moduleId}/schedule")
    public CoreDtos.StudentStreamResponse upsertSchedule(
            Principal principal,
            @PathVariable UUID streamId,
            @PathVariable UUID moduleId,
            @Valid @RequestBody CoreDtos.UpsertStreamScheduleRequest request) {
        return streamService.upsertSchedule(principal.getName(), streamId, moduleId, request);
    }

    @PostMapping("/{streamId}/members")
    public CoreDtos.StudentStreamResponse addMembers(
            Principal principal,
            @PathVariable UUID streamId,
            @Valid @RequestBody CoreDtos.AddStreamMembersRequest request) {
        return streamService.addMembers(principal.getName(), streamId, request);
    }

    @DeleteMapping("/{streamId}/members/{userId}")
    public CoreDtos.StudentStreamResponse removeMember(
            Principal principal,
            @PathVariable UUID streamId,
            @PathVariable UUID userId) {
        return streamService.removeMember(principal.getName(), streamId, userId);
    }
}
