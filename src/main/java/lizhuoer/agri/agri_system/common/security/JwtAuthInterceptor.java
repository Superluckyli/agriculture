package lizhuoer.agri.agri_system.common.security;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {
    private static final Set<String> LOGIN_REQUIRED_PATHS = Set.of(
            "/task/assign",
            "/task/accept",
            "/task/reject");

    private final ISysUserService userService;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(ISysUserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String token = extractToken(request.getHeader("Authorization"));
        boolean loginRequired = LOGIN_REQUIRED_PATHS.contains(uri);

        if (StrUtil.isBlank(token)) {
            if (loginRequired) {
                writeUnauthorized(response, "请先登录");
                return false;
            }
            return true;
        }

        JwtTokenUtil.TokenPayload payload = JwtTokenUtil.parseAndVerify(token);
        if (payload == null) {
            if (loginRequired) {
                writeUnauthorized(response, "登录已过期，请重新登录");
                return false;
            }
            return true;
        }

        SysUser user = userService.getById(payload.userId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            if (loginRequired) {
                writeUnauthorized(response, "用户不存在或已禁用");
                return false;
            }
            return true;
        }

        LoginUserContext.set(new LoginUser(user.getUserId(), user.getUsername(), userService.getRoleKeys(user.getUserId())));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
    }

    private String extractToken(String authHeader) {
        if (StrUtil.isBlank(authHeader)) {
            return null;
        }
        String prefix = "Bearer ";
        if (authHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return authHeader.substring(prefix.length()).trim();
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(HttpStatus.UNAUTHORIZED.value(), msg)));
    }
}
