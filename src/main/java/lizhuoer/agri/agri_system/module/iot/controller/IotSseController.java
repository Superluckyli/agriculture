package lizhuoer.agri.agri_system.module.iot.controller;

import lizhuoer.agri.agri_system.module.iot.sse.SseEmitterManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * IoT SSE 实时推送端点
 */
@RestController
@RequestMapping("/iot/sse")
public class IotSseController {

    private final SseEmitterManager sseManager;

    public IotSseController(SseEmitterManager sseManager) {
        this.sseManager = sseManager;
    }

    /**
     * 客户端订阅 IoT 实时数据流
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam(required = false) String clientId) {
        if (clientId == null || clientId.isBlank()) {
            clientId = UUID.randomUUID().toString().substring(0, 8);
        }
        return sseManager.create(clientId);
    }
}
