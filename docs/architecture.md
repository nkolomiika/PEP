# Архитектура

## Обзор

PEP строится как модульный monorepo с Spring Boot backend, React frontend, worker-сервисом и
локальным Kubernetes runtime через `kind`. Пользовательские Docker images считаются недоверенными
и запускаются только через ограниченные Kubernetes resources.

```mermaid
flowchart TD
    Student[Студент] --> Web[React Frontend]
    Curator[Куратор] --> Web
    Admin[Администратор] --> Web

    Web --> Api[Spring Boot API]
    Api --> Db[(PostgreSQL)]
    Api --> Storage[(S3 Storage)]
    Api --> Queue[Очередь задач]

    Queue --> Worker[Image Validation Worker]
    Worker --> Registry[Local Registry]
    Worker --> K8s[kind Kubernetes API]

    K8s --> Labs[Изолированные Lab Pods]
    Api --> Observability[Метрики Логи Трейсы]
    Worker --> Observability
```

## Backend-слой

Backend отвечает за бизнес-логику и REST API.

Основные модули:

- identity и RBAC;
- курсы и учебные материалы;
- OWASP Top 10 модули;
- submissions и lifecycle технической проверки;
- отчеты и вложения;
- review и scoring;
- black box assignment distribution;
- lab orchestration facade;
- audit log и notifications.

Долгие операции оформляются как jobs и передаются worker-сервису через очередь.

## Frontend-слой

Frontend реализует русскоязычные сценарии для ролей:

- кабинет студента и просмотр курса;
- страницы Docker-курса;
- страницы OWASP Top 10 модулей;
- форма сдачи Docker image reference;
- редактор white box отчета;
- рабочее место black box тестирования;
- очередь проверок куратора;
- панель администратора для курсов, групп, дедлайнов и lab monitoring.

Рекомендуемые библиотеки:

- React Router для навигации;
- TanStack Query для server state;
- React Hook Form и Zod для форм;
- Markdown editor для отчетов;
- code viewer для примеров уязвимого кода.

## Worker-сервис

В MVP worker не собирает приложения из исходников. Он проверяет готовые Docker images и запускает
утвержденные работы в Kubernetes.

Worker выполняет:

- pull указанного image reference;
- проверку формата image reference;
- запуск контейнера в ограниченной среде;
- проверку startup timeout;
- проверку доступности указанного порта;
- проверку health endpoint, если он указан;
- создание Kubernetes resources для lab instances;
- обновление job status в backend.

Worker не доказывает наличие уязвимости автоматически. Смысловая проверка уязвимости остается за
куратором через white box отчет.

## Хранение данных

PostgreSQL хранит доменное состояние:

- пользователей, роли и группы;
- курсы, модули и уроки;
- OWASP topics;
- submissions и validation jobs;
- lab instances и black box assignments;
- reports, reviews и scores;
- audit events.

Object storage хранит крупные артефакты:

- вложения к отчетам;
- скриншоты и PoC-файлы;
- большие логи технической проверки;
- экспортированные результаты.

## Kubernetes runtime

Для дипломной демонстрации используется локальный Kubernetes через `kind`.

Рекомендуемая модель:

- локальный `kind` cluster;
- local registry для images студентов;
- namespace для платформы;
- namespace для лабораторий;
- Deployment на каждый lab instance;
- Service на каждый lab instance;
- Ingress или port-forward для локального доступа;
- NetworkPolicy для ограничения lateral movement;
- ResourceQuota и LimitRange;
- Pod Security Standards;
- TTL cleanup job.

## API-стиль

Основной API - REST. Для статусов технической проверки и lab runtime можно добавить SSE или
WebSocket после MVP.

Основные API группы:

- `/api/auth`
- `/api/users`
- `/api/groups`
- `/api/courses`
- `/api/modules`
- `/api/lessons`
- `/api/submissions`
- `/api/validation-jobs`
- `/api/labs`
- `/api/black-box-assignments`
- `/api/reports`
- `/api/reviews`
- `/api/admin`
- `/api/audit`

## Поток технической проверки image

```mermaid
sequenceDiagram
    participant Student as Студент
    participant Api as Backend API
    participant Queue as Очередь
    participant Worker as Validation Worker
    participant Registry as Local Registry
    participant K8s as kind API

    Student->>Api: Отправляет imageReference, port, healthPath
    Api->>Api: Создает Submission
    Api->>Queue: Создает ValidationJob
    Queue->>Worker: Передает job
    Worker->>Registry: Pull image
    Worker->>Worker: Запускает container с limits
    Worker->>Worker: Проверяет port и healthPath
    Worker->>Api: Обновляет статус ValidationJob
    Api->>Api: Переводит Submission в ReadyForReview
    Api->>K8s: После approval создает LabInstance
```

## Жизненный цикл submission

```mermaid
stateDiagram-v2
    [*] --> Draft
    Draft --> Submitted
    Submitted --> ValidationQueued
    ValidationQueued --> TechnicalValidation
    TechnicalValidation --> TechnicalValidationFailed
    TechnicalValidation --> ReadyForReview
    ReadyForReview --> Approved
    ReadyForReview --> Rejected
    ReadyForReview --> NeedsRevision
    NeedsRevision --> Submitted
    Approved --> PublishedForBlackBox
    PublishedForBlackBox --> Archived
```

## Жизненный цикл validation job

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

## Среды запуска

- Локально без Kubernetes: Docker Compose для backend, frontend и PostgreSQL.
- Локально с Kubernetes: `kind` cluster, local registry, platform namespace и lab namespace.
- Production: Kubernetes cluster с отдельными namespaces для платформы и lab workloads.

## Архитектурные решения

- Для локального запуска выбран `kind`, потому что он работает поверх Docker и подходит для Windows.
- Основной формат сдачи - Docker image reference.
- Backend не выполняет Docker/Kubernetes операции внутри request handler.
- Проверка image и deploy выполняются асинхронно.
- Backend является источником истины для статусов и прав доступа.
- Worker получает минимальные права и управляет только lab resources.
- Все важные действия администратора и куратора аудируются.
