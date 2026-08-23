-- 重要日期（纪念日/倒计时）
CREATE TABLE IF NOT EXISTS day_events (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  name TEXT NOT NULL,
  event_date TEXT NOT NULL,
  repeats_yearly INTEGER NOT NULL DEFAULT 0,
  note TEXT,
  created_at INTEGER NOT NULL,
  archived INTEGER NOT NULL DEFAULT 0,
  updated_at INTEGER NOT NULL,
  first_seen_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_day_events_user_updated ON day_events(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_day_events_user_seen ON day_events(user_id, first_seen_at);
