# Queue, retry и idempotency policy

## Validation job lifecycle

```text
QUEUED -> PULLING_IMAGE -> STARTING_CONTAINER -> CHECKING_PORT -> CHECKING_HEALTH -> PASSED
                                                               \-> FAILED
```

Terminal statuses: `PASSED`, `FAILED`.

## Retry policy

- `pull image`: до 2 повторов.
- `start container`: до 1 повтора.
- `port check`: без повтора после timeout.
- `health check`: до 2 повторов в пределах общего timeout.
- Повтор всей job вручную доступен администратору или через повторную submission студента.

Retry exhaustion переводит job в `FAILED` и сохраняет sanitized error.

## Timeouts

| Операция | Timeout |
| --- | --- |
| Pull image | 120 seconds |
| Start container/pod | 60 seconds |
| Port check | 30 seconds |
| Health check | 30 seconds |
| Cleanup | 60 seconds |

## Idempotency

- `submissionId` является ключом для создания активной validation job.
- Повторная technical validation не должна создавать две running job для одной submission.
- Lab deploy использует стабильное имя namespace/pod по `labInstanceId`.
- Повторный deploy сначала сверяет состояние ресурса, затем обновляет или пересоздает его.

## Worker crash recovery

При старте worker:

- находит jobs в non-terminal статусах старше timeout;
- помечает зависшие jobs как `FAILED` или возвращает в `QUEUED`, если операция безопасно повторяема;
- запускает cleanup временных containers/pods;
- пишет audit event `VALIDATION_JOB_RECOVERED`.

## Cleanup

Cleanup должен удалять:

- временные containers;
- временные pods;
- orphan namespaces;
- временные service resources;
- logs старше retention policy.

Cleanup failures не должны блокировать новые jobs, но должны быть видны в metrics и audit.
