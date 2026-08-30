// 密码学工具：随机数、hex 编码与 PBKDF2 密码哈希
// 全部基于 Web Crypto API，可在 Workers 运行时直接使用
import { PBKDF2_ITERATIONS } from './constants';

// 字节数组转 hex 字符串（用于 token、盐、密码哈希的存储与比较）
export function toHex(bytes: Uint8Array): string {
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
}

// 生成 byteLength 字节的随机 hex 字符串
export function randomHex(byteLength: number): string {
  const bytes = new Uint8Array(byteLength);
  crypto.getRandomValues(bytes);
  return toHex(bytes);
}

// PBKDF2-SHA256 密码哈希：saltHex 为 16 字节盐的 hex 表示，输出 256 位 hex
export async function hashPassword(password: string, saltHex: string): Promise<string> {
  const saltBytes = new Uint8Array(
    saltHex.match(/.{2}/g)?.map((part) => parseInt(part, 16)) ?? [],
  );
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(password),
    'PBKDF2',
    false,
    ['deriveBits'],
  );
  const bits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', salt: saltBytes, iterations: PBKDF2_ITERATIONS, hash: 'SHA-256' },
    keyMaterial,
    256,
  );
  return toHex(new Uint8Array(bits));
}
