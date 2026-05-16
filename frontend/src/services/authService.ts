import { api } from './api';

export interface SignUpRequest {
    nickname: string;
    password: string;
}

export interface SignUpResponse {
    userId: number;
    jwt: string;
}

export interface LoginRequest {
    nickname: string;
    password: string;
}

export interface LoginResponse {
    userId: number;
    jwt: string;
}

export const authService = {
    signUp: (body: SignUpRequest) =>
        api.post<SignUpResponse>('/users', body),

    login: (body: LoginRequest) =>
        api.post<LoginResponse>('/auth/login', body),
};
