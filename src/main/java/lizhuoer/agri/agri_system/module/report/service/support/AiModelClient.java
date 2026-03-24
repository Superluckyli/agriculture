package lizhuoer.agri.agri_system.module.report.service.support;

import java.util.function.Consumer;

@FunctionalInterface
public interface AiModelClient {
    void stream(String prompt,
                Consumer<String> deltaConsumer,
                Runnable completionCallback,
                Consumer<Throwable> errorCallback);
}
