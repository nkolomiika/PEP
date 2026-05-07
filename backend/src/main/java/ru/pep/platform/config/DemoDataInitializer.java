package ru.pep.platform.config;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.Course;
import ru.pep.platform.domain.CourseStatus;
import ru.pep.platform.domain.LearningModule;
import ru.pep.platform.domain.Lesson;
import ru.pep.platform.domain.ModuleStatus;
import ru.pep.platform.domain.PentestTask;
import ru.pep.platform.domain.Role;
import ru.pep.platform.domain.StudentStream;
import ru.pep.platform.domain.StudentStreamCourse;
import ru.pep.platform.domain.StudentStreamMembership;
import ru.pep.platform.domain.StudentStreamModuleSchedule;
import ru.pep.platform.domain.StudentStreamStatus;
import ru.pep.platform.repository.AppUserRepository;
import ru.pep.platform.repository.CourseRepository;
import ru.pep.platform.repository.LearningModuleRepository;
import ru.pep.platform.repository.LessonRepository;
import ru.pep.platform.repository.PentestTaskRepository;
import ru.pep.platform.repository.StudentStreamCourseRepository;
import ru.pep.platform.repository.StudentStreamMembershipRepository;
import ru.pep.platform.repository.StudentStreamModuleScheduleRepository;
import ru.pep.platform.repository.StudentStreamRepository;

@Configuration
@ConditionalOnProperty(prefix = "pep.demo-data", name = "enabled", havingValue = "true")
public class DemoDataInitializer {

    @Bean
    CommandLineRunner seedDemoData(
            AppUserRepository users,
            CourseRepository courses,
            LearningModuleRepository modules,
            LessonRepository lessons,
            PentestTaskRepository pentestTasks,
            StudentStreamRepository streams,
            StudentStreamCourseRepository streamCourses,
            StudentStreamMembershipRepository streamMembers,
            StudentStreamModuleScheduleRepository streamSchedules,
            PasswordEncoder passwordEncoder) {
        return args -> {
            AppUser admin = createUser(users, passwordEncoder, "admin@pep.local", "admin", "Администратор", Role.ADMIN);
            createUser(users, passwordEncoder, "curator@pep.local", "curator", "Куратор", Role.CURATOR);
            AppUser student1 = createUser(users, passwordEncoder, "student1@pep.local", "student", "Студент 1", Role.STUDENT);
            AppUser student2 = createUser(users, passwordEncoder, "student2@pep.local", "student", "Студент 2", Role.STUDENT);
            AppUser student3 = createUser(users, passwordEncoder, "student3@pep.local", "student", "Студент 3", Role.STUDENT);

            LearningModule dockerModule = ensureModule(
                    courses,
                    modules,
                    "Вводный курс по Docker",
                    "Базовый курс по контейнерам, Dockerfile, Docker Compose и публикации image.",
                    "Docker intro",
                    "Docker basics");
            seedDockerLessons(lessons, dockerModule);

            archiveLegacySecurityCourses(courses);
            seedSecurityAcademyModules(courses, modules, lessons);
            seedPracticeTasks(pentestTasks);

            seedStudentStreams(
                    streams,
                    streamCourses,
                    streamMembers,
                    streamSchedules,
                    courses,
                    modules,
                    admin,
                    student1,
                    student2,
                    student3);
        };
    }

    private void seedStudentStreams(
            StudentStreamRepository streams,
            StudentStreamCourseRepository streamCourses,
            StudentStreamMembershipRepository streamMembers,
            StudentStreamModuleScheduleRepository streamSchedules,
            CourseRepository courses,
            LearningModuleRepository modules,
            AppUser admin,
            AppUser student1,
            AppUser student2,
            AppUser student3) {
        Course webAcademy = courses.findByTitle("Академия веб-безопасности").orElse(null);
        Course docker = courses.findByTitle("Вводный курс по Docker").orElse(null);

        StudentStream webStream = streams.findByName("web-academy-2026-1")
                .orElseGet(() -> streams.save(new StudentStream(
                        "web-academy-2026-1",
                        null,
                        StudentStreamStatus.ACTIVE,
                        admin)));
        StudentStream dockerStream = streams.findByName("docker-intro-2026-1")
                .orElseGet(() -> streams.save(new StudentStream(
                        "docker-intro-2026-1",
                        null,
                        StudentStreamStatus.ACTIVE,
                        admin)));

        if (webAcademy != null) {
            ensureStreamCourse(streamCourses, webStream, webAcademy, 0);
            OffsetDateTime now = OffsetDateTime.now();
            List<LearningModule> webModules = modules.findByCourseId(webAcademy.getId());
            for (LearningModule module : webModules) {
                ensureSchedule(streamSchedules, webStream, module,
                        now.minusDays(7), now.plusDays(21), now.plusDays(14), now.plusDays(28));
            }
        }
        if (docker != null) {
            ensureStreamCourse(streamCourses, dockerStream, docker, 0);
            OffsetDateTime now = OffsetDateTime.now();
            List<LearningModule> dockerModules = modules.findByCourseId(docker.getId());
            for (LearningModule module : dockerModules) {
                ensureSchedule(streamSchedules, dockerStream, module,
                        now.minusDays(14), now.plusDays(7), now.plusDays(3), now.plusDays(10));
            }
        }

        ensureMembership(streamMembers, webStream, student1);
        ensureMembership(streamMembers, webStream, student2);
        ensureMembership(streamMembers, dockerStream, student1);
        ensureMembership(streamMembers, dockerStream, student3);
    }

