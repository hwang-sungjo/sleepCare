import React from 'react';
import { LucideIcon } from 'lucide-react';

export type PageName = 'login' | 'signup' | 'home' | 'setAlarm';

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
