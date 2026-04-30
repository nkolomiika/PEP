import React, { useEffect, useMemo, useState } from "react";
import ReactDOM from "react-dom/client";
import "./styles.css";

type Role = "STUDENT" | "CURATOR" | "ADMIN";
type ReportType = "WHITE_BOX" | "BLACK_BOX";
type ReviewDecision = "APPROVED" | "NEEDS_REVISION" | "REJECTED";

type DemoAccount = {
  email: string;
  password: string;
  role: Role;
  label: string;
};

type Course = {
  id: string;
  title: string;
  description: string;
  status: string;
  modules: LearningModule[];
};

type LearningModule = {
  id: string;
  title: string;
  vulnerabilityTopic: string;
  status: string;
};

type Submission = {
  id: string;
  moduleId: string;
  studentEmail: string;
  imageReference: string;
  applicationPort: number;
  healthPath: string;
  status: string;
};

type ValidationJob = {
  id: string;
  submissionId: string;
  imageReference: string;
  status: string;
  logsUri?: string;
  errorMessage?: string;
};

type Report = {
  id: string;
  authorEmail: string;
  moduleId: string;
  submissionId?: string;
  type: ReportType;
  title: string;
  contentMarkdown: string;
  status: string;
};

type AuditEvent = {
  id: string;
  actorEmail?: string;
  action: string;
  targetType: string;
  targetId?: string;
  metadataJson: string;
  createdAt: string;
};

type Lab = {
  id: string;
  submissionId: string;
  studentEmail: string;
  imageReference: string;
  namespace: string;
  deploymentName: string;
  serviceName: string;
  routeUrl: string;
  status: string;
  expiresAt: string;
};

type BlackBoxAssignment = {
  id: string;
  moduleId: string;
  targetLabId: string;
  targetUrl: string;
  targetImageReference: string;
  status: string;
  assignedAt: string;
};

type ApiState = {
  courses: Course[];
  submissions: Submission[];
  validationJobs: ValidationJob[];
  reports: Report[];
  labs: Lab[];
  assignments: BlackBoxAssignment[];
  auditEvents: AuditEvent[];
};

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

const demoAccounts: DemoAccount[] = [
  { email: "student1@pep.local", password: "student", role: "STUDENT", label: "Студент 1" },
  { email: "curator@pep.local", password: "curator", role: "CURATOR", label: "Куратор" },
  { email: "admin@pep.local", password: "admin", role: "ADMIN", label: "Администратор" }
];

const statusLabels: Record<string, string> = {
  ACTIVE: "Активен",
  PUBLISHED: "Опубликован",
  VALIDATION_QUEUED: "Ожидает технической проверки",
  READY_FOR_REVIEW: "Ожидает проверки куратора",
  TECHNICAL_VALIDATION_FAILED: "Техническая проверка не пройдена",
  APPROVED: "Принято",
  NEEDS_REVISION: "Нужны исправления",
  REJECTED: "Отклонено",
  QUEUED: "В очереди",
  PASSED: "Проверка пройдена",
  FAILED: "Проверка завершилась ошибкой",
  SUBMITTED: "Отправлен",
  RUNNING: "Запущен",
  ASSIGNED: "Назначено",
  IN_PROGRESS: "В работе",
  SCORED: "Оценено"
};

const roleLabels: Record<Role, string> = {
  STUDENT: "Студент",
  CURATOR: "Куратор",
  ADMIN: "Администратор"
};

function authHeader(account: DemoAccount) {
  return `Basic ${btoa(`${account.email}:${account.password}`)}`;
}

async function apiRequest<T>(account: DemoAccount, path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: {
      Authorization: authHeader(account),
      "Content-Type": "application/json",
      ...init?.headers
    }
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(error?.message ?? `Ошибка API: ${response.status}`);
  }

  return response.json() as Promise<T>;
}

function StatusBadge({ value }: { value: string }) {
  return <span className="badge">{statusLabels[value] ?? value}</span>;
}

function EmptyState({ children }: { children: React.ReactNode }) {
  return <p className="muted empty">{children}</p>;
}