    private void ensureStreamCourse(
            StudentStreamCourseRepository repo,
            StudentStream stream,
            Course course,
            int position) {
        if (repo.findByStreamAndCourse(stream, course).isEmpty()) {
            repo.save(new StudentStreamCourse(stream, course, position));
        }
    }

    private void ensureMembership(
            StudentStreamMembershipRepository repo,
            StudentStream stream,
            AppUser student) {
        if (repo.findByStreamAndUser(stream, student).isEmpty()) {
            repo.save(new StudentStreamMembership(stream, student));
        }
    }

    private void ensureSchedule(
            StudentStreamModuleScheduleRepository repo,
            StudentStream stream,
            LearningModule module,
            OffsetDateTime startsAt,
            OffsetDateTime submissionDeadline,
            OffsetDateTime blackBoxStartsAt,
            OffsetDateTime blackBoxDeadline) {
        StudentStreamModuleSchedule schedule = repo.findByStreamAndModule(stream, module)
                .orElseGet(() -> new StudentStreamModuleSchedule(stream, module));
        schedule.applySchedule(startsAt, submissionDeadline, blackBoxStartsAt, blackBoxDeadline);
        repo.save(schedule);
    }

    private void archiveLegacySecurityCourses(CourseRepository courses) {
        List.of("OWASP Top 10", "Web Security Academy").forEach(title ->
                courses.findByTitle(title).ifPresent(course -> {
                    course.archive();
                    courses.save(course);
                }));
    }

