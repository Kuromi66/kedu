-- 记录首次入库时间，保证设备时钟早于水位时新记录也能被其他设备拉到。
ALTER TABLE habits ADD COLUMN first_seen_at INTEGER NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_habits_user_seen ON habits(user_id, first_seen_at);
UPDATE habits SET first_seen_at = updated_at WHERE first_seen_at = 0;

ALTER TABLE events ADD COLUMN first_seen_at INTEGER NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_events_user_seen ON events(user_id, first_seen_at);
UPDATE events SET first_seen_at = updated_at WHERE first_seen_at = 0;
