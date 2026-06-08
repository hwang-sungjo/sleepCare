import React, { useEffect, useState } from 'react';
import {
    AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip,
    ReferenceLine, ResponsiveContainer,
} from 'recharts';
import { Clock, Moon, Sunrise } from 'lucide-react';
import { PageName, DailySleepRecord } from '../types';
import { getWeeklySleepHistory } from '../api/sleepHistory';
import PageWrapper from '../layouts/PageWrapper';

interface Props {
    onBack: () => void;
    onNavigate: (page: PageName) => void;
}

const RECOMMENDED_MINUTES = 480; // 8시간

function formatDuration(minutes: number): string {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return `${h}시간 ${m > 0 ? `${m}분` : ''}`.trim();
}

const CustomTooltip = ({ active, payload, label }: any) => {
    if (!active || !payload?.length) return null;
    const val = payload[0].value as number;
    return (
        <div className="bg-slate-800 border border-slate-700 rounded-xl px-4 py-3 shadow-xl">
            <p className="text-slate-400 text-xs mb-1">{label}</p>
            <p className="text-white font-bold">{formatDuration(val)}</p>
            {val < RECOMMENDED_MINUTES ? (
                <p className="text-amber-400 text-xs mt-0.5">권장 미달 {formatDuration(RECOMMENDED_MINUTES - val)}</p>
            ) : (
                <p className="text-emerald-400 text-xs mt-0.5">권장 충족 ✓</p>
            )}
        </div>
    );
};

