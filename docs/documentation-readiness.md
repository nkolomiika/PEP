# Documentation readiness

## Definition of Ready

Документ готов к использованию в реализации, если:

- указана цель;
- понятно, какой backlog/API/test/demo он поддерживает;
- нет противоречий с `requirements.md`;
- есть acceptance criteria или checklist;
- терминология совпадает с остальной документацией.

## Definition of Done

Документ готов к включению в ВКР, если:

- покрывает заявленный пробел;
- связан с README или roadmap;
- использует русскоязычные user-facing формулировки;
- не содержит старых build/source-upload терминов;
- имеет проверяемые критерии.

## Checklist реализации

- Архитектура описывает component boundaries.
- API contract связан с OpenAPI.
- Domain model связан с database schema.
- RBAC связан с tests.
- Demo flow связан с seed data.
- Risks имеют mitigation и fallback.

## Checklist ВКР

- Есть требования.
- Есть проектирование.
- Есть реализация или план реализации.
- Есть тестирование.
- Есть демонстрационный сценарий.
- Есть выводы и дальнейшее развитие.
