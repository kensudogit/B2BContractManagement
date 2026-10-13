# 企業間契約管理システム開発 Skill（Java/Spring Boot版）

## 技術構成
Java 21 / Spring Boot 3.x / Spring Security / Spring Data JPA / PostgreSQL / React / Next.js / TypeScript / Docker。

## 配置
Cursor ユーザー共通: `~/.cursor/skills/b2b-contract-management-springboot/`
Claude Code ユーザー共通: `~/.claude/skills/b2b-contract-management-springboot/`
プロジェクト単位: `<project>/.cursor/skills/` または `<project>/.claude/skills/`
フォルダ名と SKILL.md の name は一致させる。`references/` を同じ階層に保つ。

zip を展開したフォルダごと上記へコピーする。`~/.cursor/skills-cursor/` には置かない。

## 起動例
`/b2b-contract-management-springboot`

認識されない場合はClaude Codeを再起動し、Skillの読み込み状態とSKILL.mdのYAMLを確認する。自然文でも「b2b-contract-management-springboot Skillを使って契約管理システムを設計して」と指示できる。

## 指示例
「このSkillを使用し、Java 21 + Spring Boot + PostgreSQL + Next.js + TypeScript + Dockerで企業間契約管理システムのMVPを構築。要件と未決事項を提示し、基本設計→DB→API→画面→テスト→Dockerの順で実装してください。」
「新規契約から承認までの詳細なワークフローを実装してください」

参照実装: `C:\devlop\B2BContractManagement`（UI 3040 / API 8040）。起案ウィザード、版固定、順次 WAITING/PENDING、差戻し再編集、取下げ、DEMO-* モックは `references/02-domain-workflow.md` が正本。

## 注意
これは開発手順と設計参照資料のセットであり、動作するアプリ本体は含まない。実装時に依存ライブラリのサポート状況とライセンスを確認する。
