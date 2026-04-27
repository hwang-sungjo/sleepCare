import { request } from './client';

export interface AlarmResponse {
    alarmTime: string;
    isEnabled: boolean;
}

export interface AlarmUpsertResponse {
    message: string;
    alarmTime: string;
}

export function getAlarm(): Promise<AlarmResponse> {
    return request<AlarmResponse>('/api/alarms', { auth: true });
}

export function upsertAlarm(alarmTime: string): Promise<AlarmUpsertResponse> {
    return request<AlarmUpsertResponse>('/api/alarms', {
        method: 'POST',
        body: { alarmTime },
        auth: true,
    });
}
