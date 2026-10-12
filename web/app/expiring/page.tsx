"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api, type ContractSummary } from "@/lib/api";

export default function ExpiringPage() {
  const [items, setItems] = useState<ContractSummary[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api<ContractSummary[]>("/api/contracts/expiring?days=90")
      .then(setItems)
      .catch((e) => setError(e.message));
  }, []);

  return (
    <div>
      <h2>期限一覧（90日以内）</h2>
      {error && <p className="err">{error}</p>}
      <div className="card">
        {items.length === 0 ? <p className="muted">直近で期限を迎える有効契約はありません。</p> : (
          <table>
            <thead><tr><th>契約番号</th><th>名称</th><th>終了日</th><th>自動更新</th></tr></thead>
            <tbody>
              {items.map((c) => (
                <tr key={c.id}>
                  <td><Link href={`/contracts/${c.id}`}>{c.contractNo}</Link></td>
                  <td>{c.name}</td>
                  <td>{c.endDate}</td>
                  <td>{c.autoRenewal ? "あり" : "なし"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
