import React, { useState } from 'react';
import { ChevronDown, ChevronUp, BookOpen } from 'lucide-react';
import { ChatMessage } from '../api/chat';

interface Props {
    message: ChatMessage;
}

const ChatBubble: React.FC<Props> = ({ message }) => {
    const [citationsOpen, setCitationsOpen] = useState(false);
    const isUser = message.role === 'user';
    const hasCitations = !isUser && message.citations && message.citations.length > 0;

    return (
        <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-3`}>
            {!isUser && (
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shrink-0 mr-2 mt-1">
                    <span className="text-sm">🤖</span>
                </div>
            )}
            <div className={`max-w-[78%] ${isUser ? 'items-end' : 'items-start'} flex flex-col`}>
                <div
                    className={`px-4 py-3 rounded-2xl text-sm leading-relaxed ${
                        isUser
                            ? 'bg-indigo-600 text-white rounded-tr-sm'
                            : 'bg-slate-800 text-slate-100 rounded-tl-sm border border-slate-700/50'
                    }`}
                >
                    {message.text}
                </div>

                {hasCitations && (
                    <div className="mt-1.5 w-full">
                        <button
                            onClick={() => setCitationsOpen((o) => !o)}
                            className="flex items-center gap-1 text-xs text-slate-500 hover:text-slate-300 transition-colors px-1"
                        >
                            <BookOpen size={11} />
                            참고 문헌 {message.citations!.length}건
                            {citationsOpen ? <ChevronUp size={11} /> : <ChevronDown size={11} />}
                        </button>
                        {citationsOpen && (
                            <div className="mt-1.5 space-y-1.5">
                                {message.citations!.map((c, idx) => (
                                    <div
                                        key={idx}
                                        className="bg-slate-900 border border-slate-700/50 rounded-xl px-3 py-2"
                                    >
                                        {c.location && (
                                            <p className="text-indigo-400 text-xs font-medium mb-1 truncate">
                                                📄 {c.location}
                                            </p>
                                        )}
                                        <p className="text-slate-400 text-xs leading-relaxed line-clamp-3">
                                            {c.snippet}
                                        </p>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                <p className="text-xs text-slate-600 mt-1 px-1">
                    {message.timestamp.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                </p>
            </div>
        </div>
    );
};

export default ChatBubble;
