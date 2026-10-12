"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { api, type User } from "@/lib/api";

type Detail = {
  contract: {
    id: number; contractNo: string; name: string; type: string; status: string;
    startDate: string | null; endDate: string | null; amount: number | null;
    currencyCode: string; autoRenewal: boolean; rowVersion: number;
  };
  parties: { partyId: number; legalName: string; role: string }[];
  versions: { id: number; versionNo: number; createdAt: string }[];
};

type Approval = {
  id: number; status: string; versionId: number;
  steps: { id: number; stepOrder: number; approverId: number; status: string }[];
};

export default function ContractDetailPage() {
  const params = useParams<{ id: string }>();
  const [detail, setDetail] = useState<Detail | null>(null);
  const [approvals, setApprovals] = useState<Approval[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [approverId, setApproverId] = useState("");
  const [newEnd, setNewEnd] = useState("");
  const [error, setError] = useState("");
  const [comment, setComment] = useState("");

  async function reload() {
    const id = params.id;
    const d = await api<Detail>(`/api/contracts/${id}`);
    setDetail(d);
    setApprovals(await api<Approval[]>(`/api/contracts/${id}/approval-requests`));
  }

  useEffect(() => {
    reload().catch((e) => setError(e.message));
    api<User[]>("/api/users").then((list) => {
      setUsers(list);
      const ap = list.find((u) => u.role === "APPROVER");
      if (ap) setApproverId(String(ap.id));
    }).catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.id]);

  async function run(action: () => Promise<unknown>) {
    setError("");
    try {
      await action();
      await reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "処理に失敗しました");
    }
  }

  if (!detail) return <p>読み込み中...</p>;
  const c = detail.contract;
  const open = approvals.find((a) => a.status === "REQUESTED" || a.status === "IN_PROGRESS");

  return (
    <div>
      <h2>{c.name} <span className={`badge ${c.status}`}>{c.status}</span></h2>
      {error && <p className="err">{error}</p>}
      <div className="card" style={{ marginBottom: 16 }}>
        <p>契約番号: {c.contractNo} / 種別: {c.type} / 版: {c.rowVersion}</p>
        <p>期間: {c.startDate || "—"} 〜 {c.endDate || "—"} / 金額: {c.amount == null ? "—" : `${c.amount.toLocaleString()} ${c.currencyCode}`}</p>
        <p>自動更新: {c.autoRenewal ? "あり" : "なし"}</p>
        <p>当事者: {detail.parties.map((p) => `${p.legalName}（${p.role}）`).join("、") || "未設定"}</p>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <h3>操作</h3>
        {c.status === "DRAFT" && (
          <div className="row">
            <select value={approverId} onChange={(e) => setApproverId(e.target.value)}>
              {users.filter((u) => ["APPROVER", "ADMIN", "LEGAL"].includes(u.role)).map((u) => (
                <option key={u.id} value={u.id}>{u.displayName} ({u.role})</option>
              ))}
            </select>
            <input placeholder="申請コメント" value={comment} onChange={(e) => setComment(e.target.value)} />
            <button className="btn" onClick={() => run(() => api(`/api/contracts/${c.id}/approval-requests`, {
              method: "POST",
              headers: { "Idempotency-Key": `submit-${c.id}-${c.rowVersion}` },
              body: JSON.stringify({ approverIds: [Number(approverId)], comment }),
            }))}>承認申請</button>
          </div>
        )}
        {open && (
          <div className="row" style={{ marginTop: 12 }}>
            <input placeholder="判定コメント" value={comment} onChange={(e) => setComment(e.target.value)} />
            <button className="btn" onClick={() => run(() => api(`/api/approval-requests/${open.id}/approve`, { method: "POST", body: JSON.stringify({ comment }) }))}>承認</button>
            <button className="btn secondary" onClick={() => run(() => api(`/api/approval-requests/${open.id}/return`, { method: "POST", body: JSON.stringify({ comment }) }))}>差戻し</button>
            <button className="btn danger" onClick={() => run(() => api(`/api/approval-requests/${open.id}/reject`, { method: "POST", body: JSON.stringify({ comment }) }))}>却下</button>
          </div>
        )}
        {c.status === "APPROVED" && (
          <div className="row">
            <button className="btn secondary" onClick={() => run(() => api(`/api/contracts/${c.id}/sign`, { method: "POST" }))}>締結待ちにする</button>
            <button className="btn" onClick={() => run(() => api(`/api/contracts/${c.id}/activate`, { method: "POST" }))}>有効化</button>
          </div>
        )}
        {c.status === "PENDING_SIGNATURE" && (
          <button className="btn" onClick={() => run(() => api(`/api/contracts/${c.id}/activate`, { method: "POST" }))}>有効化</button>
        )}
        {c.status === "ACTIVE" && (
          <div className="row">
            <input type="date" value={newEnd} onChange={(e) => setNewEnd(e.target.value)} />
            <button className="btn secondary" onClick={() => run(() => api(`/api/contracts/${c.id}/renewals`, {
              method: "POST", body: JSON.stringify({ newEndDate: newEnd }),
            }))}>期間更新</button>
            <button className="btn danger" onClick={() => run(() => api(`/api/contracts/${c.id}/termination`, { method: "POST", body: JSON.stringify({ comment }) }))}>解約</button>
          </div>
        )}
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <h3>文書</h3>
        <input type="file" onChange={(e) => {
          const file = e.target.files?.[0];
          if (!file) return;
          const fd = new FormData();
          fd.append("file", file);
          run(() => api(`/api/contracts/${c.id}/documents`, { method: "POST", body: fd }));
        }} />
      </div>

      <div className="card">
        <h3>版履歴</h3>
        <ul>
          {detail.versions.map((v) => (
            <li key={v.id}>版 {v.versionNo} / {v.createdAt}</li>
          ))}
        </ul>
        <h3>承認履歴</h3>
        {approvals.length === 0 ? <p className="muted">まだ申請はありません。</p> : approvals.map((a) => (
          <p key={a.id}>申請 #{a.id} / {a.status} / 版ID {a.versionId}</p>
        ))}
      </div>
    </div>
  );
}
