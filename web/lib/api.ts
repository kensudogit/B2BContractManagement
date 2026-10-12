const TOKEN_KEY = "contracts_access_token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
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
  parentContractId: number | null;
  rowVersion: number;
};
export type Party = { id: number; legalName: string; corporateNumber: string | null };
export type Page<T> = { content: T[]; totalElements: number; totalPages: number; number: number };
