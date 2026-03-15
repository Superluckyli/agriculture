package lizhuoer.agri.agri_system.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解 — 标注在 Controller 方法上，自动记录操作日志
 * <p>
 * 由 {@link AuditLogInterceptor} 在请求完成后异步写入 sys_audit_log 表。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 模块名称，如 "系统管理"、"任务管理" */
    String module();

    /** 操作类型，如 "LOGIN"、"CREATE"、"DELETE"、"ASSIGN" */
    String action();

    /** 操作对象描述，如 "用户"、"角色" */
    String target() default "";
}
