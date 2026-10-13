"use client";

import { FormEvent, useState } from "react";
import { api, setToken } from "@/lib/api";

export default function LoginPage() {
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("password");
  const [error, setError] = useState("");

  async function doLogin() {
    setError("");
    try {
      const res = await api<{ accessToken: string }>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
      });
      setToken(res.accessToken);
      window.location.assign("/contracts");
    } catch (err) {
      setError(err instanceof Error ? err.message : "ログインに失敗しました");
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    await doLogin();
  }

  return (
    <div className="login card">
      <h2>企業間契約管理</h2>
      <p className="muted">JWT でログインします。デモ: admin / approver / legal / sales （パスワードは password）</p>
      <form onSubmit={onSubmit}>
        <div className="field" style={{ margin: "12px 0" }}>
          <label>ユーザー名</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} />
        </div>
        <div className="field" style={{ marginBottom: 16 }}>
          <label>パスワード</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        {error && <p className="err">{error}</p>}
        <button className="btn" type="submit" onClick={() => { void doLogin(); }}>ログイン</button>
      </form>
    </div>
  );
}
