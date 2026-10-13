"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { getToken, setToken } from "@/lib/api";

const LINKS = [
  { href: "/contracts", label: "契約台帳" },
  { href: "/contracts/new", label: "新規契約" },
  { href: "/approvals", label: "承認待ち" },
  { href: "/expiring", label: "期限一覧" },
  { href: "/parties", label: "取引先" },
];

function navActive(href: string, path: string): boolean {
  if (href === "/contracts") {
    return path === "/contracts" || /^\/contracts\/\d+/.test(path);
  }
  return path === href || path.startsWith(`${href}/`);
}

export default function Shell({ children }: { children: React.ReactNode }) {
  const path = usePathname();
  const [ready, setReady] = useState(path === "/login");

  useEffect(() => {
    if (path === "/login") {
      setReady(true);
      return;
    }
    if (!getToken()) {
      window.location.replace("/login");
      return;
    }
    setReady(true);
  }, [path]);

  if (path === "/login") return <>{children}</>;
  if (!ready) {
    return (
      <div className="login card">
        <h2>企業間契約管理</h2>
        <p className="muted">ログインが必要です。</p>
        <a className="btn" href="/login">ログインへ</a>
      </div>
    );
  }

  return (
    <div className="shell">
      <aside className="side">
        <h1>企業間契約管理</h1>
        {LINKS.map((l) => (
          <Link key={l.href} href={l.href} className={navActive(l.href, path) ? "active" : ""}>
            {l.label}
          </Link>
        ))}
        <button
          className="btn secondary"
          style={{ marginTop: 24, width: "100%" }}
          onClick={() => {
            setToken(null);
            window.location.replace("/login");
          }}
        >
          ログアウト
        </button>
      </aside>
      <main className="main">{children}</main>
    </div>
  );
}
