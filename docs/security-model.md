# Модель безопасности

## Главный принцип

Все Docker images, опубликованные студентами, считаются недоверенным кодом. Платформа не должна
полагаться на добросовестность автора, качество исходного кода или metadata image.

## Основные активы

- backend и база данных платформы;
- аккаунты и отчеты студентов;
- решения и оценки кураторов;
- артефакты object storage;
- images в registry;
- control plane локального `kind` cluster;
- lab network и routing;
- audit log.

## Угрозы

### Container escape

Риск: malicious lab image пытается выйти из container и получить доступ к host или cluster resources.

Меры:

- запрет privileged pods;
- запрет host PID, IPC и host network;
- запрет hostPath mounts;
- restricted Pod Security admission;
- seccomp profile;
- drop Linux capabilities;
- read-only root filesystem где возможно;
- non-root user где возможно.

### Lateral movement

Риск: один lab атакует другой lab, backend services или cluster services.

Меры:

- NetworkPolicy deny-by-default;
- ingress только через контролируемый route;
- ограничение egress;
- изоляция lab namespaces;
- разделение platform и lab namespaces;
- service accounts с минимальными правами.

### Resource exhaustion

Риск: student image потребляет слишком много CPU, RAM, storage или network.

Меры:

- ResourceQuota на lab namespace;
- LimitRange для containers;
- CPU/RAM requests и limits;
- max image size;
- startup timeout;
- lab TTL и cleanup;
- rate limits на submissions и validation jobs.

### Утечка данных

Риск: студент получает доступ к чужому отчету, submission или личности автора.

Меры:

- backend RBAC checks на каждом объекте;
- доступ к object storage через signed URLs или backend proxy;
- assignment-aware access checks;
- анонимизация автора в black box phase;
- audit events для чтения чувствительных admin data.

### Abuse technical validation pipeline

Риск: malicious image злоупотребляет technical validation pipeline.

Меры:

- isolated validation workers;
- отсутствие platform secrets в validation environment;
- strict startup timeout;
- cleanup после проверки;
- image scanning в production-ready версии;
- запрет privileged mode и host network.

### SSRF и internal scanning

Риск: lab apps или validation logic используются для сканирования внутренних сервисов.

Меры:

- не выполнять произвольные student-provided URLs из privileged network zones;
- проверять допустимые registry sources;
- ограничить worker egress;
- направлять lab access через controlled ingress;
- запретить lab workloads доступ к cluster-internal services.

### Подмена отчетов

Риск: студент меняет отчет после проверки или отправляет evidence по неназначенной цели.

Меры:

- versioning отчетов;
- immutable submitted snapshots;
- assignment ownership checks;
- status transitions только по ролям;
- audit log для edits и decisions.

## Kubernetes Baseline

Recommended default for lab pods:

```yaml
securityContext:
  runAsNonRoot: true
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop:
      - ALL
  seccompProfile:
    type: RuntimeDefault
```

Some student applications may require writable temporary directories. In that case, mount an
`emptyDir` volume only for explicitly allowed paths such as `/tmp`.

## NetworkPolicy baseline

Поведение по умолчанию:

- deny all ingress;
- deny all egress;
- allow ingress from ingress controller to target service port;
- allow DNS only when required;
- allow database egress only for intentionally provisioned lab databases.

## Audit events

События, которые нужно фиксировать:

- login and logout;
- role changes;
- course publication;
- submission creation;
- validation job status changes;
- lab deployment, restart and deletion;
- black box distribution start;
- manual assignment changes;
- report submission;
- curator decision;
- score updates;
- admin export.

## Security acceptance criteria

- Студент не может открыть черновик чужого отчета.
- Студент не может получить или открыть собственную black box assignment.
- Lab pod не может использовать privileged mode.
- Lab pod не может mount host paths.
- Lab pod не может напрямую обратиться к базе платформы.
- Failed validation не оставляет persistent temporary artifacts.
- Каждое решение куратора аудируется.
