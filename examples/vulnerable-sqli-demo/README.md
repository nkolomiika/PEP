# vulnerable-sqli-demo

Демонстрационное уязвимое приложение для модуля `A03. Injection`.

## Endpoints

- `GET /health` - health check для technical validation.
- `GET /` - краткое описание приложения.
- `POST /login` - уязвимый login flow.
- `GET /search?q=` - уязвимый поиск.

## Тестовые учетные данные

- `alice@example.local` / `password`
- `bob@example.local` / `password`
- `admin@example.local` / `admin`

## SQL Injection payload

```text
' OR '1'='1
```

Пример проверки login:

```powershell
Invoke-WebRequest -Method POST -Uri http://localhost:8080/login -Body "email=' OR '1'='1&password=anything"
```

Пример проверки search:

```powershell
Invoke-WebRequest "http://localhost:8080/search?q=' OR '1'='1"
```

## Docker

```powershell
docker build -t vulnerable-sqli-demo:latest .
docker run --rm -p 8080:8080 vulnerable-sqli-demo:latest
```

Image reference для сдачи в UI:

```text
localhost:5001/vulnerable-sqli-demo:latest
```

Runtime reference внутри `kind`:

```text
pep-local-registry:5000/vulnerable-sqli-demo:latest
```
