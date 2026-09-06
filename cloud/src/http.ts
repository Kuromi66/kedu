// HTTP 工具：统一响应格式、错误对象与请求体解析
import { CORS_HEADERS, MAX_BODY_BYTES } from './constants';

// 业务异常：携带 HTTP 状态码，由入口统一转成 JSON 错误响应
export class HttpError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
  }
}

// 统一 JSON 响应，自动附带 CORS 头
export function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8', ...CORS_HEADERS },
  });
}

// 统一错误响应：{ error: 文案 }
export function error(message: string, status: number): Response {
  return json({ error: message }, status);
}

// 读取并解析请求体：限制大小，要求顶层为 JSON 对象。
// 兼容客户端 gzip 压缩的请求体。Cloudflare 对入站 Content-Encoding: gzip 的处理不一致，
// 且常会丢失该请求头。因此在解析时自动探测：先按明文解析，失败则尝试 gzip 解压后再解析，
// 不依赖请求头来判断是否解压
export async function readJson(request: Request): Promise<Record<string, unknown>> {
  const bytes = new Uint8Array(await request.arrayBuffer());
  let parsed = tryParseJson(bytes);
  if (parsed === undefined) {
    try {
      const decompressor = new DecompressionStream('gzip');
      const plain = new Uint8Array(
        await new Response(new Blob([bytes]).stream().pipeThrough(decompressor)).arrayBuffer(),
      );
      parsed = tryParseJson(plain);
    } catch {
      parsed = undefined;
    }
  }
  if (parsed === undefined) throw new HttpError('Invalid JSON', 400);
  if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
    throw new HttpError('Invalid JSON', 400);
  }
  return parsed as Record<string, unknown>;
}

// 尝试按明文 UTF-8 解析为 JSON 对象；失败返回 undefined（用 undefined 区分"解析失败"与"JSON null"）
function tryParseJson(bytes: Uint8Array): unknown {
  const text = new TextDecoder().decode(bytes);
  if (text.length > MAX_BODY_BYTES) throw new HttpError('Payload too large', 413);
  try {
    const parsed: unknown = JSON.parse(text);
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      return undefined;
    }
    return parsed;
  } catch {
    return undefined;
  }
}
