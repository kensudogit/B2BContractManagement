const TOKEN_KEY = "contracts_access_token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  try {
    return localStorage.getItem(TOKEN_KEY) || readCookie(TOKEN_KEY);
  } catch {
    return readCookie(TOKEN_KEY);
  }
}

export function setToken(token: string | null) {
  if (typeof window === "undefined") return;
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* ignore */
  }
  document.cookie = token
    ? `${TOKEN_KEY}=${token}; Path=/; Max-Age=${60 * 60 * 12}; SameSite=Lax`
    : `${TOKEN_KEY}=; Path=/; Max-Age=0`;
}

function readCookie(name: string): string | null {
  const hit = document.cookie.split("; ").find((row) => row.startsWith(`${name}=`));
  return hit ? decodeURIComponent(hit.slice(name.length + 1)) : null;
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const res = await fetch(path, { ...init, headers });
  if (res.status === 401) {
    setToken(null);
    if (typeof window !== "undefined" && !path.includes("/api/auth/login")) {
      window.location.href = "/login";
    }
  }
  if (!res.ok) {
    let detail = `${res.status} ${res.statusText}`;
    try {
      const body = await res.json();
      detail = body.detail || body.message || detail;
    } catch {
      /* ignore */
    }
    throw new Error(detail);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export type User = { id: number; username: string; displayName: string; role: string };
export type ContractSummary = {
  id: number;
  contractNo: string;
  name: string;
  type: string;
  status: string;
  startDate: string | null;
  endDate: string | null;
  amount: number | null;
  currencyCode: string;
  autoRenewal: boolean;
  noticeDays?: number | null;
  parentContractId: number | null;
  rowVersion: number;
};
export type Party = { id: number; legalName: string; corporateNumber: string | null };
export type Page<T> = { content: T[]; totalElements: number; totalPages: number; number: number };
export type ContractDetail = {
  contract: ContractSummary;
  parties: { partyId: number; legalName: string; role: string }[];
  versions: { id: number; versionNo: number; createdAt: string; snapshotJson?: string }[];
};
export type ApprovalRequest = {
  id: number;
  contractId: number;
  contractNo?: string;
  contractName?: string;
  versionId: number;
  versionNo?: number;
  status: string;
  requestedBy: number;
  requestedByName?: string;
  requestedAt: string;
  currentStepOrder?: number | null;
  currentApproverId?: number | null;
  steps: { id: number; stepOrder: number; approverId: number; approverName?: string; status: string }[];
  histories: { id: number; actorId: number; actorName?: string; action: string; commentText: string | null; actedAt: string }[];
};
export type InboxItem = {
  requestId: number;
  contractId: number;
  contractNo?: string;
  contractName?: string;
  versionId: number;
  requestStatus: string;
  stepOrder: number;
  requestedByName?: string;
};
export type DocumentView = {
  id: number;
  contractId: number;
  versionId: number;
  fileName: string;
  sha256: string;
  contentType: string;
  uploadedAt: string;
};

export const STATUS_LABEL: Record<string, string> = {
  DRAFT: "下書き",
  UNDER_REVIEW: "審査中",
  APPROVED: "承認済",
  PENDING_SIGNATURE: "締結待ち",
  ACTIVE: "有効",
  EXPIRED: "満了",
  TERMINATED: "解約",
  REJECTED: "却下",
  REQUESTED: "申請中",
  IN_PROGRESS: "審査進行中",
  RETURNED: "差戻し",
  CANCELLED: "取下げ",
  WAITING: "待ち",
  PENDING: "処理待ち",
};

export const TYPE_LABEL: Record<string, string> = {
  MASTER: "基本契約",
  SPOT: "個別契約",
  NDA: "NDA",
};

export const ACTION_LABEL: Record<string, string> = {
  SUBMIT: "申請",
  APPROVE: "承認",
  RETURN: "差戻し",
  REJECT: "却下",
  CANCEL: "取下げ",
};

export const ROLE_LABEL: Record<string, string> = {
  CUSTOMER: "発注者",
  VENDOR: "受注者",
  PARTNER: "パートナー",
  ADMIN: "管理者",
  APPROVER: "承認者",
  LEGAL: "法務",
  SALES: "営業",
};

export const WORKFLOW_STEPS = [
  { key: "DRAFT", label: "① 起案" },
  { key: "UNDER_REVIEW", label: "② 審査" },
  { key: "APPROVED", label: "③ 承認" },
  { key: "PENDING_SIGNATURE", label: "④ 締結" },
  { key: "ACTIVE", label: "⑤ 有効" },
] as const;

export function workflowIndex(status: string): number {
  if (status === "DRAFT") return 0;
  if (status === "UNDER_REVIEW") return 1;
  if (status === "APPROVED") return 2;
  if (status === "PENDING_SIGNATURE") return 3;
  if (status === "ACTIVE") return 4;
  return -1;
}
