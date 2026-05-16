import { api } from './api';

export interface GetSleepDashboardResponse {
    sleepEfficiencyPercent: number | null;
    averageSleepDurationMinutes: number | null;
    environmentHint: string | null;
}

export const dashboardService = {
    getSleepSummary: (token: string) =>
        api.get<GetSleepDashboardResponse>('/dashboard/sleep-summary', token),
};
