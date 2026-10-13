"use client";

import { useParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import WorkflowGuide from "@/components/WorkflowGuide";
import WorkflowStepper from "@/components/WorkflowStepper";
import {
  ACTION_LABEL, api, ROLE_LABEL, STATUS_LABEL, TYPE_LABEL,
  type ApprovalRequest, type ContractDetail, type DocumentView, type User,
} from "@/lib/api";

export default function ContractDetailPage() {
  const params = useParams<{ id: string }>();
  const [detail, setDetail] = useState<ContractDetail | null>(null);
  const [approvals, setApprovals] = useState<ApprovalRequest[]>([]);
  const [docs, setDocs] = useState<DocumentView[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [me, setMe] = useState<User | null>(null);
  const [approverIds, setApproverIds] = useState<string[]>([]);
  const [comment, setComment] = useState("");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [saving, setSaving] = useState(false);
  const [edit, setEdit] = useState({ name: "", startDate: "", endDate: "", amount: "", autoRenewal: false, noticeDays: "" });

  async function reload() {
    const id = params.id;
    const d = await api<ContractDetail>(`/api/contracts/${id}`);
    setDetail(d);
    setApprovals(await api<ApprovalRequest[]>(`/api/contracts/${id}/approval-requests`));
    setDocs(await api<DocumentView[]>(`/api/contracts/${id}/documents`));
    setEdit({
      name: d.contract.name,
      startDate: d.contract.startDate || "",
      endDate: d.contract.endDate || "",
      amount: d.contract.amount == null ? "" : String(d.contract.amount),
      autoRenewal: d.contract.autoRenewal,
      noticeDays: d.contract.noticeDays == null ? "" : String(d.contract.noticeDays),
    });
  }

  useEffect(() => {
    reload().catch((e) => setError(e.message));
    api<User>("/api/auth/me").then(setMe).catch(() => undefined);
    api<User[]>("/api/users").then((list) => {
      setUsers(list);
      const first = list.find((u) => u.role === "APPROVER");
      const second = list.find((u) => u.role === "LEGAL") || list.find((u) => u.role === "ADMIN");
      const defaults = [first, second].filter((u): u is User => !!u).map((u) => String(u.id));
      setApproverIds(defaults.length ? [...new Set(defaults)] : []);
    }).catch(() => undefined);
    if (typeof window !== "undefined" && new URLSearchParams(window.location.search).get("phase") === "draft") {
      setInfo("下書きを起案しました。内容を確認し、承認ステップを指定して申請してください。");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.id]);

  async function run(okMsg: string, action: () => Promise<unknown>) {
    setError("");
    setInfo("");
    setSaving(true);
    try {
      await action();
      await reload();
      setComment("");
      setInfo(okMsg);
    } catch (err) {
      setError(err instanceof Error ? err.message : "処理に失敗しました");
    } finally {
      setSaving(false);
    }
  }

  const open = useMemo(
    () => approvals.find((a) => a.status === "REQUESTED" || a.status === "IN_PROGRESS"),
    [approvals],
  );
  const latestReturn = approvals.find((a) => a.status === "RETURNED");
  const approvers = users.filter((u) => ["APPROVER", "ADMIN", "LEGAL"].includes(u.role));
  const canDecide = !!(open && me && (me.role === "ADMIN" || me.id === open.currentApproverId));
  const canCancel = !!(open && me && (me.role === "ADMIN" || me.id === open.requestedBy));

  if (!detail) return <p>読み込み中...</p>;
  const c = detail.contract;
  const draft = c.status === "DRAFT";
  const frozen = open
    ? detail.versions.find((v) => v.id === open.versionId)
    : detail.versions[0];
  let frozenSnap: Record<string, unknown> | null = null;
  if (frozen?.snapshotJson) {
    try { frozenSnap = JSON.parse(frozen.snapshotJson) as Record<string, unknown>; } catch { frozenSnap = null; }
  }

  return (
    <div>
      <h2>{c.name} <span className={`badge ${c.status}`}>{STATUS_LABEL[c.status] || c.status}</span></h2>
      <WorkflowGuide />
      <WorkflowStepper status={c.status} />
      {error && <p className="err">{error}</p>}
      {info && <p className="muted">{info}</p>}

      <div className="card" style={{ marginBottom: 16 }}>
        <h3>契約内容</h3>
        {draft ? (
          <div className="grid2">
            <div className="field"><label>名称</label><input value={edit.name} onChange={(e) => setEdit({ ...edit, name: e.target.value })} /></div>
            <div className="field"><label>金額</label><input value={edit.amount} onChange={(e) => setEdit({ ...edit, amount: e.target.value })} /></div>
            <div className="field"><label>開始日</label><input type="date" value={edit.startDate} onChange={(e) => setEdit({ ...edit, startDate: e.target.value })} /></div>
            <div className="field"><label>終了日</label><input type="date" value={edit.endDate} onChange={(e) => setEdit({ ...edit, endDate: e.target.value })} /></div>
            <div className="field"><label>解約予告日数</label><input value={edit.noticeDays} onChange={(e) => setEdit({ ...edit, noticeDays: e.target.value })} /></div>
            <label style={{ alignSelf: "end" }}>
              <input type="checkbox" checked={edit.autoRenewal} onChange={(e) => setEdit({ ...edit, autoRenewal: e.target.checked })} /> 自動更新
            </label>
          </div>
        ) : (
          <>
            <p>契約番号: {c.contractNo} / 種別: {TYPE_LABEL[c.type] || c.type}{c.parentContractId ? ` / 親契約 #${c.parentContractId}` : ""}</p>
            <p>期間: {c.startDate || "—"} 〜 {c.endDate || "—"} / 金額: {c.amount == null ? "—" : `${Number(c.amount).toLocaleString()} ${c.currencyCode}`}</p>
            <p>自動更新: {c.autoRenewal ? "あり" : "なし"} / 予告日数: {c.noticeDays ?? "—"}</p>
          </>
        )}
        <p>当事者: {detail.parties.map((p) => `${p.legalName}（${ROLE_LABEL[p.role] || p.role}）`).join("、") || "未設定"}</p>
        {draft && (
          <button className="btn secondary" disabled={saving} onClick={() => run("下書きを更新し、新しい版を確定しました。", () => api(`/api/contracts/${c.id}`, {
            method: "PUT",
            body: JSON.stringify({
              rowVersion: c.rowVersion,
              name: edit.name,
              type: c.type,
              parentContractId: c.parentContractId,
              startDate: edit.startDate || null,
              endDate: edit.endDate || null,
              amount: edit.amount ? edit.amount : null,
              currencyCode: c.currencyCode,
              autoRenewal: edit.autoRenewal,
              noticeDays: edit.noticeDays ? Number(edit.noticeDays) : null,
              parties: detail.parties.map((p) => ({ partyId: p.partyId, role: p.role })),
            }),
          }))}>内容を保存して版を更新</button>
        )}
        {latestReturn && c.status === "DRAFT" && (
          <p className="muted" style={{ marginTop: 8 }}>
            差戻し理由: {latestReturn.histories.filter((h) => h.action === "RETURN").at(-1)?.commentText || "—"}
            。再申請時は更新後の版が承認対象になります。
          </p>
        )}
      </div>

      {draft && (
        <div className="card" style={{ marginBottom: 16 }}>
          <h3>② 承認申請</h3>
          <p className="muted">承認対象はいまの最新版に固定されます。申請後は本文を差し替えできません。</p>
          <div className="steps-box">
            {approverIds.map((id, i) => (
              <div className="row" key={`${id}-${i}`} style={{ marginBottom: 8 }}>
                <label style={{ minWidth: 90 }}>ステップ {i + 1}</label>
                <select value={id} onChange={(e) => {
                  const next = [...approverIds];
                  next[i] = e.target.value;
                  setApproverIds(next);
                }}>
                  {approvers.map((u) => <option key={u.id} value={u.id}>{u.displayName}（{ROLE_LABEL[u.role] || u.role}）</option>)}
                </select>
                {approverIds.length > 1 && (
                  <button type="button" className="btn secondary" onClick={() => setApproverIds(approverIds.filter((_, j) => j !== i))}>外す</button>
                )}
              </div>
            ))}
            <button type="button" className="btn secondary" onClick={() => {
              const extra = approvers.find((u) => !approverIds.includes(String(u.id)));
              if (extra) setApproverIds([...approverIds, String(extra.id)]);
            }}>承認ステップを追加</button>
          </div>
          <textarea style={{ marginTop: 12 }} placeholder="申請コメント" value={comment} onChange={(e) => setComment(e.target.value)} />
          <div className="row" style={{ marginTop: 12 }}>
            <button className="btn" disabled={saving || approverIds.length === 0} onClick={() => run("承認申請しました。契約は審査中です。", () => api(`/api/contracts/${c.id}/approval-requests`, {
              method: "POST",
              headers: { "Idempotency-Key": `submit-${c.id}-${c.rowVersion}-${Date.now()}` },
              body: JSON.stringify({ approverIds: approverIds.map(Number), comment }),
            }))}>この版で承認申請する</button>
          </div>
        </div>
      )}

      {open && (
        <div className="card" style={{ marginBottom: 16 }}>
          <h3>審査中の申請 #{open.id}（版 {open.versionNo ?? open.versionId}）</h3>
          <p className="muted">申請者: {open.requestedByName || open.requestedBy} / 状態: {STATUS_LABEL[open.status] || open.status}</p>
          <ol>
            {open.steps.map((s) => (
              <li key={s.id}>
                ステップ {s.stepOrder}: {s.approverName || s.approverId} — <span className={`badge ${s.status}`}>{STATUS_LABEL[s.status] || s.status}</span>
                {open.currentApproverId === s.approverId && s.status === "PENDING" ? " ← いまの決裁者" : ""}
              </li>
            ))}
          </ol>
          {canDecide ? (
            <>
              <textarea placeholder="判定コメント（差戻し・却下は必須）" value={comment} onChange={(e) => setComment(e.target.value)} />
              <div className="row" style={{ marginTop: 12 }}>
                <button className="btn" disabled={saving} onClick={() => run("承認しました。", () => api(`/api/approval-requests/${open.id}/approve`, { method: "POST", body: JSON.stringify({ comment }) }))}>承認して次へ</button>
                <button className="btn secondary" disabled={saving} onClick={() => run("差し戻しました。起案者が再編集できます。", () => api(`/api/approval-requests/${open.id}/return`, { method: "POST", body: JSON.stringify({ comment }) }))}>差戻し</button>
                <button className="btn danger" disabled={saving} onClick={() => run("却下しました。", () => api(`/api/approval-requests/${open.id}/reject`, { method: "POST", body: JSON.stringify({ comment }) }))}>却下</button>
              </div>
            </>
          ) : (
            <p className="muted">このステップの承認者、または管理者だけが判定できます。</p>
          )}
          {canCancel && (
            <button className="btn secondary" style={{ marginTop: 8 }} disabled={saving} onClick={() => run("申請を取り下げました。", () => api(`/api/approval-requests/${open.id}/cancel`, { method: "POST", body: JSON.stringify({ comment }) }))}>申請を取下げ（下書きに戻す）</button>
          )}
        </div>
      )}

      {c.status === "APPROVED" && (
        <div className="card" style={{ marginBottom: 16 }}>
          <h3>③ 承認後</h3>
          <p className="muted">電子署名連携は未接続です。権限者が締結を確認するか、直接有効化できます。</p>
          <div className="row">
            <button className="btn secondary" disabled={saving} onClick={() => run("締結待ちにしました。", () => api(`/api/contracts/${c.id}/sign`, { method: "POST" }))}>締結待ちにする</button>
            <button className="btn" disabled={saving} onClick={() => run("契約を有効化しました。", () => api(`/api/contracts/${c.id}/activate`, { method: "POST" }))}>有効化</button>
          </div>
        </div>
      )}
      {c.status === "PENDING_SIGNATURE" && (
        <div className="card" style={{ marginBottom: 16 }}>
          <button className="btn" disabled={saving} onClick={() => run("契約を有効化しました。", () => api(`/api/contracts/${c.id}/activate`, { method: "POST" }))}>締結を確認して有効化</button>
        </div>
      )}

      <div className="card" style={{ marginBottom: 16 }}>
        <h3>文書（版に紐づけ）</h3>
        {draft || ["APPROVED", "PENDING_SIGNATURE", "ACTIVE"].includes(c.status) ? (
          <input type="file" onChange={(e) => {
            const file = e.target.files?.[0];
            if (!file) return;
            const fd = new FormData();
            fd.append("file", file);
            run("文書を登録しました。", () => api(`/api/contracts/${c.id}/documents`, { method: "POST", body: fd }));
          }} />
        ) : <p className="muted">審査中は文書の差し替えはできません。</p>}
        {docs.length === 0 ? <p className="muted">未登録です。</p> : (
          <ul>{docs.map((d) => <li key={d.id}>{d.fileName} / 版ID {d.versionId} / {d.uploadedAt}</li>)}</ul>
        )}
      </div>

      <div className="card">
        <h3>版履歴（承認対象は申請時の版）</h3>
        <ul>
          {detail.versions.map((v) => (
            <li key={v.id}>
              版 {v.versionNo} / {v.createdAt} / ID {v.id}
              {open?.versionId === v.id ? " ← この申請の固定版" : ""}
            </li>
          ))}
        </ul>
        {frozenSnap && (
          <p className="muted">
            固定版の内容: {String(frozenSnap.name || "")} / {String(frozenSnap.startDate || "—")} 〜 {String(frozenSnap.endDate || "—")} /
            {" "}{frozenSnap.amount == null ? "—" : String(frozenSnap.amount)} {String(frozenSnap.currencyCode || "")}
          </p>
        )}
        <h3>承認履歴</h3>
        {approvals.length === 0 ? <p className="muted">まだ申請はありません。</p> : approvals.map((a) => (
          <div key={a.id} style={{ marginBottom: 12 }}>
            <p>申請 #{a.id} / {STATUS_LABEL[a.status] || a.status} / 対象版 {a.versionNo ?? a.versionId} / {a.requestedByName}</p>
            <ul>
              {a.histories.map((h) => (
                <li key={h.id}>{h.actedAt} {h.actorName} {ACTION_LABEL[h.action] || h.action}{h.commentText ? ` — ${h.commentText}` : ""}</li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </div>
  );
}
