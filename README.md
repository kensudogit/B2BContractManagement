# 企業間契約管理（B2B Contract Management）

`b2b-contract-management-springboot` Skill に沿った MVP です。契約状態・承認状態・文書版を分けて管理します。

## スタック

- Java 21 / Spring Boot 3.3 / Spring Security (JWT) / Spring Data JPA
- PostgreSQL 16 / Flyway
- Next.js 14 / React / TypeScript
- 文書はローカルディレクトリ（`DOCUMENT_DIR`）。本番では S3 互換に差し替え可能

## 起動

```powershell
cd C:\devlop\B2BContractManagement
docker compose up -d db
cd api; mvn spring-boot:run
cd ..\web; npm install; npm run dev
```

または `.\start-all.ps1`

- UI: http://localhost:3040
- API: http://localhost:8040
- PostgreSQL: localhost:5435 / `contracts` / `contracts_app`

デモユーザー（パスワードはすべて `password`）:

| ユーザー | ロール |
|---|---|
| admin | ADMIN |
| approver | APPROVER |
| sales | SALES |

## 業務フロー

1. 営業が契約を下書き登録（`DRAFT`）し、版スナップショットが作られる
2. 承認申請すると契約は `UNDER_REVIEW`。承認中の本文差し替えは不可
3. 承認 / 差戻し / 却下は申請状態と契約状態を別々に更新する
4. 承認後に締結待ちまたは有効化。有効契約のみ期間更新・解約
5. 終了日到来は満了（`EXPIRED`）。手動解約は `TERMINATED`

## テスト

```powershell
cd api
mvn test
```

## 未実装（Skill 対比）

- MinIO / マルウェアスキャン / 電子署名連携
- Testcontainers / Playwright E2E / OpenAPI 公開
- テナント（複数会社）分離、代理承認、通知メール
- 法令・保存年限の断定的な判定
"# B2BContractManagement" 
