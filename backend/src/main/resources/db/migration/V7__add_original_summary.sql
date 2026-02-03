-- 添加原始交易摘要字段
ALTER TABLE statement_transaction ADD COLUMN txn_type_raw_original TEXT;

-- 将现有的 txn_type_raw 复制到 txn_type_raw_original
UPDATE statement_transaction SET txn_type_raw_original = txn_type_raw WHERE txn_type_raw_original IS NULL;

-- 添加注释
-- txn_type_raw: 可编辑的交易摘要，用于统计分组
-- txn_type_raw_original: 原始交易摘要，导入时保存，不可修改
