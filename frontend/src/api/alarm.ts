import { request } from './client';

export interface DailyAlarm {
    dayOfWeek: number;            // 1=월 ... 7=일
    baseWakeTime: string;         // "HH:mm"
    dynamicWakeAt: string | null; // ISO datetime
    adaptiveEnabled: boolean;
    windowMinutesBefore: number;
}

export interface AlarmResponse {
    todayDayOfWeek: number;
    todayEffectiveWakeAt: string | null;
    alarms: DailyAlarm[];
}

export interface PatchAlarmRequest {
    dayOfWeek: number;
    baseWakeTime?: string;
    adaptiveEnabled?: boolean;
    windowMinutesBefore?: number;
    recomputeDynamicNow?: boolean;
}

export function getAlarm(): Promise<AlarmResponse> {
    return request<AlarmResponse>('/alarms', { auth: true });
}

export function patchAlarm(body: PatchAlarmRequest): Promise<AlarmResponse> {
    return request<AlarmResponse>('/alarms', { method: 'PATCH', body, auth: true });
}
