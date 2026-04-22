import React from 'react';
import { Bell, ChevronRight } from 'lucide-react';
import { AlarmCardProps } from '../types';

const AlarmCard: React.FC<AlarmCardProps> = ({ time, onClick }) => (
    <div
        onClick={onClick}
        className="bg-slate-900/50 backdrop-blur-md border border-white/5 rounded-3xl p-6 mb-6 cursor-pointer hover:border-indigo-500/30 transition-all group"
    >
        <div className="flex justify-between items-center mb-4">
            <div className="bg-indigo-500/20 p-3 rounded-2xl">
                <Bell className="text-indigo-400" size={24} />
            </div>
            <div className="text-xs text-slate-500 font-medium bg-slate-800 px-3 py-1 rounded-full">
                스마트 적응형 모드 켜짐
            </div>
        </div>
        <div className="flex items-baseline gap-2">
            <span className="text-5xl font-light text-white group-hover:text-indigo-400 transition-colors">
                {time || '시간 미설정'}
            </span>
            <span className="text-xl text-slate-500">AM</span>
        </div>
        <div className="mt-4 flex items-center justify-between text-sm">
            <span className="text-slate-400 italic">"최적의 기상 타이밍을 찾는 중..."</span>
            <ChevronRight className="text-slate-600 group-hover:text-indigo-400" size={20} />
        </div>
    </div>
);

export default AlarmCard;
