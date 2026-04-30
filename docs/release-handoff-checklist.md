# Release and handoff checklist

## Перед защитой

- Backend tests проходят.
- Frontend build проходит.
- README описывает запуск.
- Demo data подготовлены.
- Vulnerable demo image доступен.
- Containerized kind smoke test выполнен через `k8s-toolbox`.
- Audit trail для demo flow создан.
- Fallback screenshots и logs сохранены.

## Артефакты для научного руководителя и комиссии

- README.
- Requirements.
- Architecture.
- Domain model.
- API contract/OpenAPI plan.
- Testing and demo plan.
- Defense presentation plan.
- Traceability matrix.
- Risk register.
- Release notes.

## Запуск с нуля

1. Установить Docker Desktop.
2. Запустить local compose по README.
3. Запустить `registry` и `k8s-toolbox`.
4. Создать kind cluster по `docs/local-kind.md`.
5. Подготовить demo image через toolbox.
6. Загрузить demo data.
7. Пройти demo script из `docs/defense-presentation.md`.

## Backup demo data

Хранить:

- SQL seed или backend seed command;
- sample reports;
- expected image references;
- screenshots key pages;
- saved validation logs.

## Post-defense maintenance

Следующие задачи:

- заменить port-forward на ingress;
- добавить email/SSE notifications;
- расширить OWASP modules;
- добавить image scanning;
- усилить production secrets management.
