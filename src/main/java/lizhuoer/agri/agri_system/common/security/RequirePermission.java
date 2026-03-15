package lizhuoer.agri.agri_system.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解 — 标注在 Controller 方法上，要求登录用户拥有指定角色
 * <p>
 * 校验逻辑在 {@link JwtAuthInterceptor#preHandle} 中执行，
 * 满足任一角色即放行 (OR 语义)。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 允许访问的角色 key 列表 (OR 语义)，如 {"ADMIN", "FARM_OWNER"}
     */
    String[] roles();
}
