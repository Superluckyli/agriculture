package lizhuoer.agri.agri_system.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lizhuoer.agri.agri_system.module.system.domain.SysAuditLog;
import lizhuoer.agri.agri_system.module.system.service.AuditLogService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 审计日志拦截器 — 配合 @AuditLog 注解记录关键操作
 * <p>
 * 在 afterCompletion 阶段异步写入审计日志，不影响请求响应时间。
 */
@Component
public class AuditLogInterceptor implements HandlerInterceptor {

    private static final String ATTR_START_TIME = "audit_start_time";

    private final AuditLogService auditLogService;

    public AuditLogInterceptor(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }

        AuditLog annotation = handlerMethod.getMethodAnnotation(AuditLog.class);
        if (annotation == null) {
            return;
        }

        LoginUser loginUser = LoginUserContext.get();
        long startTime = (long) request.getAttribute(ATTR_START_TIME);

        SysAuditLog log = new SysAuditLog();
        log.setModule(annotation.module());
        log.setAction(annotation.action());
        log.setTarget(annotation.target());
        log.setMethod(request.getMethod());
        log.setUri(request.getRequestURI());
        log.setIp(resolveClientIp(request));
        log.setStatus(response.getStatus());
        log.setDuration(System.currentTimeMillis() - startTime);

        if (loginUser != null) {
            log.setUserId(loginUser.getUserId());
            log.setUsername(loginUser.getUsername());
        }

        auditLogService.record(log);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }
        return ip;
    }
}