function App() {
  const [account, setAccount] = useState<DemoAccount>(demoAccounts[0]);
  const [state, setState] = useState<ApiState>({
    courses: [],
    submissions: [],
    validationJobs: [],
    reports: [],
    labs: [],
    assignments: [],
    auditEvents: []
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("Выберите demo-пользователя и загрузите данные.");
  const [error, setError] = useState<string | null>(null);

  const firstModule = useMemo(() => state.courses[0]?.modules[0], [state.courses]);

  async function loadDashboard(activeAccount = account) {
    setLoading(true);
    setError(null);
    try {
      const [courses, submissions, validationJobs, reports] = await Promise.all([
        apiRequest<Course[]>(activeAccount, "/api/courses"),
        apiRequest<Submission[]>(activeAccount, "/api/submissions"),
        apiRequest<ValidationJob[]>(activeAccount, "/api/validation-jobs"),
        apiRequest<Report[]>(activeAccount, "/api/reports")
      ]);
      const labs =
        activeAccount.role === "ADMIN" || activeAccount.role === "CURATOR"
          ? await apiRequest<Lab[]>(activeAccount, "/api/labs")
          : [];
      const assignments =
        activeAccount.role === "STUDENT"
          ? await apiRequest<BlackBoxAssignment[]>(activeAccount, "/api/black-box-assignments/my")
          : [];
      const auditEvents =
        activeAccount.role === "ADMIN" ? await apiRequest<AuditEvent[]>(activeAccount, "/api/audit") : [];
      setState({ courses, submissions, validationJobs, reports, labs, assignments, auditEvents });
      setMessage("Данные загружены.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Не удалось загрузить данные.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadDashboard(account);
  }, [account]);

  async function withRefresh(action: () => Promise<void>, successMessage: string) {
    setLoading(true);
    setError(null);
    try {
      await action();
      await loadDashboard(account);
      setMessage(successMessage);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Операция не выполнена.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="page">
      <section className="hero">
        <p className="eyebrow">Practical Exploitation Platform</p>
        <h1>PEP: практическая платформа по информационной безопасности</h1>
        <p>
          Demo-интерфейс покрывает core MVP: курс OWASP Top 10, отправку Docker image, technical
          validation, white box отчет, проверку куратора и audit trail.
        </p>
      </section>

      <section className="toolbar card">
        <div>
          <label htmlFor="account">Demo-пользователь</label>
          <select
            id="account"
            value={account.email}
            onChange={(event) => {
              const nextAccount = demoAccounts.find((item) => item.email === event.target.value);
              if (nextAccount) {
                setAccount(nextAccount);
              }
            }}
          >
            {demoAccounts.map((item) => (
              <option key={item.email} value={item.email}>
                {item.label} ({item.email})
              </option>
            ))}
          </select>
        </div>
        <button type="button" onClick={() => void loadDashboard()}>
          Обновить данные
        </button>
      </section>

      {loading && <p className="notice">Загрузка...</p>}
      {error && <p className="notice error">Ошибка: {error}</p>}
      {!error && <p className="notice">{message}</p>}

      <section className="grid">
        <CourseCard courses={state.courses} />
        <UserCard account={account} />
      </section>

      {account.role === "STUDENT" && (
        <StudentDashboard
          firstModule={firstModule}
          submissions={state.submissions}
          validationJobs={state.validationJobs}
          reports={state.reports}
          assignments={state.assignments}
          onCreateSubmission={(payload) =>
            withRefresh(
              () =>
                apiRequest(account, "/api/submissions", {
                  method: "POST",
                  body: JSON.stringify(payload)
                }),
              "Submission создан, validation job поставлен в очередь."
            )
          }
          onCreateReport={(payload) =>
            withRefresh(
              () =>
                apiRequest(account, "/api/reports", {
                  method: "POST",
                  body: JSON.stringify(payload)
                }),
              "Отчет отправлен куратору."
            )
          }
        />
      )}

      {account.role === "CURATOR" && (
        <CuratorDashboard
          validationJobs={state.validationJobs}
          reports={state.reports}
          onCompleteValidation={(jobId, passed) =>
            withRefresh(
              () =>
                apiRequest(account, `/api/validation-jobs/${jobId}/complete`, {
                  method: "POST",
                  body: JSON.stringify({
                    passed,
                    logsUri: passed ? "memory://validation/success.log" : undefined,
                    errorMessage: passed ? undefined : "Image не отвечает на health endpoint."
                  })
                }),
              passed ? "Validation job отмечен как успешный." : "Validation job отмечен как ошибочный."
            )
          }
          onCreateReview={(payload) =>
            withRefresh(
              () =>
                apiRequest(account, "/api/reviews", {
                  method: "POST",
                  body: JSON.stringify(payload)
                }),
              "Решение куратора сохранено."
            )
          }
        />
      )}

      {account.role === "ADMIN" && (
        <AdminDashboard
          submissions={state.submissions}
          validationJobs={state.validationJobs}
          reports={state.reports}
          labs={state.labs}
          auditEvents={state.auditEvents}
          firstModule={firstModule}
          onCreateLab={(submissionId) =>
            withRefresh(
              () =>
                apiRequest(account, "/api/labs", {
                  method: "POST",
                  body: JSON.stringify({ submissionId })
                }),
              "Lab instance создан для принятой работы."
            )
          }
          onDistribute={(moduleId) =>
            withRefresh(
              () =>
                apiRequest(account, `/api/modules/${moduleId}/black-box-assignments/distribute`, {
                  method: "POST"
                }),
              "Black box цели распределены."
            )
          }
        />
      )}
    </main>
  );
}

function CourseCard({ courses }: { courses: Course[] }) {
  return (
    <article className="card">
      <h2>Курсы</h2>
      {courses.length === 0 ? (
        <EmptyState>Курсы пока не загружены.</EmptyState>
      ) : (
        courses.map((course) => (
          <div key={course.id} className="stack">
            <h3>{course.title}</h3>
            <p>{course.description}</p>
            <StatusBadge value={course.status} />
            <ul>
              {course.modules.map((module) => (
                <li key={module.id}>
                  {module.title}: {module.vulnerabilityTopic} <StatusBadge value={module.status} />
                </li>
              ))}
            </ul>
          </div>
        ))
      )}
    </article>
  );
}

function UserCard({ account }: { account: DemoAccount }) {
  return (
    <article className="card">
      <h2>Текущая роль</h2>
      <p className="big">{roleLabels[account.role]}</p>
      <p className="muted">{account.email}</p>
      <p>
        Доступы управляются backend через HTTP Basic и роли `STUDENT`, `CURATOR`, `ADMIN`.
      </p>
    </article>
  );
}

function StudentDashboard({
  firstModule,
  submissions,
  validationJobs,
  reports,
  assignments,
  onCreateSubmission,
  onCreateReport
}: {
  firstModule?: LearningModule;
  submissions: Submission[];
  validationJobs: ValidationJob[];
  reports: Report[];
  assignments: BlackBoxAssignment[];
  onCreateSubmission: (payload: {
    moduleId: string;
    imageReference: string;
    applicationPort: number;
    healthPath: string;
  }) => Promise<void>;
  onCreateReport: (payload: {
    moduleId: string;
    submissionId?: string;
    type: ReportType;
    title: string;
    contentMarkdown: string;
  }) => Promise<void>;
}) {
  const [imageReference, setImageReference] = useState("localhost:5001/vulnerable-sqli-demo:latest");
  const [reportText, setReportText] = useState("Payload: ' OR '1'='1\nEvidence: login bypass воспроизводится.");
  const latestSubmission = submissions[0];

  return (
    <section className="grid">
      <article className="card">
        <h2>Student: сдача Docker image</h2>
        {!firstModule ? (
          <EmptyState>Нет активного модуля.</EmptyState>
        ) : (
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault();
              void onCreateSubmission({
                moduleId: firstModule.id,
                imageReference,
                applicationPort: 8080,
                healthPath: "/health"
              });
            }}
          >
            <label htmlFor="imageReference">Docker image reference</label>
            <input
              id="imageReference"
              value={imageReference}
              onChange={(event) => setImageReference(event.target.value)}
            />
            <button type="submit">Отправить image на technical validation</button>
          </form>
        )}
        <EntityList
          title="Мои submissions"
          items={submissions}
          render={(submission) => (
            <>
              <strong>{submission.imageReference}</strong>
              <StatusBadge value={submission.status} />
              <p className="muted">Port: {submission.applicationPort}, health: {submission.healthPath}</p>
            </>
          )}
        />
      </article>

      <article className="card">
        <h2>Student: white box отчет</h2>
        {!firstModule || !latestSubmission ? (
          <EmptyState>Сначала отправьте Docker image.</EmptyState>
        ) : (
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault();
              void onCreateReport({
                moduleId: firstModule.id,
                submissionId: latestSubmission.id,
                type: "WHITE_BOX",
                title: "White box отчет по SQL Injection",
                contentMarkdown: reportText
              });
            }}
          >
            <label htmlFor="reportText">Evidence и payload</label>
            <textarea
              id="reportText"
              rows={6}
              value={reportText}
              onChange={(event) => setReportText(event.target.value)}
            />
            <button type="submit">Отправить отчет куратору</button>
          </form>
        )}
        <EntityList
          title="Validation jobs"
          items={validationJobs}
          render={(job) => (
            <>
              <strong>{job.imageReference}</strong>
              <StatusBadge value={job.status} />
              {job.errorMessage && <p className="error-text">{job.errorMessage}</p>}
            </>
          )}
        />
        <EntityList
          title="Мои отчеты"
          items={reports}
          render={(report) => (
            <>
              <strong>{report.title}</strong>
              <StatusBadge value={report.status} />
            </>
          )}
        />
        <EntityList
          title="Мои black box цели"
          items={assignments}
          render={(assignment) => (
            <>
              <strong>{assignment.targetUrl}</strong>
              <StatusBadge value={assignment.status} />
              <p className="muted">Image target: {assignment.targetImageReference}</p>
            </>
          )}
        />
      </article>
    </section>
  );
}

