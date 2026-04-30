package ru.pep.platform.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/overview")
public class PlatformOverviewController {

    @GetMapping
    public PlatformOverview overview() {
        return new PlatformOverview(
                "PEP",
                "Практическая образовательная платформа по информационной безопасности",
                List.of("СТУДЕНТ", "КУРАТОР", "АДМИНИСТРАТОР"),
                List.of(
                        "Вводный курс по Docker",
                        "Изучение OWASP Top 10",
                        "Сдача Docker image уязвимого приложения",
                        "Запуск lab в локальном Kubernetes через kind",
                        "Black box тестирование чужих работ",
                        "Проверка и оценка куратором"));
    }

    public record PlatformOverview(
            String code,
            String name,
            List<String> roles,
            List<String> learningFlow) {
    }
}
