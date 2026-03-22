package lizhuoer.agri.agri_system.module.chat.ws;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletResponse;
import lizhuoer.agri.agri_system.common.security.JwtTokenUtil;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

// WebSocket 握手拦截器，在 TCP 连接升级为 WebSocket 协议前
@Component
public class ChatWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final ISysUserService userService;

    public ChatWebSocketHandshakeInterceptor(ISysUserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        // 第一步：确保是在 Servlet 环境下
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        // 第二步：从 URL 参数中提取 token（由于 WS 握手不支持直接在 Header 带数据，通常放在参数里）
        String token = servletRequest.getServletRequest().getParameter("token");
        // 第三步：解析并校验 JWT Token，获取用户身份
        JwtTokenUtil.TokenPayload payload = JwtTokenUtil.parseAndVerify(token);
        if (payload == null || payload.userId() == null) {
            writeUnauthorized(response, "聊天连接未授权");
            return false;
        }
        // 第四步：检查用户数据库状态（防止离职或被禁用用户重连）
        SysUser user = userService.getById(payload.userId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            writeUnauthorized(response, "聊天用户不存在或已禁用");
            return false;
        }
        // 第五步：将 userId 和 username 存入 attributes。这些属性会被“复制”到后续的 WebSocketSession 中
        attributes.put("userId", user.getUserId());
        attributes.put("username", user.getUsername());
        return true;
    }

    // 握手后置处理
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Exception exception) {
    }

    // 写入未授权响应
    private void writeUnauthorized(ServerHttpResponse response, String message) {
        if (response instanceof ServletServerHttpResponse servletResponse) {
            // 获取原始响应
            HttpServletResponse raw = servletResponse.getServletResponse();
            // 设置状态码
            raw.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            if (StrUtil.isNotBlank(message)) {
                raw.setHeader("X-Chat-Error", message);
            }
        }
    }
}
