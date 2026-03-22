package lizhuoer.agri.agri_system.module.chat.ws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

// WebSocket 全局配置类，负责启用 WebSocket 并绑定具体的处理器（Handler）
@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatWebSocketHandshakeInterceptor handshakeInterceptor;
    private final String[] allowedOrigins;

    public ChatWebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                               ChatWebSocketHandshakeInterceptor handshakeInterceptor,
                               @Value("${chat.websocket.allowed-origins:http://127.0.0.1:5173,http://localhost:5173}") String allowedOrigins) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册聊天 WebSocket 服务器端点，路径为 /ws/chat
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                // 添加握手拦截器，用于在正式连接前的鉴权（校验 Token）
                .addInterceptors(handshakeInterceptor)
                // 设置允许跨站访问的源
                .setAllowedOrigins(allowedOrigins);
    }
}