function CuratorDashboard({
  validationJobs,
  reports,
  onCompleteValidation,
  onCreateReview
}: {
  validationJobs: ValidationJob[];
  reports: Report[];
  onCompleteValidation: (jobId: string, passed: boolean) => Promise<void>;
  onCreateReview: (payload: {
    reportId: string;
    decision: ReviewDecision;
    score: number;
    commentMarkdown: string;
  }) => Promise<void>;
}) {
  return (
    <section className="grid">
      <article className="card">
        <h2>Curator: technical validation</h2>
        <EntityList
          title="Validation jobs"
          items={validationJobs}
          render={(job) => (
            <>
              <strong>{job.imageReference}</strong>
              <StatusBadge value={job.status} />
              <div className="actions">
                <button type="button" onClick={() => void onCompleteValidation(job.id, true)}>
                  Отметить как пройдено
                </button>
                <button type="button" className="secondary" onClick={() => void onCompleteValidation(job.id, false)}>
                  Отметить ошибку
                </button>
              </div>
            </>
          )}
        />
      </article>

      <article className="card">
        <h2>Curator: проверка отчетов</h2>
        <EntityList
          title="Очередь отчетов"
          items={reports}
          render={(report) => (
            <ReviewForm key={report.id} report={report} onCreateReview={onCreateReview} />
          )}
        />
      </article>
    </section>
  );
}