    private void seedSecurityAcademyModules(
            CourseRepository courses,
            LearningModuleRepository modules,
            LessonRepository lessons) {
        List<SecurityModuleSeed> securityModules = List.of(
                new SecurityModuleSeed(
                        "SQL-инъекции",
                        "Инъекции в запросы к базе данных",
                        "Пользовательский ввод становится частью команды к базе данных, меняет условие выборки, раскрывает данные или обходит проверку входа.",
                        "формы входа, поиск, фильтры, сортировка, параметры идентификаторов и административные отчеты",
                        """
                                app.post('/login', async (req, res) => {
                                  const sql = "SELECT id, email FROM users WHERE email = '" + req.body.email +
                                    "' AND password = '" + req.body.password + "'";
                                  const user = await db.get(sql);
                                  if (!user) return res.status(403).send('Неверные данные');
                                  res.json({ id: user.id, email: user.email });
                                });
                                """,
                        "Ввод вставляется в текст запроса. Значение `' OR '1'='1` закрывает строку и добавляет истинное условие.",
                        """
                                app.post('/login', async (req, res) => {
                                  const user = await db.get(
                                    'SELECT id, email, password_hash FROM users WHERE email = ?',
                                    [req.body.email]
                                  );
                                  if (!user || !await verifyPassword(req.body.password, user.password_hash)) {
                                    return res.status(403).send('Неверные данные');
                                  }
                                  res.json({ id: user.id, email: user.email });
                                });
                                """,
                        "параметризованные запросы, белые списки для сортировки, отдельная проверка пароля, нейтральные ошибки",
                        "' OR '1'='1, ' AND '1'='2, UNION SELECT для учебных данных, проверка задержек только в локальном стенде"),
                new SecurityModuleSeed(
                        "Межсайтовое выполнение сценариев",
                        "Внедрение сценариев в страницу",
                        "Приложение возвращает пользовательский ввод в страницу без контекстного кодирования, и браузер выполняет внедренный сценарий в контексте сайта.",
                        "поиск, комментарии, профиль, сообщения об ошибках, предпросмотр Markdown и параметры ссылки",
                        """
                                app.get('/search', (req, res) => {
                                  res.send(`<h1>Результаты для: ${req.query.q}</h1>`);
                                });
                                """,
                        "Параметр `q` попадает в HTML как разметка. Сценарий получает доступ к контексту браузера этого origin.",
                        """
                                import escapeHtml from 'escape-html';

                                app.get('/search', (req, res) => {
                                  res.send(`<h1>Результаты для: ${escapeHtml(req.query.q ?? '')}</h1>`);
                                });
                                """,
                        "контекстное кодирование, безопасные шаблонизаторы, политика защиты содержимого, запрет небезопасного HTML",
                        "<script>alert(1)</script>, <img src=x onerror=alert(1)>, проверка отраженных и сохраненных значений"),
                new SecurityModuleSeed(
                        "Инъекция в серверные шаблоны",
                        "Выполнение выражений шаблонизатора",
                        "Пользовательский ввод попадает в шаблон до обработки движком и превращается в выражение на стороне сервера.",
                        "письма, счета, генераторы страниц, предпросмотр шаблонов, персонализированные приветствия",
                        """
                                app.post('/preview', (req, res) => {
                                  const template = `Здравствуйте, ${req.body.name}`;
                                  res.send(renderTemplate(template, req.body));
                                });
                                """,
                        "Имя пользователя становится частью шаблона. Если движок поддерживает выражения, ввод может читать переменные или вызывать опасные объекты.",
                        """
                                app.post('/preview', (req, res) => {
                                  res.render('preview', { name: String(req.body.name ?? '') });
                                });
                                """,
                        "никогда не собирать шаблон из ввода, передавать ввод только как данные, ограничивать возможности движка",
                        "{{7*7}}, ${7*7}, #{7*7}, проверка отличий между текстом и вычисленным выражением"),
                new SecurityModuleSeed(
                        "Подделка серверных запросов",
                        "Сервер обращается к адресу из пользовательского ввода",
                        "Приложение делает сетевой запрос к адресу, который контролирует пользователь, и может обратиться к внутренним ресурсам.",
                        "предпросмотр ссылок, импорт изображений, проверка webhook, загрузка по адресу, генерация снимков страниц",
                        """
                                app.get('/preview', async (req, res) => {
                                  const response = await fetch(req.query.url);
                                  res.send(await response.text());
                                });
                                """,
                        "Сервер доверяет произвольному адресу. Запрос выполняется из внутренней сети приложения, а не из браузера пользователя.",
                        """
                                const allowedHosts = new Set(['docs.example.local', 'cdn.example.local']);

                                app.get('/preview', async (req, res) => {
                                  const url = new URL(req.query.url);
                                  if (url.protocol !== 'https:' || !allowedHosts.has(url.hostname)) {
                                    return res.status(400).send('Адрес не разрешен');
                                  }
                                  const response = await fetch(url, { redirect: 'manual' });
                                  res.send(await response.text());
                                });
                                """,
                        "белый список узлов, запрет внутренних адресов, запрет перенаправлений, сетевые ограничения исходящих запросов",
                        "локальные адреса, адреса метаданных, перенаправления, разные записи IPv4 и доменные имена"),
                new SecurityModuleSeed(
                        "Межсайтовая подделка запроса",
                        "Навязывание действия авторизованному пользователю",
                        "Браузер автоматически отправляет cookie, поэтому чужая страница может инициировать действие в приложении без согласия пользователя.",
                        "смена почты, смена пароля, создание ключа, перевод средств, изменение настроек",
                        """
                                app.post('/profile/email', requireLogin, async (req, res) => {
                                  await db.users.update(req.user.id, { email: req.body.email });
                                  res.json({ ok: true });
                                });
                                """,
                        "Endpoint меняет состояние и доверяет только cookie. У запроса нет одноразового токена и проверки происхождения.",
                        """
                                app.post('/profile/email', requireLogin, csrfProtection, async (req, res) => {
                                  await db.users.update(req.user.id, { email: req.body.email });
                                  res.json({ ok: true });
                                });
                                """,
                        "одноразовые токены, SameSite cookie, проверка происхождения запроса, повторная проверка важных действий",
                        "HTML-форма с автоматической отправкой, отсутствие токена, проверка SameSite и заголовка происхождения"),
                new SecurityModuleSeed(
                        "Ошибки настройки CORS",
                        "Неправильное доверие другому источнику",
                        "Сервер разрешает чужому источнику читать ответы, которые должны быть доступны только текущему сайту.",
                        "личный профиль, настройки, токены, административные ответы, ответы с cookie",
                        """
                                app.use((req, res, next) => {
                                  const origin = req.headers.origin;
                                  if (origin) {
                                    res.setHeader('Access-Control-Allow-Origin', origin);
                                    res.setHeader('Access-Control-Allow-Credentials', 'true');
                                  }
                                  next();
                                });
                                """,
                        "Любой источник отражается как доверенный. Вместе с учетными данными это разрешает чтение личных ответов чужой страницей.",
                        """
                                const allowedOrigins = new Set(['https://academy.local', 'https://teacher.local']);

                                app.use((req, res, next) => {
                                  const origin = req.headers.origin;
                                  if (origin && allowedOrigins.has(origin)) {
                                    res.setHeader('Access-Control-Allow-Origin', origin);
                                    res.setHeader('Access-Control-Allow-Credentials', 'true');
                                    res.setHeader('Vary', 'Origin');
                                  }
                                  next();
                                });
                                """,
                        "точный белый список источников, запрет отражения Origin, разделение публичных и личных ответов",
                        "Origin: https://attacker.local, Origin: null, похожие поддомены, чтение ответа с учетными данными"),
                new SecurityModuleSeed(
                        "Внешние сущности XML",
                        "Небезопасная обработка XML",
                        "XML-разборщик разрешает внешние сущности и может читать локальные файлы или выполнять сетевые обращения от имени сервера.",
                        "загрузка XML, импорт офисных документов, SOAP, SVG, старые интеграции",
                        """
                                app.post('/import', express.text({ type: 'application/xml' }), async (req, res) => {
                                  const parser = new DOMParser({ resolveExternalEntities: true });
                                  const doc = parser.parseFromString(req.body, 'text/xml');
                                  res.send(doc.getElementsByTagName('name')[0].textContent);
                                });
                                """,
                        "Разборщик доверяет DTD и внешним сущностям. XML-документ может попросить сервер прочитать файл или URL.",
                        """
                                app.post('/import', express.text({ type: 'application/xml' }), async (req, res) => {
                                  const parser = new DOMParser({
                                    resolveExternalEntities: false,
                                    disallowDoctype: true
                                  });
                                  const doc = parser.parseFromString(req.body, 'text/xml');
                                  res.send(doc.getElementsByTagName('name')[0].textContent);
                                });
                                """,
                        "запрет DTD, запрет внешних сущностей, ограничение размера XML, безопасные библиотеки разбора",
                        "<!DOCTYPE>, внешняя сущность file, внешняя сущность http, проверка ошибок разбора"),
                new SecurityModuleSeed(
                        "Уязвимости бизнес-логики",
                        "Обход правил процесса",
                        "Код технически работает, но сервер не проверяет бизнес-инварианты: порядок шагов, лимиты, владельца действия или допустимый переход состояния.",
                        "корзина, купоны, платежи, обучение, выдача заданий, смена статусов",
                        """
                                app.post('/coupon/apply', requireLogin, async (req, res) => {
                                  const cart = await db.carts.current(req.user.id);
                                  cart.total = cart.total - Number(req.body.discount);
                                  await db.carts.save(cart);
                                  res.json(cart);
                                });
                                """,
                        "Клиент сам задает скидку. Сервер не проверяет существование купона, лимит применения и минимальную сумму.",
                        """
                                app.post('/coupon/apply', requireLogin, async (req, res) => {
                                  const coupon = await db.coupons.findActive(req.body.code);
                                  const cart = await db.carts.current(req.user.id);
                                  if (!coupon || cart.total < coupon.minTotal || await usedBefore(req.user.id, coupon.id)) {
                                    return res.status(400).send('Купон недоступен');
                                  }
                                  cart.total = Math.max(0, cart.total - coupon.discount);
                                  await db.carts.save(cart);
                                  res.json(cart);
                                });
                                """,
                        "серверные инварианты, атомарные операции, проверка переходов состояния, идемпотентность",
                        "повтор действия, пропуск шага, отрицательные значения, гонки, изменение статуса вне процесса"),
                new SecurityModuleSeed(
                        "Нарушение управления доступом",
                        "Отсутствие проверки прав",
                        "Приложение проверяет вход пользователя, но не проверяет, имеет ли он право выполнять конкретное действие.",
                        "административные методы, смена роли, просмотр отчетов, операции куратора, экспорт данных",
                        """
                                app.post('/admin/users/:id/role', requireLogin, async (req, res) => {
                                  await db.users.update(req.params.id, { role: req.body.role });
                                  res.json({ ok: true });
                                });
                                """,
                        "Есть только проверка входа. Любой пользователь может вызвать административное действие напрямую.",
                        """
                                app.post('/admin/users/:id/role', requireLogin, requireRole('ADMIN'), async (req, res) => {
                                  await db.users.update(req.params.id, { role: req.body.role });
                                  res.json({ ok: true });
                                });
                                """,
                        "проверка роли на сервере, запрет по умолчанию, политики на уровне объекта и действия",
                        "прямой вызов скрытых маршрутов, смена метода, изменение роли в запросе, проверка ответа 403"),
                new SecurityModuleSeed(
                        "Небезопасные прямые ссылки на объекты",
                        "Доступ к чужому объекту по идентификатору",
                        "Пользователь меняет идентификатор объекта и получает данные или действие, принадлежащее другому пользователю.",
                        "заказы, отчеты, файлы, профили, вложения, лабораторные стенды",
                        """
                                app.get('/api/reports/:id', requireLogin, async (req, res) => {
                                  const report = await db.reports.findById(req.params.id);
                                  res.json(report);
                                });
                                """,
                        "Сервер проверяет вход, но не сравнивает владельца отчета с текущим пользователем.",
                        """
                                app.get('/api/reports/:id', requireLogin, async (req, res) => {
                                  const report = await db.reports.findById(req.params.id);
                                  if (!report || report.authorId !== req.user.id) {
                                    return res.status(404).send('Отчет не найден');
                                  }
                                  res.json(report);
                                });
                                """,
                        "проверка владельца объекта, непрямые идентификаторы, одинаковая ошибка для чужого и несуществующего объекта",
                        "изменение id, перебор соседних id, прямой доступ к файлу, проверка чужих вложений"));

        for (SecurityModuleSeed seed : securityModules) {
            LearningModule module = ensureModule(
                    courses,
                    modules,
                    "Академия веб-безопасности",
                    "Подробный русскоязычный курс по практической эксплуатации и защите web-уязвимостей.",
                    seed.title(),
                    seed.topic());
            upsertLesson(lessons, module, 1, seed.title() + ": полный разбор", securityLesson(seed));
        }
    }

