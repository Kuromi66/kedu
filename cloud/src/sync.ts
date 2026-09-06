// 数据同步：POST /sync 处理器
// 流程：先落库客户端推送（LWW），再返回该用户自 since 以来的全部变更；
// 若请求带 fetchIds（对帐补拉），则只返回指定 id 的最新正文，避免全量回显
import { authenticate } from './auth';
import {
  fetchChangedRecords,
  fetchRecordMeta,
  fetchRecordsByIds,
  upsertDayEvents,
  upsertEvents,
  upsertHabits,
} from './db';
import { error, json, readJson } from './http';
import type { Env } from './types';
import { isDayEvent, isEvent, isHabit } from './validation';

// 抽取 id 数组：仅保留非空字符串
function sanitizeIds(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((id): id is string => typeof id === 'string' && id.length > 0) : [];
}

export async function handleSync(request: Request, env: Env): Promise<Response> {
  const userId = await authenticate(request, env);
  if (!userId) return error('Unauthorized', 401);
  const body = await readJson(request);

  // since 为客户端上次成功同步的服务端水位，非法值按全量处理
  const since = typeof body.since === 'number' && body.since > 0 ? Math.floor(body.since) : 0;
  const now = Date.now();

  // 推送的记录先按 DTO 校验，再进入各自的 upsert
  const habitsIn = Array.isArray(body.habits) ? body.habits.filter(isHabit) : [];
  const eventsIn = Array.isArray(body.events) ? body.events.filter(isEvent) : [];
  const dayEventsIn = Array.isArray(body.dayEvents) ? body.dayEvents.filter(isDayEvent) : [];

  // 先习惯后打卡：同批同步中新创建的习惯需要先落库，打卡的 habitId 归属校验才能通过
  await upsertHabits(env, userId, habitsIn, now);
  await upsertEvents(env, userId, eventsIn, now);
  await upsertDayEvents(env, userId, dayEventsIn, now);

  // 对帐补拉：带 fetchIds 时只返回指定 id 的最新正文；否则走增量拉取（按 since 水位）
  const fetchIds = body.fetchIds as { habits?: unknown; events?: unknown; dayEvents?: unknown } | undefined;
  const hasFetchIds = Boolean(
    fetchIds && (sanitizeIds(fetchIds.habits).length > 0 || sanitizeIds(fetchIds.events).length > 0 || sanitizeIds(fetchIds.dayEvents).length > 0),
  );
  if (hasFetchIds) {
    const changes = await fetchRecordsByIds(env, userId, {
      habits: sanitizeIds(fetchIds!.habits),
      events: sanitizeIds(fetchIds!.events),
      dayEvents: sanitizeIds(fetchIds!.dayEvents),
    });
    return json({ serverTime: Date.now(), ...changes });
  }

  // 增量拉取：客户端用 serverTime 推进水位，保证拉取与写入在同一时间基准
  const changes = await fetchChangedRecords(env, userId, since);
  return json({ serverTime: Date.now(), ...changes });
}

// POST /sync/meta 处理器：返回该用户全部记录的轻量摘要（id + updatedAt），
// 供客户端对帐时与本地做双向版本比对，正文按需通过 /sync 的 fetchIds 补拉
export async function handleSyncMeta(request: Request, env: Env): Promise<Response> {
  const userId = await authenticate(request, env);
  if (!userId) return error('Unauthorized', 401);
  const meta = await fetchRecordMeta(env, userId);
  return json({ serverTime: Date.now(), ...meta });
}
