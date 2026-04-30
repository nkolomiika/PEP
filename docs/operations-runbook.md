# Operations runbook

## Цель

Runbook описывает порядок локальной демонстрации и восстановления типовых сбоев. Он нужен для
подготовки к защите и повторяемого запуска MVP.

## Перед демонстрацией

Проверить:

- Docker Desktop запущен;
- `docker ps` выполняется без ошибки;
- `docker compose version` работает;
- `docker compose build k8s-toolbox` проходит;
- `docker compose build backend` проходит;
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

Backend container в demo-compose запускает technical validation worker. Worker использует Docker socket
хоста только для технической проверки image: `docker pull`, временный `docker run`, проверка port и
`healthPath`, затем cleanup временного container.

## Запуск toolbox и local registry

```powershell
docker compose up -d registry k8s-toolbox
docker compose ps registry k8s-toolbox
```

Ожидаемый результат: registry и toolbox containers находятся в состоянии `running`.

## Запуск kind cluster

```powershell
docker compose exec k8s-toolbox pep-kind-create
```

Ожидаемый результат: control-plane и worker node находятся в состоянии `Ready`.

## Подготовка demo image

```powershell
docker compose exec k8s-toolbox docker build -t vulnerable-sqli-demo:latest ./examples/vulnerable-sqli-demo
docker compose exec k8s-toolbox docker tag vulnerable-sqli-demo:latest localhost:5001/vulnerable-sqli-demo:latest
docker compose exec k8s-toolbox docker push localhost:5001/vulnerable-sqli-demo:latest
```

Image reference для UI:

```text
localhost:5001/vulnerable-sqli-demo:latest
```

После отправки этого image reference через UI validation job должен автоматически пройти статусы
`PULLING_IMAGE`, `STARTING_CONTAINER`, `CHECKING_PORT`, `CHECKING_HEALTH` и завершиться `PASSED`.

Локальная smoke-проверка до публикации:

```powershell
docker compose exec k8s-toolbox docker run -d --rm --name pep-vuln-smoke -p 8081:8080 vulnerable-sqli-demo:latest
Invoke-WebRequest http://localhost:8081/health
Invoke-WebRequest "http://localhost:8081/search?q=' OR '1'='1"
docker compose exec k8s-toolbox docker rm -f pep-vuln-smoke
```

## Запуск lab из backend metadata

```powershell
docker compose exec k8s-toolbox pep-lab-deploy <submissionId> localhost:5001/vulnerable-sqli-demo:latest 8080
```

`submissionId` берется из ответа `POST /api/labs` или из команды `deployCommand`, которую показывает
Admin dashboard. Script преобразует `localhost:5001/...` в internal registry reference
`pep-local-registry:5000/...`, создает namespace `pep-lab-<submissionId-prefix>`, deployment,
service, resource quota и limits.

Admin dashboard также показывает сводку lab runtime: сколько approved submissions готовы к lab,
сколько еще без lab instance, сколько labs в `RUNNING`, статусы созданных labs, namespace/service
metadata, команды deploy/port-forward и срок жизни lab.

Ожидаемый результат: lab pod находится в состоянии `Running`, а `kubectl get pods,svc -n
pep-lab-<submissionId-prefix>` показывает deployment и service.

Для удаления lab:

```powershell
docker compose exec k8s-toolbox pep-lab-delete <submissionId>
```

## Открытие lab

```powershell
docker compose exec k8s-toolbox pep-lab-forward <submissionId> 8080 18080
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

Симптом: `pep-kind-create` сообщает, что cluster уже есть или `kubectl` видит старое состояние.

Решение:

```powershell
docker compose exec k8s-toolbox pep-kind-delete
docker compose exec k8s-toolbox pep-kind-create
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
docker compose exec k8s-toolbox kubectl get svc -n pep-labs-example
netstat -ano | findstr 18080
```

### NetworkPolicy не демонстрируется локально

Причина: не каждый local CNI в `kind` одинаково поддерживает NetworkPolicy.

Решение: показать manifests и объяснить, что production-ready cluster должен использовать CNI с
поддержкой NetworkPolicy.

## Завершение демонстрации

```powershell
docker compose exec k8s-toolbox pep-kind-delete
docker compose down
```
