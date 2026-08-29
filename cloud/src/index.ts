export interface Env {
  DB: D1Database;
}

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
};

const SESSION_DURATION_MS = 90 * 24 * 60 * 60 * 1000;
const PBKDF2_ITERATIONS = 100_000;
const MAX_BODY_BYTES = 2_000_000;
const BATCH_LIMIT = 100;
const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

class HttpError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
  }
}

function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8', ...CORS_HEADERS },
  });
}

function error(message: string, status: number): Response {
  return json({ error: message }, status);
}

function toHex(bytes: Uint8Array): string {
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
}

function randomHex(byteLength: number): string {
  const bytes = new Uint8Array(byteLength);
  crypto.getRandomValues(bytes);
  return toHex(bytes);
}

async function hashPassword(password: string, saltHex: string): Promise<string> {
  const saltBytes = new Uint8Array(
    saltHex.match(/.{2}/g)?.map((part) => parseInt(part, 16)) ?? [],
  );
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(password),
    'PBKDF2',
    false,
    ['deriveBits'],
  );
  const bits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', salt: saltBytes, iterations: PBKDF2_ITERATIONS, hash: 'SHA-256' },
    keyMaterial,
    256,
  );
  return toHex(new Uint8Array(bits));
}

function normalizeEmail(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const email = value.trim().toLowerCase();
  return email.length <= 254 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? email : null;
}

async function readJson(request: Request): Promise<Record<string, unknown>> {
  const text = await request.text();
  if (text.length > MAX_BODY_BYTES) throw new HttpError('Payload too large', 413);
  try {
    const parsed: unknown = JSON.parse(text);
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      throw new Error('not an object');
    }
    return parsed as Record<string, unknown>;
  } catch {
    throw new HttpError('Invalid JSON', 400);
  }
}

async function createSession(env: Env, userId: string): Promise<string> {
  const token = randomHex(32);
  const now = Date.now();
  await env.DB.prepare(
    'INSERT INTO sessions (token, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)',
  )
    .bind(token, userId, now, now + SESSION_DURATION_MS)
    .run();
  return token;
}

async function authenticate(request: Request, env: Env): Promise<string | null> {
  const header = request.headers.get('Authorization') ?? '';
  if (!header.startsWith('Bearer ')) return null;
  const token = header.slice('Bearer '.length).trim();
  if (!token) return null;
  const row = await env.DB.prepare(
    'SELECT user_id FROM sessions WHERE token = ? AND expires_at > ?',
  )
    .bind(token, Date.now())
    .first<{ user_id: string }>();
  return row ? row.user_id : null;
}

async function handleRegister(request: Request, env: Env): Promise<Response> {
  const body = await readJson(request);
  const email = normalizeEmail(body.email);
  const password = body.password;
  if (!email) return error('Invalid email', 400);
  if (typeof password !== 'string' || password.length < 6 || password.length > 128) {
    return error('Password must be at least 6 characters', 400);
  }
  const existing = await env.DB.prepare('SELECT id FROM users WHERE email = ?')
    .bind(email)
    .first<{ id: string }>();
  if (existing) return error('Email already registered', 409);

  const salt = randomHex(16);
  const passwordHash = await hashPassword(password, salt);
  const userId = crypto.randomUUID();
  await env.DB.prepare(
    'INSERT INTO users (id, email, password_hash, salt, created_at) VALUES (?, ?, ?, ?, ?)',
  )
    .bind(userId, email, passwordHash, salt, Date.now())
    .run();
  const token = await createSession(env, userId);
  return json({ token, userId, serverTime: Date.now() });
}

async function handleLogin(request: Request, env: Env): Promise<Response> {
  const body = await readJson(request);
  const email = normalizeEmail(body.email);
  const password = body.password;
  if (!email || typeof password !== 'string') return error('Invalid credentials', 401);
  const user = await env.DB.prepare(
    'SELECT id, password_hash, salt FROM users WHERE email = ?',
  )
    .bind(email)
    .first<{ id: string; password_hash: string; salt: string }>();
  if (!user) return error('Invalid credentials', 401);
  const hash = await hashPassword(password, user.salt);
  if (hash !== user.password_hash) return error('Invalid credentials', 401);
  const token = await createSession(env, user.id);
  return json({ token, userId: user.id, serverTime: Date.now() });
}

async function handleLogout(request: Request, env: Env): Promise<Response> {
  const header = request.headers.get('Authorization') ?? '';
  const token = header.startsWith('Bearer ') ? header.slice('Bearer '.length).trim() : '';
  if (token) {
    await env.DB.prepare('DELETE FROM sessions WHERE token = ?').bind(token).run();
  }
  return json({ ok: true });
}

