# Operations runbook

## Цель

Runbook описывает порядок локальной демонстрации и восстановления типовых сбоев. Он нужен для
подготовки к защите и повторяемого запуска MVP.

## Перед демонстрацией

Проверить:

- Docker Desktop запущен;
- `docker ps` выполняется без ошибки;
- `kubectl version --client` работает;
- `kind version` работает;
- ports `8080`, `5173`, `5001`, `18080` свободны;
- frontend build проходит;
- backend tests проходят.

## Запуск приложения без Kubernetes

```powershell
docker compose up --build
```

Проверка:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
```

Ожидаемый результат: backend возвращает health status.

## Запуск kind cluster

```powershell
kind create cluster --config deploy/kind/cluster.yaml
kubectl get nodes
```

Ожидаемый результат: control-plane и worker node находятся в состоянии `Ready`.

## Запуск local registry

```powershell
docker run -d --restart=always -p 5001:5000 --name pep-local-registry registry:2
docker network connect kind pep-local-registry
```

Если container уже существует:

```powershell
docker rm -f pep-local-registry
```

После удаления повторить запуск registry.

## Подготовка demo image

```powershell
docker build -t vulnerable-sqli-demo:latest ./examples/vulnerable-sqli-demo
docker tag vulnerable-sqli-demo:latest localhost:5001/vulnerable-sqli-demo:latest
docker push localhost:5001/vulnerable-sqli-demo:latest
```

Image reference для UI:

```text
localhost:5001/vulnerable-sqli-demo:latest
```

Локальная smoke-проверка до публикации:

```powershell
docker run --rm -p 8081:8080 vulnerable-sqli-demo:latest
Invoke-WebRequest http://localhost:8081/health
Invoke-WebRequest "http://localhost:8081/search?q=' OR '1'='1"
```

## Запуск lab manifests

```powershell
kubectl apply -f deploy/k8s/lab-security-baseline.yaml
kubectl apply -f deploy/k8s/lab-template.yaml
kubectl get pods -n pep-labs-example
```

Ожидаемый результат: lab pod находится в состоянии `Running`.

## Открытие lab

```powershell
kubectl port-forward -n pep-labs-example service/sample-student-lab 18080:8080
```

Открыть:

```text
http://localhost:18080
```

Проверить health endpoint:

```powershell
Invoke-WebRequest http://localhost:18080/health
```

## Что показать комиссии

- UI студента с Docker-курсом и модулем `A03. Injection`.
- Submission form с Docker image reference.
- Статус technical validation.
- White box отчет.
- Очередь проверки куратора.
- Запущенный pod в `kind`.
- Resource limits через `kubectl describe pod`.
- Black box assignment для второго студента.
- Black box отчет и score.
- Audit log.

## Типовые сбои

### Docker Desktop не запущен

Симптом: `docker ps` возвращает ошибку подключения.

Решение: запустить Docker Desktop и дождаться полной инициализации.

### kind cluster уже существует

Симптом: `kind create cluster` сообщает, что cluster уже есть.

Решение:

```powershell
kind delete cluster --name pep-local
kind create cluster --config deploy/kind/cluster.yaml
```

### Image не скачивается внутри kind

Симптом: pod в статусе `ImagePullBackOff`.

Проверить:

- image опубликован в local registry;
- registry подключен к network `kind`;
- image reference внутри Kubernetes указывает на доступный registry name.

### Port-forward не запускается

Симптом: port занят или service не найден.

Проверить:

```powershell
kubectl get svc -n pep-labs-example
netstat -ano | findstr 18080
```

### NetworkPolicy не демонстрируется локально

Причина: не каждый local CNI в `kind` одинаково поддерживает NetworkPolicy.

Решение: показать manifests и объяснить, что production-ready cluster должен использовать CNI с
поддержкой NetworkPolicy.

## Завершение демонстрации

```powershell
kind delete cluster --name pep-local
docker rm -f pep-local-registry
docker compose down
```
