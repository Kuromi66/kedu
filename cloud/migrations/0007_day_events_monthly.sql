-- 重要日期：按月重复
ALTER TABLE day_events ADD COLUMN repeats_monthly INTEGER NOT NULL DEFAULT 0;
