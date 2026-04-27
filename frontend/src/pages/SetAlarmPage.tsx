import React from 'react';
import { Clock } from 'lucide-react';
import PageWrapper from '../layouts/PageWrapper';
import Button from '../components/Button';

interface SetAlarmPageProps {
    alarmTime: string;
    notification: string | null;
    onAlarmTimeChange: (value: string) => void;
    onSave: (e: React.FormEvent) => void;
    onBack: () => void;
}

const SetAlarmPage: React.FC<SetAlarmPageProps> = ({
    alarmTime,
    notification,
    onAlarmTimeChange,
    onSave,
    onBack,
}) => (
    <PageWrapper title="알람 설정" showBack onBack={onBack} currentPage="setAlarm" notification={notification}>
        <p className="text-slate-400 mb-12">
            목표하는 기상 시간을 설정하세요.
            <br />
            적응형 엔진이 30분 전후로 최적의 타이밍을 결정합니다.
        </p>

        <div className="flex-1">
            <div className="bg-slate-900 rounded-3xl p-8 flex flex-col items-center justify-center mb-10 border border-white/5">
                <Clock className="text-indigo-500 mb-6" size={48} />
                <input
                    type="time"
                    value={alarmTime}
                    onChange={(e) => onAlarmTimeChange(e.target.value)}
                    className="bg-transparent text-6xl font-light text-white focus:outline-none focus:text-indigo-400 transition-colors cursor-pointer"
                />
                <div className="mt-6 flex gap-2">
                    {['월', '화', '수', '목', '금'].map((day) => (
                        <span key={day} className="w-8 h-8 rounded-full bg-indigo-600/50 flex items-center justify-center text-xs font-bold text-indigo-200">
                            {day}
                        </span>
                    ))}
                    {['토', '일'].map((day) => (
                        <span key={day} className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center text-xs font-bold text-slate-500">
                            {day}
                        </span>
                    ))}
                </div>
                <p className="mt-2 text-xs text-slate-600">요일별 반복 설정은 추후 업데이트 예정입니다</p>
            </div>

            <div className="bg-slate-900/50 p-6 rounded-2xl border border-white/5">
                <div className="flex items-center justify-between mb-4">
                    <span className="font-medium">적응형 스마트 기상</span>
                    <div className="w-12 h-6 bg-indigo-600 rounded-full relative">
                        <div className="absolute right-1 top-1 w-4 h-4 bg-white rounded-full" />
                    </div>
                </div>
                <p className="text-xs text-slate-500">
                    렘(REM) 수면 단계 분석을 통해 몸이 가장 가벼운 순간에 알람을 울려줍니다. (범위: 설정 시간 30분 전부터)
                </p>
            </div>
        </div>

        <div className="mt-auto">
            <form onSubmit={onSave}>
                <Button type="submit">알람 저장하기</Button>
            </form>
        </div>
    </PageWrapper>
);

export default SetAlarmPage;
