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
import ru.pep.platform.repository.LearningModuleRepository;
import ru.pep.platform.repository.LessonRepository;

@Configuration
public class DemoDataInitializer {

    @Bean
    CommandLineRunner seedDemoData(
            AppUserRepository users,
            CourseRepository courses,
            LearningModuleRepository modules,
            LessonRepository lessons,
            PasswordEncoder passwordEncoder) {
        return args -> {
            createUser(users, passwordEncoder, "admin@pep.local", "admin", "Администратор", Role.ADMIN);
            createUser(users, passwordEncoder, "curator@pep.local", "curator", "Куратор", Role.CURATOR);
            createUser(users, passwordEncoder, "student1@pep.local", "student", "Студент 1", Role.STUDENT);
            createUser(users, passwordEncoder, "student2@pep.local", "student", "Студент 2", Role.STUDENT);

            LearningModule dockerModule = ensureModule(
                    courses,
                    modules,
                    "Вводный курс по Docker",
                    "Базовый курс по контейнерам, Dockerfile, Docker Compose и публикации image.",
                    "Docker intro",
                    "Docker basics");
            seedDockerLessons(lessons, dockerModule);

            LearningModule injectionModule = ensureModule(
                    courses,
                    modules,
                    "OWASP Top 10",
                    "Практический курс по базовым классам web-уязвимостей.",
                    "A03. Injection",
                    "SQL Injection");
            seedInjectionLessons(lessons, injectionModule);
        };
    }

    private LearningModule ensureModule(
            CourseRepository courses,
            LearningModuleRepository modules,
            String courseTitle,
            String courseDescription,
            String moduleTitle,
            String vulnerabilityTopic) {
        Course course = courses.findByTitle(courseTitle)
                .orElseGet(() -> courses.save(new Course(courseTitle, courseDescription, CourseStatus.PUBLISHED)));
        return modules.findByCourseIdAndTitle(course.getId(), moduleTitle)
                .orElseGet(() -> modules.save(new LearningModule(course, moduleTitle, vulnerabilityTopic, ModuleStatus.ACTIVE)));
    }

    private void seedDockerLessons(LessonRepository lessons, LearningModule module) {
        if (lessons.existsByModuleId(module.getId())) {
            return;
        }
        lessons.save(new Lesson(
                module,
                "Контейнеры и образы",
                """
                        ## Цель урока

                        Понять, зачем нужна контейнеризация и чем Docker image отличается от running container.

                        ## Ключевые идеи

                        - image - неизменяемый шаблон приложения;
                        - container - запущенный процесс на основе image;
                        - registry хранит и раздает images;
                        - logs помогают диагностировать запуск приложения.

                        ## Практика

                        ```bash
                        docker run hello-world
                        docker run --rm -p 8081:80 nginx:alpine
                        docker logs <container-id>
                        ```
                        """,
                1));
        lessons.save(new Lesson(
                module,
                "Основы Dockerfile",
                """
                        ## Цель урока

                        Научиться описывать приложение как Docker image с воспроизводимым запуском.

                        ## Минимальный Dockerfile

                        ```dockerfile
                        FROM node:22-alpine
                        WORKDIR /app
                        COPY app.js .
                        EXPOSE 8080
                        CMD ["node", "app.js"]
                        ```

                        ## Checklist

                        - есть понятный `FROM`;
                        - приложение слушает документированный port;
                        - запуск происходит одной командой `docker run`.
                        """,
                2));
        lessons.save(new Lesson(
                module,
                "Публикация image в registry",
                """
                        ## Цель урока

                        Подготовить image reference, который можно отправить на платформу.

                        ## Команды для demo registry

                        ```bash
                        docker build -t vulnerable-sqli-demo:latest ./examples/vulnerable-sqli-demo
                        docker tag vulnerable-sqli-demo:latest localhost:5001/vulnerable-sqli-demo:latest
                        docker push localhost:5001/vulnerable-sqli-demo:latest
                        ```

                        ## Важно

                        В MVP платформа не собирает исходный код студента. Студент отправляет только готовый
                        Docker image reference, application port и health path.
                        """,
                3));
    }

    private void seedInjectionLessons(LessonRepository lessons, LearningModule module) {
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
