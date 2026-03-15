package lizhuoer.agri.agri_system.common.security;

import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录限流服务 — IP + 用户名维度，5 次失败锁定 15 分钟
 * <p>
 * 基于 ConcurrentHashMap 实现，适用于单实例部署。
 * 定期清理过期记录避免内存泄漏。
 */
@Service
public class LoginThrottleService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000L; // 15 分钟
    private static final long CLEANUP_THRESHOLD = 1000;

    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    /**
     * 检查是否被锁定
     *
     * @return 剩余锁定秒数，0 表示未锁定
     */
    public long checkLocked(String ip, String username) {
        String key = buildKey(ip, username);
        AttemptRecord record = attempts.get(key);
        if (record == null) {
            return 0;
        }

        if (record.isExpired()) {
            attempts.remove(key);
            return 0;
        }

        if (record.count >= MAX_ATTEMPTS) {
            long remaining = (record.lockUntil - System.currentTimeMillis()) / 1000;
            return Math.max(remaining, 1);
        }

        return 0;
    }

    /**
     * 记录一次失败尝试
     */
    public void recordFailure(String ip, String username) {
        String key = buildKey(ip, username);
        AttemptRecord record = attempts.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new AttemptRecord(1, 0);
            }
            existing.count++;
            if (existing.count >= MAX_ATTEMPTS) {
                existing.lockUntil = System.currentTimeMillis() + LOCK_DURATION_MS;
            }
            return existing;
        });

        // 周期性清理过期记录
        if (attempts.size() > CLEANUP_THRESHOLD) {
            cleanupExpired();
        }
    }

    /**
     * 登录成功后清除记录
     */
    public void clearRecord(String ip, String username) {
        attempts.remove(buildKey(ip, username));
    }

    private String buildKey(String ip, String username) {
        return (ip != null ? ip : "unknown") + ":" + (username != null ? username : "unknown");
    }

    private void cleanupExpired() {
        Iterator<Map.Entry<String, AttemptRecord>> it = attempts.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) {
                it.remove();
            }
        }
    }

    private static class AttemptRecord {
        int count;
        long lockUntil;
        final long createdAt;

        AttemptRecord(int count, long lockUntil) {
            this.count = count;
            this.lockUntil = lockUntil;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            if (lockUntil > 0) {
                return System.currentTimeMillis() > lockUntil;
            }
            // 未锁定的记录 30 分钟后自动过期
            return System.currentTimeMillis() - createdAt > 30 * 60 * 1000L;
        }
    }
}