interface HabitRecord {
  id: string;
  name: string;
  colorArgb: number;
  glyph: string;
  sortOrder: number;
  reminderEnabled: boolean;
  reminderHour: number | null;
  reminderMinute: number | null;
  targetEnabled: boolean;
  dailyTargetCount: number | null;
  createdAtEpochMillis: number;
  archived: boolean;
  updatedAtEpochMillis: number;
}

interface EventRecord {
  id: string;
  habitId: string;
  occurredAtEpochMillis: number;
  localDate: string;
  isBackfilled: boolean;
  deletedAtEpochMillis: number | null;
  updatedAtEpochMillis: number;
  note: string | null;
}

interface DayEventRecord {
  id: string;
  name: string;
  eventDate: string;
  repeatsMonthly: boolean;
  repeatsYearly: boolean;
  note: string | null;
  sortOrder: number;
  calendarType: string;
  lunarMonth: number | null;
  lunarDay: number | null;
  lunarLeap: boolean;
  createdAtEpochMillis: number;
  archived: boolean;
  updatedAtEpochMillis: number;
}

function asString(value: unknown, maxLength = 200): string | null {
  return typeof value === 'string' && value.length > 0 && value.length <= maxLength
    ? value
    : null;
}

function asNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function isHabit(value: unknown): value is HabitRecord {
  const item = value as Record<string, unknown>;
  return (
    asString(item.id) !== null &&
    asString(item.name) !== null &&
    asNumber(item.colorArgb) !== null &&
    asNumber(item.sortOrder) !== null &&
    asNumber(item.createdAtEpochMillis) !== null &&
    asNumber(item.updatedAtEpochMillis) !== null
  );
}

function isEvent(value: unknown): value is EventRecord {
  const item = value as Record<string, unknown>;
  const note = item.note;
  return (
    asString(item.id) !== null &&
    asString(item.habitId) !== null &&
    asNumber(item.occurredAtEpochMillis) !== null &&
    typeof item.localDate === 'string' &&
    DATE_RE.test(item.localDate) &&
    asNumber(item.updatedAtEpochMillis) !== null &&
    (note === null || note === undefined || (typeof note === 'string' && note.length <= 200))
  );
}

function isDayEvent(value: unknown): value is DayEventRecord {
  const item = value as Record<string, unknown>;
  const note = item.note;
  const calendarType = item.calendarType;
  return (
    asString(item.id) !== null &&
    asString(item.name) !== null &&
    typeof item.eventDate === 'string' &&
    DATE_RE.test(item.eventDate) &&
    (item.repeatsMonthly === undefined || item.repeatsMonthly === true || item.repeatsMonthly === false) &&
    asNumber(item.sortOrder) !== null &&
    (calendarType === undefined || calendarType === 'SOLAR' || calendarType === 'LUNAR') &&
    (item.lunarMonth === null || item.lunarMonth === undefined || asNumber(item.lunarMonth) !== null) &&
    (item.lunarDay === null || item.lunarDay === undefined || asNumber(item.lunarDay) !== null) &&
    asNumber(item.createdAtEpochMillis) !== null &&
    asNumber(item.updatedAtEpochMillis) !== null &&
    (note === null || note === undefined || (typeof note === 'string' && note.length <= 200))
  );
}

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
  ];
}

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
    event.createdAtEpochMillis,
    event.archived ? 1 : 0,
    event.updatedAtEpochMillis,
    firstSeenAt,
  ];
}

