import React, { useEffect, useMemo, useState } from "react";
import ReactDOM from "react-dom/client";
import "./styles.css";

type Role = "STUDENT" | "CURATOR" | "ADMIN";
type ReportType = "WHITE_BOX" | "BLACK_BOX";
type ReviewDecision = "APPROVED" | "NEEDS_REVISION" | "REJECTED";
type SubmissionSourceType = "IMAGE_REFERENCE" | "ARCHIVE";

type AuthCredentials = {
  email: string;
  password: string;
};

type AccountSession = {
  email: string;
  role: Role;
  displayName: string;
};

type AppPage = "workspace" | "courses" | "profile";

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

type MarkdownHeading = {
  id: string;
  title: string;
  level: number;
};

type MarkdownHeadingGroup = {
  heading: MarkdownHeading;
  children: MarkdownHeading[];
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
  sourceType: SubmissionSourceType;
  archiveFilename?: string;
  composeService?: string;
  buildContext?: string;
  runtimeImageReference?: string;
  publicUrl?: string;
  localHostUrl?: string;
  applicationPort: number;
  healthPath: string;
  status: string;
  createdAt?: string;
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
  sourceType: SubmissionSourceType;
  runtimeImageReference?: string;
  publicUrl?: string;
  localHostUrl?: string;
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

type CurrentUser = {
  email: string;
  displayName: string;
  role: Role;
};

type AdminUser = {
  id: string;
  email: string;
  displayName: string;
  role: Role;
  status: string;
};

type OnlineUsers = {
  activeUsers: number;
  activeSessions: number;
};

type CsrfToken = {
  token: string;
  headerName: string;
  parameterName: string;
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
  users: AdminUser[];
  onlineUsers?: OnlineUsers;
};

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "";
const REPORT_MARKDOWN_MAX_LENGTH = 20_000;
const REVIEW_COMMENT_MAX_LENGTH = 8_000;
const LOGIN_EMAIL_MAX_LENGTH = 320;
const LOGIN_PASSWORD_MAX_LENGTH = 256;
const DEFAULT_IMAGE_REFERENCE = "localhost:5001/vulnerable-sqli-demo:latest";
const DEFAULT_WHITEBOX_REPORT =
  "Payload: ' OR '1'='1\nEvidence: login bypass воспроизводится.";
const DEFAULT_BLACKBOX_REPORT =
  "Target: назначенный lab\nPayload: ' OR '1'='1\nEvidence: результат поиска раскрывает лишние данные.";
let csrfTokenCache: CsrfToken | null = null;

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

function isUnsafeMethod(method?: string) {
  const normalized = (method ?? "GET").toUpperCase();
  return normalized !== "GET" && normalized !== "HEAD" && normalized !== "OPTIONS" && normalized !== "TRACE";
}

async function csrfHeaders(init?: RequestInit): Promise<Record<string, string>> {
  if (!isUnsafeMethod(init?.method)) {
    return {};
  }

  if (!csrfTokenCache) {
    const response = await fetch(`${apiBaseUrl}/api/auth/csrf`, {
      credentials: "include"
    });
    if (!response.ok) {
      throw new Error(`Не удалось получить CSRF token: ${response.status}`);
    }
    csrfTokenCache = (await response.json()) as CsrfToken;
  }

  return { [csrfTokenCache.headerName]: csrfTokenCache.token };
}

async function loginRequest(credentials: AuthCredentials): Promise<CurrentUser> {
  const csrf = await csrfHeaders({ method: "POST" });
  const response = await fetch(`${apiBaseUrl}/api/auth/login`, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...csrf
    },
    body: JSON.stringify(credentials)
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(formatApiError(response.status, error, "Не удалось выполнить вход"));
  }

  return response.json() as Promise<CurrentUser>;
}

async function logoutRequest(): Promise<void> {
  const csrf = await csrfHeaders({ method: "POST" });
  await fetch(`${apiBaseUrl}/api/auth/logout`, {
    method: "POST",
    credentials: "include",
    headers: csrf
  });
  csrfTokenCache = null;
}

