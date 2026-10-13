# 04 REST API / Spring Boot
Base path: `/api`。APIバージョンは要件で必要な場合のみ導入。
| Method | Path | 用途 |
|---|---|---|
| GET | /contracts | 一覧、検索、ページング |
| POST | /contracts | 登録 |
| GET | /contracts/{id} | 詳細 |
| PUT | /contracts/{id} | 更新、version必須 |
| GET | /contracts/{id}/versions | 版一覧 |
| POST | /contracts/{id}/documents | 文書アップロード |
| POST | /contracts/{id}/approval-requests | 承認申請。Body: `{ approverIds, comment }`。Header: `Idempotency-Key` |
| GET | /contracts/{id}/approval-requests | 契約の申請履歴 |
| GET | /approval-requests/inbox | 自分の PENDING ステップだけ |
| GET | /approval-requests/{id} | 申請詳細（固定版・現ステップ・氏名） |
| POST | /approval-requests/{id}/approve | 承認。次が居れば IN_PROGRESS |
| POST | /approval-requests/{id}/return | 差戻し。comment 必須 |
| POST | /approval-requests/{id}/reject | 却下。comment 必須 |
| POST | /approval-requests/{id}/cancel | 取下げ。申請者または ADMIN |
| POST | /contracts/{id}/sign | 締結待ち（APPROVED → PENDING_SIGNATURE） |
| POST | /contracts/{id}/activate | 有効化（APPROVED または PENDING_SIGNATURE → ACTIVE） |
| POST | /contracts/{id}/renewals | 更新 |
| POST | /contracts/{id}/termination | 終了 |
| GET | /parties | 取引先検索 |
| POST | /parties | 取引先登録 |
## 実装パターン
`@RestController` → DTO(`record`, `@Valid`) → `@Service` → `JpaRepository` → PostgreSQL。
- 一覧はPageableで取得し、1回最大1000件、既定50件程度。ソートキーは許可リスト。
- 認証401、認可403、不存在404、排他競合409、入力不正400（Bean Validation）。
- `@RestControllerAdvice` + `ProblemDetail`等でエラーを統一。requestIdを含める。
- `@Transactional`で業務状態変更と監査記録を原子的に処理。
- 承認POSTは冪等性キー/一意制約/状態チェックで二重処理を防止。申請の参照は `requireVersion(request.versionId)`。
- 申請 View は contractNo/name、versionNo、requestedByName、currentStepOrder、currentApproverId、各ステップ/履歴の氏名を返す。
- 後続ステップは WAITING で作成し、承認時に次を PENDING にする。インボックスは PENDING のみ。
- 差戻し・却下の空コメントは 400。先回りの決裁は 403。下書き以外への申請・未完了申請の並行作成は 409。
- 添付はサイズ・MIME・拡張子・マルウェア検査し、認可後に配信する。
- OpenAPIを生成し、契約テストでDTO互換性を確認する。
