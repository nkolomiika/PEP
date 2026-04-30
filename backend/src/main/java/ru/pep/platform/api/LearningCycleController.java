package ru.pep.platform.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pep.platform.domain.LabStatus;
import ru.pep.platform.domain.ReportStatus;
import ru.pep.platform.domain.SubmissionStatus;
import ru.pep.platform.domain.ValidationJobStatus;

@RestController
@RequestMapping("/api/learning-cycle")
public class LearningCycleController {

    @GetMapping
    public LearningCycle learningCycle() {
        return new LearningCycle(
                List.of(
                        "Теория OWASP Top 10",
                        "Docker-подготовка",
                        "Публикация Docker image",
                        "Техническая проверка",
                        "White box отчет",
                        "Проверка куратора",
                        "Запуск lab в kind",
                        "Black box распределение",
                        "Black box отчет",
                        "Итоговая оценка"),
                List.of(SubmissionStatus.values()),
                List.of(ValidationJobStatus.values()),
                List.of(ReportStatus.values()),
                List.of(LabStatus.values()));
    }

    public record LearningCycle(
            List<String> phases,
            List<SubmissionStatus> submissionStatuses,
            List<ValidationJobStatus> validationJobStatuses,
            List<ReportStatus> reportStatuses,
            List<LabStatus> labStatuses) {
    }
}
