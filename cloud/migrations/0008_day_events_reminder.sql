-- 重要日期：提前提醒
ALTER TABLE day_events ADD COLUMN reminder_enabled INTEGER NOT NULL DEFAULT 0;
ALTER TABLE day_events ADD COLUMN reminder_days_before INTEGER NOT NULL DEFAULT 1;