async function apiRequest<T>(_account: AccountSession, path: string, init?: RequestInit): Promise<T> {
  const requestUrl = `${apiBaseUrl}${path}`;
  const csrf = await csrfHeaders(init);
  const response = await fetch(requestUrl, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...csrf,
      ...init?.headers
    }
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(formatApiError(response.status, error, `Запрос ${path} не выполнен`));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function uploadReportAttachment(_account: AccountSession, reportId: string, file: File): Promise<ReportAttachment> {
  const formData = new FormData();
  formData.append("file", file);
  const csrf = await csrfHeaders({ method: "POST" });

  const response = await fetch(`${apiBaseUrl}/api/reports/${reportId}/attachments`, {
    method: "POST",
    credentials: "include",
    headers: csrf,
    body: formData
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(formatApiError(response.status, error, "Не удалось загрузить вложение"));
  }

  return response.json() as Promise<ReportAttachment>;
}

async function uploadSubmissionArchive(
  _account: AccountSession,
  payload: {
    moduleId: string;
    archive: File;
    applicationPort: number;
    healthPath: string;
    composeService: string;
  }
): Promise<Submission> {
  const formData = new FormData();
  formData.append("moduleId", payload.moduleId);
  formData.append("applicationPort", String(payload.applicationPort));
  formData.append("healthPath", payload.healthPath);
  if (payload.composeService.trim()) {
    formData.append("composeService", payload.composeService.trim());
  }
  formData.append("archive", payload.archive);
  const csrf = await csrfHeaders({ method: "POST" });

  const response = await fetch(`${apiBaseUrl}/api/submissions/archive`, {
    method: "POST",
    credentials: "include",
    headers: csrf,
    body: formData
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(formatApiError(response.status, error, "Не удалось загрузить архив стенда"));
  }

  return response.json() as Promise<Submission>;
}

async function downloadText(_account: AccountSession, path: string): Promise<string> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    credentials: "include"
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(formatApiError(response.status, error, `Не удалось скачать ${path}`));
  }

  return response.text();
}

function formatApiError(status: number, error: unknown, fallback: string) {
  const payload = error as { code?: string; message?: string } | null;
  const statusText: Record<number, string> = {
    400: "Проверьте данные формы",
    401: "Нужно войти в систему",
    403: "Недостаточно прав для этой операции",
    404: "Данные не найдены",
    409: "Операция конфликтует с текущим состоянием",
    413: "Файл слишком большой",
    429: "Слишком много попыток, повторите позже",
    500: "Внутренняя ошибка сервера"
  };
  const message = payload?.message || statusText[status] || fallback;
  const code = payload?.code ? ` (${payload.code})` : "";
  return `${message}${code}. HTTP ${status}.`;
}

function liveStatusStreamUrl() {
  const baseUrl = apiBaseUrl || window.location.origin;
  return new URL("/api/live/status-stream", baseUrl).toString();
}

function StatusBadge({ value }: { value: string }) {
  return <span className="badge">{statusLabels[value] ?? value}</span>;
}

function EmptyState({ children }: { children: React.ReactNode }) {
  return <p className="muted empty">{children}</p>;
}

function MarkdownPreview({ source, withHeadingIds = false }: { source: string; withHeadingIds?: boolean }) {
  const rendered = useMemo(() => renderMarkdown(source, withHeadingIds), [source, withHeadingIds]);

  if (source.trim().length === 0) {
    return <EmptyState>Markdown preview появится после ввода текста.</EmptyState>;
  }

  return <div className="markdown-preview">{rendered}</div>;
}

function renderMarkdown(source: string, withHeadingIds = false) {
  const lines = source.replace(/\r\n/g, "\n").split("\n");
  const elements: React.ReactNode[] = [];
  const uniqueSlug = createUniqueSlugFactory();
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
      const id = withHeadingIds ? uniqueSlug(plainMarkdownText(heading[2])) : undefined;
      elements.push(
        level === 1 ? (
          <h1 key={`heading-${index}`} id={id}>
            {content}
          </h1>
        ) : level === 2 ? (
          <h2 key={`heading-${index}`} id={id}>
            {content}
          </h2>
        ) : (
          <h3 key={`heading-${index}`} id={id}>
            {content}
          </h3>
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

function extractMarkdownHeadings(source: string): MarkdownHeading[] {
  const uniqueSlug = createUniqueSlugFactory();
  return source
    .replace(/\r\n/g, "\n")
    .split("\n")
    .map((line) => /^(#{2,3})\s+(.+)$/.exec(line))
    .filter((match): match is RegExpExecArray => Boolean(match))
    .map((match) => {
      const title = plainMarkdownText(match[2]);
      return {
        id: uniqueSlug(title),
        title,
        level: match[1].length
      };
    });
}

function groupMarkdownHeadings(headings: MarkdownHeading[]): MarkdownHeadingGroup[] {
  const groups: MarkdownHeadingGroup[] = [];
  headings.forEach((heading) => {
    if (heading.level <= 2 || groups.length === 0) {
      groups.push({ heading, children: [] });
      return;
    }
    groups[groups.length - 1].children.push(heading);
  });
  return groups;
}

function createUniqueSlugFactory() {
  const seen = new Map<string, number>();
  return (value: string) => {
    const baseSlug = slugify(value) || "section";
    const count = seen.get(baseSlug) ?? 0;
    seen.set(baseSlug, count + 1);
    return count === 0 ? baseSlug : `${baseSlug}-${count + 1}`;
  };
}

function plainMarkdownText(value: string) {
  return value
    .replace(/\[[^\]]+\]\(([^)]+)\)/g, "$1")
    .replace(/[`*_]/g, "")
    .trim();
}

function slugify(value: string) {
  const transliteration: Record<string, string> = {
    а: "a",
    б: "b",
    в: "v",
    г: "g",
    д: "d",
    е: "e",
    ё: "e",
    ж: "zh",
    з: "z",
    и: "i",
    й: "y",
    к: "k",
    л: "l",
    м: "m",
    н: "n",
    о: "o",
    п: "p",
    р: "r",
    с: "s",
    т: "t",
    у: "u",
    ф: "f",
    х: "h",
    ц: "c",
    ч: "ch",
    ш: "sh",
    щ: "sch",
    ъ: "",
    ы: "y",
    ь: "",
    э: "e",
    ю: "yu",
    я: "ya"
  };
  return value
    .toLowerCase()
    .split("")
    .map((char) => transliteration[char] ?? char)
    .join("")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function webSecurityPath(module: ModuleOption) {
  const source = `${module.title} ${module.vulnerabilityTopic}`.toLowerCase();
  if (source.includes("cors")) return "/web-security/cors";
  if (source.includes("injection") || source.includes("sql")) return "/web-security/sql-injection";
  if (source.includes("access")) return "/web-security/access-control";
  if (source.includes("crypto")) return "/web-security/cryptography";
  if (source.includes("ssrf")) return "/web-security/ssrf";
  if (source.includes("authentication")) return "/web-security/authentication";
  if (source.includes("configuration")) return "/web-security/security-misconfiguration";
  if (source.includes("components")) return "/web-security/vulnerable-components";
  return `/web-security/${slugify(module.vulnerabilityTopic || module.title)}`;
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
  const parts = text.split(/(`[^`]+`|\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\))/g);
  return parts.map((part, index) => {
    if (part.startsWith("`") && part.endsWith("`")) {
      return <code key={index}>{part.slice(1, -1)}</code>;
    }
    if (part.startsWith("**") && part.endsWith("**")) {
      return <strong key={index}>{part.slice(2, -2)}</strong>;
    }
    const link = /^\[([^\]]+)\]\(([^)]+)\)$/.exec(part);
    if (link) {
      return <SafeMarkdownLink key={index} label={link[1]} href={link[2]} />;
    }
    return part;
  });
}

function SafeMarkdownLink({ label, href }: { label: string; href: string }) {
  const safeHref = normalizeSafeMarkdownHref(href);
  if (!safeHref) {
    return <span className="muted">{label}</span>;
  }
  return (
    <a href={safeHref} target="_blank" rel="noopener noreferrer">
      {label}
    </a>
  );
}

function normalizeSafeMarkdownHref(href: string) {
  const trimmed = href.trim();
  if (trimmed.startsWith("/") || trimmed.startsWith("#")) {
    return trimmed;
  }
  try {
    const parsed = new URL(trimmed);
    return parsed.protocol === "https:" || parsed.protocol === "http:" ? parsed.toString() : null;
  } catch {
    return null;
  }
}

function ReportAttachmentsList({ attachments }: { attachments: ReportAttachment[] }) {
  if (attachments.length === 0) {
    return <p className="muted">Вложений нет.</p>;
  }

  return (
    <ul className="attachment-list">
      {attachments.map((attachment) => (
        <li key={attachment.id}>
          <a href={`${apiBaseUrl}/api/report-attachments/${attachment.id}`}>
            {attachment.originalFilename}
          </a>
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
  const [account, setAccount] = useState<AccountSession | null>(null);
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
    auditEvents: [],
    users: [],
    onlineUsers: undefined
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("Войдите в систему, чтобы загрузить рабочую область.");
  const [error, setError] = useState<string | null>(null);
  const [liveStatus, setLiveStatus] = useState<LiveStatus | null>(null);
  const [liveConnected, setLiveConnected] = useState(false);
  const [activePage, setActivePage] = useState<AppPage>("workspace");

  useEffect(() => {
    if (!error) {
      return undefined;
    }
    const timeoutId = window.setTimeout(() => setError(null), 10_000);
    return () => window.clearTimeout(timeoutId);
  }, [error]);

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

  async function login(credentials: AuthCredentials) {
    setLoading(true);
    setError(null);
    try {
      const currentUser = await loginRequest(credentials);
      const nextAccount: AccountSession = {
        email: currentUser.email,
        role: currentUser.role,
        displayName: currentUser.displayName
      };
      setAccount(nextAccount);
      const loaded = await loadDashboard(nextAccount);
      if (loaded) {
        setActivePage("courses");
        setMessage("Вход выполнен.");
      }
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Не удалось выполнить вход.");
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    void logoutRequest();
    setAccount(null);
    setLiveStatus(null);
    setLiveConnected(false);
    setActivePage("workspace");
    setState({
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
      auditEvents: [],
      users: [],
      onlineUsers: undefined
    });
    setMessage("Сессия завершена.");
  }

  async function loadDashboard(activeAccount: AccountSession): Promise<boolean> {
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
      const users =
        activeAccount.role === "ADMIN" ? await apiRequest<AdminUser[]>(activeAccount, "/api/admin/users") : [];
      const onlineUsers =
        activeAccount.role === "ADMIN"
          ? await apiRequest<OnlineUsers>(activeAccount, "/api/admin/online-users")
          : undefined;
      setState({
        courses,
        submissions,
        validationJobs,
        reports,
        reviews,
        lessons,
        selectedLesson,
        lessonProgress: [],
        moduleResult: undefined,
        selectedModuleId: nextSelectedModule?.id,
        labs,
        assignments,
        auditEvents,
        users,
        onlineUsers
      });
      setMessage("Данные загружены.");
      return true;
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Не удалось загрузить данные.");
      return false;
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!account) {
      return undefined;
    }
    const events = new EventSource(liveStatusStreamUrl(), { withCredentials: true });
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

  async function withRefresh(action: () => Promise<void>, successMessage: string): Promise<boolean> {
    setLoading(true);
    setError(null);
    try {
      await action();
      if (!account) {
        throw new Error("Сначала выполните вход.");
      }
      const loaded = await loadDashboard(account);
      if (!loaded) {
        return false;
      }
      setMessage(successMessage);
      return true;
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Операция не выполнена.");
      return false;
    } finally {
      setLoading(false);
    }
  }

  async function selectModule(moduleId: string) {
    if (!account) {
      setError("Сначала выполните вход.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const lessons = await apiRequest<LessonSummary[]>(account, `/api/modules/${moduleId}/lessons`);
      const selectedLesson = lessons[0] ? await apiRequest<Lesson>(account, `/api/lessons/${lessons[0].id}`) : undefined;
      setState((current) => ({
        ...current,
        lessons,
        selectedLesson,
        lessonProgress: [],
        moduleResult: undefined,
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
    if (!account) {
      setError("Сначала выполните вход.");
      return;
    }
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
    <main className="app-shell">
      {!account ? (
        <section className="login-screen">
          <LoginForm onLogin={login} disabled={loading} />
        </section>
      ) : (
        <>
          <TopNavigation
            account={account}
            activePage={activePage}
            loading={loading}
            onSelectPage={setActivePage}
            onLogout={logout}
          />
          {loading && <div className="loading-line" aria-label="Загрузка" />}
          {error && (
            <div className="toast error" role="alert">
              <strong>Ошибка</strong>
              <span>{error}</span>
            </div>
          )}
        </>
      )}

      {account && activePage === "courses" && (
        <CoursesPage
          courses={state.courses}
          selectedModuleId={state.selectedModuleId}
          onSelectModule={(moduleId) => {
            if (account.role === "ADMIN") {
              setActivePage("workspace");
              return;
            }
            setActivePage("workspace");
            void selectModule(moduleId);
          }}
        />
      )}

      {account && activePage === "profile" && (
        <ProfilePage
          account={account}
          courses={state.courses}
          liveStatus={liveStatus}
          liveConnected={liveConnected}
          selectedModuleId={state.selectedModuleId}
          onSelectModule={(moduleId) => {
            setActivePage("workspace");
            void selectModule(moduleId);
          }}
        />
      )}

      {account?.role === "STUDENT" && activePage === "workspace" && (
        <StudentDashboard
          moduleOptions={moduleOptions}
          selectedModule={selectedModule}
          lessons={state.lessons}
          selectedLesson={state.selectedLesson}
          moduleResult={state.moduleResult}
          submissions={state.submissions}
          validationJobs={state.validationJobs}
          reports={state.reports}
          reviews={state.reviews}
          assignments={state.assignments}
          isBusy={loading}
          onSelectModule={selectModule}
          onCreateSubmission={async (payload) => {
            await withRefresh(
              () =>
                apiRequest(account, "/api/submissions", {
                  method: "POST",
                  body: JSON.stringify(payload)
                }),
              "Submission создан, validation job поставлен в очередь."
            );
          }}
          onCreateArchiveSubmission={async (payload) =>
            withRefresh(
              async () => {
                await uploadSubmissionArchive(account, payload);
              },
              "Архив стенда загружен, сборка и technical validation поставлены в очередь."
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

      {account?.role === "CURATOR" && activePage === "workspace" && (
        <CuratorDashboard
          firstModule={labModule}
          submissions={state.submissions}
          validationJobs={state.validationJobs}
          reports={state.reports}
          isBusy={loading}
          onExportGrades={exportGrades}
          onCompleteValidation={async (jobId, passed) => {
            await withRefresh(
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
            );
          }}
          onCreateReview={async (payload) => {
            await withRefresh(
              () =>
                apiRequest(account, "/api/reviews", {
                  method: "POST",
                  body: JSON.stringify(payload)
                }),
              "Решение куратора сохранено."
            );
          }}
        />
      )}

      {account?.role === "ADMIN" && activePage === "workspace" && (
        <AdminDashboard
          submissions={state.submissions}
          validationJobs={state.validationJobs}
          reports={state.reports}
          labs={state.labs}
          auditEvents={state.auditEvents}
          courses={state.courses}
          users={state.users}
          onlineUsers={state.onlineUsers}
          firstModule={labModule}
          isBusy={loading}
          onRefresh={() => void loadDashboard(account)}
          onCreateCourse={(payload) =>
            withRefresh(
              () =>
                apiRequest(account, "/api/admin/courses", {
                  method: "POST",
                  body: JSON.stringify(payload)
                }),
              "Курс сохранен."
            )
          }
          onCreateModule={(payload) =>
            withRefresh(
              () =>
                apiRequest(account, "/api/admin/modules", {
                  method: "POST",
                  body: JSON.stringify(payload)
                }),
              "Модуль сохранен."
            )
          }
          onUpsertLesson={(payload) =>
            withRefresh(
              () =>
                apiRequest(account, "/api/admin/lessons", {
                  method: "POST",
                  body: JSON.stringify(payload)
                }),
              "Страница модуля сохранена."
            )
          }
          onCreateUser={(payload) =>
            withRefresh(
              () =>
                apiRequest(account, "/api/admin/users", {
                  method: "POST",
                  body: JSON.stringify(payload)
                }),
              "Пользователь создан."
            )
          }
          onDisableUser={(userId) =>
            withRefresh(
              () =>
                apiRequest(account, `/api/admin/users/${userId}`, {
                  method: "DELETE"
                }),
              "Пользователь удален."
            )
          }
          onExportGrades={exportGrades}
          onCreateLab={async (submissionId) => {
            await withRefresh(
              () =>
                apiRequest(account, "/api/labs", {
                  method: "POST",
                  body: JSON.stringify({ submissionId })
                }),
              "Lab instance создан для принятой работы."
            );
          }}
          onDistribute={async (moduleId) => {
            await withRefresh(
              () =>
                apiRequest(account, `/api/modules/${moduleId}/black-box-assignments/distribute`, {
                  method: "POST"
                }),
              "Black box цели распределены."
            );
          }}
        />
      )}
    </main>
  );
}

function TopNavigation({
  account,
  activePage,
  loading,
  onSelectPage,
  onLogout
}: {
  account: AccountSession;
  activePage: AppPage;
  loading: boolean;
  onSelectPage: (page: AppPage) => void;
  onLogout: () => void;
}) {
  const pages: Array<{ id: AppPage; label: string }> = [
    { id: "workspace", label: "Рабочая область" },
    { id: "courses", label: "Курсы" },
    { id: "profile", label: "Личный кабинет" }
  ];

  return (
    <header className="academy-header">
      <div className="academy-header-top">
        <div className="academy-brand">
          <span className="academy-logo-mark">P</span>
          <div>
            <strong>PEP Academy</strong>
            <span>Web Security Training</span>
          </div>
        </div>
        <div className="academy-user">
          <span>{account.displayName}</span>
          <small>{roleLabels[account.role]}</small>
        </div>
      </div>
      <div className="academy-header-bottom">
        <nav className="academy-nav-links" aria-label="Основные разделы">
          {pages.map((page) => (
            <button
              key={page.id}
              type="button"
              className={activePage === page.id ? "academy-nav-link active" : "academy-nav-link"}
              disabled={loading}
              onClick={() => onSelectPage(page.id)}
            >
              {page.label}
            </button>
          ))}
        </nav>
        <div className="academy-nav-actions">
          <button type="button" className="academy-action logout-icon" aria-label="Выйти" title="Выйти" disabled={loading} onClick={onLogout}>
            ⎋
          </button>
        </div>
      </div>
    </header>
  );
}

function CoursesPage({
  courses,
  selectedModuleId,
  onSelectModule
}: {
  courses: Course[];
  selectedModuleId?: string;
  onSelectModule: (moduleId: string) => void;
}) {
  return (
    <section className="page-section courses-page">
      {courses.length === 0 ? (
        <EmptyState>Курсы пока не загружены.</EmptyState>
      ) : (
        <div className="course-category-list">
          {courses.map((course) => (
            <details key={course.id} className="course-category" open>
              <summary>
                <span>{course.title}</span>
                <span className="course-category-arrow" aria-hidden="true" />
              </summary>
              <p>{course.description}</p>
              <div className="course-card-row">
                {course.modules.map((module) => (
                  <button
                    key={module.id}
                    type="button"
                    className={module.id === selectedModuleId ? "course-tile active" : "course-tile"}
                    onClick={() => onSelectModule(module.id)}
                  >
                    <span className="course-tile-cover">
                      <span>{module.title}</span>
                    </span>
                    <span className="course-tile-title">
                      {module.title}
                    </span>
                  </button>
                ))}
              </div>
            </details>
          ))}
        </div>
      )}
    </section>
  );
}

function ProfilePage({
  account,
  courses,
  liveStatus,
  liveConnected,
  selectedModuleId,
  onSelectModule
}: {
  account: AccountSession;
  courses: Course[];
  liveStatus: LiveStatus | null;
  liveConnected: boolean;
  selectedModuleId?: string;
  onSelectModule: (moduleId: string) => void;
}) {
  const modulesCount = courses.reduce((total, course) => total + course.modules.length, 0);
  const isAdmin = account.role === "ADMIN";

  return (
    <section className="page-section">
      <header className="page-section-header">
        <h1>Личный кабинет</h1>
      </header>
      <div className="profile-grid">
        <article className="profile-card">
          <h2>{account.displayName}</h2>
          <p className="muted mono-wrap">{account.email}</p>
          <StatusBadge value={roleLabels[account.role]} />
        </article>
        {!isAdmin && (
          <article className="profile-card">
            <h2>Курсы</h2>
            <p className="big">{courses.length}</p>
            <p className="muted">Модулей: {modulesCount}</p>
          </article>
        )}
        {!isAdmin && (
          <article className="profile-card">
            <h2>Статус</h2>
            <StatusBadge value={liveConnected ? "ONLINE" : "OFFLINE"} />
            {liveStatus?.updatedAt && <p className="muted">Обновлено {new Date(liveStatus.updatedAt).toLocaleTimeString("ru-RU")}</p>}
          </article>
        )}
      </div>
      {!isAdmin && <CoursesPage courses={courses} selectedModuleId={selectedModuleId} onSelectModule={onSelectModule} />}
    </section>
  );
}

function LoginForm({ onLogin, disabled }: { onLogin: (credentials: AuthCredentials) => Promise<void>; disabled: boolean }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const normalizedEmail = email.trim();
  const canSubmit = !disabled && normalizedEmail.length > 0 && password.length > 0;

  return (
    <section className="card login-card">
      <h2>Вход в платформу</h2>
      <form
        className="form compact-form"
        onSubmit={(event) => {
          event.preventDefault();
          if (!canSubmit) {
            return;
          }
          void onLogin({ email: normalizedEmail, password });
        }}
      >
        <label htmlFor="loginEmail">Email</label>
        <input
          id="loginEmail"
          type="email"
          autoComplete="username"
          maxLength={LOGIN_EMAIL_MAX_LENGTH}
          disabled={disabled}
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />
        <label htmlFor="loginPassword">Пароль</label>
        <input
          id="loginPassword"
          type="password"
          autoComplete="current-password"
          maxLength={LOGIN_PASSWORD_MAX_LENGTH}
          disabled={disabled}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />
        <button type="submit" disabled={!canSubmit}>
          Войти
        </button>
      </form>
    </section>
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
            <ul className="compact-list">
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

function UserCard({ account }: { account: AccountSession }) {
  return (
    <article className="card">
      <h2>Текущая роль</h2>
      <p className="big">{roleLabels[account.role]}</p>
      <p>{account.displayName}</p>
      <p className="muted mono-wrap">{account.email}</p>
      <p>
        Роль и профиль подтверждены backend через `/api/me`.
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

type StudentSectionId = "learning" | "progress" | "practice";

function StudentDashboard({
  moduleOptions,
  selectedModule,
  lessons,
  selectedLesson,
  moduleResult,
  submissions,
  validationJobs,
  reports,
  reviews,
  assignments,
  isBusy,
  onSelectModule,
  onCreateSubmission,
  onCreateArchiveSubmission,
  onCreateReport
}: {
  moduleOptions: ModuleOption[];
  selectedModule?: ModuleOption;
  lessons: LessonSummary[];
  selectedLesson?: Lesson;
  moduleResult?: ModuleResult;
  submissions: Submission[];
  validationJobs: ValidationJob[];
  reports: Report[];
  reviews: Review[];
  assignments: BlackBoxAssignment[];
  isBusy: boolean;
  onSelectModule: (moduleId: string) => Promise<void>;
  onCreateSubmission: (payload: {
    moduleId: string;
    imageReference: string;
    applicationPort: number;
    healthPath: string;
  }) => Promise<void>;
  onCreateArchiveSubmission: (payload: {
    moduleId: string;
    archive: File;
    applicationPort: number;
    healthPath: string;
    composeService: string;
  }) => Promise<boolean>;
  onCreateReport: (payload: {
    moduleId: string;
    submissionId?: string;
    blackBoxAssignmentId?: string;
    type: ReportType;
    title: string;
    contentMarkdown: string;
    attachment?: File | null;
  }) => Promise<boolean>;
}) {
  const [imageReference, setImageReference] = useState(DEFAULT_IMAGE_REFERENCE);
  const [standArchive, setStandArchive] = useState<File | null>(null);
  const [archiveInputVersion, setArchiveInputVersion] = useState(0);
  const [applicationPort, setApplicationPort] = useState(8080);
  const [healthPath, setHealthPath] = useState("/health");
  const [composeService, setComposeService] = useState("");
  const [reportText, setReportText] = useState(DEFAULT_WHITEBOX_REPORT);
  const [reportAttachment, setReportAttachment] = useState<File | null>(null);
  const [reportAttachmentInputVersion, setReportAttachmentInputVersion] = useState(0);
  const [blackBoxReportTexts, setBlackBoxReportTexts] = useState<Record<string, string>>({});
  const [blackBoxReportAttachments, setBlackBoxReportAttachments] = useState<Record<string, File | null>>({});
  const [blackBoxAttachmentInputVersions, setBlackBoxAttachmentInputVersions] = useState<Record<string, number>>({});
  const latestSubmission = useMemo(() => {
    const moduleSubmissions = submissions.filter((submission) => submission.moduleId === selectedModule?.id);
    if (moduleSubmissions.length === 0) {
      return undefined;
    }
    const submissionsWithCreatedAt = moduleSubmissions.filter((submission) => submission.createdAt);
    if (submissionsWithCreatedAt.length === 0) {
      return moduleSubmissions[moduleSubmissions.length - 1];
    }
    return [...submissionsWithCreatedAt].sort((left, right) => {
      const leftTime = left.createdAt ? Date.parse(left.createdAt) : Number.NaN;
      const rightTime = right.createdAt ? Date.parse(right.createdAt) : Number.NaN;
      if (!Number.isNaN(leftTime) && !Number.isNaN(rightTime)) {
        return rightTime - leftTime;
      }
      if (Number.isNaN(leftTime) && Number.isNaN(rightTime)) {
        return 0;
      }
      return Number.isNaN(leftTime) ? 1 : -1;
    })[0];
  }, [selectedModule?.id, submissions]);
  const reviewsByReportId = useMemo(
    () => new Map(reviews.map((review) => [review.reportId, review])),
    [reviews]
  );
  const reviewedReportsCount = reports.filter((report) => reviewsByReportId.has(report.id)).length;
  const submittedAssignmentsCount = assignments.filter((assignment) => assignment.status === "SUBMITTED").length;
  const [activeSection, setActiveSection] = useState<StudentSectionId>("learning");
  const blackBoxDefaultTemplate = DEFAULT_BLACKBOX_REPORT;
  const normalizedImageReference = imageReference.trim();
  const normalizedWhiteBoxReport = reportText.trim();
  const normalizedHealthPath = healthPath.trim() || "/health";
  const selectedLessonHeadings = useMemo(
    () => (selectedLesson ? extractMarkdownHeadings(selectedLesson.contentMarkdown) : []),
    [selectedLesson]
  );
  const selectedLessonHeadingGroups = useMemo(
    () => groupMarkdownHeadings(selectedLessonHeadings),
    [selectedLessonHeadings]
  );
  const selectedLessonPath = selectedModule ? webSecurityPath(selectedModule) : "/web-security";
  const selectedModuleIndex = selectedModule
    ? moduleOptions.findIndex((module) => module.id === selectedModule.id)
    : -1;
  const previousModule = selectedModuleIndex > 0 ? moduleOptions[selectedModuleIndex - 1] : undefined;
  const nextModule =
    selectedModuleIndex >= 0 && selectedModuleIndex < moduleOptions.length - 1
      ? moduleOptions[selectedModuleIndex + 1]
      : undefined;

  function openLessonAnchor(anchorId: string) {
    window.history.pushState(null, "", `${selectedLessonPath}#${anchorId}`);
    document.getElementById(anchorId)?.scrollIntoView({ block: "start", behavior: "smooth" });
  }

  function blackBoxText(assignmentId: string) {
    return blackBoxReportTexts[assignmentId] ?? blackBoxDefaultTemplate;
  }

  function blackBoxAttachment(assignmentId: string) {
    return blackBoxReportAttachments[assignmentId] ?? null;
  }

  function hasBlackBoxContent(assignmentId: string) {
    return blackBoxText(assignmentId).trim().length > 0;
  }

  function clearBlackBoxDraft(assignmentId: string) {
    setBlackBoxReportTexts((current) => {
      const next = { ...current };
      delete next[assignmentId];
      return next;
    });
    setBlackBoxReportAttachments((current) => {
      const next = { ...current };
      delete next[assignmentId];
      return next;
    });
    setBlackBoxAttachmentInputVersions((current) => ({
      ...current,
      [assignmentId]: (current[assignmentId] ?? 0) + 1
    }));
  }

  useEffect(() => {
    // Module switch should not keep old evidence files/drafts.
    setReportText(DEFAULT_WHITEBOX_REPORT);
    setReportAttachment(null);
    setReportAttachmentInputVersion((current) => current + 1);
    setBlackBoxReportTexts({});
    setBlackBoxReportAttachments({});
    setBlackBoxAttachmentInputVersions({});
  }, [selectedModule?.id]);

  useEffect(() => {
    if (activeSection !== "learning" || !selectedModule || !selectedLesson) {
      return;
    }
    const requestedAnchor = window.location.hash.replace("#", "");
    const initialAnchor = selectedLessonHeadings.some((heading) => heading.id === requestedAnchor)
      ? requestedAnchor
      : selectedLessonHeadings[0]?.id;
    const nextUrl = initialAnchor ? `${selectedLessonPath}#${initialAnchor}` : selectedLessonPath;
    if (`${window.location.pathname}${window.location.hash}` !== nextUrl) {
      window.history.replaceState(null, "", nextUrl);
    }
  }, [activeSection, selectedLesson, selectedLessonHeadings, selectedLessonPath, selectedModule]);

  return (
    <section className="section-panel">
      <nav className="section-nav" aria-label="Разделы студента">
        <button
          type="button"
          disabled={isBusy}
          className={activeSection === "learning" ? "section-nav-button active" : "section-nav-button"}
          onClick={() => setActiveSection("learning")}
        >
          Обучение
          <span className="badge-count">{lessons.length}</span>
        </button>
        <button
          type="button"
          disabled={isBusy}
          className={activeSection === "progress" ? "section-nav-button active" : "section-nav-button"}
          onClick={() => setActiveSection("progress")}
        >
          Прогресс
        </button>
        <button
          type="button"
          disabled={isBusy}
          className={activeSection === "practice" ? "section-nav-button active" : "section-nav-button"}
          onClick={() => setActiveSection("practice")}
        >
          Практика
          <span className="badge-count">{submissions.length}</span>
        </button>
      </nav>

      <div className="grid">
      {activeSection === "learning" && <article className="card wide">
        <h2>Учебные материалы модуля</h2>
        {!selectedModule ? (
          <EmptyState>Нет активного модуля.</EmptyState>
        ) : (
          <div className="lesson-shell">
            <aside className="lesson-sidebar" aria-label="Разделы модуля">
              <p className="lesson-sidebar-title">Разделы модуля</p>
              <div className="lesson-page-toc compact" aria-label="Разделы модуля">
                {selectedLessonHeadings.length === 0 ? (
                  <p className="muted">Разделы появятся после загрузки материала.</p>
                ) : (
                  selectedLessonHeadingGroups.map((group) => (
                    <details key={group.heading.id} className="toc-tree-group" open>
                      <summary
                        onClick={(event) => {
                          if (event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) {
                            return;
                          }
                        }}
                      >
                        <a
                          className="lesson-page-toc-link"
                          href={`${selectedLessonPath}#${group.heading.id}`}
                          onClick={(event) => {
                            event.preventDefault();
                            openLessonAnchor(group.heading.id);
                          }}
                        >
                          {group.heading.title}
                        </a>
                      </summary>
                      {group.children.map((heading) => (
                        <a
                          key={heading.id}
                          className="lesson-page-toc-link nested"
                          href={`${selectedLessonPath}#${heading.id}`}
                          onClick={(event) => {
                            event.preventDefault();
                            openLessonAnchor(heading.id);
                          }}
                        >
                          {heading.title}
                        </a>
                      ))}
                    </details>
                  ))
                )}
              </div>
            </aside>
            <div className="lesson-main">
              {selectedLesson ? (
                <>
                  <p className="lesson-breadcrumbs">
                    Академия веб-безопасности · <span>{selectedModule.courseTitle}</span> · <span>{selectedModule.title}</span>
                  </p>
                  <h1 className="lesson-title">{selectedLesson.title}</h1>
                  <div className="lesson-meta">
                    <span>
                      Тема: <strong>{selectedModule.vulnerabilityTopic}</strong>
                    </span>
                    <span>
                      Урок <strong>{selectedLesson.position}</strong> из <strong>{lessons.length}</strong>
                    </span>
                  </div>
                  <article className="lesson-doc">
                    <MarkdownPreview source={selectedLesson.contentMarkdown} withHeadingIds />
                  </article>
                  <div className="module-step-actions">
                    <button
                      type="button"
                      className="secondary"
                      disabled={isBusy || !previousModule}
                      onClick={() => previousModule && void onSelectModule(previousModule.id)}
                    >
                      Предыдущий модуль
                    </button>
                    <button
                      type="button"
                      disabled={isBusy || !nextModule}
                      onClick={() => nextModule && void onSelectModule(nextModule.id)}
                    >
                      Следующий модуль
                    </button>
                  </div>
                </>
              ) : (
                <div className="lesson-empty">
                  <p className="lesson-breadcrumbs">
                    Академия веб-безопасности · <span>{selectedModule.courseTitle}</span>
                  </p>
                  <h1 className="lesson-title">Материалы пока не опубликованы</h1>
                </div>
              )}
            </div>
          </div>
        )}
      </article>}

      {activeSection === "progress" && <article className="card wide">
        <h2>Progress dashboard</h2>
        <div className="chart-grid">
          <ProgressBar label="Отчеты с feedback" value={reviewedReportsCount} total={reports.length} />
          <ProgressBar label="Black box цели закрыты" value={submittedAssignmentsCount} total={assignments.length} />
          <StatusDistribution title="Submissions по статусам" counts={countByStatus(submissions)} />
          <StatusDistribution title="Reports по статусам" counts={countByStatus(reports)} />
        </div>
      </article>}

      {activeSection === "progress" && <article className="card wide">
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
      </article>}

      {activeSection === "practice" && <article className="card">
        <h2>Student: загрузка стенда</h2>
        {!selectedModule ? (
          <EmptyState>Нет активного модуля.</EmptyState>
        ) : (
          <div className="stack">
            <form
              className="form compact-form"
              onSubmit={async (event) => {
                event.preventDefault();
                if (isBusy || !standArchive) {
                  return;
                }
                const success = await onCreateArchiveSubmission({
                  moduleId: selectedModule.id,
                  archive: standArchive,
                  applicationPort,
                  healthPath: normalizedHealthPath,
                  composeService
                });
                if (success) {
                  setStandArchive(null);
                  setArchiveInputVersion((current) => current + 1);
                }
              }}
            >
              <label htmlFor="standArchive">Архив проекта (.zip, .tar, .tar.gz)</label>
              <input
                key={archiveInputVersion}
                id="standArchive"
                type="file"
                accept=".zip,.tar,.gz,.tgz,.tar.gz"
                disabled={isBusy}
                onChange={(event) => setStandArchive(event.target.files?.[0] ?? null)}
              />
              {standArchive && (
                <p className="muted">
                  Выбрано: {standArchive.name} ({formatBytes(standArchive.size)})
                </p>
              )}
              <label htmlFor="applicationPort">Порт web-сервиса</label>
              <input
                id="applicationPort"
                type="number"
                min={1}
                max={65535}
                disabled={isBusy}
                value={applicationPort}
                onChange={(event) => setApplicationPort(Math.min(65535, Math.max(1, Number(event.target.value) || 8080)))}
              />
              <label htmlFor="healthPath">Health path</label>
              <input
                id="healthPath"
                disabled={isBusy}
                value={healthPath}
                onChange={(event) => setHealthPath(event.target.value)}
              />
              <label htmlFor="composeService">Compose service (если в архиве docker-compose.yml)</label>
              <input
                id="composeService"
                disabled={isBusy}
                placeholder="web"
                value={composeService}
                onChange={(event) => setComposeService(event.target.value)}
              />
              <button type="submit" disabled={isBusy || !standArchive}>
                Загрузить архив и поставить на technical validation
              </button>
            </form>
            <details className="advanced-panel">
              <summary>Advanced: отправить готовый Docker image reference</summary>
              <form
                className="form compact-form"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (isBusy || !normalizedImageReference) {
                    return;
                  }
                  void onCreateSubmission({
                    moduleId: selectedModule.id,
                    imageReference: normalizedImageReference,
                    applicationPort,
                    healthPath: normalizedHealthPath
                  });
                }}
              >
                <label htmlFor="imageReference">Docker image reference</label>
                <input
                  id="imageReference"
                  disabled={isBusy}
                  value={imageReference}
                  onChange={(event) => setImageReference(event.target.value)}
                />
                <button type="submit" disabled={isBusy || !normalizedImageReference}>
                  Отправить image на technical validation
                </button>
              </form>
            </details>
          </div>
        )}
        <EntityList
          title="Мои submissions"
          items={submissions}
          render={(submission) => (
            <>
              <strong>{submission.sourceType === "ARCHIVE" ? submission.archiveFilename : submission.imageReference}</strong>
              <StatusBadge value={submission.status} />
              <p className="muted">Источник: {submission.sourceType === "ARCHIVE" ? "архив проекта" : "Docker image"}</p>
              {submission.runtimeImageReference && <p className="muted">Runtime image: {submission.runtimeImageReference}</p>}
              {submission.publicUrl && (
                <p>
                  <a href={submission.publicUrl} target="_blank" rel="noopener noreferrer">
                    {submission.publicUrl}
                  </a>
                </p>
              )}
              {submission.localHostUrl && <p className="muted">Local domain: {submission.localHostUrl}</p>}
              <p className="muted">Port: {submission.applicationPort}, health: {submission.healthPath}</p>
            </>
          )}
        />
      </article>}

      {activeSection === "practice" && <article className="card">
        <h2>Student: white box отчет</h2>
        {!selectedModule || !latestSubmission ? (
          <EmptyState>Сначала отправьте Docker image.</EmptyState>
        ) : (
          <form
            className="form compact-form"
            onSubmit={async (event) => {
              event.preventDefault();
              if (isBusy) {
                return;
              }
              if (!normalizedWhiteBoxReport) {
                return;
              }
              const success = await onCreateReport({
                moduleId: selectedModule.id,
                submissionId: latestSubmission.id,
                type: "WHITE_BOX",
                title: "White box отчет по SQL Injection",
                contentMarkdown: normalizedWhiteBoxReport,
                attachment: reportAttachment
              });
              if (success) {
                setReportAttachment(null);
                setReportAttachmentInputVersion((current) => current + 1);
              }
            }}
          >
            <label htmlFor="reportText">Evidence и payload</label>
            <textarea
              id="reportText"
              rows={4}
              disabled={isBusy}
              maxLength={REPORT_MARKDOWN_MAX_LENGTH}
              value={reportText}
              onChange={(event) => setReportText(event.target.value)}
            />
            <p className="muted field-hint">
              {reportText.length}/{REPORT_MARKDOWN_MAX_LENGTH}
            </p>
            <div className="preview-panel">
              <strong>Preview отчета</strong>
              <MarkdownPreview source={reportText} />
            </div>
            <label htmlFor="reportAttachment">Вложение с evidence (опционально)</label>
            <input
              key={reportAttachmentInputVersion}
              id="reportAttachment"
              type="file"
              disabled={isBusy}
              onChange={(event) => setReportAttachment(event.target.files?.[0] ?? null)}
            />
            {reportAttachment && (
              <p className="muted">
                Выбрано: {reportAttachment.name} ({formatBytes(reportAttachment.size)})
              </p>
            )}
            <button type="submit" disabled={isBusy || !normalizedWhiteBoxReport}>
              Отправить отчет куратору
            </button>
          </form>
        )}
        <EntityList
          title="Validation jobs"
          items={validationJobs}
          maxHeight={320}
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
          maxHeight={360}
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
          maxHeight={420}
          render={(assignment) => (
            <>
              <strong>{assignment.targetUrl}</strong>
              <StatusBadge value={assignment.status} />
              <p className="muted">Image target: {assignment.targetImageReference}</p>
              {assignment.status !== "SUBMITTED" && (
                <form
                  className="form nested-form compact-form"
                  onSubmit={async (event) => {
                    event.preventDefault();
                    if (isBusy) {
                      return;
                    }
                    const assignmentReportText = blackBoxText(assignment.id);
                    const normalizedAssignmentReportText = assignmentReportText.trim();
                    if (!normalizedAssignmentReportText) {
                      return;
                    }
                    const assignmentAttachment = blackBoxAttachment(assignment.id);
                    const success = await onCreateReport({
                      moduleId: assignment.moduleId,
                      blackBoxAssignmentId: assignment.id,
                      type: "BLACK_BOX",
                      title: "Black box отчет по SQL Injection",
                      contentMarkdown: normalizedAssignmentReportText,
                      attachment: assignmentAttachment
                    });
                    if (success) {
                      clearBlackBoxDraft(assignment.id);
                    }
                  }}
                >
                  <label htmlFor={`blackBoxReport-${assignment.id}`}>Black box evidence</label>
                  <textarea
                    id={`blackBoxReport-${assignment.id}`}
                    rows={4}
                    disabled={isBusy}
                    maxLength={REPORT_MARKDOWN_MAX_LENGTH}
                    value={blackBoxText(assignment.id)}
                    onChange={(event) =>
                      setBlackBoxReportTexts((current) => ({
                        ...current,
                        [assignment.id]: event.target.value
                      }))
                    }
                  />
                  <p className="muted field-hint">
                    {blackBoxText(assignment.id).length}/{REPORT_MARKDOWN_MAX_LENGTH}
                  </p>
                  <div className="preview-panel">
                    <strong>Preview black box отчета</strong>
                    <MarkdownPreview source={blackBoxText(assignment.id)} />
                  </div>
                  <label htmlFor={`blackBoxAttachment-${assignment.id}`}>Вложение с evidence (опционально)</label>
                  <input
                    key={blackBoxAttachmentInputVersions[assignment.id] ?? 0}
                    id={`blackBoxAttachment-${assignment.id}`}
                    type="file"
                    disabled={isBusy}
                    onChange={(event) =>
                      setBlackBoxReportAttachments((current) => ({
                        ...current,
                        [assignment.id]: event.target.files?.[0] ?? null
                      }))
                    }
                  />
                  {blackBoxAttachment(assignment.id) && (
                    <p className="muted">
                      Выбрано: {blackBoxAttachment(assignment.id)!.name} ({formatBytes(blackBoxAttachment(assignment.id)!.size)})
                    </p>
                  )}
                  <button type="submit" disabled={isBusy || !hasBlackBoxContent(assignment.id)}>
                    Отправить black box отчет
                  </button>
                </form>
              )}
            </>
          )}
        />
      </article>}
      </div>
    </section>
  );
}

type CuratorSectionId = "validation" | "reports";

function CuratorDashboard({
  firstModule,
  submissions,
  validationJobs,
  reports,
  isBusy,
  onExportGrades,
  onCompleteValidation,
  onCreateReview
}: {
  firstModule?: LearningModule;
  submissions: Submission[];
  validationJobs: ValidationJob[];
  reports: Report[];
  isBusy: boolean;
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
  const [activeSection, setActiveSection] = useState<CuratorSectionId>("validation");
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
    <section className="section-panel">
      <nav className="section-nav" aria-label="Разделы куратора">
        <button
          type="button"
          disabled={isBusy}
          className={activeSection === "validation" ? "section-nav-button active" : "section-nav-button"}
          onClick={() => setActiveSection("validation")}
        >
          Technical validation
          <span className="badge-count">{validationJobs.length}</span>
        </button>
        <button
          type="button"
          disabled={isBusy}
          className={activeSection === "reports" ? "section-nav-button active" : "section-nav-button"}
          onClick={() => setActiveSection("reports")}
        >
          Очередь отчетов
          <span className="badge-count">{filteredReports.length}</span>
        </button>
      </nav>
      <div className="grid">
      {activeSection === "validation" && <article className="card">
        <h2>Curator: technical validation</h2>
        <EntityList
          title="Validation jobs"
          items={validationJobs}
          maxHeight={420}
          render={(job) => (
            <>
              <strong>{job.imageReference}</strong>
              <StatusBadge value={job.status} />
              <ImageScanSummary job={job} />
              <DependencyScanSummary job={job} />
              <div className="actions">
                <button type="button" disabled={isBusy} onClick={() => void onCompleteValidation(job.id, true)}>
                  Отметить как пройдено
                </button>
                <button type="button" className="secondary" disabled={isBusy} onClick={() => void onCompleteValidation(job.id, false)}>
                  Отметить ошибку
                </button>
              </div>
            </>
          )}
        />
      </article>}

      {activeSection === "reports" && <article className="card">
        <h2>Curator: проверка отчетов</h2>
        {firstModule && (
          <div className="actions">
            <button type="button" className="secondary" disabled={isBusy} onClick={() => void onExportGrades(firstModule.id)}>
              Экспорт оценок CSV
            </button>
          </div>
        )}
        <div className="filter-panel">
          <div>
            <label htmlFor="reportStatusFilter">Статус</label>
            <select id="reportStatusFilter" disabled={isBusy} value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
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
              disabled={isBusy}
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
              disabled={isBusy}
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
          maxHeight={560}
          render={(report) => (
            <ReviewForm
              report={report}
              submissions={submissions}
              validationJobs={validationJobs}
              isBusy={isBusy}
              onCreateReview={onCreateReview}
            />
          )}
        />
      </article>}
      </div>
    </section>
  );
}

function ReviewForm({
  report,
  submissions,
  validationJobs,
  isBusy,
  onCreateReview
}: {
  report: Report;
  submissions: Submission[];
  validationJobs: ValidationJob[];
  isBusy: boolean;
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
  const normalizedComment = comment.trim();

  function clampScore(nextScore: number) {
    if (Number.isNaN(nextScore)) {
      return 0;
    }
    return Math.min(100, Math.max(0, nextScore));
  }

  return (
    <form
      className="review-box compact-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (isBusy) {
          return;
        }
        if (!normalizedComment) {
          return;
        }
        void onCreateReview({ reportId: report.id, decision, score: clampScore(score), commentMarkdown: normalizedComment });
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
      <select
        id={`decision-${report.id}`}
        disabled={isBusy}
        value={decision}
        onChange={(event) => setDecision(event.target.value as ReviewDecision)}
      >
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
        disabled={isBusy}
        value={score}
        onChange={(event) => setScore(clampScore(Number(event.target.value)))}
      />
      <label htmlFor={`comment-${report.id}`}>Комментарий</label>
      <textarea
        id={`comment-${report.id}`}
        rows={4}
        disabled={isBusy}
        maxLength={REVIEW_COMMENT_MAX_LENGTH}
        value={comment}
        onChange={(event) => setComment(event.target.value)}
      />
      <p className="muted field-hint">
        {comment.length}/{REVIEW_COMMENT_MAX_LENGTH}
      </p>
      <div className="preview-panel">
        <strong>Preview комментария</strong>
        <MarkdownPreview source={comment} />
      </div>
      <button type="submit" disabled={isBusy || !normalizedComment}>
        Сохранить решение
      </button>
    </form>
  );
}

type AdminSectionId = "overview" | "content" | "users" | "labs" | "analytics" | "audit";

function AdminDashboard({
  submissions,
  validationJobs,
  reports,
  labs,
  auditEvents,
  courses,
  users,
  onlineUsers,
  firstModule,
  isBusy,
  onRefresh,
  onCreateCourse,
  onCreateModule,
  onUpsertLesson,
  onCreateUser,
  onDisableUser,
  onExportGrades,
  onCreateLab,
  onDistribute
}: {
  submissions: Submission[];
  validationJobs: ValidationJob[];
  reports: Report[];
  labs: Lab[];
  auditEvents: AuditEvent[];
  courses: Course[];
  users: AdminUser[];
  onlineUsers?: OnlineUsers;
  firstModule?: LearningModule;
  isBusy: boolean;
  onRefresh: () => void;
  onCreateCourse: (payload: { title: string; description: string }) => Promise<boolean>;
  onCreateModule: (payload: { courseId: string; title: string; vulnerabilityTopic: string }) => Promise<boolean>;
  onUpsertLesson: (payload: { moduleId: string; title: string; contentMarkdown: string; position: number }) => Promise<boolean>;
  onCreateUser: (payload: { email: string; password: string; displayName: string; role: Role }) => Promise<boolean>;
  onDisableUser: (userId: string) => Promise<boolean>;
  onExportGrades: (moduleId: string) => Promise<void>;
  onCreateLab: (submissionId: string) => Promise<void>;
  onDistribute: (moduleId: string) => Promise<void>;
}) {
  const [activeSection, setActiveSection] = useState<AdminSectionId>("overview");
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

  const sections: Array<{ id: AdminSectionId; label: string; count?: number }> = [
    { id: "overview", label: "Обзор" },
    { id: "content", label: "Курсы", count: courses.reduce((sum, course) => sum + course.modules.length, 0) },
    { id: "users", label: "Пользователи", count: users.length },
    { id: "labs", label: "Lab runtime", count: labs.length },
    { id: "analytics", label: "Аналитика" },
    { id: "audit", label: "Аудит", count: auditEvents.length }
  ];

  return (
    <section className="section-panel">
      <nav className="section-nav" aria-label="Разделы администратора">
        {sections.map((section) => (
          <button
            key={section.id}
            type="button"
            disabled={isBusy}
            className={section.id === activeSection ? "section-nav-button active" : "section-nav-button"}
            onClick={() => setActiveSection(section.id)}
          >
            {section.label}
            {typeof section.count === "number" && <span className="badge-count">{section.count}</span>}
          </button>
        ))}
      </nav>

      {activeSection === "overview" && (
        <div className="section-panel">
          <header className="section-panel-header">
            <div>
              <h2>Состояние платформы</h2>
            </div>
          </header>
          <article className="card">
            <h2>Ключевые метрики</h2>
            <dl className="metrics">
              <div>
                <dt>Online users</dt>
                <dd>{onlineUsers?.activeUsers ?? 0}</dd>
              </div>
              <div>
                <dt>Active sessions</dt>
                <dd>{onlineUsers?.activeSessions ?? 0}</dd>
              </div>
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
            <div className="actions">
              <button type="button" className="secondary" disabled={isBusy} onClick={onRefresh}>
                Обновить статистику
              </button>
            </div>
          </article>
          <article className="card">
            <h2>Готовность lab runtime</h2>
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
        </div>
      )}

      {activeSection === "content" && (
        <AdminContentManager
          courses={courses}
          isBusy={isBusy}
          onCreateCourse={onCreateCourse}
          onCreateModule={onCreateModule}
          onUpsertLesson={onUpsertLesson}
        />
      )}

      {activeSection === "users" && (
        <AdminUsersManager
          users={users}
          isBusy={isBusy}
          onCreateUser={onCreateUser}
          onDisableUser={onDisableUser}
        />
      )}

      {activeSection === "labs" && (
        <div className="section-panel">
          <header className="section-panel-header">
            <div>
              <h2>Lab runtime</h2>
            </div>
            {firstModule && (
              <div className="actions">
                <button type="button" disabled={isBusy} onClick={() => void onDistribute(firstModule.id)}>
                  Распределить black box цели
                </button>
                <button type="button" className="secondary" disabled={isBusy} onClick={() => void onExportGrades(firstModule.id)}>
                  Экспорт оценок CSV
                </button>
              </div>
            )}
          </header>
          <article className="card">
            <h2>Принятые submissions</h2>
            <EntityList
              title="Готовы к созданию lab"
              items={approvedSubmissions}
              maxHeight={360}
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
                    <button type="button" disabled={isBusy} onClick={() => void onCreateLab(submission.id)}>
                      Создать lab instance
                    </button>
                  )}
                </>
              )}
            />
          </article>
          <article className="card">
            <h2>Lab instances</h2>
            <EntityList
              title="Запущенные окружения"
              items={labs}
              maxHeight={520}
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
          </article>
        </div>
      )}

      {activeSection === "analytics" && (
        <div className="section-panel">
          <header className="section-panel-header">
            <div>
              <h2>Аналитика прогресса</h2>
            </div>
          </header>
          <article className="card">
            <h2>Сводные графики</h2>
            <div className="chart-grid">
              <ProgressBar label="Technical validation passed" value={passedValidationJobs} total={validationJobs.length} />
              <ProgressBar label="Approved reports" value={approvedReports} total={reports.length} />
              <ProgressBar label="Labs created" value={labs.length} total={approvedSubmissions.length} />
              <StatusDistribution title="Validation jobs по статусам" counts={countByStatus(validationJobs)} />
              <StatusDistribution title="Labs по статусам" counts={labStatusCounts} />
            </div>
          </article>
        </div>
      )}

      {activeSection === "audit" && (
        <div className="section-panel">
          <header className="section-panel-header">
            <div>
              <h2>Audit trail</h2>
            </div>
          </header>
          <article className="card">
            <h2>События</h2>
            <EntityList
              title="Последние события"
              items={auditEvents}
              maxHeight={480}
              render={(event) => (
                <div className="audit-row">
                  <time>{new Date(event.createdAt).toLocaleString("ru-RU")}</time>
                  <strong>{event.action}</strong>
                  <span>{event.actorEmail ?? "system"} - {event.targetType}</span>
                  <code>{event.metadataJson}</code>
                </div>
              )}
            />
          </article>
        </div>
      )}
    </section>
  );
}

function AdminContentManager({
  courses,
  isBusy,
  onCreateCourse,
  onCreateModule,
  onUpsertLesson
}: {
  courses: Course[];
  isBusy: boolean;
  onCreateCourse: (payload: { title: string; description: string }) => Promise<boolean>;
  onCreateModule: (payload: { courseId: string; title: string; vulnerabilityTopic: string }) => Promise<boolean>;
  onUpsertLesson: (payload: { moduleId: string; title: string; contentMarkdown: string; position: number }) => Promise<boolean>;
}) {
  const firstCourseId = courses[0]?.id ?? "";
  const firstModuleId = courses.flatMap((course) => course.modules)[0]?.id ?? "";
  const [courseTitle, setCourseTitle] = useState("");
  const [courseDescription, setCourseDescription] = useState("");
  const [moduleCourseId, setModuleCourseId] = useState(firstCourseId);
  const [moduleTitle, setModuleTitle] = useState("");
  const [moduleTopic, setModuleTopic] = useState("");
  const [lessonModuleId, setLessonModuleId] = useState(firstModuleId);
  const [lessonTitle, setLessonTitle] = useState("");
  const [lessonPosition, setLessonPosition] = useState(1);
  const [lessonMarkdown, setLessonMarkdown] = useState("## Новый раздел\n\nТекст материала в формате Markdown.");

  useEffect(() => {
    if (!moduleCourseId && firstCourseId) {
      setModuleCourseId(firstCourseId);
    }
    if (!lessonModuleId && firstModuleId) {
      setLessonModuleId(firstModuleId);
    }
  }, [firstCourseId, firstModuleId, lessonModuleId, moduleCourseId]);

  return (
    <div className="grid">
      <article className="card">
        <h2>Создать курс</h2>
        <form
          className="form compact-form"
          onSubmit={async (event) => {
            event.preventDefault();
            const success = await onCreateCourse({ title: courseTitle.trim(), description: courseDescription.trim() });
            if (success) {
              setCourseTitle("");
              setCourseDescription("");
            }
          }}
        >
          <label htmlFor="adminCourseTitle">Название курса</label>
          <input id="adminCourseTitle" disabled={isBusy} value={courseTitle} onChange={(event) => setCourseTitle(event.target.value)} />
          <label htmlFor="adminCourseDescription">Описание курса</label>
          <MarkdownAdminTextarea
            id="adminCourseDescription"
            rows={5}
            disabled={isBusy}
            value={courseDescription}
            onChange={setCourseDescription}
          />
          <button type="submit" disabled={isBusy || !courseTitle.trim() || !courseDescription.trim()}>
            Сохранить курс
          </button>
        </form>
      </article>

      <article className="card">
        <h2>Создать модуль</h2>
        <form
          className="form compact-form"
          onSubmit={async (event) => {
            event.preventDefault();
            const success = await onCreateModule({
              courseId: moduleCourseId,
              title: moduleTitle.trim(),
              vulnerabilityTopic: moduleTopic.trim()
            });
            if (success) {
              setModuleTitle("");
              setModuleTopic("");
            }
          }}
        >
          <label htmlFor="adminModuleCourse">Курс</label>
          <select id="adminModuleCourse" disabled={isBusy} value={moduleCourseId} onChange={(event) => setModuleCourseId(event.target.value)}>
            {courses.map((course) => (
              <option key={course.id} value={course.id}>
                {course.title}
              </option>
            ))}
          </select>
          <label htmlFor="adminModuleTitle">Название модуля</label>
          <input id="adminModuleTitle" disabled={isBusy} value={moduleTitle} onChange={(event) => setModuleTitle(event.target.value)} />
          <label htmlFor="adminModuleTopic">Тема</label>
          <input id="adminModuleTopic" disabled={isBusy} value={moduleTopic} onChange={(event) => setModuleTopic(event.target.value)} />
          <button type="submit" disabled={isBusy || !moduleCourseId || !moduleTitle.trim() || !moduleTopic.trim()}>
            Сохранить модуль
          </button>
        </form>
      </article>

      <article className="card wide">
        <h2>Редактор страницы модуля</h2>
        <form
          className="form compact-form"
          onSubmit={async (event) => {
            event.preventDefault();
            await onUpsertLesson({
              moduleId: lessonModuleId,
              title: lessonTitle.trim(),
              contentMarkdown: lessonMarkdown,
              position: lessonPosition
            });
          }}
        >
          <label htmlFor="adminLessonModule">Модуль</label>
          <select id="adminLessonModule" disabled={isBusy} value={lessonModuleId} onChange={(event) => setLessonModuleId(event.target.value)}>
            {courses.flatMap((course) =>
              course.modules.map((module) => (
                <option key={module.id} value={module.id}>
                  {course.title}: {module.title}
                </option>
              ))
            )}
          </select>
          <label htmlFor="adminLessonTitle">Заголовок страницы</label>
          <input id="adminLessonTitle" disabled={isBusy} value={lessonTitle} onChange={(event) => setLessonTitle(event.target.value)} />
          <label htmlFor="adminLessonPosition">Позиция</label>
          <input
            id="adminLessonPosition"
            type="number"
            min={1}
            disabled={isBusy}
            value={lessonPosition}
            onChange={(event) => setLessonPosition(Math.max(1, Number(event.target.value) || 1))}
          />
          <label htmlFor="adminLessonMarkdown">Markdown-контент</label>
          <MarkdownAdminTextarea
            id="adminLessonMarkdown"
            rows={14}
            disabled={isBusy}
            value={lessonMarkdown}
            onChange={setLessonMarkdown}
          />
          <div className="preview-panel">
            <strong>Предпросмотр</strong>
            <MarkdownPreview source={lessonMarkdown} />
          </div>
          <button type="submit" disabled={isBusy || !lessonModuleId || !lessonTitle.trim() || !lessonMarkdown.trim()}>
            Сохранить страницу
          </button>
        </form>
      </article>
    </div>
  );
}

function MarkdownAdminTextarea({
  id,
  rows,
  disabled,
  value,
  onChange
}: {
  id: string;
  rows: number;
  disabled: boolean;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <textarea
      id={id}
      rows={rows}
      disabled={disabled}
      value={value}
      onChange={(event) => onChange(event.target.value)}
      onPaste={(event) => {
        const imageItem = Array.from(event.clipboardData.items).find((item) => item.type.startsWith("image/"));
        const file = imageItem?.getAsFile();
        if (!file) {
          return;
        }
        event.preventDefault();
        const start = event.currentTarget.selectionStart;
        const end = event.currentTarget.selectionEnd;
        const reader = new FileReader();
        reader.onload = () => {
          const text = String(reader.result ?? "");
          onChange(`${value.slice(0, start)}${text}${value.slice(end)}`);
        };
        reader.readAsDataURL(file);
      }}
    />
  );
}

function AdminUsersManager({
  users,
  isBusy,
  onCreateUser,
  onDisableUser
}: {
  users: AdminUser[];
  isBusy: boolean;
  onCreateUser: (payload: { email: string; password: string; displayName: string; role: Role }) => Promise<boolean>;
  onDisableUser: (userId: string) => Promise<boolean>;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [role, setRole] = useState<Role>("STUDENT");

  return (
    <div className="grid">
      <article className="card">
        <h2>Создать пользователя</h2>
        <form
          className="form compact-form"
          onSubmit={async (event) => {
            event.preventDefault();
            const success = await onCreateUser({
              email: email.trim(),
              password,
              displayName: displayName.trim(),
              role
            });
            if (success) {
              setEmail("");
              setPassword("");
              setDisplayName("");
              setRole("STUDENT");
            }
          }}
        >
          <label htmlFor="adminUserEmail">Email</label>
          <input id="adminUserEmail" type="email" disabled={isBusy} value={email} onChange={(event) => setEmail(event.target.value)} />
          <label htmlFor="adminUserName">Имя</label>
          <input id="adminUserName" disabled={isBusy} value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
          <label htmlFor="adminUserPassword">Пароль</label>
          <input id="adminUserPassword" type="password" disabled={isBusy} value={password} onChange={(event) => setPassword(event.target.value)} />
          <label htmlFor="adminUserRole">Роль</label>
          <select id="adminUserRole" disabled={isBusy} value={role} onChange={(event) => setRole(event.target.value as Role)}>
            <option value="STUDENT">Студент</option>
            <option value="CURATOR">Куратор</option>
            <option value="ADMIN">Администратор</option>
          </select>
          <button type="submit" disabled={isBusy || !email.trim() || !password || !displayName.trim()}>
            Создать пользователя
          </button>
        </form>
      </article>

      <article className="card">
        <h2>Все пользователи</h2>
        <EntityList
          title="Пользователи"
          items={users}
          maxHeight={520}
          render={(user) => (
            <>
              <strong>{user.displayName}</strong>
              <p className="muted mono-wrap">{user.email}</p>
              <p>
                <StatusBadge value={roleLabels[user.role]} /> <StatusBadge value={user.status} />
              </p>
              <button type="button" className="secondary" disabled={isBusy || user.status !== "ACTIVE"} onClick={() => void onDisableUser(user.id)}>
                Отключить
              </button>
            </>
          )}
        />
      </article>
    </div>
  );
}

function EntityList<T>({
  title,
  items,
  render,
  getKey,
  maxHeight
}: {
  title: string;
  items: T[];
  render: (item: T) => React.ReactNode;
  getKey?: (item: T, index: number) => string;
  maxHeight?: number;
}) {
  function resolveItemKey(item: T, index: number) {
    if (getKey) {
      return getKey(item, index);
    }
    if (typeof item === "object" && item !== null && "id" in item) {
      const itemId = (item as { id?: unknown }).id;
      if (typeof itemId === "string" || typeof itemId === "number") {
        return String(itemId);
      }
    }
    return String(index);
  }

  return (
    <div className="list-block">
      <h3>{title}</h3>
      {items.length === 0 ? (
        <EmptyState>Нет данных для отображения.</EmptyState>
      ) : (
        <ul className={maxHeight ? "entity-list scrollable" : "entity-list"} style={maxHeight ? { maxHeight } : undefined}>
          {items.map((item, index) => (
            <li key={resolveItemKey(item, index)}>{render(item)}</li>
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
