# Структура ВКР

## Цель

Документ помогает связать разработку платформы с текстом выпускной квалификационной работы.
Он фиксирует, какие материалы из репозитория можно использовать в разделах диплома.

## Введение

Содержание:

- актуальность практического обучения информационной безопасности;
- проблема нехватки безопасных учебных сред для red team / pentest практики;
- цель работы;
- задачи работы;
- объект и предмет исследования;
- используемый стек: Java/Spring, React/TypeScript, Docker, Kubernetes через `kind`.

Артефакты:

- `docs/requirements.md`;
- `docs/learning-cycle.md`;
- `docs/mvp-scope-boundaries.md`.

## Глава 1. Анализ предметной области

Содержание:

- обзор форматов обучения: LMS, CTF, лабораторные стенды;
- отличие white box и black box практики;
- роль OWASP Top 10 в базовом обучении web security;
- требования к запуску недоверенных контейнеров;
- ограничения дипломного MVP.

Артефакты:

- `docs/owasp-top-10-course.md`;
- `docs/docker-course.md`;
- `docs/security-model.md`;
- `docs/acceptable-use-and-ethics.md`;
- `docs/privacy-and-data.md`.

## Глава 2. Проектирование платформы

Содержание:

- роли пользователей;
- user stories;
- архитектура системы;
- доменная модель;
- API contract;
- жизненный цикл submission, validation job, lab instance и report;
- модель угроз.

Артефакты:

- `docs/user-stories.md`;
- `docs/architecture.md`;
- `docs/domain-model.md`;
- `docs/api-contract.md`;
- `docs/openapi-plan.md`;
- `docs/database-schema-plan.md`;
- `docs/rbac-permissions.md`;
- `docs/status-and-errors.md`;
- `docs/security-model.md`;
- `docs/architecture-decisions.md`.

## Глава 3. Реализация

Содержание:

- backend на Spring Boot;
- frontend на React/TypeScript;
- хранение данных в PostgreSQL;
- техническая проверка Docker image;
- локальный запуск lab runtime через `kind`;
- Kubernetes-манифесты для lab isolation;
- русскоязычные учебные материалы.

Артефакты:

- `backend/`;
- `frontend/`;
- `deploy/k8s/`;
- `deploy/kind/`;
- `docs/local-kind.md`;
- `docs/sample-vulnerable-app.md`;
- `docs/frontend-route-map.md`;
- `docs/configuration-and-secrets.md`;
- `docs/queue-retry-policy.md`;
- `docs/observability-plan.md`.

## Глава 4. Тестирование и демонстрация

Содержание:

- backend tests;
- frontend build;
- infrastructure checks;
- security checks;
- демонстрационный сценарий защиты;
- критерии оценивания студенческих работ.

Артефакты:

- `docs/testing-and-demo.md`;
- `docs/grading-rubrics.md`;
- `docs/operations-runbook.md`;
- `docs/demo-data-plan.md`;
- `docs/traceability-matrix.md`;
- `docs/defense-presentation.md`;
- `docs/ci-quality-gates.md`;
- `docs/accessibility-compatibility.md`.

## Заключение

Содержание:

- достигнутые результаты;
- ограничения MVP;
- дальнейшее развитие production-ready версии;
- возможные улучшения: image scanning, полноценный ingress, мониторинг, расширение OWASP-модулей.

Артефакты:

- `docs/roadmap.md`;
- `docs/mvp-backlog.md`;
- `docs/risk-register.md`;
- `docs/release-handoff-checklist.md`;
- `docs/documentation-readiness.md`;
- `docs/content-versioning-and-licensing.md`;
- `docs/implementation-dependencies.md`.

## Список демонстрационных материалов

- скриншоты UI;
- вывод `docker compose exec k8s-toolbox kubectl get pods`;
- вывод `kubectl describe pod` с resource limits;
- пример white box отчета;
- пример black box отчета;
- audit log событий;
- диаграммы архитектуры и жизненных циклов;
- traceability matrix;
- release/handoff checklist;
- ADR-журнал ключевых решений.
