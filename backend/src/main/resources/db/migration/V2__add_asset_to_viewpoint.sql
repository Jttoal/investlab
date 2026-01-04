-- 增加 viewpoint 与 asset 的关联
ALTER TABLE viewpoint ADD COLUMN asset_id INTEGER NULL REFERENCES asset(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_viewpoint_asset ON viewpoint(asset_id);
ALTER TABLE viewpoint ADD COLUMN remark TEXT;
