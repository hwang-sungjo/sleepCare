package project.server.ai.chatbot;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.bedrockruntime.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Converse API 멀티턴용 메시지 히스토리 (인메모리).
 */
@Component
public class ChatbotConversationStore {

    private final ConcurrentMap<String, List<Message>> sessions = new ConcurrentHashMap<>();

    public String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId.trim();
    }

    public List<Message> history(String sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    public void reset(String sessionId) {
        sessions.remove(sessionId);
    }
}
