import React, { useEffect, useState } from 'react';
import { Activity, Clock, Settings } from 'lucide-react';
import { PageName } from '../types';
import PageWrapper from '../layouts/PageWrapper';
import AlarmCard from '../components/AlarmCard';
import { getDashboard, DashboardResponse } from '../api/dashboard';
import { getAlarm } from '../api/alarm';
import { getMe } from '../api/user';
import { ApiError } from '../api/client';

interface HomePageProps {
    nickname: string;
    notification: string | null;
    onNavigate: (page: PageName) => void;
    onLogout: () => void;
    onNicknameLoaded: (nickname: string) => void;
}

function formatMinutes(totalMinutes: number): string {
    if (!totalMinutes || totalMinutes <= 0) return '0h 0m';
    const hours = Math.floor(totalMinutes / 60);
    const mins = totalMinutes % 60;
    return `${hours}h ${mins}m`;
}

function extractHHmm(isoOrNull: string | null): string {
    if (!isoOrNull) return '미설정';
    // ISO datetime → HH:mm 추출
    const d = new Date(isoOrNull);
    if (isNaN(d.getTime())) return '미설정';
    const hh = String(d.getHours()).padStart(2, '0');
    const mm = String(d.getMinutes()).padStart(2, '0');
    return `${hh}:${mm}`;
}

const HomePage: React.FC<HomePageProps> = ({ nickname, notification, onNavigate, onLogout, onNicknameLoaded }) => {
    const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
    const [alarmTime, setAlarmTime] = useState<string>('--:--');
    const [loadError, setLoadError] = useState<string | null>(null);

    useEffect(() => {
        let active = true;

        Promise.all([getDashboard(), getAlarm(), getMe()])
            .then(([d, a, me]) => {
                if (!active) return;
                setDashboard(d);
                setAlarmTime(extractHHmm(a.todayEffectiveWakeAt));
                if (me.nickname && me.nickname !== nickname) {
                    onNicknameLoaded(me.nickname);
                }
            })
            .catch((err: unknown) => {
                if (!active) return;
                const message = err instanceof ApiError ? err.message : '데이터를 불러오지 못했습니다.';
                setLoadError(message);
            });

        return () => {
            active = false;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <PageWrapper title="대시보드" currentPage="home" notification={notification}>
            <div className="mb-8">
                <p className="text-slate-400 mb-1">좋은 밤입니다,</p>
                <h2 className="text-3xl font-bold">{nickname || 'Sleepy User'} 님 🌙</h2>
            </div>

            {loadError && (
                <div className="mb-6 px-4 py-3 rounded-2xl bg-rose-900/30 border border-rose-500/30 text-rose-200 text-sm">
                    {loadError}
                </div>
            )}

            <section className="mb-10">
                <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-4">오늘 알람</h3>
                <AlarmCard time={alarmTime} onClick={() => onNavigate('setAlarm')} />
            </section>

            <section className="grid grid-cols-2 gap-4">
                <div className="bg-slate-900 p-5 rounded-3xl border border-white/5">
                    <div className="text-emerald-400 mb-2"><Activity size={24} /></div>
                    <div className="text-sm text-slate-400">수면 효율</div>
                    <div className="text-xl font-bold">{dashboard ? `${dashboard.sleepEfficiencyPercent}%` : '—'}</div>
                </div>
                <div className="bg-slate-900 p-5 rounded-3xl border border-white/5">
                    <div className="text-indigo-400 mb-2"><Clock size={24} /></div>
                    <div className="text-sm text-slate-400">평균 수면</div>
                    <div className="text-xl font-bold">{dashboard ? formatMinutes(dashboard.averageSleepDurationMinutes) : '—'}</div>
                </div>
            </section>

            <div className="mt-8 bg-indigo-900/30 p-6 rounded-3xl border border-indigo-500/20">
                <h4 className="font-bold mb-2 flex items-center gap-2">
                    <Settings size={18} className="text-indigo-300" />
                    수면 가이드
                </h4>
                <p className="text-sm text-indigo-200/80 leading-relaxed">
                    {dashboard ? dashboard.environmentHint : '데이터를 불러오는 중입니다...'}
                </p>
            </div>

            <div className="mt-auto pt-6 flex justify-center">
                <button
                    onClick={onLogout}
                    className="text-slate-600 text-sm hover:text-slate-400 underline underline-offset-4"
                >
                    로그아웃
                </button>
            </div>
        </PageWrapper>
    );
};

export default HomePage;
