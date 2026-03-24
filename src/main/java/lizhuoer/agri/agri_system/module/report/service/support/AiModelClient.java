package lizhuoer.agri.agri_system.module.report.service.support;

import java.util.function.Consumer;

@FunctionalInterface
public interface AiModelClient {
    /**
     * 按 prompt 流式输出 provider 文本增量。
     * <p>
     * 实现类必须把 {@code completionCallback} 和 {@code errorCallback}
     * 当作互斥的终止回调；任一终止回调触发后，都不应继续派发新的 delta。
     */
    void stream(String prompt,
                Consumer<String> deltaConsumer,
                Runnable completionCallback,
                Consumer<Throwable> errorCallback);
}
