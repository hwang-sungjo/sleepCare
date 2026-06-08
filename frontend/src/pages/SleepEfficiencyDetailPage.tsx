import React, { useEffect, useState } from 'react';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
    ReferenceLine, ResponsiveContainer, Cell,
} from 'recharts';
import { TrendingUp, Award, AlertCircle } from 'lucide-react';
import { PageName, DailySleepRecord } from '../types';
import { getWeeklySleepHistory } from '../api/sleepHistory';
import PageWrapper from '../layouts/PageWrapper';

interface Props {
    onBack: () => void;
    onNavigate: (page: PageName) => void;
}

const GOOD_THRESHOLD = 85;
const FAIR_THRESHOLD = 70;

function getBarColor(value: number): string {
    if (value >= GOOD_THRESHOLD) return '#34d399'; // emerald-400
    if (value >= FAIR_THRESHOLD) return '#fbbf24'; // amber-400
    return '#f87171'; // red-400
}

function getGradeLabel(value: number): { label: string; color: string } {
    if (value >= GOOD_THRESHOLD) return { label: '양호', color: 'text-emerald-400' };
    if (value >= FAIR_THRESHOLD) return { label: '보통', color: 'text-amber-400' };
    return { label: '부족', color: 'text-red-400' };
}

const CustomTooltip = ({ active, payload, label }: any) => {
    if (!active || !payload?.length) return null;
    const val = payload[0].value as number;
    const { label: grade, color } = getGradeLabel(val);
    return (
        <div className="bg-slate-800 border border-slate-700 rounded-xl px-4 py-3 shadow-xl">
            <p className="text-slate-400 text-xs mb-1">{label}</p>
            <p className="text-white font-bold text-lg">{val}%</p>
            <p className={`text-xs font-semibold ${color}`}>{grade}</p>
        </div>
    );
};