    private void seedPracticeTasks(PentestTaskRepository tasks) {
        List<PracticeTaskSeed> categories = List.of(
                new PracticeTaskSeed("SQL-инъекции", "sqli", "авторизация, поиск и фильтры с SQL-параметрами"),
                new PracticeTaskSeed("Межсайтовое выполнение сценариев", "xss", "отражение и хранение пользовательского HTML/JS"),
                new PracticeTaskSeed("Инъекция в серверные шаблоны", "ssti", "рендер серверных шаблонов из пользовательских данных"),
                new PracticeTaskSeed("Подделка серверных запросов", "ssrf", "запросы backend к пользовательским URL"),
                new PracticeTaskSeed("Межсайтовая подделка запроса", "csrf", "изменение состояния по cookie без защиты токеном"),
                new PracticeTaskSeed("Ошибки настройки CORS", "cors", "доверие чужому origin и ответы с credentials"),
                new PracticeTaskSeed("Внешние сущности XML", "xxe", "разбор XML с DTD и внешними сущностями"),
                new PracticeTaskSeed("Уязвимости бизнес-логики", "business-logic", "обход шагов и инвариантов процесса"),
                new PracticeTaskSeed("Нарушение управления доступом", "bac", "доступ к административным действиям без роли"),
                new PracticeTaskSeed("Небезопасные прямые ссылки на объекты", "idor", "чужие объекты через подмену идентификатора"));
        List<PracticeVariantSeed> variants = List.of(
                new PracticeVariantSeed(
                        "exploit",
                        "easy",
                        "базовая эксплуатация",
                        "Эксплуатация уязвимости",
                        "Найдите уязвимость в базовом endpoint и покажите минимальный воспроизводимый payload.",
                        "Добавьте в отчет исходный запрос и измененный запрос с объяснением эффекта."),
                new PracticeVariantSeed(
                        "blackbox",
                        "medium",
                        "слепой black-box сценарий",
                        "Black-box исследование",
                        "Проведите проверку только через HTTP-запросы и поведение сервера без чтения исходного кода.",
                        "Зафиксируйте признаки уязвимости и обоснование влияния на безопасность."),
                new PracticeVariantSeed(
                        "fix-check",
                        "hard",
                        "проверка исправления",
                        "Верификация защиты",
                        "Покажите, как воспроизвести дефект до исправления и как подтвердить, что после фикса сценарий больше не работает.",
                        "Приложите негативный тест и критерий приемки для защищенной версии."));
        long projectId = -1;
        for (PracticeTaskSeed category : categories) {
            for (PracticeVariantSeed variant : variants) {
                String slug = category.slugPrefix() + "-" + variant.slugSuffix();
                String title = category.category() + ": " + variant.titleSuffix();
                seedPracticeTask(
                        tasks,
                        projectId--,
                        category.category(),
                        slug,
                        title,
                        variant.difficulty(),
                        practiceTaskDescription(category, variant));
            }
        }
    }

