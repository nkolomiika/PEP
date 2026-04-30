# CI quality gates

## Backend

Blocking gates:

- `mvn -q test`;
- Flyway migrations validate;
- API contract tests for P0 endpoints;
- security tests for RBAC negative cases;
- no secrets in tracked files.

## Frontend

Blocking gates:

- TypeScript typecheck;
- production build;
- role guard smoke checks;
- русскоязычный UI smoke check;
- no unresolved TODO in user-facing text.

## Docs

Blocking gates:

- mandatory documents exist;
- README links to core docs;
- no outdated build terms in `docs/`;
- links to `examples/vulnerable-sqli-demo` are covered by `docs/sample-vulnerable-app.md`;
- status names match `docs/status-and-errors.md`.

## Container и kind

Local demo gates:

- backend image builds;
- frontend image builds;
- `k8s-toolbox` image builds;
- `docker compose config` passes;
- kind cluster can be created from `k8s-toolbox`;
- local registry container is reachable;
- vulnerable demo image can be pulled;
- lab pod reaches `Running`.

## Blocking failure

Failure blocks release candidate, если:

- tests fail;
- frontend build fails;
- migration does not apply;
- P0 endpoint undocumented;
- RBAC allows cross-user data access;
- demo lab cannot start and fallback artifacts are missing.
