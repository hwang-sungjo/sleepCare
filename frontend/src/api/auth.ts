import { request } from './client';

export interface SignupResponse {
    userId: number;
    jwt: string;
}

export interface LoginResponse {
    userId: number;
    jwt: string;
}

export interface SignupBody {
    nickname: string;
    password: string;
    fitbitUserId?: string;
    fitbitUserPassword?: string;
}

export function signup(body: SignupBody): Promise<SignupResponse> {
    // 빈 문자열은 보내지 않도록 정리 (원격이 fitbit 필드를 모르거나 검증을 거는 경우 대비)
    const payload: any = {
        nickname: body.nickname,
        password: body.password,
    };
    if (body.fitbitUserId && body.fitbitUserId.trim()) payload.fitbit_user_id = body.fitbitUserId.trim();
    if (body.fitbitUserPassword && body.fitbitUserPassword) payload.fitbit_user_password = body.fitbitUserPassword;

    return request<SignupResponse>('/users', { method: 'POST', body: payload });
}

export function login(nickname: string, password: string): Promise<LoginResponse> {
    return request<LoginResponse>('/auth/login', {
        method: 'POST',
        body: { nickname, password },
    });
}
