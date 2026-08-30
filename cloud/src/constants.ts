// 全局常量：CORS、会话有效期、安全参数与同步限额集中管理

// CORS 响应头：允许浏览器端跨域调用
export const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
};

// 登录会话有效期：90 天
export const SESSION_DURATION_MS = 90 * 24 * 60 * 60 * 1000;

// 密码哈希迭代次数：兼顾安全与性能
export const PBKDF2_ITERATIONS = 100_000;

// 请求体大小上限（字节）：防止超大同步包拖垮 Worker
export const MAX_BODY_BYTES = 2_000_000;

// D1 batch 单批最大语句数：超过时需分批执行
export const BATCH_LIMIT = 100;

// localDate 格式校验：YYYY-MM-DD
export const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

// 应用版本清单：每次发版时更新这里，并随 Worker 一起部署。
// 客户端「检查更新」读取该清单，versionCode 大于当前版本即提示更新。
export const VERSION_MANIFEST = {
  versionCode: 14,
  versionName: '1.7.0',
  notes:
    '新增检查更新功能：设置页可手动检查更新，发现新版本会弹窗展示说明并可跳转下载；后台每日检查并推送新版本提醒。',
  url: 'https://github.com/Kuromi66/kedu/releases/tag/v1.7.0',
};
