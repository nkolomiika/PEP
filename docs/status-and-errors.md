# Статусы и ошибки

## Submission

| Status | UI label | Действие пользователя |
| --- | --- | --- |
| `DRAFT` | Черновик | Заполнить image reference и отчет |
| `SUBMITTED` | Отправлено | Ожидать technical validation |
| `VALIDATION_QUEUED` | Ожидает проверки | Ничего не требуется |
| `TECHNICAL_VALIDATION` | Идет техническая проверка | Ожидать завершения |
| `TECHNICAL_VALIDATION_FAILED` | Техническая проверка не пройдена | Открыть logs и исправить image |
| `READY_FOR_REVIEW` | Ожидает проверки куратора | Ожидать решения |
| `APPROVED` | Принято | Ожидать публикации lab |
| `REJECTED` | Требуются исправления | Исправить отчет или image |

## ValidationJob

| Status | UI label | Действие пользователя |
| --- | --- | --- |
| `QUEUED` | В очереди | Ничего не требуется |
| `PULLING_IMAGE` | Загружается image | Проверить доступность registry при долгом ожидании |
| `STARTING_CONTAINER` | Запускается контейнер | Ожидать |
| `CHECKING_PORT` | Проверяется порт | Убедиться, что приложение слушает указанный port |
| `CHECKING_HEALTH` | Проверяется health endpoint | Исправить `healthPath` при ошибке |
| `PASSED` | Проверка пройдена | Перейти к отчету |
| `FAILED` | Проверка завершилась ошибкой | Открыть детали ошибки |

## Report

| Status | UI label | Действие пользователя |
| --- | --- | --- |
| `DRAFT` | Черновик | Заполнить evidence |
| `SUBMITTED` | Отправлен | Ожидать проверки |
| `NEEDS_REVISION` | Нужны исправления | Исправить по комментариям |
| `APPROVED` | Принят | Ничего не требуется |
| `REJECTED` | Отклонен | Повторить работу по модулю |

## LabInstance

| Status | UI label | Действие пользователя |
| --- | --- | --- |
| `PENDING` | Готовится | Ожидать запуска |
| `RUNNING` | Запущен | Открыть lab |
| `FAILED` | Ошибка запуска | Обратиться к администратору |
| `STOPPED` | Остановлен | Ожидать перезапуска или завершения модуля |

## BlackBoxAssignment

| Status | UI label | Действие пользователя |
| --- | --- | --- |
| `ASSIGNED` | Назначено | Начать тестирование |
| `IN_PROGRESS` | В работе | Заполнить отчет |
| `SUBMITTED` | Отчет отправлен | Ожидать проверки |
| `SCORED` | Оценено | Посмотреть результат |

## API error codes

| Code | HTTP | Сообщение |
| --- | --- | --- |
| `AUTH_REQUIRED` | 401 | Требуется вход в систему |
| `ACCESS_DENIED` | 403 | Недостаточно прав для операции |
| `NOT_FOUND` | 404 | Объект не найден |
| `VALIDATION_FAILED` | 400 | Проверьте заполненные поля |
| `INVALID_IMAGE_REFERENCE` | 400 | Укажите корректный Docker image reference |
| `STATUS_TRANSITION_DENIED` | 409 | Переход статуса сейчас невозможен |
| `LAB_RUNTIME_UNAVAILABLE` | 503 | Lab runtime временно недоступен |
| `INTERNAL_ERROR` | 500 | Внутренняя ошибка, попробуйте позже |
