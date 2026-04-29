import { request } from './client';

export interface DashboardResponse {
    sleepEfficiencyPercent: number;
    averageSleepDurationMinutes: number;
    environmentHint: string;
}

export function getDashboard(): Promise<DashboardResponse> {
    return request<DashboardResponse>('/dashboard/sleep-summary', { auth: true });
}
