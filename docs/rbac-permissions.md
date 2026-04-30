# RBAC и permissions

## Роли

- `STUDENT` - проходит обучение и сдает отчеты.
- `CURATOR` - проверяет отчеты и выставляет баллы.
- `ADMIN` - управляет курсами, группами, фазами и инфраструктурой.
- `SYSTEM_WORKER` - выполняет technical validation и lab operations.

## Матрица доступа

| Объект | Student | Curator | Admin | System worker |
| --- | --- | --- | --- | --- |
| Courses | read published | read published | create/update/publish | no access |
| Lessons | read published | read published | create/update/publish | no access |
| Submissions | create/read own | read assigned queue | read all | update validation status |
| Validation jobs | read own | read related | read all/retry | create/update |
| Reports | create/read own | review assigned | read all | no access |
| Reviews | read own result | create/update own review | read all | no access |
| Labs | read assigned | read related | create/restart/stop | create/update status |
| Assignments | read own | read related | distribute/update | no access |
| Audit events | no access | limited own actions | read all | append system events |

## Object-level access

- Студент видит только свои submissions, reports, reviews и назначенные black box targets.
- Студент не видит автора lab в black box фазе.
- Куратор видит отчеты, назначенные ему или находящиеся в общей очереди проверки.
- Администратор видит все учебные и инфраструктурные объекты.
- Worker не использует пользовательские permissions и работает через отдельный service identity.

## Backend checks

Каждый endpoint должен проверять:

- authenticated user;
- role permission;
- ownership или assignment relation;
- module phase;
- object status transition.

## Тесты

Обязательные negative tests:

- студент не читает чужой draft report;
- студент не открывает собственный black box target;
- куратор не меняет admin settings;
- worker не читает пользовательские отчеты;
- anonymous user не получает защищенные данные.