    private void seedPracticeTask(
            PentestTaskRepository tasks,
            long projectId,
            String category,
            String slug,
            String title,
            String difficulty,
            String description) {
        String commitSha = "00000000" + Integer.toHexString(description.hashCode()).replace("-", "0");
        commitSha = commitSha.substring(commitSha.length() - 8);
        String resolvedCommitSha = commitSha;
        PentestTask task = tasks.findBySlug(slug)
                .orElseGet(() -> new PentestTask(
                        projectId,
                        "local/" + slug,
                        title,
                        slug,
                        category,
                        difficulty,
                        240,
                        8080,
                        "/health",
                        null,
                        description,
                        "workspace:examples/pentest-tasks/generic-web",
                        "main",
                        resolvedCommitSha,
                        Integer.toHexString(description.hashCode())));
        task.updateFromManifest(
                title,
                slug,
                category,
                difficulty,
                240,
                8080,
                "/health",
                null,
                description,
                "workspace:examples/pentest-tasks/generic-web",
                "main",
                commitSha,
                Integer.toHexString(description.hashCode()),
                "Разбор примера");
        // API list returns only ARCHIVE / PROMOTED_FROM_STAND tasks (см. PentestTaskService.isSupportedTaskSource),
        // и V21 удаляет любые GITLAB-задачи. Помечаем сидовые лабораторные как архивные с READY-статусом,
        // чтобы они были видны в карточках модулей.
        task.markAsSeededArchive("workspace:examples/pentest-tasks/generic-web");
        tasks.save(task);
    }

