"use client";

import { STATUS_LABEL, WORKFLOW_STEPS, workflowIndex } from "@/lib/api";

export default function WorkflowStepper({ status }: { status: string }) {
  const current = workflowIndex(status);
  const terminal = ["REJECTED", "EXPIRED", "TERMINATED"].includes(status);
  return (
    <div className="card" style={{ marginBottom: 16 }}>
      <ol className="stepper">
        {WORKFLOW_STEPS.map((s, i) => {
          const state = current < 0 ? "" : i < current ? "done" : i === current ? "current" : "";
          return (
            <li key={s.key} className={state}>
              <span className="dot">{i + 1}</span>
              <span>{s.label}</span>
            </li>
          );
        })}
      </ol>
      <p className="muted" style={{ margin: "12px 0 0" }}>
        いまの契約状態: <span className={`badge ${status}`}>{STATUS_LABEL[status] || status}</span>
        {terminal ? " （終了状態。満了と解約は区別されます）" : " ／ 契約状態・承認状態・文書版は別管理です"}
      </p>
    </div>
  );
}
