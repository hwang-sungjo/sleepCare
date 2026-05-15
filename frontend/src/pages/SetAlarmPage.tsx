import React, { useEffect, useState } from 'react';
import { Clock } from 'lucide-react';
import PageWrapper from '../layouts/PageWrapper';
import Button from '../components/Button';
import { getAlarm, patchAlarm, DailyAlarm } from '../api/alarm';
import { ApiError } from '../api/client';

interface SetAlarmPageProps {
    notification: string | null;
    onSaved: (message: string) => void;
    onBack: () => void;
}

const DAY_LABELS = ['', '월', '화', '수', '목', '금', '토', '일']; // 1..7
const DEFAULT_TIME = '07:30';
const DEFAULT_WINDOW = 30;

interface RowState {
    dayOfWeek: number;
    baseWakeTime: string;
    adaptiveEnabled: boolean;
    windowMinutesBefore: number;
    /** 서버에 저장된 원본 (변경 여부 비교용) */
    original: { baseWakeTime: string; adaptiveEnabled: boolean; windowMinutesBefore: number } | null;
}

function makeInitialRows(): RowState[] {
    return [1, 2, 3, 4, 5, 6, 7].map((d) => ({
        dayOfWeek: d,
        baseWakeTime: DEFAULT_TIME,
        adaptiveEnabled: false,
        windowMinutesBefore: DEFAULT_WINDOW,
        original: null,
    }));
}

function applyServerAlarms(initial: RowState[], serverAlarms: DailyAlarm[]): RowState[] {
    return initial.map((row) => {
        const found = serverAlarms.find((a) => a.dayOfWeek === row.dayOfWeek);
        if (!found) return row;
        return {
            dayOfWeek: row.dayOfWeek,
            baseWakeTime: found.baseWakeTime?.substring(0, 5) ?? DEFAULT_TIME,
            adaptiveEnabled: found.adaptiveEnabled,
            windowMinutesBefore: found.windowMinutesBefore,
            original: {
                baseWakeTime: found.baseWakeTime?.substring(0, 5) ?? DEFAULT_TIME,
                adaptiveEnabled: found.adaptiveEnabled,
                windowMinutesBefore: found.windowMinutesBefore,
            },
        };
    });
}

function isRowDirty(row: RowState): boolean {
    if (row.original === null) return row.adaptiveEnabled || row.baseWakeTime !== DEFAULT_TIME || row.windowMinutesBefore !== DEFAULT_WINDOW;
    return (
        row.original.baseWakeTime !== row.baseWakeTime ||
        row.original.adaptiveEnabled !== row.adaptiveEnabled ||
        row.original.windowMinutesBefore !== row.windowMinutesBefore
    );
}

const SetAlarmPage: React.FC<SetAlarmPageProps> = ({ notification, onSaved, onBack }) => {
    const [rows, setRows] = useState<RowState[]>(makeInitialRows);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        let active = true;
        getAlarm()
            .then((res) => {
                if (!active) return;
                setRows((prev) => applyServerAlarms(prev, res.alarms ?? []));
            })
            .catch((err: unknown) => {
                if (!active) return;
                const msg = err instanceof ApiError ? err.message : '알람 정보를 불러오지 못했습니다.';
                setLoadError(msg);
            });
        return () => { active = false; };
    }, []);

    const updateRow = (day: number, patch: Partial<RowState>) => {
        setRows((prev) => prev.map((r) => (r.dayOfWeek === day ? { ...r, ...patch } : r)));
    };

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        const dirty = rows.filter(isRowDirty);
        if (dirty.length === 0) {
            onSaved('변경된 항목이 없습니다.');
            return;
        }
        setSaving(true);
        try {
            // 변경된 요일만 순차 PATCH (요일별 1행이라 병렬도 가능하나 단순화 위해 순차)
            for (const r of dirty) {
                await patchAlarm({
                    dayOfWeek: r.dayOfWeek,
                    baseWakeTime: r.baseWakeTime,
                    adaptiveEnabled: r.adaptiveEnabled,
                    windowMinutesBefore: r.windowMinutesBefore,
                });
            }
            onSaved(`${dirty.length}개 요일의 알람이 저장되었습니다.`);
        } catch (err) {
            const msg = err instanceof ApiError ? err.message : '알람 저장 중 오류가 발생했습니다.';
            onSaved(msg);
        } finally {
            setSaving(false);
        }
    };

    return (
        <PageWrapper title="알람 설정" showBack onBack={onBack} currentPage="setAlarm" notification={notification}>
            <p className="text-slate-400 mb-6">
                요일별로 기상 시간을 설정하세요.
                <br />
                적응형 모드를 켜면 설정 시간 전 윈도우 안에서 가장 가벼운 순간에 깨워줍니다.
            </p>

            {loadError && (
                <div className="mb-4 px-4 py-3 rounded-2xl bg-rose-900/30 border border-rose-500/30 text-rose-200 text-sm">
                    {loadError}
                </div>
            )}

            <form onSubmit={handleSave} className="flex flex-col flex-1">
                <div className="flex flex-col gap-3 mb-6">
                    {rows.map((r) => {
                        const dirty = isRowDirty(r);
                        return (
                            <div
                                key={r.dayOfWeek}
                                className={`bg-slate-900 rounded-2xl p-4 border ${dirty ? 'border-indigo-500/40' : 'border-white/5'}`}
                            >
                                <div className="flex items-center gap-3">
                                    <span className="w-8 h-8 rounded-full bg-indigo-600/30 flex items-center justify-center text-sm font-bold text-indigo-200">
                                        {DAY_LABELS[r.dayOfWeek]}
                                    </span>
                                    <Clock size={16} className="text-slate-500" />
                                    <input
                                        type="time"
                                        value={r.baseWakeTime}
                                        onChange={(e) => updateRow(r.dayOfWeek, { baseWakeTime: e.target.value })}
                                        className="bg-transparent text-2xl font-light text-white focus:outline-none focus:text-indigo-400 transition-colors cursor-pointer"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => updateRow(r.dayOfWeek, { adaptiveEnabled: !r.adaptiveEnabled })}
                                        className={`ml-auto px-3 py-1.5 rounded-full text-xs font-semibold transition-colors ${
                                            r.adaptiveEnabled
                                                ? 'bg-indigo-600 text-white'
                                                : 'bg-slate-800 text-slate-500'
                                        }`}
                                    >
                                        적응형 {r.adaptiveEnabled ? 'ON' : 'OFF'}
                                    </button>
                                </div>
                                {r.adaptiveEnabled && (
                                    <div className="mt-3 flex items-center gap-2 pl-11">
                                        <span className="text-xs text-slate-500">탐색 윈도우</span>
                                        <select
                                            value={r.windowMinutesBefore}
                                            onChange={(e) => updateRow(r.dayOfWeek, { windowMinutesBefore: Number(e.target.value) })}
                                            className="bg-slate-800 border border-slate-700 text-slate-200 text-xs rounded-lg px-2 py-1 focus:outline-none focus:border-indigo-500"
                                        >
                                            {[15, 30, 45, 60, 90, 120].map((m) => (
                                                <option key={m} value={m}>{m}분 전부터</option>
                                            ))}
                                        </select>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>

                <div className="mt-auto">
                    <Button type="submit">{saving ? '저장 중...' : '알람 저장하기'}</Button>
                </div>
            </form>
        </PageWrapper>
    );
};

export default SetAlarmPage;
