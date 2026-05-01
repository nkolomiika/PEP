# PEP - Практическая образовательная платформа по информационной безопасности

PEP - образовательная платформа для студентов и начинающих специалистов в red team / pentest.

Платформа поддерживает полный практический цикл обучения:

- изучение теории и примеров уязвимого кода на русском языке;
- прохождение вводного курса по Docker;
- создание уязвимого веб-приложения;
- загрузка архива стенда с `Dockerfile`/`docker-compose.yml` или fallback-публикация готового Docker image;
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

## Быстрый локальный запуск demo-контура

Для локальной демонстрации backend, frontend, PostgreSQL, registry и toolbox запускаются через Docker
Compose. В этом режиме явно включены demo data и demo validation worker:

```powershell
docker compose up --build
```

После запуска:

- frontend HTTPS: `https://localhost:5443`
- frontend HTTP fallback: `http://localhost:5173`
- backend API: `http://localhost:8080`
- health endpoint: `http://localhost:8080/actuator/health`

Локальный HTTPS использует self-signed certificate, поэтому браузер покажет предупреждение. Для
demo-запуска `compose.yaml` задает `PEP_DEMO_DATA_ENABLED=true`, чтобы создать учебные аккаунты и
курсы. В production этот флаг по умолчанию выключен.

Frontend больше не содержит demo-пароли в bundle. Вход выполняется через `/api/auth/login`, backend
создает server-side session и устанавливает `HttpOnly` cookie `PEP_SESSION`. Перед login и
любыми небезопасными запросами frontend получает CSRF token через `/api/auth/csrf` и отправляет
его в `X-XSRF-TOKEN`. Для demo входа
используйте seed-аккаунты: `student1@pep.local`, `student2@pep.local`, `curator@pep.local`,
`admin@pep.local`.

Auth-события пишутся в audit trail: успешный login, logout, неверные попытки входа и срабатывание
rate limit. Для неуспешных попыток в metadata сохраняется hash email, а не raw password или введенный
секрет.

Frontend nginx отдает SPA через HTTPS redirect и добавляет production security headers: Content
Security Policy, HSTS, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` и
`Permissions-Policy`. Backend API также добавляет deny-by-default CSP для JSON endpoints.

В demo-режиме backend также запускает validation worker: он использует Docker socket хоста,
скачивает отправленный image, временно запускает container и проверяет port/health endpoint.

## Production defaults

Production-конфигурация не должна создавать demo-пользователей и учебные пароли автоматически.
По умолчанию backend использует:

- `PEP_DEMO_DATA_ENABLED=false` - demo seed отключен;
- `PEP_AUTH_BASIC_ENABLED=false` - HTTP Basic отключен, используется session cookie;
- `PEP_AUTH_CSRF_ENABLED=true` - CSRF-защита включена для cookie-based auth;
- `PEP_AUTH_SESSION_TTL_HOURS=8` - срок жизни server-side session;
- `PEP_AUTH_LOGIN_MAX_FAILED_ATTEMPTS=5`, `PEP_AUTH_LOGIN_FAILED_WINDOW_MINUTES=15`,
  `PEP_AUTH_LOGIN_LOCK_MINUTES=15` - защита login от brute force;
- `PEP_AUTH_CLEANUP_INTERVAL_MS=3600000`, `PEP_AUTH_CLEANUP_EXPIRED_RETENTION_HOURS=24`,
  `PEP_AUTH_CLEANUP_REVOKED_RETENTION_HOURS=24`,
  `PEP_AUTH_CLEANUP_THROTTLE_RETENTION_HOURS=24` - очистка inactive sessions и login throttle rows;
- `PEP_VALIDATION_WORKER_ENABLED=false` - worker включается только на выделенном runtime;
- `PEP_MULTIPART_MAX_FILE_SIZE=10MB` и `PEP_MULTIPART_MAX_REQUEST_SIZE=12MB` - явные лимиты upload;
- внешние secret values для database credentials, TLS и storage paths.

Для production deployment нужно создать первого администратора отдельной миграцией, admin CLI или
интеграцией с корпоративным identity provider. Demo-аккаунты `*@pep.local` не должны попадать в
production database.

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
docker compose exec k8s-toolbox pep-ingress-install
docker compose exec k8s-toolbox pep-lab-deploy <submissionId> localhost:5001/vulnerable-sqli-demo:latest 8080
```

Открыть lab через ingress URL:

```text
http://lab-<submissionId-prefix>.127.0.0.1.nip.io:8088
```

Port-forward остается fallback-вариантом:

```powershell
docker compose exec k8s-toolbox pep-lab-forward <submissionId> 8080 18080
```

Подробный сценарий описан в `docs/local-kind.md`.

## Демонстрационный сценарий

1. Администратор создает курс OWASP Top 10 и модуль `A03. Injection`.
2. Студент проходит вводный Docker-курс.
3. Студент загружает архив проекта со стендом или отправляет готовый image reference.
4. Worker собирает archive submission в local registry и выполняет technical-only validation.
5. Студент отправляет white box отчет.
6. Куратор утверждает white box отчет.
7. Платформа запускает lab в `kind`.
8. Другой студент получает lab для black box тестирования.
9. Студент отправляет black box отчет.
10. Куратор выставляет баллы и комментарии.

## Примечание по планам Cursor

Исходные plan-файлы Cursor не являются частью runtime приложения и не редактируются при доработке
проекта. Актуальная проектная документация хранится в `docs/`.
