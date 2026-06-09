import React from 'react';
import { Activity, Clock, Settings, ChevronRight, MessageCircle } from 'lucide-react';
import { PageName } from '../types';
import { GetAlarmResponse } from '../services/alarmService';
import { GetSleepDashboardResponse } from '../services/dashboardService';
import PageWrapper from '../layouts/PageWrapper';
import AlarmCard from '../components/AlarmCard';
import CitationList from '../components/CitationList';

interface HomePageProps {
    alarmTime: string;
    alarmData: GetAlarmResponse | null;
    nickname: string;
    dashboardData: GetSleepDashboardResponse | null;
    notification: string | null;
    onNavigate: (page: PageName) => void;
    onLogout: () => void;
}

const HomePage: React.FC<HomePageProps> = ({ alarmTime, alarmData, nickname, dashboardData, notification, onNavigate, onLogout }) => {
    const todayAlarm = alarmData?.alarms.find(a => a.dayOfWeek === alarmData.todayDayOfWeek);
    const displayTime = todayAlarm?.baseWakeTime ?? alarmTime;

    return (
        <PageWrapper title="대시보드" currentPage="home" notification={notification}>
            <div className="mb-8">
                <p className="text-slate-400 mb-1">좋은 밤입니다,</p>
                <h2 className="text-3xl font-bold">{nickname} 님 🌙</h2>
            </div>

            <section className="mb-10">
                <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-4">현재 설정된 알람</h3>
                <AlarmCard time={displayTime} onClick={() => onNavigate('setAlarm')} />
            </section>

            {/* 수면 지표 카드 — 클릭 시 상세 페이지로 이동 */}
            <section className="grid grid-cols-2 gap-4">
                <button
                    id="sleep-efficiency-card"
                    onClick={() => onNavigate('sleepEfficiencyDetail')}
                    className="bg-slate-900 p-5 rounded-3xl border border-white/5 text-left hover:border-emerald-500/30 hover:bg-slate-800/80 transition-all group"
                >
                    <div className="flex items-start justify-between mb-2">
                        <div className="text-emerald-400"><Activity size={24} /></div>
                        <ChevronRight size={14} className="text-slate-600 group-hover:text-emerald-400 transition-colors mt-0.5" />
                    </div>
                    <div className="text-sm text-slate-400">수면 효율</div>
                    <div className="text-xl font-bold">
                        {dashboardData?.sleepEfficiencyPercent != null ? `${dashboardData.sleepEfficiencyPercent}%` : '-'}
                    </div>
                    <div className="text-xs text-slate-600 mt-1">7일 상세 보기</div>
                </button>
                <button
                    id="sleep-duration-card"
                    onClick={() => onNavigate('sleepDurationDetail')}
                    className="bg-slate-900 p-5 rounded-3xl border border-white/5 text-left hover:border-indigo-500/30 hover:bg-slate-800/80 transition-all group"
                >
                    <div className="flex items-start justify-between mb-2">
                        <div className="text-indigo-400"><Clock size={24} /></div>
                        <ChevronRight size={14} className="text-slate-600 group-hover:text-indigo-400 transition-colors mt-0.5" />
                    </div>
                    <div className="text-sm text-slate-400">평균 수면</div>
                    <div className="text-xl font-bold">
                        {dashboardData?.averageSleepDurationMinutes != null
                            ? `${Math.floor(dashboardData.averageSleepDurationMinutes / 60)}h ${dashboardData.averageSleepDurationMinutes % 60}m`
                            : '-'}
                    </div>
                    <div className="text-xs text-slate-600 mt-1">7일 상세 보기</div>
                </button>
            </section>

            <div className="mt-8 bg-indigo-900/30 p-6 rounded-3xl border border-indigo-500/20">
                <h4 className="font-bold mb-2 flex items-center gap-2">
                    <Settings size={18} className="text-indigo-300" />
                    수면 가이드
                </h4>
                <p className="text-sm text-indigo-200/80 leading-relaxed">
                    {dashboardData?.aiAdvice || '오늘의 수면 가이드를 준비 중입니다.'}
                </p>
                {dashboardData?.citations && dashboardData.citations.length > 0 && (
                    <CitationList citations={dashboardData.citations} variant="compact" />
                )}
            </div>

            {/* 챗봇 진입 버튼 */}
            <button
                id="open-chatbot"
                onClick={() => onNavigate('chatbot')}
                className="mt-4 w-full flex items-center gap-3 bg-gradient-to-r from-indigo-900/40 to-purple-900/40 border border-indigo-500/25 hover:border-indigo-400/50 hover:from-indigo-900/60 hover:to-purple-900/60 transition-all p-4 rounded-3xl group"
            >
                <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/30 shrink-0">
                    <MessageCircle size={18} className="text-white" />
                </div>
                <div className="text-left">
                    <p className="text-sm font-semibold">AI 수면 챗봇</p>
                    <p className="text-xs text-slate-400">수면 데이터 기반 맞춤 분석 · 조언</p>
                </div>
                <ChevronRight size={16} className="text-slate-600 group-hover:text-indigo-400 transition-colors ml-auto" />
            </button>

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
