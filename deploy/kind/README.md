# Локальный Kubernetes через containerized kind

Для MVP `kind` и `kubectl` запускаются не на host-машине, а внутри `k8s-toolbox` container.
На host нужен Docker Desktop/Engine с Docker Compose.

## Запуск toolbox и registry

```powershell
docker compose up -d registry k8s-toolbox
```

## Создание кластера

```powershell
docker compose exec k8s-toolbox pep-kind-create
```

## Проверка

```powershell
docker compose exec k8s-toolbox kubectl cluster-info
docker compose exec k8s-toolbox kubectl get nodes
```

## Применение baseline-манифестов

```powershell
docker compose exec k8s-toolbox pep-kind-apply
```

## Удаление кластера

```powershell
docker compose exec k8s-toolbox pep-kind-delete
```
