# API contract

## Цель

Документ описывает минимальный REST API для MVP. Он нужен как ориентир для backend/frontend
разработки и как часть проектирования ВКР.

## Общие правила

- Формат данных: JSON.
- Авторизация: authenticated requests для всех endpoint, кроме login, CSRF token, overview и health.
- CSRF: все небезопасные методы (`POST`, `PUT`, `PATCH`, `DELETE`) должны отправлять header
  `X-XSRF-TOKEN`, полученный через `GET /api/auth/csrf`.
- Security headers: API responses должны включать deny-by-default `Content-Security-Policy`,
  `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer` и ограничительный `Permissions-Policy`.
- Права доступа: проверяются на уровне роли и конкретного объекта.
- Ошибки возвращаются в едином формате.

Пример ошибки:

```json
{
  "code": "ACCESS_DENIED",
  "message": "Недостаточно прав для выполнения операции"
}
```

Security layer использует тот же JSON-формат: unauthenticated private API возвращает
`401 AUTHENTICATION_REQUIRED`, authenticated-but-forbidden действия возвращают `403 ACCESS_DENIED`.

## Auth

### `GET /api/auth/csrf`

Назначение: выдать CSRF token для cookie-based authentication. Frontend вызывает endpoint перед
`POST /api/auth/login` и переиспользует token для последующих небезопасных запросов.

Response:

```json
{
  "token": "csrf-token",
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf"
}
```

### `GET /api/me`

Назначение: проверить текущие учетные данные и получить профиль пользователя. В текущей реализации
запрос защищен session cookie; frontend получает роль из этого endpoint.

Response:

```json
{
  "email": "student@example.com",
  "displayName": "Студент",
  "role": "STUDENT"
}
```

### `POST /api/auth/login`

Назначение: создать server-side session и установить `HttpOnly` cookie `PEP_SESSION`.
Требует CSRF header.
Валидация payload: `email` должен быть корректным email до 320 символов, `password` - непустая
строка до 256 символов.
Повторные ошибки входа ограничиваются по normalized email и remote address; при превышении лимита
endpoint возвращает `429 AUTH_RATE_LIMITED`.

Request:

```json
{
  "email": "student@example.com",
  "password": "password"
}
```

Response:

```json
{
  "email": "student@example.com",
  "displayName": "Студент",
  "role": "STUDENT"
}
```

### `POST /api/auth/logout`

Назначение: отозвать текущую server-side session и очистить cookie.
Требует CSRF header.

## Courses

### `GET /api/courses`

Назначение: получить список доступных курсов.

### `POST /api/courses`

Роль: администратор.

Назначение: создать курс.

### `GET /api/courses/{courseId}/modules`

Назначение: получить модули курса.

## Lessons

### `GET /api/modules/{moduleId}/lessons`

Назначение: получить уроки модуля.

### `GET /api/lessons/{lessonId}`

Назначение: открыть урок с Markdown-контентом и примерами кода.
Frontend обязан рендерить Markdown безопасно: без raw HTML injection и без кликабельных опасных
link schemes.

### `GET /api/modules/{moduleId}/lesson-progress`

Роль: студент.

Назначение: получить progress студента по урокам модуля.

### `GET /api/modules/{moduleId}/result`

Роль: студент.

Назначение: получить итог модуля по формуле `WhiteBox * 0.45 + BlackBox * 0.55`, если Docker-допуск
пройден и оба отчета проверены куратором.

### `POST /api/lessons/{lessonId}/complete`

Роль: студент.

Назначение: отметить урок как изученный.

## Submissions

### `POST /api/submissions`

Роль: студент.

Назначение: отправить готовый Docker image reference. Это fallback-режим для студентов, которые уже
собрали и опубликовали image в local registry.

Request:

```json
{
  "moduleId": "uuid",
  "imageReference": "localhost:5001/vulnerable-sqli-demo:latest",
  "applicationPort": 8080,
  "healthPath": "/health",
  "healthPath": "/health"
}
```

Response:

