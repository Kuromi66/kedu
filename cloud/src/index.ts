// Workers 入口：路由分发与统一错误处理
// 业务逻辑按职责拆分在 auth / sync / db / validation / http / crypto / constants 等模块
import { handleLogin, handleLogout, handleRegister } from './auth';
import { CORS_HEADERS, VERSION_MANIFEST } from './constants';
import { purgeDeletedRecords } from './db';
import { error, HttpError, json } from './http';
import { handleSync, handleSyncMeta } from './sync';
import type { Env } from './types';

export type { Env };

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    // CORS 预检请求：直接返回 204，不做业务处理
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }
    const url = new URL(request.url);
    try {
      // 健康检查：不依赖数据库，供部署/监控探活
      if (url.pathname === '/health' && request.method === 'GET') {
        return json({ ok: true });
      }
      // 版本清单：公开接口，供客户端「检查更新」读取，无需登录
      if (url.pathname === '/version' && request.method === 'GET') {
        return json(VERSION_MANIFEST);
      }
      // 账号体系
      if (url.pathname === '/auth/register' && request.method === 'POST') {
        return await handleRegister(request, env);
      }
      if (url.pathname === '/auth/login' && request.method === 'POST') {
        return await handleLogin(request, env);
      }
      if (url.pathname === '/auth/logout' && request.method === 'POST') {
        return await handleLogout(request, env);
      }
      // 多端数据同步
      if (url.pathname === '/sync' && request.method === 'POST') {
        return await handleSync(request, env);
      }
      // 同步对帐：轻量摘要，供客户端校验与补拉
      if (url.pathname === '/sync/meta' && request.method === 'POST') {
        return await handleSyncMeta(request, env);
      }
      return error('Not found', 404);
    } catch (err) {
      // 业务异常按状态码返回；其余兜底为 500，避免向客户端泄漏内部细节
      if (err instanceof HttpError) return error(err.message, err.status);
      return error('Internal error', 500);
    }
  },
  // 定时任务：每周清理超过保留期的彻底删除墓碑，避免 D1 中长期堆积无用行
  async scheduled(_controller: ScheduledController, env: Env): Promise<void> {
    await purgeDeletedRecords(env);
  },
};
