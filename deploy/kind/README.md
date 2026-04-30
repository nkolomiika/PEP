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

## Развертывание lab instance

```powershell
docker compose exec k8s-toolbox pep-lab-deploy <submissionId> localhost:5001/vulnerable-sqli-demo:latest 8080
docker compose exec k8s-toolbox pep-lab-forward <submissionId> 8080 18080
```

## Удаление кластера

```powershell
docker compose exec k8s-toolbox pep-kind-delete
```
