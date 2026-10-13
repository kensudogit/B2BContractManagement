---
name: b2b-contract-management-springboot
description: >
  Java 21、Spring Boot、Spring Security、Spring Data JPA、PostgreSQL、React、Next.js、TypeScript、Dockerで企業間契約管理システムを設計・実装・レビュー・テストする際に使用する。契約台帳、基本契約と個別契約、契約書版管理、新規契約から承認までの順次ワークフロー（起案ウィザード、版固定、WAITING/PENDING、差戻し再申請、取下げ）、期限通知、RBAC、監査ログ、REST API、DB設計、Docker環境構築を支援する。
---

# B2B Contract Management — Java/Spring Boot Skill

## 発動条件
企業間契約管理の要件定義、基本・詳細設計、実装、レビュー、テスト、運用、既存システム改修の依頼で使用する。
「新規契約から承認」「承認ワークフロー」「起案ウィザード」「順次決裁」「差戻し再申請」「デモデータ」「再編集」が出たら、実装前に `references/02-domain-workflow.md` を読む。参照実装は `C:\devlop\B2BContractManagement`。

## 技術スタック（必須）
- Backend: Java 21 LTS、Spring Boot 3.x（採用版はプロジェクトで固定）、Spring Web、Spring Security、Spring Validation、Spring Data JPA
- Database: PostgreSQL 16+、Flyway migration、JPA/Hibernate。必要に応じて明示的SQL/JPQLを使う。
- Frontend: React、Next.js App Router、TypeScript strict、フォームバリデーション
- Build/test: Maven Wrapper、JUnit 5、Mockito、Spring Boot Test、Testcontainers、Vitest、Playwright
- Infrastructure: Docker、Docker Compose。文書はS3互換オブジェクトストレージ（開発時MinIO等）に保管。
- Python/FastAPI/SQLAlchemy/Alembicを新規実装に混在させない。既存資産を移行する場合のみ移行計画に記載する。

## 必ず守るドメイン原則
- 契約の状態、承認申請の状態、文書の版を別々に管理する。
- 基本契約と個別契約は親子関連を持つ。複数当事者は中間テーブルで表現する。
- 承認対象は申請時の契約版（version_id）に固定し、審査中の本文・文書差し替えを禁止する。
- 承認ステップは先頭だけ PENDING、後続は WAITING。同じ承認者の重複指定を禁止する。
- 差戻し・却下はコメント必須。差戻し後は再編集で新版を切り、再申請は新しい version_id を対象にする。
- 申請者または ADMIN の取下げは申請を CANCELLED、契約を DRAFT に戻す。
- 契約更新・解約・解除・満了の業務上の意味を区別し、状態遷移をサービス層で検証する。
- 金額はBigDecimal/NUMERIC、日付はLocalDate、日時はOffsetDateTime/Instantを使用する。
- 法令、電子署名の有効性、文書保存年限は個別に確認し、断定しない。

## 実行順序
1. `references/01-requirements.md` で要件・前提・未決事項を整理する。
2. `references/02-domain-workflow.md` で状態遷移と承認フローを設計する。
3. `references/03-data-model.md` でER図、JPA Entity、Flyway DDL、索引を設計する。
4. `references/04-api-design.md` でDTO、Controller、Service、Repository、例外処理を実装する。
5. `references/05-frontend.md` でNext.js画面とAPIクライアントを実装する。
6. `references/06-security.md` でSpring Security、認可、監査、ファイル保護を実装する。
7. `references/07-testing.md` でバックエンド・フロントエンド・E2Eを検証する。
8. `references/08-deployment.md` でDocker、設定、起動、監視、バックアップを整備する。
9. `references/09-acceptance.md` で受入条件を検査し、未完了項目を報告する。
10. `references/10-java-implementation.md` に従いJava実装規約を守る。

## 新規契約から承認（欠かさない実装）
1. 起案ウィザード（基本情報 → 当事者 → 確認して起案）で DRAFT と版スナップショットを確定する。
2. 承認申請前に当事者1件以上と確定版があることを検証する。
3. 申請時に最新版を `approval_requests.version_id` へ固定し、契約を UNDER_REVIEW にする。`Idempotency-Key` で二重申請を防ぐ。
4. インボックスはいまの PENDING ステップだけを返す。後続承認者は WAITING の間は見えない。
5. 承認で次ステップを PENDING にし申請を IN_PROGRESS。全ステップ完了で契約 APPROVED。
6. 画面はステッパー、固定版、差戻し理由、「再編集して新しい版を確定」、再申請導線を持つ。認可は API 側で判定する。
7. 確認用モックは Flyway で `DEMO-*` を投入する（V3 で各状態、V4 で差戻し直後の未再編集）。
詳細は `references/02-domain-workflow.md`、API は `references/04-api-design.md`、画面は `references/05-frontend.md`。

## 実装規約
- ControllerはHTTP/DTO処理、Serviceは業務処理、Repositoryは永続化に責務分離する。
- EntityをAPIで直接公開しない。DTOとBean Validation（jakarta.validation）を使う。
- `@Transactional` は業務単位に設定し、重要更新と監査記録の整合性を保証する。
- `@Version` による楽観的排他制御を使い、競合はHTTP 409を返す。
- `@RestControllerAdvice` で例外を統一し、内部スタックトレースを外部公開しない。
- Spring SecurityでAPI単位・契約単位の認可を行い、UI表示制御だけで済ませない。
- APIは既定で `/api` を採用し、版番号は明示要求がある場合のみ導入する。
- GET一覧は上限1000件以下（通常は小さいページサイズ）とし、許可済み項目のみソート可能とする。
- 冪等性キーまたは業務一意制約で二重承認・二重更新を防ぐ。
- 秘密情報、認証トークン、契約本文、個人情報をログに出さない。
- 不明点は仮定と確認事項を明記し、実装・テストの未実施を成功と報告しない。

## 成果物
要件一覧、業務フロー、画面一覧、状態遷移、ER図、Flyway SQL、Java/Next.jsソース、OpenAPI、Docker Compose、テスト、README、運用手順、未実装一覧。
このSkillは開発ガイドであり、完成済みアプリ本体ではない。
