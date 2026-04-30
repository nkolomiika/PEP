# Traceability matrix

## Цель

Матрица связывает требования, user stories, API, проверки и demo steps, чтобы показать полноту MVP
для ВКР.

| Требование | User story | API | Тесты | Demo step |
| --- | --- | --- | --- | --- |
| Роли и доступ | Student/Curator/Admin stories | Auth, RBAC-protected endpoints | Security tests | Войти под разными ролями |
| Docker intro | Прохождение Docker-курса | Courses, Lessons | API/UI smoke | Открыть вводный урок |
| OWASP A03 | Изучение Injection | Courses, Lessons | Content smoke | Открыть модуль A03 |
| Submission image | Сдача уязвимого приложения | `POST /api/submissions` | Validation tests | Отправить image reference |
| Technical validation | Проверка image | Validation jobs | Worker tests | Показать PASSED/FAILED job |
| White box report | Доказать уязвимость | Reports, Reviews | Review flow tests | Отправить и проверить отчет |
| Lab runtime | Запуск lab | Labs | kind smoke | Открыть lab через port-forward |
| Black box assignment | Тестировать чужие app | Assignments | Distribution tests | Получить target |
| Black box report | Отчет по найденной уязвимости | Reports, Reviews | Scoring tests | Выставить score |
| Audit | Просмотр действий | Audit API | Audit tests | Показать audit trail |

## P0 coverage

P0 backlog считается покрытым, если:

- есть endpoint или UI flow;
- есть минимум один тестовый сценарий;
- есть demo step;
- статус и ошибка описаны в `docs/status-and-errors.md`;
- доступ описан в `docs/rbac-permissions.md`.

## Подтверждение цели ВКР

MVP закрывает цель ВКР, если студент проходит теорию, создает уязвимое приложение, отправляет
Docker image reference, доказывает уязвимость в white box отчете, тестирует чужой lab в black box
формате и получает проверку куратора.
