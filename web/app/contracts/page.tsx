"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api, type ContractSummary, type Page } from "@/lib/api";

export default function ContractsPage() {
  const [q, setQ] = useState("");
  const [status, setStatus] = useState("");
  const [data, setData] = useState<Page<ContractSummary> | null>(null);
  const [error, setError] = useState("");

  async function load() {
    setError("");
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    if (status) params.set("status", status);
    try {
      setData(await api<Page<ContractSummary>>(`/api/contracts?${params}`));
    } catch (err) {
      setError(err instanceof Error ? err.message : "取得に失敗しました");
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <div className="row" style={{ justifyContent: "space-between" }}>
        <h2>契約台帳</h2>
        <Link className="btn" href="/contracts/new">新規登録</Link>
      </div>
      <div className="card" style={{ marginBottom: 16 }}>
        <div className="row">
          <div className="field">
            <label>検索</label>
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="契約番号・名称" />
          </div>
          <div className="field">
            <label>状態</label>
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">すべて</option>
              {["DRAFT","UNDER_REVIEW","APPROVED","PENDING_SIGNATURE","ACTIVE","EXPIRED","TERMINATED","REJECTED"].map((s) => (
                <option key={s}>{s}</option>
              ))}
            </select>
          </div>
          <button className="btn" onClick={load}>検索</button>
        </div>
      </div>
      {error && <p className="err">{error}</p>}
      <div className="card">
        {!data || data.content.length === 0 ? (
          <p className="muted">該当する契約がありません。</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>契約番号</th><th>名称</th><th>種別</th><th>状態</th><th>期間</th><th>金額</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((c) => (
                <tr key={c.id}>
                  <td><Link href={`/contracts/${c.id}`}>{c.contractNo}</Link></td>
                  <td>{c.name}</td>
                  <td>{c.type}</td>
                  <td><span className={`badge ${c.status}`}>{c.status}</span></td>
                  <td>{c.startDate || "—"} 〜 {c.endDate || "—"}</td>
                  <td>{c.amount == null ? "—" : `${c.amount.toLocaleString()} ${c.currencyCode}`}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <p className="muted">{data ? `全 ${data.totalElements} 件` : ""}</p>
      </div>
    </div>
  );
}
