"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api, STATUS_LABEL, type InboxItem } from "@/lib/api";

export default function ApprovalsPage() {
  const [items, setItems] = useState<InboxItem[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api<InboxItem[]>("/api/approval-requests/inbox")
      .then(setItems)
      .catch((e) => setError(e.message));
  }, []);

  return (
    <div>
      <h2>承認待ち</h2>
      <p className="muted">いま決裁番のステップ（PENDING）だけが表示されます。後続ステップは待ち（WAITING）のため、先回りはできません。</p>
      {error && <p className="err">{error}</p>}
      <div className="card">
        {items.length === 0 ? <p className="muted">処理待ちの申請はありません。</p> : (
          <table>
            <thead>
              <tr><th>申請</th><th>契約</th><th>申請者</th><th>状態</th><th>あなたのステップ</th></tr>
            </thead>
            <tbody>
              {items.map((i) => (
                <tr key={i.requestId}>
                  <td>#{i.requestId}</td>
                  <td><Link href={`/contracts/${i.contractId}`}>{i.contractNo || `契約 ${i.contractId}`} {i.contractName || ""}</Link></td>
                  <td>{i.requestedByName || "—"}</td>
                  <td><span className={`badge ${i.requestStatus}`}>{STATUS_LABEL[i.requestStatus] || i.requestStatus}</span></td>
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
