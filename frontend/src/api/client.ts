const TOKEN_KEY = 'sleepCare.token';

export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
    localStorage.removeItem(TOKEN_KEY);
}

// 원격 백엔드의 BaseErrorResponse 포맷
export interface ApiErrorBody {
    code?: number;
    status?: number;
    message?: string;
    timestamp?: string;
}

export class ApiError extends Error {
    readonly status: number;
    readonly code?: number;

    constructor(status: number, body: ApiErrorBody | null, fallback: string) {
        super(body?.message || fallback);
        this.status = status;
        this.code = body?.code;
    }
}

interface RequestOptions {
    method?: 'GET' | 'POST' | 'PATCH' | 'DELETE';
    body?: unknown;
    auth?: boolean;
}

export async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
    const { method = 'GET', body, auth = false } = opts;
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };

    if (auth) {
        const token = getToken();
        if (token) headers['Authorization'] = `Bearer ${token}`;
    }

    const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';
    const url = path.startsWith('http') ? path : `${API_BASE_URL}${path}`;

    const res = await fetch(url, {
        method,
        headers,
        body: body == null ? undefined : JSON.stringify(body),
    });

    if (!res.ok) {
        let errBody: ApiErrorBody | null = null;
        try {
            errBody = (await res.json()) as ApiErrorBody;
        } catch {
            errBody = null;
        }
        throw new ApiError(res.status, errBody, `요청 실패 (${res.status})`);
    }

    if (res.status === 204) return undefined as T;
    const data = await res.json();
    // 백엔드가 { code, status, message, result } 형태로 래핑하여 보내는 경우 처리
    if (data && 'result' in data) {
        return data.result as T;
    }
    return data as T;
}
