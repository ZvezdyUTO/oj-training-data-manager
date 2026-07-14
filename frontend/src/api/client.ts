export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: number | null,
    message: string,
    readonly body: unknown,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

const API_BASE = import.meta.env.VITE_API_BASE || '/api';

export function operationHeaders(password: string): HeadersInit {
  if (!password.trim()) throw new Error('请输入操作密码。');
  return { 'X-Operation-Password': password };
}

export async function requestData<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has('Accept')) headers.set('Accept', 'application/json');
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const response = await fetch(API_BASE + path, { ...init, headers });
  const body = await response.json().catch(() => null) as ApiResult<T> | null;
  if (!response.ok || !body || body.code !== 200) {
    throw new ApiError(
      response.status,
      body?.code ?? null,
      body?.message || 'HTTP ' + response.status,
      body,
    );
  }
  return body.data;
}
