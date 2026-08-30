# Pulse 云端同步后端

基于 Cloudflare Workers + D1 的同步服务，为「刻度」应用提供账号与多设备数据同步。

## 部署步骤

1. 安装依赖：`npm install`
2. 登录 Cloudflare：`npx wrangler login`
3. 创建 D1 数据库：`npx wrangler d1 create pulse`
4. 把输出中的 `database_id` 填到 `wrangler.toml`
5. 执行数据库迁移：`npm run migrate:remote`（本地调试可用 `npm run migrate:local`）
6. 本地调试：`npm run dev`；正式部署：`npm run deploy`

部署后得到形如 `https://pulse-sync.<子域名>.workers.dev` 的地址，把它写入 Android 工程根目录 `local.properties`：

```properties
PULSE_API_BASE_URL=https://pulse-sync.<子域名>.workers.dev
```

## 定时任务

Worker 注册了每周日凌晨 04:00（UTC）的 Cron 触发，自动物理清除超过 90 天保留期的「彻底删除」墓碑（习惯与打卡），并兜底清理指向已不存在习惯的孤儿打卡。保留期大于常见离线时长，确保删除能先同步到其他设备再被清除。

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/register` | 注册，body `{ email, password }` |
| POST | `/auth/login` | 登录，body `{ email, password }`，返回 `{ token, userId, serverTime }` |
| POST | `/auth/logout` | 登出，需 `Authorization: Bearer <token>` |
| POST | `/sync` | 同步，需 Bearer；body `{ since, habits[], events[], dayEvents[] }`，返回 `{ serverTime, habits[], events[], dayEvents[] }` |
| GET | `/version` | 版本清单，无需登录，供客户端检查更新 |

同步采用单条记录 Last-Write-Wins：推送时服务端保留 `updated_at` 较大的版本，随后返回该用户 `updated_at > since` 的全部记录，客户端幂等合并。

## 代码结构

`src/` 按职责拆分，便于维护与扩展：

| 文件 | 职责 |
| --- | --- |
| `index.ts` | Workers 入口：路由分发、统一错误处理与定时清理入口 |
| `types.ts` | 环境绑定类型（D1） |
| `constants.ts` | CORS、会话有效期、同步限额、版本清单等常量 |
| `http.ts` | JSON/错误响应与请求体解析 |
| `validation.ts` | 记录 DTO 类型与字段校验 |
| `crypto.ts` | 随机数、hex 与 PBKDF2 密码哈希 |
| `db.ts` | D1 全部 SQL：会话、用户、习惯/打卡/重要日期 upsert 与增量拉取 |
| `auth.ts` | 注册、登录、登出与 Bearer 鉴权 |
| `sync.ts` | 同步处理器：先落库推送（LWW），再返回增量变更 |
