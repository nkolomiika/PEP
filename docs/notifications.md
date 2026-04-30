# Notifications

## Цель

Уведомления помогают студенту, куратору и администратору понимать, что произошло с submission,
report, lab или assignment без ручной проверки всех страниц.

## MVP approach

В MVP используются in-app notifications:

- список уведомлений в интерфейсе;
- статус `read/unread`;
- ссылка на связанный объект;
- создание уведомления из backend service после важного события.

Email, SSE и WebSocket относятся к production-ready развитию.

## События студента

| Событие | Текст |
| --- | --- |
| `VALIDATION_STARTED` | Техническая проверка image началась |
| `VALIDATION_FAILED` | Техническая проверка не пройдена, откройте детали |
| `VALIDATION_PASSED` | Image прошел техническую проверку |
| `WHITE_BOX_REVIEW_COMPLETED` | Куратор проверил white box отчет |
| `ASSIGNMENT_RECEIVED` | Вам назначена цель для black box тестирования |
| `BLACK_BOX_REVIEW_COMPLETED` | Куратор проверил black box отчет |

## События куратора

| Событие | Текст |
| --- | --- |
| `WHITE_BOX_REPORT_SUBMITTED` | Новый white box отчет ожидает проверки |
| `BLACK_BOX_REPORT_SUBMITTED` | Новый black box отчет ожидает проверки |
| `REVISION_SUBMITTED` | Студент отправил исправленную версию отчета |

## События администратора

| Событие | Текст |
| --- | --- |
| `DISTRIBUTION_COMPLETED` | Black box распределение завершено |
| `LAB_FAILED` | Lab instance завершился с ошибкой |
| `VALIDATION_WORKER_FAILED` | Worker не смог завершить validation job |

## Правила

- Уведомление создается только после сохранения основного бизнес-события.
- Уведомления не заменяют audit log.
- Уведомления не должны содержать secrets, payload с персональными данными или приватные evidence.
- Студент не получает сведения об авторе чужого lab.

## Acceptance criteria

- Для каждого P0 flow есть минимум одно пользовательское уведомление.
- Уведомление содержит роль, текст, ссылку на объект и дату создания.
- Notification flow покрыт в traceability matrix.
