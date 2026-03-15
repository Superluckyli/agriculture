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
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证 + 权限拦截器
 * <p>
 * 1. deny-by-default: 所有路径强制 JWT 鉴权（公开路径在 WebMvcConfig 中排除）
 * 2. @RequirePermission: 方法级角色校验，满足任一角色即放行
 * 3. Actuator 保护: /actuator/** (非 health/info) 仅 ADMIN 可访问
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {
    private final ISysUserService userService;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(ISysUserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = extractToken(request.getHeader("Authorization"));

        if (StrUtil.isBlank(token)) {
            writeUnauthorized(response, "请先登录");
            return false;
        }

        JwtTokenUtil.TokenPayload payload = JwtTokenUtil.parseAndVerify(token);
        if (payload == null) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        SysUser user = userService.getById(payload.userId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            writeUnauthorized(response, "用户不存在或已禁用");
            return false;
        }

        LoginUser loginUser = new LoginUser(user.getUserId(), user.getUsername(),
                userService.getRoleKeys(user.getUserId()));
        LoginUserContext.set(loginUser);

        // Actuator 端点保护: 非 health/info 需要 ADMIN 角色
        String uri = request.getRequestURI();
        if (uri.startsWith("/actuator/") && !uri.equals("/actuator/health") && !uri.equals("/actuator/info")) {
            if (!loginUser.hasRole("ADMIN")) {
                writeForbidden(response, "无权访问管理端点");
                return false;
            }
        }

        // @RequirePermission 方法级角色校验
        if (handler instanceof HandlerMethod handlerMethod) {
            RequirePermission rp = handlerMethod.getMethodAnnotation(RequirePermission.class);
            if (rp != null && rp.roles().length > 0) {
                if (!loginUser.hasAnyRole(rp.roles())) {
                    writeForbidden(response, "无权限操作");
                    return false;
                }
            }
        }

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

    private void writeForbidden(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(HttpStatus.FORBIDDEN.value(), msg)));
    }
}
