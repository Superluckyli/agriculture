package lizhuoer.agri.agri_system.common.security;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;

import java.nio.charset.StandardCharsets;

public final class JwtTokenUtil {
    public static final byte[] JWT_KEY = "AgriSystemKey_123456".getBytes(StandardCharsets.UTF_8);

    private JwtTokenUtil() {
    }

    public static String generateToken(Long userId, String username) {
        return JWT.create()
                .setPayload("userId", userId)
                .setPayload("username", username)
                .setKey(JWT_KEY)
                .sign();
    }

    public static TokenPayload parseAndVerify(String token) {
        if (StrUtil.isBlank(token) || !JWTUtil.verify(token, JWT_KEY)) {
            return null;
        }
        JWT jwt = JWTUtil.parseToken(token);
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
