package lizhuoer.agri.agri_system.module.report;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lizhuoer.agri.agri_system.module.report.service.support.HttpAiModelClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpAiModelClientTest {

    @Test
    void streamsOpenAiCompatibleChunksIntoPlainTextDeltas() throws Exception {
        try (FakeAiServer server = FakeAiServer.start("""
                data: {\"choices\":[{\"delta\":{\"content\":\"[SECTION:conclusion]任务\"}}]}

                data: {\"choices\":[{\"delta\":{\"content\":\"稳定。\"}}]}

                data: [DONE]
                """)) {
            List<String> deltas = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            HttpAiModelClient client = clientPointingTo(server.baseUrl(), true);

            client.stream("prompt", deltas::add, () -> completed.set(true), errorRef::set);

            assertThat(errorRef.get()).isNull();
            assertThat(completed).isTrue();
            assertThat(deltas).containsExactly("[SECTION:conclusion]任务", "稳定。");
            assertThat(server.requestCount()).isEqualTo(1);
        }
    }

    @Test
    void ignoresNonDataLinesBeforeStreamingDeltas() throws Exception {
        try (FakeAiServer server = FakeAiServer.start("""
                : keep-alive
                event: message
                id: 1

                data: {\"choices\":[{\"delta\":{\"content\":\"可忽略元数据后继续。\"}}]}

                data: [DONE]
                """)) {
            List<String> deltas = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            HttpAiModelClient client = clientPointingTo(server.baseUrl(), true);

            client.stream("prompt", deltas::add, () -> completed.set(true), errorRef::set);

            assertThat(errorRef.get()).isNull();
            assertThat(completed).isTrue();
            assertThat(deltas).containsExactly("可忽略元数据后继续。");
        }
    }

    @Test
    void eofBeforeDoneIsReportedAsFailure() throws Exception {
        try (FakeAiServer server = FakeAiServer.start("""
                data: {\"choices\":[{\"delta\":{\"content\":\"未完整结束\"}}]}
                """)) {
            List<String> deltas = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            HttpAiModelClient client = clientPointingTo(server.baseUrl(), true);

            client.stream("prompt", deltas::add, () -> completed.set(true), errorRef::set);

            assertThat(deltas).containsExactly("未完整结束");
            assertThat(completed).isFalse();
            assertThat(errorRef.get())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("before [DONE]");
        }
    }


    @Test
    void interruptedSendRestoresThreadInterruptFlagAndReportsError() {
        InterruptingHttpAiModelClient client = new InterruptingHttpAiModelClient();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        assertThat(Thread.currentThread().isInterrupted()).isFalse();
        try {
            client.stream("prompt", delta -> {}, () -> completed.set(true), errorRef::set);

            assertThat(completed).isFalse();
            assertThat(errorRef.get()).isInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void disabledProviderFailsFastBeforeNetworkCall() throws Exception {
        try (FakeAiServer server = FakeAiServer.start("""
                data: [DONE]
                """)) {
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            HttpAiModelClient client = clientPointingTo(server.baseUrl(), false);

            client.stream("prompt", delta -> {}, () -> completed.set(true), errorRef::set);

            assertThat(completed).isFalse();
            assertThat(errorRef.get())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("provider disabled");
            assertThat(server.requestCount()).isZero();
        }
    }

    @Test
    void enabledProviderRequiresBaseUrlAndModel() {
        assertThatThrownBy(() -> new HttpAiModelClient(true, "", "test-key", "gpt-test", "/v1/chat/completions", 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base-url");

        assertThatThrownBy(() -> new HttpAiModelClient(true, "http://127.0.0.1:8080", "test-key", "", "/v1/chat/completions", 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("model");
    }

    private HttpAiModelClient clientPointingTo(String baseUrl, boolean enabled) {
        return new HttpAiModelClient(enabled, baseUrl, "test-key", "gpt-test", "/v1/chat/completions", 5);
    }


    private static final class InterruptingHttpAiModelClient extends HttpAiModelClient {
        private InterruptingHttpAiModelClient() {
            super(true, "http://127.0.0.1:65535", "test-key", "gpt-test", "/v1/chat/completions", 5);
        }

        @Override
        protected java.net.http.HttpResponse<java.io.InputStream> send(java.net.http.HttpRequest request)
                throws java.io.IOException, InterruptedException {
            throw new InterruptedException("interrupted for test");
        }
    }

    private static final class FakeAiServer implements AutoCloseable {
        private final HttpServer server;
        private final AtomicInteger requestCount = new AtomicInteger();

        private FakeAiServer(HttpServer server) {
            this.server = server;
        }

        static FakeAiServer start(String responseBody) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            FakeAiServer fakeAiServer = new FakeAiServer(server);
            server.createContext("/v1/chat/completions", new StreamingHandler(fakeAiServer.requestCount, responseBody));
            server.start();
            return fakeAiServer;
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        int requestCount() {
            return requestCount.get();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static final class StreamingHandler implements HttpHandler {
        private final AtomicInteger requestCount;
        private final byte[] responseBytes;

        private StreamingHandler(AtomicInteger requestCount, String responseBody) {
            this.requestCount = requestCount;
            this.responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestCount.incrementAndGet();
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBytes);
            }
        }
    }
}
