CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  salt TEXT NOT NULL,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sessions (
  token TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id);

CREATE TABLE IF NOT EXISTS habits (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  name TEXT NOT NULL,
  color_argb INTEGER NOT NULL,
  glyph TEXT NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  reminder_enabled INTEGER NOT NULL DEFAULT 0,
  reminder_hour INTEGER,
  reminder_minute INTEGER,
  target_enabled INTEGER NOT NULL DEFAULT 0,
  daily_target_count INTEGER,
  created_at INTEGER NOT NULL,
  archived INTEGER NOT NULL DEFAULT 0,
  updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_habits_user_updated ON habits(user_id, updated_at);

CREATE TABLE IF NOT EXISTS events (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  habit_id TEXT NOT NULL,
  occurred_at INTEGER NOT NULL,
  local_date TEXT NOT NULL,
  is_backfilled INTEGER NOT NULL DEFAULT 0,
  deleted_at INTEGER,
  updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_events_user_updated ON events(user_id, updated_at);
