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

- Курс: `OWASP Top 10`.
- Вводный модуль: `Docker intro`.
- Практический модуль: `A03. Injection`.
- Основная тема: SQL Injection.
- Дедлайны: учебные значения, достаточные для прохождения demo flow за один запуск.

## Docker images

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

- `USER_LOGIN`;
- `COURSE_CREATED`;
- `SUBMISSION_CREATED`;
- `VALIDATION_JOB_PASSED`;
- `WHITE_BOX_REVIEW_APPROVED`;
- `LAB_STARTED`;
- `BLACK_BOX_DISTRIBUTION_STARTED`;
- `BLACK_BOX_REPORT_SUBMITTED`;
- `REVIEW_SCORE_ASSIGNED`.

## Acceptance criteria

- Demo data можно создать без ручного редактирования БД.
- Данные не содержат реальных персональных данных.
- Demo flow проходит от входа администратора до оценки black box отчета.
