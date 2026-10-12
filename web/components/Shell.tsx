"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { getToken, setToken } from "@/lib/api";

const LINKS = [
  { href: "/contracts", label: "契約台帳" },
  { href: "/contracts/new", label: "新規契約" },
  { href: "/approvals", label: "承認待ち" },
  { href: "/expiring", label: "期限一覧" },
  { href: "/parties", label: "取引先" },
];

export default function Shell({ children }: { children: React.ReactNode }) {
  const path = usePathname();
  const router = useRouter();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (!getToken()) router.replace("/login");
    else setReady(true);
  }, [router]);

  if (path === "/login") return <>{children}</>;
  if (!ready) return <div className="main">読み込み中...</div>;

  return (
    <div className="shell">
      <aside className="side">
        <h1>企業間契約管理</h1>
        {LINKS.map((l) => (
          <Link key={l.href} href={l.href} className={path.startsWith(l.href) ? "active" : ""}>
            {l.label}
          </Link>
        ))}
        <button
          className="btn secondary"
          style={{ marginTop: 24, width: "100%" }}
          onClick={() => {
            setToken(null);
            router.replace("/login");
          }}
        >
          ログアウト
        </button>
      </aside>
      <main className="main">{children}</main>
    </div>
  );
}
