# Observability plan

## Цель

Observability должен показывать состояние образовательного workflow, technical validation и lab
runtime без доступа к внутренней БД.

## Audit events

Обязательные события:

- `USER_LOGIN`;
- `COURSE_CREATED`, `COURSE_PUBLISHED`;
- `SUBMISSION_CREATED`;
- `VALIDATION_JOB_CREATED`, `VALIDATION_JOB_PASSED`, `VALIDATION_JOB_FAILED`;
- `REPORT_SUBMITTED`, `REVIEW_COMPLETED`;
- `LAB_CREATED`, `LAB_FAILED`, `LAB_STOPPED`;
- `BLACK_BOX_DISTRIBUTION_STARTED`, `BLACK_BOX_DISTRIBUTION_COMPLETED`;
- `ASSIGNMENT_CREATED`, `SCORE_ASSIGNED`.

## Backend metrics

- HTTP requests by method, path, status.
- HTTP error rate.
- Validation duration by result.
- Review queue size.
- Active lab count.
- Number of pending black box assignments.

## Worker metrics

- Jobs queued, running, passed, failed.
- Retry count by job.
- Image pull duration.
- Container startup duration.
- Port check failures.
- Health check failures.
- Cleanup failures.

## Lab runtime checks

- Kubernetes namespace exists.
- Pod status is `Running`.
- Restart count is below threshold.
- Resource requests/limits are set.
- Service or port-forward target is reachable.

## Logs

Logs must include:

- correlation id;
- user id or service identity;
- submission id, validation job id or lab id;
- status transition;
- sanitized error message.

Logs must not include passwords, tokens, full cookies or private evidence contents.

## Что показать на защите

- audit trail для demo flow;
- successful and failed validation job logs;
- список running labs;
- метрики очереди validation/review;
- пример correlation id от API request до worker action.
