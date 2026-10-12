"use client";

import { FormEvent, useEffect, useState } from "react";
import { api, type Party } from "@/lib/api";

export default function PartiesPage() {
  const [items, setItems] = useState<Party[]>([]);
  const [legalName, setLegalName] = useState("");
  const [corporateNumber, setCorporateNumber] = useState("");
  const [error, setError] = useState("");

  async function load() {
    setItems(await api<Party[]>("/api/parties"));
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, []);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    try {
      await api("/api/parties", {
        method: "POST",
        body: JSON.stringify({ legalName, corporateNumber }),
      });
      setLegalName("");
      setCorporateNumber("");
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "登録に失敗しました");
    }
  }

  return (
    <div>
      <h2>取引先</h2>
      <form className="card row" onSubmit={onSubmit} style={{ marginBottom: 16 }}>
        <div className="field"><label>商号</label><input required value={legalName} onChange={(e) => setLegalName(e.target.value)} /></div>
        <div className="field"><label>法人番号</label><input value={corporateNumber} onChange={(e) => setCorporateNumber(e.target.value)} /></div>
        <button className="btn" type="submit">登録</button>
      </form>
      {error && <p className="err">{error}</p>}
      <div className="card">
        <table>
          <thead><tr><th>ID</th><th>商号</th><th>法人番号</th></tr></thead>
          <tbody>
            {items.map((p) => (
              <tr key={p.id}><td>{p.id}</td><td>{p.legalName}</td><td>{p.corporateNumber || "—"}</td></tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
