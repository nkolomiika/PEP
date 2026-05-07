# Frontend route map

## Общие маршруты

| Route | Доступ | Назначение |
| --- | --- | --- |
| `/login` | anonymous | Вход |
| `/workspace` | authenticated | Рабочая область после входа |
| `/courses` | authenticated | Список курсов |
| `/profile` | authenticated | Профиль и уведомления |

Frontend показывает форму входа и отправляет учетные данные в `POST /api/auth/login`. Backend
устанавливает `HttpOnly` session cookie, после чего роль, email и display name проверяются через
`GET /api/me`. Перед login и другими небезопасными запросами frontend получает CSRF token через
`GET /api/auth/csrf` и отправляет его в `X-XSRF-TOKEN`; demo-пароли не должны храниться в client bundle.

## Student routes

| Route | Сценарий | API |
| --- | --- | --- |
| `/courses` | Список курсов | Courses |
| `/web-security/...` | Теория и архивные задачи модуля | Lessons, pentest tasks |
| `/my-stand?module=:slug` | Загрузка и запуск своего стенда | User stands |
| `/peer-tasks?module=:slug` | Назначенные задачи студентов | Peer stand assignments |

Student dashboard открывает материалы выбранного модуля Академии web-безопасности, форму загрузки
архива стенда, отчеты и назначенные задачи студентов. Студент больше не запрашивает peer-задачи сам:
они появляются только после распределения куратором или администратором.

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
| `/workspace` | Админская рабочая область | Overview, content, users, streams, reviews, analytics, audit |
| `/courses` | Просмотр курсов как студент без привязки к потоку | Courses, lessons |

Admin dashboard показывает online-статистику, пользователей, конструктор курсов/модулей/страниц,
потоки, стенды на проверке, аналитику и audit trail. Разделы `Системные задачи` и `Lab runtime`
убраны из навигации; GitLab-задачи считаются legacy.

## Progress Charts

- Student dashboard показывает отчеты с feedback, black box цели и статусы submissions/reports.
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
- Вложения отображаются как ссылки на backend download endpoint, чтобы права доступа проверялись API.
- Очередь куратора фильтруется по статусу, типу отчета и email автора.

Markdown preview рендерится без `dangerouslySetInnerHTML`: HTML остается текстом React, ссылки
разрешены только для `http`, `https`, относительных URL и anchors, а внешние ссылки получают
`rel="noopener noreferrer"`.
Backend ограничивает размер пользовательского Markdown, поэтому frontend должен показывать ошибку API,
если отчет или комментарий превышают production limits.

## Live Statuses

- Общий dashboard подключается к SSE `/api/live/status-stream`.
- Live card показывает состояние SSE connection и счетчики submissions, validation jobs, reports, labs и assignments.

## Acceptance criteria

- Все P0 user stories имеют route.
- Все routes связаны с API groups.
- Role guards покрыты frontend tests или smoke checks.
