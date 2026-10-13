-- ワークフロー確認用モック。ユーザーは username で解決する。
INSERT INTO parties (legal_name, corporate_number) VALUES
 ('株式会社ガンマ', '1112223334445'),
 ('株式会社デルタ', '5556667778889');

CREATE OR REPLACE FUNCTION demo_contract_snapshot(
    p_id BIGINT,
    p_no TEXT,
    p_name TEXT,
    p_type TEXT,
    p_status TEXT,
    p_start DATE,
    p_end DATE,
    p_amount NUMERIC,
    p_auto BOOLEAN,
    p_notice INT,
    p_parent BIGINT,
    p_party BIGINT,
    p_role TEXT
) RETURNS JSONB LANGUAGE sql AS $$
    SELECT jsonb_build_object(
        'contractId', p_id,
        'contractNo', p_no,
        'name', p_name,
        'type', p_type,
        'status', p_status,
        'startDate', p_start,
        'endDate', p_end,
        'amount', p_amount,
        'currencyCode', 'JPY',
        'autoRenewal', p_auto,
        'noticeDays', p_notice,
        'parentContractId', p_parent,
        'parties', jsonb_build_array(jsonb_build_object('partyId', p_party, 'role', p_role))
    );
$$;

DO $$
DECLARE
    sales_id BIGINT;
    approver_id BIGINT;
    legal_id BIGINT;
    admin_id BIGINT;
    alpha_id BIGINT;
    beta_id BIGINT;
    gamma_id BIGINT;
    master_id BIGINT;
    master_ver BIGINT;
    cid BIGINT;
    vid BIGINT;
    vid2 BIGINT;
    rid BIGINT;