async function handleSync(request: Request, env: Env): Promise<Response> {
  const userId = await authenticate(request, env);
  if (!userId) return error('Unauthorized', 401);
  const body = await readJson(request);
  const since = typeof body.since === 'number' && body.since > 0 ? Math.floor(body.since) : 0;
  const now = Date.now();
  const habitsIn = Array.isArray(body.habits) ? body.habits.filter(isHabit) : [];
  const eventsIn = Array.isArray(body.events) ? body.events.filter(isEvent) : [];
  const dayEventsIn = Array.isArray(body.dayEvents) ? body.dayEvents.filter(isDayEvent) : [];
  const habitStatements = habitsIn.map((habit) =>
    env.DB.prepare(
      `INSERT INTO habits
         (id, user_id, name, color_argb, glyph, sort_order, reminder_enabled, reminder_hour,
          reminder_minute, target_enabled, daily_target_count, created_at, archived, updated_at,
          first_seen_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         name = excluded.name, color_argb = excluded.color_argb, glyph = excluded.glyph,
         sort_order = excluded.sort_order, reminder_enabled = excluded.reminder_enabled,
         reminder_hour = excluded.reminder_hour, reminder_minute = excluded.reminder_minute,
         target_enabled = excluded.target_enabled, daily_target_count = excluded.daily_target_count,
         created_at = excluded.created_at, archived = excluded.archived, updated_at = excluded.updated_at
       WHERE excluded.updated_at > habits.updated_at AND habits.user_id = excluded.user_id`,
    ).bind(...habitBindings(userId, habit, now)),
  );
  for (let index = 0; index < habitStatements.length; index += BATCH_LIMIT) {
    await env.DB.batch(habitStatements.slice(index, index + BATCH_LIMIT));
  }

  const { results: habitRows } = await env.DB.prepare('SELECT id FROM habits WHERE user_id = ?')
    .bind(userId)
    .all<{ id: string }>();
  const validHabitIds = new Set(habitRows.map((row) => row.id));
  const eventStatements = eventsIn
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
      ).bind(...eventBindings(userId, event, now)),
    );
  for (let index = 0; index < eventStatements.length; index += BATCH_LIMIT) {
    await env.DB.batch(eventStatements.slice(index, index + BATCH_LIMIT));
  }

  const dayEventStatements = dayEventsIn.map((dayEvent) =>
    env.DB.prepare(
      `INSERT INTO day_events
         (id, user_id, name, event_date, repeats_monthly, repeats_yearly, note, sort_order, calendar_type,
          lunar_month, lunar_day, lunar_leap, created_at, archived, updated_at, first_seen_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         name = excluded.name, event_date = excluded.event_date,
         repeats_monthly = excluded.repeats_monthly,
         repeats_yearly = excluded.repeats_yearly, note = excluded.note,
         sort_order = excluded.sort_order,
         calendar_type = excluded.calendar_type,
         lunar_month = excluded.lunar_month, lunar_day = excluded.lunar_day,
         lunar_leap = excluded.lunar_leap,
         created_at = excluded.created_at, archived = excluded.archived,
         updated_at = excluded.updated_at
       WHERE excluded.updated_at > day_events.updated_at AND day_events.user_id = excluded.user_id`,
    ).bind(...dayEventBindings(userId, dayEvent, now)),
  );
  for (let index = 0; index < dayEventStatements.length; index += BATCH_LIMIT) {
    await env.DB.batch(dayEventStatements.slice(index, index + BATCH_LIMIT));
  }

  const { results: habits } = await env.DB.prepare(
    `SELECT id, name, color_argb, glyph, sort_order, reminder_enabled, reminder_hour,
            reminder_minute, target_enabled, daily_target_count, created_at, archived, updated_at
     FROM habits WHERE user_id = ? AND (updated_at > ? OR first_seen_at > ?) ORDER BY updated_at ASC`,
  )
    .bind(userId, since, since)
    .all();
  const { results: events } = await env.DB.prepare(
    `SELECT id, habit_id, occurred_at, local_date, is_backfilled, deleted_at, updated_at, note
     FROM events WHERE user_id = ? AND (updated_at > ? OR first_seen_at > ?) ORDER BY updated_at ASC`,
  )
    .bind(userId, since, since)
    .all();
  const { results: dayEvents } = await env.DB.prepare(
    `SELECT id, name, event_date, repeats_monthly, repeats_yearly, note, sort_order, calendar_type,
            lunar_month, lunar_day, lunar_leap, created_at, archived, updated_at
     FROM day_events WHERE user_id = ? AND (updated_at > ? OR first_seen_at > ?) ORDER BY updated_at ASC`,
  )
    .bind(userId, since, since)
    .all();

  return json({
    serverTime: Date.now(),
    habits: habits.map((row) => ({
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
    })),
    events: events.map((row) => ({
      id: row.id,
      habitId: row.habit_id,
      occurredAtEpochMillis: row.occurred_at,
      localDate: row.local_date,
      isBackfilled: !!row.is_backfilled,
      deletedAtEpochMillis: row.deleted_at ?? null,
      updatedAtEpochMillis: row.updated_at,
      note: row.note ?? null,
    })),
    dayEvents: dayEvents.map((row) => ({
      id: row.id,
      name: row.name,
      eventDate: row.event_date,
      repeatsMonthly: !!row.repeats_monthly,
      repeatsYearly: !!row.repeats_yearly,
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
  });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }
    const url = new URL(request.url);
    try {
      if (url.pathname === '/health' && request.method === 'GET') {
        return json({ ok: true });
      }
      if (url.pathname === '/auth/register' && request.method === 'POST') {
        return await handleRegister(request, env);
      }
      if (url.pathname === '/auth/login' && request.method === 'POST') {
        return await handleLogin(request, env);
      }
      if (url.pathname === '/auth/logout' && request.method === 'POST') {
        return await handleLogout(request, env);
      }
      if (url.pathname === '/sync' && request.method === 'POST') {
        return await handleSync(request, env);
      }
      return error('Not found', 404);
    } catch (err) {
      if (err instanceof HttpError) return error(err.message, err.status);
      return error('Internal error', 500);
    }
  },
};
