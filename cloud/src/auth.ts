// 账号体系：注册、登录、登出与 Bearer 鉴权
import { SESSION_DURATION_MS } from './constants';
import { hashPassword, randomHex } from './crypto';
import {
  deleteSessionByToken,
  emailExists,
  findUserByEmail,
  findUserBySession,
  insertSession,
  insertUser,
  pruneUserSessions,
} from './db';
import { error, json, readJson } from './http';
import type { Env } from './types';
import { normalizeEmail } from './validation';

// 从 Authorization 头提取 Bearer token；缺失或格式不符返回 null
export function extractBearerToken(request: Request): string | null {
  const header = request.headers.get('Authorization') ?? '';
  if (!header.startsWith('Bearer ')) return null;
  const token = header.slice('Bearer '.length).trim();
  return token || null;
}

// 校验请求登录态：返回对应用户 id，无效返回 null（由调用方决定如何响应）
export async function authenticate(request: Request, env: Env): Promise<string | null> {
  const token = extractBearerToken(request);
  if (!token) return null;
  return findUserBySession(env, token);
}

// 创建登录会话：生成 32 字节随机 token，有效期 90 天
async function createSession(env: Env, userId: string): Promise<string> {
  const token = randomHex(32);
  await insertSession(env, token, userId, Date.now() + SESSION_DURATION_MS);
  return token;
}

// POST /auth/register：邮箱注册并自动登录，返回 token + userId + serverTime
export async function handleRegister(request: Request, env: Env): Promise<Response> {
  const body = await readJson(request);
  const email = normalizeEmail(body.email);
  const password = body.password;
  if (!email) return error('Invalid email', 400);
  if (typeof password !== 'string' || password.length < 6 || password.length > 128) {
    return error('Password must be at least 6 characters', 400);
  }
  if (await emailExists(env, email)) return error('Email already registered', 409);

  // 密码只存 PBKDF2 加盐哈希，不落明文
  const salt = randomHex(16);
  const passwordHash = await hashPassword(password, salt);
  const userId = crypto.randomUUID();
  await insertUser(env, userId, email, passwordHash, salt);
  const token = await createSession(env, userId);
  return json({ token, userId, serverTime: Date.now() });
}

// POST /auth/login：校验邮箱密码后签发新会话
export async function handleLogin(request: Request, env: Env): Promise<Response> {
  const body = await readJson(request);
  const email = normalizeEmail(body.email);
  const password = body.password;
  if (!email || typeof password !== 'string') return error('Invalid credentials', 401);
  const user = await findUserByEmail(env, email);
  if (!user) return error('Invalid credentials', 401);
  const hash = await hashPassword(password, user.salt);
  if (hash !== user.password_hash) return error('Invalid credentials', 401);
  // 签发新 token 后清理该用户过期/超量 session，不影响多设备同时在线
  const token = await createSession(env, user.id);
  await pruneUserSessions(env, user.id);
  return json({ token, userId: user.id, serverTime: Date.now() });
}

// POST /auth/logout：删除当前会话；token 不存在也返回成功（幂等）
export async function handleLogout(request: Request, env: Env): Promise<Response> {
  const token = extractBearerToken(request);
  if (token) await deleteSessionByToken(env, token);
  return json({ ok: true });
}
