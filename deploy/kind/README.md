# Локальный Kubernetes через kind

## Создание кластера

```powershell
kind create cluster --config deploy/kind/cluster.yaml
```

## Проверка

```powershell
kubectl cluster-info --context kind-pep-local
kubectl get nodes
```

## Применение baseline-манифестов

```powershell
kubectl apply -f deploy/k8s/lab-security-baseline.yaml
kubectl apply -f deploy/k8s/lab-template.yaml
```

## Удаление кластера

```powershell
kind delete cluster --name pep-local
```