const SleepDurationDetailPage: React.FC<Props> = ({ onBack, onNavigate }) => {
    const [records, setRecords] = useState<DailySleepRecord[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        getWeeklySleepHistory().then((data) => {
            setRecords(data);
            setLoading(false);
        });
    }, []);

    const avg =
        records.length > 0
            ? Math.round(records.reduce((s, r) => s + r.sleepDurationMinutes, 0) / records.length)
            : 0;
    const maxDur = records.length > 0 ? Math.max(...records.map((r) => r.sleepDurationMinutes)) : 0;
    const minDur = records.length > 0 ? Math.min(...records.map((r) => r.sleepDurationMinutes)) : 0;

    // 차트용 데이터: 수면 시간(분) + 수면 단계 구성
    const chartData = records.map((r) => ({
        dayLabel: r.dayLabel,
        date: r.date,
        sleepDurationMinutes: r.sleepDurationMinutes,
        deepMins: r.deepMins,
        remMins: r.remMins,
        lightMins: r.lightMins,
    }));

    return (
        <PageWrapper
            title="수면 시간"
            currentPage="sleepDurationDetail"
            showBack
            onBack={onBack}
        >
            {loading ? (
                <div className="flex-1 flex items-center justify-center">
                    <div className="flex flex-col items-center gap-3">
                        <div className="w-10 h-10 border-2 border-indigo-400 border-t-transparent rounded-full animate-spin" />
                        <p className="text-slate-400 text-sm">데이터 불러오는 중...</p>
                    </div>
                </div>
            ) : (
                <>
                    {/* 요약 카드 */}
                    <div className="grid grid-cols-3 gap-3 mb-6">
                        <div className="bg-slate-900 rounded-2xl p-4 border border-white/5 text-center">
                            <Clock size={18} className="text-indigo-400 mx-auto mb-1" />
                            <p className="text-slate-400 text-xs mb-1">평균</p>
                            <p className="text-base font-bold leading-tight">
                                {Math.floor(avg / 60)}h {avg % 60}m
                            </p>
                        </div>
                        <div className="bg-slate-900 rounded-2xl p-4 border border-white/5 text-center">
                            <Sunrise size={18} className="text-emerald-400 mx-auto mb-1" />
                            <p className="text-slate-400 text-xs mb-1">최장</p>
                            <p className="text-base font-bold leading-tight text-emerald-400">
                                {Math.floor(maxDur / 60)}h {maxDur % 60}m
                            </p>
                        </div>
                        <div className="bg-slate-900 rounded-2xl p-4 border border-white/5 text-center">
                            <Moon size={18} className="text-slate-400 mx-auto mb-1" />
                            <p className="text-slate-400 text-xs mb-1">최단</p>
                            <p className="text-base font-bold leading-tight text-amber-400">
                                {Math.floor(minDur / 60)}h {minDur % 60}m
                            </p>
                        </div>
                    </div>

                    {/* 영역 차트 */}
                    <div className="bg-slate-900 rounded-3xl p-5 border border-white/5 mb-6">
                        <p className="text-sm text-slate-400 mb-4">최근 7일 수면 시간</p>
                        <ResponsiveContainer width="100%" height={200}>
                            <AreaChart
                                data={chartData}
                                margin={{ top: 4, right: 4, left: -10, bottom: 0 }}
                            >
                                <defs>
                                    <linearGradient id="sleepGrad" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#818cf8" stopOpacity={0.4} />
                                        <stop offset="95%" stopColor="#818cf8" stopOpacity={0.02} />
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                                <XAxis
                                    dataKey="dayLabel"
                                    tick={{ fill: '#94a3b8', fontSize: 12 }}
                                    axisLine={false}
                                    tickLine={false}
                                />
                                <YAxis
                                    domain={[240, 600]}
                                    tick={{ fill: '#94a3b8', fontSize: 11 }}
                                    axisLine={false}
                                    tickLine={false}
                                    tickFormatter={(v) => `${Math.floor(v / 60)}h`}
                                />
                                <Tooltip content={<CustomTooltip />} />
                                <ReferenceLine
                                    y={RECOMMENDED_MINUTES}
                                    stroke="#34d399"
                                    strokeDasharray="4 4"
                                    strokeOpacity={0.6}
                                    label={{ value: '권장 8h', fill: '#34d399', fontSize: 10, position: 'right' }}
                                />
                                <Area
                                    type="monotone"
                                    dataKey="sleepDurationMinutes"
                                    stroke="#818cf8"
                                    strokeWidth={2.5}
                                    fill="url(#sleepGrad)"
                                    dot={{ fill: '#818cf8', strokeWidth: 0, r: 4 }}
                                    activeDot={{ fill: '#a5b4fc', r: 6, strokeWidth: 0 }}
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>

                    {/* 수면 단계 구성 바 */}
                    <div className="bg-slate-900 rounded-3xl border border-white/5 p-5 mb-4">
                        <p className="text-sm text-slate-400 mb-4">수면 단계 구성 (분)</p>
                        <div className="space-y-3">
                            {records.map((record, idx) => {
                                const total = record.deepMins + record.remMins + record.lightMins + record.wakeMins;
                                return (
                                    <div key={idx}>
                                        <div className="flex justify-between text-xs text-slate-400 mb-1.5">
                                            <span>{record.date.slice(5).replace('-', '/')} ({record.dayLabel})</span>
                                            <span>{formatDuration(record.sleepDurationMinutes)}</span>
                                        </div>
                                        <div className="flex rounded-full overflow-hidden h-3 gap-0.5">
                                            <div
                                                style={{ width: `${(record.deepMins / total) * 100}%` }}
                                                className="bg-indigo-500 rounded-l-full"
                                                title={`깊은 수면: ${record.deepMins}분`}
                                            />
                                            <div
                                                style={{ width: `${(record.remMins / total) * 100}%` }}
                                                className="bg-violet-500"
                                                title={`REM: ${record.remMins}분`}
                                            />
                                            <div
                                                style={{ width: `${(record.lightMins / total) * 100}%` }}
                                                className="bg-blue-400"
                                                title={`얕은 수면: ${record.lightMins}분`}
                                            />
                                            <div
                                                style={{ width: `${(record.wakeMins / total) * 100}%` }}
                                                className="bg-slate-600 rounded-r-full"
                                                title={`각성: ${record.wakeMins}분`}
                                            />
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                        {/* 범례 */}
                        <div className="flex gap-4 mt-4 flex-wrap">
                            {[
                                { color: 'bg-indigo-500', label: '깊은 수면' },
                                { color: 'bg-violet-500', label: 'REM' },
                                { color: 'bg-blue-400', label: '얕은 수면' },
                                { color: 'bg-slate-600', label: '각성' },
                            ].map(({ color, label }) => (
                                <div key={label} className="flex items-center gap-1.5 text-xs text-slate-400">
                                    <div className={`w-2.5 h-2.5 rounded-sm ${color}`} />
                                    {label}
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* 챗봇 안내 */}
                    <div className="bg-indigo-900/20 border border-indigo-500/20 rounded-2xl p-4 flex items-center gap-3">
                        <span className="text-2xl">🤖</span>
                        <div className="flex-1">
                            <p className="text-sm font-medium">수면 시간 개선하고 싶으신가요?</p>
                            <p className="text-xs text-slate-400 mt-0.5">AI에게 맞춤 조언을 받아보세요</p>
                        </div>
                        <button
                            id="go-chatbot-from-duration"
                            onClick={() => onNavigate('chatbot')}
                            className="text-xs bg-indigo-600 hover:bg-indigo-500 transition-colors px-3 py-1.5 rounded-xl font-medium shrink-0"
                        >
                            챗봇
                        </button>
                    </div>
                </>
            )}
        </PageWrapper>
    );
};

export default SleepDurationDetailPage;
