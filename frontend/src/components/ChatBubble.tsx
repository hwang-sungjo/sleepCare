import React from 'react';
import { ChatMessage } from '../api/chat';
import { formatToolCallLabel } from '../utils/chatbotSkills';
import CitationList from './CitationList';

interface Props {
    message: ChatMessage;
}

const ChatBubble: React.FC<Props> = ({ message }) => {
    const isUser = message.role === 'user';
    const hasToolCalls = !isUser && message.toolCalls && message.toolCalls.length > 0;
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

                {hasToolCalls && (
                    <div className="mt-1 px-1 space-y-0.5">
                        {message.toolCalls!.map((tc, idx) => (
                            <p key={idx} className="text-xs text-slate-600">
                                {formatToolCallLabel(tc.skillId, tc.status)}
                            </p>
                        ))}
                    </div>
                )}

                {hasCitations && <CitationList citations={message.citations!} />}

                <p className="text-xs text-slate-600 mt-1 px-1">
                    {message.timestamp.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                </p>
            </div>
        </div>
    );
};

export default ChatBubble;
