import { useState, useRef } from 'react';
import { sendChatMessage, ChatMessage, CitationItem } from '../api/chat';

export function useChat() {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const sessionIdRef = useRef<string | undefined>(undefined);

    const sendMessage = async (text: string) => {
        const userMsg: ChatMessage = {
            role: 'user',
            text,
            timestamp: new Date(),
        };
        setMessages((prev) => [...prev, userMsg]);
        setIsLoading(true);

        try {
            const res = await sendChatMessage(text, sessionIdRef.current);
            sessionIdRef.current = res.sessionId;

            const assistantMsg: ChatMessage = {
                role: 'assistant',
                text: res.reply,
                citations: (res.citations ?? []).filter(
                    (c): c is CitationItem => c.snippet != null
                ),
                toolCalls: res.toolCalls ?? undefined,
                timestamp: new Date(),
            };
            setMessages((prev) => [...prev, assistantMsg]);
        } catch (err) {
            const errorMsg: ChatMessage = {
                role: 'assistant',
                text: '죄송합니다. 응답을 가져오는 데 실패했습니다. 잠시 후 다시 시도해 주세요.',
                timestamp: new Date(),
            };
            setMessages((prev) => [...prev, errorMsg]);
        } finally {
            setIsLoading(false);
        }
    };

    const clearSession = () => {
        setMessages([]);
        sessionIdRef.current = undefined;
    };

    return { messages, isLoading, sendMessage, clearSession };
}
