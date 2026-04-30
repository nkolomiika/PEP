# OpenAPI и contract testing

## Цель

OpenAPI specification должен быть технической версией markdown-контракта из
`docs/api-contract.md` и использоваться для Swagger UI, тестов и демонстрации backend.

## Источник контракта

`docs/api-contract.md` остается продуктовым источником требований. Реализация backend должна
генерировать OpenAPI через Springdoc или эквивалентный инструмент.

## MVP endpoints

В Swagger UI должны быть описаны:

- auth endpoints;
- courses and lessons;
- submissions;
- validation jobs;
- reports;
- reviews;
- labs;
- black box assignments;
- audit read endpoints для администратора.

## Требования к схемам

Каждый endpoint должен иметь:

- request schema;
- response schema;
- error schema;
- status codes;
- role/permission note;
- пример успешного и ошибочного ответа.

## Swagger UI на защите

Показать:

- создание submission с `imageReference`, `applicationPort`, `healthPath`;
- получение validation job status;
- отправку report;
- чтение audit events администратором.

## Contract testing checklist

- OpenAPI содержит все P0 endpoints из `docs/api-contract.md`.
- Backend tests проверяют status codes для happy path и access denied.
- Ошибки соответствуют `docs/status-and-errors.md`.
- Request validation соответствует обязательным полям.
- Breaking changes в API требуют обновления markdown-контракта и traceability matrix.
