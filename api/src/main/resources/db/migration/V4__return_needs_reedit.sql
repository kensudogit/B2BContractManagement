-- DEMO-RETURN-001 を差戻し直後（未再編集）に戻す。利用者が金額を直して新版を切れる。
UPDATE contracts
SET name = '【デモ】差戻し後（再編集してください）',
    amount = 1200000.00,
    row_version = 0,
    updated_at = now()
WHERE contract_no = 'DEMO-RETURN-001';

DELETE FROM contract_versions
WHERE version_no > 1
  AND contract_id = (SELECT contract_id FROM contracts WHERE contract_no = 'DEMO-RETURN-001');
