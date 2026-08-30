// 数据库访问层：封装 D1 的全部 SQL 语句
// 约定：同步采用单条记录 Last-Write-Wins（LWW），即 updated_at 大者胜；
// first_seen_at 用于登录后首次同步，保证云端已有但本地从未见过的记录也能被拉回
import { BATCH_LIMIT, DELETED_RETENTION_MS } from './constants';
import type { Env } from './types';
import type { DayEventRecord, EventRecord, HabitRecord } from './validation';

// ---------- 会话 ----------

// 写入一条会话记录；token 为登录时生成的随机串，expiresAt 为过期时间戳
export async function insertSession(
  env: Env,
  token: string,
  userId: string,
  expiresAt: number,
): Promise<void> {
  await env.DB.prepare(
    'INSERT INTO sessions (token, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)',
  )
    .bind(token, userId, Date.now(), expiresAt)
    .run();
}

// 按 token 查询有效会话归属的用户 id；token 不存在或已过期返回 null
export async function findUserBySession(env: Env, token: string): Promise<string | null> {
  const row = await env.DB.prepare(
    'SELECT user_id FROM sessions WHERE token = ? AND expires_at > ?',
  )
    .bind(token, Date.now())
    .first<{ user_id: string }>();
  return row ? row.user_id : null;
}

// 登出：删除指定会话
export async function deleteSessionByToken(env: Env, token: string): Promise<void> {
  await env.DB.prepare('DELETE FROM sessions WHERE token = ?').bind(token).run();
}

// ---------- 用户 ----------

// 按邮箱查用户（含密码哈希与盐），用于登录校验；不存在返回 null
export async function findUserByEmail(
  env: Env,
  email: string,
): Promise<{ id: string; password_hash: string; salt: string } | null> {
  const row = await env.DB.prepare('SELECT id, password_hash, salt FROM users WHERE email = ?')
    .bind(email)
    .first<{ id: string; password_hash: string; salt: string }>();
  return row ?? null;
}

// 邮箱是否已注册（注册前查重）
export async function emailExists(env: Env, email: string): Promise<boolean> {
  const row = await env.DB.prepare('SELECT id FROM users WHERE email = ?')
    .bind(email)
    .first<{ id: string }>();
  return row !== null;
}

// 创建用户；id、passwordHash、salt 由调用方（auth 模块）生成
export async function insertUser(
  env: Env,
  id: string,
  email: string,
  passwordHash: string,
  salt: string,
): Promise<void> {
  await env.DB.prepare(
    'INSERT INTO users (id, email, password_hash, salt, created_at) VALUES (?, ?, ?, ?, ?)',
  )
    .bind(id, email, passwordHash, salt, Date.now())
    .run();
}

// ---------- 习惯同步 ----------

// 习惯记录绑定参数：顺序必须与 upsert SQL 的占位符一致
function habitBindings(userId: string, habit: HabitRecord, firstSeenAt: number): unknown[] {
  return [
    habit.id,
    userId,
    habit.name,
    habit.colorArgb,
    habit.glyph,
    habit.sortOrder,
    habit.reminderEnabled ? 1 : 0,
    habit.reminderHour ?? null,
    habit.reminderMinute ?? null,
    habit.targetEnabled ? 1 : 0,
    habit.dailyTargetCount ?? null,
    habit.createdAtEpochMillis,
    habit.archived ? 1 : 0,
    habit.updatedAtEpochMillis,
    firstSeenAt,
    habit.deletedAtEpochMillis ?? null,
  ];
}

// 批量 upsert 习惯：按 updated_at 大者胜（LWW），自动分批执行
export async function upsertHabits(
  env: Env,
  userId: string,
  habits: HabitRecord[],
  firstSeenAt: number,
): Promise<void> {
  const statements = habits.map((habit) =>
    env.DB.prepare(
      `INSERT INTO habits
         (id, user_id, name, color_argb, glyph, sort_order, reminder_enabled, reminder_hour,
          reminder_minute, target_enabled, daily_target_count, created_at, archived, updated_at,
          first_seen_at, deleted_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         name = excluded.name, color_argb = excluded.color_argb, glyph = excluded.glyph,
         sort_order = excluded.sort_order, reminder_enabled = excluded.reminder_enabled,
         reminder_hour = excluded.reminder_hour, reminder_minute = excluded.reminder_minute,
         target_enabled = excluded.target_enabled, daily_target_count = excluded.daily_target_count,
         created_at = excluded.created_at, archived = excluded.archived,
         updated_at = excluded.updated_at, deleted_at = excluded.deleted_at
       WHERE excluded.updated_at > habits.updated_at AND habits.user_id = excluded.user_id`,
    ).bind(...habitBindings(userId, habit, firstSeenAt)),
  );
  await runBatches(env, statements);
}

// ---------- 打卡记录同步 ----------

