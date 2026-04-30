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

type ModuleOption = LearningModule & {
  courseTitle: string;
};

type LessonSummary = {
  id: string;
  moduleId: string;
  title: string;
  position: number;
};

type Lesson = {
  id: string;
  moduleId: string;
  title: string;
  contentMarkdown: string;
  position: number;
};

type LessonProgress = {
  lessonId: string;
  completed: boolean;
  completedAt: string;
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
  imageScanStatus?: string;
  imageScanSummary?: string;
  imageScanReport?: string;
  dependencyScanStatus?: string;
  dependencyScanSummary?: string;
  dependencyScanReport?: string;
};

type ReportAttachment = {
  id: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
};

type Report = {
  id: string;
  authorEmail: string;
  moduleId: string;
  submissionId?: string;
  blackBoxAssignmentId?: string;
  type: ReportType;
  title: string;
  contentMarkdown: string;
  status: string;
  attachments: ReportAttachment[];
};

type Review = {
  id: string;
  reportId: string;
  curatorEmail: string;
  decision: ReviewDecision;
  score: number;
  commentMarkdown: string;
};

type ModuleResult = {
  moduleId: string;
  dockerPassed: boolean;
  whiteBoxScore?: number | null;
  blackBoxScore?: number | null;
  finalScore?: number | null;
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
  ingressUrl: string;
  deployCommand: string;
  ingressInstallCommand: string;
  portForwardCommand: string;
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

type LiveStatus = {
  role: Role;
  submissions: number;
  validationJobs: number;
  runningLabs: number;
  reports: number;
  assignments: number;
  updatedAt: string;
};

type ApiState = {
  courses: Course[];
  submissions: Submission[];
  validationJobs: ValidationJob[];
  reports: Report[];
  reviews: Review[];
  lessons: LessonSummary[];
  selectedLesson?: Lesson;
  lessonProgress: LessonProgress[];
  moduleResult?: ModuleResult;
  selectedModuleId?: string;
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
  WARNINGS: "Есть предупреждения",
  SUBMITTED: "Отправлен",
  RUNNING: "Запущен",
  ASSIGNED: "Назначено",
  IN_PROGRESS: "В работе",
  COMPLETED: "Изучено",
  DOCKER_REQUIRED: "Нужен Docker-допуск",
  SCORED: "Оценено",
  CONNECTED: "Подключено",
  DISCONNECTED: "Нет соединения"
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

async function uploadReportAttachment(account: DemoAccount, reportId: string, file: File): Promise<ReportAttachment> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${apiBaseUrl}/api/reports/${reportId}/attachments`, {
    method: "POST",
    headers: {
      Authorization: authHeader(account)
    },
    body: formData
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(error?.message ?? `Ошибка API: ${response.status}`);
  }

  return response.json() as Promise<ReportAttachment>;
}

async function downloadText(account: DemoAccount, path: string): Promise<string> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: {
      Authorization: authHeader(account)
    }
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(error?.message ?? `Ошибка API: ${response.status}`);
  }

  return response.text();
}

function liveStatusStreamUrl(account: DemoAccount) {
  const url = new URL("/api/live/status-stream", apiBaseUrl);
  url.username = account.email;
  url.password = account.password;
  return url.toString();
}

function StatusBadge({ value }: { value: string }) {
  return <span className="badge">{statusLabels[value] ?? value}</span>;
}

function EmptyState({ children }: { children: React.ReactNode }) {
  return <p className="muted empty">{children}</p>;
}

function MarkdownPreview({ source }: { source: string }) {
  const rendered = useMemo(() => renderMarkdown(source), [source]);

  if (source.trim().length === 0) {
    return <EmptyState>Markdown preview появится после ввода текста.</EmptyState>;
  }

  return <div className="markdown-preview">{rendered}</div>;
}

function renderMarkdown(source: string) {
  const lines = source.replace(/\r\n/g, "\n").split("\n");
  const elements: React.ReactNode[] = [];
  let index = 0;

  while (index < lines.length) {
    const line = lines[index];

    if (line.trim() === "") {
      index++;
      continue;
    }

    if (line.trim().startsWith("```")) {
      const codeLines: string[] = [];
      index++;
      while (index < lines.length && !lines[index].trim().startsWith("```")) {
        codeLines.push(lines[index]);
        index++;
      }
      index++;
      elements.push(
        <pre key={`code-${index}`}>
          <code>{codeLines.join("\n")}</code>
        </pre>
      );
      continue;
    }

    const heading = /^(#{1,3})\s+(.+)$/.exec(line);
    if (heading) {
      const level = heading[1].length;
      const content = renderInlineMarkdown(heading[2]);
      elements.push(
        level === 1 ? (
          <h1 key={`heading-${index}`}>{content}</h1>
        ) : level === 2 ? (
          <h2 key={`heading-${index}`}>{content}</h2>
        ) : (
          <h3 key={`heading-${index}`}>{content}</h3>
        )
      );
      index++;
      continue;
    }

    if (/^\s*[-*]\s+/.test(line)) {
      const items: React.ReactNode[] = [];
      while (index < lines.length && /^\s*[-*]\s+/.test(lines[index])) {
        items.push(
          <li key={`li-${index}`}>
            {renderInlineMarkdown(lines[index].replace(/^\s*[-*]\s+/, ""))}
          </li>
        );
        index++;
      }
      elements.push(<ul key={`ul-${index}`}>{items}</ul>);
      continue;
    }

    if (/^\s*\d+\.\s+/.test(line)) {
      const items: React.ReactNode[] = [];
      while (index < lines.length && /^\s*\d+\.\s+/.test(lines[index])) {
        items.push(
          <li key={`oli-${index}`}>
            {renderInlineMarkdown(lines[index].replace(/^\s*\d+\.\s+/, ""))}
          </li>
        );
        index++;
      }
      elements.push(<ol key={`ol-${index}`}>{items}</ol>);
      continue;
    }

    if (/^\s*>\s?/.test(line)) {
      const quoteLines: string[] = [];
      while (index < lines.length && /^\s*>\s?/.test(lines[index])) {
        quoteLines.push(lines[index].replace(/^\s*>\s?/, ""));
        index++;
      }
      elements.push(<blockquote key={`quote-${index}`}>{renderInlineMarkdown(quoteLines.join(" "))}</blockquote>);
      continue;
    }

    const paragraphLines = [line];
    index++;
    while (index < lines.length && lines[index].trim() !== "" && !isMarkdownBlockStart(lines[index])) {
      paragraphLines.push(lines[index]);
      index++;
    }
    elements.push(<p key={`p-${index}`}>{renderInlineMarkdown(paragraphLines.join(" "))}</p>);
  }

  return elements;
}

