# Configuration and secrets

## Окружения

- `local-compose` - backend, frontend, PostgreSQL без Kubernetes.
- `local-kind` - lab runtime через containerized kind, `k8s-toolbox` и local registry.
- `test` - automated tests и contract checks.
- `production-ready target` - внешние secrets, managed storage, hardened ingress.

## Backend env vars

- `SPRING_DATASOURCE_URL`;
- `SPRING_DATASOURCE_USERNAME`;
- `SPRING_DATASOURCE_PASSWORD`;
- `JWT_SECRET`;
- `STORAGE_ENDPOINT`;
- `STORAGE_BUCKET`;
- `QUEUE_URL`;
- `APP_DEMO_DATA_ENABLED`.

## Frontend env vars

- `VITE_API_BASE_URL`;
- `VITE_APP_TITLE`;
- `VITE_DEMO_MODE`.

## Worker env vars

- `QUEUE_URL`;
- `KUBECONFIG_PATH`;
- `LOCAL_REGISTRY_URL`;
- `VALIDATION_TIMEOUT_SECONDS`;
- `LAB_NAMESPACE_PREFIX`;
- `STORAGE_ENDPOINT`;

## Toolbox env vars

- `KUBECONFIG=/kube/config`;
- `KIND_CLUSTER_NAME=pep-local`;
- `TOOLBOX_CONTAINER_NAME=pep-k8s-toolbox`;
- `REGISTRY_CONTAINER_NAME=pep-local-registry`.

## `.env.example`

Можно хранить:

- local hostnames;
- local ports;
- fake usernames;
- fake passwords for demo only with clear label.

Нельзя хранить:

- реальные passwords;
- JWT secrets;
- cloud access keys;
- registry tokens;
- kubeconfig production clusters.

## Safe defaults

- Demo secrets должны быть очевидно учебными.
- Network access для lab по умолчанию минимален.
- Debug logs не включаются вне local.
- Upload size ограничен.
- На host требуется только Docker; `kind` и `kubectl` запускаются внутри `k8s-toolbox`.

## Acceptance criteria

- Все required env vars документированы.
- Реальные credentials не попадают в repository.
- README ссылается на этот документ для настройки окружения.