function ReviewForm({
  report,
  onCreateReview
}: {
  report: Report;
  onCreateReview: (payload: {
    reportId: string;
    decision: ReviewDecision;
    score: number;
    commentMarkdown: string;
  }) => Promise<void>;
}) {
  const [score, setScore] = useState(90);
  const [decision, setDecision] = useState<ReviewDecision>("APPROVED");
  const [comment, setComment] = useState("Уязвимость воспроизводится, evidence достаточно.");

  return (
    <form
      className="review-box"
      onSubmit={(event) => {
        event.preventDefault();
        void onCreateReview({ reportId: report.id, decision, score, commentMarkdown: comment });
      }}
    >
      <strong>{report.title}</strong>
      <StatusBadge value={report.status} />
      <p className="muted">{report.authorEmail}</p>
      <pre>{report.contentMarkdown}</pre>
      <label htmlFor={`decision-${report.id}`}>Решение</label>
      <select id={`decision-${report.id}`} value={decision} onChange={(event) => setDecision(event.target.value as ReviewDecision)}>
        <option value="APPROVED">Принять</option>
        <option value="NEEDS_REVISION">Вернуть на доработку</option>
        <option value="REJECTED">Отклонить</option>
      </select>
      <label htmlFor={`score-${report.id}`}>Баллы</label>
      <input
        id={`score-${report.id}`}
        type="number"
        min={0}
        max={100}
        value={score}
        onChange={(event) => setScore(Number(event.target.value))}
      />
      <label htmlFor={`comment-${report.id}`}>Комментарий</label>
      <textarea id={`comment-${report.id}`} value={comment} onChange={(event) => setComment(event.target.value)} />
      <button type="submit">Сохранить решение</button>
    </form>
  );
}

