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
                        "Изучение Академии web-безопасности",
                        "Загрузка архива стенда или готового Docker image",
                        "Запуск lab в локальном Kubernetes через kind",
                        "Запуск системных стендов на 4 часа",
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
