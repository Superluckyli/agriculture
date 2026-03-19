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

@Component
public class ChatWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final ISysUserService userService;

    public ChatWebSocketHandshakeInterceptor(ISysUserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter("token");
        JwtTokenUtil.TokenPayload payload = JwtTokenUtil.parseAndVerify(token);
        if (payload == null || payload.userId() == null) {
            writeUnauthorized(response, "聊天连接未授权");
            return false;
        }
        SysUser user = userService.getById(payload.userId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            writeUnauthorized(response, "聊天用户不存在或已禁用");
            return false;
        }
        attributes.put("userId", user.getUserId());
        attributes.put("username", user.getUsername());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
    }

    private void writeUnauthorized(ServerHttpResponse response, String message) {
        if (response instanceof ServletServerHttpResponse servletResponse) {
            HttpServletResponse raw = servletResponse.getServletResponse();
            raw.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            if (StrUtil.isNotBlank(message)) {
                raw.setHeader("X-Chat-Error", message);
            }
        }
    }
}
