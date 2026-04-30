# Доменная модель

## Основные сущности

```mermaid
erDiagram
    USER ||--o{ GROUP_MEMBER : joins
    GROUP ||--o{ GROUP_MEMBER : contains
    COURSE ||--o{ MODULE : contains
    MODULE ||--o{ LESSON : contains
    MODULE ||--o{ SUBMISSION : receives
    USER ||--o{ SUBMISSION : creates
    SUBMISSION ||--o{ VALIDATION_JOB : has
    SUBMISSION ||--o| LAB_INSTANCE : publishes
    SUBMISSION ||--o{ REPORT : documents
    USER ||--o{ REPORT : writes
    REPORT ||--o{ REVIEW : reviewed_by
    USER ||--o{ REVIEW : performs
    LAB_INSTANCE ||--o{ BLACK_BOX_ASSIGNMENT : assigned_as_target
    USER ||--o{ BLACK_BOX_ASSIGNMENT : receives
```

## Описание сущностей

### User

Аккаунт пользователя платформы.

Поля:

- `id`
- `email`
- `passwordHash`
- `displayName`
- `role`
- `status`
- `createdAt`
- `updatedAt`

### Group

Учебная группа или поток.

Поля:

- `id`
- `name`
- `startsAt`
- `endsAt`
- `status`

### Course

Учебная программа.

Поля:

- `id`
- `title`
- `description`
- `status`
- `createdAt`
- `updatedAt`

### Module

Учебный модуль, обычно рассчитанный примерно на одну неделю.

Поля:

- `id`
- `courseId`
- `title`
- `vulnerabilityTopic`
- `startsAt`
- `submissionDeadline`
- `blackBoxStartsAt`
- `blackBoxDeadline`
- `status`

### Lesson

Страница теоретического или практического материала на русском языке.

Поля:

- `id`
- `moduleId`
- `title`
- `contentMarkdown`
- `orderIndex`
- `lessonType`
- `published`

### Submission

Сдача уязвимого приложения студентом. Основной формат MVP - готовый Docker image reference.

Поля:

- `id`
- `moduleId`
- `studentId`
- `imageReference`
- `applicationPort`
- `healthPath`
- `status`
- `createdAt`
- `submittedAt`
- `approvedAt`

Жизненный цикл:

```mermaid
stateDiagram-v2
    [*] --> Draft
    Draft --> Submitted
    Submitted --> ValidationQueued
    ValidationQueued --> TechnicalValidation
    TechnicalValidation --> ReadyForReview
    TechnicalValidation --> TechnicalValidationFailed
    ReadyForReview --> Approved
    ReadyForReview --> Rejected
    ReadyForReview --> NeedsRevision
    NeedsRevision --> Submitted
    Approved --> PublishedForBlackBox
    PublishedForBlackBox --> Archived
```

### ValidationJob

Асинхронная техническая проверка Docker image. В MVP это validation job, а не сборка из исходников.

Поля:

- `id`
- `submissionId`
- `imageReference`
- `status`
- `logsUri`
- `errorMessage`
- `createdAt`
- `startedAt`
- `finishedAt`

Жизненный цикл:

```mermaid
stateDiagram-v2
    [*] --> QUEUED
    QUEUED --> PULLING_IMAGE
    PULLING_IMAGE --> STARTING_CONTAINER
    STARTING_CONTAINER --> CHECKING_PORT
    CHECKING_PORT --> CHECKING_HEALTH
    CHECKING_PORT --> PASSED
    CHECKING_HEALTH --> PASSED
    PULLING_IMAGE --> FAILED
    STARTING_CONTAINER --> FAILED
    CHECKING_PORT --> FAILED
    CHECKING_HEALTH --> FAILED
```

### LabInstance

Запущенный lab в локальном Kubernetes через `kind`.

Поля:

- `id`
- `submissionId`
- `namespace`
- `deploymentName`
- `serviceName`
- `routeUrl`
- `status`
- `expiresAt`
- `createdAt`
- `updatedAt`

Жизненный цикл:

```mermaid
stateDiagram-v2
    [*] --> Pending
    Pending --> Deploying
    Deploying --> Running
    Deploying --> Failed
    Running --> Unhealthy
    Unhealthy --> Running
    Running --> Stopping
    Failed --> Stopping
    Stopping --> Stopped
    Stopped --> Archived
```

### BlackBoxAssignment

Назначенная студенту цель для black box тестирования.

Поля:

- `id`
- `moduleId`
- `studentId`
- `targetLabInstanceId`
- `status`
- `assignedAt`
- `completedAt`

Правила:

- `studentId` не должен совпадать с владельцем target submission.
- Студент получает три assignments, если достаточно targets.
- Targets распределяются по возможности равномерно.

### Report

White box или black box отчет.

Поля:

- `id`
- `authorId`
- `moduleId`
- `submissionId`
- `blackBoxAssignmentId`
- `type`
- `title`
- `contentMarkdown`
- `status`
- `submittedAt`
- `updatedAt`

Жизненный цикл:

```mermaid
stateDiagram-v2
    [*] --> Draft
    Draft --> Submitted
    Submitted --> InReview
    InReview --> Approved
    InReview --> Rejected
    InReview --> NeedsRevision
    NeedsRevision --> Submitted
```

### Review

Проверка, комментарии и оценка куратора.

Поля:

- `id`
- `reportId`
- `curatorId`
- `decision`
- `score`
- `commentMarkdown`
- `createdAt`
- `updatedAt`

## Основные инварианты

- Только approved submissions могут стать black box targets.
- Студент не может тестировать собственный lab.
- Отчеты редактируются автором только в статусах draft или needs revision.
- Решения куратора неизменяемы после финального закрытия модуля, кроме audit admin actions.
- Lab instances должны иметь TTL и resource limits.
- Каждое security-sensitive действие должно создавать audit event.
