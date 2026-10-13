# 02 業務フロー・状態遷移

起案 → 審査 → 承認 → 締結待ち → 有効 → 更新 / 満了 / 解約・解除。

## 契約状態
DRAFT, UNDER_REVIEW, APPROVED, PENDING_SIGNATURE, ACTIVE, EXPIRED, TERMINATED, REJECTED。

## 承認申請状態
REQUESTED, IN_PROGRESS, RETURNED, APPROVED, REJECTED, CANCELLED。

## 承認ステップ状態
- PENDING: いま決裁番。インボックスに出す。
- WAITING: 前段未了。決裁不可、インボックスに出さない。
- APPROVED / RETURNED / REJECTED / CANCELLED: 終了。

## 新規契約から承認（必須経路）
1. **起案**: 基本情報 → 当事者（1件以上） → 確認。契約は DRAFT。保存時に版スナップショットを確定する。
2. **承認申請**: 下書きのみ。当事者と確定版が必須。`approverIds` は1人以上・重複なし・APPROVER/ADMIN/LEGAL。申請時の最新版を `version_id` に固定し、契約を UNDER_REVIEW、申請を REQUESTED。先頭ステップだけ PENDING、後続は WAITING。
3. **順次決裁**: 現ステップの承認者、または ADMIN だけが判定できる。後続の先回りは 403。
   - 承認: 現ステップ APPROVED。次の WAITING を PENDING、申請を IN_PROGRESS。残りが無ければ申請・契約とも APPROVED。
   - 差戻し: コメント必須。申請 RETURNED、残りステップ CANCELLED、契約 DRAFT。起案者が再編集すると新しい版番号が付く。再申請の対象は新しい version_id。
   - 却下: コメント必須。申請 REJECTED、残りステップ CANCELLED、契約 REJECTED。
   - 取下げ: 申請者または ADMIN。申請 CANCELLED、未了ステップ CANCELLED、契約 DRAFT。
4. **承認後**: 電子署名連携または権限者確認で PENDING_SIGNATURE。ADMIN/LEGAL が有効化して ACTIVE。
5. **審査中の禁止**: 契約本文の更新、文書差し替え、別の未完了申請の並行作成。承認対象の参照は申請の version_id（最新版ではない）。

## ルール
- 契約状態・承認申請状態・文書版は別管理。UI のボタン表示だけで認可しない。
- 決裁時刻・行為者・コメントは `approval_histories` に追記する。承認済み版は不変。変更は新版。
- 更新は新期間と条件の審査要否を判定する。自動更新でも通知期限を管理する。
- 満了は予定ジョブで判定し、手動解約/解除と区別する。
- 法令、電子署名の有効性、文書保存年限は個別に確認し、断定しない。

## 参照実装の役割
デモユーザー（パスワード `password`）: sales=起案、approver=1段目、legal=2段目、admin=全体。
パッケージは `approval` に Service/Controller/DTO/Step。画面は `/contracts/new` ウィザードと `/contracts/{id}` 詳細、`/approvals` インボックス。

## 確認用モック（DEMO-*）
Flyway `V3__workflow_demo_data.sql` と `V4__return_needs_reedit.sql`。契約番号は `DEMO-*`。

| 契約番号 | 状態 | 確認操作 |
|---|---|---|
| DEMO-DRAFT-001 | DRAFT | sales が承認申請 |
| DEMO-REVIEW-001 | UNDER_REVIEW | approver の1段目。legal のインボックスには出ない |
| DEMO-REVIEW-002 | UNDER_REVIEW | legal の2段目。1段目は承認済み |
| DEMO-NDA-001 | UNDER_REVIEW | approver だけの1段 |
| DEMO-RETURN-001 | DRAFT | 差戻し直後・版1・金額120万。再編集で新版→再申請 |
| DEMO-CANCEL-001 | DRAFT | 取下げ後の再申請 |
| DEMO-APPROVED-001 | APPROVED | 締結待ちまたは有効化 |
| DEMO-SIGN-001 | PENDING_SIGNATURE | 有効化 |
| DEMO-ACTIVE-001 | ACTIVE | 基本契約（親） |
| DEMO-SPOT-001 | ACTIVE | 個別契約（子） |
| DEMO-EXPIRE-001 | ACTIVE | 期限一覧（90日以内） |
| DEMO-REJECT-001 | REJECTED | 却下コメント |
