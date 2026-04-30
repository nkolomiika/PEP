# Frontend route map

## Общие маршруты

| Route | Доступ | Назначение |
| --- | --- | --- |
| `/login` | anonymous | Вход |
| `/` | authenticated | Redirect по роли |
| `/profile` | authenticated | Профиль и уведомления |

## Student routes

| Route | Сценарий | API |
| --- | --- | --- |
| `/student/courses` | Список курсов | Courses |
| `/student/courses/:courseId/modules/:moduleId` | Теория и задания | Lessons |
| `/student/submissions/new` | Сдача image | Submissions |
| `/student/submissions/:id` | Статус validation | Submissions, validation jobs |
| `/student/reports/:id/edit` | White/black box отчет | Reports |
| `/student/assignments` | Black box targets | Assignments |

## Curator routes

| Route | Сценарий | API |
| --- | --- | --- |
| `/curator/reviews` | Очередь проверки | Reports |
| `/curator/reviews/:reportId` | Проверка отчета | Reviews |
| `/curator/students/:studentId/progress` | Прогресс студента | Courses, reports |

## Admin routes

| Route | Сценарий | API |
| --- | --- | --- |
| `/admin/courses` | Управление курсами | Courses |
| `/admin/groups` | Управление группами | Groups |
| `/admin/labs` | Lab runtime | Labs |
| `/admin/distribution` | Black box distribution | Assignments |
| `/admin/audit` | Audit trail | Audit |

## Route guards

- Anonymous user доступен только к `/login`.
- Student не открывает curator/admin routes.
- Curator не открывает admin-only routes.
- Admin может открывать admin routes и read-only учебные материалы.

## Page states

Каждая страница должна иметь:

- loading state;
- empty state;
- error state с русским сообщением;
- success state;
- access denied state.

## Report UX

- Формы white box и black box отчетов показывают markdown preview до отправки.
- Куратор видит report content и preview своего feedback в формате markdown.

## Acceptance criteria

- Все P0 user stories имеют route.
- Все routes связаны с API groups.
- Role guards покрыты frontend tests или smoke checks.
