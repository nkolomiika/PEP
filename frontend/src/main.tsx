import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import ReactDOM from "react-dom/client";
import "./styles.css";

type Role = "STUDENT" | "CURATOR" | "ADMIN";
type StreamEnrollRoleFilter = "all" | Extract<Role, "STUDENT" | "CURATOR">;
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
  avatarUrl?: string | null;
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
  courseId: string;
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

type PentestTask = {
  id: string;
  title: string;
  slug: string;
  category: string;
  difficulty: string;
  durationMinutes: number;
  entrypointPort: number | null;
  healthPath?: string;
  composeService?: string;
  descriptionMarkdown: string;
  repositoryUrl?: string | null;
  branchName?: string | null;
  commitSha?: string | null;
  status: string;
  buildStatus: string;
  runtimeImageReference?: string | null;
  placementAfterHeading?: string | null;
  lessonId?: string | null;
  moduleId?: string | null;
  sourceKind?: "ARCHIVE" | "PROMOTED_FROM_STAND" | "SYSTEM_ARCHIVE" | string | null;
  archiveStatus?: "UPLOADED" | "BUILDING" | "VALIDATING" | "READY" | "FAILED" | null;
  archiveFailedStage?: "UPLOAD" | "BUILD" | "VALIDATE" | null;
  archiveFilename?: string | null;
  archiveSizeBytes?: number | null;
  archiveBuildLog?: string | null;
  createdByUserId?: string | null;
};

type PentestTaskInstance = {
  id: string;
  taskId: string;
  taskTitle: string;
  category: string;
  namespace: string;
  runtimeImageReference: string;
  publicUrl: string;
  localHostUrl?: string;
  deployCommand?: string;
  status: string;
  flagAttempts: number;
  flagSolved: boolean;
  flagSolvedAt?: string;
  expiresAt: string;
};

type PentestTaskFlagSubmitResponse = {
  instanceId: string;
  accepted: boolean;
  solved: boolean;
  attempts: number;
  status: string;
  solvedAt?: string;
  message: string;
};

type UserPentestStandStatus =
  | "UPLOADED"
  | "BUILDING"
  | "VALIDATING"
  | "READY"
  | "FAILED"
  | "ARCHIVED";

type UserPentestStandInstanceStatus = "RUNNING" | "STOPPED" | "EXPIRED";

type UserPentestStandFailedStage = "UPLOAD" | "BUILD" | "VALIDATE";

type UserPentestStandReviewStatus =
  | "DRAFT"
  | "PENDING_REVIEW"
  | "APPROVED"
  | "REJECTED";

type UserPentestStand = {
  id: string;
  moduleId: string;
  moduleTitle?: string | null;
  ownerId?: string | null;
  ownerEmail?: string | null;
  ownerDisplayName?: string | null;
  displayName: string;
  description?: string | null;
  originalFilename: string;
  archiveSizeBytes: number;
  applicationPort?: number | null;
  composeService?: string | null;
  composeServices?: string | null;
  authorSolved: boolean;
  authorSolvedAt?: string | null;
  imageSizeBytes?: number | null;
  status: UserPentestStandStatus;
  failedStage?: UserPentestStandFailedStage | null;
  reviewStatus: UserPentestStandReviewStatus;
  reviewComment?: string | null;
  reviewedAt?: string | null;
  reviewedByEmail?: string | null;
  submittedForReviewAt?: string | null;
  runtimeImageReference?: string | null;
  s3Bucket?: string | null;
  s3Key?: string | null;
  lastError?: string | null;
  buildLog?: string | null;
  createdAt: string;
  updatedAt: string;
  builtAt?: string | null;
};

type UserPentestStandInstance = {
  id: string;
  standId: string;
  standDisplayName: string;
  namespace: string;
  runtimeImageReference: string;
  publicUrl: string;
  deployCommand?: string | null;
  status: UserPentestStandInstanceStatus;
  flagSolved: boolean;
  flagSolvedAt?: string | null;
  flagAttempts?: number | null;
  expiresAt: string;
  stoppedAt?: string | null;
  createdAt: string;
};

type StandLaunchToast = {
  variant: "launched" | "blocked";
  standName: string;
  publicUrl?: string | null;
};

type PeerStandAssignment = {
  assignmentId: string;
  standId: string;
  standDisplayName: string;
  standDescription?: string | null;
  moduleId: string;
  authorDisplayName?: string | null;
  solved: boolean;
  solvedAt?: string | null;
  assignedAt: string;
};

type CurrentUser = {
  email: string;
  displayName: string;
  role: Role;
  avatarUrl?: string | null;
};

type AdminUser = {
  id: string;
  email: string;
  displayName: string;
  role: Role;
  status: string;
};

type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

type StudentStreamSummary = {
  id: string;
  name: string;
  description: string | null;
  status: "DRAFT" | "ACTIVE" | "ARCHIVED";
  courseCount: number;
  memberCount: number;
  createdAt: string;
  updatedAt: string;
};

type StudentStreamCourse = {
  courseId: string;
  title: string;
  status: string;
  position: number | null;
};

type StudentStreamMember = {
  userId: string;
  email: string;
  displayName: string;
  role: Role;
  status: "ACTIVE" | "REMOVED";
  enrolledAt: string;
};

type StudentStreamModuleSchedule = {
  moduleId: string;
  moduleTitle: string;
  startsAt: string | null;
  submissionDeadline: string | null;
  blackBoxStartsAt: string | null;
  blackBoxDeadline: string | null;
};

type StudentStreamDetail = {
  id: string;
  name: string;
  description: string | null;
  status: "DRAFT" | "ACTIVE" | "ARCHIVED";
  createdAt: string;
  updatedAt: string;
  courses: StudentStreamCourse[];
  members: StudentStreamMember[];
  moduleSchedules: StudentStreamModuleSchedule[];
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
  selectedModuleId?: string;
  labs: Lab[];
  assignments: BlackBoxAssignment[];
  auditEvents: AuditEvent[];
  users: AdminUser[];
  onlineUsers?: OnlineUsers;
  pentestTasks: PentestTask[];
  pentestTaskInstances: PentestTaskInstance[];
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
  STOPPED: "Остановлен",
  EXPIRED: "Истёк",
  ASSIGNED: "Назначено",
  IN_PROGRESS: "В работе",
  COMPLETED: "Изучено",
  DOCKER_REQUIRED: "Нужен Docker-допуск",
  SCORED: "Оценено",
  CONNECTED: "Подключено",
  DISCONNECTED: "Нет соединения",
  UPLOADED: "Загружен",
  BUILDING: "Сборка",
  VALIDATING: "Проверка",
  READY: "Готов к запуску",
  ARCHIVED: "В архиве",
  DRAFT: "Черновик",
  REMOVED: "Отчислен"
};

const roleLabels: Record<Role, string> = {
  STUDENT: "Студент",
  CURATOR: "Куратор",
  ADMIN: "Администратор"
};

const PENTEST_FLAG_PLACEHOLDER = 'Введите флаг формата pep{[a-zA-Z0-9]{20}}';

function difficultyUiClass(difficulty: string): string {
  const n = difficulty.trim().toLowerCase();
  if (n === "easy") return "easy";
  if (n === "medium") return "medium";
  if (n === "hard") return "hard";
  return "unknown";
}

function currentUserToAccount(user: CurrentUser): AccountSession {
  return {
    email: user.email,
    role: user.role,
    displayName: user.displayName,
    avatarUrl: user.avatarUrl ?? null
  };
}

function avatarImageSrc(account: AccountSession): string | undefined {
  if (!account.avatarUrl) {
    return undefined;
  }
  return `${apiBaseUrl}${account.avatarUrl}`;
}

function formatDifficultyLabel(difficulty: string) {
  const normalized = difficulty.trim().toLowerCase();
  if (normalized === "easy") return "Легкий";
  if (normalized === "medium") return "Средний";
  if (normalized === "hard") return "Сложный";
  return difficulty;
}

