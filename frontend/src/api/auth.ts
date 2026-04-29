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
}

export function signup(body: SignupBody): Promise<SignupResponse> {
    const payload = {
        nickname: body.nickname,
        password: body.password,
    };

    return request<SignupResponse>('/users', { method: 'POST', body: payload });
}

export function login(nickname: string, password: string): Promise<LoginResponse> {
    return request<LoginResponse>('/auth/login', {
        method: 'POST',
        body: { nickname, password },
    });
}
