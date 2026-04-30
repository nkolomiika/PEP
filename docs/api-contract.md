# API contract

## Цель

Документ описывает минимальный REST API для MVP. Он нужен как ориентир для backend/frontend
разработки и как часть проектирования ВКР.

## Общие правила

- Формат данных: JSON.
- Авторизация: authenticated requests для всех endpoint, кроме login и health.
- Права доступа: проверяются на уровне роли и конкретного объекта.
- Ошибки возвращаются в едином формате.

Пример ошибки:

```json
{
  "code": "ACCESS_DENIED",
  "message": "Недостаточно прав для выполнения операции"
}
```

## Auth

### `POST /api/auth/login`

Назначение: вход пользователя.

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
  "accessToken": "jwt",
  "refreshToken": "jwt",
  "role": "STUDENT"
}
```

### `POST /api/auth/logout`

Назначение: завершение сессии.

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

Назначение: отправить Docker image reference и white box отчет.

Request:

```json
{
  "moduleId": "uuid",
  "imageReference": "localhost:5001/vulnerable-sqli-demo:latest",
  "applicationPort": 8080,
  "healthPath": "/health",
  "whiteBoxReportMarkdown": "# SQL Injection\n..."
}
```

Response:

```json
{
  "submissionId": "uuid",
  "status": "VALIDATION_QUEUED",
  "validationJobId": "uuid"
}
```

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

## Reviews

### `GET /api/reviews/queue`

Роль: куратор.

Назначение: получить очередь отчетов на проверку.

### `POST /api/reviews`

Роль: куратор.

Назначение: принять решение по отчету.

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

Роль: студент, куратор или администратор.

Назначение: загрузить файл evidence к отчету через `multipart/form-data` поле `file`. Студент может
добавлять вложения только к своим отчетам; куратор и администратор видят вложения в очереди проверки.

### `POST /api/reports/black-box`

Роль: студент.

Назначение: отправить black box отчет.

## Audit

### `GET /api/audit/events`

Роль: администратор.

Назначение: просмотреть audit events.
