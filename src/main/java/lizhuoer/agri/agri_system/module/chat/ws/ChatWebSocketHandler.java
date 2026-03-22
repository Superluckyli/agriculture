package lizhuoer.agri.agri_system.module.chat.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lizhuoer.agri.agri_system.module.chat.service.IChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

// 聊天 WebSocket 消息处理器，继承自文本消息处理器（TextWebSocketHandler）
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

    // 1. 建立长连接建立后的钩子：将当前会话 Session 注册到注册表中，方便全双工通信时查找到特定用户
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.register(currentUserId(session), session);
    }

    // 2. 核心：处理客户端发送过来的文本消息
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = currentUserId(session);
        try {
            // 解析 JSON 信封，包含 type（消息类型）和 payload（具体数据）
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.path("type").asText();
            JsonNode payload = root.path("payload");
            
            // 根据不同的信封类型（Action）派发到 Service 处理层
            switch (type) {
                case "chat.send" -> chatService.sendMessage(
                        userId,
                        payload.path("conversationId").asLong(),
                        payload.path("content").asText(""));
                case "chat.read" -> chatService.markConversationRead(
                        userId,
                        payload.path("conversationId").asLong());
                default -> eventPublisher.publishError(userId, "不支持的聊天事件类型");
            }
        } catch (Exception e) {
            // 异常时推送错误通知给客户端
            eventPublisher.publishError(userId, e.getMessage() != null ? e.getMessage() : "聊天事件处理失败");
        }
    }

    // 连接关闭后注销会话
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(currentUserId(session), session);
    }

    // 获取当前用户ID
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
