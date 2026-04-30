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
  "errorMessage": null
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
  "deployCommand": "docker compose exec k8s-toolbox pep-lab-deploy <submissionId> <image> <port>",
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

### `POST /api/reports/black-box`

Роль: студент.

Назначение: отправить black box отчет.

## Audit

### `GET /api/audit/events`

Роль: администратор.

Назначение: просмотреть audit events.
