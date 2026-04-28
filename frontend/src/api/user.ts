import { request } from './client';

export interface UserProfileResponse {
    nickname: string;
}

export function getMe(): Promise<UserProfileResponse> {
    return request<UserProfileResponse>('/users/me', { auth: true });
}