    private String practiceTaskDescription(PracticeTaskSeed category, PracticeVariantSeed variant) {
        return """
                ### Тип задачи

                **%s**

                ### Цель

                Запустите учебный стенд по теме **%s** и выполните сценарий: %s

                ### Фокус проверки

                - зона риска: %s;
                - работайте по методике соответствующего модуля Академии веб-безопасности;
                - зафиксируйте только безопасные учебные payloads в рамках выделенного стенда.

                ### Что приложить в отчет

                %s
                """.formatted(
                variant.taskType(),
                category.category(),
                variant.objective(),
                category.focusArea(),
                variant.reportRequirement());
    }

    private record PracticeTaskSeed(String category, String slugPrefix, String focusArea) {
    }

    private record PracticeVariantSeed(
            String slugSuffix,
            String difficulty,
            String titleSuffix,
            String taskType,
            String objective,
            String reportRequirement) {
    }

    private String securityLesson(SecurityModuleSeed seed) {
        return """
                ## Цель модуля

                Этот материал помогает понять уязвимость `%s` на уровне модели угроз, кода, признаков в интерфейсе
                и безопасного исправления. Студент создает локальный стенд, доказывает проблему белым ящиком и затем
                проверяет чужие стенды черным ящиком.

                ## Краткое определение

                %s

                ## Где искать уязвимость

                Чаще всего проблема встречается в таких местах: %s. Не ограничивайтесь видимыми кнопками в интерфейсе:
                проверяйте реальные HTTP-запросы, параметры, заголовки, тело запроса, cookie, перенаправления и
                ответы сервера. Если действие меняет данные, раскрывает сведения или обращается к внешнему ресурсу,
                оно должно иметь явную проверку на сервере.

                ## Модель угроз

                1. Определите актив: учетная запись, отчет, заказ, токен, файл, лабораторный стенд или внутренняя сеть.
                2. Найдите границу доверия: браузер, сервер, база данных, шаблонизатор, XML-разборщик или сетевой клиент.
                3. Найдите источник ввода: путь, query-параметр, тело запроса, заголовок, cookie, загруженный файл.
                4. Проверьте, может ли пользователь изменить поведение сервера, обойти право или прочитать лишние данные.
                5. Оцените влияние: конфиденциальность, целостность, доступность, обход процесса, расширение прав.

                ## Уязвимый пример кода

                ```javascript
                %s
                ```

                ## Разбор примера

                %s Суть проблемы не в конкретном языке, а в нарушении инварианта: данные из недоверенного источника
                используются как доверенные. В отчете важно показать не только полезную нагрузку, но и строку кода,
                где теряется граница доверия.

                ## Признаки при проверке черным ящиком

                - разные ответы на похожие запросы;
                - изменение статуса, тела ответа или времени ответа при изменении одного параметра;
                - доступ к объекту, который не должен принадлежать текущему пользователю;
                - выполнение действия без подтверждения или без нужной роли;
                - появление в ответе данных, которые пришли из пользовательского ввода;
                - сервер делает запрос, рендерит шаблон или разбирает XML по данным пользователя.

                ## Методика ручной проверки

                Начните с безопасной базовой проверки. Сохраните исходный запрос, измените только один параметр,
                сравните результат и повторите проверку на другом объекте или другом пользователе. Не используйте
                разрушительные действия, не меняйте реальные данные и не выходите за границы назначенного стенда.

                ```text
                1. Выполните обычное действие в приложении.
                2. Повторите запрос с измененным параметром.
                3. Сравните статус ответа, тело ответа и побочные эффекты.
                4. Зафиксируйте минимальное доказательство.
                5. Опишите, какая проверка отсутствует на сервере.
                ```

                ## Полезные нагрузки для лаборатории

                ```text
                %s
                ```

                Эти значения предназначены только для учебного стенда. В реальной системе используйте минимальные
                безопасные проверки и согласованный объем тестирования.

                ## Разновидности уязвимости

                %s

                ## Методика эксплуатации в учебном стенде

                %s

                ## Инструменты для практики

                %s

                ## Примеры HTTP-запросов

                ```http
                %s
                ```

                ## Типовые ошибки при исправлении

                - исправление только на стороне интерфейса без серверной проверки;
                - фильтрация одной строки вместо защиты конкретного контекста;
                - разные сообщения для чужого и несуществующего объекта;
                - отсутствие негативного теста после исправления;
                - логирование секретов или полезной нагрузки без маскирования;
                - доверие заголовкам и параметрам, которые полностью контролирует пользователь.

                ## Матрица проверки

                | Проверка | Что подтвердить |
                | --- | --- |
                | Обычный запрос | Приложение работает в штатном сценарии |
                | Измененный параметр | Видно отличие ответа или состояния |
                | Чужой пользователь | Действие не должно быть доступно без права |
                | Повтор запроса | Нет обхода через повтор или гонку |
                | Исправленная версия | Тот же сценарий больше не воспроизводится |

                ## Безопасное исправление

                ```javascript
                %s
                ```

                ## Почему исправление работает

                Исправление должно устранять корень проблемы, а не маскировать симптом. Например, скрытая кнопка
                во фронтенде не является контролем доступа, а фильтрация одной строки не заменяет контекстную защиту.

                ## Защитные меры

                - %s;
                - централизованная проверка прав и инвариантов на сервере;
                - безопасные настройки библиотек и фреймворков по умолчанию;
                - журналирование отказов доступа и подозрительных запросов без записи секретов;
                - негативные тесты на каждую найденную уязвимость;
                - минимизация данных в ответе и единые нейтральные сообщения об ошибках.

                ## Задание для белого ящика

                Создайте уязвимый стенд с одним основным endpoint по теме `%s`. В отчете покажите фрагмент кода,
                обычный запрос, измененный запрос, результат, влияние и исправленную версию. Приложение должно
                запускаться из Docker-архива, отвечать на `/health` и содержать только учебные данные.

                ## Задание для черного ящика

                Получите назначенный стенд другого студента. Найдите точку входа, выполните минимальную проверку,
                оформите доказательство и предложите исправление. Если гипотеза не подтвердилась, укажите это отдельно
                и не выдавайте ее за находку.

                ## Чек-лист отчета

                - название уязвимости и краткое влияние;
                - endpoint и метод запроса;
                - исходный запрос и измененный запрос;
                - наблюдаемый результат;
                - уязвимый фрагмент кода или объяснение поведения;
                - безопасное исправление;
                - негативный тест, который должен пройти после исправления.
                """.formatted(
                seed.title(),
                seed.definition(),
                seed.searchAreas(),
                seed.vulnerableCode(),
                seed.codeExplanation(),
                seed.payloads(),
                variantGuide(seed.title()),
                exploitationGuide(seed.title()),
                toolsGuide(seed.title()),
                httpExample(seed.title()),
                seed.safeCode(),
                seed.defense(),
                seed.title());
    }

