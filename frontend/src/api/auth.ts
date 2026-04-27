import { request } from './client';

export interface SignupResponse {
    message: string;
    userId: string;
}

export interface LoginResponse {
    token: string;
    userName: string;
    userId: string;
}

export function signup(userId: string, password: string): Promise<SignupResponse> {
    return request<SignupResponse>('/api/auth/signup', {
        method: 'POST',
        body: { userId, password },
    });
}

export function login(userId: string, password: string): Promise<LoginResponse> {
    return request<LoginResponse>('/api/auth/login', {
        method: 'POST',
        body: { userId, password },
    });
}
