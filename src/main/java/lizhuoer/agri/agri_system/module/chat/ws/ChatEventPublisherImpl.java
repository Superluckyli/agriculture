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

    // 发送消息
    @Override
    public void publishMessage(Long senderId, Long receiverId, ChatMessageVO message) {
        String payload = toEnvelope("chat.message", message);
        sessionRegistry.sendToUser(senderId, payload);
        if (!senderId.equals(receiverId)) {
            sessionRegistry.sendToUser(receiverId, payload);
        }
    }

    // 会话更新通知：当会话列表有变动时（如收到新消息），实时通知前端更新列表
    @Override
    public void publishConversationUpdate(Long userId, ChatConversationSummaryVO summary) {
        sessionRegistry.sendToUser(userId, toEnvelope("chat.conversation.update", summary));
    }

    // 已读同步通知：当对方已读消息时，实时通知前端更新已读状态
    @Override
    public void publishReadSync(Long userId, Long conversationId) {
        sessionRegistry.sendToUser(userId, toEnvelope("chat.read.sync", Map.of("conversationId", conversationId)));
    }

    // 错误通知：当发生错误时，实时通知前端
    @Override
    public void publishError(Long userId, String message) {
        sessionRegistry.sendToUser(userId, toEnvelope("chat.error", Map.of("message", message)));
    }

    // 转换为信封格式
    private String toEnvelope(String type, Object payload) {
        try {
            return objectMapper.writeValueAsString(Map.of("type", type, "payload", payload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("聊天事件序列化失败", e);
        }
    }
}