    private String variantGuide(String title) {
        if (title.contains("SQL")) {
            return """
                    - логическая инъекция: проверка истинных и ложных условий;
                    - инъекция через объединение выборок: контролируемое добавление учебных строк в результат;
                    - инъекция в сортировку: подмена имени поля, когда разработчик не использует белый список;
                    - слепая инъекция: вывод не меняется явно, но меняется время ответа или косвенный признак;
                    - инъекция второго порядка: значение сохраняется, а выполняется позже в другом запросе.
                    """;
        }
        if (title.contains("сценариев")) {
            return """
                    - отраженная уязвимость: значение сразу возвращается в ответе;
                    - сохраненная уязвимость: значение сохраняется и отображается другим пользователям;
                    - уязвимость в структуре документа: ввод попадает в атрибут, ссылку или сценарий;
                    - уязвимость на стороне клиента: опасная обработка fragment, postMessage или данных из адреса;
                    - обход фильтра: смена контекста, кодировки или события элемента.
                    """;
        }
        if (title.contains("шаблоны")) {
            return """
                    - простое вычисление выражений;
                    - чтение переменных шаблона;
                    - обращение к объектам окружения шаблонизатора;
                    - обход фильтров через альтернативный синтаксис;
                    - смешение пользовательского шаблона и пользовательских данных.
                    """;
        }
        if (title.contains("серверных запросов")) {
            return """
                    - обращение к внутреннему адресу;
                    - обход через перенаправление;
                    - обход через альтернативную запись адреса;
                    - обращение к служебным метаданным;
                    - использование доверенного домена, который сам перенаправляет запрос.
                    """;
        }
        if (title.contains("подделка запроса")) {
            return """
                    - простая отправка формы с другого сайта;
                    - запрос с учетными данными браузера;
                    - обход через небезопасный метод GET;
                    - слабый или переиспользуемый токен;
                    - отсутствие проверки источника запроса.
                    """;
        }
        if (title.contains("CORS")) {
            return """
                    - отражение любого источника;
                    - доверие `null`-источнику;
                    - слишком широкий шаблон поддоменов;
                    - разрешение учетных данных для личных ответов;
                    - отсутствие заголовка `Vary: Origin` и смешивание кеша.
                    """;
        }
        if (title.contains("XML")) {
            return """
                    - внешняя сущность для чтения файла;
                    - внешняя сущность для сетевого запроса;
                    - раскрытие ошибки разборщика;
                    - атака через вложенный формат, который внутри содержит XML;
                    - чрезмерно большой XML, перегружающий разборщик.
                    """;
        }
        if (title.contains("бизнес")) {
            return """
                    - обход порядка шагов;
                    - повтор одноразового действия;
                    - отрицательные или слишком большие значения;
                    - гонка между двумя запросами;
                    - доверие цене, скидке, роли или статусу из клиента.
                    """;
        }
        if (title.contains("управления доступом")) {
            return """
                    - доступ к административному маршруту без роли;
                    - смена метода запроса;
                    - изменение роли или владельца в теле запроса;
                    - скрытая кнопка без серверной проверки;
                    - доступ к функции через прямой URL.
                    """;
        }
        return """
                - изменение идентификатора объекта;
                - перебор соседних идентификаторов;
                - доступ к чужому файлу;
                - доступ к объекту после смены пользователя;
                - различие ответа для чужого и несуществующего объекта.
                """;
    }

