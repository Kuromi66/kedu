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

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/register` | 注册，body `{ email, password }` |
| POST | `/auth/login` | 登录，body `{ email, password }`，返回 `{ token, userId, serverTime }` |
| POST | `/auth/logout` | 登出，需 `Authorization: Bearer <token>` |
| POST | `/sync` | 同步，需 Bearer；body `{ since, habits[], events[] }`，返回 `{ serverTime, habits[], events[] }` |

同步采用单条记录 Last-Write-Wins：推送时服务端保留 `updated_at` 较大的版本，随后返回该用户 `updated_at > since` 的全部记录，客户端幂等合并。
