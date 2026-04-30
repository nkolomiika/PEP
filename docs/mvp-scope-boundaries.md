# MVP scope boundaries

## Входит в MVP защиты

- Роли Student, Curator, Admin.
- Русскоязычные учебные материалы.
- Docker intro course.
- OWASP Top 10 overview.
- Полный модуль `A03. Injection`.
- Submission через Docker image reference.
- Technical-only validation.
- White box report and review.
- Lab runtime через local `kind`.
- Black box assignment and report.
- Audit trail и базовые notifications.

## Не входит в MVP

- Автоматическое доказательство эксплуатации уязвимости.
- Source code upload и platform-side build.
- Multi-cluster lab runtime.
- Full image scanning.
- LMS integration.
- Production ingress hardening.
- Email/SSE/WebSocket notifications.
- Полная поддержка mobile UI.

## Критерии переноса в P1/P2

Задача переносится после защиты, если:

- не нужна для demo flow;
- не влияет на безопасность MVP;
- требует внешней инфраструктуры;
- повышает сложность без явной пользы для ВКР;
- может быть объяснена как production-ready развитие.

## Scope creep risks

- Попытка реализовать автоматическую exploit validation.
- Поддержка нескольких языков UI.
- Сложная оркестрация ingress вместо port-forward.
- Полный marketplace курсов до завершения P0 flow.