    private String exploitationGuide(String title) {
        return """
                1. Подготовьте два учебных пользователя и минимальный набор данных.
                2. Выполните штатный запрос и сохраните метод, путь, параметры, тело и важные заголовки.
                3. Измените только один элемент: параметр, идентификатор, источник, тело XML или значение формы.
                4. Сравните ответ: статус, тело, время, перенаправление, запись в базе или изменение состояния.
                5. Повторите проверку от имени второго пользователя, чтобы отделить ошибку доступа от ошибки валидации.
                6. Зафиксируйте минимальное доказательство без реальных секретов.
                7. Внесите исправление и выполните тот же запрос повторно как негативный тест.
                """;
    }

    private String toolsGuide(String title) {
        return """
                - встроенные инструменты разработчика браузера для просмотра запросов;
                - перехватывающий прокси для повторения и изменения HTTP-запросов;
                - `curl` или HTTP-клиент для точного воспроизведения запроса;
                - локальные журналы приложения и контейнера для подтверждения серверного поведения;
                - unit/integration-тесты, которые фиксируют исправление.
                """;
    }

    private String httpExample(String title) {
        if (title.contains("SQL")) {
            return "POST /login HTTP/1.1\\nContent-Type: application/json\\n\\n{\\\"email\\\":\\\"' OR '1'='1\\\",\\\"password\\\":\\\"test\\\"}";
        }
        if (title.contains("сценариев")) {
            return "GET /search?q=<img src=x onerror=alert(1)> HTTP/1.1\\nHost: lab.local";
        }
        if (title.contains("серверных запросов")) {
            return "GET /preview?url=http://127.0.0.1:8080/internal HTTP/1.1\\nHost: lab.local";
        }
        if (title.contains("подделка запроса")) {
            return "POST /profile/email HTTP/1.1\\nContent-Type: application/x-www-form-urlencoded\\n\\nemail=attacker@example.local";
        }
        if (title.contains("CORS")) {
            return "GET /api/me HTTP/1.1\\nHost: lab.local\\nOrigin: https://attacker.local";
        }
        if (title.contains("XML")) {
            return "POST /import HTTP/1.1\\nContent-Type: application/xml\\n\\n<!DOCTYPE x [<!ENTITY e SYSTEM \\\"file:///etc/hostname\\\">]><x>&e;</x>";
        }
        if (title.contains("управления доступом")) {
            return "POST /admin/users/42/role HTTP/1.1\\nContent-Type: application/json\\n\\n{\\\"role\\\":\\\"ADMIN\\\"}";
        }
        return "GET /api/reports/2 HTTP/1.1\\nCookie: session=student-one";
    }

    private LearningModule ensureModule(
            CourseRepository courses,
            LearningModuleRepository modules,
            String courseTitle,
            String courseDescription,
            String moduleTitle,
            String vulnerabilityTopic) {
        Course course = courses.findByTitle(courseTitle)
                .map(existing -> {
                    existing.updateDetails(courseDescription, CourseStatus.PUBLISHED);
                    return courses.save(existing);
                })
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

    private void upsertLesson(
            LessonRepository lessons,
            LearningModule module,
            int position,
            String title,
            String contentMarkdown) {
        Map<Integer, Lesson> byPosition = lessons.findByModuleIdOrderByPositionAsc(module.getId()).stream()
                .collect(Collectors.toMap(Lesson::getPosition, Function.identity(), (left, right) -> left));
        Lesson existing = byPosition.get(position);
        if (existing == null) {
            lessons.save(new Lesson(module, title, contentMarkdown, position));
        } else {
            existing.updateContent(title, contentMarkdown, position);
            lessons.save(existing);
        }
    }

    private record SecurityModuleSeed(
            String title,
            String topic,
            String definition,
            String searchAreas,
            String vulnerableCode,
            String codeExplanation,
            String safeCode,
            String defense,
            String payloads) {
    }

    private AppUser createUser(
            AppUserRepository users,
            PasswordEncoder passwordEncoder,
            String email,
            String rawPassword,
            String displayName,
            Role role) {
        return users.findByEmail(email)
                .orElseGet(() -> users.save(new AppUser(email, passwordEncoder.encode(rawPassword), displayName, role)));
    }
}