// 打卡记录绑定参数：顺序必须与 upsert SQL 的占位符一致
function eventBindings(userId: string, event: EventRecord, firstSeenAt: number): unknown[] {
  return [
    event.id,
    userId,
    event.habitId,
    event.occurredAtEpochMillis,
    event.localDate,
    event.isBackfilled ? 1 : 0,
    event.deletedAtEpochMillis ?? null,
    event.updatedAtEpochMillis,
    firstSeenAt,
    event.note ?? null,
  ];
}

// 批量 upsert 打卡记录：仅接受归属当前用户已有习惯的记录（防止脏数据）；
// 软删除以 deleted_at 墓碑传播，不做物理删除
export async function upsertEvents(
  env: Env,
  userId: string,
  events: EventRecord[],
  firstSeenAt: number,
): Promise<void> {
  const { results: habitRows } = await env.DB.prepare(
    'SELECT id FROM habits WHERE user_id = ?',
  )
    .bind(userId)
    .all<{ id: string }>();
  const validHabitIds = new Set(habitRows.map((row) => row.id));
  const statements = events
    .filter((event) => validHabitIds.has(event.habitId))
    .map((event) =>
      env.DB.prepare(
        `INSERT INTO events
           (id, user_id, habit_id, occurred_at, local_date, is_backfilled, deleted_at, updated_at,
            first_seen_at, note)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT(id) DO UPDATE SET
           habit_id = excluded.habit_id, occurred_at = excluded.occurred_at,
           local_date = excluded.local_date, is_backfilled = excluded.is_backfilled,
           deleted_at = excluded.deleted_at, updated_at = excluded.updated_at,
           note = excluded.note
         WHERE excluded.updated_at > events.updated_at AND events.user_id = excluded.user_id`,
      ).bind(...eventBindings(userId, event, firstSeenAt)),
    );
  await runBatches(env, statements);
}

// ---------- 重要日期同步 ----------

// 重要日期绑定参数：顺序必须与 upsert SQL 的占位符一致
function dayEventBindings(userId: string, event: DayEventRecord, firstSeenAt: number): unknown[] {
  return [
    event.id,
    userId,
    event.name,
    event.eventDate,
    event.repeatsMonthly ? 1 : 0,
    event.repeatsYearly ? 1 : 0,
    event.note ?? null,
    event.sortOrder,
    event.calendarType ?? 'SOLAR',
    event.lunarMonth ?? null,
    event.lunarDay ?? null,
    event.lunarLeap ? 1 : 0,
    event.reminderEnabled ? 1 : 0,
    event.reminderDaysBefore ?? 1,
    event.createdAtEpochMillis,
    event.archived ? 1 : 0,
    event.updatedAtEpochMillis,
    firstSeenAt,
  ];
}

// 批量 upsert 重要日期：与习惯/打卡相同的 LWW 策略
export async function upsertDayEvents(
  env: Env,
  userId: string,
  dayEvents: DayEventRecord[],
  firstSeenAt: number,
): Promise<void> {
  const statements = dayEvents.map((dayEvent) =>
    env.DB.prepare(
      `INSERT INTO day_events
         (id, user_id, name, event_date, repeats_monthly, repeats_yearly, note, sort_order, calendar_type,
          lunar_month, lunar_day, lunar_leap, reminder_enabled, reminder_days_before, created_at, archived,
          updated_at, first_seen_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         name = excluded.name, event_date = excluded.event_date,
         repeats_monthly = excluded.repeats_monthly,
         repeats_yearly = excluded.repeats_yearly, note = excluded.note,
         sort_order = excluded.sort_order,
         calendar_type = excluded.calendar_type,
         lunar_month = excluded.lunar_month, lunar_day = excluded.lunar_day,
         lunar_leap = excluded.lunar_leap,
         reminder_enabled = excluded.reminder_enabled,
         reminder_days_before = excluded.reminder_days_before,
         created_at = excluded.created_at, archived = excluded.archived,
         updated_at = excluded.updated_at
       WHERE excluded.updated_at > day_events.updated_at AND day_events.user_id = excluded.user_id`,
    ).bind(...dayEventBindings(userId, dayEvent, firstSeenAt)),
  );
  await runBatches(env, statements);
}

// ---------- 增量拉取 ----------

// ---------- 过期墓碑清理 ----------

// 物理清除超过保留期的彻底删除记录，供定时任务（scheduled）调用。
// 先清打卡再清习惯，最后兜底清掉指向已不存在习惯的孤儿打卡；
// day_events 只有归档（archived）没有墓碑，归档可恢复，不在此清理范围
export async function purgeDeletedRecords(env: Env): Promise<void> {
  const cutoff = Date.now() - DELETED_RETENTION_MS;
  await env.DB.prepare(
    'DELETE FROM events WHERE deleted_at IS NOT NULL AND deleted_at < ?',
  )
    .bind(cutoff)
    .run();
  await env.DB.prepare(
    'DELETE FROM habits WHERE deleted_at IS NOT NULL AND deleted_at < ?',
  )
    .bind(cutoff)
    .run();
  await env.DB.prepare('DELETE FROM events WHERE habit_id NOT IN (SELECT id FROM habits)')
    .run();
}

