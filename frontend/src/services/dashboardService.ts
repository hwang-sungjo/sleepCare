import { api } from './api';

/** Bedrock KB 인용 — 챗봇 `citations` 와 동일 구조 */
export interface DashboardCitationItem {
    location: string | null;
    snippet: string | null;
}

export interface GetSleepDashboardResponse {
    sleepEfficiencyPercent: number | null;
    averageSleepDurationMinutes: number | null;
    environmentHint: string | null;
    aiAdvice?: string | null;
    citations?: DashboardCitationItem[] | null;
}

export const dashboardService = {
    getSleepSummary: (token: string) =>
        api.get<GetSleepDashboardResponse>('/dashboard/sleep-summary', token),
};
