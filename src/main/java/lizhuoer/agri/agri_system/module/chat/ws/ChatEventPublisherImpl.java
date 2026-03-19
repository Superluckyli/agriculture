package lizhuoer.agri.agri_system.module.chat.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatConversationSummaryVO;
import lizhuoer.agri.agri_system.module.chat.domain.vo.ChatMessageVO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ChatEventPublisherImpl implements ChatEventPublisher {

    private final ChatWebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public ChatEventPublisherImpl(ChatWebSocketSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishMessage(Long senderId, Long receiverId, ChatMessageVO message) {
        String payload = toEnvelope("chat.message", message);
        sessionRegistry.sendToUser(senderId, payload);
        if (!senderId.equals(receiverId)) {
            sessionRegistry.sendToUser(receiverId, payload);
        }
    }

    @Override
    public void publishConversationUpdate(Long userId, ChatConversationSummaryVO summary) {
        sessionRegistry.sendToUser(userId, toEnvelope("chat.conversation.update", summary));
    }

    @Override
    public void publishReadSync(Long userId, Long conversationId) {
        sessionRegistry.sendToUser(userId, toEnvelope("chat.read.sync", Map.of("conversationId", conversationId)));
    }

    @Override
    public void publishError(Long userId, String message) {
        sessionRegistry.sendToUser(userId, toEnvelope("chat.error", Map.of("message", message)));
    }

    private String toEnvelope(String type, Object payload) {
        try {
            return objectMapper.writeValueAsString(Map.of("type", type, "payload", payload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("聊天事件序列化失败", e);
        }
    }
}
