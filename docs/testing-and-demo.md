# Стратегия тестирования и демонстрации

## Тесты backend

Обязательные уровни:

- unit tests для сервисов и статусных переходов;
- integration tests для repositories и migrations;
- API tests для role-based access control;
- tests для black box assignment distribution;
- tests для report lifecycle;
- tests для audit event creation;
- tests для validation job lifecycle.
- contract tests для основных REST endpoints.

Приоритетные сценарии:

- студент не может открыть черновик чужого отчета;
- куратор может проверить назначенные отчеты;
- администратор может запустить distribution;
- студент не может получить собственный lab;
- невалидный image не может стать lab instance;
- финальный отчет нельзя редактировать без возврата на доработку.
- API возвращает единый формат ошибок.
- RBAC matrix покрывает все P0 API groups.

## Тесты frontend

Обязательные уровни:

- component tests для форм и dashboard;
- validation tests для шаблонов отчетов;
- route guard tests для ролей;
- end-to-end tests для дипломного demo flow.

Приоритетные flow:

- вход студентом;
- открытие Docker-курса;
- сдача Docker image reference;
- заполнение white box отчета;
- открытие назначенного black box lab;
- отправка black box отчета;
- вход куратором и проверка отчетов.
- отображение критериев оценивания перед отправкой отчета.
- все user-facing статусы и ошибки показаны на русском языке.

## Инфраструктурные тесты

Обязательные проверки:

- backend health endpoint отвечает;
- frontend отдает static app;
- worker получает validation job;
- worker выполняет pull test image;
- lab pod стартует с resource limits;
- lab pod не может использовать privileged mode;
- lab namespace имеет default-deny NetworkPolicy;
- TTL cleanup удаляет expired labs;
- logs и metrics доступны.
- operations runbook позволяет повторить локальную демонстрацию с нуля.
- queue/retry policy покрывает retry exhaustion, worker crash и cleanup.

## Тесты безопасности

Обязательные проверки:

- broken access control для reports, submissions и assignments;
- validation image reference allowlist/format;
- SSRF checks для внешних URL, если они появятся в будущих версиях;
- rate limiting;
- Kubernetes policy checks;
- dependency и container image scanning;
- audit log integrity checks.

## Демонстрационный сценарий для защиты

Подготовка:

- создать admin, curator и двух student accounts;
- создать курс OWASP Top 10;
- опубликовать Docker intro module;
- опубликовать модуль `A03. Injection`;
- подготовить один валидный vulnerable app image;
- подготовить один невалидный image reference для демонстрации ошибки;
- поднять containerized `kind` cluster через `k8s-toolbox`.
- сверить готовность по `docs/demo-data-plan.md` и `docs/release-handoff-checklist.md`.

Живая демонстрация:

1. Администратор открывает dashboard и показывает курс.
2. Студент открывает Docker intro lesson и отмечает прохождение.
3. Студент открывает урок SQL Injection с примером уязвимого кода.
4. Студент отправляет Docker image reference, port и white box отчет.
5. Worker создает validation job и проверяет image технически.
6. Куратор проверяет и утверждает white box отчет.
7. Платформа поднимает lab в `kind`.
8. Администратор запускает black box distribution.
9. Второй студент открывает назначенный lab URL.
10. Второй студент отправляет black box отчет с payload и evidence.
11. Куратор проверяет и оценивает black box отчет.
12. Администратор показывает audit events, lab status и resource limits.

Подробный сценарий защиты и fallback описаны в `docs/defense-presentation.md`.
Связь требований, API, тестов и demo steps описана в `docs/traceability-matrix.md`.

## Чеклист приемки

- Полный student flow работает без ручных изменений в базе данных.
- Куратор завершает оба типа проверки.
- Администратор запускает black box distribution.
- Docker image проходит technical-only validation.
- Lab запускается в локальном `kind`.
- Kubernetes manifests демонстрируют isolation controls.
- Audit log фиксирует важные действия.
- Документация объясняет архитектуру, безопасность и локальный запуск.
- User stories, API contract и критерии оценивания согласованы с MVP-сценарием.
- MVP backlog содержит все обязательные P0 flow.
- Структура ВКР покрывает анализ, проектирование, реализацию, тестирование и заключение.
- Demo data, traceability matrix и presentation checklist готовы к защите.
