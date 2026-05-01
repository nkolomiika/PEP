# Тест-кейсы функционального покрытия

## Цель

Набор тест-кейсов покрывает не менее 95% ключевого функционала MVP: учебные материалы, Docker image
submission, technical validation, white box review, lab runtime, black box distribution, отчеты,
оценки, роли, live statuses и demo-инфраструктуру. Покрытие считается функциональным: проверяются
пользовательские сценарии и критические негативные ветки, а не только процент строк кода.

## Автоматизированные integration tests

### TC-A01. Demo seed и учебные материалы

Предусловия: приложение запущено с demo seed.

Шаги:

1. Войти как `student1@pep.local`.
2. Получить список курсов через `/api/courses`.
3. Проверить наличие курса Docker и курса `OWASP Top 10`.
4. Проверить, что OWASP содержит модули `A01`-`A10`.
5. Открыть уроки `A03. Injection`.
6. Открыть первый урок и отметить его изученным.

Ожидаемый результат: курсы и уроки доступны, progress создается только для студента.

### TC-A02. Docker image submission и technical validation

Шаги:

1. Студент отправляет image reference, port и health path.
2. Платформа создает submission со статусом `VALIDATION_QUEUED`.
3. Куратор видит validation job.
4. Куратор вручную завершает validation как passed или failed.

Ожидаемый результат: статус validation job и submission меняется согласно результату проверки.

### TC-A03. White box отчет, вложение и review

Шаги:

1. Студент создает white box отчет по своей submission.
2. Студент прикладывает evidence file разрешенного типа: `txt`, `md`, `pdf`, `png`, `jpg`, `jpeg`, `webp`.
3. Куратор открывает отчет и создает review с оценкой.
4. Студент видит feedback и оценку.

Ожидаемый результат: отчет хранит Markdown, список вложений, статус review и score. Вложения может
добавлять только автор отчета; скачать файл может автор, куратор и администратор; другой студент
получает `403 ACCESS_DENIED`. Успешное скачивание evidence попадает в audit trail.

### TC-A04. Lab runtime и black box distribution

Шаги:

1. Admin создает lab для approved submission.
2. Admin запускает распределение целей.
3. Студент получает чужой lab.
4. Повторный запуск distribution не создает дубликаты.

Ожидаемый результат: студент не получает собственный lab, назначений не больше трех, операция idempotent.

### TC-A05. Black box отчет и итоговый результат

Шаги:

1. Студент отправляет black box отчет по назначенной цели.
2. Куратор оценивает отчет.
3. Студент открывает итог модуля.
4. Curator/Admin экспортирует оценки CSV.

Ожидаемый результат: black box score учитывается в module result, CSV содержит студентов и статусы.

### TC-A06. RBAC и изоляция данных

Шаги:

1. Anonymous пользователь открывает private API.
2. Student пытается экспортировать оценки.
3. Curator пытается создать lab.
4. Admin пытается отметить урок как студент.
5. Student2 пытается открыть validation job или вложить файл в отчет Student1.
6. Curator пытается вложить файл в отчет студента.

Ожидаемый результат: private API требует auth и возвращает `401 AUTHENTICATION_REQUIRED`,
недоступные действия возвращают `403 ACCESS_DENIED`.

### TC-A07. Валидация и инварианты отчетов

Шаги:

1. Отправить submission с port вне диапазона `1`-`65535`.
2. Создать white box отчет с moduleId, не совпадающим с moduleId submission.
3. Создать black box отчет без assignment.
4. Создать black box отчет с assignment из другого модуля.
5. Загрузить attachment с опасным extension/MIME, например `exe`.

Ожидаемый результат: запросы отклоняются как `400 VALIDATION_FAILED`, неконсистентные оценки не создаются.

### TC-A08. Live statuses

Шаги:

1. Curator открывает `/api/live/status`.
2. UI подключается к `/api/live/status-stream`.
3. Создать submission/report/lab.
4. Проверить обновление счетчиков.

Ожидаемый результат: snapshot и SSE показывают актуальные счетчики с учетом роли пользователя.

## Manual smoke tests

### TC-M01. Frontend happy path

1. Открыть `https://localhost:5443`.
2. Ввести email и пароль demo-студента в форму входа.
3. Переключить модуль в selector.
4. Открыть урок, проверить Markdown preview.
5. Создать submission и white box отчет.

Ожидаемый результат: UI показывает русские тексты, empty/loading/error states и обновляет данные после действий.
Frontend не содержит hardcoded demo-пароли, вход выполняется через `/api/auth/login`, а дальнейшие
API-запросы используют `HttpOnly` session cookie и CSRF header `X-XSRF-TOKEN` для небезопасных
методов.
Login endpoint должен отклонять невалидный email и чрезмерно длинный password как
`400 VALIDATION_FAILED` до проверки учетных данных и throttle logic.

Критерий безопасности: повторные неверные попытки входа для той же пары email и remote address
ограничиваются ответом `429 AUTH_RATE_LIMITED`, при этом ответ не раскрывает, существует ли аккаунт.
Auth-события входа, выхода, неверного пароля и rate limit должны попадать в audit trail без raw
password и без хранения введенного email в открытом виде для неуспешных попыток.
Server-side sessions должны очищаться scheduled job: revoked/expired sessions удаляются после
retention, активная session остается пригодной для `GET /api/me`.
Login throttle records также должны очищаться по retention, чтобы старые failed attempts не накапливались
в production базе.
API и frontend edge должны отдавать production security headers: CSP, frame denial, referrer policy,
permissions policy и MIME sniffing protection.
Markdown preview должен рендериться без raw HTML injection: HTML отображается как текст, опасные
link schemes вроде `javascript:` не становятся кликабельными, внешние ссылки открываются с
`rel="noopener noreferrer"`.
Backend должен отклонять чрезмерно большие Markdown payloads: отчет больше 20 000 символов,
комментарий куратора больше 8 000 символов и title отчета больше 180 символов.

### TC-M02. Curator dashboard

1. Войти как куратор.
2. Проверить очередь отчетов.
3. Использовать фильтры по статусу, типу и email автора.
4. Оставить feedback с Markdown preview.
5. Скачать CSV export.

Ожидаемый результат: фильтры не скрывают доступные отчеты ошибочно, CSV скачивается.

### TC-M03. Admin dashboard и lab metadata

1. Войти как admin.
2. Проверить lab summary cards.
3. Создать lab для approved submission.
4. Проверить deploy, ingress install и port-forward commands.
5. Запустить black box distribution.

Ожидаемый результат: dashboard показывает runtime metadata и не создает дубликаты lab/distribution.

### TC-M04. Containerized demo contour

1. Запустить `docker compose up --build`.
2. Проверить backend health.
3. Проверить frontend.
4. Проверить registry.
5. Запустить toolbox-команды `pep-kind-up`, `pep-lab-deploy`, `pep-lab-forward` при наличии Docker.

Ожидаемый результат: приложение запускается без host-side Kubernetes, lab tooling работает из контейнера.

## Не покрывается автоматически

- Реальное наличие уязвимости в студенческом приложении: это проверяет куратор, automatic validation остается technical-only.
- Реальная эксплуатация чужих labs beyond smoke: выполняется вручную в рамках black box тестирования.
- Production monitoring и external LMS: это post-MVP scope.
