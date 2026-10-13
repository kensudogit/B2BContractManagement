"use client";

import { FormEvent, useEffect, useState } from "react";
import WorkflowGuide from "@/components/WorkflowGuide";
import { api, ROLE_LABEL, type ContractSummary, type Party, type Page } from "@/lib/api";

const WIZARD = ["基本情報", "当事者", "確認して起案"];

type PartyRow = { partyId: string; role: string };

export default function NewContractPage() {
  const [step, setStep] = useState(0);
  const [parties, setParties] = useState<Party[]>([]);
  const [masters, setMasters] = useState<ContractSummary[]>([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    contractNo: "",
    name: "",
    type: "MASTER",
    parentContractId: "",
    startDate: "",
    endDate: "",
    amount: "",
    currencyCode: "JPY",
    autoRenewal: false,
    noticeDays: "30",
  });
  const [partyRows, setPartyRows] = useState<PartyRow[]>([{ partyId: "", role: "CUSTOMER" }]);

  useEffect(() => {
    api<Party[]>("/api/parties").then(setParties).catch((e) => setError(e.message));
    api<Page<ContractSummary>>("/api/contracts?status=ACTIVE&size=100")
      .then((p) => setMasters(p.content.filter((c) => c.type === "MASTER")))
      .catch(() => undefined);
  }, []);

  function validateStep(): string | null {
    if (step === 0) {
      if (!form.contractNo.trim() || !form.name.trim()) return "契約番号と名称は必須です";
      if (form.startDate && form.endDate && form.endDate < form.startDate) return "終了日は開始日以降にしてください";
      if (form.type === "SPOT" && masters.length > 0 && !form.parentContractId) return "個別契約は基本契約を選択してください";
    }
    if (step === 1) {
      const filled = partyRows.filter((r) => r.partyId);
      if (filled.length === 0) return "当事者を1件以上選択してください";
    }
    return null;
  }

  function next() {
    const msg = validateStep();
    if (msg) { setError(msg); return; }
    setError("");
    setStep((s) => Math.min(s + 1, 2));
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const msg = validateStep();
    if (msg) { setError(msg); return; }
    setSaving(true);
    setError("");
    try {
      const created = await api<{ contract: { id: number } }>("/api/contracts", {
        method: "POST",
        body: JSON.stringify({
          contractNo: form.contractNo.trim(),
          name: form.name.trim(),
          type: form.type,
          parentContractId: form.parentContractId ? Number(form.parentContractId) : null,
          startDate: form.startDate || null,
          endDate: form.endDate || null,
          amount: form.amount ? form.amount : null,
          currencyCode: form.currencyCode,
          autoRenewal: form.autoRenewal,
          noticeDays: form.noticeDays ? Number(form.noticeDays) : null,
          parties: partyRows.filter((r) => r.partyId).map((r) => ({ partyId: Number(r.partyId), role: r.role })),
        }),
      });
      window.location.assign(`/contracts/${created.contract.id}?phase=draft`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "登録に失敗しました");
      setSaving(false);
    }
  }

  const partyName = (id: string) => parties.find((p) => String(p.id) === id)?.legalName || "—";
  const typeLabel = form.type === "MASTER" ? "基本契約" : form.type === "SPOT" ? "個別契約" : "NDA";

  return (
    <div>
      <h2>新規契約（起案）</h2>
      <WorkflowGuide />
      <div className="wizard-nav">
        {WIZARD.map((label, i) => (
          <span key={label} className={i === step ? "on" : ""}>{i + 1}. {label}</span>
        ))}
      </div>
      <form className="card" onSubmit={onSubmit}>
        {step === 0 && (
          <div className="grid2">
            <div className="field"><label>契約番号</label><input required value={form.contractNo} onChange={(e) => setForm({ ...form, contractNo: e.target.value })} placeholder="M-2026-002" /></div>
            <div className="field"><label>名称</label><input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
            <div className="field">
              <label>種別</label>
              <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value, parentContractId: "" })}>
                <option value="MASTER">基本契約</option>
                <option value="SPOT">個別契約</option>
                <option value="NDA">NDA</option>
              </select>
            </div>
            {form.type === "SPOT" && (
              <div className="field">
                <label>親となる基本契約</label>
                <select value={form.parentContractId} onChange={(e) => setForm({ ...form, parentContractId: e.target.value })}>
                  <option value="">選択してください</option>
                  {masters.map((m) => <option key={m.id} value={m.id}>{m.contractNo} {m.name}</option>)}
                </select>
              </div>
            )}
            <div className="field"><label>開始日</label><input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></div>
            <div className="field"><label>終了日</label><input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></div>
            <div className="field"><label>金額</label><input value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} placeholder="1200000.00" /></div>
            <div className="field"><label>通貨</label><input value={form.currencyCode} onChange={(e) => setForm({ ...form, currencyCode: e.target.value })} /></div>
            <div className="field"><label>解約予告日数</label><input value={form.noticeDays} onChange={(e) => setForm({ ...form, noticeDays: e.target.value })} /></div>
            <label style={{ alignSelf: "end" }}>
              <input type="checkbox" checked={form.autoRenewal} onChange={(e) => setForm({ ...form, autoRenewal: e.target.checked })} /> 自動更新
            </label>
          </div>
        )}

        {step === 1 && (
          <div>
            <p className="muted">複数当事者は中間テーブルで保持します。役割は CUSTOMER / VENDOR / PARTNER など。</p>
            {partyRows.map((row, i) => (
              <div className="row" key={i} style={{ marginTop: 8 }}>
                <div className="field">
                  <label>取引先</label>
                  <select value={row.partyId} onChange={(e) => {
                    const nextRows = [...partyRows];
                    nextRows[i] = { ...row, partyId: e.target.value };
                    setPartyRows(nextRows);
                  }}>
                    <option value="">未選択</option>
                    {parties.map((p) => <option key={p.id} value={p.id}>{p.legalName}</option>)}
                  </select>
                </div>
                <div className="field">
                  <label>役割</label>
                  <select value={row.role} onChange={(e) => {
                    const nextRows = [...partyRows];
                    nextRows[i] = { ...row, role: e.target.value };
                    setPartyRows(nextRows);
                  }}>
                    <option value="CUSTOMER">{ROLE_LABEL.CUSTOMER}</option>
                    <option value="VENDOR">{ROLE_LABEL.VENDOR}</option>
                    <option value="PARTNER">{ROLE_LABEL.PARTNER}</option>
                  </select>
                </div>
                {partyRows.length > 1 && (
                  <button type="button" className="btn secondary" onClick={() => setPartyRows(partyRows.filter((_, j) => j !== i))}>削除</button>
                )}
              </div>
            ))}
            <button type="button" className="btn secondary" style={{ marginTop: 12 }} onClick={() => setPartyRows([...partyRows, { partyId: "", role: "CUSTOMER" }])}>当事者を追加</button>
          </div>
        )}

        {step === 2 && (
          <div>
            <p>この内容で下書き（DRAFT）を作成し、版スナップショットを確定します。承認申請は次の画面で行います。</p>
            <ul>
              <li>契約番号: {form.contractNo} / {typeLabel} / {form.name}</li>
              <li>期間: {form.startDate || "—"} 〜 {form.endDate || "—"} / {form.amount || "—"} {form.currencyCode}</li>
              <li>自動更新: {form.autoRenewal ? "あり" : "なし"} / 予告 {form.noticeDays || "—"} 日</li>
              <li>当事者: {partyRows.filter((r) => r.partyId).map((r) => `${partyName(r.partyId)}（${ROLE_LABEL[r.role] || r.role}）`).join("、")}</li>
            </ul>
          </div>
        )}

        {error && <p className="err">{error}</p>}
        <div className="row" style={{ marginTop: 16 }}>
          {step > 0 && <button type="button" className="btn secondary" onClick={() => { setError(""); setStep(step - 1); }}>戻る</button>}
          {step < 2 && <button type="button" className="btn" onClick={next}>次へ</button>}
          {step === 2 && <button className="btn" type="submit" disabled={saving}>{saving ? "保存中..." : "下書きとして起案する"}</button>}
        </div>
      </form>
    </div>
  );
}
