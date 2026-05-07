# Demo data plan

## Цель

Demo data нужен, чтобы защита ВКР запускалась повторяемо: одни и те же пользователи, курс,
модуль, image references, отчеты и audit events должны приводить к одному демонстрационному
сценарию.

## Demo accounts

| Роль | Email | Назначение |
| --- | --- | --- |
| Администратор | `admin@pep.local` | Создание курса, запуск distribution, просмотр audit |
| Куратор | `curator@pep.local` | Проверка white box и black box отчетов |
| Студент 1 | `student1@pep.local` | Автор vulnerable app |
| Студент 2 | `student2@pep.local` | Black box tester |

Пароли для демонстрации должны храниться только в seed/demo конфигурации и не использоваться в
production-ready окружении.

## Учебные данные

- Курс: `Вводный курс по Docker`.
- Курс: `Академия веб-безопасности`.
- Модули Академии: SQL-инъекции, межсайтовое выполнение сценариев, инъекция в серверные шаблоны,
  подделка серверных запросов, межсайтовая подделка запроса, ошибки настройки CORS, внешние сущности
  XML, уязвимости бизнес-логики, нарушение управления доступом, небезопасные прямые ссылки на объекты.
- Старые demo-курсы `OWASP Top 10` и `Web Security Academy` архивируются, если они есть в БД.
- Дедлайны: учебные значения, достаточные для прохождения demo flow за один запуск.

## Стенды

Основной сценарий:

- студент загружает архив проекта с `Dockerfile` или `docker-compose.yml`;
- validation worker собирает image и публикует его в local registry;
- администратор запускает lab instance или студент запускает официальный системный стенд.

Fallback image:

Валидный image:

```text
localhost:5001/vulnerable-sqli-demo:latest
```

Невалидный image для демонстрации ошибки:

```text
localhost:5001/missing-image:latest
```

## Отчеты

White box отчет должен содержать:

- уязвимый endpoint;
- уязвимый SQL-запрос;
- payload `' OR '1'='1`;
- evidence успешного обхода;
- рекомендацию использовать parameterized query.

Black box отчет должен содержать:

- назначенный target lab;
- payload;
- HTTP request/response или скриншот;
- impact;
- рекомендацию по исправлению.

## Минимальный audit trail

Для демонстрации нужны события:

- `AUTH_LOGIN_SUCCESS`;
- `COURSE_UPSERTED`;
- `SUBMISSION_CREATED`;
- `SUBMISSION_ARCHIVE_UPLOADED`;
- `VALIDATION_JOB_PASSED`;
- `REVIEW_COMPLETED`;
- `LAB_CREATED`;
- `BLACK_BOX_DISTRIBUTION_COMPLETED`;
- `REPORT_SUBMITTED`;
- `LESSON_UPSERTED`;
- `ADMIN_USER_CREATED`.

## Данные системных задач

Для official pentest стендов подготовьте метаданные задачи (`slug`, категория, сложность, entrypoint, описание) и runtime-образ, например:

```text
title: SQL Injection Basic Lab
slug: sqli-basic
category: SQL-инъекции
difficulty: beginner
durationMinutes: 240
entrypointPort: 8080
healthPath: /health
composeService: web
description: Найдите SQL-инъекцию в форме входа.
```

## Acceptance criteria

- Demo data можно создать без ручного редактирования БД.
- Данные не содержат реальных персональных данных.
- Demo flow проходит от входа администратора до оценки black box отчета и запуска системного стенда.
