# Roadmap разработки

## Этап 0. Аналитика и проектирование

Результаты:

- требования на русском языке;
- user stories;
- архитектурная схема;
- ERD;
- draft REST API;
- OpenAPI/Swagger plan;
- критерии оценивания;
- модель угроз;
- границы MVP;
- RBAC matrix;
- каталог статусов и ошибок;
- frontend route map;
- database schema plan;
- configuration/secrets plan;
- risk register;
- ADR-журнал;
- MVP backlog;
- структура ВКР;
- traceability matrix;
- release/handoff checklist;
- сценарий локальной демонстрации через `kind`.

## Этап 1. Основа проекта

Backend:

- Spring Boot application;
- подключение PostgreSQL;
- Flyway migrations;
- Spring Security foundation;
- роли и права;
- health и metrics endpoints;
- audit event model.
- notification model.

Frontend:

- Vite React TypeScript application;
- русскоязычная навигация;
- API client;
- authenticated layout;
- кабинеты студента, куратора и администратора.

DevOps:

- Dockerfiles;
- local Docker Compose;
- `kind` cluster configuration;
- local registry;
- Kubernetes namespace manifests;
- operations runbook для локальной демонстрации.
- configuration and secrets defaults.

## Этап 2. Курсы, Docker intro и OWASP Top 10

Backend:

- CRUD курсов;
- CRUD модулей;
- уроки;
- темы OWASP Top 10;
- примеры уязвимого кода;
- progress tracking.

Frontend:

- каталог курсов;
- просмотр урока;
- страницы Docker-курса;
- страницы OWASP-модулей;
- индикаторы прогресса.

Контент:

- вводный курс по Docker на русском;
- программа OWASP Top 10;
- первый полный модуль `A03. Injection`;
- шаблоны отчетов;
- grading rubrics для Docker image, white box и black box отчетов.
- правила версионирования и лицензирования контента.

## Этап 3. Сдача Docker image и white box отчеты

Backend:

- submission model с `imageReference`;
- port и health endpoint;
- white box report model;
- REST API contract для submissions и validation jobs;
- lifecycle статусов;
- базовая проверка куратора.
- единый каталог ошибок и русских UI labels.

Frontend:

- форма сдачи Docker image reference;
- редактор отчета;
- страница статуса технической проверки;
- очередь проверок куратора.

## Этап 4. Technical validation worker

Backend и worker:

- очередь задач;
- validation job lifecycle;
- pull Docker image;
- запуск container в ограниченной среде;
- проверка startup timeout;
- проверка доступности port;
- проверка health endpoint;
- сохранение логов.
- retry, idempotency и cleanup policy.

Не входит в MVP:

- сборка image из исходников;
- автоматическое доказательство уязвимости;
- автоматическое scoring уязвимости.

## Этап 5. Kubernetes lab runtime через kind

Worker и инфраструктура:

- создание локального `kind` cluster;
- подключение local registry;
- lab namespace setup;
- deployment/service/ingress или port-forward;
- resource limits;
- network policies;
- TTL cleanup;
- lab status reporting.
- демонстрационное приложение `vulnerable-sqli-demo`.

## Этап 6. Black box assignments

Backend:

- алгоритм распределения;
- сохранение assignments;
- запрет назначения собственной работы;
- балансировка назначений;
- audit log.
- notification events.

Frontend:

- список назначенных целей;
- страница запуска lab;
- редактор black box отчета.

## Этап 7. Review и scoring

Backend:

- чеклист проверки;
- scoring;
- комментарии;
- flow доработки отчета;
- экспорт результатов.

Frontend:

- интерфейс проверки куратора;
- отображение баллов;
- просмотр обратной связи.

## Этап 8. Администрирование и observability

Backend:

- управление группами;
- дедлайны;
- управление фазами модуля;
- admin override actions;
- audit search.

Инфраструктура:

- Prometheus metrics;
- Grafana dashboards;
- централизованные логи;
- alerts.
- traceability для demo checks.

## Этап 9. Production hardening

Security:

- повторный threat model review;
- image scanning;
- dependency scanning;
- validation allowlists;
- rate limiting;
- backup и restore;
- secret management;
- pentest самой платформы.

Reliability:

- идемпотентность worker jobs;
- retry policy;
- disaster recovery runbook;
- нагрузочное тестирование;
- настройка resource quotas.

## MVP для защиты

MVP считается готовым, когда работает сценарий:

1. Администратор создает курс и модуль `A03. Injection`.
2. Студент проходит Docker intro.
3. Студент изучает урок по SQL Injection на русском.
4. Студент публикует Docker image уязвимого приложения.
5. Студент отправляет image reference, port и white box отчет.
6. Worker выполняет техническую проверку image.
7. Куратор утверждает white box отчет.
8. Платформа запускает lab в локальном `kind`.
9. Администратор запускает black box distribution.
10. Второй студент получает цель и открывает lab.
11. Второй студент отправляет black box отчет.
12. Куратор выставляет баллы.
13. Администратор показывает audit log, lab status и Kubernetes limits.

Контрольные артефакты готовности: `docs/demo-data-plan.md`,
`docs/testing-and-demo.md`, `docs/traceability-matrix.md`,
`docs/defense-presentation.md`, `docs/release-handoff-checklist.md`.

## Предлагаемый 16-недельный график

| Недели | Фокус |
| --- | --- |
| 1-2 | Аналитика, архитектура, ERD, threat model |
| 3-4 | Backend и frontend foundation |
| 5-6 | Курсы, Docker intro, OWASP Top 10 |
| 7-8 | Submissions, reports, white box review |
| 9-10 | Validation worker, local registry, kind runtime |
| 11-12 | Black box distribution и reports |
| 13-14 | Admin, observability, hardening |
| 15-16 | Тестирование, operations runbook, структура ВКР, подготовка к защите |
