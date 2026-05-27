import { request } from './client';

export interface DashboardCitationItem {
    location: string | null;
    snippet: string | null;
}

export interface DashboardResponse {
    sleepEfficiencyPercent: number;
    averageSleepDurationMinutes: number;
    environmentHint: string;
    aiAdvice?: string | null;
    citations?: DashboardCitationItem[] | null;
}

export function getDashboard(): Promise<DashboardResponse> {
    return request<DashboardResponse>('/dashboard/sleep-summary', { auth: true });
}