// 行类型：与表字段 snake_case 一一对应，用于读取后映射为 API 响应形状
interface HabitRow {
  id: string;
  name: string;
  color_argb: number;
  glyph: string;
  sort_order: number;
  reminder_enabled: number;
  reminder_hour: number | null;
  reminder_minute: number | null;
  target_enabled: number;
  daily_target_count: number | null;
  created_at: number;
  archived: number;
  updated_at: number;
  deleted_at: number | null;
}

interface EventRow {
  id: string;
  habit_id: string;
  occurred_at: number;
  local_date: string;
  is_backfilled: number;
  deleted_at: number | null;
  updated_at: number;
  note: string | null;
}

interface DayEventRow {
  id: string;
  name: string;
  event_date: string;
  repeats_monthly: number;
  repeats_yearly: number;
  reminder_enabled: number;
  reminder_days_before: number | null;
  note: string | null;
  sort_order: number;
  calendar_type: string | null;
  lunar_month: number | null;
  lunar_day: number | null;
  lunar_leap: number;
  created_at: number;
  archived: number;
  updated_at: number;
}

// 拉取该用户所有 updated_at 或 first_seen_at 大于 since 的记录，按 updated_at 升序返回。
// since 使用服务端时间戳水位：客户端以响应里的 serverTime 推进水位，避免重复拉取
export async function fetchChangedRecords(
  env: Env,
  userId: string,
  since: number,
): Promise<{ habits: HabitRecord[]; events: EventRecord[]; dayEvents: DayEventRecord[] }> {
  const { results: habitRows } = await env.DB.prepare(
    `SELECT id, name, color_argb, glyph, sort_order, reminder_enabled, reminder_hour,
            reminder_minute, target_enabled, daily_target_count, created_at, archived, updated_at,
            deleted_at
     FROM habits WHERE user_id = ? AND (updated_at > ? OR first_seen_at > ?) ORDER BY updated_at ASC`,
  )
    .bind(userId, since, since)
    .all<HabitRow>();
  const { results: eventRows } = await env.DB.prepare(
    `SELECT id, habit_id, occurred_at, local_date, is_backfilled, deleted_at, updated_at, note
     FROM events WHERE user_id = ? AND (updated_at > ? OR first_seen_at > ?) ORDER BY updated_at ASC`,
  )
    .bind(userId, since, since)
    .all<EventRow>();
  const { results: dayEventRows } = await env.DB.prepare(
    `SELECT id, name, event_date, repeats_monthly, repeats_yearly, note, sort_order, calendar_type,
            lunar_month, lunar_day, lunar_leap, reminder_enabled, reminder_days_before,
            created_at, archived, updated_at
     FROM day_events WHERE user_id = ? AND (updated_at > ? OR first_seen_at > ?) ORDER BY updated_at ASC`,
  )
    .bind(userId, since, since)
    .all<DayEventRow>();

  return {
    habits: habitRows.map((row) => ({
      id: row.id,
      name: row.name,
      colorArgb: row.color_argb,
      glyph: row.glyph,
      sortOrder: row.sort_order,
      reminderEnabled: !!row.reminder_enabled,
      reminderHour: row.reminder_hour ?? null,
      reminderMinute: row.reminder_minute ?? null,
      targetEnabled: !!row.target_enabled,
      dailyTargetCount: row.daily_target_count ?? null,
      createdAtEpochMillis: row.created_at,
      archived: !!row.archived,
      updatedAtEpochMillis: row.updated_at,
      deletedAtEpochMillis: row.deleted_at ?? null,
    })),
    events: eventRows.map((row) => ({
      id: row.id,
      habitId: row.habit_id,
      occurredAtEpochMillis: row.occurred_at,
      localDate: row.local_date,
      isBackfilled: !!row.is_backfilled,
      deletedAtEpochMillis: row.deleted_at ?? null,
      updatedAtEpochMillis: row.updated_at,
      note: row.note ?? null,
    })),
    dayEvents: dayEventRows.map((row) => ({
      id: row.id,
      name: row.name,
      eventDate: row.event_date,
      repeatsMonthly: !!row.repeats_monthly,
      repeatsYearly: !!row.repeats_yearly,
      reminderEnabled: !!row.reminder_enabled,
      reminderDaysBefore: row.reminder_days_before ?? 1,
      note: row.note ?? null,
      sortOrder: row.sort_order,
      calendarType: row.calendar_type ?? 'SOLAR',
      lunarMonth: row.lunar_month ?? null,
      lunarDay: row.lunar_day ?? null,
      lunarLeap: !!row.lunar_leap,
      createdAtEpochMillis: row.created_at,
      archived: !!row.archived,
      updatedAtEpochMillis: row.updated_at,
    })),
  };
}

// ---------- 公共工具 ----------

// 分批执行 D1 语句：batch 有单次语句数上限，超出时切批
async function runBatches(env: Env, statements: D1PreparedStatement[]): Promise<void> {
  for (let index = 0; index < statements.length; index += BATCH_LIMIT) {
    await env.DB.batch(statements.slice(index, index + BATCH_LIMIT));
  }
}
