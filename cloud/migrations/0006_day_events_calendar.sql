-- 重要日期：阳历/农历类型与农历月日
ALTER TABLE day_events ADD COLUMN calendar_type TEXT NOT NULL DEFAULT 'SOLAR';
ALTER TABLE day_events ADD COLUMN lunar_month INTEGER;
ALTER TABLE day_events ADD COLUMN lunar_day INTEGER;
ALTER TABLE day_events ADD COLUMN lunar_leap INTEGER NOT NULL DEFAULT 0;
