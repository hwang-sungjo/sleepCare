import { request } from './client';

export interface DashboardResponse {
    sleepEfficiency: number;
    averageSleepTime: string;
    guideMessage: string;
}

export function getDashboard(): Promise<DashboardResponse> {
    return request<DashboardResponse>('/api/dashboard', { auth: true });
}
