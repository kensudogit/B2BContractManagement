# 05 React / Next.js / TypeScript
Next.js App Router、TypeScript strict。画面: ログイン、契約一覧、新規契約ウィザード、契約詳細（起案〜決裁〜締結）、版履歴、承認待ち、取引先管理、期限一覧。
## 新規契約〜承認の画面
- `/contracts/new`: ウィザード「基本情報 → 当事者 → 確認して起案」。個別契約は ACTIVE な基本契約を親に選ぶ。保存後は `/contracts/{id}?phase=draft`。
- `/contracts/{id}`: ステッパー（起案→審査→承認→締結→有効）。下書きは編集して新版。承認ステップを順に指定して申請。審査中は固定版・現決裁者・WAITING/PENDING を表示。差戻し後は理由を出し「再編集して新しい版を確定」してから再申請する。差し戻された版は変えない。ADMIN または現承認者が判定、申請者または ADMIN が取下げ。
- `/approvals`: 自分の PENDING だけ。後続 WAITING は出さない。
- 一覧・詳細の状態/種別/役割/履歴アクションは日本語ラベル。金額は文字列のまま送り精度を落とさない。
## 実装規約
- Server/Client Componentsを用途に応じて分ける。業務APIはSpring Bootを正本とする。
- 入力フォームは型定義・検証スキーマを共有し、金額の精度を失わない。
- 契約一覧はサーバー側ページング、検索、ソート、空結果、エラー、ローディングを実装。
- 更新時は `rowVersion` を送信し、409競合時に再取得・差分確認を案内。
- 権限によるボタン表示制御はUX目的。認可判定は必ずバックエンドで行う。
- ファイルアップロード進捗・サイズ制限・エラー表示を実装。審査中はアップロード不可。
- 認証は JWT を `Authorization` と Cookie `contracts_access_token` の両方に持つ。Next middleware は未ログインを `/login` へ 307。localStorage だけでは SSR が通らない。
- Docker Desktop（Windows）では Next のファイル監視が遅れる。`webpack.watchOptions.poll` を入れ、画面が古いときは `.next` を消して web を再起動する。
- 秘密鍵をブラウザに渡さない。
