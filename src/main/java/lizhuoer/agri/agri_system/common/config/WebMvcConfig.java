package lizhuoer.agri.agri_system.common.config;

import lizhuoer.agri.agri_system.common.security.AuditLogInterceptor;
import lizhuoer.agri.agri_system.common.security.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final AuditLogInterceptor auditLogInterceptor;
    private final String taskLogImageStorageRoot;
    private final String taskLogImageUrlPrefix;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor,
                        AuditLogInterceptor auditLogInterceptor,
                        @Value("${task.log.image.storage-root:uploads}") String taskLogImageStorageRoot,
                        @Value("${task.log.image.url-prefix:/uploads}") String taskLogImageUrlPrefix) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.auditLogInterceptor = auditLogInterceptor;
        this.taskLogImageStorageRoot = taskLogImageStorageRoot;
        this.taskLogImageUrlPrefix = normalizeUrlPrefix(taskLogImageUrlPrefix);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/register", "/error",
                        "/actuator/health", "/actuator/info",
                        "/iot/sse/**", taskLogImageUrlPrefix + "/task-log/**");

        registry.addInterceptor(auditLogInterceptor)
                .addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String taskLogLocation = Path.of(taskLogImageStorageRoot, "task-log")
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        registry.addResourceHandler(taskLogImageUrlPrefix + "/task-log/**")
                .addResourceLocations(taskLogLocation.endsWith("/") ? taskLogLocation : taskLogLocation + "/");
    }

    private String normalizeUrlPrefix(String urlPrefix) {
        if (!StringUtils.hasText(urlPrefix)) {
            return "/uploads";
        }
        String normalized = urlPrefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
