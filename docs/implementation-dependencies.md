# Implementation dependencies

## Порядок использования документов

1. `requirements.md`, `mvp-scope-boundaries.md`.
2. `user-stories.md`, `rbac-permissions.md`.
3. `api-contract.md`, `openapi-plan.md`, `status-and-errors.md`.
4. `database-schema-plan.md`, `domain-model.md`.
5. `frontend-route-map.md`, `ui-content-guidelines.md`.
6. `local-kind.md`, `sample-vulnerable-app.md`, `queue-retry-policy.md`.
7. `testing-and-demo.md`, `traceability-matrix.md`, `defense-presentation.md`.

## P0 dependencies

```text
auth -> courses -> lessons -> submissions -> validation jobs -> reports/reviews -> labs -> distribution -> scoring/audit
```

## Backend blockers

- RBAC matrix;
- API contract;
- status and errors catalog;
- database schema plan;
- privacy rules;
- audit event catalog.

## Frontend blockers

- frontend route map;
- UI content guidelines;
- status labels;
- RBAC route guards;
- accessibility checklist.

## Worker and infra blockers

- sample vulnerable app plan;
- queue/retry policy;
- local kind guide;
- configuration/secrets plan;
- observability plan.

## Milestone checklist

- M1: auth, roles, seed users.
- M2: courses, lessons, Russian content.
- M3: submissions and technical validation.
- M4: white box review.
- M5: kind lab runtime.
- M6: black box assignments and reports.
- M7: audit, notifications, demo readiness.
