package lizhuoer.agri.agri_system.common.security;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * JWT 令牌工具 — @Component 注入配置，静态方法供全局调用
 */
@Component
public class JwtTokenUtil {
    private static byte[] jwtKey;
    private static long ttlMillis;

    public JwtTokenUtil(@Value("${jwt.secret:AgriSystemKey_123456}") String secret,
                        @Value("${jwt.ttl-hours:24}") int ttlHours) {
        jwtKey = secret.getBytes(StandardCharsets.UTF_8);
        ttlMillis = (long) ttlHours * 3600_000L;
    }

    public static String generateToken(Long userId, String username) {
        long now = System.currentTimeMillis();
        return JWT.create()
                .setPayload("userId", userId)
                .setPayload("username", username)
                .setPayload("iat", now)
                .setPayload("exp", now + ttlMillis)
                .setKey(jwtKey)
                .sign();
    }

    public static TokenPayload parseAndVerify(String token) {
        if (StrUtil.isBlank(token) || !JWTUtil.verify(token, jwtKey)) {
            return null;
        }
        JWT jwt = JWTUtil.parseToken(token);
        // 校验过期时间（无 exp 声明的旧令牌视为已过期）
        Long exp = Convert.toLong(jwt.getPayload("exp"), null);
        if (exp == null || System.currentTimeMillis() > exp) {
            return null;
        }
        Long userId = Convert.toLong(jwt.getPayload("userId"), null);
        if (userId == null) {
            return null;
        }
        String username = Convert.toStr(jwt.getPayload("username"), null);
        return new TokenPayload(userId, username);
    }

    public record TokenPayload(Long userId, String username) {
    }
}