```json
{
  "id": "uuid",
  "sourceType": "IMAGE_REFERENCE",
  "imageReference": "localhost:5001/vulnerable-sqli-demo:latest",
  "runtimeImageReference": "localhost:5001/vulnerable-sqli-demo:latest",
  "status": "VALIDATION_QUEUED",
  "applicationPort": 8080,
  "healthPath": "/health"
}
```

### `POST /api/submissions/archive`

Роль: студент.

Назначение: загрузить архив проекта со стендом. Архив может содержать `Dockerfile` или
`docker-compose.yml`. Платформа сохраняет архив, ставит validation job в очередь, worker распаковывает
проект, собирает runtime image, публикует его в local registry и дальше запускает стандартную
technical-only validation.

Content type: `multipart/form-data`.

Поля:

- `moduleId` - UUID модуля;
- `archive` - `.zip`, `.tar`, `.tar.gz` или `.tgz`;
- `applicationPort` - порт web-сервиса внутри контейнера;
- `healthPath` - optional health endpoint, по умолчанию `/health`;
- `composeService` - optional имя web-сервиса из `docker-compose.yml`.

Response содержит `sourceType: "ARCHIVE"`, `archiveFilename`, `runtimeImageReference` после сборки,
а после создания lab - `publicUrl` и `localHostUrl`.

### `GET /api/submissions/{submissionId}`

Назначение: получить состояние сдачи.

### `GET /api/submissions/{submissionId}/validation-jobs`

Назначение: получить историю технических проверок.

## Validation jobs

### `GET /api/validation-jobs/{jobId}`

Назначение: получить статус technical-only validation.

Response:

```json
{
  "id": "uuid",
  "submissionId": "uuid",
  "status": "CHECKING_PORT",
  "logsUri": "s3://pep/logs/job.log",
  "errorMessage": null,
  "imageScanStatus": "WARNINGS",
  "imageScanSummary": "Image запускается от root; application port не объявлен в EXPOSE;",
  "imageScanReport": "Baseline image scan...",
  "dependencyScanStatus": "WARNINGS",
  "dependencyScanSummary": "SBOM/dependency labels не найдены; history содержит package install commands;",
  "dependencyScanReport": "Baseline dependency scan..."
}
```

Статусы:

- `QUEUED`;
- `PULLING_IMAGE`;
- `STARTING_CONTAINER`;
- `CHECKING_PORT`;
- `CHECKING_HEALTH`;
- `PASSED`;
- `FAILED`.

`imageScanStatus` не блокирует technical validation и показывает результат baseline image scan:
`PASSED`, `WARNINGS` или `FAILED`. MVP scan использует `docker image inspect`, проверяет запуск от root
и наличие `EXPOSE` для application port. Полноценный CVE scanner остается задачей production-ready
версии.

`dependencyScanStatus` не блокирует technical validation и показывает baseline dependency scan:
`PASSED`, `WARNINGS` или `FAILED`. MVP scan использует `docker image inspect` labels и
`docker image history`, чтобы подсветить отсутствие SBOM labels и package install commands.
Полноценный dependency/CVE scanner остается production-ready расширением.

## Live statuses

### `GET /api/live/status`

Роль: студент, куратор или администратор.

Назначение: получить snapshot live-счетчиков для текущей роли.

### `GET /api/live/status-stream`

Роль: студент, куратор или администратор.

Назначение: SSE stream с event `status`, который каждые 2 секунды отправляет тот же payload, что
`GET /api/live/status`. Demo frontend использует stream для live indicator и счетчиков dashboard.

## Reviews

### `GET /api/reviews/queue`

Роль: куратор.

Назначение: получить очередь отчетов на проверку.

### `POST /api/reviews`

Роль: куратор.

Назначение: принять решение по отчету.
Production limit: `commentMarkdown` до 8 000 символов.

Request:

```json
{
  "reportId": "uuid",
  "decision": "APPROVED",
  "score": 85,
  "commentMarkdown": "Уязвимость воспроизводится, отчет оформлен корректно."
}
```

### `GET /api/reviews`

Роль: студент, куратор, администратор.

Назначение: получить feedback по проверенным отчетам. Студент видит только reviews по своим отчетам,
куратор и администратор видят все reviews.

