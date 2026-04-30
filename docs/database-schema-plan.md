# Database schema plan

## MVP tables

- `users`;
- `roles`;
- `courses`;
- `modules`;
- `lessons`;
- `submissions`;
- `validation_jobs`;
- `reports`;
- `reviews`;
- `lab_instances`;
- `black_box_assignments`;
- `notifications`;
- `audit_events`.

## Constraints

- `users.email` уникален.
- `submissions.image_reference` обязателен.
- `submissions.application_port` в диапазоне `1..65535`.
- `validation_jobs.submission_id` ссылается на `submissions`.
- `reports.submission_id` или `assignment_id` обязательны по типу отчета.
- Status fields ограничены enum/check constraints.

## Indexes

- `users(email)`;
- `submissions(student_id, module_id, status)`;
- `validation_jobs(submission_id, status, created_at)`;
- `reports(author_id, status, type)`;
- `reviews(report_id, curator_id)`;
- `lab_instances(submission_id, status)`;
- `black_box_assignments(student_id, status)`;
- `audit_events(actor_id, created_at)`;
- `audit_events(object_type, object_id)`.

## Flyway policy

- Каждое изменение схемы оформляется отдельной migration.
- Migration не редактируется после попадания в main branch.
- Seed/demo data отделяется от structural migrations.
- Rollback для MVP описывается в runbook, а не хранится как destructive migration.

## Seed/demo data

Demo data загружается отдельным profile или command.
Состав данных описан в `docs/demo-data-plan.md`.

## Large artifacts

Attachments, screenshots и logs большого объема не хранятся в PostgreSQL. В БД хранится metadata и
URI на S3-compatible storage или локальный storage для demo.
