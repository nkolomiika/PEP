# MVP backlog

## Цель

Backlog разбивает MVP на проверяемые задачи. Он дополняет roadmap и помогает контролировать
готовность дипломной демонстрации.

## P0. Обязательные задачи для защиты

### P0.1 Авторизация и роли

- Создать роли `STUDENT`, `CURATOR`, `ADMIN`.
- Реализовать вход пользователя.
- Ограничить доступ к endpoint по ролям.
- Добавить базовые route guards на frontend.

Готово, когда студент, куратор и администратор видят разные сценарии.

### P0.2 Курсы и уроки

- Создать курс OWASP Top 10.
- Создать Docker intro module.
- Создать модуль `A03. Injection`.
- Отображать уроки на русском языке.
- Хранить progress студента.

Готово, когда студент может открыть Docker-урок и урок SQL Injection.

### P0.3 Submission через Docker image

- Реализовать форму `imageReference`, `applicationPort`, `healthPath`.
- Создать `Submission`.
- Создать `ValidationJob`.
- Показать статус technical validation.

Готово, когда студент отправляет image reference и видит статус проверки.

### P0.4 Technical validation worker

- Получать validation job из очереди.
- Выполнять pull image.
- Запускать container с timeout.
- Проверять port.
- Проверять health endpoint, если указан.
- Сохранять logs и status.

Готово, когда валидный image получает `PASSED`, а невалидный получает `FAILED`.

### P0.5 Lab runtime через kind

- Поднять `kind` cluster.
- Поднять local registry.
- Создать lab namespace.
- Запустить lab deployment.
- Открыть lab через port-forward.
- Показать resource limits.

Готово, когда lab доступен локально и виден через `kubectl`.

### P0.6 White box review

- Куратор видит очередь white box отчетов.
- Куратор открывает отчет.
- Куратор принимает, отклоняет или возвращает на доработку.
- Решение попадает в audit log.

Готово, когда approved submission может стать lab target.

### P0.7 Black box distribution

- Администратор запускает distribution.
- Система назначает до трех чужих labs студенту.
- Система не назначает собственный lab.
- Результат сохраняется.

Готово, когда студент видит назначенные чужие цели, а повторный запуск distribution не создает дубликаты.

### P0.8 Black box report и scoring

- Студент отправляет black box отчет.
- Куратор проверяет отчет.
- Куратор выставляет score.
- Студент видит feedback.

Готово, когда модуль закрывается с итоговой оценкой.

## P1. Желательные задачи

- Markdown preview для отчетов.
- Загрузка вложений к отчетам.
- Экспорт оценок.
- Фильтры в очереди куратора.
- Admin dashboard по состоянию labs.
- Базовые графики по progress.

## P2. После защиты

- Image scanning.
- Dependency scanning.
- Полноценный ingress вместо port-forward.
- SSE/WebSocket для live statuses.
- Расширение всех OWASP Top 10 тем до полноценных labs. Базовый seed `A01`-`A10` с теорией, white box guidance и black box checklist готов; `A03. Injection` остается полным demo-flow модулем.
- Интеграция с внешним LMS.
- Production monitoring и alerting.

## Definition of Done для MVP

- Все P0 flow работают без ручных изменений в базе данных.
- Все пользовательские тексты интерфейса на русском.
- Technical validation не пытается автоматически доказывать уязвимость.
- Lab запускается локально через `kind`.
- Куратор может проверить оба типа отчетов.
- Администратор может показать audit log и Kubernetes resource limits.