function isMarkdownBlockStart(line: string) {
  const trimmed = line.trim();
  return (
    trimmed.startsWith("```") ||
    /^(#{1,3})\s+/.test(line) ||
    /^\s*[-*]\s+/.test(line) ||
    /^\s*\d+\.\s+/.test(line) ||
    /^\s*>\s?/.test(line)
  );
}

function renderInlineMarkdown(text: string) {
  const parts = text.split(/(`[^`]+`|\*\*[^*]+\*\*)/g);
  return parts.map((part, index) => {
    if (part.startsWith("`") && part.endsWith("`")) {
      return <code key={index}>{part.slice(1, -1)}</code>;
    }
    if (part.startsWith("**") && part.endsWith("**")) {
      return <strong key={index}>{part.slice(2, -2)}</strong>;
    }
    return part;
  });
}

function ReportAttachmentsList({ attachments }: { attachments: ReportAttachment[] }) {
  if (attachments.length === 0) {
    return <p className="muted">Вложений нет.</p>;
  }

  return (
    <ul className="attachment-list">
      {attachments.map((attachment) => (
        <li key={attachment.id}>
          <strong>{attachment.originalFilename}</strong>
          <span className="muted">
            {attachment.contentType}, {formatBytes(attachment.sizeBytes)}
          </span>
        </li>
      ))}
    </ul>
  );
}

function formatBytes(sizeBytes: number) {
  if (sizeBytes < 1024) {
    return `${sizeBytes} B`;
  }
  if (sizeBytes < 1024 * 1024) {
    return `${(sizeBytes / 1024).toFixed(1)} KB`;
  }
  return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`;
}

function ProgressBar({ label, value, total }: { label: string; value: number; total: number }) {
  const percent = total === 0 ? 0 : Math.round((value / total) * 100);

  return (
    <div className="progress-chart">
      <div className="progress-chart-header">
        <strong>{label}</strong>
        <span className="muted">
          {value}/{total} ({percent}%)
        </span>
      </div>
      <div className="progress-track" aria-label={`${label}: ${percent}%`}>
        <div className="progress-fill" style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
}

function StatusDistribution({ title, counts }: { title: string; counts: Record<string, number> }) {
  const entries = Object.entries(counts);
  const total = entries.reduce((sum, [, count]) => sum + count, 0);

  return (
    <div className="progress-chart">
      <strong>{title}</strong>
      {entries.length === 0 ? (
        <p className="muted">Нет данных для графика.</p>
      ) : (
        entries.map(([status, count]) => (
          <ProgressBar key={status} label={statusLabels[status] ?? status} value={count} total={total} />
        ))
      )}
    </div>
  );
}

function ImageScanSummary({ job }: { job: ValidationJob }) {
  if (!job.imageScanStatus) {
    return <p className="muted">Image scan еще не выполнялся.</p>;
  }

  return (
    <div className="feedback-box">
      <strong>Image scan</strong>
      <StatusBadge value={job.imageScanStatus} />
      {job.imageScanSummary && <p>{job.imageScanSummary}</p>}
      {job.imageScanReport && (
        <details>
          <summary>Технический report</summary>
          <pre>{job.imageScanReport}</pre>
        </details>
      )}
    </div>
  );
}

function DependencyScanSummary({ job }: { job: ValidationJob }) {
  if (!job.dependencyScanStatus) {
    return <p className="muted">Dependency scan еще не выполнялся.</p>;
  }

  return (
    <div className="feedback-box">
      <strong>Dependency scan</strong>
      <StatusBadge value={job.dependencyScanStatus} />
      {job.dependencyScanSummary && <p>{job.dependencyScanSummary}</p>}
      {job.dependencyScanReport && (
        <details>
          <summary>Технический report</summary>
          <pre>{job.dependencyScanReport}</pre>
        </details>
      )}
    </div>
  );
}

function countByStatus(items: Array<{ status: string }>) {
  return items.reduce<Record<string, number>>((counts, item) => {
    counts[item.status] = (counts[item.status] ?? 0) + 1;
    return counts;
  }, {});
}

function App() {
  const [account, setAccount] = useState<DemoAccount>(demoAccounts[0]);
  const [state, setState] = useState<ApiState>({
    courses: [],
    submissions: [],
    validationJobs: [],
    reports: [],
    reviews: [],
    lessons: [],
    selectedLesson: undefined,
    lessonProgress: [],
    moduleResult: undefined,
    selectedModuleId: undefined,
    labs: [],
    assignments: [],
    auditEvents: []
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("Выберите demo-пользователя и загрузите данные.");
  const [error, setError] = useState<string | null>(null);
  const [liveStatus, setLiveStatus] = useState<LiveStatus | null>(null);
  const [liveConnected, setLiveConnected] = useState(false);

  const moduleOptions = useMemo(
    () =>
      state.courses.flatMap((course) =>
        course.modules.map((module) => ({
          ...module,
          courseTitle: course.title
        }))
      ),
    [state.courses]
  );
  const selectedModule = useMemo(
    () => moduleOptions.find((module) => module.id === state.selectedModuleId) ?? moduleOptions[0],
    [moduleOptions, state.selectedModuleId]
  );
  const labModule = useMemo(
    () => moduleOptions.find((module) => module.title === "A03. Injection") ?? selectedModule,
    [moduleOptions, selectedModule]
  );

  async function loadDashboard(activeAccount = account) {
    setLoading(true);
    setError(null);
    try {
      const [courses, submissions, validationJobs, reports, reviews] = await Promise.all([
        apiRequest<Course[]>(activeAccount, "/api/courses"),
        apiRequest<Submission[]>(activeAccount, "/api/submissions"),
        apiRequest<ValidationJob[]>(activeAccount, "/api/validation-jobs"),
        apiRequest<Report[]>(activeAccount, "/api/reports"),
        apiRequest<Review[]>(activeAccount, "/api/reviews")
      ]);
      const loadedModules = courses.flatMap((course) => course.modules);
      const nextSelectedModule =
        loadedModules.find((module) => module.id === state.selectedModuleId) ?? loadedModules[0];
      const lessons = nextSelectedModule
        ? await apiRequest<LessonSummary[]>(activeAccount, `/api/modules/${nextSelectedModule.id}/lessons`)
        : [];
      const selectedLesson = lessons[0]
        ? await apiRequest<Lesson>(activeAccount, `/api/lessons/${lessons[0].id}`)
        : undefined;
      const lessonProgress =
        activeAccount.role === "STUDENT" && nextSelectedModule
          ? await apiRequest<LessonProgress[]>(activeAccount, `/api/modules/${nextSelectedModule.id}/lesson-progress`)
          : [];
      const moduleResult =
        activeAccount.role === "STUDENT" && nextSelectedModule
          ? await apiRequest<ModuleResult>(activeAccount, `/api/modules/${nextSelectedModule.id}/result`)
          : undefined;
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
      setState({
        courses,
        submissions,
        validationJobs,
        reports,
        reviews,
        lessons,
        selectedLesson,
        lessonProgress,
        moduleResult,
        selectedModuleId: nextSelectedModule?.id,
        labs,
        assignments,
        auditEvents
      });
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

  useEffect(() => {
    const events = new EventSource(liveStatusStreamUrl(account));
    events.onopen = () => setLiveConnected(true);
    events.addEventListener("status", (event) => {
      setLiveStatus(JSON.parse((event as MessageEvent).data) as LiveStatus);
      setLiveConnected(true);
    });
    events.onerror = () => setLiveConnected(false);
    return () => {
      events.close();
      setLiveConnected(false);
    };
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

  async function openLesson(lessonId: string) {
    setLoading(true);
    setError(null);
    try {
      const selectedLesson = await apiRequest<Lesson>(account, `/api/lessons/${lessonId}`);
      setState((current) => ({ ...current, selectedLesson }));
      setMessage("Урок открыт.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Не удалось открыть урок.");
    } finally {
      setLoading(false);
    }
  }

  async function selectModule(moduleId: string) {
    setLoading(true);
    setError(null);
    try {
      const lessons = await apiRequest<LessonSummary[]>(account, `/api/modules/${moduleId}/lessons`);
      const selectedLesson = lessons[0] ? await apiRequest<Lesson>(account, `/api/lessons/${lessons[0].id}`) : undefined;
      const lessonProgress =
        account.role === "STUDENT"
          ? await apiRequest<LessonProgress[]>(account, `/api/modules/${moduleId}/lesson-progress`)
          : [];
      const moduleResult =
        account.role === "STUDENT" ? await apiRequest<ModuleResult>(account, `/api/modules/${moduleId}/result`) : undefined;
      setState((current) => ({
        ...current,
        lessons,
        selectedLesson,
        lessonProgress,
        moduleResult,
        selectedModuleId: moduleId
      }));
      setMessage("Учебный модуль открыт.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Не удалось открыть модуль.");
    } finally {
      setLoading(false);
    }
  }

  async function exportGrades(moduleId: string) {
    setLoading(true);
    setError(null);
    try {
      const csv = await downloadText(account, `/api/modules/${moduleId}/grades/export`);
      const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = `pep-module-${moduleId}-grades.csv`;
      link.click();
      URL.revokeObjectURL(url);
      setMessage("CSV с оценками сформирован.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Не удалось экспортировать оценки.");
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

      <LiveStatusCard status={liveStatus} connected={liveConnected} />

      {loading && <p className="notice">Загрузка...</p>}
      {error && <p className="notice error">Ошибка: {error}</p>}
      {!error && <p className="notice">{message}</p>}

      <section className="grid">
        <CourseCard courses={state.courses} />
        <UserCard account={account} />
      </section>

      {account.role === "STUDENT" && (
        <StudentDashboard
          moduleOptions={moduleOptions}
          selectedModule={selectedModule}
          lessons={state.lessons}
          selectedLesson={state.selectedLesson}
          lessonProgress={state.lessonProgress}
          moduleResult={state.moduleResult}
          submissions={state.submissions}
          validationJobs={state.validationJobs}
          reports={state.reports}
          reviews={state.reviews}
          assignments={state.assignments}
          onSelectModule={selectModule}
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
          onSelectLesson={openLesson}
          onCompleteLesson={(lessonId) =>
            withRefresh(
              () =>
                apiRequest(account, `/api/lessons/${lessonId}/complete`, {
                  method: "POST"
                }),
              "Урок отмечен как изученный."
            )
          }
          onCreateReport={(payload) =>
            withRefresh(
              async () => {
                const { attachment, ...reportPayload } = payload;
                const report = await apiRequest<Report>(account, "/api/reports", {
                  method: "POST",
                  body: JSON.stringify(reportPayload)
                });
                if (attachment) {
                  await uploadReportAttachment(account, report.id, attachment);
                }
              },
              payload.attachment ? "Отчет и вложение отправлены куратору." : "Отчет отправлен куратору."
            )
          }
        />
      )}

      {account.role === "CURATOR" && (
        <CuratorDashboard
          firstModule={labModule}
          submissions={state.submissions}
          validationJobs={state.validationJobs}
          reports={state.reports}
          onExportGrades={exportGrades}
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
          firstModule={labModule}
          onExportGrades={exportGrades}
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

function LiveStatusCard({ status, connected }: { status: LiveStatus | null; connected: boolean }) {
  return (
    <section className="card live-status">
      <div>
        <h2>Live statuses</h2>
        <p className="muted">
          SSE: <StatusBadge value={connected ? "CONNECTED" : "DISCONNECTED"} />
          {status?.updatedAt ? ` обновлено ${new Date(status.updatedAt).toLocaleTimeString("ru-RU")}` : ""}
        </p>
      </div>
      {status ? (
        <dl className="metrics module-result">
          <div>
            <dt>Submissions</dt>
            <dd>{status.submissions}</dd>
          </div>
          <div>
            <dt>Validation jobs</dt>
            <dd>{status.validationJobs}</dd>
          </div>
          <div>
            <dt>Reports</dt>
            <dd>{status.reports}</dd>
          </div>
          <div>
            <dt>Labs running</dt>
            <dd>{status.runningLabs}</dd>
          </div>
          <div>
            <dt>Assignments</dt>
            <dd>{status.assignments}</dd>
          </div>
        </dl>
      ) : (
        <EmptyState>Live snapshot еще не получен.</EmptyState>
      )}
    </section>
  );
}

function StudentDashboard({
  moduleOptions,
  selectedModule,
  lessons,
  selectedLesson,
  lessonProgress,
  moduleResult,
  submissions,
  validationJobs,
  reports,
  reviews,
  assignments,
  onSelectModule,
  onCreateSubmission,
  onSelectLesson,
  onCompleteLesson,
  onCreateReport
}: {
  moduleOptions: ModuleOption[];
  selectedModule?: ModuleOption;
  lessons: LessonSummary[];
  selectedLesson?: Lesson;
  lessonProgress: LessonProgress[];
  moduleResult?: ModuleResult;
  submissions: Submission[];
  validationJobs: ValidationJob[];
  reports: Report[];
  reviews: Review[];
  assignments: BlackBoxAssignment[];
  onSelectModule: (moduleId: string) => Promise<void>;
  onCreateSubmission: (payload: {
    moduleId: string;
    imageReference: string;
    applicationPort: number;
    healthPath: string;
  }) => Promise<void>;
  onSelectLesson: (lessonId: string) => Promise<void>;
  onCompleteLesson: (lessonId: string) => Promise<void>;
  onCreateReport: (payload: {
    moduleId: string;
    submissionId?: string;
    blackBoxAssignmentId?: string;
    type: ReportType;
    title: string;
    contentMarkdown: string;
    attachment?: File | null;
  }) => Promise<void>;
}) {
  const [imageReference, setImageReference] = useState("localhost:5001/vulnerable-sqli-demo:latest");
  const [reportText, setReportText] = useState("Payload: ' OR '1'='1\nEvidence: login bypass воспроизводится.");
  const [reportAttachment, setReportAttachment] = useState<File | null>(null);
  const [blackBoxReportText, setBlackBoxReportText] = useState(
    "Target: назначенный lab\nPayload: ' OR '1'='1\nEvidence: результат поиска раскрывает лишние данные."
  );
  const [blackBoxReportAttachment, setBlackBoxReportAttachment] = useState<File | null>(null);
  const latestSubmission = submissions.find((submission) => submission.moduleId === selectedModule?.id);
  const completedLessonIds = useMemo(
    () => new Set(lessonProgress.filter((item) => item.completed).map((item) => item.lessonId)),
    [lessonProgress]
  );
  const reviewsByReportId = useMemo(
    () => new Map(reviews.map((review) => [review.reportId, review])),
    [reviews]
  );
  const selectedLessonCompleted = selectedLesson ? completedLessonIds.has(selectedLesson.id) : false;
  const reviewedReportsCount = reports.filter((report) => reviewsByReportId.has(report.id)).length;
  const submittedAssignmentsCount = assignments.filter((assignment) => assignment.status === "SUBMITTED").length;

  return (
    <section className="grid">
      <article className="card wide">
        <h2>Учебные материалы модуля</h2>
        {!selectedModule ? (
          <EmptyState>Нет активного модуля.</EmptyState>
        ) : (
          <>
            <label htmlFor="moduleSelect">Курс и модуль</label>
            <select
              id="moduleSelect"
              value={selectedModule.id}
              onChange={(event) => void onSelectModule(event.target.value)}
            >
              {moduleOptions.map((module) => (
                <option key={module.id} value={module.id}>
                  {module.courseTitle}: {module.title}
                </option>
              ))}
            </select>
            <p className="muted">
              {selectedModule.courseTitle}: {selectedModule.title} ({selectedModule.vulnerabilityTopic})
            </p>
            <p className="progress-line">
              Изучено: {completedLessonIds.size} из {lessons.length}
            </p>
            <div className="lesson-layout">
              <div className="lesson-list" aria-label="Список уроков">
                {lessons.map((lesson) => (
                  <button
                    key={lesson.id}
                    type="button"
                    className={lesson.id === selectedLesson?.id ? "lesson-item active" : "lesson-item"}
                    onClick={() => void onSelectLesson(lesson.id)}
                  >
                    {completedLessonIds.has(lesson.id) ? "✓ " : ""}
                    {lesson.position}. {lesson.title}
                  </button>
                ))}
              </div>
              <div className="lesson-content">
                {selectedLesson ? (
                  <>
                    <h3>{selectedLesson.title}</h3>
                    <StatusBadge value={selectedLessonCompleted ? "COMPLETED" : "IN_PROGRESS"} />
                    <pre>{selectedLesson.contentMarkdown}</pre>
                    {!selectedLessonCompleted && (
                      <button type="button" onClick={() => void onCompleteLesson(selectedLesson.id)}>
                        Отметить урок как изученный
                      </button>
                    )}
                  </>
                ) : (
                  <EmptyState>Материалы пока не опубликованы.</EmptyState>
                )}
              </div>
            </div>
          </>
        )}
      </article>

      <article className="card wide">
        <h2>Progress dashboard</h2>
        <div className="chart-grid">
          <ProgressBar label="Изучение уроков" value={completedLessonIds.size} total={lessons.length} />
          <ProgressBar label="Отчеты с feedback" value={reviewedReportsCount} total={reports.length} />
          <ProgressBar label="Black box цели закрыты" value={submittedAssignmentsCount} total={assignments.length} />
          <StatusDistribution title="Submissions по статусам" counts={countByStatus(submissions)} />
          <StatusDistribution title="Reports по статусам" counts={countByStatus(reports)} />
        </div>
      </article>

      <article className="card wide">
        <h2>Итог модуля</h2>
        {!moduleResult ? (
          <EmptyState>Итог появится после выбора учебного модуля.</EmptyState>
        ) : (
          <div className="metrics module-result">
            <div>
              <dt>Docker-допуск</dt>
              <dd>{moduleResult.dockerPassed ? "Да" : "Нет"}</dd>
            </div>
            <div>
              <dt>White box</dt>
              <dd>{moduleResult.whiteBoxScore ?? "-"}</dd>
            </div>
            <div>
              <dt>Black box</dt>
              <dd>{moduleResult.blackBoxScore ?? "-"}</dd>
            </div>
            <div>
              <dt>Итог</dt>
              <dd>{moduleResult.finalScore ?? "-"}</dd>
            </div>
            <div>
              <dt>Статус</dt>
              <dd>
                <StatusBadge value={moduleResult.status} />
              </dd>
            </div>
          </div>
        )}
      </article>

      <article className="card">
        <h2>Student: сдача Docker image</h2>
        {!selectedModule ? (
          <EmptyState>Нет активного модуля.</EmptyState>
        ) : (
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault();
              void onCreateSubmission({
                moduleId: selectedModule.id,
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
        {!selectedModule || !latestSubmission ? (
          <EmptyState>Сначала отправьте Docker image.</EmptyState>
        ) : (
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault();
              void onCreateReport({
                moduleId: selectedModule.id,
                submissionId: latestSubmission.id,
                type: "WHITE_BOX",
                title: "White box отчет по SQL Injection",
                contentMarkdown: reportText,
                attachment: reportAttachment
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
            <div className="preview-panel">
              <strong>Preview отчета</strong>
              <MarkdownPreview source={reportText} />
            </div>
            <label htmlFor="reportAttachment">Вложение с evidence (опционально)</label>
            <input
              id="reportAttachment"
              type="file"
              onChange={(event) => setReportAttachment(event.target.files?.[0] ?? null)}
            />
            {reportAttachment && (
              <p className="muted">
                Выбрано: {reportAttachment.name} ({formatBytes(reportAttachment.size)})
              </p>
            )}
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
              <ImageScanSummary job={job} />
              <DependencyScanSummary job={job} />
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
              <ReportAttachmentsList attachments={report.attachments} />
              {reviewsByReportId.has(report.id) ? (
                <div className="review-box feedback-box">
                  <strong>Feedback куратора</strong>
                  <p>
                    Решение: <StatusBadge value={reviewsByReportId.get(report.id)!.decision} />
                  </p>
                  <p className="big">{reviewsByReportId.get(report.id)!.score}/100</p>
                  <MarkdownPreview source={reviewsByReportId.get(report.id)!.commentMarkdown} />
                </div>
              ) : (
                <p className="muted">Feedback еще не получен.</p>
              )}
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
              {selectedModule && assignment.status !== "SUBMITTED" && (
                <form
                  className="form nested-form"
                  onSubmit={(event) => {
                    event.preventDefault();
                    void onCreateReport({
                      moduleId: assignment.moduleId,
                      blackBoxAssignmentId: assignment.id,
                      type: "BLACK_BOX",
                      title: "Black box отчет по SQL Injection",
                      contentMarkdown: blackBoxReportText,
                      attachment: blackBoxReportAttachment
                    });
                  }}
                >
                  <label htmlFor={`blackBoxReport-${assignment.id}`}>Black box evidence</label>
                  <textarea
                    id={`blackBoxReport-${assignment.id}`}
                    rows={5}
                    value={blackBoxReportText}
                    onChange={(event) => setBlackBoxReportText(event.target.value)}
                  />
                  <div className="preview-panel">
                    <strong>Preview black box отчета</strong>
                    <MarkdownPreview source={blackBoxReportText} />
                  </div>
                  <label htmlFor={`blackBoxAttachment-${assignment.id}`}>Вложение с evidence (опционально)</label>
                  <input
                    id={`blackBoxAttachment-${assignment.id}`}
                    type="file"
                    onChange={(event) => setBlackBoxReportAttachment(event.target.files?.[0] ?? null)}
                  />
                  {blackBoxReportAttachment && (
                    <p className="muted">
                      Выбрано: {blackBoxReportAttachment.name} ({formatBytes(blackBoxReportAttachment.size)})
                    </p>
                  )}
                  <button type="submit">Отправить black box отчет</button>
                </form>
              )}
            </>
          )}
        />
      </article>
    </section>
  );
}

function CuratorDashboard({
  firstModule,
  submissions,
  validationJobs,
  reports,
  onExportGrades,
  onCompleteValidation,
  onCreateReview
}: {
  firstModule?: LearningModule;
  submissions: Submission[];
  validationJobs: ValidationJob[];
  reports: Report[];
  onExportGrades: (moduleId: string) => Promise<void>;
  onCompleteValidation: (jobId: string, passed: boolean) => Promise<void>;
  onCreateReview: (payload: {
    reportId: string;
    decision: ReviewDecision;
    score: number;
    commentMarkdown: string;
  }) => Promise<void>;
}) {
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState<ReportType | "ALL">("ALL");
  const [authorFilter, setAuthorFilter] = useState("");
  const reportStatuses = useMemo(() => Array.from(new Set(reports.map((report) => report.status))).sort(), [reports]);
  const filteredReports = useMemo(
    () =>
      reports.filter((report) => {
        const statusMatches = statusFilter === "ALL" || report.status === statusFilter;
        const typeMatches = typeFilter === "ALL" || report.type === typeFilter;
        const authorMatches = report.authorEmail.toLowerCase().includes(authorFilter.trim().toLowerCase());
        return statusMatches && typeMatches && authorMatches;
      }),
    [authorFilter, reports, statusFilter, typeFilter]
  );

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
              <ImageScanSummary job={job} />
              <DependencyScanSummary job={job} />
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
        {firstModule && (
          <div className="actions">
            <button type="button" className="secondary" onClick={() => void onExportGrades(firstModule.id)}>
              Экспорт оценок CSV
            </button>
          </div>
        )}
        <div className="filter-panel">
          <div>
            <label htmlFor="reportStatusFilter">Статус</label>
            <select id="reportStatusFilter" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="ALL">Все статусы</option>
              {reportStatuses.map((status) => (
                <option key={status} value={status}>
                  {statusLabels[status] ?? status}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="reportTypeFilter">Тип отчета</label>
            <select
              id="reportTypeFilter"
              value={typeFilter}
              onChange={(event) => setTypeFilter(event.target.value as ReportType | "ALL")}
            >
              <option value="ALL">Все типы</option>
              <option value="WHITE_BOX">White box</option>
              <option value="BLACK_BOX">Black box</option>
            </select>
          </div>
          <div>
            <label htmlFor="reportAuthorFilter">Автор</label>
            <input
              id="reportAuthorFilter"
              placeholder="student@pep.local"
              value={authorFilter}
              onChange={(event) => setAuthorFilter(event.target.value)}
            />
          </div>
        </div>
        <p className="muted">
          Найдено отчетов: {filteredReports.length} из {reports.length}
        </p>
        <EntityList
          title="Очередь отчетов"
          items={filteredReports}
          render={(report) => (
            <ReviewForm
              key={report.id}
              report={report}
              submissions={submissions}
              validationJobs={validationJobs}
              onCreateReview={onCreateReview}
            />
          )}
        />
      </article>
    </section>
  );
}

function ReviewForm({
  report,
  submissions,
  validationJobs,
  onCreateReview
}: {
  report: Report;
  submissions: Submission[];
  validationJobs: ValidationJob[];
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
  const submission = submissions.find((item) => item.id === report.submissionId);
  const validationJob = validationJobs.find((job) => job.submissionId === report.submissionId);

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
      <p className="muted">
        Тип: {report.type === "WHITE_BOX" ? "White box" : "Black box"}
        {report.blackBoxAssignmentId ? `, assignment: ${report.blackBoxAssignmentId}` : ""}
      </p>
      {submission && (
        <div className="feedback-box">
          <strong>Submission context</strong>
          <p>{submission.imageReference}</p>
          <StatusBadge value={submission.status} />
          <p className="muted">
            Port: {submission.applicationPort}, health: {submission.healthPath}
          </p>
        </div>
      )}
      {validationJob && (
        <div className="feedback-box">
          <strong>Technical validation</strong>
          <StatusBadge value={validationJob.status} />
          <ImageScanSummary job={validationJob} />
          <DependencyScanSummary job={validationJob} />
          {validationJob.logsUri && <p className="muted">Logs: {validationJob.logsUri}</p>}
          {validationJob.errorMessage && <p className="error-text">{validationJob.errorMessage}</p>}
        </div>
      )}
      <MarkdownPreview source={report.contentMarkdown} />
      <ReportAttachmentsList attachments={report.attachments} />
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
      <div className="preview-panel">
        <strong>Preview комментария</strong>
        <MarkdownPreview source={comment} />
      </div>
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
  onExportGrades,
  onCreateLab,
  onDistribute
}: {
  submissions: Submission[];
  validationJobs: ValidationJob[];
  reports: Report[];
  labs: Lab[];
  auditEvents: AuditEvent[];
  firstModule?: LearningModule;
  onExportGrades: (moduleId: string) => Promise<void>;
  onCreateLab: (submissionId: string) => Promise<void>;
  onDistribute: (moduleId: string) => Promise<void>;
}) {
  const approvedSubmissions = submissions.filter((submission) => submission.status === "APPROVED");
  const labBySubmissionId = useMemo(() => new Map(labs.map((lab) => [lab.submissionId, lab])), [labs]);
  const labSubmissionIds = new Set(labBySubmissionId.keys());
  const pendingLabSubmissions = approvedSubmissions.filter((submission) => !labSubmissionIds.has(submission.id));
  const runningLabs = labs.filter((lab) => lab.status === "RUNNING");
  const labStatusCounts = useMemo(
    () => countByStatus(labs),
    [labs]
  );
  const passedValidationJobs = validationJobs.filter((job) => job.status === "PASSED").length;
  const approvedReports = reports.filter((report) => report.status === "APPROVED").length;

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

      <article className="card wide">
        <h2>Admin: состояние lab runtime</h2>
        <dl className="metrics module-result">
          <div>
            <dt>Готовы к lab</dt>
            <dd>{approvedSubmissions.length}</dd>
          </div>
          <div>
            <dt>Без lab</dt>
            <dd>{pendingLabSubmissions.length}</dd>
          </div>
          <div>
            <dt>Running labs</dt>
            <dd>{runningLabs.length}</dd>
          </div>
          <div>
            <dt>Статусы</dt>
            <dd>{Object.keys(labStatusCounts).length || "-"}</dd>
          </div>
        </dl>
        <div className="lab-status-grid">
          <div className="feedback-box">
            <strong>Lab readiness</strong>
            <p className="muted">Approved submissions должны получить lab instance перед black box distribution.</p>
            {pendingLabSubmissions.length === 0 ? (
              <StatusBadge value="COMPLETED" />
            ) : (
              <p className="error-text">Ожидают lab: {pendingLabSubmissions.length}</p>
            )}
          </div>
          <div className="feedback-box">
            <strong>Lab statuses</strong>
            {Object.entries(labStatusCounts).length === 0 ? (
              <p className="muted">Lab instances пока не созданы.</p>
            ) : (
              <ul className="compact-list">
                {Object.entries(labStatusCounts).map(([status, count]) => (
                  <li key={status}>
                    <StatusBadge value={status} /> {count}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </article>

      <article className="card wide">
        <h2>Progress analytics</h2>
        <div className="chart-grid">
          <ProgressBar label="Technical validation passed" value={passedValidationJobs} total={validationJobs.length} />
          <ProgressBar label="Approved reports" value={approvedReports} total={reports.length} />
          <ProgressBar label="Labs created" value={labs.length} total={approvedSubmissions.length} />
          <StatusDistribution title="Validation jobs по статусам" counts={countByStatus(validationJobs)} />
          <StatusDistribution title="Labs по статусам" counts={labStatusCounts} />
        </div>
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
                <>
                  <StatusBadge value={labBySubmissionId.get(submission.id)!.status} />
                  <p className="muted">Namespace: {labBySubmissionId.get(submission.id)!.namespace}</p>
                </>
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
              <p className="muted">Student: {lab.studentEmail}</p>
              <p className="muted">Image: {lab.imageReference}</p>
              <p className="muted">Ingress URL:</p>
              <code>{lab.ingressUrl}</code>
              <p className="muted">
                {lab.namespace} / {lab.serviceName}
              </p>
              <p className="muted">Expires: {new Date(lab.expiresAt).toLocaleString("ru-RU")}</p>
              <p className="muted">Install ingress controller:</p>
              <code>{lab.ingressInstallCommand}</code>
              <p className="muted">Deploy в kind:</p>
              <code>{lab.deployCommand}</code>
              <p className="muted">Port-forward:</p>
              <code>{lab.portForwardCommand}</code>
            </>
          )}
        />
        {firstModule ? (
          <div className="actions">
            <button type="button" onClick={() => void onDistribute(firstModule.id)}>
              Распределить black box цели
            </button>
            <button type="button" className="secondary" onClick={() => void onExportGrades(firstModule.id)}>
              Экспорт оценок CSV
            </button>
          </div>
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
