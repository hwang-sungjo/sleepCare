import { DailySleepRecord } from '../types';
import { dashboardService } from '../services/dashboardService';

/** 최근 7일(KST) 수면 상세 기록을 백엔드에서 조회한다. */
export function getWeeklySleepHistory(token: string): Promise<DailySleepRecord[]> {
    return dashboardService.getWeeklySleepHistory(token).then((res) => res.records ?? []);
}
