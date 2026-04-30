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

Student dashboard показывает selector учебного модуля. Для курса OWASP Top 10 selector содержит
`A01`-`A10`, после выбора модуль загружает свои уроки, progress, сдачу Docker image и module result.
Admin/Curator demo flow по умолчанию продолжает использовать `A03. Injection` как полностью
демонстрируемый lab-модуль.

## Curator routes

| Route | Сценарий | API |
| --- | --- | --- |
| `/curator/reviews` | Очередь проверки | Reports |
| `/curator/reviews/:reportId` | Проверка отчета | Reviews |
| `/curator/students/:studentId/progress` | Прогресс студента | Courses, reports |
| `/curator/grades/export` | Экспорт оценок CSV | Module results |

## Admin routes

| Route | Сценарий | API |
| --- | --- | --- |
| `/admin/courses` | Управление курсами | Courses |
| `/admin/groups` | Управление группами | Groups |
| `/admin/labs` | Lab runtime | Labs |
| `/admin/distribution` | Black box distribution | Assignments |
| `/admin/grades/export` | Экспорт оценок CSV | Module results |
| `/admin/audit` | Audit trail | Audit |

Admin dashboard показывает количество approved submissions, labs без созданного instance, running labs,
статусы lab instances, namespace/service metadata, команды deploy/port-forward и срок жизни lab.
Для lab runtime Admin dashboard также показывает ingress URL и команду установки ingress controller.

## Progress Charts

- Student dashboard показывает progress bars по урокам, отчетам с feedback, black box целям и статусам submissions/reports.
- Admin dashboard показывает progress bars по technical validation, approved reports, созданным labs и статусам validation jobs/labs.

## Image Scanning

- Student и Curator dashboards показывают baseline image scan status, summary и technical report внутри validation job.
- Scan warnings не блокируют technical validation, но помогают обсудить container hardening.

## Dependency Scanning

- Student и Curator dashboards показывают baseline dependency scan status, summary и technical report внутри validation job.
- Scan warnings не блокируют technical validation, но помогают обсудить SBOM и управление зависимостями.

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
- Формы white box и black box отчетов позволяют приложить файл evidence.
- Куратор видит report content и preview своего feedback в формате markdown.
- Куратор видит список вложений, приложенных к отчету.
- Очередь куратора фильтруется по статусу, типу отчета и email автора.

## Live Statuses

- Общий dashboard подключается к SSE `/api/live/status-stream`.
- Live card показывает состояние SSE connection и счетчики submissions, validation jobs, reports, labs и assignments.

## Acceptance criteria

- Все P0 user stories имеют route.
- Все routes связаны с API groups.
- Role guards покрыты frontend tests или smoke checks.