function AdminDashboard({
  submissions,
  validationJobs,
  reports,
  labs,
  auditEvents,
  firstModule,
  onCreateLab,
  onDistribute
}: {
  submissions: Submission[];
  validationJobs: ValidationJob[];
  reports: Report[];
  labs: Lab[];
  auditEvents: AuditEvent[];
  firstModule?: LearningModule;
  onCreateLab: (submissionId: string) => Promise<void>;
  onDistribute: (moduleId: string) => Promise<void>;
}) {
  const approvedSubmissions = submissions.filter((submission) => submission.status === "APPROVED");
  const labSubmissionIds = new Set(labs.map((lab) => lab.submissionId));

  return (
    <section className="grid">
      <article className="card">
        <h2>Admin: состояние MVP</h2>
        <dl className="metrics">
          <div>
            <dt>Submissions</dt>
            <dd>{submissions.length}</dd>
          </div>
          <div>
            <dt>Validation jobs</dt>
            <dd>{validationJobs.length}</dd>
          </div>
          <div>
            <dt>Reports</dt>
            <dd>{reports.length}</dd>
          </div>
          <div>
            <dt>Labs</dt>
            <dd>{labs.length}</dd>
          </div>
        </dl>
      </article>

      <article className="card">
        <h2>Admin: lab runtime</h2>
        <EntityList
          title="Принятые submissions"
          items={approvedSubmissions}
          render={(submission) => (
            <>
              <strong>{submission.imageReference}</strong>
              <p className="muted">{submission.studentEmail}</p>
              {labSubmissionIds.has(submission.id) ? (
                <StatusBadge value="RUNNING" />
              ) : (
                <button type="button" onClick={() => void onCreateLab(submission.id)}>
                  Создать lab instance
                </button>
              )}
            </>
          )}
        />
        <EntityList
          title="Lab instances"
          items={labs}
          render={(lab) => (
            <>
              <strong>{lab.routeUrl}</strong>
              <StatusBadge value={lab.status} />
              <p className="muted">
                {lab.namespace} / {lab.serviceName}
              </p>
            </>
          )}
        />
        {firstModule ? (
          <button type="button" onClick={() => void onDistribute(firstModule.id)}>
            Распределить black box цели
          </button>
        ) : (
          <EmptyState>Нет активного модуля для распределения.</EmptyState>
        )}
      </article>

      <article className="card">
        <h2>Admin: audit trail</h2>
        <EntityList
          title="Последние события"
          items={auditEvents}
          render={(event) => (
            <>
              <strong>{event.action}</strong>
              <p className="muted">
                {event.actorEmail ?? "system"} - {event.targetType}
              </p>
              <code>{event.metadataJson}</code>
            </>
          )}
        />
      </article>
    </section>
  );
}

function EntityList<T>({
  title,
  items,
  render
}: {
  title: string;
  items: T[];
  render: (item: T) => React.ReactNode;
}) {
  return (
    <div className="list-block">
      <h3>{title}</h3>
      {items.length === 0 ? (
        <EmptyState>Нет данных для отображения.</EmptyState>
      ) : (
        <ul className="entity-list">
          {items.map((item, index) => (
            <li key={index}>{render(item)}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
