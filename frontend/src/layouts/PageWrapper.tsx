import React from 'react';
import { ArrowLeft, Activity, CheckCircle2 } from 'lucide-react';
import { PageWrapperProps } from '../types';
import { useNotification } from '../hooks/useNotification';

const PageWrapper: React.FC<PageWrapperProps & { notification?: string | null }> = ({
    children,
    title,
    showBack = false,
    onBack,
    currentPage,
    notification,
}) => (
    <div className="min-h-screen bg-slate-950 text-white flex flex-col p-6 max-w-md mx-auto shadow-2xl relative overflow-hidden">
        {/* 배경 장식 */}
        <div className="absolute -top-20 -right-20 w-64 h-64 bg-indigo-600/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute top-1/2 -left-20 w-48 h-48 bg-purple-600/10 rounded-full blur-3xl pointer-events-none" />

        <header className="flex items-center justify-between mb-8 z-10">
            <div className="flex items-center gap-4">
                {showBack && onBack && (
                    <button onClick={onBack} className="p-2 -ml-2 hover:bg-slate-800 rounded-xl transition-colors">
                        <ArrowLeft size={24} />
                    </button>
                )}
                <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
            </div>
            {currentPage === 'home' && (
                <div className="bg-slate-800 p-2 rounded-xl">
                    <Activity className="text-emerald-400" size={20} />
                </div>
            )}
        </header>

        <main className="flex-1 flex flex-col z-10">{children}</main>

        {notification && (
            <div className="fixed bottom-10 left-1/2 -translate-x-1/2 bg-slate-800 border border-slate-700 px-6 py-3 rounded-2xl shadow-2xl z-50 flex items-center gap-3 animate-bounce">
                <CheckCircle2 className="text-emerald-400" size={20} />
                <span className="text-sm font-medium">{notification}</span>
            </div>
        )}
    </div>
);

export default PageWrapper;
