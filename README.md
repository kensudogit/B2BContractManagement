# 企業間契約管理（B2B Contract Management）

`b2b-contract-management-springboot` Skill に沿った MVP です。契約状態・承認状態・文書版を分けて管理します。

## スタック

- Java 21 / Spring Boot 3.3 / Spring Security (JWT) / Spring Data JPA
- PostgreSQL 16 / Flyway
- Next.js 14 / React / TypeScript
- 文書はローカルディレクトリ（`DOCUMENT_DIR`）。本番では S3 互換に差し替え可能

## 起動

Docker Desktop の context は `desktop-linux` を使います。Bake が context とずれると
`use docker --context=desktop-linux buildx` でビルドが即終了するので、プロジェクトの `.env` で `COMPOSE_BAKE=false` にしています。

```powershell
cd C:\devlop\B2BContractManagement
.\start-all.ps1
```

`start-all.ps1` は Docker Compose で db / api / web を起動します。Bake ずれ対策で `COMPOSE_BAKE=false` を入れています。手で起動する場合:

```powershell
docker context use desktop-linux
docker compose up --build -d
```

ホストで API / 画面だけ動かす場合（Docker の api/web は止める）:

```powershell
docker compose up -d db
docker compose stop api web
cd api; mvn spring-boot:run
cd ..\web; npm install; npm run dev
```

- UI: http://localhost:3040
- API: http://localhost:8040
- PostgreSQL: localhost:5435 / `contracts` / `contracts_app`

デモユーザー（パスワードはすべて `password`）:

| ユーザー | ロール | 確認すること |
|---|---|---|
| sales | SALES | 起案、申請、差戻し後の再申請、取下げ |
| approver | APPROVER | 1段目のインボックス、承認・差戻し・却下 |
| legal | LEGAL | 2段目のインボックス、締結待ちの有効化 |
| admin | ADMIN | 全ステップの代決、有効化 |

## デモデータでワークフローを確認

Flyway `V3__workflow_demo_data.sql` と `V4__return_needs_reedit.sql` が契約番号 `DEMO-*` を投入します。再作成後も同じ番号です。

| 契約番号 | 契約状態 | 確認操作 |
|---|---|---|
| DEMO-DRAFT-001 | 下書き | sales が承認ステップを付けて申請 |
| DEMO-REVIEW-001 | 審査中 | approver の承認待ち。legal のインボックスには出ない |
| DEMO-REVIEW-002 | 審査中 | legal の2段目待ち。1段目は承認済み |
| DEMO-NDA-001 | 審査中 | approver だけの1段審査 |
| DEMO-RETURN-001 | 下書き | 差戻し直後。金額120万を直して「再編集して新しい版を確定」→再申請 |
| DEMO-CANCEL-001 | 下書き | 取下げ履歴を確認して再申請 |
| DEMO-APPROVED-001 | 承認済 | admin/legal が締結待ちまたは有効化 |
| DEMO-SIGN-001 | 締結待ち | 締結確認して有効化 |
| DEMO-ACTIVE-001 | 有効 | 基本契約。個別契約の親 |
| DEMO-SPOT-001 | 有効 | 基本契約の子 |
| DEMO-EXPIRE-001 | 有効 | 期限一覧（90日以内） |
| DEMO-REJECT-001 | 却下 | 却下コメントと終了状態 |

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
