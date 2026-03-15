package lizhuoer.agri.agri_system.common.config;

import lizhuoer.agri.agri_system.common.security.AuditLogInterceptor;
import lizhuoer.agri.agri_system.common.security.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final AuditLogInterceptor auditLogInterceptor;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor,
                        AuditLogInterceptor auditLogInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.auditLogInterceptor = auditLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/register", "/error",
                        "/actuator/health", "/actuator/info",
                        "/iot/sse/**");

        registry.addInterceptor(auditLogInterceptor)
                .addPathPatterns("/**");
    }
}
