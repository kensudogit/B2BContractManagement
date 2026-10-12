"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api, type Party } from "@/lib/api";

export default function NewContractPage() {
  const router = useRouter();
  const [parties, setParties] = useState<Party[]>([]);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    contractNo: "",
    name: "",
    type: "MASTER",
    startDate: "",
    endDate: "",
    amount: "",
    currencyCode: "JPY",
    autoRenewal: false,
    noticeDays: "30",
    partyId: "",
    role: "CUSTOMER",
  });

  useEffect(() => {
    api<Party[]>("/api/parties").then(setParties).catch((e) => setError(e.message));
  }, []);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    try {
      const created = await api<{ contract: { id: number } }>("/api/contracts", {
        method: "POST",
        body: JSON.stringify({
          contractNo: form.contractNo,
          name: form.name,
          type: form.type,
          startDate: form.startDate || null,
          endDate: form.endDate || null,
          amount: form.amount ? form.amount : null,
          currencyCode: form.currencyCode,
          autoRenewal: form.autoRenewal,
          noticeDays: form.noticeDays ? Number(form.noticeDays) : null,
          parties: form.partyId ? [{ partyId: Number(form.partyId), role: form.role }] : [],
        }),
      });
      router.push(`/contracts/${created.contract.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "登録に失敗しました");
    }
  }

  return (
    <div>
      <h2>新規契約</h2>
      <form className="card" onSubmit={onSubmit}>
        <div className="grid2">
          <div className="field"><label>契約番号</label><input required value={form.contractNo} onChange={(e) => setForm({ ...form, contractNo: e.target.value })} /></div>
          <div className="field"><label>名称</label><input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
          <div className="field">
            <label>種別</label>
            <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
              <option value="MASTER">基本契約</option>
              <option value="SPOT">個別契約</option>
              <option value="NDA">NDA</option>
            </select>
          </div>
          <div className="field"><label>金額</label><input value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} placeholder="1200000.00" /></div>
          <div className="field"><label>開始日</label><input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></div>
          <div className="field"><label>終了日</label><input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></div>
          <div className="field"><label>解約予告日数</label><input value={form.noticeDays} onChange={(e) => setForm({ ...form, noticeDays: e.target.value })} /></div>
          <div className="field">
            <label>取引先</label>
            <select value={form.partyId} onChange={(e) => setForm({ ...form, partyId: e.target.value })}>
              <option value="">未選択</option>
              {parties.map((p) => <option key={p.id} value={p.id}>{p.legalName}</option>)}
            </select>
          </div>
        </div>
        <label style={{ marginTop: 12 }}>
          <input type="checkbox" checked={form.autoRenewal} onChange={(e) => setForm({ ...form, autoRenewal: e.target.checked })} /> 自動更新
        </label>
        {error && <p className="err">{error}</p>}
        <div className="row" style={{ marginTop: 16 }}>
          <button className="btn" type="submit">下書き保存</button>
        </div>
      </form>
    </div>
  );
}
