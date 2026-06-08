import React from 'react';
import { LucideIcon } from 'lucide-react';

export type PageName =
    | 'login'
    | 'signup'
    | 'home'
    | 'setAlarm'
    | 'sleepEfficiencyDetail'
    | 'sleepDurationDetail'
    | 'chatbot';

/** 최근 7일 수면 일별 기록 (Mock 또는 실제 API 응답 공통 형태) */
export interface DailySleepRecord {
    date: string;                 // "YYYY-MM-DD"
    dayLabel: string;             // "월", "화", ... 단축 요일
    sleepEfficiency: number;      // 0~100 (%)
    sleepDurationMinutes: number; // 총 수면 시간(분)
    sleepStartTime: string;       // "HH:mm"
    sleepEndTime: string;         // "HH:mm"
    deepMins: number;
    remMins: number;
    lightMins: number;
    wakeMins: number;
}

export interface ButtonProps {
    children: React.ReactNode;
    onClick?: () => void;
    variant?: 'primary' | 'secondary' | 'outline';
    className?: string;
    type?: 'button' | 'submit' | 'reset';
}

export interface InputFieldProps {
    label: string;
    type?: string;
    placeholder?: string;
    value?: string;
    onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
    icon?: LucideIcon;
}

export interface AlarmCardProps {
    time: string;
    onClick: () => void;
}

export interface PageWrapperProps {
    children: React.ReactNode;
    title: string;
    showBack?: boolean;
    onBack?: () => void;
    currentPage: PageName;
}