## Labs

### `POST /api/labs`

Роль: администратор или worker.

Назначение: создать lab instance для approved submission.

Response включает Kubernetes metadata и demo-команды:

```json
{
  "id": "uuid",
  "submissionId": "uuid",
  "namespace": "pep-lab-12345678",
  "deploymentName": "lab-12345678",
  "serviceName": "svc-12345678",
  "routeUrl": "http://localhost:18080",
  "ingressUrl": "http://lab-12345678.127.0.0.1.nip.io:8088",
  "deployCommand": "docker compose exec k8s-toolbox pep-lab-deploy <submissionId> <image> <port>",
  "ingressInstallCommand": "docker compose exec k8s-toolbox pep-ingress-install",
  "portForwardCommand": "docker compose exec k8s-toolbox pep-lab-forward <submissionId> <port> 18080",
  "status": "RUNNING"
}
```

### `GET /api/labs/{labId}`

Назначение: получить статус lab instance.

### `POST /api/labs/{labId}/restart`

Роль: администратор.

Назначение: перезапустить lab.

## Black box assignments

### `POST /api/modules/{moduleId}/black-box-assignments/distribute`

Роль: администратор.

Назначение: запустить распределение целей. Система назначает студенту до трех чужих labs в рамках
модуля, не назначает собственную lab и не создает дубликаты при повторном запуске.

### `GET /api/black-box-assignments/my`

Роль: студент.

Назначение: получить назначенные цели.

## Reports

### `GET /api/modules/{moduleId}/grades/export`

Роль: куратор или администратор.

Назначение: экспортировать оценки студентов по модулю в CSV. Колонки: `studentEmail`,
`displayName`, `dockerPassed`, `whiteBoxScore`, `blackBoxScore`, `finalScore`, `status`.

### `POST /api/reports`

Роль: студент.

Назначение: отправить white box или black box отчет в формате markdown.
Markdown считается пользовательским контентом и должен отображаться только безопасным renderer.
Production limit: `title` до 180 символов, `contentMarkdown` до 20 000 символов.

`ReportResponse` включает `attachments` - список метаданных загруженных вложений:

```json
[
  {
    "id": "uuid",
    "originalFilename": "sqli-evidence.txt",
    "contentType": "text/plain",
    "sizeBytes": 25,
    "uploadedAt": "2026-04-30T12:00:00Z"
  }
]
```

### `POST /api/reports/{reportId}/attachments`

Роль: студент.

Назначение: загрузить файл evidence к отчету через `multipart/form-data` поле `file`. Студент может
добавлять вложения только к своим отчетам; куратор и администратор видят вложения в очереди проверки.

### `GET /api/report-attachments/{attachmentId}`

Роль: студент, куратор или администратор.

Назначение: безопасно скачать файл evidence через backend. Автор отчета, куратор и администратор
получают файл с `Content-Disposition: attachment`; другой студент получает `403 ACCESS_DENIED`.
Успешное скачивание пишет audit event `REPORT_ATTACHMENT_DOWNLOADED` с `reportId` в metadata.

### `POST /api/reports/black-box`

Роль: студент.

Назначение: отправить black box отчет.

## Audit

### `GET /api/audit/events`

Роль: администратор.

Назначение: просмотреть audit events.

Auth-события, которые должны попадать в audit trail:

- `AUTH_LOGIN_SUCCESS` - успешный вход, actor равен пользователю, metadata содержит `remoteAddress`;
- `AUTH_LOGOUT` - завершение server-side session;
- `AUTH_LOGIN_FAILED` - неверные учетные данные, actor пустой, metadata содержит `emailHash`, `remoteAddress`, `locked`;
- `AUTH_LOGIN_RATE_LIMITED` - попытка входа во время блокировки throttle window.

Inactive server-side sessions очищаются scheduled cleanup job: expired sessions и revoked sessions
удаляются после настроенного retention, активные sessions не должны удаляться cleanup job.
Login throttle rows также очищаются scheduled cleanup job после отдельного retention, чтобы таблица
не росла из-за старых неуспешных попыток входа.
