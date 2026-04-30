# PEP - Практическая образовательная платформа по информационной безопасности

PEP - образовательная платформа для студентов и начинающих специалистов в red team / pentest.

Платформа поддерживает полный практический цикл обучения:

- изучение теории и примеров уязвимого кода на русском языке;
- прохождение вводного курса по Docker;
- создание уязвимого веб-приложения;
- публикация готового Docker image и отправка его на техническую проверку;
- доказательство уязвимости в white box отчете;
- тестирование чужих приложений в black box формате;
- экспертная проверка отчетов и выставление баллов.

## Технологический стек

- Backend: Java, Spring Boot, Spring Security, Spring Data JPA
- Frontend: React, TypeScript, Vite
- База данных: PostgreSQL
- Хранилище: S3-compatible object storage
- Фоновые задачи: очередь сообщений и worker-сервисы
- Запуск лабораторий: `kind` внутри Docker, управление через `k8s-toolbox` container
- Наблюдаемость: OpenTelemetry, Prometheus, Grafana, Loki или ELK

## Структура репозитория

- `backend/` - Spring Boot API и доменная логика
- `frontend/` - React/TypeScript интерфейс
- `docs/` - требования, архитектура, доменная модель, безопасность и учебный план
- `examples/vulnerable-sqli-demo/` - демонстрационное приложение для модуля `A03. Injection`
- `deploy/k8s/` - Kubernetes-манифесты платформы и лабораторной среды

## Документация

Базовое проектирование:

- `docs/requirements.md`
- `docs/user-stories.md`
- `docs/architecture.md`
- `docs/domain-model.md`
- `docs/api-contract.md`
- `docs/openapi-plan.md`
- `docs/database-schema-plan.md`
- `docs/rbac-permissions.md`
- `docs/status-and-errors.md`

Учебный контент и оценивание:

- `docs/docker-course.md`
- `docs/owasp-top-10-course.md`
- `docs/grading-rubrics.md`
- `docs/learning-cycle.md`
- `docs/content-versioning-and-licensing.md`
- `docs/ui-content-guidelines.md`

Lab runtime, безопасность и эксплуатация:

- `docs/security-model.md`
- `docs/local-kind.md`
- `docs/sample-vulnerable-app.md`
- `docs/operations-runbook.md`
- `docs/queue-retry-policy.md`
- `docs/observability-plan.md`
- `docs/configuration-and-secrets.md`
- `docs/privacy-and-data.md`
- `docs/acceptable-use-and-ethics.md`

MVP, тестирование и защита:

- `docs/mvp-backlog.md`
- `docs/mvp-scope-boundaries.md`
- `docs/implementation-dependencies.md`
- `docs/frontend-route-map.md`
- `docs/accessibility-compatibility.md`
- `docs/notifications.md`
- `docs/demo-data-plan.md`
- `docs/traceability-matrix.md`
- `docs/ci-quality-gates.md`
- `docs/risk-register.md`
- `docs/documentation-readiness.md`
- `docs/release-handoff-checklist.md`
- `docs/architecture-decisions.md`
- `docs/roadmap.md`
- `docs/testing-and-demo.md`
- `docs/thesis-outline.md`
- `docs/defense-presentation.md`

## Быстрый локальный запуск приложения

Для запуска backend, frontend и PostgreSQL без Kubernetes:

```powershell
docker compose up --build
```

После запуска:

- frontend: `http://localhost:5173`
- backend API: `http://localhost:8080`
- health endpoint: `http://localhost:8080/actuator/health`

В этом режиме backend также запускает demo worker для technical validation: он использует Docker socket
хоста, скачивает отправленный image, временно запускает container и проверяет port/health endpoint.

## Запуск lab runtime через containerized kind

Для демонстрации запуска уязвимых приложений используется `kind`, но `kind`, `kubectl` и Docker CLI
запускаются из `k8s-toolbox` container. На хосте нужен только Docker Desktop/Engine с Docker Compose.

Запустить registry и toolbox:

```powershell
docker compose up -d registry k8s-toolbox
```

Создать `kind` cluster из toolbox container:

```powershell
docker compose exec k8s-toolbox pep-kind-create
```

Собрать и опубликовать demo image через toolbox:

```powershell
docker compose exec k8s-toolbox docker build -t vulnerable-sqli-demo:latest ./examples/vulnerable-sqli-demo
docker compose exec k8s-toolbox docker tag vulnerable-sqli-demo:latest localhost:5001/vulnerable-sqli-demo:latest
docker compose exec k8s-toolbox docker push localhost:5001/vulnerable-sqli-demo:latest
```

Развернуть lab для принятой submission:

```powershell
docker compose exec k8s-toolbox pep-lab-deploy <submissionId> localhost:5001/vulnerable-sqli-demo:latest 8080
```

Открыть lab через port-forward:

```powershell
docker compose exec k8s-toolbox pep-lab-forward <submissionId> 8080 18080
```

Подробный сценарий описан в `docs/local-kind.md`.

## Демонстрационный сценарий

1. Администратор создает курс OWASP Top 10 и модуль `A03. Injection`.
2. Студент проходит вводный Docker-курс.
3. Студент публикует Docker image уязвимого приложения в local registry.
4. Студент отправляет image reference, port и white box отчет.
5. Worker выполняет technical-only validation.
6. Куратор утверждает white box отчет.
7. Платформа запускает lab в `kind`.
8. Другой студент получает lab для black box тестирования.
9. Студент отправляет black box отчет.
10. Куратор выставляет баллы и комментарии.

## Примечание по планам Cursor

Исходные plan-файлы Cursor не являются частью runtime приложения и не редактируются при доработке
проекта. Актуальная проектная документация хранится в `docs/`.
