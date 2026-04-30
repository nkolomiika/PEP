# План презентации и demo script

## Структура слайдов

1. Тема и цель ВКР.
2. Проблема обучения практической безопасности.
3. Роли платформы: студент, куратор, администратор.
4. Архитектура: Spring Boot, React, PostgreSQL, worker, kind.
5. Учебный цикл: теория, white box, technical validation, black box, review.
6. Безопасность и изоляция lab runtime.
7. Demo flow.
8. Результаты MVP и дальнейшее развитие.

## Demo script на 5-7 минут

1. Открыть платформу и войти студентом.
2. Показать курс Docker и модуль `A03. Injection`.
3. Отправить submission с `localhost:5001/vulnerable-sqli-demo:latest`.
4. Показать technical validation job.
5. Отправить white box отчет с payload.
6. Войти куратором и принять отчет.
7. Войти администратором, показать lab runtime и запустить distribution.
8. Войти вторым студентом, открыть black box assignment и отправить отчет.
9. Показать review score и audit trail.

## Команды для демонстрации

```bash
docker ps
docker images
kind get clusters
kubectl get pods -A
kubectl get svc -A
kubectl port-forward svc/<lab-service> 8081:8080
```

## Ожидаемые вопросы

**Почему kind?** Для повторяемой локальной демонстрации Kubernetes без внешней инфраструктуры.

**Почему image reference, а не загрузка исходного кода?** Это снижает сложность MVP и фокусирует платформу
на technical validation и lab runtime.

**Почему нет автоматического доказательства уязвимости?** Для защиты достаточно technical-only
validation; доказательство уязвимости делает студент в отчете, а куратор проверяет результат.

**Как ограничивается риск недоверенных контейнеров?** Namespace, resource limits, non-root,
NetworkPolicy, Pod Security и audit.

## Fallback

Если kind недоступен, показать заранее подготовленные скриншоты, audit events, validation logs и
запуск vulnerable app через Docker Compose.
