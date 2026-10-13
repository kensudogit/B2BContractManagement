# 08 Docker / デプロイ
推奨構成: frontend(Next.js)、backend(Spring Boot)、db(PostgreSQL)、object-storage(MinIO等、開発のみ)。
参照実装ポート: UI 3040、API 8040、Postgres 5435。デモユーザー admin / approver / legal / sales（パスワード `password`）。
## 環境変数例
`SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/contracts`
`SPRING_DATASOURCE_USERNAME=contracts_app`
`SPRING_DATASOURCE_PASSWORD` はSecrets経由で設定。
`SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
`SPRING_FLYWAY_ENABLED=true`
`NEXT_PUBLIC_API_BASE_URL` は公開してよいURLだけを設定。
## 必須
- Maven Wrapperでビルド、Java 21ランタイムのマルチステージDockerfile。
- DB起動ヘルスチェック、アプリreadiness/liveness、非root実行。
- DBと文書ストレージのバックアップ、復元テスト、監視、アラート。
- 本番ではデフォルト認証情報・公開DBポート・平文秘密情報を禁止。
- `docker compose up --build -d`、`docker compose ps`、`docker compose logs` を運用手順に記載。
- Docker Desktop で Bake/buildx の context がずれる場合は `COMPOSE_BAKE=false` を `.env` に置き、`BUILDX_BUILDER` を外す。
- web は `./web:/app` + 匿名 `node_modules` ボリューム。Windows では Next の HMR が遅れるので poll を有効化し、古い画面が出たら `.next` 削除のうえ web 再起動。
- 確認用データは Flyway V3/V4 の `DEMO-*`。既存 DB には migrate で追加される。消して入れ直す場合だけ volume を作り直す。
