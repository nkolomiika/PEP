package ru.pep.platform.config;

import java.util.List;
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

            seedOwaspTop10Modules(courses, modules, lessons);
        };
    }

    private void seedOwaspTop10Modules(
            CourseRepository courses,
            LearningModuleRepository modules,
            LessonRepository lessons) {
        List<OwaspModuleSeed> owaspModules = List.of(
                new OwaspModuleSeed(
                        "A01. Broken Access Control",
                        "Broken Access Control",
                        "ошибки проверки владельца объекта, IDOR и обход ролей",
                        "создайте приложение, где пользователь может получить чужую запись по предсказуемому ID",
                        "проверьте прямой доступ к чужим объектам, изменение ID в URL и отсутствие role checks"),
                new OwaspModuleSeed(
                        "A02. Cryptographic Failures",
                        "Cryptographic Failures",
                        "небезопасное хранение секретов, слабое хеширование и отсутствие защиты чувствительных данных",
                        "создайте приложение, где секрет хранится открыто или пароль хешируется слабым алгоритмом",
                        "ищите утечки секретов, слабые reset flows и данные, передаваемые без должной защиты"),
                new OwaspModuleSeed(
                        "A04. Insecure Design",
                        "Insecure Design",
                        "ошибки бизнес-логики и workflow, которые не исправляются одной input validation",
                        "создайте сценарий обхода лимита, статуса заказа или обязательного шага процесса",
                        "проверьте пропуск шагов workflow, повтор операций и изменение состояния не по правилам"),
                new OwaspModuleSeed(
                        "A05. Security Misconfiguration",
                        "Security Misconfiguration",
                        "debug endpoints, default credentials, лишние headers и открытые админские функции",
                        "создайте приложение с включенным debug endpoint или тестовыми учетными данными",
                        "ищите default credentials, debug output, stack traces и открытые actuator/admin paths"),
                new OwaspModuleSeed(
                        "A06. Vulnerable and Outdated Components",
                        "Vulnerable Components",
                        "риски устаревших библиотек, frameworks и container base images",
                        "создайте учебный пример с устаревшей зависимостью и опишите публичное CVE",
                        "проверьте версии библиотек, package metadata, headers и признаки устаревшего framework"),
                new OwaspModuleSeed(
                        "A07. Identification and Authentication Failures",
                        "Authentication Failures",
                        "слабые пароли, predictable reset tokens и небезопасные session settings",
                        "создайте reset token, который можно предсказать или перебрать в учебном scope",
                        "проверьте reset flows, session fixation, weak password policy и predictable tokens"),
                new OwaspModuleSeed(
                        "A08. Software and Data Integrity Failures",
                        "Integrity Failures",
                        "доверие к неподписанным данным, insecure deserialization и небезопасные update flows",
                        "создайте endpoint, который доверяет неподписанному JSON/cookie payload",
                        "проверьте подмену client-side данных, unsigned tokens и отсутствие integrity checks"),
                new OwaspModuleSeed(
                        "A09. Security Logging and Monitoring Failures",
                        "Logging and Monitoring Failures",
                        "отсутствие событий безопасности, audit trail и сигналов для обнаружения атак",
                        "создайте сценарий атаки, который проходит без полезной записи в логах",
                        "проверьте, фиксируются ли login failures, access denied, report actions и подозрительные payloads"),
                new OwaspModuleSeed(
                        "A10. Server-Side Request Forgery",
                        "SSRF",
                        "серверные запросы к URL, controlled by user, и риск доступа к internal resources",
                        "создайте безопасный учебный SSRF endpoint с allowlisted локальной целью",
                        "проверьте URL fetch endpoints, redirects, private IP ranges и bypass allowlist"));

        for (OwaspModuleSeed seed : owaspModules) {
            LearningModule module = ensureModule(
                    courses,
                    modules,
                    "OWASP Top 10",
                    "Практический курс по базовым классам web-уязвимостей.",
                    seed.title(),
                    seed.topic());
            seedGenericOwaspLessons(lessons, module, seed);
        }
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

    private void seedGenericOwaspLessons(LessonRepository lessons, LearningModule module, OwaspModuleSeed seed) {
        if (lessons.existsByModuleId(module.getId())) {
            return;
        }
        lessons.save(new Lesson(
                module,
                seed.title() + ": теория и scope",
                """
                        ## Цель урока

                        Понять, как проявляется %s, где проходит граница учебного scope и какие evidence нужны
                        для отчета.

                        ## Что изучить

                        Тема покрывает %s. В практической работе студент должен показать минимальный
                        воспроизводимый сценарий, не выходя за назначенный lab и не создавая DoS-нагрузку.

                        ## Что приложить в отчет

                        - endpoint или screen flow;
                        - шаги воспроизведения;
                        - evidence влияния на безопасность;
                        - объяснение причины;
                        - рекомендацию по исправлению.
                        """.formatted(seed.title(), seed.theoryFocus()),
                1));
        lessons.save(new Lesson(
                module,
                "White box lab: " + seed.title(),
                """
                        ## Задание

                        %s.

                        ## Минимальные требования к lab

                        - приложение собрано в Docker image;
                        - image опубликован в registry и проходит technical validation;
                        - уязвимость соответствует теме `%s`;
                        - есть тестовые данные или учетные записи, если они нужны для воспроизведения;
                        - white box отчет объясняет уязвимый участок кода и безопасное исправление.

                        ## Ограничения

                        Не используйте реальные секреты, внешние сервисы без разрешения и payloads, которые могут
                        привести к denial of service.
                        """.formatted(seed.whiteBoxTask(), seed.title()),
                2));
        lessons.save(new Lesson(
                module,
                "Black box чеклист: " + seed.title(),
                """
                        ## Чеклист тестирования

                        %s.

                        ## Правила отчета

                        - проверяйте только назначенный lab;
                        - сохраняйте payloads и HTTP-запросы в минимальном виде;
                        - отделяйте подтвержденные находки от гипотез;
                        - оценивайте impact через доступ, данные, целостность и воспроизводимость;
                        - предлагайте исправление, которое устраняет причину, а не только симптом.
                        """.formatted(seed.blackBoxChecklist()),
                3));
    }

    private record OwaspModuleSeed(
            String title,
            String topic,
            String theoryFocus,
            String whiteBoxTask,
            String blackBoxChecklist) {
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
