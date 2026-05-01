# Sample vulnerable app

## Назначение

`vulnerable-sqli-demo` - демонстрационное уязвимое приложение для модуля `A03. Injection`.
Оно нужно для локальной защиты, technical validation и black box тестирования.

Реализация находится в `examples/vulnerable-sqli-demo`.

## Минимальные endpoints

| Endpoint | Назначение |
| --- | --- |
| `GET /health` | Health check для validation worker |
| `GET /` | Простая стартовая страница |
| `POST /login` | Уязвимый login flow для SQL Injection |
| `GET /search?q=` | Уязвимый поиск для black box проверки |

## Тестовые данные

Пользователи приложения:

- `alice@example.local` / `password`;
- `bob@example.local` / `password`;
- `admin@example.local` / `admin`.

Данные являются демонстрационными и не должны совпадать с учетными записями платформы.

## Уязвимость

Уязвимый сценарий:

```text
POST /login
email=' OR '1'='1
password=anything
```

Ожидаемое поведение уязвимой версии:

- login bypass проходит;
- приложение показывает успешный вход;
- в отчете можно приложить payload и скриншот результата.

Ожидаемое поведение исправленной версии:

- payload не приводит к входу;
- запрос использует parameterized query.

## Docker requirements

Студент может отправить либо готовый image reference, либо архив проекта. Архив должен содержать
`Dockerfile` или `docker-compose.yml`; если используется compose, web-сервис можно указать в поле
`composeService`.

Стенд должен:

- запускаться без интерактивного ввода;
- слушать port `8080`;
- отвечать на `GET /health`;
- не требовать `--privileged`;
- не требовать `--network host`;
- не хранить реальные секреты.

User-facing reference:

```text
localhost:5001/vulnerable-sqli-demo:latest
```

Runtime reference внутри `kind`:

```text
pep-local-registry:5000/vulnerable-sqli-demo:latest
```

Для archive flow платформа сама соберет image вида:

```text
localhost:5001/student-lab-<submissionId>:latest
```

После создания lab стенд открывается по основному локальному домену:

```text
http://lab-<submissionId-prefix>.127.0.0.1.nip.io:8088
```

Дополнительно UI показывает красивый вариант `http://lab-<submissionId-prefix>.local.host`; для него
на Windows нужна hosts/DNS/proxy настройка.

## Acceptance criteria

- Image или загруженный archive flow проходят technical-only validation.
- Приложение можно открыть через ingress domain или port-forward fallback.
- Payload воспроизводим для white box и black box демонстрации.
- README приложения объясняет endpoint, port и тестовые данные.
