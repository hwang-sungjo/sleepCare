const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

interface ApiResponse<T> {
    code: number;
    status: number;
    message?: string;
    result: T;
}

async function request<T>(
    path: string,
    options: RequestInit = {},
    token?: string
): Promise<T> {
    const headers: HeadersInit = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (token) {
        (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
    }

    const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });
    const data: ApiResponse<T> = await res.json();

    if (!res.ok || data.code === undefined) {
        throw new Error(data.message ?? '서버 오류가 발생했습니다.');
    }

    // 성공 코드가 1000번대가 아닌 경우 에러로 처리
    if (data.code < 1000 || data.code >= 2000) {
        throw new Error(data.message ?? '요청에 실패했습니다.');
    }

    return data.result;
}

export const api = {
    get: <T>(path: string, token?: string) =>
        request<T>(path, { method: 'GET' }, token),

    post: <T>(path: string, body: unknown, token?: string) =>
        request<T>(path, { method: 'POST', body: JSON.stringify(body) }, token),

    patch: <T>(path: string, body: unknown, token?: string) =>
        request<T>(path, { method: 'PATCH', body: JSON.stringify(body) }, token),
};
