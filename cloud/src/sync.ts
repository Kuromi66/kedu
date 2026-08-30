// 数据同步：POST /sync 处理器
// 流程：先落库客户端推送（LWW），再返回该用户自 since 以来的全部变更
import { authenticate } from './auth';
import { fetchChangedRecords, upsertDayEvents, upsertEvents, upsertHabits } from './db';
import { error, json, readJson } from './http';
import type { Env } from './types';
import { isDayEvent, isEvent, isHabit } from './validation';

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

  // 增量拉取：客户端用 serverTime 推进水位，保证拉取与写入在同一时间基准
  const changes = await fetchChangedRecords(env, userId, since);
  return json({ serverTime: Date.now(), ...changes });
}
