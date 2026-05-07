package ru.pep.platform.api;

import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pep.platform.service.StudentStreamService;

@RestController
@RequestMapping("/api")
public class StudentStreamController {

    private final StudentStreamService streamService;

    public StudentStreamController(StudentStreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping("/my-streams")
    @PreAuthorize("hasAnyRole('STUDENT','CURATOR','ADMIN')")
    public List<CoreDtos.StudentStreamSummaryResponse> myStreams(Principal principal) {
        return streamService.listActiveStreamsForStudent(principal.getName());
    }
}
