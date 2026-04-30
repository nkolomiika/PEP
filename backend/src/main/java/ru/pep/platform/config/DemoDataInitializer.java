package ru.pep.platform.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.Course;
import ru.pep.platform.domain.CourseStatus;
import ru.pep.platform.domain.LearningModule;
import ru.pep.platform.domain.Lesson;
import ru.pep.platform.domain.ModuleStatus;
import ru.pep.platform.domain.Role;
import ru.pep.platform.repository.AppUserRepository;
import ru.pep.platform.repository.CourseRepository;
import ru.pep.platform.repository.LessonRepository;

@Configuration
public class DemoDataInitializer {

    @Bean
    CommandLineRunner seedDemoData(
            AppUserRepository users,
            CourseRepository courses,
            LessonRepository lessons,
            PasswordEncoder passwordEncoder) {
        return args -> {
            createUser(users, passwordEncoder, "admin@pep.local", "admin", "Администратор", Role.ADMIN);
            createUser(users, passwordEncoder, "curator@pep.local", "curator", "Куратор", Role.CURATOR);
            createUser(users, passwordEncoder, "student1@pep.local", "student", "Студент 1", Role.STUDENT);
            createUser(users, passwordEncoder, "student2@pep.local", "student", "Студент 2", Role.STUDENT);

            if (courses.count() == 0) {
                Course course = new Course(
                        "OWASP Top 10",
                        "Практический курс по базовым классам web-уязвимостей.",
                        CourseStatus.PUBLISHED);
                LearningModule module = new LearningModule("A03. Injection", "SQL Injection", ModuleStatus.ACTIVE);
                course.addModule(module);
                courses.save(course);
                seedLessons(lessons, module);
            }
        };
    }

    private void seedLessons(LessonRepository lessons, LearningModule module) {
        if (lessons.existsByModuleId(module.getId())) {
            return;
        }
        lessons.save(new Lesson(
                module,
                "Docker image для сдачи на платформе",
                """
                        ## Цель урока

                        Научиться готовить приложение как Docker image, который можно отправить на technical validation.

                        ## Минимальные требования

                        - image запускается без ручных действий;
                        - приложение слушает документированный port `8080`;
                        - endpoint `/health` возвращает успешный ответ;
                        - не используется `--privileged`, host network и реальные секреты.

                        ## Команды

                        ```bash
                        docker build -t vulnerable-sqli-demo:latest ./examples/vulnerable-sqli-demo
                        docker tag vulnerable-sqli-demo:latest localhost:5001/vulnerable-sqli-demo:latest
                        docker push localhost:5001/vulnerable-sqli-demo:latest
                        ```
                        """,
                1));
        lessons.save(new Lesson(
                module,
                "SQL Injection: причина и эксплуатация",
                """
                        ## Что такое SQL Injection

                        SQL Injection возникает, когда пользовательский ввод попадает в SQL-запрос как часть команды,
                        а не как данные.

                        Уязвимый пример:

                        ```sql
                        SELECT * FROM users WHERE email = '<input>' AND password = '<input>';
                        ```

                        Payload для демонстрации:

                        ```text
                        ' OR '1'='1
                        ```

                        ## Что приложить в отчет

                        - endpoint;
                        - payload;
                        - evidence успешной эксплуатации;
                        - рекомендацию использовать parameterized queries.
                        """,
                2));
        lessons.save(new Lesson(
                module,
                "Black box чеклист для A03. Injection",
                """
                        ## Чеклист

                        - найти формы ввода и query parameters;
                        - проверить признаки boolean-based injection;
                        - не выполнять DoS и не выходить за scope назначенного lab;
                        - зафиксировать только минимальное evidence;
                        - оформить impact и рекомендацию по исправлению.
                        """,
                3));
    }

    private void createUser(
            AppUserRepository users,
            PasswordEncoder passwordEncoder,
            String email,
            String rawPassword,
            String displayName,
            Role role) {
        if (!users.existsByEmail(email)) {
            users.save(new AppUser(email, passwordEncoder.encode(rawPassword), displayName, role));
        }
    }
}
