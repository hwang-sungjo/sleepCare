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

export interface ApiErrorBody {
    errorCode?: string;
    message?: string;
    timestamp?: string;
}

export class ApiError extends Error {
    readonly status: number;
    readonly errorCode?: string;

    constructor(status: number, body: ApiErrorBody | null, fallback: string) {
        super(body?.message || fallback);
        this.status = status;
        this.errorCode = body?.errorCode;
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

    const res = await fetch(path, {
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
    return (await res.json()) as T;
}
