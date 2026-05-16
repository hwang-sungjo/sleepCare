import { api } from './api';

export interface DailyAlarmItem {
    dayOfWeek: number;       // 1=월 ... 7=일
    baseWakeTime: string;    // "HH:mm"
    dynamicWakeAt: string | null;
    adaptiveEnabled: boolean;
    windowMinutesBefore: number;
}

export interface GetAlarmResponse {
    todayDayOfWeek: number;
    todayEffectiveWakeAt: string | null;
    alarms: DailyAlarmItem[];
}

export interface PatchAlarmRequest {
    dayOfWeek: number;
    baseWakeTime?: string;
    adaptiveEnabled?: boolean;
    windowMinutesBefore?: number;
    recomputeDynamicNow?: boolean;
}

export const alarmService = {
    getAlarm: (token: string) =>
        api.get<GetAlarmResponse>('/alarms', token),

    patchAlarm: (body: PatchAlarmRequest, token: string) =>
        api.patch<GetAlarmResponse>('/alarms', body, token),
};
