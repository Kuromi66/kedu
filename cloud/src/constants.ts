// 全局常量：CORS、会话有效期、安全参数与同步限额集中管理

// CORS 响应头：允许浏览器端跨域调用
export const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
};

// 登录会话有效期：90 天
export const SESSION_DURATION_MS = 90 * 24 * 60 * 60 * 1000;

// 单个用户最多保留的有效会话数（多设备在线上限，超出清理最陈旧项）
export const SESSION_MAX_PER_USER = 10;

// 密码哈希迭代次数：兼顾安全与性能
export const PBKDF2_ITERATIONS = 100_000;

// 请求体大小上限（字节）：防止超大同步包拖垮 Worker
export const MAX_BODY_BYTES = 2_000_000;

// D1 batch 单批最大语句数：超过时需分批执行
export const BATCH_LIMIT = 100;

// 彻底删除墓碑保留期：超过该时长后由定时任务物理清除。
// 必须大于任意设备可能离线的时间，否则离线设备会把已删除记录重新上传
export const DELETED_RETENTION_MS = 90 * 24 * 60 * 60 * 1000;

// localDate 格式校验：YYYY-MM-DD
export const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

// 应用版本清单：每次发版时更新这里，并随 Worker 一起部署。
// 客户端「检查更新」读取该清单，versionCode 大于当前版本即提示更新。
export const VERSION_MANIFEST = {
  versionCode: 15,
  versionName: '1.7.1',
  notes:
    '云同步性能与稳定性优化：日常同步改为只上传增量记录；新增定时对帐（每 12 小时与进入前台时按版本双向补全，时钟漂移也稳）；同步请求体 gzip 压缩；服务端批量写入；修复首条重要日期因默认字段缺失被静默丢弃的问题；登录自动清理过期与超量会话。',
  url: 'https://github.com/Kuromi66/kedu/releases/tag/v1.7.1',
};