const SleepEfficiencyDetailPage: React.FC<Props> = ({ onBack, onNavigate }) => {
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
            ? Math.round(records.reduce((s, r) => s + r.sleepEfficiency, 0) / records.length)
            : 0;

    const best = records.length > 0 ? Math.max(...records.map((r) => r.sleepEfficiency)) : 0;
    const worst = records.length > 0 ? Math.min(...records.map((r) => r.sleepEfficiency)) : 0;
    const { label: avgGrade, color: avgColor } = getGradeLabel(avg);

    return (
        <PageWrapper
            title="수면 효율"
            currentPage="sleepEfficiencyDetail"
            showBack
            onBack={onBack}
        >
            {loading ? (
                <div className="flex-1 flex items-center justify-center">
                    <div className="flex flex-col items-center gap-3">
                        <div className="w-10 h-10 border-2 border-emerald-400 border-t-transparent rounded-full animate-spin" />
                        <p className="text-slate-400 text-sm">데이터 불러오는 중...</p>
                    </div>
                </div>
            ) : (
                <>
                    {/* 요약 카드 3개 */}
                    <div className="grid grid-cols-3 gap-3 mb-6">
                        <div className="bg-slate-900 rounded-2xl p-4 border border-white/5 text-center">
                            <TrendingUp size={18} className="text-emerald-400 mx-auto mb-1" />
                            <p className="text-slate-400 text-xs mb-1">평균 효율</p>
                            <p className="text-xl font-bold">{avg}%</p>
                            <p className={`text-xs font-semibold mt-1 ${avgColor}`}>{avgGrade}</p>
                        </div>
                        <div className="bg-slate-900 rounded-2xl p-4 border border-white/5 text-center">
                            <Award size={18} className="text-indigo-400 mx-auto mb-1" />
                            <p className="text-slate-400 text-xs mb-1">최고</p>
                            <p className="text-xl font-bold text-emerald-400">{best}%</p>
                        </div>
                        <div className="bg-slate-900 rounded-2xl p-4 border border-white/5 text-center">
                            <AlertCircle size={18} className="text-slate-400 mx-auto mb-1" />
                            <p className="text-slate-400 text-xs mb-1">최저</p>
                            <p className="text-xl font-bold text-red-400">{worst}%</p>
                        </div>
                    </div>

                    {/* 막대 차트 */}
                    <div className="bg-slate-900 rounded-3xl p-5 border border-white/5 mb-6">
                        <p className="text-sm text-slate-400 mb-4">최근 7일 수면 효율</p>
                        <ResponsiveContainer width="100%" height={200}>
                            <BarChart
                                data={records}
                                margin={{ top: 4, right: 4, left: -24, bottom: 0 }}
                                barCategoryGap="30%"
                            >
                                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                                <XAxis
                                    dataKey="dayLabel"
                                    tick={{ fill: '#94a3b8', fontSize: 12 }}
                                    axisLine={false}
                                    tickLine={false}
                                />
                                <YAxis
                                    domain={[40, 100]}
                                    tick={{ fill: '#94a3b8', fontSize: 11 }}
                                    axisLine={false}
                                    tickLine={false}
                                    tickFormatter={(v) => `${v}%`}
                                />
                                <Tooltip content={<CustomTooltip />} cursor={{ fill: '#1e293b' }} />
                                <ReferenceLine
                                    y={GOOD_THRESHOLD}
                                    stroke="#34d399"
                                    strokeDasharray="4 4"
                                    strokeOpacity={0.5}
                                    label={{ value: '양호', fill: '#34d399', fontSize: 10, position: 'right' }}
                                />
                                <Bar dataKey="sleepEfficiency" radius={[6, 6, 0, 0]}>
                                    {records.map((entry, index) => (
                                        <Cell key={index} fill={getBarColor(entry.sleepEfficiency)} />
                                    ))}
                                </Bar>
                            </BarChart>
                        </ResponsiveContainer>
                    </div>

                    {/* 날짜별 상세 리스트 */}
                    <div className="bg-slate-900 rounded-3xl border border-white/5 overflow-hidden">
                        <p className="text-sm text-slate-400 px-5 pt-5 pb-3">일별 상세</p>
                        {records.map((record, idx) => {
                            const { label, color } = getGradeLabel(record.sleepEfficiency);
                            return (
                                <div
                                    key={record.date}
                                    className={`flex items-center justify-between px-5 py-3.5 ${
                                        idx < records.length - 1 ? 'border-b border-slate-800' : ''
                                    }`}
                                >
                                    <div className="flex items-center gap-3">
                                        <div
                                            className="w-2 h-2 rounded-full"
                                            style={{ backgroundColor: getBarColor(record.sleepEfficiency) }}
                                        />
                                        <div>
                                            <p className="text-sm font-medium">
                                                {record.date.slice(5).replace('-', '/')} ({record.dayLabel})
                                            </p>
                                            <p className="text-xs text-slate-500">
                                                {record.sleepStartTime} ~ {record.sleepEndTime}
                                            </p>
                                        </div>
                                    </div>
                                    <div className="text-right">
                                        <p className="font-bold">{record.sleepEfficiency}%</p>
                                        <p className={`text-xs font-semibold ${color}`}>{label}</p>
                                    </div>
                                </div>
                            );
                        })}
                    </div>

                    {/* 챗봇 안내 */}
                    <div className="mt-4 bg-indigo-900/20 border border-indigo-500/20 rounded-2xl p-4 flex items-center gap-3">
                        <span className="text-2xl">🤖</span>
                        <div className="flex-1">
                            <p className="text-sm font-medium">AI 수면 분석이 궁금하신가요?</p>
                            <p className="text-xs text-slate-400 mt-0.5">챗봇에게 수면 효율에 대해 물어보세요</p>
                        </div>
                        <button
                            id="go-chatbot-from-efficiency"
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

export default SleepEfficiencyDetailPage;
