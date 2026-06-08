import { request } from './client';

export interface ChatMessage {
    role: 'user' | 'assistant';
    text: string;
    citations?: CitationItem[];
    timestamp: Date;
}

export interface CitationItem {
    location: string | null;
    snippet: string | null;
}

interface ChatApiResponse {
    reply: string;
    sessionId: string;
    citations?: CitationItem[] | null;
    toolCalls?: { skillId: string; status: string }[] | null;
}

export function sendChatMessage(
    message: string,
    sessionId?: string
): Promise<ChatApiResponse> {
    return request<ChatApiResponse>('/chat/messages', {
        method: 'POST',
        body: { message, sessionId: sessionId ?? null },
        auth: true,
    });
}
