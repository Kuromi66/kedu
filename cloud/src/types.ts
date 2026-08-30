// Workers 环境绑定类型：DB 由 wrangler.toml 中的 [[d1_databases]] 注入
export interface Env {
  DB: D1Database;
}
