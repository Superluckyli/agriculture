package lizhuoer.agri.agri_system.module.chat.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lizhuoer.agri.agri_system.module.chat.service.IChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatWebSocketSessionRegistry sessionRegistry;
    private final IChatService chatService;
    private final ChatEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(ChatWebSocketSessionRegistry sessionRegistry,
                                IChatService chatService,
                                ChatEventPublisher eventPublisher,
                                ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.chatService = chatService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.register(currentUserId(session), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = currentUserId(session);
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.path("type").asText();
            JsonNode payload = root.path("payload");
            switch (type) {
                case "chat.send" -> chatService.sendMessage(
                        userId,
                        payload.path("conversationId").asLong(),
                        payload.path("content").asText("")
                );
                case "chat.read" -> chatService.markConversationRead(
                        userId,
                        payload.path("conversationId").asLong()
                );
                default -> eventPublisher.publishError(userId, "不支持的聊天事件类型");
            }
        } catch (Exception e) {
            eventPublisher.publishError(userId, e.getMessage() != null ? e.getMessage() : "聊天事件处理失败");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(currentUserId(session), session);
    }

    private Long currentUserId(WebSocketSession session) {
        Object value = session.getAttributes().get("userId");
        if (value instanceof Long id) {
            return id;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new RuntimeException("聊天连接缺少用户身份");
    }
}