BEGIN
    SELECT user_id INTO STRICT sales_id FROM app_users WHERE username = 'sales';
    SELECT user_id INTO STRICT approver_id FROM app_users WHERE username = 'approver';
    SELECT user_id INTO STRICT legal_id FROM app_users WHERE username = 'legal';
    SELECT user_id INTO STRICT admin_id FROM app_users WHERE username = 'admin';
    SELECT party_id INTO STRICT alpha_id FROM parties WHERE legal_name = '株式会社アルファ';
    SELECT party_id INTO STRICT beta_id FROM parties WHERE legal_name = '株式会社ベータ';
    SELECT party_id INTO STRICT gamma_id FROM parties WHERE legal_name = '株式会社ガンマ';

    -- 有効な基本契約（個別契約の親・承認済み経路の完了形）
    INSERT INTO contracts (contract_no, name, type, status, start_date, end_date, amount, currency_code, auto_renewal, notice_days, row_version)
    VALUES ('DEMO-ACTIVE-001', '【デモ】有効・基本契約', 'MASTER', 'ACTIVE', DATE '2026-04-01', DATE '2028-03-31', 2400000.00, 'JPY', TRUE, 60, 0)
    RETURNING contract_id INTO master_id;
    INSERT INTO contract_parties (contract_id, party_id, party_role) VALUES (master_id, alpha_id, 'CUSTOMER');
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (master_id, 1, demo_contract_snapshot(master_id, 'DEMO-ACTIVE-001', '【デモ】有効・基本契約', 'MASTER', 'ACTIVE', DATE '2026-04-01', DATE '2028-03-31', 2400000.00, TRUE, 60, NULL, alpha_id, 'CUSTOMER'))
    RETURNING version_id INTO master_ver;
    INSERT INTO approval_requests (contract_id, version_id, status, requested_by, requested_at, idempotency_key)
    VALUES (master_id, master_ver, 'APPROVED', sales_id, now() - INTERVAL '40 days', 'demo-active-001')
    RETURNING request_id INTO rid;
    INSERT INTO approval_steps (request_id, step_order, approver_id, status) VALUES
        (rid, 1, approver_id, 'APPROVED'),
        (rid, 2, legal_id, 'APPROVED');
    INSERT INTO approval_histories (request_id, actor_id, action, comment_text, acted_at) VALUES
        (rid, sales_id, 'SUBMIT', '基本契約の承認をお願いします', now() - INTERVAL '40 days'),
        (rid, approver_id, 'APPROVE', '条件問題なし', now() - INTERVAL '39 days'),
        (rid, legal_id, 'APPROVE', '法務確認済み', now() - INTERVAL '38 days');
    INSERT INTO audit_logs (actor_id, resource_type, resource_id, action) VALUES
        (sales_id, 'CONTRACT', master_id::TEXT, 'CREATE'),
        (sales_id, 'APPROVAL', rid::TEXT, 'SUBMIT');

    -- 有効な個別契約（親子）
    INSERT INTO contracts (parent_contract_id, contract_no, name, type, status, start_date, end_date, amount, currency_code, auto_renewal, notice_days, row_version)
    VALUES (master_id, 'DEMO-SPOT-001', '【デモ】有効・個別契約', 'SPOT', 'ACTIVE', DATE '2026-07-01', DATE '2027-06-30', 860000.00, 'JPY', FALSE, 30, 0)
    RETURNING contract_id INTO cid;
    INSERT INTO contract_parties (contract_id, party_id, party_role) VALUES (cid, alpha_id, 'CUSTOMER'), (cid, beta_id, 'VENDOR');
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (cid, 1, demo_contract_snapshot(cid, 'DEMO-SPOT-001', '【デモ】有効・個別契約', 'SPOT', 'ACTIVE', DATE '2026-07-01', DATE '2027-06-30', 860000.00, FALSE, 30, master_id, alpha_id, 'CUSTOMER'))
    RETURNING version_id INTO vid;
    INSERT INTO approval_requests (contract_id, version_id, status, requested_by, requested_at, idempotency_key)
    VALUES (cid, vid, 'APPROVED', sales_id, now() - INTERVAL '20 days', 'demo-spot-001')
    RETURNING request_id INTO rid;
    INSERT INTO approval_steps (request_id, step_order, approver_id, status) VALUES (rid, 1, approver_id, 'APPROVED');
    INSERT INTO approval_histories (request_id, actor_id, action, comment_text, acted_at) VALUES
        (rid, sales_id, 'SUBMIT', '個別発注の承認', now() - INTERVAL '20 days'),
        (rid, approver_id, 'APPROVE', '承認', now() - INTERVAL '19 days');

    -- 期限接近（90日以内の期限一覧）
    INSERT INTO contracts (parent_contract_id, contract_no, name, type, status, start_date, end_date, amount, currency_code, auto_renewal, notice_days, row_version)
    VALUES (master_id, 'DEMO-EXPIRE-001', '【デモ】期限接近', 'SPOT', 'ACTIVE', DATE '2026-01-01', CURRENT_DATE + 25, 320000.00, 'JPY', TRUE, 15, 0)
    RETURNING contract_id INTO cid;
    INSERT INTO contract_parties (contract_id, party_id, party_role) VALUES (cid, gamma_id, 'CUSTOMER');
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (cid, 1, demo_contract_snapshot(cid, 'DEMO-EXPIRE-001', '【デモ】期限接近', 'SPOT', 'ACTIVE', DATE '2026-01-01', CURRENT_DATE + 25, 320000.00, TRUE, 15, master_id, gamma_id, 'CUSTOMER'));

    -- 起案直後（sales が承認申請できる）
    INSERT INTO contracts (contract_no, name, type, status, start_date, end_date, amount, currency_code, auto_renewal, notice_days, row_version)
    VALUES ('DEMO-DRAFT-001', '【デモ】起案直後（申請待ち）', 'MASTER', 'DRAFT', DATE '2026-11-01', DATE '2027-10-31', 1500000.00, 'JPY', FALSE, 30, 0)
    RETURNING contract_id INTO cid;
    INSERT INTO contract_parties (contract_id, party_id, party_role) VALUES (cid, beta_id, 'CUSTOMER');
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (cid, 1, demo_contract_snapshot(cid, 'DEMO-DRAFT-001', '【デモ】起案直後（申請待ち）', 'MASTER', 'DRAFT', DATE '2026-11-01', DATE '2027-10-31', 1500000.00, FALSE, 30, NULL, beta_id, 'CUSTOMER'));
    INSERT INTO audit_logs (actor_id, resource_type, resource_id, action) VALUES (sales_id, 'CONTRACT', cid::TEXT, 'CREATE');

    -- 審査中・1段目待ち（approver のインボックス）
    INSERT INTO contracts (parent_contract_id, contract_no, name, type, status, start_date, end_date, amount, currency_code, auto_renewal, notice_days, row_version)
    VALUES (master_id, 'DEMO-REVIEW-001', '【デモ】審査中・1段目待ち', 'SPOT', 'UNDER_REVIEW', DATE '2026-10-01', DATE '2027-03-31', 540000.00, 'JPY', FALSE, 20, 0)
    RETURNING contract_id INTO cid;
    INSERT INTO contract_parties (contract_id, party_id, party_role) VALUES (cid, alpha_id, 'CUSTOMER');
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (cid, 1, demo_contract_snapshot(cid, 'DEMO-REVIEW-001', '【デモ】審査中・1段目待ち', 'SPOT', 'UNDER_REVIEW', DATE '2026-10-01', DATE '2027-03-31', 540000.00, FALSE, 20, master_id, alpha_id, 'CUSTOMER'))
    RETURNING version_id INTO vid;
    INSERT INTO approval_requests (contract_id, version_id, status, requested_by, requested_at, idempotency_key)
    VALUES (cid, vid, 'REQUESTED', sales_id, now() - INTERVAL '2 days', 'demo-review-001')
    RETURNING request_id INTO rid;
    INSERT INTO approval_steps (request_id, step_order, approver_id, status) VALUES
        (rid, 1, approver_id, 'PENDING'),
        (rid, 2, legal_id, 'WAITING');
    INSERT INTO approval_histories (request_id, actor_id, action, comment_text, acted_at) VALUES
        (rid, sales_id, 'SUBMIT', '2段階でお願いします', now() - INTERVAL '2 days');

    -- 審査中・2段目待ち（legal のインボックス。1段目は承認済み）
    INSERT INTO contracts (parent_contract_id, contract_no, name, type, status, start_date, end_date, amount, currency_code, auto_renewal, notice_days, row_version)
    VALUES (master_id, 'DEMO-REVIEW-002', '【デモ】審査中・2段目待ち', 'SPOT', 'UNDER_REVIEW', DATE '2026-09-01', DATE '2027-02-28', 410000.00, 'JPY', FALSE, 20, 0)
    RETURNING contract_id INTO cid;
    INSERT INTO contract_parties (contract_id, party_id, party_role) VALUES (cid, gamma_id, 'CUSTOMER');
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (cid, 1, demo_contract_snapshot(cid, 'DEMO-REVIEW-002', '【デモ】審査中・2段目待ち', 'SPOT', 'UNDER_REVIEW', DATE '2026-09-01', DATE '2027-02-28', 410000.00, FALSE, 20, master_id, gamma_id, 'CUSTOMER'))
    RETURNING version_id INTO vid;
    INSERT INTO approval_requests (contract_id, version_id, status, requested_by, requested_at, idempotency_key)
    VALUES (cid, vid, 'IN_PROGRESS', sales_id, now() - INTERVAL '6 days', 'demo-review-002')
    RETURNING request_id INTO rid;
    INSERT INTO approval_steps (request_id, step_order, approver_id, status) VALUES
        (rid, 1, approver_id, 'APPROVED'),
        (rid, 2, legal_id, 'PENDING');
    INSERT INTO approval_histories (request_id, actor_id, action, comment_text, acted_at) VALUES
        (rid, sales_id, 'SUBMIT', '法務最終確認までお願いします', now() - INTERVAL '6 days'),
        (rid, approver_id, 'APPROVE', '1段目OK。金額は妥当です', now() - INTERVAL '4 days');

    -- NDA 1段審査（approver のみ）
    INSERT INTO contracts (contract_no, name, type, status, start_date, end_date, amount, currency_code, auto_renewal, notice_days, row_version)
    VALUES ('DEMO-NDA-001', '【デモ】NDA・1段審査', 'NDA', 'UNDER_REVIEW', DATE '2026-10-01', DATE '2027-09-30', NULL, 'JPY', FALSE, NULL, 0)
    RETURNING contract_id INTO cid;
    INSERT INTO contract_parties (contract_id, party_id, party_role) VALUES (cid, beta_id, 'PARTNER');
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (cid, 1, demo_contract_snapshot(cid, 'DEMO-NDA-001', '【デモ】NDA・1段審査', 'NDA', 'UNDER_REVIEW', DATE '2026-10-01', DATE '2027-09-30', NULL, FALSE, NULL, NULL, beta_id, 'PARTNER'))
    RETURNING version_id INTO vid;
    INSERT INTO approval_requests (contract_id, version_id, status, requested_by, requested_at, idempotency_key)
    VALUES (cid, vid, 'REQUESTED', sales_id, now() - INTERVAL '1 day', 'demo-nda-001')
    RETURNING request_id INTO rid;
    INSERT INTO approval_steps (request_id, step_order, approver_id, status) VALUES (rid, 1, approver_id, 'PENDING');
    INSERT INTO approval_histories (request_id, actor_id, action, comment_text, acted_at) VALUES
        (rid, sales_id, 'SUBMIT', '秘密保持契約の確認をお願いします', now() - INTERVAL '1 day');

    -- 差戻し後（版2が最新。再申請すると新しい版が対象）
    INSERT INTO contracts (contract_no, name, type, status, start_date, end_date, amount, currency_code, auto_renewal, notice_days, row_version)
    VALUES ('DEMO-RETURN-001', '【デモ】差戻し後（再編集・再申請）', 'MASTER', 'DRAFT', DATE '2026-12-01', DATE '2027-11-30', 980000.00, 'JPY', FALSE, 45, 1)
    RETURNING contract_id INTO cid;
    INSERT INTO contract_parties (contract_id, party_id, party_role) VALUES (cid, beta_id, 'CUSTOMER');
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (cid, 1, demo_contract_snapshot(cid, 'DEMO-RETURN-001', '【デモ】差戻し後（再編集・再申請）', 'MASTER', 'UNDER_REVIEW', DATE '2026-12-01', DATE '2027-11-30', 1200000.00, FALSE, 45, NULL, beta_id, 'CUSTOMER'))
    RETURNING version_id INTO vid;
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (cid, 2, demo_contract_snapshot(cid, 'DEMO-RETURN-001', '【デモ】差戻し後（再編集・再申請）', 'MASTER', 'DRAFT', DATE '2026-12-01', DATE '2027-11-30', 980000.00, FALSE, 45, NULL, beta_id, 'CUSTOMER'))
    RETURNING version_id INTO vid2;
    INSERT INTO approval_requests (contract_id, version_id, status, requested_by, requested_at, idempotency_key)
    VALUES (cid, vid, 'RETURNED', sales_id, now() - INTERVAL '8 days', 'demo-return-001')
    RETURNING request_id INTO rid;
    INSERT INTO approval_steps (request_id, step_order, approver_id, status) VALUES
        (rid, 1, approver_id, 'RETURNED'),
        (rid, 2, legal_id, 'CANCELLED');
    INSERT INTO approval_histories (request_id, actor_id, action, comment_text, acted_at) VALUES
        (rid, sales_id, 'SUBMIT', '金額120万で申請', now() - INTERVAL '8 days'),
        (rid, approver_id, 'RETURN', '金額を見直してください。98万で再提案を。', now() - INTERVAL '5 days');

    -- 取下げ後
    INSERT INTO contracts (contract_no, name, type, status, start_date, end_date, amount, currency_code, auto_renewal, notice_days, row_version)
    VALUES ('DEMO-CANCEL-001', '【デモ】取下げ後（下書き）', 'NDA', 'DRAFT', DATE '2026-11-15', DATE '2027-11-14', NULL, 'JPY', FALSE, NULL, 0)
    RETURNING contract_id INTO cid;
    INSERT INTO contract_parties (contract_id, party_id, party_role) VALUES (cid, gamma_id, 'PARTNER');
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (cid, 1, demo_contract_snapshot(cid, 'DEMO-CANCEL-001', '【デモ】取下げ後（下書き）', 'NDA', 'DRAFT', DATE '2026-11-15', DATE '2027-11-14', NULL, FALSE, NULL, NULL, gamma_id, 'PARTNER'))
    RETURNING version_id INTO vid;
    INSERT INTO approval_requests (contract_id, version_id, status, requested_by, requested_at, idempotency_key)
    VALUES (cid, vid, 'CANCELLED', sales_id, now() - INTERVAL '3 days', 'demo-cancel-001')
    RETURNING request_id INTO rid;
    INSERT INTO approval_steps (request_id, step_order, approver_id, status) VALUES
        (rid, 1, approver_id, 'CANCELLED'),
        (rid, 2, legal_id, 'CANCELLED');
    INSERT INTO approval_histories (request_id, actor_id, action, comment_text, acted_at) VALUES
        (rid, sales_id, 'SUBMIT', '先に申請', now() - INTERVAL '3 days'),
        (rid, sales_id, 'CANCEL', '条件が固まるまで取り下げます', now() - INTERVAL '2 days');

    -- 却下済み
    INSERT INTO contracts (parent_contract_id, contract_no, name, type, status, start_date, end_date, amount, currency_code, auto_renewal, notice_days, row_version)
    VALUES (master_id, 'DEMO-REJECT-001', '【デモ】却下済み', 'SPOT', 'REJECTED', DATE '2026-08-01', DATE '2026-12-31', 2500000.00, 'JPY', FALSE, 10, 0)
    RETURNING contract_id INTO cid;
    INSERT INTO contract_parties (contract_id, party_id, party_role) VALUES (cid, alpha_id, 'CUSTOMER');
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (cid, 1, demo_contract_snapshot(cid, 'DEMO-REJECT-001', '【デモ】却下済み', 'SPOT', 'REJECTED', DATE '2026-08-01', DATE '2026-12-31', 2500000.00, FALSE, 10, master_id, alpha_id, 'CUSTOMER'))
    RETURNING version_id INTO vid;
    INSERT INTO approval_requests (contract_id, version_id, status, requested_by, requested_at, idempotency_key)
    VALUES (cid, vid, 'REJECTED', sales_id, now() - INTERVAL '15 days', 'demo-reject-001')
    RETURNING request_id INTO rid;
    INSERT INTO approval_steps (request_id, step_order, approver_id, status) VALUES
        (rid, 1, approver_id, 'REJECTED'),
        (rid, 2, legal_id, 'CANCELLED');
    INSERT INTO approval_histories (request_id, actor_id, action, comment_text, acted_at) VALUES
        (rid, sales_id, 'SUBMIT', '特急案件です', now() - INTERVAL '15 days'),
        (rid, approver_id, 'REJECT', '予算枠を超えているため却下します', now() - INTERVAL '14 days');

    -- 承認済（締結待ちまたは有効化ができる）
    INSERT INTO contracts (parent_contract_id, contract_no, name, type, status, start_date, end_date, amount, currency_code, auto_renewal, notice_days, row_version)
    VALUES (master_id, 'DEMO-APPROVED-001', '【デモ】承認済（締結/有効化）', 'SPOT', 'APPROVED', DATE '2026-11-01', DATE '2027-04-30', 675000.00, 'JPY', FALSE, 30, 0)
    RETURNING contract_id INTO cid;
    INSERT INTO contract_parties (contract_id, party_id, party_role) VALUES (cid, beta_id, 'VENDOR');
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (cid, 1, demo_contract_snapshot(cid, 'DEMO-APPROVED-001', '【デモ】承認済（締結/有効化）', 'SPOT', 'APPROVED', DATE '2026-11-01', DATE '2027-04-30', 675000.00, FALSE, 30, master_id, beta_id, 'VENDOR'))
    RETURNING version_id INTO vid;
    INSERT INTO approval_requests (contract_id, version_id, status, requested_by, requested_at, idempotency_key)
    VALUES (cid, vid, 'APPROVED', sales_id, now() - INTERVAL '10 days', 'demo-approved-001')
    RETURNING request_id INTO rid;
    INSERT INTO approval_steps (request_id, step_order, approver_id, status) VALUES
        (rid, 1, approver_id, 'APPROVED'),
        (rid, 2, legal_id, 'APPROVED');
    INSERT INTO approval_histories (request_id, actor_id, action, comment_text, acted_at) VALUES
        (rid, sales_id, 'SUBMIT', '承認後に締結します', now() - INTERVAL '10 days'),
        (rid, approver_id, 'APPROVE', '1段目承認', now() - INTERVAL '9 days'),
        (rid, legal_id, 'APPROVE', '条項問題なし', now() - INTERVAL '8 days');

    -- 締結待ち
    INSERT INTO contracts (parent_contract_id, contract_no, name, type, status, start_date, end_date, amount, currency_code, auto_renewal, notice_days, row_version)
    VALUES (master_id, 'DEMO-SIGN-001', '【デモ】締結待ち', 'SPOT', 'PENDING_SIGNATURE', DATE '2026-10-15', DATE '2027-10-14', 190000.00, 'JPY', FALSE, 14, 0)
    RETURNING contract_id INTO cid;
    INSERT INTO contract_parties (contract_id, party_id, party_role) VALUES (cid, gamma_id, 'CUSTOMER');
    INSERT INTO contract_versions (contract_id, version_no, snapshot_json)
    VALUES (cid, 1, demo_contract_snapshot(cid, 'DEMO-SIGN-001', '【デモ】締結待ち', 'SPOT', 'PENDING_SIGNATURE', DATE '2026-10-15', DATE '2027-10-14', 190000.00, FALSE, 14, master_id, gamma_id, 'CUSTOMER'))
    RETURNING version_id INTO vid;
    INSERT INTO approval_requests (contract_id, version_id, status, requested_by, requested_at, idempotency_key)
    VALUES (cid, vid, 'APPROVED', sales_id, now() - INTERVAL '7 days', 'demo-sign-001')
    RETURNING request_id INTO rid;
    INSERT INTO approval_steps (request_id, step_order, approver_id, status) VALUES (rid, 1, legal_id, 'APPROVED');
    INSERT INTO approval_histories (request_id, actor_id, action, comment_text, acted_at) VALUES
        (rid, sales_id, 'SUBMIT', '締結手続きに進みます', now() - INTERVAL '7 days'),
        (rid, legal_id, 'APPROVE', '署名待ちにしてください', now() - INTERVAL '6 days');
END $$;

DROP FUNCTION demo_contract_snapshot(BIGINT, TEXT, TEXT, TEXT, TEXT, DATE, DATE, NUMERIC, BOOLEAN, INT, BIGINT, BIGINT, TEXT);
