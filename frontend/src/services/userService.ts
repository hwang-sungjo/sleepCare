import { api } from './api';

export interface GetUserProfileResponse {
    nickname: string;
}

export const userService = {
    getUserProfile: (token: string) =>
        api.get<GetUserProfileResponse>('/users/me', token),
};
