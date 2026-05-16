import React from 'react';
import { Activity, Clock, Settings } from 'lucide-react';
import { PageName } from '../types';
import { GetAlarmResponse } from '../services/alarmService';
import PageWrapper from '../layouts/PageWrapper';
import AlarmCard from '../components/AlarmCard';

interface HomePageProps {
    alarmTime: string;
    alarmData: GetAlarmResponse | null;
    notification: string | null;
    onNavigate: (page: PageName) => void;
    onLogout: () => void;
}

const HomePage: React.FC<HomePageProps> = ({ alarmTime, alarmData, notification, onNavigate, onLogout }) => {
    const todayAlarm = alarmData?.alarms.find(a => a.dayOfWeek === alarmData.todayDayOfWeek);
    const displayTime = todayAlarm?.baseWakeTime ?? alarmTime;

    return (
        <PageWrapper title="대시보드" currentPage="home" notification={notification}>
            <div className="mb-8">
                <p className="text-slate-400 mb-1">좋은 밤입니다,</p>
                <h2 className="text-3xl font-bold">DeepSleep 님 🌙</h2>
            </div>

            <section className="mb-10">
                <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-4">현재 설정된 알람</h3>
                <AlarmCard time={displayTime} onClick={() => onNavigate('setAlarm')} />
            </section>

            <section className="grid grid-cols-2 gap-4">
                <div className="bg-slate-900 p-5 rounded-3xl border border-white/5">
                    <div className="text-emerald-400 mb-2"><Activity size={24} /></div>
                    <div className="text-sm text-slate-400">수면 효율</div>
                    <div className="text-xl font-bold">92%</div>
                </div>
                <div className="bg-slate-900 p-5 rounded-3xl border border-white/5">
                    <div className="text-indigo-400 mb-2"><Clock size={24} /></div>
                    <div className="text-sm text-slate-400">평균 수면</div>
                    <div className="text-xl font-bold">7h 20m</div>
                </div>
            </section>

            <div className="mt-8 bg-indigo-900/30 p-6 rounded-3xl border border-indigo-500/20">
                <h4 className="font-bold mb-2 flex items-center gap-2">
                    <Settings size={18} className="text-indigo-300" />
                    수면 가이드
                </h4>
                <p className="text-sm text-indigo-200/80 leading-relaxed">
                    웨어러블 기기 분석 결과, 오늘은 습도가 높습니다. 에어컨 제습 모드를 24도로 설정하는 것을 추천드려요.
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
