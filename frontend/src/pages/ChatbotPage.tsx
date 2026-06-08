import React, { useRef, useEffect, useState } from 'react';
import { Send, RefreshCw } from 'lucide-react';
import { PageName } from '../types';
import { useChat } from '../hooks/useChat';
import ChatBubble from '../components/ChatBubble';
import PageWrapper from '../layouts/PageWrapper';

interface Props {
    onBack: () => void;
    onNavigate: (page: PageName) => void;
}

const QUICK_QUESTIONS = [
    '어젯밤 수면 분석해줘',
    '수면 효율 올리는 방법은?',
    '깊은 수면을 늘리려면?',
    '취침 전 루틴 추천해줘',
];

const TypingIndicator: React.FC = () => (
    <div className="flex justify-start mb-3">
        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shrink-0 mr-2 mt-1">
            <span className="text-sm">🤖</span>
        </div>
        <div className="bg-slate-800 border border-slate-700/50 rounded-2xl rounded-tl-sm px-4 py-3">
            <div className="flex gap-1.5 items-center h-4">
                <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
            </div>
        </div>
    </div>
);

const ChatbotPage: React.FC<Props> = ({ onBack }) => {
    const { messages, isLoading, sendMessage, clearSession } = useChat();
    const [input, setInput] = useState('');
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLTextAreaElement>(null);
    const isFirstRender = messages.length === 0;

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, isLoading]);

    const handleSend = () => {
        const text = input.trim();
        if (!text || isLoading) return;
        setInput('');
        sendMessage(text);
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    const handleQuickQuestion = (q: string) => {
        if (isLoading) return;
        sendMessage(q);
    };

    return (
        <PageWrapper title="AI 수면 챗봇" currentPage="chatbot" showBack onBack={onBack}>
            {/* 새 대화 버튼 */}
            {messages.length > 0 && (
                <div className="flex justify-end mb-2 -mt-4">
                    <button
                        onClick={clearSession}
                        className="flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-300 transition-colors"
                    >
                        <RefreshCw size={12} />
                        새 대화
                    </button>
                </div>
            )}

            {/* 메시지 영역 */}
            <div className="flex-1 overflow-y-auto pb-4 min-h-0" style={{ maxHeight: 'calc(100vh - 280px)' }}>
                {isFirstRender && (
                    <div className="flex flex-col items-center text-center pt-6 pb-4">
                        <div className="w-16 h-16 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center mb-4 shadow-lg shadow-indigo-500/30">
                            <span className="text-3xl">🌙</span>
                        </div>
                        <h2 className="text-lg font-bold mb-1">SleepCare AI</h2>
                        <p className="text-slate-400 text-sm leading-relaxed max-w-[260px]">
                            수면 데이터를 분석하고 개인 맞춤 수면 조언을 드립니다.
                            <br />무엇이 궁금하신가요?
                        </p>
                    </div>
                )}

                {messages.map((msg, idx) => (
                    <ChatBubble key={idx} message={msg} />
                ))}

                {isLoading && <TypingIndicator />}
                <div ref={messagesEndRef} />
            </div>

            {/* 빠른 질문 버튼 (첫 화면에만) */}
            {isFirstRender && !isLoading && (
                <div className="flex flex-wrap gap-2 mb-4">
                    {QUICK_QUESTIONS.map((q) => (
                        <button
                            key={q}
                            onClick={() => handleQuickQuestion(q)}
                            className="text-xs bg-slate-800 hover:bg-slate-700 border border-slate-700 hover:border-indigo-500/50 transition-all px-3 py-2 rounded-xl text-slate-300"
                        >
                            {q}
                        </button>
                    ))}
                </div>
            )}

            {/* 입력창 */}
            <div className="flex gap-2 items-end pt-2 border-t border-slate-800">
                <textarea
                    ref={inputRef}
                    id="chat-input"
                    rows={1}
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="수면에 대해 무엇이든 물어보세요..."
                    disabled={isLoading}
                    className="flex-1 bg-slate-800 border border-slate-700 focus:border-indigo-500 outline-none rounded-2xl px-4 py-3 text-sm text-white placeholder-slate-500 resize-none transition-colors disabled:opacity-50"
                    style={{ minHeight: '48px', maxHeight: '120px' }}
                    onInput={(e) => {
                        const el = e.currentTarget;
                        el.style.height = 'auto';
                        el.style.height = `${el.scrollHeight}px`;
                    }}
                />
                <button
                    id="chat-send-button"
                    onClick={handleSend}
                    disabled={!input.trim() || isLoading}
                    className="w-12 h-12 rounded-2xl bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-700 disabled:opacity-50 flex items-center justify-center transition-all shrink-0"
                >
                    <Send size={18} className="text-white" />
                </button>
            </div>
        </PageWrapper>
    );
};

export default ChatbotPage;
