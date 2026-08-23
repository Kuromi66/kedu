-- 重要日期手动排序
ALTER TABLE day_events ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;