function formatRemainingTimer(expiresAtIso: string, nowMs: number) {
  const expiresAt = Date.parse(expiresAtIso);
  if (Number.isNaN(expiresAt)) {
    return "00:00:00";
  }
  const totalSeconds = Math.max(0, Math.floor((expiresAt - nowMs) / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

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

  csrfTokenCache = null;
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

async function fetchCurrentUserSession(): Promise<CurrentUser | null> {
  const response = await fetch(`${apiBaseUrl}/api/me`, { credentials: "include" });
  if (!response.ok) {
    return null;
  }
  return response.json() as Promise<CurrentUser>;
}

async function apiRequest<T>(_account: AccountSession, path: string, init?: RequestInit): Promise<T> {
  const requestUrl = `${apiBaseUrl}${path}`;
  const response = await fetchWithCsrf(requestUrl, init);

  if (!response.ok && response.status === 403 && isUnsafeMethod(init?.method)) {
    csrfTokenCache = null;
    const retryResponse = await fetchWithCsrf(requestUrl, init);
    if (retryResponse.ok) {
      if (retryResponse.status === 204) {
        return undefined as T;
      }
      return (await retryResponse.json()) as T;
    }

    const retryError = await retryResponse.json().catch(() => null);
    throw new Error(formatApiError(retryResponse.status, retryError, `Запрос ${path} не выполнен`));
  }

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(formatApiError(response.status, error, `Запрос ${path} не выполнен`));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

async function fetchWithCsrf(requestUrl: string, init?: RequestInit) {
  const csrf = await csrfHeaders(init);
  const needsJsonContentType = !(init?.body instanceof FormData);
  return fetch(requestUrl, {
    ...init,
    credentials: "include",
    headers: {
      ...(needsJsonContentType ? { "Content-Type": "application/json" } : {}),
      ...csrf,
      ...init?.headers
    }
  });
}

async function fetchWithCsrfRetry(requestUrl: string, init?: RequestInit): Promise<Response> {
  const response = await fetchWithCsrf(requestUrl, init);
  if (!response.ok && response.status === 403 && isUnsafeMethod(init?.method)) {
    csrfTokenCache = null;
    return fetchWithCsrf(requestUrl, init);
  }
  return response;
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

async function patchMyProfile(body: { displayName?: string; email?: string }): Promise<CurrentUser> {
  const response = await fetchWithCsrfRetry(`${apiBaseUrl}/api/me`, {
    method: "PATCH",
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(formatApiError(response.status, error, "Не удалось сохранить профиль"));
  }
  return response.json() as Promise<CurrentUser>;
}

async function changeMyPassword(currentPassword: string, newPassword: string): Promise<void> {
  const response = await fetchWithCsrfRetry(`${apiBaseUrl}/api/me/password`, {
    method: "POST",
    body: JSON.stringify({ currentPassword, newPassword })
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(formatApiError(response.status, error, "Не удалось сменить пароль"));
  }
}

async function uploadMyAvatar(file: File): Promise<CurrentUser> {
  const formData = new FormData();
  formData.append("file", file);
  const response = await fetchWithCsrfRetry(`${apiBaseUrl}/api/me/avatar`, {
    method: "POST",
    headers: {},
    body: formData
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(formatApiError(response.status, error, "Не удалось загрузить аватар"));
  }
  return response.json() as Promise<CurrentUser>;
}

async function uploadUserStandArchive(
  _account: AccountSession,
  payload: {
    archive: File;
    displayName: string;
    description?: string;
    moduleId: string;
  }
): Promise<UserPentestStand> {
  const formData = new FormData();
  formData.append("archive", payload.archive);
  formData.append("moduleId", payload.moduleId);
  if (payload.displayName.trim()) {
    formData.append("displayName", payload.displayName.trim());
  }
  if (payload.description && payload.description.trim()) {
    formData.append("description", payload.description.trim());
  }
  const response = await fetchWithCsrfRetry(`${apiBaseUrl}/api/user-stands`, {
    method: "POST",
    headers: {},
    body: formData
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(formatApiError(response.status, error, "Не удалось загрузить архив стенда"));
  }
  return response.json() as Promise<UserPentestStand>;
}

async function adminCreatePentestTaskFromArchive(
  _account: AccountSession,
  archive: File,
  metadata: {
    title: string;
    category: string;
    difficulty: string;
    durationMinutes: number;
    descriptionMarkdown?: string;
    flag: string;
    lessonId?: string;
    placementAfterHeading?: string;
  }
): Promise<PentestTask> {
  const formData = new FormData();
  formData.append("archive", archive);
  formData.append(
    "metadata",
    new Blob([JSON.stringify(metadata)], { type: "application/json" })
  );
  const csrf = await csrfHeaders({ method: "POST" });
  const response = await fetch(`${apiBaseUrl}/api/admin/pentest-tasks`, {
    method: "POST",
    credentials: "include",
    headers: csrf,
    body: formData
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(formatApiError(response.status, error, "Не удалось загрузить архив задачи"));
  }
  return response.json() as Promise<PentestTask>;
}

async function adminReplacePentestTaskArchive(
  _account: AccountSession,
  taskId: string,
  archive: File
): Promise<PentestTask> {
  const formData = new FormData();
  formData.append("archive", archive);
  const csrf = await csrfHeaders({ method: "PUT" });
  const response = await fetch(`${apiBaseUrl}/api/admin/pentest-tasks/${taskId}/archive`, {
    method: "PUT",
    credentials: "include",
    headers: csrf,
    body: formData
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(formatApiError(response.status, error, "Не удалось обновить архив задачи"));
  }
  return response.json() as Promise<PentestTask>;
}

async function adminPromoteStandToTask(
  account: AccountSession,
  standId: string,
  payload: {
    title: string;
    category: string;
    difficulty: string;
    durationMinutes: number;
    lessonId?: string;
    placementAfterHeading?: string;
  }
): Promise<PentestTask> {
  return apiRequest<PentestTask>(account, `/api/admin/pentest-tasks/from-stand/${standId}`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

async function adminLinkTaskToLesson(
  account: AccountSession,
  lessonId: string,
  taskId: string,
  payload: { placementAfterHeading?: string }
): Promise<PentestTask> {
  return apiRequest<PentestTask>(account, `/api/admin/lessons/${lessonId}/tasks/${taskId}`, {
    method: "POST",
    body: JSON.stringify(payload ?? {})
  });
}

async function adminUnlinkTaskFromLesson(
  account: AccountSession,
  lessonId: string,
  taskId: string
): Promise<void> {
  await apiRequest<void>(account, `/api/admin/lessons/${lessonId}/tasks/${taskId}`, {
    method: "DELETE"
  });
}

async function fetchLessonPentestTasks(
  account: AccountSession,
  lessonId: string
): Promise<PentestTask[]> {
  return apiRequest<PentestTask[]>(account, `/api/lessons/${lessonId}/pentest-tasks`);
}

async function fetchMyUserStands(account: AccountSession): Promise<UserPentestStand[]> {
  return apiRequest<UserPentestStand[]>(account, "/api/user-stands");
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

function ActionIcon({ name }: { name: "add" | "edit" | "delete" | "disable" }) {
  const props = { width: 16, height: 16, viewBox: "0 0 24 24", "aria-hidden": true } as const;
  switch (name) {
    case "add":
      return (
        <svg {...props}>
          <path
            d="M12 5v14M5 12h14"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          />
        </svg>
      );
    case "edit":
      return (
        <svg {...props}>
          <path
            d="M14.06 6.19l3.75 3.75M3 21h3.75L18.81 8.94a2.121 2.121 0 0 0-3-3L3.75 17.94 3 21z"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.75"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    case "delete":
      return (
        <svg {...props}>
          <path
            d="M4 7h16M9 7V4h6v3M6 7l1 13a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-13M10 11v6M14 11v6"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.75"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    case "disable":
      return (
        <svg {...props}>
          <path
            d="M12 3v9M5.64 7.64a8 8 0 1 0 12.72 0"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.75"
            strokeLinecap="round"
          />
        </svg>
      );
  }
}

function StreamAdminUserChecklist({
  users,
  selection,
  onToggle,
  disabled,
  emptyLabel,
  labelledById,
  listClassName
}: {
  users: AdminUser[];
  selection: Set<string>;
  onToggle: (userId: string) => void;
  disabled: boolean;
  emptyLabel: string;
  labelledById?: string;
  listClassName?: string;
}) {
  if (users.length === 0) {
    return <p className="muted stream-checklist-empty">{emptyLabel}</p>;
  }
  return (
    <div
      className={["stream-user-checklist", listClassName].filter(Boolean).join(" ")}
      role="group"
      aria-labelledby={labelledById}
    >
      {users.map((user) => {
        const checked = selection.has(user.id);
        return (
          <label key={user.id} className="stream-user-checklist-row">
            <input type="checkbox" checked={checked} disabled={disabled} onChange={() => onToggle(user.id)} />
            <span className="stream-user-checklist-body">
              <strong>{user.displayName}</strong>
              <span className="muted">
                {" · "}{user.email} · {user.role === "CURATOR" ? "куратор" : "студент"}
              </span>
            </span>
          </label>
        );
      })}
    </div>
  );
}

function filterEnrollCandidates(
  users: AdminUser[],
  search: string,
  roleFilter: StreamEnrollRoleFilter
): AdminUser[] {
  const needle = search.trim().toLowerCase();
  return users.filter((user) => {
    if (roleFilter !== "all" && user.role !== roleFilter) {
      return false;
    }
    if (!needle) {
      return true;
    }
    return user.displayName.toLowerCase().includes(needle) || user.email.toLowerCase().includes(needle);
  });
}

function StreamMemberEnrollSection({
  subtitleId,
  subtitle,
  searchInputId,
  candidates,
  selection,
  onSelectionChange,
  disabled,
  emptyNoCandidatesMessage,
  emptyFilteredMessage,
  listClassName
}: {
  subtitleId?: string;
  subtitle?: string;
  searchInputId: string;
  candidates: AdminUser[];
  selection: Set<string>;
  onSelectionChange: (next: Set<string>) => void;
  disabled: boolean;
  emptyNoCandidatesMessage: string;
  emptyFilteredMessage: string;
  listClassName?: string;
}) {
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<StreamEnrollRoleFilter>("all");

  const filtered = useMemo(
    () => filterEnrollCandidates(candidates, search, roleFilter),
    [candidates, roleFilter, search]
  );

  const toggleOne = (userId: string) => {
    const next = new Set(selection);
    if (next.has(userId)) {
      next.delete(userId);
    } else {
      next.add(userId);
    }
    onSelectionChange(next);
  };

  const selectAllVisible = () => {
    const next = new Set(selection);
    for (const user of filtered) {
      next.add(user.id);
    }
    onSelectionChange(next);
  };

  const clearAllSelection = () => onSelectionChange(new Set());

  if (candidates.length === 0) {
    return (
      <div className="stream-member-enroll-section">
        {subtitle?.trim() ? (
          <p className="field-caption" id={subtitleId}>
            {subtitle}
          </p>
        ) : null}
        <p className="muted stream-checklist-empty">{emptyNoCandidatesMessage}</p>
      </div>
    );
  }

  return (
    <div className="stream-member-enroll-section">
      {subtitle?.trim() ? (
        <p className="field-caption" id={subtitleId}>
          {subtitle}
        </p>
      ) : null}
      <div className="stream-enroll-toolbar">
        <div className="stream-enroll-search-row">
          <input
            id={searchInputId}
            type="search"
            className="stream-enroll-search"
            aria-label="Поиск"
            placeholder=""
            autoComplete="off"
            spellCheck={false}
            disabled={disabled}
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <div className="stream-role-segmented" role="group" aria-label="Кого показывать">
          {(
            [
              ["all", "Все"] as const,
              ["STUDENT", "Студенты"] as const,
              ["CURATOR", "Кураторы"] as const
            ]
          ).map(([value, label]) => (
            <button
              key={value}
              type="button"
              disabled={disabled}
              className={value === roleFilter ? "stream-role-chip is-active" : "stream-role-chip"}
              onClick={() => setRoleFilter(value)}
            >
              {label}
            </button>
          ))}
        </div>
      </div>
      <div className="stream-enroll-bulk-row">
        <span className="stream-enroll-bulk-meta">
          Кандидатов в списке: <strong>{filtered.length}</strong> из {candidates.length}. Выбрано всего:{" "}
          <strong>{selection.size}</strong>.
        </span>
        <div className="stream-enroll-bulk-actions">
          <button
            type="button"
            className="secondary"
            disabled={disabled || filtered.length === 0}
            onClick={selectAllVisible}
          >
            Выбрать всех показанных
          </button>
          <button type="button" className="secondary" disabled={disabled || selection.size === 0} onClick={clearAllSelection}>
            Снять выбор
          </button>
        </div>
      </div>
      {filtered.length === 0 ? (
        <p className="muted stream-checklist-empty">{emptyFilteredMessage}</p>
      ) : (
        <StreamAdminUserChecklist
          labelledById={subtitleId ?? searchInputId}
          users={filtered}
          selection={selection}
          disabled={disabled}
          listClassName={listClassName}
          emptyLabel=""
          onToggle={toggleOne}
        />
      )}
    </div>
  );
}

function MarkdownPreview({
  source,
  withHeadingIds = false,
  sectionH2Id
}: {
  source: string;
  withHeadingIds?: boolean;
  sectionH2Id?: string;
}) {
  const rendered = useMemo(
    () => renderMarkdown(source, withHeadingIds, sectionH2Id),
    [source, withHeadingIds, sectionH2Id]
  );

  if (source.trim().length === 0) {
    return <EmptyState>Markdown preview появится после ввода текста.</EmptyState>;
  }

  return <div className="markdown-preview">{rendered}</div>;
}

function normalizeEscapedNewlines(value: string) {
  return value.replace(/\\n/g, "\n");
}

function renderTextWithBreaks(value: string, keyPrefix: string) {
  const lines = value.split("\n");
  return lines.map((line, index) => (
    <React.Fragment key={`${keyPrefix}-${index}`}>
      {line}
      {index < lines.length - 1 && <br />}
    </React.Fragment>
  ));
}

function CopyableBlock({ text, children }: { text: string; children: React.ReactNode }) {
  return (
    <div className="copyable-block">
      <button
        type="button"
        className="copyable-block-button"
        aria-label="Копировать"
        title="Копировать"
        onClick={() => {
          void navigator.clipboard.writeText(text);
        }}
      >
        <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true">
          <rect x="8.5" y="8.5" width="12" height="12" rx="1.5" fill="none" stroke="currentColor" strokeWidth="1.75" />
          <path
            d="M15.5 8.5V6.5A2 2 0 0 0 13.5 4.5H6.5A2 2 0 0 0 4.5 6.5V14.5A2 2 0 0 0 6.5 16.5H8.5"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.75"
            strokeLinecap="round"
          />
        </svg>
      </button>
      {children}
    </div>
  );
}

function renderMarkdown(source: string, withHeadingIds = false, sectionH2Id?: string) {
  const lines = source.replace(/\r\n/g, "\n").split("\n");
  const elements: React.ReactNode[] = [];
  const uniqueSlug = createUniqueSlugFactory();
  const childSlug = sectionH2Id && withHeadingIds ? createUniqueSlugFactory() : null;
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
      const codeText = normalizeEscapedNewlines(codeLines.join("\n"));
      elements.push(
        <CopyableBlock key={`code-${index}`} text={codeText}>
          <pre>
            <code>{codeText}</code>
          </pre>
        </CopyableBlock>
      );
      continue;
    }

    const heading = /^(#{1,3})\s+(.+)$/.exec(line);
    if (heading) {
      const level = heading[1].length;
      const content = renderInlineMarkdown(heading[2]);
      const plain = plainMarkdownText(heading[2]);
      let id: string | undefined;
      if (withHeadingIds) {
        if (level === 3 && sectionH2Id && childSlug) {
          id = `${sectionH2Id}--${childSlug(plain)}`;
        } else if (level <= 3) {
          id = uniqueSlug(plain);
        }
      }
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
      const quoteText = normalizeEscapedNewlines(quoteLines.join("\n"));
      elements.push(
        <CopyableBlock key={`quote-${index}`} text={quoteText}>
          <blockquote>{renderInlineMarkdown(quoteText)}</blockquote>
        </CopyableBlock>
      );
      continue;
    }

    if (isMarkdownTableStart(lines, index)) {
      const header = splitMarkdownTableRow(lines[index]);
      index += 2;
      const bodyRows: string[][] = [];
      while (
        index < lines.length &&
        isMarkdownTableRow(lines[index]) &&
        !isMarkdownTableDivider(lines[index])
      ) {
        bodyRows.push(splitMarkdownTableRow(lines[index]));
        index++;
      }
      elements.push(renderMarkdownTable(header, bodyRows, index));
      continue;
    }

    const paragraphLines = [line];
    index++;
    while (
      index < lines.length &&
      lines[index].trim() !== "" &&
      !isMarkdownBlockStart(lines[index], lines, index)
    ) {
      paragraphLines.push(lines[index]);
      index++;
    }
    elements.push(<p key={`p-${index}`}>{renderInlineMarkdown(paragraphLines.join(" "))}</p>);
  }

  return elements;
}

function extractMarkdownHeadings(source: string): MarkdownHeading[] {
  const lines = source.replace(/\r\n/g, "\n").split("\n");
  const h2Slug = createUniqueSlugFactory();
  const headings: MarkdownHeading[] = [];
  let currentH2Id: string | null = null;
  let childSlug: ReturnType<typeof createUniqueSlugFactory> | null = null;
  for (const line of lines) {
    const match = /^(#{2,3})\s+(.+)$/.exec(line);
    if (!match) {
      continue;
    }
    const level = match[1].length;
    const title = plainMarkdownText(match[2]);
    if (level === 2) {
      currentH2Id = h2Slug(title);
      childSlug = createUniqueSlugFactory();
      headings.push({ id: currentH2Id, title, level });
    } else if (level === 3 && currentH2Id && childSlug) {
      headings.push({ id: `${currentH2Id}--${childSlug(title)}`, title, level: 3 });
    } else if (level === 3) {
      headings.push({ id: h2Slug(title), title, level: 3 });
    }
  }
  return headings;
}

function splitMarkdownH2Sections(source: string): Array<{ heading: MarkdownHeading; body: string }> {
  const lines = source.replace(/\r\n/g, "\n").split("\n");
  const slugFactory = createUniqueSlugFactory();
  const sections: Array<{ heading: MarkdownHeading; body: string }> = [];
  let current: MarkdownHeading | null = null;
  const buffer: string[] = [];
  const flush = () => {
    if (current) {
      sections.push({ heading: current, body: buffer.join("\n").replace(/\n+$/, "") });
      buffer.length = 0;
    }
  };
  for (const line of lines) {
    const match = /^## (.+)$/.exec(line);
    if (match && !/^###/.test(line)) {
      flush();
      const title = plainMarkdownText(match[1]);
      current = { id: slugFactory(title), title, level: 2 };
    } else {
      buffer.push(line);
    }
  }
  flush();
  return sections;
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

type LessonBlockType = "p" | "h1" | "h2" | "h3" | "code" | "ul" | "ol" | "quote" | "table";

type LessonBlock = {
  id: string;
  type: LessonBlockType;
  text: string;
};

let lessonBlockIdSeq = 0;
function newBlockId() {
  lessonBlockIdSeq += 1;
  return `blk-${Date.now().toString(36)}-${lessonBlockIdSeq}`;
}

function parseMarkdownToBlocks(source: string): LessonBlock[] {
  const blocks: LessonBlock[] = [];
  if (!source) {
    return [{ id: newBlockId(), type: "p", text: "" }];
  }
  const lines = source.replace(/\r\n/g, "\n").split("\n");
  let index = 0;
  while (index < lines.length) {
    const line = lines[index];
    if (line === undefined) {
      index += 1;
      continue;
    }
    if (line.trim() === "") {
      index += 1;
      continue;
    }
    const fenceMatch = /^```/.exec(line);
    if (fenceMatch) {
      const codeLines: string[] = [];
      index += 1;
      while (index < lines.length && !/^```/.test(lines[index] ?? "")) {
        codeLines.push(lines[index] ?? "");
        index += 1;
      }
      if (index < lines.length) {
        index += 1;
      }
      blocks.push({ id: newBlockId(), type: "code", text: codeLines.join("\n") });
      continue;
    }
    const heading = /^(#{1,3})\s+(.*)$/.exec(line);
    if (heading) {
      const level = heading[1].length as 1 | 2 | 3;
      const type = (`h${level}` as LessonBlockType);
      blocks.push({ id: newBlockId(), type, text: heading[2].trim() });
      index += 1;
      continue;
    }
    if (isMarkdownTableStart(lines, index)) {
      const tableLines: string[] = [];
      while (index < lines.length && lines[index] !== undefined && lines[index].trim() !== "") {
        tableLines.push(lines[index]);
        index += 1;
      }
      blocks.push({ id: newBlockId(), type: "table", text: tableLines.join("\n") });
      continue;
    }
    if (/^\s*>\s?/.test(line)) {
      const quoteLines: string[] = [];
      while (index < lines.length && /^\s*>\s?/.test(lines[index] ?? "")) {
        quoteLines.push((lines[index] ?? "").replace(/^\s*>\s?/, ""));
        index += 1;
      }
      blocks.push({ id: newBlockId(), type: "quote", text: quoteLines.join("\n") });
      continue;
    }
    if (/^\s*[-*]\s+/.test(line)) {
      const ulLines: string[] = [];
      while (index < lines.length && /^\s*[-*]\s+/.test(lines[index] ?? "")) {
        ulLines.push((lines[index] ?? "").replace(/^\s*[-*]\s+/, ""));
        index += 1;
      }
      blocks.push({ id: newBlockId(), type: "ul", text: ulLines.join("\n") });
      continue;
    }
    if (/^\s*\d+\.\s+/.test(line)) {
      const olLines: string[] = [];
      while (index < lines.length && /^\s*\d+\.\s+/.test(lines[index] ?? "")) {
        olLines.push((lines[index] ?? "").replace(/^\s*\d+\.\s+/, ""));
        index += 1;
      }
      blocks.push({ id: newBlockId(), type: "ol", text: olLines.join("\n") });
      continue;
    }
    // Параграф: собираем подряд идущие строки до пустой строки или начала блока другого типа.
    const paragraphLines: string[] = [];
    while (index < lines.length) {
      const current = lines[index] ?? "";
      if (current.trim() === "") {
        break;
      }
      if (
        /^(#{1,3})\s+/.test(current)
        || /^```/.test(current)
        || /^\s*>\s?/.test(current)
        || /^\s*[-*]\s+/.test(current)
        || /^\s*\d+\.\s+/.test(current)
        || isMarkdownTableStart(lines, index)
      ) {
        break;
      }
      paragraphLines.push(current);
      index += 1;
    }
    blocks.push({ id: newBlockId(), type: "p", text: paragraphLines.join("\n") });
  }
  if (blocks.length === 0) {
    blocks.push({ id: newBlockId(), type: "p", text: "" });
  }
  return blocks;
}

function serializeBlocksToMarkdown(blocks: LessonBlock[]): string {
  const parts: string[] = [];
  blocks.forEach((block) => {
    switch (block.type) {
      case "h1":
        parts.push(`# ${block.text.trim()}`);
        break;
      case "h2":
        parts.push(`## ${block.text.trim()}`);
        break;
      case "h3":
        parts.push(`### ${block.text.trim()}`);
        break;
      case "code":
        parts.push("```\n" + block.text.replace(/\s+$/, "") + "\n```");
        break;
      case "ul":
        parts.push(
          block.text
            .split("\n")
            .map((line) => `- ${line}`)
            .join("\n")
        );
        break;
      case "ol":
        parts.push(
          block.text
            .split("\n")
            .map((line, idx) => `${idx + 1}. ${line}`)
            .join("\n")
        );
        break;
      case "quote":
        parts.push(
          block.text
            .split("\n")
            .map((line) => `> ${line}`)
            .join("\n")
        );
        break;
      case "table":
        parts.push(block.text);
        break;
      case "p":
      default:
        parts.push(block.text);
        break;
    }
  });
  return parts.join("\n\n").replace(/\n{3,}/g, "\n\n");
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

function moduleTopicSlug(module: ModuleOption): string {
  return webSecurityPath(module).replace(/^\/web-security\//, "");
}

/**
 * URL для рабочего пространства, учитывающий активный раздел из текущего pathname.
 * - learning: /web-security/<slug>
 * - practice: /my-stand?module=<slug>
 * - peer: /peer-tasks?module=<slug>
 */
function workspaceUrlForModule(module: ModuleOption): string {
  const slug = moduleTopicSlug(module);
  const section =
    typeof window === "undefined" ? null : studentSectionFromPathname(window.location.pathname);
  if (section === "practice") {
    return `/my-stand?module=${encodeURIComponent(slug)}`;
  }
  if (section === "peer") {
    return `/peer-tasks?module=${encodeURIComponent(slug)}`;
  }
  return webSecurityPath(module);
}

function findModuleIdByTopicSlug(slug: string, courses: Course[]): string | undefined {
  const target = `/web-security/${slug}`;
  for (const course of courses) {
    for (const module of course.modules) {
      const option = { ...module, courseTitle: course.title, courseId: course.id } as ModuleOption;
      if (webSecurityPath(option) === target) {
        return module.id;
      }
    }
  }
  return undefined;
}

function pentestTaskUrl(lessonPath: string, taskSlug: string) {
  return `${lessonPath}/vulnerability/${taskSlug}`;
}

function parsePentestTaskSlugFromPath(pathname: string): string | null {
  const match = pathname.match(/\/vulnerability\/([^/]+)\/?$/);
  return match ? match[1] : null;
}

/** Базовый путь модуля без сегмента /vulnerability/… */
function lessonPathPrefixFromPathname(pathname: string): string {
  const normalized = pathname.replace(/\/$/, "") || "/";
  return normalized.replace(/\/vulnerability\/[^/]+$/, "") || "/";
}

function studentSectionFromPathname(pathname: string): StudentSectionId | null {
  const normalized = pathname.replace(/\/$/, "") || "/";
  if (normalized === "/my-stand" || normalized.startsWith("/my-stand/")) return "practice";
  if (normalized === "/peer-tasks" || normalized.startsWith("/peer-tasks/")) return "peer";
  return null;
}

function moduleSlugFromLocation(): string | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  const slug = params.get("module");
  return slug && slug.length > 0 ? slug : null;
}

function inferActivePageFromPathname(pathname: string): AppPage {
  const p = pathname.replace(/\/$/, "") || "/";
  if (p === "/courses") {
    return "courses";
  }
  if (p === "/profile") {
    return "profile";
  }
  if (p === "/stands") {
    return "courses";
  }
  return "workspace";
}

function findModuleIdByWebLessonPath(lessonPathPrefix: string, courses: Course[]): string | undefined {
  for (const course of courses) {
    for (const module of course.modules) {
      const option = { ...module, courseTitle: course.title, courseId: course.id } as ModuleOption;
      if (webSecurityPath(option) === lessonPathPrefix) {
        return module.id;
      }
    }
  }
  return undefined;
}

function LessonMaterialBreadcrumbs({
  courseTitle,
  moduleTitle,
  thirdLabel,
  lessonPath,
  onCourseClick,
  onNavigateLessonTitle,
  onThirdClick
}: {
  courseTitle: string;
  moduleTitle: string;
  thirdLabel: string;
  lessonPath: string;
  onCourseClick: () => void;
  onNavigateLessonTitle: () => void;
  onThirdClick: () => void;
}) {
  return (
    <nav className="lesson-breadcrumbs lesson-breadcrumbs-linked" aria-label="Навигация по курсу">
      <a
        className="breadcrumb-link"
        href="/courses"
        onClick={(event) => {
          event.preventDefault();
          onCourseClick();
        }}
      >
        {courseTitle}
      </a>
      <span className="breadcrumb-sep" aria-hidden>
        ·
      </span>
      <a
        className="breadcrumb-link"
        href={lessonPath}
        onClick={(event) => {
          event.preventDefault();
          onNavigateLessonTitle();
        }}
      >
        {moduleTitle}
      </a>
      <span className="breadcrumb-sep" aria-hidden>
        ·
      </span>
      <a
        className="breadcrumb-link"
        href={lessonPath}
        onClick={(event) => {
          event.preventDefault();
          onThirdClick();
        }}
      >
        {thirdLabel}
      </a>
    </nav>
  );
}

function TaskPageBreadcrumbs({
  courseTitle,
  moduleTitle,
  taskTitle,
  taskSlug,
  lessonPath,
  onCourseClick,
  onModuleClick,
  onTaskClick
}: {
  courseTitle: string;
  moduleTitle: string;
  taskTitle: string;
  taskSlug: string;
  lessonPath: string;
  onCourseClick: () => void;
  onModuleClick: () => void;
  onTaskClick: () => void;
}) {
  return (
    <nav className="lesson-breadcrumbs lesson-breadcrumbs-linked" aria-label="Навигация по заданию">
      <a
        className="breadcrumb-link"
        href="/courses"
        onClick={(event) => {
          event.preventDefault();
          onCourseClick();
        }}
      >
        {courseTitle}
      </a>
      <span className="breadcrumb-sep" aria-hidden>
        ·
      </span>
      <a
        className="breadcrumb-link"
        href={lessonPath}
        onClick={(event) => {
          event.preventDefault();
          onModuleClick();
        }}
      >
        {moduleTitle}
      </a>
      <span className="breadcrumb-sep" aria-hidden>
        ·
      </span>
      <a
        className="breadcrumb-link"
        href={pentestTaskUrl(lessonPath, taskSlug)}
        onClick={(event) => {
          event.preventDefault();
          onTaskClick();
        }}
      >
        {taskTitle}
      </a>
    </nav>
  );
}

/** Убирает шаблонное описание практических заданий из markdown (старые данные в БД / синхронизация). */
function sanitizePentestTaskDescriptionMarkdown(source: string): string {
  return source
    .replace(/##\s*Описание\s+задачи[\s\S]*$/i, "")
    .replace(/##\s*Что\s+сделать[\s\S]*$/i, "")
    .trim();
}

function ModuleTaskLabRow({
  task,
  solved,
  onOpen
}: {
  task: PentestTask;
  solved?: boolean;
  onOpen: () => void;
}) {
  const levelLabel = formatDifficultyLabel(task.difficulty);
  const dClass = difficultyUiClass(task.difficulty);
  return (
    <button
      type="button"
      className={`secondary module-lab-row${solved ? " module-lab-row-solved" : ""}`}
      onClick={onOpen}
    >
      <span className="module-lab-pill module-lab-pill-brand">Лаборатория</span>
      <span className={`module-lab-pill module-lab-pill-level difficulty-${dClass}`}>{levelLabel}</span>
      <span className="module-lab-row-title">{task.title}</span>
      <span className="module-lab-row-arrow" aria-hidden>
        →
      </span>
    </button>
  );
}

function isMarkdownTableRow(line: string): boolean {
  const t = line.trim();
  return t.startsWith("|") && t.lastIndexOf("|") > 0;
}

function isMarkdownTableDivider(line: string): boolean {
  if (!isMarkdownTableRow(line)) {
    return false;
  }
  const inner = line
    .trim()
    .replace(/^\|/, "")
    .replace(/\|$/, "")
    .split("|");
  return inner.every((cell) => /^:?-{3,}:?$/.test(cell.trim()));
}

function splitMarkdownTableRow(line: string): string[] {
  let t = line.trim();
  if (t.startsWith("|")) {
    t = t.slice(1);
  }
  if (t.endsWith("|")) {
    t = t.slice(0, -1);
  }
  return t.split("|").map((cell) => cell.trim());
}

function isMarkdownTableStart(lines: readonly string[], index: number): boolean {
  if (index >= lines.length || index + 1 >= lines.length) {
    return false;
  }
  const line = lines[index];
  if (!isMarkdownTableRow(line) || isMarkdownTableDivider(line)) {
    return false;
  }
  return isMarkdownTableDivider(lines[index + 1]);
}

function renderMarkdownTable(header: string[], bodyRows: string[][], key: number) {
  const columnCount =
    bodyRows.length > 0
      ? Math.max(header.length, ...bodyRows.map((r) => r.length))
      : header.length;
  const normalize = (cells: string[]) => {
    const next = [...cells];
    while (next.length < columnCount) {
      next.push("");
    }
    return next.slice(0, columnCount);
  };
  const head = normalize(header);
  const rows = bodyRows.map(normalize);
  return (
    <div className="md-table-wrap" key={`md-table-${key}`}>
      <table className="md-table">
        <thead>
          <tr>
            {head.map((cell, i) => (
              <th key={i}>{renderInlineMarkdown(cell)}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, ri) => (
            <tr key={ri}>
              {row.map((cell, ci) => (
                <td key={ci}>{renderInlineMarkdown(cell)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function isMarkdownBlockStart(line: string, lines?: readonly string[], lineIndex?: number) {
  const trimmed = line.trim();
  if (
    trimmed.startsWith("```") ||
    /^(#{1,3})\s+/.test(line) ||
    /^\s*[-*]\s+/.test(line) ||
    /^\s*\d+\.\s+/.test(line) ||
    /^\s*>\s?/.test(line)
  ) {
    return true;
  }
  if (lines && lineIndex !== undefined && isMarkdownTableStart(lines, lineIndex)) {
    return true;
  }
  return false;
}

function renderInlineMarkdown(text: string) {
  const parts = normalizeEscapedNewlines(text).split(/(`[^`]+`|\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\))/g);
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
    return renderTextWithBreaks(part, `inline-${index}`);
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

function moduleLabProgress(
  moduleTitle: string,
  pentestTasks: PentestTask[],
  pentestTaskInstances: PentestTaskInstance[]
): { completed: number; total: number } {
  const title = moduleTitle.trim();
  const labs = pentestTasks.filter((t) => t.category.trim() === title);
  if (labs.length === 0) {
    return { completed: 0, total: 0 };
  }
  const solvedIds = new Set(pentestTaskInstances.filter((i) => i.flagSolved).map((i) => i.taskId));
  const completed = labs.filter((lab) => solvedIds.has(lab.id)).length;
  return { completed, total: labs.length };
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

function ModuleLabMiniBar({ completed, total }: { completed: number; total: number }) {
  if (total <= 0) {
    return null;
  }
  const percent = Math.round((completed / total) * 100);
  return (
    <div className="course-tile-lab-bar" aria-label={`Лабораторные: ${completed} из ${total}`}>
      <div className="course-tile-lab-track">
        <div className="course-tile-lab-fill" style={{ width: `${percent}%` }} />
      </div>
      <span className="course-tile-lab-count">
        Лабораторные: {completed}/{total}
      </span>
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
    selectedModuleId: undefined,
    labs: [],
    assignments: [],
    auditEvents: [],
    users: [],
    onlineUsers: undefined,
    pentestTasks: [],
    pentestTaskInstances: []
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("Войдите в систему, чтобы загрузить рабочую область.");
  const [error, setError] = useState<string | null>(null);
  const [standLaunchToast, setStandLaunchToast] = useState<StandLaunchToast | null>(null);
  const [liveStatus, setLiveStatus] = useState<LiveStatus | null>(null);
  const [liveConnected, setLiveConnected] = useState(false);
  const [activePage, setActivePage] = useState<AppPage>("workspace");
  const [workspaceTaskId, setWorkspaceTaskId] = useState<string | null>(null);
  const [coursePreviewMode, setCoursePreviewMode] = useState(false);
  const [authReady, setAuthReady] = useState(false);
  const [locationNonce, setLocationNonce] = useState(0);
  useEffect(() => {
    const bump = () => setLocationNonce((n) => n + 1);
    window.addEventListener("popstate", bump);
    return () => window.removeEventListener("popstate", bump);
  }, []);

  const coursesOpenCourseId = useMemo(() => {
    void locationNonce;
    if (activePage !== "courses") {
      return null;
    }
    return new URLSearchParams(window.location.search).get("openCourse");
  }, [activePage, locationNonce]);

  useEffect(() => {
    if (!error) {
      return undefined;
    }
    const timeoutId = window.setTimeout(() => setError(null), 10_000);
    return () => window.clearTimeout(timeoutId);
  }, [error]);

  useEffect(() => {
    if (!standLaunchToast) {
      return undefined;
    }
    const timeoutId = window.setTimeout(() => setStandLaunchToast(null), 15_000);
    return () => window.clearTimeout(timeoutId);
  }, [standLaunchToast]);

  const moduleOptions = useMemo(
    () =>
      state.courses.flatMap((course) =>
        course.modules.map((module) => ({
          ...module,
          courseTitle: course.title,
          courseId: course.id
        }))
      ),
    [state.courses]
  );
  const selectedModule = useMemo(
    () => moduleOptions.find((module) => module.id === state.selectedModuleId) ?? moduleOptions[0],
    [moduleOptions, state.selectedModuleId]
  );
  const labModule = selectedModule;

  const pentestTasksRef = useRef<PentestTask[]>([]);
  pentestTasksRef.current = state.pentestTasks;
  const selectModuleRef = useRef<((moduleId: string, options?: { openTaskId?: string }) => Promise<void>) | null>(
    null
  );
  const closeWorkspaceTask = useCallback(() => {
    const mod = moduleOptions.find((m) => m.id === state.selectedModuleId);
    if (mod) {
      window.history.replaceState(null, "", workspaceUrlForModule(mod));
    }
    setWorkspaceTaskId(null);
  }, [moduleOptions, state.selectedModuleId]);
  const openWorkspaceTask = useCallback(
    (taskId: string) => {
      const task = state.pentestTasks.find((t) => t.id === taskId);
      const mod = moduleOptions.find((m) => m.id === state.selectedModuleId);
      if (!task || !mod || task.category !== mod.title) {
        return;
      }
      const base = webSecurityPath(mod);
      window.history.pushState({ pepTask: taskId }, "", pentestTaskUrl(base, task.slug));
      setWorkspaceTaskId(taskId);
      window.scrollTo(0, 0);
    },
    [moduleOptions, state.pentestTasks, state.selectedModuleId]
  );
  const navigateToCourses = useCallback((openCourseId?: string) => {
    setWorkspaceTaskId(null);
    setCoursePreviewMode(false);
    setActivePage("courses");
    const qs = openCourseId ? `?openCourse=${encodeURIComponent(openCourseId)}` : "";
    window.history.replaceState(null, "", `/courses${qs}`);
    setLocationNonce((n) => n + 1);
    window.scrollTo(0, 0);
  }, []);
  const navigateToWorkspace = useCallback(() => {
    setWorkspaceTaskId(null);
    setCoursePreviewMode(false);
    setActivePage("workspace");
    window.history.replaceState(null, "", "/workspace");
    setLocationNonce((n) => n + 1);
    window.scrollTo(0, 0);
  }, []);
  const handleSelectPage = useCallback(
    (page: AppPage) => {
      if (page === "courses") {
        navigateToCourses();
        return;
      }
      if (page === "profile") {
        setWorkspaceTaskId(null);
        setActivePage("profile");
        window.history.replaceState(null, "", "/profile");
        setLocationNonce((n) => n + 1);
        window.scrollTo(0, 0);
        return;
      }
      navigateToWorkspace();
    },
    [navigateToCourses, navigateToWorkspace]
  );

  async function login(credentials: AuthCredentials) {
    setLoading(true);
    setError(null);
    try {
      const currentUser = await loginRequest(credentials);
      const nextAccount = currentUserToAccount(currentUser);
      setAccount(nextAccount);
      const loaded = await loadDashboard(nextAccount);
      if (loaded) {
        const deepSlug =
          nextAccount.role === "STUDENT" ? parsePentestTaskSlugFromPath(window.location.pathname) : null;
        if (deepSlug) {
          setActivePage("workspace");
        } else {
          navigateToWorkspace();
        }
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
    setWorkspaceTaskId(null);
    setActivePage("workspace");
    setState({
      courses: [],
      submissions: [],
      validationJobs: [],
      reports: [],
      reviews: [],
      lessons: [],
      selectedLesson: undefined,
      selectedModuleId: undefined,
      labs: [],
      assignments: [],
      auditEvents: [],
      users: [],
      onlineUsers: undefined,
      pentestTasks: [],
      pentestTaskInstances: []
    });
    setMessage("Сессия завершена.");
  }

  async function loadDashboard(activeAccount: AccountSession, options?: { pathnameHint?: string }): Promise<boolean> {
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
      const pathnameHint = options?.pathnameHint?.trim() ?? "";
      let nextSelectedModuleId =
        loadedModules.find((module) => module.id === state.selectedModuleId)?.id ?? loadedModules[0]?.id;
      if (pathnameHint) {
        const page = inferActivePageFromPathname(pathnameHint);
        if (page === "workspace") {
          const sectionFromPath = studentSectionFromPathname(pathnameHint);
          if (sectionFromPath) {
            const slug = moduleSlugFromLocation();
            if (slug) {
              const fromSlug = findModuleIdByTopicSlug(slug, courses);
              if (fromSlug) {
                nextSelectedModuleId = fromSlug;
              }
            }
          } else {
            const lessonPrefix = lessonPathPrefixFromPathname(pathnameHint);
            const fromUrl = findModuleIdByWebLessonPath(lessonPrefix, courses);
            if (fromUrl) {
              nextSelectedModuleId = fromUrl;
            }
          }
        }
      }
      const nextSelectedModule = loadedModules.find((module) => module.id === nextSelectedModuleId) ?? loadedModules[0];
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
        activeAccount.role === "ADMIN"
          ? (await apiRequest<PageResponse<AdminUser>>(activeAccount, "/api/admin/users?size=100")).items
          : [];
      const onlineUsers =
        activeAccount.role === "ADMIN"
          ? await apiRequest<OnlineUsers>(activeAccount, "/api/admin/online-users")
          : undefined;
      const pentestTasks = await apiRequest<PentestTask[]>(activeAccount, "/api/pentest-tasks");
      const pentestTaskInstances =
        activeAccount.role === "STUDENT"
          ? await apiRequest<PentestTaskInstance[]>(activeAccount, "/api/pentest-task-instances/my")
          : [];
      setState({
        courses,
        submissions,
        validationJobs,
        reports,
        reviews,
        lessons,
        selectedLesson,
        selectedModuleId: nextSelectedModule?.id,
        labs,
        assignments,
        auditEvents,
        users,
        onlineUsers,
        pentestTasks,
        pentestTaskInstances
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
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const user = await fetchCurrentUserSession();
        if (cancelled || !user) {
          return;
        }
        const nextAccount = currentUserToAccount(user);
        setAccount(nextAccount);
        const loaded = await loadDashboard(nextAccount, { pathnameHint: window.location.pathname });
        if (!cancelled && loaded) {
          setActivePage(inferActivePageFromPathname(window.location.pathname));
        }
      } finally {
        if (!cancelled) {
          setAuthReady(true);
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

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

  useEffect(() => {
    if (!account || account.role !== "STUDENT") {
      return undefined;
    }
    const syncPath = () => {
      if (activePage !== "workspace") {
        return;
      }
      const select = selectModuleRef.current;
      if (!select) {
        return;
      }
      const slug = parsePentestTaskSlugFromPath(window.location.pathname);
      if (!slug) {
        setWorkspaceTaskId(null);
        return;
      }
      const task = pentestTasksRef.current.find((t) => t.slug === slug);
      if (!task) {
        setWorkspaceTaskId(null);
        return;
      }
      const moduleMeta = moduleOptions.find((m) => m.title === task.category);
      if (!moduleMeta) {
        setWorkspaceTaskId(null);
        return;
      }
      if (state.selectedModuleId !== moduleMeta.id) {
        void select(moduleMeta.id, { openTaskId: task.id });
        return;
      }
      setWorkspaceTaskId(task.id);
    };
    syncPath();
    window.addEventListener("popstate", syncPath);
    return () => window.removeEventListener("popstate", syncPath);
  }, [account, activePage, moduleOptions, state.selectedModuleId, state.pentestTasks]);

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

  async function selectModule(moduleId: string, options?: { openTaskId?: string }) {
    if (!account) {
      setError("Сначала выполните вход.");
      return;
    }
    setWorkspaceTaskId(null);
    setLoading(true);
    setError(null);
    try {
      const lessons = await apiRequest<LessonSummary[]>(account, `/api/modules/${moduleId}/lessons`);
      const selectedLesson = lessons[0] ? await apiRequest<Lesson>(account, `/api/lessons/${lessons[0].id}`) : undefined;
      const moduleMeta = state.courses
        .flatMap((course) =>
          course.modules.map(
            (module) => ({ ...module, courseTitle: course.title, courseId: course.id }) as ModuleOption
          )
        )
        .find((module) => module.id === moduleId);
      setState((current) => ({
        ...current,
        lessons,
        selectedLesson,
        selectedModuleId: moduleId
      }));
      const lessonPath = moduleMeta ? webSecurityPath(moduleMeta) : "/web-security";
      const sectionUrl = moduleMeta ? workspaceUrlForModule(moduleMeta) : "/web-security";
      const openTaskId = options?.openTaskId;
      window.requestAnimationFrame(() => {
        if (openTaskId) {
          const task = pentestTasksRef.current.find((t) => t.id === openTaskId);
          if (task && task.category === moduleMeta?.title) {
            window.history.replaceState(null, "", pentestTaskUrl(lessonPath, task.slug));
            window.scrollTo(0, 0);
            setWorkspaceTaskId(task.id);
            return;
          }
        }
        window.history.replaceState(null, "", sectionUrl);
        window.scrollTo(0, 0);
      });
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

  selectModuleRef.current = selectModule;

  return (
    <main className="app-shell">
      {!authReady && <div className="loading-line" aria-label="Проверка сессии" />}
      {authReady && !account && (
        <section className="login-screen">
          <LoginForm onLogin={login} disabled={loading} />
        </section>
      )}
      {authReady && account ? (
        <>
          <TopNavigation
            account={account}
            activePage={account.role === "STUDENT" && activePage === "workspace" ? "courses" : activePage}
            loading={loading}
            onSelectPage={handleSelectPage}
            onLogout={logout}
          />
          {loading && <div className="loading-line" aria-label="Загрузка" />}
          {error && (
            <div className="toast toast--uniform toast--dismissible error" role="alert">
              <button
                type="button"
                className="toast-dismiss"
                aria-label="Закрыть уведомление"
                onClick={() => setError(null)}
              >
                ×
              </button>
              <strong>Ошибка</strong>
              <span className="toast-body">{error}</span>
            </div>
          )}
          {standLaunchToast && (
            <div
              className={[
                "toast toast--uniform toast--dismissible",
                standLaunchToast.variant === "blocked" ? "error" : "success",
                standLaunchToast.variant === "blocked" && standLaunchToast.publicUrl?.trim()
                  ? "toast--with-action"
                  : ""
              ]
                .filter(Boolean)
                .join(" ")}
              role="status"
            >
              <button
                type="button"
                className="toast-dismiss"
                aria-label="Закрыть уведомление"
                onClick={() => setStandLaunchToast(null)}
              >
                ×
              </button>
              <strong>
                {standLaunchToast.variant === "launched"
                  ? "Стенд запущен"
                  : "Стенд уже запущен"}
              </strong>
              <span className="toast-body">«{standLaunchToast.standName}»</span>
              {standLaunchToast.variant === "blocked" && standLaunchToast.publicUrl?.trim() ? (
                <a
                  className="toast-action"
                  href={standLaunchToast.publicUrl!.trim()}
                  target="_blank"
                  rel="noopener noreferrer"
                  onClick={() => setStandLaunchToast(null)}
                >
                  Перейти к стенду
                </a>
              ) : null}
            </div>
          )}
        </>
      ) : null}

      {account && activePage === "courses" && (
        <CoursesPage
          courses={state.courses}
          selectedModuleId={state.selectedModuleId}
          pinOpenCourseId={coursesOpenCourseId}
          pentestTasks={state.pentestTasks}
          pentestTaskInstances={state.pentestTaskInstances}
          onSelectModule={(moduleId) => {
            setCoursePreviewMode(account.role !== "STUDENT");
            setActivePage("workspace");
            void selectModule(moduleId);
          }}
        />
      )}

      {account && activePage === "profile" && (
        <ProfilePage
          account={account}
          disabled={loading}
          onProfileUpdated={(user) => setAccount(currentUserToAccount(user))}
          onMessage={setMessage}
        />
      )}

      {account && activePage === "workspace" && (account.role === "STUDENT" || coursePreviewMode) && (
        <StudentDashboard
          account={account}
          moduleOptions={moduleOptions}
          selectedModule={selectedModule}
          lessons={state.lessons}
          selectedLesson={state.selectedLesson}
          submissions={state.submissions}
          validationJobs={state.validationJobs}
          reports={state.reports}
          reviews={state.reviews}
          assignments={state.assignments}
          pentestTasks={state.pentestTasks}
          pentestTaskInstances={state.pentestTaskInstances}
          isBusy={loading}
          onSelectModule={selectModule}
          workspaceTaskId={workspaceTaskId}
          onOpenWorkspaceTask={openWorkspaceTask}
          onCloseWorkspaceTask={closeWorkspaceTask}
          onGoToCourses={navigateToCourses}
          onStartTask={async (taskId) => {
            setLoading(true);
            setError(null);
            try {
              csrfTokenCache = null;
              const instance = await apiRequest<PentestTaskInstance>(
                account,
                `/api/pentest-tasks/${taskId}/instances`,
                {
                  method: "POST"
                }
              );
              const loaded = await loadDashboard(account);
              if (!loaded) {
                return false;
              }
              setMessage("Стенд запущен на 1 час.");
              return instance;
            } catch (caught) {
              setError(caught instanceof Error ? caught.message : "Не удалось запустить стенд.");
              return false;
            } finally {
              setLoading(false);
            }
          }}
          onStopTask={(instanceId) =>
            withRefresh(
              () =>
                apiRequest(account, `/api/pentest-task-instances/${instanceId}/stop`, {
                  method: "POST"
                }),
              "Стенд остановлен."
            )
          }
          onSubmitTaskFlag={async (instanceId, flag) => {
            const res = await apiRequest<PentestTaskFlagSubmitResponse>(
              account,
              `/api/pentest-task-instances/${instanceId}/flag`,
              {
                method: "POST",
                body: JSON.stringify({ flag })
              }
            );
            await loadDashboard(account);
            return res;
          }}
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
          onUserStandMessage={setMessage}
          onUserStandError={setError}
          onStandLaunched={setStandLaunchToast}
          readOnlyCourseView={coursePreviewMode}
        />
      )}

      {account?.role === "CURATOR" && activePage === "workspace" && !coursePreviewMode && (
        <CuratorDashboard
          account={account}
          firstModule={labModule}
          submissions={state.submissions}
          validationJobs={state.validationJobs}
          reports={state.reports}
          isBusy={loading}
          onExportGrades={exportGrades}
          onMessage={setMessage}
          onError={setError}
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

      {account?.role === "ADMIN" && activePage === "workspace" && !coursePreviewMode && (
        <AdminDashboard
          account={account}
          onMessage={setMessage}
          onError={setError}
          submissions={state.submissions}
          validationJobs={state.validationJobs}
          reports={state.reports}
          labs={state.labs}
          auditEvents={state.auditEvents}
          courses={state.courses}
          users={state.users}
          pentestTasks={state.pentestTasks}
          isBusy={loading}
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
    ...(account.role === "STUDENT" ? [] : [{ id: "workspace" as const, label: "Рабочая область" }]),
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
          {avatarImageSrc(account) ? (
            <img className="academy-user-avatar" src={avatarImageSrc(account)} alt="" width={36} height={36} />
          ) : (
            <span className="academy-user-avatar academy-user-avatar-fallback" aria-hidden>
              {account.displayName.trim().charAt(0).toUpperCase() || "?"}
            </span>
          )}
          <div className="academy-user-text">
            <span>{account.displayName}</span>
            <small>{roleLabels[account.role]}</small>
          </div>
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
          <button type="button" className="academy-action logout-button" aria-label="Выйти" title="Выйти" disabled={loading} onClick={onLogout}>
            <svg className="logout-icon" width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"
              />
            </svg>
            <span>Выйти</span>
          </button>
        </div>
      </div>
    </header>
  );
}

function CoursesPage({
  courses,
  selectedModuleId,
  pinOpenCourseId,
  pentestTasks,
  pentestTaskInstances,
  onSelectModule
}: {
  courses: Course[];
  selectedModuleId?: string;
  pinOpenCourseId?: string | null;
  pentestTasks: PentestTask[];
  pentestTaskInstances: PentestTaskInstance[];
  onSelectModule: (moduleId: string) => void;
}) {
  const [expandedByCourseId, setExpandedByCourseId] = useState<Record<string, boolean>>({});

  useEffect(() => {
    if (pinOpenCourseId) {
      const valid = courses.some((c) => c.id === pinOpenCourseId);
      if (valid) {
        setExpandedByCourseId(() => {
          const next: Record<string, boolean> = {};
          for (const c of courses) {
            next[c.id] = c.id === pinOpenCourseId;
          }
          return next;
        });
        return;
      }
    }
    setExpandedByCourseId((prev) => {
      const next = { ...prev };
      const validIds = new Set(courses.map((c) => c.id));
      for (const id of Object.keys(next)) {
        if (!validIds.has(id)) {
          delete next[id];
        }
      }
      for (const c of courses) {
        if (next[c.id] === undefined) {
          next[c.id] = true;
        }
      }
      return next;
    });
  }, [courses, pinOpenCourseId]);

  const isCourseExpanded = (courseId: string) => expandedByCourseId[courseId] !== false;

  function toggleCourse(courseId: string) {
    setExpandedByCourseId((prev) => {
      const open = prev[courseId] !== false;
      return { ...prev, [courseId]: !open };
    });
  }

  return (
    <section className="page-section courses-page">
      {courses.length === 0 ? (
        <EmptyState>Курсы пока не загружены.</EmptyState>
      ) : (
        <div className="course-category-list">
          {courses.map((course) => {
            const expanded = isCourseExpanded(course.id);
            return (
              <div
                key={course.id}
                className="course-category"
                data-expanded={expanded ? "true" : "false"}
              >
                <button
                  type="button"
                  className="course-category-trigger"
                  aria-expanded={expanded}
                  aria-controls={`course-modules-${course.id}`}
                  id={`course-head-${course.id}`}
                  onClick={() => toggleCourse(course.id)}
                >
                  <span>{course.title}</span>
                  <span className="course-category-arrow" aria-hidden="true" />
                </button>
                <div
                  className="course-category-body"
                  id={`course-modules-${course.id}`}
                  role="region"
                  aria-labelledby={`course-head-${course.id}`}
                  aria-hidden={!expanded}
                >
                  <div className="course-category-body-inner">
                    <div className="course-card-row">
                      {course.modules.map((module, index) => {
                        const labPr = moduleLabProgress(module.title, pentestTasks, pentestTaskInstances);
                        return (
                        <button
                          key={module.id}
                          type="button"
                          className={module.id === selectedModuleId ? "course-tile active" : "course-tile"}
                          style={{ "--course-tile-i": index } as React.CSSProperties}
                          onClick={() => onSelectModule(module.id)}
                        >
                          <span className="course-tile-cover">
                            <span>{module.title}</span>
                          </span>
                          <span className="course-tile-title">
                            {module.title}
                          </span>
                          <ModuleLabMiniBar completed={labPr.completed} total={labPr.total} />
                        </button>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

function ProfilePage({
  account,
  disabled,
  onProfileUpdated,
  onMessage
}: {
  account: AccountSession;
  disabled: boolean;
  onProfileUpdated: (user: CurrentUser) => void;
  onMessage: (text: string) => void;
}) {
  const [displayName, setDisplayName] = useState(account.displayName);
  const [email, setEmail] = useState(account.email);
  const [avatarVersion, setAvatarVersion] = useState(0);
  const [profileSaving, setProfileSaving] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const avatarSrc = avatarImageSrc(account);
  const previewAvatarUrl = avatarSrc ? `${avatarSrc}${avatarSrc.includes("?") ? "&" : "?"}v=${avatarVersion}` : undefined;

  useEffect(() => {
    setDisplayName(account.displayName);
    setEmail(account.email);
  }, [account.displayName, account.email, account.avatarUrl]);

  async function saveProfile(event: React.FormEvent) {
    event.preventDefault();
    if (disabled || profileSaving) {
      return;
    }
    setProfileSaving(true);
    try {
      const user = await patchMyProfile({
        displayName: displayName.trim(),
        email: email.trim().toLowerCase()
      });
      onProfileUpdated(user);
      onMessage("Профиль сохранён.");
    } catch (caught) {
      window.alert(caught instanceof Error ? caught.message : "Не удалось сохранить профиль.");
    } finally {
      setProfileSaving(false);
    }
  }

  async function savePassword(event: React.FormEvent) {
    event.preventDefault();
    if (disabled || passwordSaving) {
      return;
    }
    if (newPassword !== confirmPassword) {
      window.alert("Новый пароль и подтверждение не совпадают.");
      return;
    }
    setPasswordSaving(true);
    try {
      await changeMyPassword(currentPassword, newPassword);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      onMessage("Пароль изменён.");
    } catch (caught) {
      window.alert(caught instanceof Error ? caught.message : "Не удалось сменить пароль.");
    } finally {
      setPasswordSaving(false);
    }
  }

  async function onAvatarPick(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file || disabled) {
      return;
    }
    try {
      const user = await uploadMyAvatar(file);
      onProfileUpdated(user);
      setAvatarVersion((v) => v + 1);
      onMessage("Аватар обновлён.");
    } catch (caught) {
      window.alert(caught instanceof Error ? caught.message : "Не удалось загрузить файл.");
    }
  }

  return (
    <section className="page-section">
      <header className="page-section-header">
        <h1>Личный кабинет</h1>
      </header>
      <div className="profile-grid">
        <article className="profile-card profile-card-editable">
          <h2>Профиль</h2>
          <div className="profile-avatar-row">
            {previewAvatarUrl ? (
              <img className="profile-avatar-preview" src={previewAvatarUrl} alt="" width={72} height={72} />
            ) : (
              <div className="profile-avatar-placeholder" aria-hidden>
                {account.displayName.trim().charAt(0).toUpperCase() || "?"}
              </div>
            )}
            <label className="profile-avatar-upload">
              <input type="file" accept="image/png,image/jpeg,image/webp" disabled={disabled} onChange={onAvatarPick} />
              <span className="secondary">Загрузить фото</span>
            </label>
          </div>
          <form className="form profile-edit-form" onSubmit={saveProfile}>
            <label htmlFor="profileDisplayName">Отображаемое имя</label>
            <input
              id="profileDisplayName"
              type="text"
              autoComplete="nickname"
              maxLength={160}
              disabled={disabled || profileSaving}
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
            />
            <label htmlFor="profileEmail">Email</label>
            <input
              id="profileEmail"
              type="email"
              autoComplete="email"
              maxLength={LOGIN_EMAIL_MAX_LENGTH}
              disabled={disabled || profileSaving}
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
            <div className="profile-form-actions">
              <button type="submit" disabled={disabled || profileSaving || !displayName.trim() || !email.trim()}>
                {profileSaving ? "Сохранение…" : "Сохранить"}
              </button>
            </div>
          </form>
        </article>
        <article className="profile-card">
          <h3 className="profile-subheading">Смена пароля</h3>
          <form className="form profile-edit-form" onSubmit={savePassword}>
            <label htmlFor="profileCurrentPassword">Текущий пароль</label>
            <input
              id="profileCurrentPassword"
              type="password"
              autoComplete="current-password"
              maxLength={LOGIN_PASSWORD_MAX_LENGTH}
              disabled={disabled || passwordSaving}
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
            />
            <label htmlFor="profileNewPassword">Новый пароль</label>
            <input
              id="profileNewPassword"
              type="password"
              autoComplete="new-password"
              maxLength={LOGIN_PASSWORD_MAX_LENGTH}
              disabled={disabled || passwordSaving}
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
            />
            <label htmlFor="profileConfirmPassword">Подтверждение</label>
            <input
              id="profileConfirmPassword"
              type="password"
              autoComplete="new-password"
              maxLength={LOGIN_PASSWORD_MAX_LENGTH}
              disabled={disabled || passwordSaving}
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
            />
            <div className="profile-form-actions">
              <button
                type="submit"
                className="secondary"
                disabled={disabled || passwordSaving || !currentPassword || !newPassword || !confirmPassword}
              >
                {passwordSaving ? "Смена…" : "Сменить пароль"}
              </button>
            </div>
          </form>
        </article>
      </div>
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

type StudentSectionId = "learning" | "practice" | "peer";

function StudentDashboard({
  account,
  moduleOptions,
  selectedModule,
  lessons,
  selectedLesson,
  submissions,
  validationJobs,
  reports,
  reviews,
  assignments,
  pentestTasks,
  pentestTaskInstances,
  isBusy,
  onSelectModule,
  onStartTask,
  onStopTask,
  workspaceTaskId,
  onOpenWorkspaceTask,
  onCloseWorkspaceTask,
  onGoToCourses,
  onSubmitTaskFlag,
  onCreateSubmission,
  onCreateArchiveSubmission,
  onCreateReport,
  onUserStandMessage,
  onUserStandError,
  onStandLaunched,
  readOnlyCourseView = false
}: {
  account: AccountSession;
  moduleOptions: ModuleOption[];
  selectedModule?: ModuleOption;
  lessons: LessonSummary[];
  selectedLesson?: Lesson;
  submissions: Submission[];
  validationJobs: ValidationJob[];
  reports: Report[];
  reviews: Review[];
  assignments: BlackBoxAssignment[];
  pentestTasks: PentestTask[];
  pentestTaskInstances: PentestTaskInstance[];
  isBusy: boolean;
  onSelectModule: (moduleId: string) => Promise<void>;
  onStartTask: (taskId: string) => Promise<PentestTaskInstance | false>;
  onStopTask: (instanceId: string) => Promise<boolean>;
  workspaceTaskId: string | null;
  onOpenWorkspaceTask: (taskId: string) => void;
  onCloseWorkspaceTask: () => void;
  onGoToCourses: (openCourseId?: string) => void;
  onSubmitTaskFlag: (instanceId: string, flag: string) => Promise<PentestTaskFlagSubmitResponse>;
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
  onUserStandMessage: (message: string) => void;
  onUserStandError: (message: string) => void;
  onStandLaunched: (toast: StandLaunchToast | null) => void;
  readOnlyCourseView?: boolean;
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
  const [expandedTocGroups, setExpandedTocGroups] = useState<Set<string>>(() => new Set());
  const [lessonOutlineVisible, setLessonOutlineVisible] = useState(true);
  const [peerAssignments, setPeerAssignments] = useState<PeerStandAssignment[]>([]);
  const [peerInstances, setPeerInstances] = useState<UserPentestStandInstance[]>([]);
  const [peerBusy, setPeerBusy] = useState(false);

  const reloadPeerData = useCallback(async () => {
    if (!selectedModule || readOnlyCourseView || account.role !== "STUDENT") {
      setPeerAssignments([]);
      setPeerInstances([]);
      return;
    }
    try {
      const [assignments, runningInstances] = await Promise.all([
        apiRequest<PeerStandAssignment[]>(account, `/api/modules/${selectedModule.id}/peer-stands`),
        apiRequest<UserPentestStandInstance[]>(account, "/api/user-stand-instances/my")
      ]);
      setPeerAssignments(assignments);
      setPeerInstances(runningInstances);
    } catch (caught) {
      onUserStandError(caught instanceof Error ? caught.message : "Не удалось загрузить peer-стенды.");
    }
  }, [account, selectedModule?.id, onUserStandError, readOnlyCourseView]);

  useEffect(() => {
    void reloadPeerData();
  }, [reloadPeerData]);

  const handlePeerStart = useCallback(async (standId: string) => {
    setPeerBusy(true);
    try {
      const instance = await apiRequest<UserPentestStandInstance>(
        account,
        `/api/user-stands/${standId}/instances`,
        { method: "POST" }
      );
      const assignment = peerAssignments.find((a) => a.standId === standId);
      const standName =
        instance.standDisplayName || assignment?.standDisplayName || "Стенд другого студента";
      onStandLaunched({ variant: "launched", standName, publicUrl: instance.publicUrl });
      onUserStandMessage(`Стенд «${standName}» запущен. Доступ появится через несколько секунд.`);
      await reloadPeerData();
    } catch (caught) {
      const messageText = caught instanceof Error ? caught.message : "Не удалось запустить стенд.";
      const running = peerInstances.find((inst) => inst.status === "RUNNING");
      if (running && /запущенный/.test(messageText)) {
        onStandLaunched({
          variant: "blocked",
          standName: running.standDisplayName || "Запущенный стенд",
          publicUrl: running.publicUrl
        });
      }
      onUserStandError(messageText);
    } finally {
      setPeerBusy(false);
    }
  }, [account, onStandLaunched, onUserStandError, onUserStandMessage, peerAssignments, peerInstances, reloadPeerData]);

  const handlePeerStop = useCallback(async (instanceId: string) => {
    setPeerBusy(true);
    try {
      await apiRequest(account, `/api/user-stand-instances/${instanceId}/stop`, { method: "POST" });
      onUserStandMessage("Стенд остановлен.");
      await reloadPeerData();
    } catch (caught) {
      onUserStandError(caught instanceof Error ? caught.message : "Не удалось остановить стенд.");
    } finally {
      setPeerBusy(false);
    }
  }, [account, onUserStandError, onUserStandMessage, reloadPeerData]);

  const handlePeerSubmitFlag = useCallback(async (standId: string, flag: string) => {
    setPeerBusy(true);
    try {
      const result = await apiRequest<{ accepted: boolean; solved: boolean; attempts: number; message: string }>(
        account,
        `/api/user-stands/${standId}/flag`,
        {
          method: "POST",
          body: JSON.stringify({ flag })
        }
      );
      if (result.accepted) {
        onUserStandMessage(result.message || "Флаг принят.");
      } else {
        onUserStandError(result.message || "Неверный флаг.");
      }
      await reloadPeerData();
    } catch (caught) {
      onUserStandError(caught instanceof Error ? caught.message : "Не удалось проверить флаг.");
    } finally {
      setPeerBusy(false);
    }
  }, [account, onUserStandError, onUserStandMessage, reloadPeerData]);

  const peerInstanceByStandId = useMemo(() => {
    const map = new Map<string, UserPentestStandInstance>();
    for (const instance of peerInstances) {
      if (instance.status === "RUNNING") {
        map.set(instance.standId, instance);
      }
    }
    return map;
  }, [peerInstances]);
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

  const [activeSection, setActiveSection] = useState<StudentSectionId>(() => {
    if (typeof window === "undefined") {
      return "learning";
    }
    const fromPath = studentSectionFromPathname(window.location.pathname);
    if (fromPath) return fromPath;
    const fromQuery = new URLSearchParams(window.location.search).get("studentSection");
    if (fromQuery === "practice") return "practice";
    if (fromQuery === "peer") return "peer";
    return "learning";
  });
  useEffect(() => {
    if (readOnlyCourseView && activeSection !== "learning") {
      setActiveSection("learning");
    }
  }, [activeSection, readOnlyCourseView]);
  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    const url = new URL(window.location.href);
    url.searchParams.delete("studentSection");
    const slug = selectedModule ? moduleTopicSlug(selectedModule) : null;
    let nextPath: string;
    let nextSearch = url.search;
    if (activeSection === "practice") {
      nextPath = "/my-stand";
      const params = new URLSearchParams(url.search);
      if (slug) {
        params.set("module", slug);
      } else {
        params.delete("module");
      }
      const qs = params.toString();
      nextSearch = qs ? `?${qs}` : "";
    } else if (activeSection === "peer") {
      nextPath = "/peer-tasks";
      const params = new URLSearchParams(url.search);
      if (slug) {
        params.set("module", slug);
      } else {
        params.delete("module");
      }
      const qs = params.toString();
      nextSearch = qs ? `?${qs}` : "";
    } else {
      const basePath = selectedModule ? webSecurityPath(selectedModule) : lessonPathPrefixFromPathname(url.pathname);
      const taskMatch = url.pathname.match(/\/vulnerability\/[^/]+$/);
      const taskSuffix = taskMatch ? taskMatch[0] : "";
      nextPath = `${basePath === "/" ? "" : basePath}${taskSuffix}` || "/";
      const params = new URLSearchParams(url.search);
      params.delete("module");
      const qs = params.toString();
      nextSearch = qs ? `?${qs}` : "";
    }
    window.history.replaceState(null, "", `${nextPath}${nextSearch}${url.hash}`);
  }, [activeSection, selectedModule?.id]);
  const lessonShellMeasureRef = useRef<HTMLDivElement>(null);
  const lessonHeroMeasureRef = useRef<HTMLDivElement>(null);
  const lessonDockRafRef = useRef(0);
  const [lessonDockReady, setLessonDockReady] = useState(false);
  const [lessonContentTopOffset, setLessonContentTopOffset] = useState(0);

  const syncLessonDock = useCallback(() => {
    const el = lessonShellMeasureRef.current;
    if (!el || typeof document === "undefined") {
      return;
    }
    const rect = el.getBoundingClientRect();
    const dockWidth = Math.max(0, Math.round(rect.left));
    const dockTop = Math.max(0, Math.round(rect.top));
    const dockHeight = Math.max(0, Math.round(rect.height));
    document.documentElement.style.setProperty("--lesson-dock-width", `${dockWidth}px`);
    document.documentElement.style.setProperty("--lesson-dock-top", `${dockTop}px`);
    document.documentElement.style.setProperty("--lesson-dock-height", `${dockHeight}px`);
    setLessonContentTopOffset(Math.max(0, Math.round(el.offsetTop)));
    setLessonDockReady(true);
  }, []);

  useLayoutEffect(() => {
    if (activeSection !== "learning") {
      return;
    }
    syncLessonDock();
  }, [activeSection, syncLessonDock, selectedLesson?.id, selectedModule?.id, lessonOutlineVisible]);

  useEffect(() => {
    if (activeSection !== "learning") {
      return undefined;
    }
    const schedule = () => {
      if (lessonDockRafRef.current) {
        cancelAnimationFrame(lessonDockRafRef.current);
      }
      lessonDockRafRef.current = requestAnimationFrame(() => {
        lessonDockRafRef.current = 0;
        syncLessonDock();
      });
    };
    window.addEventListener("resize", schedule, { passive: true });
    window.addEventListener("scroll", schedule, { passive: true });
    window.addEventListener("load", schedule);
    const fonts = (document as Document & { fonts?: { ready?: Promise<unknown> } }).fonts;
    if (fonts?.ready && typeof fonts.ready.then === "function") {
      fonts.ready.then(schedule).catch(() => {});
    }
    const r1 = requestAnimationFrame(() => requestAnimationFrame(schedule));
    const el = lessonShellMeasureRef.current;
    let ro: ResizeObserver | undefined;
    if (el && typeof ResizeObserver !== "undefined") {
      ro = new ResizeObserver(schedule);
      ro.observe(el);
    }
    let bodyRo: ResizeObserver | undefined;
    if (typeof ResizeObserver !== "undefined") {
      bodyRo = new ResizeObserver(schedule);
      bodyRo.observe(document.body);
    }
    schedule();
    return () => {
      window.removeEventListener("resize", schedule);
      window.removeEventListener("scroll", schedule);
      window.removeEventListener("load", schedule);
      cancelAnimationFrame(r1);
      ro?.disconnect();
      bodyRo?.disconnect();
      if (lessonDockRafRef.current) {
        cancelAnimationFrame(lessonDockRafRef.current);
      }
    };
  }, [activeSection, syncLessonDock]);
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
  const moduleTasks = useMemo(
    () => pentestTasks.filter((task) => task.category === selectedModule?.title),
    [pentestTasks, selectedModule?.title]
  );
  const lessonSections = useMemo(
    () => (selectedLesson ? splitMarkdownH2Sections(selectedLesson.contentMarkdown) : []),
    [selectedLesson]
  );
  const focusedTask = workspaceTaskId ? pentestTasks.find((task) => task.id === workspaceTaskId) : undefined;
  const focusedTaskSanitizedDescription = useMemo(
    () => (focusedTask ? sanitizePentestTaskDescriptionMarkdown(focusedTask.descriptionMarkdown) : ""),
    [focusedTask]
  );
  const focusedTaskRunningInstance = useMemo(() => {
    if (!focusedTask) {
      return undefined;
    }
    return pentestTaskInstances.find(
      (instance) => instance.taskId === focusedTask.id && instance.status === "RUNNING"
    );
  }, [focusedTask, pentestTaskInstances]);
  const taskSolvedByTaskId = useMemo(() => {
    const map = new Map<string, boolean>();
    for (const inst of pentestTaskInstances) {
      if (inst.flagSolved) {
        map.set(inst.taskId, true);
      }
    }
    return map;
  }, [pentestTaskInstances]);

  const focusedTaskSolved = focusedTask ? taskSolvedByTaskId.get(focusedTask.id) === true : false;
  const moduleDifficultyProgress = useMemo(() => {
    const buckets = new Map<string, { key: string; label: string; total: number; solved: number }>();
    for (const task of moduleTasks) {
      const key = difficultyUiClass(task.difficulty);
      const label = formatDifficultyLabel(task.difficulty);
      const current = buckets.get(key) ?? { key, label, total: 0, solved: 0 };
      current.total += 1;
      if (taskSolvedByTaskId.get(task.id) === true) {
        current.solved += 1;
      }
      buckets.set(key, current);
    }
    const order = ["easy", "medium", "hard", "unknown"];
    return Array.from(buckets.values()).sort((left, right) => {
      const li = order.indexOf(left.key);
      const ri = order.indexOf(right.key);
      const leftRank = li < 0 ? Number.MAX_SAFE_INTEGER : li;
      const rightRank = ri < 0 ? Number.MAX_SAFE_INTEGER : ri;
      if (leftRank !== rightRank) {
        return leftRank - rightRank;
      }
      return left.label.localeCompare(right.label, "ru");
    });
  }, [moduleTasks, taskSolvedByTaskId]);
  const courseTaskProgress = useMemo(() => {
    if (!selectedModule) {
      return { solved: 0, total: 0, percent: 0 };
    }
    const moduleTitles = new Set(
      moduleOptions.filter((module) => module.courseTitle === selectedModule.courseTitle).map((module) => module.title)
    );
    const courseTasks = pentestTasks.filter((task) => moduleTitles.has(task.category));
    const total = courseTasks.length;
    const solved = courseTasks.reduce((acc, task) => acc + (taskSolvedByTaskId.get(task.id) === true ? 1 : 0), 0);
    const percent = total > 0 ? Math.round((solved / total) * 100) : 0;
    return { solved, total, percent };
  }, [moduleOptions, pentestTasks, selectedModule, taskSolvedByTaskId]);

  const unplacedModuleTasks = useMemo(() => {
    const sectionTitles = new Set(
      lessonSections.map((section) => section.heading.title.trim()).filter((title) => title.length > 0)
    );
    return moduleTasks.filter((task) => {
      const placement = (task.placementAfterHeading ?? "").trim();
      return placement.length === 0 || !sectionTitles.has(placement);
    });
  }, [moduleTasks, lessonSections]);

  useEffect(() => {
  }, [workspaceTaskId, focusedTask?.id, focusedTaskSolved, focusedTaskRunningInstance?.id, focusedTaskRunningInstance?.status]);

  const [flagDraft, setFlagDraft] = useState("");
  const [flagBusy, setFlagBusy] = useState(false);
  const [flagToast, setFlagToast] = useState<{ message: string; variant: "error" | "success" } | null>(null);
  const [celebrateFlag, setCelebrateFlag] = useState(false);

  const confettiPieces = useMemo(
    () =>
      Array.from({ length: 24 }, (_, i) => ({
        id: i,
        left: `${(Math.sin(i * 1.71) * 42 + 48).toFixed(1)}%`,
        delay: `${(i % 10) * 0.05}s`,
        hue: (i * 53) % 360
      })),
    []
  );

  useEffect(() => {
    setFlagDraft("");
    setFlagToast(null);
  }, [workspaceTaskId]);

  useEffect(() => {
    if (!flagToast) {
      return undefined;
    }
    const timerId = window.setTimeout(() => setFlagToast(null), 10_000);
    return () => window.clearTimeout(timerId);
  }, [flagToast]);

  useEffect(() => {
    if (!celebrateFlag) {
      return undefined;
    }
    const timerId = window.setTimeout(() => setCelebrateFlag(false), 3200);
    return () => window.clearTimeout(timerId);
  }, [celebrateFlag]);

  const modulesInSelectedCourse = useMemo(() => {
    if (!selectedModule) {
      return [];
    }
    return moduleOptions.filter((module) => module.courseTitle === selectedModule.courseTitle);
  }, [moduleOptions, selectedModule]);
  const selectedModuleIndex = selectedModule
    ? modulesInSelectedCourse.findIndex((module) => module.id === selectedModule.id)
    : -1;
  const previousModule = selectedModuleIndex > 0 ? modulesInSelectedCourse[selectedModuleIndex - 1] : undefined;
  const nextModule =
    selectedModuleIndex >= 0 && selectedModuleIndex < modulesInSelectedCourse.length - 1
      ? modulesInSelectedCourse[selectedModuleIndex + 1]
      : undefined;

  function goToLessonTitle() {
    window.history.replaceState(null, "", selectedLessonPath);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function openLessonAnchor(anchorId: string) {
    window.history.pushState(null, "", `${selectedLessonPath}#${anchorId}`);
    document.getElementById(anchorId)?.scrollIntoView({ block: "start", behavior: "smooth" });
  }

  function toggleTocGroup(groupId: string) {
    setExpandedTocGroups((current) => {
      const next = new Set(current);
      if (next.has(groupId)) {
        next.delete(groupId);
      } else {
        next.add(groupId);
      }
      return next;
    });
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
    setLessonOutlineVisible(true);
  }, [selectedModule?.id]);

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
    if (activeSection !== "learning") {
      onCloseWorkspaceTask();
    }
  }, [activeSection, onCloseWorkspaceTask]);

  useEffect(() => {
    if (workspaceTaskId) {
      return;
    }
    if (activeSection !== "learning" || !selectedModule || !selectedLesson) {
      return;
    }
    const requestedAnchor = window.location.hash.replace("#", "");
    if (!requestedAnchor || requestedAnchor === "lesson-title") {
      const nextUrl = selectedLessonPath;
      if (`${window.location.pathname}${window.location.hash}` !== nextUrl) {
        window.history.replaceState(null, "", nextUrl);
      }
      return;
    }
    const initialAnchor = selectedLessonHeadings.some((heading) => heading.id === requestedAnchor)
      ? requestedAnchor
      : selectedLessonHeadings[0]?.id;
    const nextUrl = initialAnchor ? `${selectedLessonPath}#${initialAnchor}` : selectedLessonPath;
    if (`${window.location.pathname}${window.location.hash}` !== nextUrl) {
      window.history.replaceState(null, "", nextUrl);
    }
  }, [
    activeSection,
    selectedLesson,
    selectedLessonHeadings,
    selectedLessonPath,
    selectedModule,
    workspaceTaskId
  ]);

  useEffect(() => {
    setExpandedTocGroups(
      new Set(selectedLessonHeadingGroups.filter((group) => group.children.length > 0).map((group) => group.heading.id))
    );
  }, [selectedLessonHeadingGroups]);

  async function submitFocusedFlag(event: React.FormEvent) {
    event.preventDefault();
    if (!focusedTaskRunningInstance || flagBusy) {
      return;
    }
    const trimmed = flagDraft.trim();
    if (!trimmed) {
      return;
    }
    setFlagBusy(true);
    setFlagToast(null);
    try {
      const res = await onSubmitTaskFlag(focusedTaskRunningInstance.id, trimmed);
      if (res.accepted && res.solved) {
        setCelebrateFlag(true);
        setFlagDraft("");
        setFlagToast({
          variant: "success",
          message: (res.message ?? "").trim() || "Флаг принят, лабораторная завершена."
        });
      } else {
        setFlagToast({
          variant: "error",
          message: (res.message ?? "").trim() || "Неверный флаг."
        });
      }
    } catch (caught) {
      setFlagToast({
        variant: "error",
        message: caught instanceof Error ? caught.message : "Не удалось отправить флаг."
      });
    } finally {
      setFlagBusy(false);
    }
  }

  return (
    <section className="section-panel">
      <div className="section-nav-row">
        <nav className="section-nav" aria-label="Разделы студента">
          <button
            type="button"
            disabled={isBusy}
            className={activeSection === "learning" ? "section-nav-button active" : "section-nav-button"}
            onClick={() => setActiveSection("learning")}
          >
            Курс
            <span className="badge-count">{lessons.length}</span>
          </button>
          {!readOnlyCourseView && (
            <>
              <button
                type="button"
                disabled={isBusy}
                className={activeSection === "practice" ? "section-nav-button active" : "section-nav-button"}
                onClick={() => setActiveSection("practice")}
              >
                Мой стенд
              </button>
              <button
                type="button"
                disabled={isBusy}
                className={activeSection === "peer" ? "section-nav-button active" : "section-nav-button"}
                onClick={() => setActiveSection("peer")}
              >
                Задачи студентов
                <span className="badge-count">{peerAssignments.length}</span>
              </button>
            </>
          )}
        </nav>
      </div>

      <div className="grid">
      {activeSection === "learning" && <div className="learning-section wide">
        {!selectedModule ? (
          <EmptyState>Нет активного модуля.</EmptyState>
        ) : focusedTask ? (
          <div className="lesson-task-page">
            <TaskPageBreadcrumbs
              courseTitle={selectedModule.courseTitle}
              moduleTitle={selectedModule.title}
              taskTitle={focusedTask.title}
              taskSlug={focusedTask.slug}
              lessonPath={selectedLessonPath}
              onCourseClick={() => {
                onCloseWorkspaceTask();
                onGoToCourses(selectedModule.courseId);
              }}
              onModuleClick={() => {
                onCloseWorkspaceTask();
                goToLessonTitle();
              }}
              onTaskClick={() => {
                window.history.replaceState(null, "", pentestTaskUrl(selectedLessonPath, focusedTask.slug));
                document.getElementById("task-page-title")?.scrollIntoView({ block: "start", behavior: "smooth" });
              }}
            />
            <div className="lab-task-panel lab-task-panel-minimal" aria-label={focusedTask.title}>
              <h1 className="lesson-title task-page-title" id="task-page-title">
                {focusedTask.title}
              </h1>
              {focusedTaskSanitizedDescription ? (
                <div className="lab-task-card-desc markdown-preview lab-task-desc-only">
                  <MarkdownPreview source={focusedTaskSanitizedDescription} withHeadingIds />
                </div>
              ) : null}
              {focusedTaskRunningInstance ? (
                <form className="lab-flag-form-compact" onSubmit={submitFocusedFlag}>
                  <input
                    type="text"
                    autoComplete="off"
                    spellCheck={false}
                    placeholder={PENTEST_FLAG_PLACEHOLDER}
                    disabled={flagBusy || isBusy}
                    value={flagDraft}
                    onChange={(event) => setFlagDraft(event.target.value)}
                  />
                  <button type="submit" disabled={flagBusy || isBusy || !flagDraft.trim()}>
                    Отправить
                  </button>
                </form>
              ) : null}
              {!readOnlyCourseView && (
                <LabStandToolbar
                  running={focusedTaskRunningInstance ?? null}
                  busy={isBusy}
                  onStart={() => onStartTask(focusedTask.id)}
                  onStop={() => focusedTaskRunningInstance && onStopTask(focusedTaskRunningInstance.id)}
                />
              )}
            </div>
          </div>
        ) : (
          <div className={selectedModule ? "lesson-with-progress-layout" : ""}>
            <div className="lesson-workspace lesson-workspace--with-dock">
              <div ref={lessonHeroMeasureRef} className="lesson-page-hero lesson-focus-column">
                <LessonMaterialBreadcrumbs
                  courseTitle={selectedModule.courseTitle}
                  moduleTitle={selectedModule.title}
                  lessonPath={selectedLessonPath}
                  thirdLabel={selectedLesson ? selectedLesson.title : "Материал"}
                  onCourseClick={() => onGoToCourses(selectedModule.courseId)}
                  onNavigateLessonTitle={goToLessonTitle}
                  onThirdClick={goToLessonTitle}
                />
                {!lessonOutlineVisible ? (
                  <button
                    type="button"
                    className="secondary lesson-outline-reveal"
                    disabled={isBusy}
                    onClick={() => setLessonOutlineVisible(true)}
                  >
                    Показать разделы
                  </button>
                ) : null}
              </div>
              <div ref={lessonShellMeasureRef} className="lesson-shell lesson-shell--solo lesson-focus-column">
                <div className="lesson-main">
                {selectedLesson ? (
                  <>
                  <h1 className="lesson-title" id="lesson-title">
                    {selectedLesson.title}
                  </h1>
                  <article className="lesson-doc lesson-doc-sectioned">
                    {lessonSections.map((section) => (
                      <section key={section.heading.id} className="lesson-doc-section" aria-labelledby={section.heading.id}>
                        <h2 className="lesson-h2" id={section.heading.id}>
                          {section.heading.title}
                        </h2>
                        <MarkdownPreview
                          source={section.body}
                          withHeadingIds
                          sectionH2Id={section.heading.id}
                        />
                        {moduleTasks
                          .filter(
                            (task) =>
                              (task.placementAfterHeading ?? "").trim() === section.heading.title.trim()
                          )
                          .map((task) => (
                            <ModuleTaskLabRow
                              key={task.id}
                              task={task}
                              solved={taskSolvedByTaskId.get(task.id) === true}
                              onOpen={() => onOpenWorkspaceTask(task.id)}
                            />
                          ))}
                      </section>
                    ))}
                    {unplacedModuleTasks.length > 0 && (
                      <section className="lesson-doc-section lesson-doc-section--labs">
                        <h2 className="lesson-h2">Лабораторные работы</h2>
                        {unplacedModuleTasks.map((task) => (
                          <ModuleTaskLabRow
                            key={task.id}
                            task={task}
                            solved={taskSolvedByTaskId.get(task.id) === true}
                            onOpen={() => onOpenWorkspaceTask(task.id)}
                          />
                        ))}
                      </section>
                    )}
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
                  <>
                    <h1 className="lesson-title" id="lesson-title">
                      Материалы пока не опубликованы
                    </h1>
                    <p className="lesson-empty muted">Выберите другой модуль или дождитесь публикации материалов.</p>
                    {moduleTasks.length > 0 && (
                      <article className="lesson-doc lesson-doc-sectioned">
                        <section className="lesson-doc-section lesson-doc-section--labs">
                          <h2 className="lesson-h2">Лабораторные работы</h2>
                          {moduleTasks.map((task) => (
                            <ModuleTaskLabRow
                              key={task.id}
                              task={task}
                              solved={taskSolvedByTaskId.get(task.id) === true}
                              onOpen={() => onOpenWorkspaceTask(task.id)}
                            />
                          ))}
                        </section>
                      </article>
                    )}
                  </>
                )}
                </div>
              </div>
              <aside
                className={`lesson-sidebar lesson-sidebar--sticky lesson-sidebar--dock${lessonOutlineVisible ? " lesson-sidebar--dock-open" : " lesson-sidebar--dock-closed"}`}
                aria-label="Разделы модуля"
                aria-hidden={!lessonOutlineVisible}
                style={lessonDockReady ? undefined : { visibility: "hidden" }}
                onClick={!lessonOutlineVisible ? () => setLessonOutlineVisible(true) : undefined}
                role={!lessonOutlineVisible ? "button" : undefined}
                tabIndex={!lessonOutlineVisible ? 0 : undefined}
                onKeyDown={
                  !lessonOutlineVisible
                    ? (event) => {
                        if (event.key === "Enter" || event.key === " ") {
                          event.preventDefault();
                          setLessonOutlineVisible(true);
                        }
                      }
                    : undefined
                }
              >
                <span className="lesson-sidebar-collapsed-arrow" aria-hidden>
                  ›
                </span>
                <nav className="lesson-sidebar-scrollable" aria-label="Разделы модуля">
                  <div className="lesson-sidebar-header">
                    <p className="lesson-sidebar-title">Разделы модуля</p>
                    <button
                      type="button"
                      className="lesson-sidebar-close"
                      aria-label="Скрыть панель разделов"
                      disabled={isBusy}
                      onClick={() => setLessonOutlineVisible(false)}
                    >
                      ×
                    </button>
                  </div>
                  <div className="lesson-page-toc compact lesson-sidebar-toc-scroll">
                    {selectedLessonHeadings.length === 0 ? (
                      <p className="muted">Разделы появятся после загрузки материала.</p>
                    ) : (
                      selectedLessonHeadingGroups.map((group) => {
                        const hasChildren = group.children.length > 0;
                        const isExpanded = expandedTocGroups.has(group.heading.id);

                        return (
                          <div
                            key={group.heading.id}
                            className={hasChildren ? "toc-tree-group has-children" : "toc-tree-group"}
                            data-open={hasChildren && isExpanded ? "true" : "false"}
                          >
                            <div className="toc-tree-row">
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
                              {hasChildren && (
                                <button
                                  type="button"
                                  className="toc-tree-toggle"
                                  aria-expanded={isExpanded}
                                  aria-label={isExpanded ? "Скрыть подразделы" : "Показать подразделы"}
                                  onClick={() => toggleTocGroup(group.heading.id)}
                                >
                                  <span aria-hidden="true" />
                                </button>
                              )}
                            </div>
                            {hasChildren && (
                              <div className="toc-tree-children">
                                <div>
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
                                </div>
                              </div>
                            )}
                          </div>
                        );
                      })
                    )}
                  </div>
                </nav>
              </aside>
            </div>
            {selectedModule ? (
              <ModuleProgressTracker
                moduleDifficultyProgress={moduleDifficultyProgress}
                courseTaskProgress={courseTaskProgress}
                peerAssignments={peerAssignments}
                topOffset={lessonContentTopOffset}
              />
            ) : null}
          </div>
        )}
      </div>}
      {activeSection === "practice" && (
        <UserPentestStandsCard
          account={account}
          externalBusy={isBusy}
          selectedModule={selectedModule ?? null}
          onMessage={onUserStandMessage}
          onError={onUserStandError}
          onStandLaunched={onStandLaunched}
        />
      )}
      {activeSection === "peer" && (
        <PeerStandsSection
          module={selectedModule}
          assignments={peerAssignments}
          instanceByStandId={peerInstanceByStandId}
          busy={peerBusy || isBusy}
          onStart={handlePeerStart}
          onStop={handlePeerStop}
          onSubmitFlag={handlePeerSubmitFlag}
        />
      )}
      </div>
      {celebrateFlag ? (
        <div className="confetti-overlay" aria-hidden>
          <div className="confetti-burst">
            {confettiPieces.map((piece) => (
              <span
                key={piece.id}
                className="confetti-piece"
                style={
                  {
                    left: piece.left,
                    animationDelay: piece.delay,
                    backgroundColor: `hsl(${piece.hue} 85% 55%)`
                  } as React.CSSProperties
                }
              />
            ))}
          </div>
          <p className="confetti-message">Отлично!</p>
        </div>
      ) : null}
      {flagToast ? (
        <div
          className={`toast toast--uniform toast--dismissible${
            flagToast.variant === "error" ? " error" : " success"
          }`}
          role={flagToast.variant === "error" ? "alert" : "status"}
        >
          <button
            type="button"
            className="toast-dismiss"
            aria-label="Закрыть уведомление"
            onClick={() => setFlagToast(null)}
          >
            ×
          </button>
          <strong>{flagToast.variant === "success" ? "Лабораторная решена" : "Неверный флаг"}</strong>
          <span className="toast-body">{flagToast.message}</span>
        </div>
      ) : null}
    </section>
  );
}

type CuratorSectionId = "validation" | "reports" | "stands";

function CuratorDashboard({
  account,
  firstModule,
  submissions,
  validationJobs,
  reports,
  isBusy,
  onExportGrades,
  onCompleteValidation,
  onCreateReview,
  onMessage,
  onError
}: {
  account: AccountSession;
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
  onMessage: (message: string) => void;
  onError: (message: string) => void;
}) {
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState<ReportType | "ALL">("ALL");
  const [authorFilter, setAuthorFilter] = useState("");
  const [activeSection, setActiveSection] = useState<CuratorSectionId>(() => {
    if (typeof window === "undefined") return "validation";
    const fromUrl = new URLSearchParams(window.location.search).get("curatorSection");
    if (fromUrl === "reports") return "reports";
    if (fromUrl === "stands") return "stands";
    return "validation";
  });
  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    const url = new URL(window.location.href);
    if (activeSection === "validation") {
      url.searchParams.delete("curatorSection");
    } else {
      url.searchParams.set("curatorSection", activeSection);
    }
    window.history.replaceState(null, "", url.pathname + url.search + url.hash);
  }, [activeSection]);
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
        <button
          type="button"
          disabled={isBusy}
          className={activeSection === "stands" ? "section-nav-button active" : "section-nav-button"}
          onClick={() => setActiveSection("stands")}
        >
          Стенды на проверке
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

      {activeSection === "stands" && (
        <StandReviewSection account={account} isBusy={isBusy} onMessage={onMessage} onError={onError} />
      )}
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

type AdminSectionId = "content" | "users" | "streams" | "stands" | "analytics" | "audit";

function AdminDashboard({
  account,
  submissions,
  validationJobs,
  reports,
  labs,
  auditEvents,
  courses,
  users,
  pentestTasks,
  isBusy,
  onCreateCourse,
  onCreateModule,
  onUpsertLesson,
  onMessage,
  onError
}: {
  account: AccountSession;
  submissions: Submission[];
  validationJobs: ValidationJob[];
  reports: Report[];
  labs: Lab[];
  auditEvents: AuditEvent[];
  courses: Course[];
  users: AdminUser[];
  pentestTasks: PentestTask[];
  isBusy: boolean;
  onCreateCourse: (payload: { title: string; description: string }) => Promise<boolean>;
  onCreateModule: (payload: { courseId: string; title: string; vulnerabilityTopic: string }) => Promise<boolean>;
  onUpsertLesson: (payload: { moduleId: string; title: string; contentMarkdown: string; position: number }) => Promise<boolean>;
  onMessage: (message: string) => void;
  onError: (message: string) => void;
}) {
  const [activeSection, setActiveSection] = useState<AdminSectionId>(() => {
    if (typeof window === "undefined") {
      return "content";
    }
    const allowed = new Set<AdminSectionId>(["content", "users", "streams", "stands", "analytics", "audit"]);
    const fromUrl = new URLSearchParams(window.location.search).get("adminSection") as AdminSectionId | null;
    return fromUrl && allowed.has(fromUrl) ? fromUrl : "content";
  });
  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    const url = new URL(window.location.href);
    if (activeSection === "content") {
      url.searchParams.delete("adminSection");
    } else {
      url.searchParams.set("adminSection", activeSection);
    }
    window.history.replaceState(null, "", url.pathname + url.search + url.hash);
  }, [activeSection]);
  const approvedSubmissions = submissions.filter((submission) => submission.status === "APPROVED");
  const labStatusCounts = useMemo(
    () => countByStatus(labs),
    [labs]
  );
  const passedValidationJobs = validationJobs.filter((job) => job.status === "PASSED").length;
  const approvedReports = reports.filter((report) => report.status === "APPROVED").length;

  const sections: Array<{ id: AdminSectionId; label: string; count?: number }> = [
    { id: "content", label: "Курсы", count: courses.reduce((sum, course) => sum + course.modules.length, 0) },
    { id: "users", label: "Пользователи", count: users.length },
    { id: "streams", label: "Потоки" },
    { id: "stands", label: "Стенды на проверке" },
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

      {activeSection === "content" && (
        <AdminContentWorkspace
          account={account}
          courses={courses}
          pentestTasks={pentestTasks}
          isBusy={isBusy}
          onCreateCourse={onCreateCourse}
          onCreateModule={onCreateModule}
          onUpsertLesson={onUpsertLesson}
          onMessage={onMessage}
          onError={onError}
        />
      )}

      {activeSection === "users" && (
        <AdminUsersManager
          account={account}
          isBusy={isBusy}
          onMessage={onMessage}
          onError={onError}
        />
      )}

      {activeSection === "streams" && (
        <AdminStreamsManager
          account={account}
          users={users}
          courses={courses}
          isBusy={isBusy}
          onMessage={onMessage}
          onError={onError}
        />
      )}

      {activeSection === "stands" && (
        <div className="section-panel">
          <header className="section-panel-header">
            <div>
              <h2>Стенды на проверке</h2>
            </div>
          </header>
          <StandReviewSection account={account} isBusy={isBusy} onMessage={onMessage} onError={onError} />
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

type AdminContentWorkspaceProps = {
  account: AccountSession;
  courses: Course[];
  pentestTasks: PentestTask[];
  isBusy: boolean;
  onCreateCourse: (payload: { title: string; description: string }) => Promise<boolean>;
  onCreateModule: (payload: { courseId: string; title: string; vulnerabilityTopic: string }) => Promise<boolean>;
  onUpsertLesson: (payload: { moduleId: string; title: string; contentMarkdown: string; position: number }) => Promise<boolean>;
  onMessage: (msg: string) => void;
  onError: (msg: string) => void;
};

function AdminContentWorkspace({
  account,
  courses,
  pentestTasks,
  isBusy,
  onCreateCourse,
  onCreateModule,
  onUpsertLesson,
  onMessage,
  onError
}: AdminContentWorkspaceProps) {
  const allModules = useMemo(
    () => courses.flatMap((course) => course.modules.map((module) => ({ courseTitle: course.title, courseId: course.id, module }))),
    [courses]
  );
  const [selectedModuleId, setSelectedModuleId] = useState<string>(allModules[0]?.module.id ?? "");
  const [lessonSummaries, setLessonSummaries] = useState<LessonSummary[]>([]);
  const [selectedLessonId, setSelectedLessonId] = useState<string>("");
  const [activeLesson, setActiveLesson] = useState<Lesson | null>(null);
  const [editorBlocks, setEditorBlocks] = useState<LessonBlock[]>(() => parseMarkdownToBlocks(""));
  const [titleDraft, setTitleDraft] = useState("");
  const [saveState, setSaveState] = useState<"idle" | "saving" | "saved">("idle");
  const saveTimerRef = useRef<number | null>(null);
  const [linkedTasks, setLinkedTasks] = useState<PentestTask[]>([]);
  const [attachContext, setAttachContext] = useState<{ heading?: string } | null>(null);
  const [showCourseDialog, setShowCourseDialog] = useState(false);
  const [showModuleDialog, setShowModuleDialog] = useState(false);
  const [showLessonDialog, setShowLessonDialog] = useState(false);

  useEffect(() => {
    if (!selectedModuleId && allModules[0]) {
      setSelectedModuleId(allModules[0].module.id);
    }
  }, [allModules, selectedModuleId]);

  // Load lessons for module
  useEffect(() => {
    if (!selectedModuleId) {
      setLessonSummaries([]);
      return;
    }
    void (async () => {
      try {
        const list = await apiRequest<LessonSummary[]>(account, `/api/modules/${selectedModuleId}/lessons`);
        setLessonSummaries(list);
        if (list.length > 0 && !list.some((l) => l.id === selectedLessonId)) {
          setSelectedLessonId(list[0].id);
        }
      } catch (err) {
        onError(err instanceof Error ? err.message : "Не удалось загрузить уроки");
      }
    })();
  }, [account, onError, selectedLessonId, selectedModuleId]);

  // Load lesson + attached tasks
  useEffect(() => {
    if (!selectedLessonId) {
      setActiveLesson(null);
      setEditorBlocks(parseMarkdownToBlocks(""));
      setLinkedTasks([]);
      return;
    }
    void (async () => {
      try {
        const lesson = await apiRequest<Lesson>(account, `/api/lessons/${selectedLessonId}`);
        setActiveLesson(lesson);
        setTitleDraft(lesson.title);
        setEditorBlocks(parseMarkdownToBlocks(lesson.contentMarkdown ?? ""));
        const tasks = await fetchLessonPentestTasks(account, selectedLessonId);
        setLinkedTasks(tasks);
      } catch (err) {
        onError(err instanceof Error ? err.message : "Не удалось загрузить урок");
      }
    })();
  }, [account, onError, selectedLessonId]);

  const triggerSave = useCallback(
    (blocks: LessonBlock[], title: string) => {
      if (!activeLesson) return;
      if (saveTimerRef.current) {
        window.clearTimeout(saveTimerRef.current);
      }
      setSaveState("saving");
      saveTimerRef.current = window.setTimeout(async () => {
        const markdown = serializeBlocksToMarkdown(blocks);
        const ok = await onUpsertLesson({
          moduleId: activeLesson.moduleId,
          title: title.trim() || activeLesson.title,
          contentMarkdown: markdown,
          position: activeLesson.position
        });
        if (ok) {
          setSaveState("saved");
          window.setTimeout(() => setSaveState("idle"), 1500);
        } else {
          setSaveState("idle");
        }
      }, 600);
    },
    [activeLesson, onUpsertLesson]
  );

  const handleEditorChange = useCallback(
    (next: LessonBlock[]) => {
      setEditorBlocks(next);
      triggerSave(next, titleDraft);
    },
    [titleDraft, triggerSave]
  );

  const handleTitleChange = useCallback(
    (next: string) => {
      setTitleDraft(next);
      triggerSave(editorBlocks, next);
    },
    [editorBlocks, triggerSave]
  );

  const reloadLinkedTasks = useCallback(async () => {
    if (!selectedLessonId) return;
    try {
      const tasks = await fetchLessonPentestTasks(account, selectedLessonId);
      setLinkedTasks(tasks);
    } catch (err) {
      onError(err instanceof Error ? err.message : "Не удалось обновить список задач");
    }
  }, [account, onError, selectedLessonId]);

  const headings = useMemo(
    () => editorBlocks.filter((b) => b.type === "h1" || b.type === "h2" || b.type === "h3"),
    [editorBlocks]
  );

  const tasksByHeading = useMemo(() => {
    const map: Record<string, PentestTask[]> = {};
    linkedTasks.forEach((t) => {
      const key = t.placementAfterHeading ?? "";
      if (!map[key]) map[key] = [];
      map[key].push(t);
    });
    return map;
  }, [linkedTasks]);

  return (
    <div className="lesson-with-progress-layout">
      <article className="card lesson-workspace">
        <header className="lesson-page-hero">
          <div className="lesson-editor-toolbar">
            <select
              value={selectedModuleId}
              onChange={(e) => {
                setSelectedModuleId(e.target.value);
                setSelectedLessonId("");
              }}
              disabled={isBusy}
            >
              {allModules.map((m) => (
                <option key={m.module.id} value={m.module.id}>
                  {m.courseTitle}: {m.module.title}
                </option>
              ))}
            </select>
            <select
              value={selectedLessonId}
              onChange={(e) => setSelectedLessonId(e.target.value)}
              disabled={isBusy || lessonSummaries.length === 0}
            >
              {lessonSummaries.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.title}
                </option>
              ))}
            </select>
            <button type="button" className="secondary" onClick={() => setShowCourseDialog(true)} disabled={isBusy}>
              + курс
            </button>
            <button type="button" className="secondary" onClick={() => setShowModuleDialog(true)} disabled={isBusy}>
              + модуль
            </button>
            <button type="button" className="secondary" onClick={() => setShowLessonDialog(true)} disabled={isBusy || !selectedModuleId}>
              + урок
            </button>
            <span className={`save-status${saveState === "saved" ? " save-status--saved" : ""}`}>
              {saveState === "saving" ? "Сохраняется…" : saveState === "saved" ? "Сохранено" : ""}
            </span>
          </div>
          {activeLesson && (
            <input
              className="lesson-editor-title"
              value={titleDraft}
              onChange={(e) => handleTitleChange(e.target.value)}
              placeholder="Заголовок урока"
              disabled={isBusy}
              style={{ fontSize: 22, fontWeight: 600, marginTop: 12 }}
            />
          )}
        </header>
        <div className="lesson-shell lesson-shell--solo">
          <div className="lesson-main">
            {activeLesson ? (
              <>
                <LessonBlockEditor
                  blocks={editorBlocks}
                  onChange={handleEditorChange}
                  disabled={isBusy}
                  onAttachTaskAfterHeading={(headingText) => setAttachContext({ heading: headingText })}
                />
                {headings.filter((h) => h.type === "h2").map((h) => {
                  const matched = tasksByHeading[h.text] ?? [];
                  if (matched.length === 0) return null;
                  return (
                    <div key={`tasks-after-${h.id}`} style={{ marginTop: 12, padding: 8, border: "1px dashed var(--pep-border)", borderRadius: 6 }}>
                      <p className="muted">Привязанные задачи под «{h.text}»:</p>
                      {matched.map((t) => (
                        <div key={t.id} className="task-list-row">
                          <div>
                            <strong>{t.title}</strong>
                            <span className="muted">
                              {t.category} • {t.difficulty} • {t.sourceKind ?? "SYSTEM_ARCHIVE"}
                              {t.archiveStatus ? ` • ${t.archiveStatus}` : ""}
                            </span>
                          </div>
                          <div className="actions">
                            <button
                              type="button"
                              className="secondary"
                              disabled={isBusy}
                              onClick={async () => {
                                try {
                                  await adminUnlinkTaskFromLesson(account, selectedLessonId, t.id);
                                  await reloadLinkedTasks();
                                  onMessage("Задача отвязана");
                                } catch (err) {
                                  onError(err instanceof Error ? err.message : "Не удалось отвязать");
                                }
                              }}
                            >
                              Отвязать
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  );
                })}
              </>
            ) : (
              <p className="muted">Выберите модуль и урок или создайте новый.</p>
            )}
          </div>
        </div>
      </article>

      {showCourseDialog && (
        <SimpleCreateDialog
          title="Новый курс"
          fields={[
            { id: "title", label: "Название", required: true },
            { id: "description", label: "Описание", required: true, multiline: true }
          ]}
          isBusy={isBusy}
          onClose={() => setShowCourseDialog(false)}
          onSubmit={async (values) =>
            onCreateCourse({ title: values.title.trim(), description: values.description.trim() })
          }
        />
      )}
      {showModuleDialog && (
        <SimpleCreateDialog
          title="Новый модуль"
          fields={[
            { id: "title", label: "Название модуля", required: true },
            { id: "vulnerabilityTopic", label: "Тема уязвимости", required: true }
          ]}
          isBusy={isBusy}
          onClose={() => setShowModuleDialog(false)}
          onSubmit={async (values) => {
            if (!selectedModuleId) {
              onError("Сначала выберите модуль");
              return false;
            }
            const courseEntry = allModules.find((m) => m.module.id === selectedModuleId);
            const courseId = courseEntry?.courseId ?? courses[0]?.id ?? "";
            return onCreateModule({
              courseId,
              title: values.title.trim(),
              vulnerabilityTopic: values.vulnerabilityTopic.trim()
            });
          }}
        />
      )}
      {showLessonDialog && (
        <SimpleCreateDialog
          title="Новый урок"
          fields={[
            { id: "title", label: "Заголовок страницы", required: true },
            { id: "position", label: "Позиция (число)" }
          ]}
          isBusy={isBusy}
          onClose={() => setShowLessonDialog(false)}
          onSubmit={async (values) => {
            const position = Math.max(1, Number(values.position) || (lessonSummaries.length + 1));
            const ok = await onUpsertLesson({
              moduleId: selectedModuleId,
              title: values.title.trim(),
              contentMarkdown: "## Новый раздел\n\nТекст материала в формате Markdown.",
              position
            });
            if (ok) {
              const list = await apiRequest<LessonSummary[]>(account, `/api/modules/${selectedModuleId}/lessons`);
              setLessonSummaries(list);
              const created = list.find((l) => l.title === values.title.trim());
              if (created) setSelectedLessonId(created.id);
            }
            return ok;
          }}
        />
      )}
      {attachContext && selectedLessonId && (
        <AttachTaskDialog
          account={account}
          lessonId={selectedLessonId}
          placementAfterHeading={attachContext.heading}
          pentestTasks={pentestTasks}
          onClose={() => setAttachContext(null)}
          onAttached={reloadLinkedTasks}
          onMessage={onMessage}
          onError={onError}
        />
      )}
    </div>
  );
}

function AdminTasksManager({
  account,
  pentestTasks,
  isBusy,
  onMessage,
  onError,
  onRefresh
}: {
  account: AccountSession;
  pentestTasks: PentestTask[];
  isBusy: boolean;
  onMessage: (msg: string) => void;
  onError: (msg: string) => void;
  onRefresh: () => void;
}) {
  const [busyTaskId, setBusyTaskId] = useState<string | null>(null);
  const handleReplaceArchive = useCallback(
    async (taskId: string, file: File) => {
      setBusyTaskId(taskId);
      try {
        await adminReplacePentestTaskArchive(account, taskId, file);
        onMessage("Архив отправлен на пересборку");
        onRefresh();
      } catch (err) {
        onError(err instanceof Error ? err.message : "Не удалось обновить архив");
      } finally {
        setBusyTaskId(null);
      }
    },
    [account, onError, onMessage, onRefresh]
  );

  const handleUnlink = useCallback(
    async (lessonId: string, taskId: string) => {
      setBusyTaskId(taskId);
      try {
        await adminUnlinkTaskFromLesson(account, lessonId, taskId);
        onMessage("Задача отвязана от урока");
        onRefresh();
      } catch (err) {
        onError(err instanceof Error ? err.message : "Не удалось отвязать задачу");
      } finally {
        setBusyTaskId(null);
      }
    },
    [account, onError, onMessage, onRefresh]
  );

  return (
    <article className="card">
      <header className="section-panel-header">
        <div>
          <h2>Системные задачи</h2>
          <p className="muted">
            Системные задачи управляются внутри платформы, архивные/promote-задачи живут в S3 системного бакета.
          </p>
        </div>
      </header>
      <div style={{ display: "grid", gap: 8 }}>
        {pentestTasks.length === 0 && <p className="muted">Задач пока нет.</p>}
        {pentestTasks.map((task) => (
          <div key={task.id} className="task-list-row" style={{ flexDirection: "column", alignItems: "stretch" }}>
            <div style={{ display: "flex", justifyContent: "space-between", flexWrap: "wrap", gap: 8 }}>
              <div>
                <strong>{task.title}</strong>
                <p className="muted">
                  {task.category} • {task.difficulty} • источник: {task.sourceKind ?? "SYSTEM_ARCHIVE"}
                </p>
                {task.archiveStatus && (
                  <p className="muted">
                    Архив: <StatusBadge value={task.archiveStatus} />
                    {task.archiveFailedStage ? ` • ошибка на стадии ${task.archiveFailedStage}` : ""}
                    {task.archiveSizeBytes ? ` • ${formatBytes(task.archiveSizeBytes)}` : ""}
                  </p>
                )}
                {task.repositoryUrl && <p className="muted mono-wrap">{task.repositoryUrl}</p>}
                {task.runtimeImageReference && <code>{task.runtimeImageReference}</code>}
                {task.lessonId && (
                  <p className="muted">Привязана к уроку {task.lessonId.substring(0, 8)}…</p>
                )}
              </div>
              <div className="actions" style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                <label
                  className={`secondary${busyTaskId === task.id || isBusy ? " disabled" : ""}`}
                  style={{ cursor: "pointer", display: "inline-flex", alignItems: "center", padding: "6px 10px", border: "1px solid var(--pep-border)", borderRadius: 4 }}
                >
                  Загрузить новый архив
                  <input
                    type="file"
                    hidden
                    accept=".zip,.tar,.tar.gz,.tgz"
                    disabled={busyTaskId === task.id || isBusy}
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) void handleReplaceArchive(task.id, file);
                      e.target.value = "";
                    }}
                  />
                </label>
                {task.lessonId && (
                  <button
                    type="button"
                    className="secondary"
                    disabled={busyTaskId === task.id || isBusy}
                    onClick={() => void handleUnlink(task.lessonId!, task.id)}
                  >
                    Отвязать от урока
                  </button>
                )}
                <StatusBadge value={task.buildStatus} />
              </div>
            </div>
          </div>
        ))}
      </div>
    </article>
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

type LessonBlockEditorProps = {
  blocks: LessonBlock[];
  onChange: (blocks: LessonBlock[]) => void;
  disabled?: boolean;
  onAttachTaskAfterHeading?: (headingText: string, blockId: string) => void;
};

function LessonBlockEditor({ blocks, onChange, disabled, onAttachTaskAfterHeading }: LessonBlockEditorProps) {
  const [popoverIndex, setPopoverIndex] = useState<number | null>(null);

  const updateBlock = useCallback(
    (index: number, patch: Partial<LessonBlock>) => {
      onChange(blocks.map((block, i) => (i === index ? { ...block, ...patch } : block)));
    },
    [blocks, onChange]
  );

  const insertBlock = useCallback(
    (index: number, type: LessonBlockType) => {
      const next: LessonBlock = { id: newBlockId(), type, text: "" };
      const result = [...blocks.slice(0, index), next, ...blocks.slice(index)];
      onChange(result);
      setPopoverIndex(null);
    },
    [blocks, onChange]
  );

  const removeBlock = useCallback(
    (index: number) => {
      if (blocks.length <= 1) {
        onChange([{ id: newBlockId(), type: "p", text: "" }]);
        return;
      }
      onChange(blocks.filter((_, i) => i !== index));
    },
    [blocks, onChange]
  );

  const transformShortcut = useCallback(
    (index: number, raw: string): boolean => {
      const trimmedStart = raw.replace(/^\s+/, "");
      const headingMatch = /^(#{1,3})\s+(.*)$/.exec(trimmedStart);
      if (headingMatch) {
        const level = headingMatch[1].length as 1 | 2 | 3;
        updateBlock(index, { type: (`h${level}` as LessonBlockType), text: headingMatch[2] });
        return true;
      }
      if (/^>\s+(.*)$/.test(trimmedStart)) {
        const value = trimmedStart.replace(/^>\s+/, "");
        updateBlock(index, { type: "quote", text: value });
        return true;
      }
      if (/^[-*]\s+(.*)$/.test(trimmedStart)) {
        const value = trimmedStart.replace(/^[-*]\s+/, "");
        updateBlock(index, { type: "ul", text: value });
        return true;
      }
      if (/^\d+\.\s+(.*)$/.test(trimmedStart)) {
        const value = trimmedStart.replace(/^\d+\.\s+/, "");
        updateBlock(index, { type: "ol", text: value });
        return true;
      }
      if (/^```/.test(trimmedStart)) {
        updateBlock(index, { type: "code", text: trimmedStart.replace(/^```\s*/, "") });
        return true;
      }
      return false;
    },
    [updateBlock]
  );

  return (
    <div className="lesson-block-editor">
      {blocks.map((block, index) => (
        <React.Fragment key={block.id}>
          <BlockSeparator
            disabled={!!disabled}
            isOpen={popoverIndex === index}
            onToggle={() => setPopoverIndex(popoverIndex === index ? null : index)}
            onInsert={(type) => insertBlock(index, type)}
          />
          <BlockRow
            block={block}
            index={index}
            disabled={!!disabled}
            onUpdate={updateBlock}
            onTransformShortcut={transformShortcut}
            onSplit={(splitText) => {
              const next: LessonBlock = { id: newBlockId(), type: "p", text: splitText };
              const result = [...blocks.slice(0, index + 1), next, ...blocks.slice(index + 1)];
              onChange(result);
            }}
            onMerge={() => removeBlock(index)}
            onAttachTask={
              block.type === "h2" && onAttachTaskAfterHeading
                ? () => onAttachTaskAfterHeading(block.text, block.id)
                : undefined
            }
          />
        </React.Fragment>
      ))}
      <BlockSeparator
        disabled={!!disabled}
        isOpen={popoverIndex === blocks.length}
        onToggle={() => setPopoverIndex(popoverIndex === blocks.length ? null : blocks.length)}
        onInsert={(type) => insertBlock(blocks.length, type)}
      />
    </div>
  );
}

const BLOCK_PALETTE: Array<{ type: LessonBlockType; label: string }> = [
  { type: "p", label: "Параграф" },
  { type: "h1", label: "Заголовок H1" },
  { type: "h2", label: "Заголовок H2" },
  { type: "h3", label: "Заголовок H3" },
  { type: "ul", label: "Список" },
  { type: "ol", label: "Нумерованный список" },
  { type: "quote", label: "Цитата" },
  { type: "code", label: "Код" },
  { type: "table", label: "Таблица" }
];

function BlockSeparator({
  disabled,
  isOpen,
  onToggle,
  onInsert
}: {
  disabled: boolean;
  isOpen: boolean;
  onToggle: () => void;
  onInsert: (type: LessonBlockType) => void;
}) {
  if (disabled) {
    return null;
  }
  return (
    <div className={`block-separator${isOpen ? " block-separator--open" : ""}`}>
      <button type="button" className="block-separator-btn" onClick={onToggle} aria-label="Добавить блок">
        +
      </button>
      {isOpen && (
        <div className="block-separator-popover">
          {BLOCK_PALETTE.map((item) => (
            <button key={item.type} type="button" onClick={() => onInsert(item.type)}>
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function BlockRow({
  block,
  index,
  disabled,
  onUpdate,
  onTransformShortcut,
  onSplit,
  onMerge,
  onAttachTask
}: {
  block: LessonBlock;
  index: number;
  disabled: boolean;
  onUpdate: (index: number, patch: Partial<LessonBlock>) => void;
  onTransformShortcut: (index: number, raw: string) => boolean;
  onSplit: (splitText: string) => void;
  onMerge: () => void;
  onAttachTask?: () => void;
}) {
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) {
      return;
    }
    if (el.textContent !== block.text) {
      el.textContent = block.text;
    }
  }, [block.id, block.type, block.text]);

  const handleInput = useCallback(() => {
    const el = ref.current;
    if (!el) {
      return;
    }
    const value = el.innerText.replace(/\u00a0/g, " ");
    if (block.type === "p" && onTransformShortcut(index, value)) {
      return;
    }
    onUpdate(index, { text: value });
  }, [block.type, index, onTransformShortcut, onUpdate]);

  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLDivElement>) => {
      if (block.type === "code") {
        return; // в код-блоке Enter переносит строку как обычно
      }
      if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        const el = ref.current;
        if (!el) {
          return;
        }
        const selection = window.getSelection();
        if (!selection || selection.rangeCount === 0) {
          onSplit("");
          return;
        }
        const range = selection.getRangeAt(0);
        const before = range.startOffset;
        const text = el.innerText;
        const head = text.slice(0, before);
        const tail = text.slice(before);
        onUpdate(index, { text: head });
        onSplit(tail);
        return;
      }
      if (event.key === "Backspace") {
        const el = ref.current;
        if (el && el.innerText === "") {
          event.preventDefault();
          onMerge();
        }
      }
    },
    [block.type, index, onMerge, onSplit, onUpdate]
  );

  const className = `lesson-block lesson-block--${block.type}`;
  const placeholderByType: Record<LessonBlockType, string> = {
    p: "Параграф…",
    h1: "Заголовок H1",
    h2: "Заголовок H2",
    h3: "Заголовок H3",
    code: "Код…",
    ul: "Элементы списка через перенос строки",
    ol: "Элементы списка через перенос строки",
    quote: "Цитата…",
    table: "| Заголовок | … |\n| --- | --- |\n| ячейка | … |"
  };

  const renderEditable = (tag: string, extraClass = "") => {
    const Tag: any = tag;
    return (
      <Tag
        ref={ref as any}
        contentEditable={!disabled}
        suppressContentEditableWarning
        onInput={handleInput}
        onKeyDown={handleKeyDown}
        className={`block-editable ${extraClass}`.trim()}
        data-placeholder={placeholderByType[block.type]}
      />
    );
  };

  if (block.type === "h1" || block.type === "h2" || block.type === "h3") {
    return (
      <div className={className}>
        {renderEditable(block.type)}
        {block.type === "h2" && onAttachTask && !disabled && (
          <button type="button" className="block-attach-btn" onClick={onAttachTask}>
            + задача
          </button>
        )}
      </div>
    );
  }
  if (block.type === "code") {
    return (
      <pre className={className}>
        {renderEditable("code", "block-code")}
      </pre>
    );
  }
  if (block.type === "ul" || block.type === "ol") {
    return (
      <div className={className}>
        {renderEditable("div", "block-list")}
      </div>
    );
  }
  if (block.type === "quote") {
    return <blockquote className={className}>{renderEditable("div")}</blockquote>;
  }
  if (block.type === "table") {
    return (
      <div className={className}>
        {renderEditable("pre", "block-table-source")}
      </div>
    );
  }
  return <p className={className}>{renderEditable("span")}</p>;
}

function ModalShell({
  title,
  onClose,
  children,
  actions
}: {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
  actions?: React.ReactNode;
}) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [onClose]);
  return (
    <div className="modal-overlay" role="dialog" aria-modal="true" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <h3>{title}</h3>
        {children}
        <div className="modal-actions">{actions}</div>
      </div>
    </div>
  );
}

function SimpleCreateDialog({
  title,
  fields,
  onSubmit,
  onClose,
  isBusy
}: {
  title: string;
  fields: Array<{ id: string; label: string; required?: boolean; multiline?: boolean }>;
  onSubmit: (values: Record<string, string>) => Promise<boolean>;
  onClose: () => void;
  isBusy: boolean;
}) {
  const [values, setValues] = useState<Record<string, string>>(() => {
    const init: Record<string, string> = {};
    fields.forEach((f) => {
      init[f.id] = "";
    });
    return init;
  });
  const submit = async () => {
    if (fields.some((f) => f.required && !values[f.id]?.trim())) {
      return;
    }
    const ok = await onSubmit(values);
    if (ok) {
      onClose();
    }
  };
  return (
    <ModalShell
      title={title}
      onClose={onClose}
      actions={
        <>
          <button type="button" className="secondary" onClick={onClose} disabled={isBusy}>
            Отмена
          </button>
          <button type="button" onClick={() => void submit()} disabled={isBusy}>
            Создать
          </button>
        </>
      }
    >
      <form
        className="form compact-form"
        onSubmit={(e) => {
          e.preventDefault();
          void submit();
        }}
      >
        {fields.map((f) => (
          <React.Fragment key={f.id}>
            <label htmlFor={`simple-create-${f.id}`}>{f.label}</label>
            {f.multiline ? (
              <textarea
                id={`simple-create-${f.id}`}
                rows={4}
                disabled={isBusy}
                value={values[f.id] ?? ""}
                onChange={(e) => setValues({ ...values, [f.id]: e.target.value })}
              />
            ) : (
              <input
                id={`simple-create-${f.id}`}
                disabled={isBusy}
                value={values[f.id] ?? ""}
                onChange={(e) => setValues({ ...values, [f.id]: e.target.value })}
              />
            )}
          </React.Fragment>
        ))}
      </form>
    </ModalShell>
  );
}

type AttachTaskDialogProps = {
  account: AccountSession;
  lessonId: string;
  placementAfterHeading?: string;
  pentestTasks: PentestTask[];
  onClose: () => void;
  onAttached: () => Promise<void> | void;
  onMessage: (msg: string) => void;
  onError: (msg: string) => void;
};

function AttachTaskDialog({
  account,
  lessonId,
  placementAfterHeading,
  pentestTasks,
  onClose,
  onAttached,
  onMessage,
  onError
}: AttachTaskDialogProps) {
  const [tab, setTab] = useState<"existing" | "upload" | "promote">("existing");
  const [filter, setFilter] = useState("");
  const [busy, setBusy] = useState(false);
  const filtered = pentestTasks.filter(
    (t) => t.title.toLowerCase().includes(filter.toLowerCase()) || t.category.toLowerCase().includes(filter.toLowerCase())
  );

  // upload tab
  const [uploadArchive, setUploadArchive] = useState<File | null>(null);
  const [uploadDragging, setUploadDragging] = useState(false);
  const [uploadTitle, setUploadTitle] = useState("");
  const [uploadCategory, setUploadCategory] = useState("");
  const [uploadDifficulty, setUploadDifficulty] = useState("EASY");
  const [uploadDuration, setUploadDuration] = useState(60);
  const [uploadFlag, setUploadFlag] = useState("");
  const [uploadDescription, setUploadDescription] = useState("");

  // promote tab
  const [stands, setStands] = useState<UserPentestStand[]>([]);
  const [standsLoaded, setStandsLoaded] = useState(false);
  const [selectedStand, setSelectedStand] = useState<string>("");
  const [promoteTitle, setPromoteTitle] = useState("");
  const [promoteCategory, setPromoteCategory] = useState("");
  const [promoteDifficulty, setPromoteDifficulty] = useState("EASY");
  const [promoteDuration, setPromoteDuration] = useState(60);

  useEffect(() => {
    if (tab !== "promote" || standsLoaded) {
      return;
    }
    void (async () => {
      try {
        const list = await fetchMyUserStands(account);
        setStands(list.filter((s) => s.status === "READY"));
        setStandsLoaded(true);
      } catch (err) {
        onError(err instanceof Error ? err.message : "Не удалось загрузить стенды");
      }
    })();
  }, [tab, standsLoaded, account, onError]);

  const attachExisting = async (taskId: string) => {
    setBusy(true);
    try {
      await adminLinkTaskToLesson(account, lessonId, taskId, { placementAfterHeading });
      onMessage("Задача привязана");
      await onAttached();
      onClose();
    } catch (err) {
      onError(err instanceof Error ? err.message : "Не удалось привязать задачу");
    } finally {
      setBusy(false);
    }
  };

  const submitUpload = async () => {
    if (!uploadArchive || !uploadTitle.trim() || !uploadCategory.trim()) {
      onError("Заполните название, категорию и архив");
      return;
    }
    setBusy(true);
    try {
      await adminCreatePentestTaskFromArchive(account, uploadArchive, {
        title: uploadTitle.trim(),
        category: uploadCategory.trim(),
        difficulty: uploadDifficulty,
        durationMinutes: uploadDuration,
        descriptionMarkdown: uploadDescription || undefined,
        flag: uploadFlag.trim(),
        lessonId,
        placementAfterHeading
      });
      onMessage("Задача загружена и поставлена в очередь сборки");
      await onAttached();
      onClose();
    } catch (err) {
      onError(err instanceof Error ? err.message : "Не удалось загрузить архив");
    } finally {
      setBusy(false);
    }
  };

  const submitPromote = async () => {
    if (!selectedStand || !promoteTitle.trim() || !promoteCategory.trim()) {
      onError("Заполните название, категорию и выберите стенд");
      return;
    }
    setBusy(true);
    try {
      await adminPromoteStandToTask(account, selectedStand, {
        title: promoteTitle.trim(),
        category: promoteCategory.trim(),
        difficulty: promoteDifficulty,
        durationMinutes: promoteDuration,
        lessonId,
        placementAfterHeading
      });
      onMessage("Стенд преобразован в системную задачу");
      await onAttached();
      onClose();
    } catch (err) {
      onError(err instanceof Error ? err.message : "Не удалось выполнить promote");
    } finally {
      setBusy(false);
    }
  };

  return (
    <ModalShell
      title={`Привязать задачу${placementAfterHeading ? ` после «${placementAfterHeading}»` : ""}`}
      onClose={onClose}
      actions={
        <button type="button" className="secondary" onClick={onClose}>
          Закрыть
        </button>
      }
    >
      <div className="tab-bar">
        <button
          type="button"
          className={tab === "existing" ? "tab-bar--active" : ""}
          onClick={() => setTab("existing")}
        >
          Существующие
        </button>
        <button
          type="button"
          className={tab === "upload" ? "tab-bar--active" : ""}
          onClick={() => setTab("upload")}
        >
          Новый архив
        </button>
        <button
          type="button"
          className={tab === "promote" ? "tab-bar--active" : ""}
          onClick={() => setTab("promote")}
        >
          Из моих стендов
        </button>
      </div>

      {tab === "existing" && (
        <div>
          <input
            aria-label="Поиск"
            placeholder=""
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            disabled={busy}
          />
          <div style={{ marginTop: 12, maxHeight: 360, overflow: "auto" }}>
            {filtered.length === 0 && <p className="muted">Нет подходящих задач.</p>}
            {filtered.map((task) => (
              <div key={task.id} className="task-list-row">
                <div>
                  <strong>{task.title}</strong>
                  <span className="muted">
                    {task.category} • {task.difficulty} • {task.sourceKind ?? "SYSTEM_ARCHIVE"}
                  </span>
                </div>
                <div className="actions">
                  <button type="button" disabled={busy} onClick={() => void attachExisting(task.id)}>
                    Привязать
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {tab === "upload" && (
        <form className="form compact-form" onSubmit={(e) => { e.preventDefault(); void submitUpload(); }}>
          <label>Архив (.zip / .tar / .tar.gz)</label>
          <div
            className={`stand-dropzone${uploadDragging ? " stand-dropzone--active" : ""}${uploadArchive ? " stand-dropzone--filled" : ""}`}
            onDragOver={(e) => { e.preventDefault(); setUploadDragging(true); }}
            onDragLeave={() => setUploadDragging(false)}
            onDrop={(e) => {
              e.preventDefault();
              setUploadDragging(false);
              const file = e.dataTransfer.files?.[0];
              if (file) setUploadArchive(file);
            }}
            onClick={() => {
              const input = document.getElementById("attach-archive-input") as HTMLInputElement | null;
              input?.click();
            }}
          >
            <input
              id="attach-archive-input"
              type="file"
              hidden
              accept=".zip,.tar,.tar.gz,.tgz"
              onChange={(e) => {
                const file = e.target.files?.[0] ?? null;
                if (file) setUploadArchive(file);
              }}
            />
            <div className="stand-dropzone-content">
              <strong>{uploadArchive ? uploadArchive.name : "Перетащите архив сюда"}</strong>
              <span className="muted">{uploadArchive ? formatBytes(uploadArchive.size) : "или нажмите, чтобы выбрать"}</span>
            </div>
          </div>
          <label>Название</label>
          <input value={uploadTitle} onChange={(e) => setUploadTitle(e.target.value)} disabled={busy} />
          <label>Категория</label>
          <input value={uploadCategory} onChange={(e) => setUploadCategory(e.target.value)} disabled={busy} />
          <label>Сложность</label>
          <select value={uploadDifficulty} onChange={(e) => setUploadDifficulty(e.target.value)} disabled={busy}>
            <option value="EASY">EASY</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HARD">HARD</option>
          </select>
          <label>Длительность, минут</label>
          <input
            type="number"
            min={1}
            max={720}
            value={uploadDuration}
            onChange={(e) => setUploadDuration(Math.max(1, Number(e.target.value) || 60))}
            disabled={busy}
          />
          <label>Флаг (формат pep&#123;[A-Za-z0-9]&#123;20&#125;&#125;)</label>
          <input
            value={uploadFlag}
            onChange={(e) => setUploadFlag(e.target.value)}
            placeholder={PENTEST_FLAG_PLACEHOLDER}
            disabled={busy}
          />
          <label>Описание (Markdown)</label>
          <textarea
            rows={4}
            value={uploadDescription}
            onChange={(e) => setUploadDescription(e.target.value)}
            disabled={busy}
          />
          <button type="submit" disabled={busy || !uploadArchive}>
            Загрузить и привязать
          </button>
        </form>
      )}

      {tab === "promote" && (
        <form className="form compact-form" onSubmit={(e) => { e.preventDefault(); void submitPromote(); }}>
          <label>Свой стенд</label>
          <select value={selectedStand} onChange={(e) => setSelectedStand(e.target.value)} disabled={busy}>
            <option value="">— выберите —</option>
            {stands.map((s) => (
              <option key={s.id} value={s.id}>
                {s.displayName} ({s.originalFilename})
              </option>
            ))}
          </select>
          <label>Название задачи</label>
          <input value={promoteTitle} onChange={(e) => setPromoteTitle(e.target.value)} disabled={busy} />
          <label>Категория</label>
          <input value={promoteCategory} onChange={(e) => setPromoteCategory(e.target.value)} disabled={busy} />
          <label>Сложность</label>
          <select value={promoteDifficulty} onChange={(e) => setPromoteDifficulty(e.target.value)} disabled={busy}>
            <option value="EASY">EASY</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HARD">HARD</option>
          </select>
          <label>Длительность, минут</label>
          <input
            type="number"
            min={1}
            max={720}
            value={promoteDuration}
            onChange={(e) => setPromoteDuration(Math.max(1, Number(e.target.value) || 60))}
            disabled={busy}
          />
          <button type="submit" disabled={busy || !selectedStand}>
            Promote и привязать
          </button>
        </form>
      )}
    </ModalShell>
  );
}

function AdminUsersManager({
  account,
  isBusy,
  onMessage,
  onError
}: {
  account: AccountSession;
  isBusy: boolean;
  onMessage: (message: string) => void;
  onError: (message: string) => void;
}) {
  const [usersPage, setUsersPage] = useState<PageResponse<AdminUser>>({
    items: [],
    page: 0,
    size: 10,
    totalItems: 0,
    totalPages: 0
  });
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [bulkMode, setBulkMode] = useState<"delete" | "disable" | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [createOpen, setCreateOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<AdminUser | null>(null);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [role, setRole] = useState<Role>("STUDENT");
  const [editEmail, setEditEmail] = useState("");
  const [editDisplayName, setEditDisplayName] = useState("");
  const [editRole, setEditRole] = useState<Role>("STUDENT");
  const [editStatus, setEditStatus] = useState("ACTIVE");

  const loadUsers = useCallback(async () => {
    setLoadingUsers(true);
    try {
      const params = new URLSearchParams({
        page: String(page),
        size: "10"
      });
      if (query.trim()) {
        params.set("q", query.trim());
      }
      const data = await apiRequest<PageResponse<AdminUser>>(account, `/api/admin/users?${params.toString()}`);
      setUsersPage(data);
      setSelectedIds(new Set());
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось загрузить пользователей");
    } finally {
      setLoadingUsers(false);
    }
  }, [account, page, query, onError]);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  useEffect(() => {
    setPage(0);
  }, [query]);

  const openEdit = (user: AdminUser) => {
    setEditingUser(user);
    setEditEmail(user.email);
    setEditDisplayName(user.displayName);
    setEditRole(user.role);
    setEditStatus(user.status);
  };

  const toggleSelected = (userId: string) => {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (next.has(userId)) {
        next.delete(userId);
      } else {
        next.add(userId);
      }
      return next;
    });
  };

  const closeBulkMode = () => {
    setBulkMode(null);
    setSelectedIds(new Set());
  };

  const handleBulkAction = async () => {
    if (!bulkMode || selectedIds.size === 0) {
      closeBulkMode();
      return;
    }
    try {
      await apiRequest(account, `/api/admin/users/${bulkMode === "delete" ? "delete" : "disable"}`, {
        method: "POST",
        body: JSON.stringify({ userIds: Array.from(selectedIds) })
      });
      onMessage(bulkMode === "delete" ? "Пользователи удалены." : "Пользователи отключены.");
      closeBulkMode();
      await loadUsers();
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось выполнить действие");
    }
  };

  return (
    <article className="card admin-list-card">
      <header className="section-panel-header admin-list-header">
        <div>
          <h2>Пользователи</h2>
        </div>
        <div className="admin-menu-wrap">
          <button type="button" className="ghost-menu-button" onClick={() => setMenuOpen((open) => !open)} aria-label="Действия с пользователями">
            <span aria-hidden="true">{"\u22EE"}</span>
          </button>
          {menuOpen && (
            <div className="admin-action-menu">
              <button type="button" className="admin-action-menu-item" onClick={() => { setCreateOpen(true); setMenuOpen(false); }}>
                <span className="admin-action-menu__icon" aria-hidden="true">
                  <ActionIcon name="add" />
                </span>
                <span>Добавить</span>
              </button>
              <button type="button" className="admin-action-menu-item" onClick={() => { setBulkMode("delete"); setMenuOpen(false); }}>
                <span className="admin-action-menu__icon" aria-hidden="true">
                  <ActionIcon name="delete" />
                </span>
                <span>Удалить</span>
              </button>
              <button type="button" className="admin-action-menu-item" onClick={() => { setBulkMode("disable"); setMenuOpen(false); }}>
                <span className="admin-action-menu__icon" aria-hidden="true">
                  <ActionIcon name="disable" />
                </span>
                <span>Отключить</span>
              </button>
            </div>
          )}
        </div>
      </header>
      <div className="admin-list-toolbar">
        <input
          aria-label="Поиск"
          placeholder=""
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
      </div>
      <div className="admin-flat-list">
        {usersPage.items.length === 0 ? (
          <EmptyState>{loadingUsers ? "Загрузка пользователей..." : "Пользователи не найдены."}</EmptyState>
        ) : (
          usersPage.items.map((user) => (
            <div key={user.id} className="admin-flat-row admin-user-row">
              <div className="admin-row-main">
                <strong>{user.displayName}</strong>
                <span className="muted mono-wrap">{user.email}</span>
              </div>
              <div className="admin-row-meta">
                <StatusBadge value={roleLabels[user.role]} />
                <StatusBadge value={user.status} />
              </div>
              <button type="button" className="row-edit-button" aria-label="Редактировать пользователя" onClick={() => openEdit(user)}>
                <ActionIcon name="edit" />
              </button>
              {bulkMode && (
                <input
                  className="row-select-checkbox"
                  type="checkbox"
                  aria-label={`Выбрать ${user.displayName}`}
                  checked={selectedIds.has(user.id)}
                  onChange={() => toggleSelected(user.id)}
                />
              )}
            </div>
          ))
        )}
      </div>
      <div className="pagination-row">
        <button
          type="button"
          className="secondary pagination-nav"
          disabled={page === 0 || isBusy || loadingUsers}
          aria-label="Предыдущая страница"
          onClick={() => setPage((p) => Math.max(0, p - 1))}
        >
          {"<"}
        </button>
        <span className="muted">Страница {usersPage.totalPages === 0 ? 0 : usersPage.page + 1} из {usersPage.totalPages}</span>
        <button
          type="button"
          className="secondary pagination-nav"
          disabled={page + 1 >= usersPage.totalPages || isBusy || loadingUsers}
          aria-label="Следующая страница"
          onClick={() => setPage((p) => p + 1)}
        >
          {">"}
        </button>
      </div>
      {bulkMode && (
        <div className="bulk-action-bar">
          <span>{selectedIds.size > 0 ? `Выбрано: ${selectedIds.size}` : "Ничего не выбрано"}</span>
          <button type="button" disabled={isBusy || loadingUsers} onClick={() => void handleBulkAction()}>
            {selectedIds.size === 0 ? "Отменить" : bulkMode === "delete" ? "Удалить" : "Отключить"}
          </button>
        </div>
      )}
      {createOpen && (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <form
            className="modal-card form compact-form"
            onSubmit={async (event) => {
              event.preventDefault();
              try {
                await apiRequest(account, "/api/admin/users", {
                  method: "POST",
                  body: JSON.stringify({ email: email.trim(), password, displayName: displayName.trim(), role })
                });
                setEmail("");
                setPassword("");
                setDisplayName("");
                setRole("STUDENT");
                setCreateOpen(false);
                onMessage("Пользователь создан.");
                await loadUsers();
              } catch (caught) {
                onError(caught instanceof Error ? caught.message : "Не удалось создать пользователя");
              }
            }}
          >
            <h2>Добавить пользователя</h2>
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
            <div className="actions">
              <button type="submit" disabled={isBusy || !email.trim() || !password || !displayName.trim()}>Добавить</button>
              <button type="button" className="secondary" onClick={() => setCreateOpen(false)}>Отмена</button>
            </div>
          </form>
        </div>
      )}
      {editingUser && (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <form
            className="modal-card form compact-form"
            onSubmit={async (event) => {
              event.preventDefault();
              try {
                await apiRequest(account, `/api/admin/users/${editingUser.id}`, {
                  method: "PATCH",
                  body: JSON.stringify({ email: editEmail.trim(), displayName: editDisplayName.trim(), role: editRole, status: editStatus })
                });
                setEditingUser(null);
                onMessage("Пользователь обновлён.");
                await loadUsers();
              } catch (caught) {
                onError(caught instanceof Error ? caught.message : "Не удалось обновить пользователя");
              }
            }}
          >
            <h2>Редактировать пользователя</h2>
            <label htmlFor="editUserEmail">Email</label>
            <input id="editUserEmail" type="email" value={editEmail} onChange={(event) => setEditEmail(event.target.value)} />
            <label htmlFor="editUserName">Имя</label>
            <input id="editUserName" value={editDisplayName} onChange={(event) => setEditDisplayName(event.target.value)} />
            <label htmlFor="editUserRole">Роль</label>
            <select id="editUserRole" value={editRole} onChange={(event) => setEditRole(event.target.value as Role)}>
              <option value="STUDENT">Студент</option>
              <option value="CURATOR">Куратор</option>
              <option value="ADMIN">Администратор</option>
            </select>
            <label htmlFor="editUserStatus">Статус</label>
            <select id="editUserStatus" value={editStatus} onChange={(event) => setEditStatus(event.target.value)}>
              <option value="ACTIVE">Активен</option>
              <option value="DISABLED">Отключён</option>
            </select>
            <div className="actions">
              <button type="submit" disabled={!editEmail.trim() || !editDisplayName.trim()}>Сохранить</button>
              <button type="button" className="secondary" onClick={() => setEditingUser(null)}>Отмена</button>
            </div>
          </form>
        </div>
      )}
    </article>
  );
}

function AdminStreamsManager({
  account,
  users,
  courses,
  isBusy,
  onMessage,
  onError
}: {
  account: AccountSession;
  users: AdminUser[];
  courses: Course[];
  isBusy: boolean;
  onMessage: (message: string) => void;
  onError: (message: string) => void;
}) {
  const [streamsPage, setStreamsPage] = useState<PageResponse<StudentStreamSummary>>({
    items: [],
    page: 0,
    size: 10,
    totalItems: 0,
    totalPages: 0
  });
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [bulkMode, setBulkMode] = useState<"delete" | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [createOpen, setCreateOpen] = useState(false);

  const [newName, setNewName] = useState("");
  const [newCourseSelection, setNewCourseSelection] = useState<Set<string>>(new Set());
  const [newMemberSelection, setNewMemberSelection] = useState<Set<string>>(new Set());

  const [editing, setEditing] = useState<StudentStreamDetail | null>(null);
  const [editingLoading, setEditingLoading] = useState(false);
  const [editName, setEditName] = useState("");
  const [editStatus, setEditStatus] = useState<"DRAFT" | "ACTIVE" | "ARCHIVED">("ACTIVE");
  const [scheduleModuleId, setScheduleModuleId] = useState<string>("");
  const [scheduleFields, setScheduleFields] = useState({
    startsAt: "",
    submissionDeadline: "",
    blackBoxStartsAt: "",
    blackBoxDeadline: ""
  });

  const loadStreams = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page: String(page), size: "10" });
      if (query.trim()) {
        params.set("q", query.trim());
      }
      const data = await apiRequest<PageResponse<StudentStreamSummary>>(
        account,
        `/api/admin/streams?${params.toString()}`
      );
      setStreamsPage(data);
      setSelectedIds(new Set());
    } catch (error) {
      onError(error instanceof Error ? error.message : "Не удалось загрузить потоки");
    } finally {
      setLoading(false);
    }
  }, [account, page, query, onError]);

  useEffect(() => {
    void loadStreams();
  }, [loadStreams]);

  useEffect(() => {
    setPage(0);
  }, [query]);

  const refreshEditing = async (streamId: string) => {
    try {
      const data = await apiRequest<StudentStreamDetail>(account, `/api/admin/streams/${streamId}`);
      setEditing(data);
    } catch (error) {
      onError(error instanceof Error ? error.message : "Не удалось обновить поток");
    }
  };

  const userCandidates = useMemo(
    () =>
      users
        .filter((u) => (u.role === "STUDENT" || u.role === "CURATOR") && u.status === "ACTIVE")
        .slice()
        .sort((a, b) => {
          if (a.role !== b.role) {
            return a.role === "CURATOR" ? -1 : 1;
          }
          return a.displayName.localeCompare(b.displayName, "ru");
        }),
    [users]
  );

  const editingCourseIds = useMemo(
    () => new Set(editing?.courses.map((c) => c.courseId) ?? []),
    [editing]
  );

  const editingActiveMemberIds = useMemo(
    () =>
      new Set(
        (editing?.members ?? [])
          .filter((m) => m.status === "ACTIVE")
          .map((m) => m.userId)
      ),
    [editing]
  );

  const editingModules = useMemo(() => {
    if (!editing) {
      return [] as LearningModule[];
    }
    const ids = new Set(editing.courses.map((c) => c.courseId));
    return courses.filter((c) => ids.has(c.id)).flatMap((c) => c.modules);
  }, [editing, courses]);

  const openEdit = async (streamId: string) => {
    setEditingLoading(true);
    setMenuOpen(false);
    try {
      const data = await apiRequest<StudentStreamDetail>(account, `/api/admin/streams/${streamId}`);
      setEditing(data);
      setEditName(data.name);
      setEditStatus(data.status);
      setScheduleModuleId("");
      setScheduleFields({ startsAt: "", submissionDeadline: "", blackBoxStartsAt: "", blackBoxDeadline: "" });
    } catch (error) {
      onError(error instanceof Error ? error.message : "Не удалось загрузить поток");
    } finally {
      setEditingLoading(false);
    }
  };

  const closeEdit = () => {
    setEditing(null);
    setEditingLoading(false);
  };

  const toggleSelected = (streamId: string) => {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (next.has(streamId)) {
        next.delete(streamId);
      } else {
        next.add(streamId);
      }
      return next;
    });
  };

  const closeBulkMode = () => {
    setBulkMode(null);
    setSelectedIds(new Set());
  };

  const handleBulkDelete = async () => {
    if (selectedIds.size === 0) {
      closeBulkMode();
      return;
    }
    let succeeded = 0;
    const failures: string[] = [];
    for (const streamId of Array.from(selectedIds)) {
      try {
        await apiRequest<void>(account, `/api/admin/streams/${streamId}`, { method: "DELETE" });
        succeeded += 1;
      } catch (error) {
        failures.push(error instanceof Error ? error.message : "Неизвестная ошибка");
      }
    }
    closeBulkMode();
    await loadStreams();
    if (succeeded > 0) {
      onMessage(`Удалено потоков: ${succeeded}`);
    }
    if (failures.length > 0) {
      onError(`Не удалось удалить ${failures.length} поток(а): ${failures[0]}`);
    }
  };

  const handleCreate = async () => {
    const name = newName.trim();
    if (!name) {
      return;
    }
    try {
      const created = await apiRequest<StudentStreamDetail>(account, "/api/admin/streams", {
        method: "POST",
        body: JSON.stringify({ name, description: null, status: "ACTIVE" })
      });
      for (const courseId of Array.from(newCourseSelection)) {
        try {
          await apiRequest<StudentStreamDetail>(account, `/api/admin/streams/${created.id}/courses`, {
            method: "POST",
            body: JSON.stringify({ courseId })
          });
        } catch (courseError) {
          onError(
            courseError instanceof Error
              ? `Поток создан, но не удалось добавить курс: ${courseError.message}`
              : "Поток создан, но не удалось добавить курс"
          );
        }
      }
      const pickedIds = Array.from(newMemberSelection);
      if (pickedIds.length > 0) {
        try {
          await apiRequest<StudentStreamDetail>(account, `/api/admin/streams/${created.id}/members`, {
            method: "POST",
            body: JSON.stringify({ userIds: pickedIds })
          });
        } catch (memberError) {
          onError(
            memberError instanceof Error
              ? `Поток создан, но не удалось зачислить пользователей: ${memberError.message}`
              : "Поток создан, но не удалось зачислить пользователей"
          );
        }
      }
      setNewName("");
      setNewMemberSelection(new Set());
      setNewCourseSelection(new Set());
      setCreateOpen(false);
      await loadStreams();
      onMessage("Поток создан");
    } catch (error) {
      onError(error instanceof Error ? error.message : "Не удалось создать поток");
    }
  };

  const handleToggleCourse = async (courseId: string, isLinked: boolean) => {
    if (!editing) {
      return;
    }
    try {
      if (isLinked) {
        await apiRequest<StudentStreamDetail>(
          account,
          `/api/admin/streams/${editing.id}/courses/${courseId}`,
          { method: "DELETE" }
        );
      } else {
        await apiRequest<StudentStreamDetail>(account, `/api/admin/streams/${editing.id}/courses`, {
          method: "POST",
          body: JSON.stringify({ courseId })
        });
      }
      await refreshEditing(editing.id);
      await loadStreams();
    } catch (error) {
      onError(error instanceof Error ? error.message : "Не удалось обновить курсы потока");
    }
  };

  const handleToggleMember = async (userId: string, isMember: boolean) => {
    if (!editing) {
      return;
    }
    try {
      if (isMember) {
        await apiRequest<StudentStreamDetail>(
          account,
          `/api/admin/streams/${editing.id}/members/${userId}`,
          { method: "DELETE" }
        );
      } else {
        await apiRequest<StudentStreamDetail>(account, `/api/admin/streams/${editing.id}/members`, {
          method: "POST",
          body: JSON.stringify({ userIds: [userId] })
        });
      }
      await refreshEditing(editing.id);
      await loadStreams();
    } catch (error) {
      onError(error instanceof Error ? error.message : "Не удалось обновить участников");
    }
  };

  const handleSaveSchedule = async () => {
    if (!editing || !scheduleModuleId) {
      return;
    }
    try {
      await apiRequest<StudentStreamDetail>(
        account,
        `/api/admin/streams/${editing.id}/modules/${scheduleModuleId}/schedule`,
        {
          method: "PUT",
          body: JSON.stringify({
            startsAt: scheduleFields.startsAt ? new Date(scheduleFields.startsAt).toISOString() : null,
            submissionDeadline: scheduleFields.submissionDeadline
              ? new Date(scheduleFields.submissionDeadline).toISOString()
              : null,
            blackBoxStartsAt: scheduleFields.blackBoxStartsAt
              ? new Date(scheduleFields.blackBoxStartsAt).toISOString()
              : null,
            blackBoxDeadline: scheduleFields.blackBoxDeadline
              ? new Date(scheduleFields.blackBoxDeadline).toISOString()
              : null
          })
        }
      );
      setScheduleModuleId("");
      setScheduleFields({ startsAt: "", submissionDeadline: "", blackBoxStartsAt: "", blackBoxDeadline: "" });
      await refreshEditing(editing.id);
      onMessage("Расписание обновлено");
    } catch (error) {
      onError(error instanceof Error ? error.message : "Не удалось сохранить расписание");
    }
  };

  const handleSaveStreamSettings = async () => {
    if (!editing) {
      return;
    }
    const name = editName.trim();
    if (!name) {
      onError("Укажите название потока.");
      return;
    }
    try {
      await apiRequest<StudentStreamDetail>(account, `/api/admin/streams/${editing.id}`, {
        method: "PATCH",
        body: JSON.stringify({ name, description: null, status: editStatus })
      });
      await refreshEditing(editing.id);
      await loadStreams();
      onMessage("Данные потока сохранены");
    } catch (error) {
      onError(error instanceof Error ? error.message : "Не удалось сохранить настройки");
    }
  };

  const handleAssignPeerTasks = async (moduleId: string) => {
    if (!editing || !moduleId) {
      return;
    }
    const studentIds = editing.members
      .filter((member) => member.role === "STUDENT" && member.status === "ACTIVE")
      .map((member) => member.userId);
    if (studentIds.length === 0) {
      onError("В потоке нет активных студентов.");
      return;
    }
    try {
      await apiRequest<PeerStandAssignment[]>(account, "/api/admin/peer-stands/assign", {
        method: "POST",
        body: JSON.stringify({ moduleId, userIds: studentIds, count: 4 })
      });
      onMessage("Задачи студентов распределены для выбранного модуля.");
    } catch (error) {
      onError(error instanceof Error ? error.message : "Не удалось распределить задачи студентов");
    }
  };

  return (
    <article className="card admin-list-card">
      <header className="section-panel-header admin-list-header">
        <div>
          <h2>Потоки</h2>
        </div>
        <div className="admin-menu-wrap">
          <button
            type="button"
            className="ghost-menu-button"
            onClick={() => setMenuOpen((open) => !open)}
            aria-label="Действия с потоками"
          >
            <span aria-hidden="true">{"\u22EE"}</span>
          </button>
          {menuOpen && (
            <div className="admin-action-menu">
              <button
                type="button"
                className="admin-action-menu-item"
                onClick={() => { setCreateOpen(true); setMenuOpen(false); }}
              >
                <span className="admin-action-menu__icon" aria-hidden="true">
                  <ActionIcon name="add" />
                </span>
                <span>Добавить</span>
              </button>
              <button
                type="button"
                className="admin-action-menu-item"
                onClick={() => { setBulkMode("delete"); setMenuOpen(false); }}
              >
                <span className="admin-action-menu__icon" aria-hidden="true">
                  <ActionIcon name="delete" />
                </span>
                <span>Удалить</span>
              </button>
            </div>
          )}
        </div>
      </header>
      <div className="admin-list-toolbar">
        <input
          aria-label="Поиск"
          placeholder=""
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
      </div>
      <div className="admin-flat-list">
        {streamsPage.items.length === 0 ? (
          <EmptyState>{loading ? "Загрузка потоков..." : "Потоки не найдены."}</EmptyState>
        ) : (
          streamsPage.items.map((stream) => (
            <div key={stream.id} className="admin-flat-row admin-stream-row">
              <div className="admin-row-main">
                <strong>{stream.name}</strong>
                <span className="muted">
                  Курсов: {stream.courseCount} · Участников: {stream.memberCount}
                </span>
              </div>
              <div className="admin-row-meta">
                <StatusBadge value={stream.status} />
              </div>
              <button
                type="button"
                className="row-edit-button"
                aria-label="Редактировать поток"
                onClick={() => void openEdit(stream.id)}
              >
                <ActionIcon name="edit" />
              </button>
              {bulkMode && (
                <input
                  className="row-select-checkbox"
                  type="checkbox"
                  aria-label={`Выбрать ${stream.name}`}
                  checked={selectedIds.has(stream.id)}
                  onChange={() => toggleSelected(stream.id)}
                />
              )}
            </div>
          ))
        )}
      </div>
      <div className="pagination-row">
        <button
          type="button"
          className="secondary pagination-nav"
          disabled={page === 0 || isBusy || loading}
          aria-label="Предыдущая страница"
          onClick={() => setPage((p) => Math.max(0, p - 1))}
        >
          {"<"}
        </button>
        <span className="muted">
          Страница {streamsPage.totalPages === 0 ? 0 : streamsPage.page + 1} из {streamsPage.totalPages}
        </span>
        <button
          type="button"
          className="secondary pagination-nav"
          disabled={page + 1 >= streamsPage.totalPages || isBusy || loading}
          aria-label="Следующая страница"
          onClick={() => setPage((p) => p + 1)}
        >
          {">"}
        </button>
      </div>
      {bulkMode && (
        <div className="bulk-action-bar">
          <span>{selectedIds.size > 0 ? `Выбрано: ${selectedIds.size}` : "Ничего не выбрано"}</span>
          <button type="button" disabled={isBusy || loading} onClick={() => void handleBulkDelete()}>
            {selectedIds.size === 0 ? "Отменить" : "Удалить"}
          </button>
        </div>
      )}
      {createOpen && (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <form
            className="modal-card form compact-form"
            onSubmit={async (event) => {
              event.preventDefault();
              await handleCreate();
            }}
          >
            <h2>Добавить поток</h2>
            <label htmlFor="adminNewStreamName">Название</label>
            <input
              id="adminNewStreamName"
              disabled={isBusy || loading}
              value={newName}
              onChange={(event) => setNewName(event.target.value)}
            />
            <div className="checkbox-picker-block">
              <h4>Курсы</h4>
              <div className="checkbox-picker-list">
                {courses.length === 0 ? (
                  <p className="muted">Сначала создайте хотя бы один курс.</p>
                ) : (
                  courses.map((course) => (
                    <label key={course.id} className="checkbox-picker-row">
                      <input
                        type="checkbox"
                        checked={newCourseSelection.has(course.id)}
                        onChange={() =>
                          setNewCourseSelection((current) => {
                            const next = new Set(current);
                            if (next.has(course.id)) {
                              next.delete(course.id);
                            } else {
                              next.add(course.id);
                            }
                            return next;
                          })
                        }
                      />
                      <span>{course.title}</span>
                    </label>
                  ))
                )}
              </div>
            </div>
            <div className="checkbox-picker-block">
              <h4>Пользователи</h4>
              <div className="checkbox-picker-list">
                {userCandidates.length === 0 ? (
                  <p className="muted">Нет активных пользователей для зачисления.</p>
                ) : (
                  userCandidates.map((user) => (
                    <label key={user.id} className="checkbox-picker-row">
                      <input
                        type="checkbox"
                        checked={newMemberSelection.has(user.id)}
                        onChange={() =>
                          setNewMemberSelection((current) => {
                            const next = new Set(current);
                            if (next.has(user.id)) {
                              next.delete(user.id);
                            } else {
                              next.add(user.id);
                            }
                            return next;
                          })
                        }
                      />
                      <span>
                        {user.displayName}
                        <span className="muted">
                          {" · "}{user.role === "CURATOR" ? "куратор" : "студент"}
                        </span>
                      </span>
                    </label>
                  ))
                )}
              </div>
            </div>
            <div className="actions">
              <button type="submit" disabled={isBusy || loading || !newName.trim()}>
                Создать
              </button>
              <button type="button" className="secondary" onClick={() => setCreateOpen(false)}>
                Отмена
              </button>
            </div>
          </form>
        </div>
      )}
      {(editing || editingLoading) && (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal-card form compact-form admin-stream-edit-modal">
            {editingLoading && !editing ? (
              <p className="muted">Загрузка карточки потока…</p>
            ) : editing ? (
              <>
                <h2>Редактировать поток</h2>
                <label htmlFor="streamEditName">Название</label>
                <input
                  id="streamEditName"
                  disabled={isBusy}
                  value={editName}
                  onChange={(event) => setEditName(event.target.value)}
                />
                <label htmlFor="streamStatusSelect">Статус</label>
                <select
                  id="streamStatusSelect"
                  disabled={isBusy}
                  value={editStatus}
                  onChange={(event) => setEditStatus(event.target.value as "DRAFT" | "ACTIVE" | "ARCHIVED")}
                >
                  <option value="DRAFT">Черновик</option>
                  <option value="ACTIVE">Активен</option>
                  <option value="ARCHIVED">В архиве</option>
                </select>
                <div className="actions">
                  <button
                    type="button"
                    disabled={isBusy || !editName.trim()}
                    onClick={() => void handleSaveStreamSettings()}
                  >
                    Сохранить название и статус
                  </button>
                </div>
                <div className="checkbox-picker-block">
                  <h4>Курсы</h4>
                  <div className="checkbox-picker-list">
                    {courses.length === 0 ? (
                      <p className="muted">Сначала создайте хотя бы один курс.</p>
                    ) : (
                      courses.map((course) => {
                        const linked = editingCourseIds.has(course.id);
                        return (
                          <label key={course.id} className="checkbox-picker-row">
                            <input
                              type="checkbox"
                              checked={linked}
                              disabled={isBusy}
                              onChange={() => void handleToggleCourse(course.id, linked)}
                            />
                            <span>{course.title}</span>
                          </label>
                        );
                      })
                    )}
                  </div>
                </div>
                <div className="checkbox-picker-block">
                  <h4>Пользователи</h4>
                  <div className="checkbox-picker-list">
                    {userCandidates.length === 0 ? (
                      <p className="muted">Нет доступных пользователей.</p>
                    ) : (
                      userCandidates.map((user) => {
                        const isMember = editingActiveMemberIds.has(user.id);
                        return (
                          <label key={user.id} className="checkbox-picker-row">
                            <input
                              type="checkbox"
                              checked={isMember}
                              disabled={isBusy}
                              onChange={() => void handleToggleMember(user.id, isMember)}
                            />
                            <span>
                              {user.displayName}
                              <span className="muted">
                                {" · "}{user.role === "CURATOR" ? "куратор" : "студент"}
                              </span>
                            </span>
                          </label>
                        );
                      })
                    )}
                  </div>
                </div>
                {editingModules.length > 0 && (
                  <div className="checkbox-picker-block">
                    <h4>Расписание модуля</h4>
                    <div className="form compact-form stream-schedule-form">
                      <label htmlFor="streamModuleSelect">Модуль</label>
                      <select
                        id="streamModuleSelect"
                        value={scheduleModuleId}
                        disabled={isBusy}
                        onChange={(event) => setScheduleModuleId(event.target.value)}
                      >
                        <option value="">Выберите модуль</option>
                        {editingModules.map((module) => (
                          <option key={module.id} value={module.id}>
                            {module.title}
                          </option>
                        ))}
                      </select>
                      <div className="stream-datetime-grid">
                        <div className="stream-field-group">
                          <label htmlFor="streamSchedStart">Начало</label>
                          <input
                            id="streamSchedStart"
                            type="datetime-local"
                            value={scheduleFields.startsAt}
                            onChange={(event) =>
                              setScheduleFields((prev) => ({ ...prev, startsAt: event.target.value }))
                            }
                          />
                        </div>
                        <div className="stream-field-group">
                          <label htmlFor="streamSchedSubmit">Дедлайн сдачи</label>
                          <input
                            id="streamSchedSubmit"
                            type="datetime-local"
                            value={scheduleFields.submissionDeadline}
                            onChange={(event) =>
                              setScheduleFields((prev) => ({ ...prev, submissionDeadline: event.target.value }))
                            }
                          />
                        </div>
                        <div className="stream-field-group">
                          <label htmlFor="streamSchedBbStart">Старт black-box</label>
                          <input
                            id="streamSchedBbStart"
                            type="datetime-local"
                            value={scheduleFields.blackBoxStartsAt}
                            onChange={(event) =>
                              setScheduleFields((prev) => ({ ...prev, blackBoxStartsAt: event.target.value }))
                            }
                          />
                        </div>
                        <div className="stream-field-group">
                          <label htmlFor="streamSchedBbEnd">Дедлайн black-box</label>
                          <input
                            id="streamSchedBbEnd"
                            type="datetime-local"
                            value={scheduleFields.blackBoxDeadline}
                            onChange={(event) =>
                              setScheduleFields((prev) => ({ ...prev, blackBoxDeadline: event.target.value }))
                            }
                          />
                        </div>
                      </div>
                      <div className="actions">
                        <button
                          type="button"
                          disabled={!scheduleModuleId || isBusy}
                          onClick={() => void handleSaveSchedule()}
                        >
                          Сохранить расписание
                        </button>
                        <button
                          type="button"
                          className="secondary"
                          disabled={!scheduleModuleId || isBusy}
                          onClick={() => void handleAssignPeerTasks(scheduleModuleId)}
                        >
                          Распределить задачи
                        </button>
                      </div>
                    </div>
                  </div>
                )}
                <div className="actions">
                  <button type="button" className="secondary" onClick={closeEdit}>
                    Закрыть
                  </button>
                </div>
              </>
            ) : null}
          </div>
        </div>
      )}
    </article>
  );
}

type ModuleProgressLevel = {
  key: string;
  label: string;
  total: number;
  solved: number;
};

const LEVEL_PALETTE: Record<string, { ring: string; track: string; valueColor: string }> = {
  easy: { ring: "#10b981", track: "#d1fae5", valueColor: "#047857" },
  medium: { ring: "#f59e0b", track: "#fef3c7", valueColor: "#b45309" },
  hard: { ring: "#ef4444", track: "#fee2e2", valueColor: "#b91c1c" },
  unknown: { ring: "#94a3b8", track: "#e2e8f0", valueColor: "#475569" }
};

function ModuleProgressTracker({
  moduleDifficultyProgress,
  courseTaskProgress,
  peerAssignments,
  topOffset
}: {
  moduleDifficultyProgress: ModuleProgressLevel[];
  courseTaskProgress: { solved: number; total: number; percent: number };
  peerAssignments: PeerStandAssignment[];
  topOffset: number;
}) {
  const labsPercent = Math.max(0, Math.min(100, courseTaskProgress.percent));
  return (
    <aside className="progress-tracker" aria-label="Ваш прогресс" style={{ marginTop: topOffset }}>
      <div className="progress-tracker-header">
        <h3>Ваш прогресс</h3>
      </div>
      <div className="progress-tracker-section">
        <div className="progress-tracker-row">
          <span className="progress-tracker-label">Лабораторные:</span>
        </div>
        <div className="progress-tracker-bar" role="img" aria-label={`Лабораторные: ${labsPercent}%`}>
          <div className="progress-tracker-bar-fill" style={{ width: `${labsPercent}%` }} />
        </div>
        <div className="progress-tracker-bar-meta">
          <span>{courseTaskProgress.solved} из {courseTaskProgress.total}</span>
          <strong>{labsPercent}%</strong>
        </div>
      </div>
      <div className="progress-tracker-section">
        <p className="progress-tracker-label">Прогресс по уровням:</p>
        {moduleDifficultyProgress.length === 0 ? (
          <p className="muted">В этом модуле пока нет лабораторных задач.</p>
        ) : (
          <div className="progress-tracker-rings">
            {moduleDifficultyProgress.map((item) => (
              <ProgressRing key={item.key} item={item} />
            ))}
          </div>
        )}
      </div>
      <div className="progress-tracker-section module-progress-peer">
        <p className="progress-tracker-label">Задачи студентов:</p>
        {peerAssignments.length === 0 ? (
          <p className="muted">Нет назначенных peer-стендов.</p>
        ) : (
          <div className="module-peer-circles" role="list">
            {peerAssignments.map((assignment) => (
              <span
                key={assignment.assignmentId}
                role="listitem"
                title={`${assignment.standDisplayName}${assignment.solved ? " — решено" : ""}`}
                className={`module-peer-circle ${assignment.solved ? "solved" : "pending"}`}
              />
            ))}
          </div>
        )}
      </div>
    </aside>
  );
}

function ProgressRing({ item }: { item: ModuleProgressLevel }) {
  const palette = LEVEL_PALETTE[item.key] ?? LEVEL_PALETTE.unknown;
  const size = 72;
  const stroke = 6;
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const ratio = item.total > 0 ? Math.min(1, Math.max(0, item.solved / item.total)) : 0;
  const dashOffset = circumference * (1 - ratio);
  return (
    <div className="progress-tracker-ring" title={`${item.label}: ${item.solved} из ${item.total}`}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} aria-hidden="true">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={palette.track}
          strokeWidth={stroke}
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={palette.ring}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={dashOffset}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
        />
        <text
          x="50%"
          y="46%"
          textAnchor="middle"
          dominantBaseline="middle"
          className="progress-tracker-ring-value"
          style={{ fill: palette.valueColor }}
        >
          {item.solved}
        </text>
        <text
          x="50%"
          y="64%"
          textAnchor="middle"
          dominantBaseline="middle"
          className="progress-tracker-ring-total"
        >
          из {item.total}
        </text>
      </svg>
      <span className="progress-tracker-ring-label">{item.label}</span>
    </div>
  );
}

type PipelineStageState = "pending" | "running" | "success" | "error";

type PipelineStage = {
  key: string;
  title: string;
  state: PipelineStageState;
  hint?: string;
};

function buildStandPipelineStages(stand: UserPentestStand): PipelineStage[] {
  const status = stand.status;
  const failedStage = stand.failedStage;
  const buildStageState = (
    target: "UPLOAD" | "BUILD" | "VALIDATE" | "READY"
  ): PipelineStageState => {
    if (status === "FAILED") {
      const order = { UPLOAD: 0, BUILD: 1, VALIDATE: 2, READY: 3 };
      const failed = failedStage ?? "UPLOAD";
      const failedIdx = order[failed];
      const targetIdx = order[target];
      if (targetIdx < failedIdx) return "success";
      if (targetIdx === failedIdx) return "error";
      return "pending";
    }
    if (status === "READY") return "success";
    if (status === "UPLOADED") {
      return target === "UPLOAD" ? "running" : "pending";
    }
    if (status === "BUILDING") {
      if (target === "UPLOAD") return "success";
      if (target === "BUILD") return "running";
      return "pending";
    }
    if (status === "VALIDATING") {
      if (target === "UPLOAD" || target === "BUILD") return "success";
      if (target === "VALIDATE") return "running";
      return "pending";
    }
    return "pending";
  };
  const ready = status === "READY";
  const solved = ready && stand.authorSolved;
  const review = stand.reviewStatus;
  const solvedState: PipelineStageState = solved ? "success" : ready ? "running" : "pending";
  const reviewState: PipelineStageState =
    review === "APPROVED"
      ? "success"
      : review === "PENDING_REVIEW"
        ? "running"
        : review === "REJECTED"
          ? "error"
          : "pending";
  const sharedState: PipelineStageState =
    review === "APPROVED" ? "success" : "pending";
  return [
    { key: "UPLOAD", title: "Загрузка", state: buildStageState("UPLOAD") },
    { key: "BUILD", title: "Сборка", state: buildStageState("BUILD") },
    { key: "VALIDATE", title: "Health-check", state: buildStageState("VALIDATE") },
    { key: "READY", title: "Готов", state: buildStageState("READY") },
    { key: "SOLVED", title: "Решён автором", state: solvedState },
    { key: "REVIEW", title: "Проверка куратором", state: reviewState },
    { key: "SHARED", title: "Передан студентам", state: sharedState }
  ];
}

function StandPipelineBar({ stages }: { stages: PipelineStage[] }) {
  return (
    <ol className="stand-pipeline" aria-label="Стадии проверки стенда">
      {stages.map((stage, index) => (
        <li key={stage.key} className={`stand-pipeline-stage stand-pipeline-stage--${stage.state}`}>
          <span className="stand-pipeline-icon" aria-hidden="true">
            {stage.state === "success" && "\u2713"}
            {stage.state === "error" && "\u2715"}
            {stage.state === "running" && <span className="stand-pipeline-spinner" />}
            {stage.state === "pending" && (index + 1)}
          </span>
          <span className="stand-pipeline-title">{stage.title}</span>
        </li>
      ))}
    </ol>
  );
}

type LabStandRunningSnapshot = {
  publicUrl?: string | null;
  expiresAt: string;
};

function useLabStandClock(running: LabStandRunningSnapshot | null) {
  const [nowMs, setNowMs] = useState<number>(() => Date.now());
  const runningId = running ? `${running.publicUrl ?? ""}|${running.expiresAt}` : null;
  useEffect(() => {
    if (!runningId) {
      return;
    }
    setNowMs(Date.now());
    const handle = window.setInterval(() => setNowMs(Date.now()), 1000);
    return () => window.clearInterval(handle);
  }, [runningId]);
  return nowMs;
}

function LabStandToolbar({
  running,
  busy,
  onStart,
  onStop,
  startLabel = "Запустить стенд",
  stopLabel = "Остановить стенд",
  extras
}: {
  running: LabStandRunningSnapshot | null;
  busy: boolean;
  onStart: () => unknown;
  onStop: () => unknown;
  startLabel?: string;
  stopLabel?: string;
  extras?: React.ReactNode;
}) {
  const nowMs = useLabStandClock(running);
  const countdown = running ? formatRemainingTimer(running.expiresAt, nowMs) : "";
  const publicUrl = running?.publicUrl?.trim();
  return (
    <div className="lab-task-stand-toolbar">
      <button
        type="button"
        className="secondary"
        disabled={busy}
        onClick={() => {
          if (running) {
            void onStop();
          } else {
            void onStart();
          }
        }}
      >
        {running ? stopLabel : startLabel}
      </button>
      {running ? (
        publicUrl ? (
          <a
            className="lab-stand-url-inline"
            href={publicUrl}
            target="_blank"
            rel="noopener noreferrer"
          >
            Открыть стенд
          </a>
        ) : (
          <span className="muted lab-stand-booting">Стенд поднимается…</span>
        )
      ) : null}
      {running ? (
        <span className="lab-stand-timer muted" title="Оставшееся время">
          {countdown}
        </span>
      ) : null}
      {extras}
    </div>
  );
}

function LabStandFlagForm({
  busy,
  onSubmit,
  buttonLabel = "Отправить"
}: {
  busy: boolean;
  onSubmit: (flag: string) => void | Promise<void>;
  buttonLabel?: string;
}) {
  const [draft, setDraft] = useState("");
  const trimmed = draft.trim();
  return (
    <form
      className="lab-flag-form-compact"
      onSubmit={async (event) => {
        event.preventDefault();
        if (!trimmed || busy) {
          return;
        }
        await onSubmit(trimmed);
        setDraft("");
      }}
    >
      <input
        type="text"
        autoComplete="off"
        spellCheck={false}
        placeholder={PENTEST_FLAG_PLACEHOLDER}
        disabled={busy}
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
      />
      <button type="submit" disabled={busy || !trimmed}>
        {buttonLabel}
      </button>
    </form>
  );
}

function UserPentestStandsCard({
  account,
  externalBusy,
  selectedModule,
  onMessage,
  onError,
  onStandLaunched
}: {
  account: AccountSession;
  externalBusy: boolean;
  selectedModule: ModuleOption | null;
  onMessage: (message: string) => void;
  onError: (message: string) => void;
  onStandLaunched: (toast: StandLaunchToast | null) => void;
}) {
  const [stands, setStands] = useState<UserPentestStand[]>([]);
  const [instances, setInstances] = useState<UserPentestStandInstance[]>([]);
  const [busy, setBusy] = useState(false);
  const [archive, setArchive] = useState<File | null>(null);
  const [archiveInputVersion, setArchiveInputVersion] = useState(0);
  const [displayName, setDisplayName] = useState("");
  const [description, setDescription] = useState("");
  const [editingStandId, setEditingStandId] = useState<string | null>(null);
  const [expandedLogs, setExpandedLogs] = useState<Set<string>>(() => new Set());
  const [dragging, setDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setEditingStandId(null);
    setArchive(null);
    setArchiveInputVersion((v) => v + 1);
    setDisplayName("");
    setDescription("");
  }, [selectedModule?.id]);

  const isBusy = busy || externalBusy;
  const moduleId = selectedModule?.id ?? "";

  const loadAll = useCallback(async () => {
    try {
      const [list, runningInstances] = await Promise.all([
        apiRequest<UserPentestStand[]>(account, "/api/user-stands"),
        apiRequest<UserPentestStandInstance[]>(account, "/api/user-stand-instances/my")
      ]);
      setStands(list);
      setInstances(runningInstances);
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось загрузить пользовательские стенды.");
    }
  }, [account, onError]);

  useEffect(() => {
    void loadAll();
  }, [loadAll]);

  useEffect(() => {
    const hasPending = stands.some(
      (stand) => stand.status === "UPLOADED" || stand.status === "BUILDING" || stand.status === "VALIDATING"
    );
    if (!hasPending) {
      return;
    }
    const handle = window.setInterval(() => {
      void loadAll();
    }, 2500);
    return () => window.clearInterval(handle);
  }, [stands, loadAll]);

  const instanceByStandId = useMemo(() => {
    const map = new Map<string, UserPentestStandInstance>();
    for (const instance of instances) {
      if (instance.status === "RUNNING") {
        map.set(instance.standId, instance);
      }
    }
    return map;
  }, [instances]);

  const moduleStand = useMemo<UserPentestStand | null>(() => {
    if (!moduleId) {
      return null;
    }
    return stands.find((stand) => stand.moduleId === moduleId) ?? null;
  }, [stands, moduleId]);

  const isEditing = moduleStand !== null && editingStandId === moduleStand.id;
  const showForm = moduleStand === null || isEditing;

  function pickArchiveFile(file: File | null) {
    if (!file) {
      setArchive(null);
      return;
    }
    const lower = file.name.toLowerCase();
    const allowed = [".zip", ".tar", ".tar.gz", ".tgz", ".gz"];
    if (!allowed.some((ext) => lower.endsWith(ext))) {
      onError("Поддерживаются архивы .zip, .tar, .tar.gz, .tgz");
      return;
    }
    setArchive(file);
  }

  function handleDragOver(event: React.DragEvent<HTMLDivElement>) {
    event.preventDefault();
    if (!isBusy && !dragging) {
      setDragging(true);
    }
  }

  function handleDragLeave(event: React.DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragging(false);
  }

  function handleDrop(event: React.DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragging(false);
    if (isBusy) {
      return;
    }
    const file = event.dataTransfer.files?.[0] ?? null;
    pickArchiveFile(file);
  }

  const toggleLog = useCallback((standId: string) => {
    setExpandedLogs((current) => {
      const next = new Set(current);
      if (next.has(standId)) {
        next.delete(standId);
      } else {
        next.add(standId);
      }
      return next;
    });
  }, []);

  async function handleUpload(event: React.FormEvent) {
    event.preventDefault();
    if (isBusy || !archive) {
      return;
    }
    if (!moduleId) {
      onError("Откройте раздел практики из конкретного модуля курса.");
      return;
    }
    setBusy(true);
    try {
      if (isEditing && moduleStand) {
        await apiRequest(account, `/api/user-stands/${moduleStand.id}`, { method: "DELETE" });
      }
      await uploadUserStandArchive(account, {
        archive,
        displayName,
        description,
        moduleId
      });
      onMessage(isEditing ? "Стенд заменён. Идёт сборка и проверка контейнера..." : "Архив принят. Идет сборка и проверка контейнера...");
      setArchive(null);
      setArchiveInputVersion((v) => v + 1);
      setDisplayName("");
      setDescription("");
      setEditingStandId(null);
      await loadAll();
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось загрузить архив стенда.");
    } finally {
      setBusy(false);
    }
  }

  function handleStartEdit(stand: UserPentestStand) {
    setEditingStandId(stand.id);
    setDisplayName(stand.displayName);
    setDescription(stand.description ?? "");
    setArchive(null);
    setArchiveInputVersion((v) => v + 1);
  }

  function handleCancelEdit() {
    setEditingStandId(null);
    setArchive(null);
    setArchiveInputVersion((v) => v + 1);
    setDisplayName("");
    setDescription("");
  }

  async function handleSubmitFlag(standId: string, candidate: string) {
    setBusy(true);
    try {
      const result = await apiRequest<{ accepted: boolean; solved: boolean; attempts: number; message: string }>(
        account,
        `/api/user-stands/${standId}/flag`,
        {
          method: "POST",
          body: JSON.stringify({ flag: candidate })
        }
      );
      if (result.accepted) {
        onMessage(result.message || "Флаг принят. Стенд подтверждён!");
      } else {
        onError(result.message || "Неверный флаг.");
      }
      await loadAll();
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось проверить флаг.");
    } finally {
      setBusy(false);
    }
  }

  async function handleSubmitForReview(standId: string) {
    setBusy(true);
    try {
      await apiRequest(account, `/api/user-stands/${standId}/submit-review`, { method: "POST" });
      onMessage("Стенд отправлен на проверку куратору.");
      await loadAll();
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось отправить стенд на проверку.");
    } finally {
      setBusy(false);
    }
  }

  async function handleWithdrawReview(standId: string) {
    if (!window.confirm("Отозвать стенд с проверки? Куратор больше не увидит его в очереди.")) {
      return;
    }
    setBusy(true);
    try {
      await apiRequest(account, `/api/user-stands/${standId}/withdraw-review`, { method: "POST" });
      onMessage("Стенд отозван с проверки. Можно отредактировать и отправить заново.");
      await loadAll();
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось отозвать стенд с проверки.");
    } finally {
      setBusy(false);
    }
  }

  async function handleStart(standId: string) {
    setBusy(true);
    try {
      const instance = await apiRequest<UserPentestStandInstance>(
        account,
        `/api/user-stands/${standId}/instances`,
        { method: "POST" }
      );
      const standName =
        instance.standDisplayName
        || stands.find((s) => s.id === standId)?.displayName
        || "Мой стенд";
      onStandLaunched({ variant: "launched", standName, publicUrl: instance.publicUrl });
      onMessage(`Стенд «${standName}» запущен. Доступ появится через несколько секунд.`);
      await loadAll();
    } catch (caught) {
      const messageText = caught instanceof Error ? caught.message : "Не удалось запустить стенд.";
      const running = instances.find((inst) => inst.status === "RUNNING");
      if (running && /запущенный/.test(messageText)) {
        onStandLaunched({
          variant: "blocked",
          standName: running.standDisplayName || "Запущенный стенд",
          publicUrl: running.publicUrl
        });
      }
      onError(messageText);
    } finally {
      setBusy(false);
    }
  }

  async function handleStop(instanceId: string) {
    setBusy(true);
    try {
      await apiRequest(account, `/api/user-stand-instances/${instanceId}/stop`, {
        method: "POST"
      });
      onMessage("Стенд остановлен.");
      await loadAll();
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось остановить стенд.");
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete(standId: string) {
    if (!window.confirm("Удалить стенд и архив? Действие нельзя отменить.")) {
      return;
    }
    setBusy(true);
    try {
      await apiRequest(account, `/api/user-stands/${standId}`, { method: "DELETE" });
      onMessage("Стенд удалён.");
      if (editingStandId === standId) {
        setEditingStandId(null);
      }
      setArchive(null);
      setArchiveInputVersion((v) => v + 1);
      setDisplayName("");
      setDescription("");
      await loadAll();
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось удалить стенд.");
    } finally {
      setBusy(false);
    }
  }

  if (!selectedModule) {
    return (
      <article className="card">
        <EmptyState>Выберите модуль курса, чтобы загрузить или открыть свой стенд.</EmptyState>
      </article>
    );
  }

  const stand: UserPentestStand | null = moduleStand && !isEditing ? moduleStand : null;
  const standRunning = stand ? instanceByStandId.get(stand.id) ?? null : null;
  const standPipelineStages = stand ? buildStandPipelineStages(stand) : [];
  const showStandPipeline = stand !== null && stand.status !== "ARCHIVED";
  const standIsReady = stand?.status === "READY";
  const canSubmitForReview =
    stand !== null
      && stand.authorSolved
      && (stand.reviewStatus === "DRAFT" || stand.reviewStatus === "REJECTED");
  const canWithdrawReview = stand !== null && stand.reviewStatus === "PENDING_REVIEW";

  return (
    <article className="card">
      {showForm && (
        <form className="form compact-form" onSubmit={handleUpload}>
          <label htmlFor="userStandDisplayName">Название стенда</label>
          <input
            id="userStandDisplayName"
            disabled={isBusy}
            value={displayName}
            maxLength={200}
            placeholder="Например, my-vuln-app"
            onChange={(event) => setDisplayName(event.target.value)}
          />
          <label htmlFor="userStandDescription">Описание (опционально)</label>
          <textarea
            id="userStandDescription"
            rows={2}
            disabled={isBusy}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
          <label>Архив (.zip, .tar, .tar.gz)</label>
          <div
            className={`stand-dropzone${dragging ? " stand-dropzone--active" : ""}${archive ? " stand-dropzone--filled" : ""}${isBusy ? " stand-dropzone--disabled" : ""}`}
            onDragOver={handleDragOver}
            onDragEnter={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => {
              if (!isBusy) {
                fileInputRef.current?.click();
              }
            }}
            role="button"
            tabIndex={0}
            onKeyDown={(event) => {
              if ((event.key === "Enter" || event.key === " ") && !isBusy) {
                event.preventDefault();
                fileInputRef.current?.click();
              }
            }}
          >
            <input
              key={archiveInputVersion}
              ref={fileInputRef}
              id="userStandArchive"
              type="file"
              accept=".zip,.tar,.gz,.tgz,.tar.gz"
              disabled={isBusy}
              style={{ display: "none" }}
              onChange={(event) => pickArchiveFile(event.target.files?.[0] ?? null)}
            />
            {archive ? (
              <div className="stand-dropzone-content">
                <strong>{archive.name}</strong>
                <span className="muted">{formatBytes(archive.size)}</span>
                <button
                  type="button"
                  className="link-button"
                  disabled={isBusy}
                  onClick={(event) => {
                    event.stopPropagation();
                    setArchive(null);
                    setArchiveInputVersion((v) => v + 1);
                  }}
                >
                  Убрать
                </button>
              </div>
            ) : (
              <div className="stand-dropzone-content">
                <strong>Перетащите архив сюда</strong>
                <span className="muted">или нажмите, чтобы выбрать файл (.zip, .tar, .tar.gz, .tgz)</span>
              </div>
            )}
          </div>
          <div className="lab-task-stand-toolbar">
            <button type="submit" disabled={isBusy || !archive || !moduleId}>
              {isEditing ? "Заменить стенд" : "Загрузить и проверить"}
            </button>
            {isEditing && (
              <button type="button" className="secondary" disabled={isBusy} onClick={handleCancelEdit}>
                Отменить
              </button>
            )}
          </div>
        </form>
      )}

      {stand && (
        <div className="stand-card">
          <div className="lab-stand-header">
            <strong>{stand.displayName}</strong>
          </div>
          <p className="muted">
            {stand.originalFilename} · {formatBytes(stand.archiveSizeBytes)}
            {stand.applicationPort ? ` · port ${stand.applicationPort}` : ""}
            {stand.composeService ? ` · compose: ${stand.composeService}` : ""}
            {typeof stand.imageSizeBytes === "number" && stand.imageSizeBytes > 0
              ? ` · образ ${formatBytes(stand.imageSizeBytes)}`
              : ""}
          </p>
          {stand.description && <p>{stand.description}</p>}
          {stand.reviewStatus === "REJECTED" && stand.reviewComment && (
            <p className="error-text" style={{ marginTop: "0.25rem" }}>
              Куратор: {stand.reviewComment}
            </p>
          )}
          {showStandPipeline && <StandPipelineBar stages={standPipelineStages} />}
          {stand.status === "FAILED" && stand.lastError && (
            <p className="error-text" style={{ marginTop: "0.5rem" }}>Ошибка: {stand.lastError}</p>
          )}
          {(stand.buildLog || stand.lastError) && (
            <details
              open={expandedLogs.has(stand.id)}
              onToggle={() => toggleLog(stand.id)}
              className="advanced-panel stand-pipeline-log"
            >
              <summary>Подробный лог</summary>
              <pre className="mono-wrap" style={{ whiteSpace: "pre-wrap", maxHeight: 240, overflow: "auto" }}>
                {stand.buildLog ?? stand.lastError}
              </pre>
            </details>
          )}

          {standRunning && (
            <LabStandFlagForm
              busy={isBusy}
              onSubmit={(value) => handleSubmitFlag(stand.id, value)}
            />
          )}

          {standIsReady && (
            <LabStandToolbar
              running={standRunning}
              busy={isBusy}
              onStart={() => handleStart(stand.id)}
              onStop={() => standRunning && handleStop(standRunning.id)}
            />
          )}

          <div className="stand-actions">
            <div className="stand-actions-primary">
              {canSubmitForReview && (
                <button
                  type="button"
                  disabled={isBusy}
                  onClick={() => void handleSubmitForReview(stand.id)}
                >
                  Отправить на проверку куратору
                </button>
              )}
              {canWithdrawReview && (
                <button
                  type="button"
                  className="secondary"
                  disabled={isBusy}
                  onClick={() => void handleWithdrawReview(stand.id)}
                >
                  Отозвать с проверки
                </button>
              )}
            </div>
            <div className="stand-actions-secondary">
              <button
                type="button"
                className="secondary"
                disabled={isBusy}
                onClick={() => handleStartEdit(stand)}
              >
                Редактировать
              </button>
              <button
                type="button"
                className="secondary"
                disabled={isBusy}
                onClick={() => void handleDelete(stand.id)}
              >
                Удалить
              </button>
            </div>
          </div>
        </div>
      )}
    </article>
  );
}

const REVIEW_STATUS_LABEL: Record<UserPentestStandReviewStatus, string> = {
  DRAFT: "Черновик",
  PENDING_REVIEW: "На проверке",
  APPROVED: "Одобрен",
  REJECTED: "Отклонён"
};

function StandReviewSection({
  account,
  isBusy,
  onMessage,
  onError
}: {
  account: AccountSession;
  isBusy: boolean;
  onMessage: (message: string) => void;
  onError: (message: string) => void;
}) {
  const [stands, setStands] = useState<UserPentestStand[]>([]);
  const [instances, setInstances] = useState<UserPentestStandInstance[]>([]);
  const [busy, setBusy] = useState(false);
  const [rejectDrafts, setRejectDrafts] = useState<Record<string, string>>({});

  const reload = useCallback(async () => {
    try {
      const [list, runningInstances] = await Promise.all([
        apiRequest<UserPentestStand[]>(account, "/api/user-stand-reviews"),
        apiRequest<UserPentestStandInstance[]>(account, "/api/user-stand-instances/my")
      ]);
      setStands(list);
      setInstances(runningInstances);
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось загрузить стенды на проверку.");
    }
  }, [account, onError]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const instanceByStandId = useMemo(() => {
    const map = new Map<string, UserPentestStandInstance>();
    for (const instance of instances) {
      if (instance.status === "RUNNING") {
        map.set(instance.standId, instance);
      }
    }
    return map;
  }, [instances]);

  const isAnyBusy = busy || isBusy;

  async function handleStart(standId: string) {
    setBusy(true);
    try {
      await apiRequest<UserPentestStandInstance>(account, `/api/user-stands/${standId}/instances`, { method: "POST" });
      onMessage("Стенд запущен. Доступ появится через несколько секунд.");
      await reload();
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось запустить стенд.");
    } finally {
      setBusy(false);
    }
  }

  async function handleStop(instanceId: string) {
    setBusy(true);
    try {
      await apiRequest(account, `/api/user-stand-instances/${instanceId}/stop`, { method: "POST" });
      onMessage("Стенд остановлен.");
      await reload();
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось остановить стенд.");
    } finally {
      setBusy(false);
    }
  }

  async function handleSubmitFlag(standId: string, flag: string) {
    setBusy(true);
    try {
      const result = await apiRequest<{ accepted: boolean; solved: boolean; attempts: number; message: string }>(
        account,
        `/api/user-stands/${standId}/flag`,
        {
          method: "POST",
          body: JSON.stringify({ flag })
        }
      );
      if (result.accepted) {
        onMessage(result.message || "Флаг принят.");
      } else {
        onError(result.message || "Неверный флаг.");
      }
      await reload();
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось проверить флаг.");
    } finally {
      setBusy(false);
    }
  }

  async function handleApprove(standId: string) {
    setBusy(true);
    try {
      await apiRequest(account, `/api/user-stand-reviews/${standId}/approve`, { method: "POST" });
      onMessage("Стенд одобрен.");
      await reload();
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось одобрить стенд.");
    } finally {
      setBusy(false);
    }
  }

  async function handleReject(standId: string) {
    const reason = (rejectDrafts[standId] ?? "").trim();
    if (!reason) {
      onError("Укажите причину отклонения.");
      return;
    }
    setBusy(true);
    try {
      await apiRequest(account, `/api/user-stand-reviews/${standId}/reject`, {
        method: "POST",
        body: JSON.stringify({ comment: reason })
      });
      onMessage("Стенд отклонён.");
      setRejectDrafts((current) => ({ ...current, [standId]: "" }));
      await reload();
    } catch (caught) {
      onError(caught instanceof Error ? caught.message : "Не удалось отклонить стенд.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <article className="card">
      <h2>Стенды на проверке</h2>
      <p className="muted">
        Студенческие стенды, отправленные авторами на проверку. Запустите инстанс, найдите флаг и одобрите либо отклоните стенд.
      </p>
      {stands.length === 0 ? (
        <EmptyState>Нет стендов в очереди на проверку.</EmptyState>
      ) : (
        <ul className="list" style={{ marginTop: "1rem" }}>
          {stands.map((stand) => {
            const running = instanceByStandId.get(stand.id) ?? null;
            const rejectDraft = rejectDrafts[stand.id] ?? "";
            return (
              <li key={stand.id} className="list-item lab-task-panel lab-task-panel-minimal review-stand-card">
                <div className="lab-stand-header">
                  <strong>{stand.displayName}</strong>
                  <span className="status-pill stand-review-pill stand-review-pill--pending_review">
                    {REVIEW_STATUS_LABEL[stand.reviewStatus]}
                  </span>
                </div>
                <p className="muted">
                  {stand.moduleTitle ? `${stand.moduleTitle} · ` : ""}
                  Автор: {stand.ownerDisplayName ?? stand.ownerEmail ?? "—"}
                  {stand.submittedForReviewAt
                    ? ` · отправлено ${new Date(stand.submittedForReviewAt).toLocaleString()}`
                    : ""}
                </p>
                {stand.description && <p>{stand.description}</p>}
                {running && (
                  <LabStandFlagForm busy={isAnyBusy} onSubmit={(flag) => handleSubmitFlag(stand.id, flag)} />
                )}
                <LabStandToolbar
                  running={running}
                  busy={isAnyBusy}
                  startLabel="Запустить и проверить"
                  onStart={() => handleStart(stand.id)}
                  onStop={() => running && handleStop(running.id)}
                />
                <div className="review-stand-card-actions">
                  <button type="button" disabled={isAnyBusy} onClick={() => void handleApprove(stand.id)}>
                    Одобрить
                  </button>
                </div>
                <form
                  className="review-stand-reject-form"
                  onSubmit={(event) => {
                    event.preventDefault();
                    void handleReject(stand.id);
                  }}
                >
                  <input
                    type="text"
                    placeholder="Причина отклонения"
                    disabled={isAnyBusy}
                    value={rejectDraft}
                    maxLength={500}
                    onChange={(event) =>
                      setRejectDrafts((current) => ({ ...current, [stand.id]: event.target.value }))
                    }
                  />
                  <button type="submit" className="secondary" disabled={isAnyBusy || !rejectDraft.trim()}>
                    Отклонить
                  </button>
                </form>
              </li>
            );
          })}
        </ul>
      )}
    </article>
  );
}

function PeerStandsSection({
  module,
  assignments,
  instanceByStandId,
  busy,
  onStart,
  onStop,
  onSubmitFlag
}: {
  module?: ModuleOption;
  assignments: PeerStandAssignment[];
  instanceByStandId: Map<string, UserPentestStandInstance>;
  busy: boolean;
  onStart: (standId: string) => void | Promise<void>;
  onStop: (instanceId: string) => void | Promise<void>;
  onSubmitFlag: (standId: string, flag: string) => void | Promise<void>;
}) {
  if (!module) {
    return (
      <article className="card">
        <h2>Задачи студентов</h2>
        <EmptyState>Выберите модуль курса, чтобы увидеть назначенные стенды.</EmptyState>
      </article>
    );
  }
  return (
    <article className="card">
      <div className="peer-section-header">
        <div>
          <h2>Задачи студентов</h2>
          <p className="muted">
            Стенды от других студентов из модуля «{module.title}». Найдите и сдайте флаг каждого назначенного стенда.
          </p>
        </div>
      </div>
      {assignments.length === 0 ? (
        <EmptyState>Назначенных стендов пока нет. Они появятся после распределения куратором или администратором.</EmptyState>
      ) : (
        <div className="peer-stands-grid">
          {assignments.map((assignment) => {
            const running = instanceByStandId.get(assignment.standId) ?? null;
            return (
              <article key={assignment.assignmentId} className="lab-task-panel lab-task-panel-minimal peer-stand-card">
                <div className="lab-stand-header">
                  <strong>{assignment.standDisplayName}</strong>
                  {assignment.solved ? (
                    <span className="status-pill status-pill--success" style={{ marginLeft: "0.5rem" }}>
                      Решено
                    </span>
                  ) : (
                    <span className="status-pill" style={{ marginLeft: "0.5rem" }}>В процессе</span>
                  )}
                </div>
                {assignment.authorDisplayName && (
                  <p className="muted">Автор: {assignment.authorDisplayName}</p>
                )}
                {assignment.standDescription && <p>{assignment.standDescription}</p>}
                {running && !assignment.solved && (
                  <LabStandFlagForm busy={busy} onSubmit={(flag) => onSubmitFlag(assignment.standId, flag)} />
                )}
                <LabStandToolbar
                  running={running}
                  busy={busy}
                  onStart={() => onStart(assignment.standId)}
                  onStop={() => running && onStop(running.id)}
                />
              </article>
            );
          })}
        </div>
      )}
    </article>
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

















