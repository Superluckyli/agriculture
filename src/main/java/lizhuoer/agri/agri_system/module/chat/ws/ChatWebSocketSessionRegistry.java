package lizhuoer.agri.agri_system.module.chat.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 核心容器：维护 UserId -> 多端 Session 的映射关系
// 使用 ConcurrentHashMap 保证多线程并发下的安全性（例如 A 同时在手机、电脑端登录聊天）
@Component
public class ChatWebSocketSessionRegistry {
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    // 注册会话：用户连接成功后调用
    public void register(Long userId, WebSocketSession session) {
        sessionsByUser.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    // 注销会话：用户断开连接后调用
    public void unregister(Long userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    // 定向发送：把服务器产生的消息、通知，通过 WebSocket 通道推送给某个用户
    public void sendToUser(Long userId, String payload) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return; // 用户离线时，这里什么都不做（持久化由 Service 层处理）
        }
        TextMessage message = new TextMessage(payload);
        // 如果用户在多个Session登录，需要给所有的 Session 都推送一份
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException ignored) {
            }
        }
    }
}
