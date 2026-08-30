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

// 读取并解析请求体：限制大小，要求顶层为 JSON 对象
export async function readJson(request: Request): Promise<Record<string, unknown>> {
  const text = await request.text();
  if (text.length > MAX_BODY_BYTES) throw new HttpError('Payload too large', 413);
  try {
    const parsed: unknown = JSON.parse(text);
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      throw new Error('not an object');
    }
    return parsed as Record<string, unknown>;
  } catch {
    throw new HttpError('Invalid JSON', 400);
  }
}
