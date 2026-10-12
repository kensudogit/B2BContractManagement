"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";

type Inbox = { requestId: number; contractId: number; versionId: number; requestStatus: string; stepOrder: number };

export default function ApprovalsPage() {
  const [items, setItems] = useState<Inbox[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api<Inbox[]>("/api/approval-requests/inbox")
      .then(setItems)
      .catch((e) => setError(e.message));
  }, []);

  return (
    <div>
      <h2>承認待ち</h2>
      {error && <p className="err">{error}</p>}
      <div className="card">
        {items.length === 0 ? <p className="muted">処理待ちの申請はありません。</p> : (
          <table>
            <thead><tr><th>申請</th><th>契約</th><th>版</th><th>状態</th><th>ステップ</th></tr></thead>
            <tbody>
              {items.map((i) => (
                <tr key={i.requestId}>
                  <td>#{i.requestId}</td>
                  <td><Link href={`/contracts/${i.contractId}`}>契約 {i.contractId}</Link></td>
                  <td>{i.versionId}</td>
                  <td><span className={`badge ${i.requestStatus}`}>{i.requestStatus}</span></td>
                  <td>{i.stepOrder}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
