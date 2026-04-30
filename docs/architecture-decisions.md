# Architecture decisions

## ADR-001: локальный Kubernetes через containerized kind

Контекст: для защиты нужен повторяемый Kubernetes runtime без внешнего cloud и без установки
Kubernetes tooling на host.

Решение: использовать `kind`, local registry и `k8s-toolbox` container с Docker CLI, `kind` и
`kubectl`.

Альтернативы: локально установленный `kind`, minikube, k3d, cloud cluster.

Последствия: demo воспроизводим локально, Kubernetes-ноды и tooling запускаются как containers, но
toolbox получает доступ к Docker socket и не является production security pattern.

## ADR-002: submission через Docker image reference

Контекст: загрузка исходного кода и сборка image на стороне платформы повышают сложность и риски MVP.

Решение: студент публикует готовый Docker image и отправляет image reference.

Альтернативы: архив исходного кода, ссылка на git-репозиторий, прямая загрузка Dockerfile.

Последствия: backend и worker фокусируются на validation и lab runtime.

## ADR-003: technical-only validation

Контекст: автоматическое доказательство уязвимости требует exploit logic и может давать ложные выводы.

Решение: worker проверяет pull, запуск, port и health; уязвимость доказывает студент в отчете.

Альтернативы: автоматический exploit scanner, ручная проверка без worker.

Последствия: MVP проще и надежнее, качество уязвимости обеспечивается rubric и curator review.

## ADR-004: port-forward для MVP lab access

Контекст: ingress hardening требует дополнительных решений по TLS, routing и isolation.

Решение: для защиты использовать `kubectl port-forward` из `k8s-toolbox` container с публикацией
порта через Docker Compose.

Альтернативы: Ingress Controller, NodePort, LoadBalancer.

Последствия: доступ подходит для локальной защиты; production-ready доступ переносится после MVP.
