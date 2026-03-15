package lizhuoer.agri.agri_system.module.iot.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器：维护所有 IoT 数据订阅客户端
 */
@Component
public class SseEmitterManager {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterManager.class);
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5分钟超时

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 创建新的 SSE 连接
     */
    public SseEmitter create(String clientId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            emitters.remove(clientId);
            log.debug("SSE 连接完成: {}", clientId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(clientId);
            log.debug("SSE 连接超时: {}", clientId);
        });
        emitter.onError(e -> {
            emitters.remove(clientId);
            log.debug("SSE 连接异常: {}", clientId);
        });

        emitters.put(clientId, emitter);
        log.info("SSE 新连接: {}, 当前在线: {}", clientId, emitters.size());
        return emitter;
    }

    /**
     * 向所有订阅者广播 IoT 数据事件
     */
    public void broadcast(String eventName, Object data) {
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                emitters.remove(id);
                log.debug("SSE 发送失败，移除: {}", id);
            }
        });
    }

    public int getConnectionCount() {
        return emitters.size();
    }
}
