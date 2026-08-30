// 请求校验：记录 DTO 类型定义与基础字段校验
// 这些接口与客户端同步 DTO 一一对应，保证跨端字段形状稳定
import { DATE_RE } from './constants';

// 习惯记录（含软删除标记，删除后仍参与同步以便传播到其他设备）
export interface HabitRecord {
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
  deletedAtEpochMillis: number | null;
}

// 打卡记录（软删除墓碑：deletedAtEpochMillis 非空表示已删除）
export interface EventRecord {
  id: string;
  habitId: string;
  occurredAtEpochMillis: number;
  localDate: string;
  isBackfilled: boolean;
  deletedAtEpochMillis: number | null;
  updatedAtEpochMillis: number;
  note: string | null;
}

// 重要日期记录（支持阳历/农历、按月/按年重复）
export interface DayEventRecord {
  id: string;
  name: string;
  eventDate: string;
  repeatsMonthly: boolean;
  repeatsYearly: boolean;
  reminderEnabled: boolean;
  reminderDaysBefore: number;
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

// 邮箱规范化与校验：去空白、转小写，限制长度与格式
export function normalizeEmail(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const email = value.trim().toLowerCase();
  return email.length <= 254 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? email : null;
}

// 非空字符串校验：限制长度，超长或为空返回 null
export function asString(value: unknown, maxLength = 200): string | null {
  return typeof value === 'string' && value.length > 0 && value.length <= maxLength
    ? value
    : null;
}

// 有限数值校验：排除 NaN/Infinity
export function asNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

// 习惯记录校验：必填字段齐全即通过，宽松校验以便老客户端兼容
export function isHabit(value: unknown): value is HabitRecord {
  const item = value as Record<string, unknown>;
  return (
    asString(item.id) !== null &&
    asString(item.name) !== null &&
    asNumber(item.colorArgb) !== null &&
    asNumber(item.sortOrder) !== null &&
    asNumber(item.createdAtEpochMillis) !== null &&
    asNumber(item.updatedAtEpochMillis) !== null &&
    (item.deletedAtEpochMillis === null ||
      item.deletedAtEpochMillis === undefined ||
      asNumber(item.deletedAtEpochMillis) !== null)
  );
}

// 打卡记录校验：localDate 必须符合 YYYY-MM-DD，note 可空且限长
export function isEvent(value: unknown): value is EventRecord {
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

// 重要日期校验：calendarType 仅接受 SOLAR/LUNAR，可选字段缺省时按默认值处理
export function isDayEvent(value: unknown): value is DayEventRecord {
  const item = value as Record<string, unknown>;
  const note = item.note;
  const calendarType = item.calendarType;
  return (
    asString(item.id) !== null &&
    asString(item.name) !== null &&
    typeof item.eventDate === 'string' &&
    DATE_RE.test(item.eventDate) &&
    (item.repeatsMonthly === undefined ||
      item.repeatsMonthly === true ||
      item.repeatsMonthly === false) &&
    (item.reminderEnabled === undefined ||
      item.reminderEnabled === true ||
      item.reminderEnabled === false) &&
    (item.reminderDaysBefore === undefined || asNumber(item.reminderDaysBefore) !== null) &&
    asNumber(item.sortOrder) !== null &&
    (calendarType === undefined || calendarType === 'SOLAR' || calendarType === 'LUNAR') &&
    (item.lunarMonth === null ||
      item.lunarMonth === undefined ||
      asNumber(item.lunarMonth) !== null) &&
    (item.lunarDay === null ||
      item.lunarDay === undefined ||
      asNumber(item.lunarDay) !== null) &&
    asNumber(item.createdAtEpochMillis) !== null &&
    asNumber(item.updatedAtEpochMillis) !== null &&
    (note === null || note === undefined || (typeof note === 'string' && note.length <= 200))
  );
}
