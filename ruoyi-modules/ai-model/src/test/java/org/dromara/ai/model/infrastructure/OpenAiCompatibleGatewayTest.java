package org.dromara.ai.model.infrastructure;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.dromara.ai.model.application.ChatMessage;
import org.dromara.ai.model.application.ChatModelRequest;
import org.dromara.ai.model.application.ChatModelResponse;
import org.dromara.ai.model.application.ChatStreamEvent;
import org.dromara.ai.model.application.ModelGatewayError;
import org.dromara.ai.model.application.ModelGatewayException;
import org.dromara.ai.model.application.ModelRuntimeConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class OpenAiCompatibleGatewayTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void mapsChatCompletionAndUsage() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        startServer(exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "application/json", """
                {"id":"req-1","choices":[{"message":{"content":"你好"},"finish_reason":"stop"}],
                 "usage":{"prompt_tokens":12,"completion_tokens":3}}
                """);
        });

        ChatModelResponse response = gateway().chat(request(Duration.ofSeconds(2)));

        assertEquals("你好", response.content());
        assertEquals(12, response.usage().inputTokens());
        assertEquals(3, response.usage().outputTokens());
        assertEquals("req-1", response.providerRequestId());
        assertEquals("stop", response.finishReason());
        assertEquals("Bearer sk-test", authorization.get());
    }

    @Test
    void publishesSseContentAndCompletionUsage() throws Exception {
        startServer(exchange -> respond(exchange, 200, "text/event-stream", """
            data: {"choices":[{"delta":{"content":"你"},"finish_reason":null}]}

            data: {"choices":[{"delta":{"content":"好"},"finish_reason":"stop"}]}

            data: {"choices":[],"usage":{"prompt_tokens":4,"completion_tokens":2}}

            data: [DONE]

            """));
        List<ChatStreamEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);

        gateway().stream(request(Duration.ofSeconds(2))).subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ChatStreamEvent item) {
                events.add(item);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                completed.countDown();
            }

            @Override
            public void onComplete() {
                completed.countDown();
            }
        });

        assertTrue(completed.await(3, TimeUnit.SECONDS));
        assertNull(error.get());
        assertEquals(List.of("你", "好"), events.stream()
            .filter(event -> event.type() == ChatStreamEvent.Type.CONTENT)
            .map(ChatStreamEvent::content).toList());
        ChatStreamEvent finalEvent = events.get(events.size() - 1);
        assertEquals(ChatStreamEvent.Type.COMPLETED, finalEvent.type());
        assertEquals(4, finalEvent.usage().inputTokens());
        assertEquals(2, finalEvent.usage().outputTokens());
        assertEquals("stop", finalEvent.finishReason());
    }

    @Test
    void normalizesProviderAuthenticationFailure() throws Exception {
        startServer(exchange -> respond(exchange, 401, "application/json", "{\"error\":{\"message\":\"secret\"}}"));

        ModelGatewayException exception = assertThrows(ModelGatewayException.class,
            () -> gateway().chat(request(Duration.ofSeconds(2))));

        assertEquals(ModelGatewayError.AUTHENTICATION_FAILED, exception.error());
        assertEquals(401, exception.providerStatus());
        assertTrue(!exception.getMessage().contains("secret"));
    }

    @Test
    void normalizesRequestTimeout() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(300);
                respond(exchange, 200, "application/json", "{}");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });

        ModelGatewayException exception = assertThrows(ModelGatewayException.class,
            () -> gateway().chat(request(Duration.ofMillis(50))));

        assertEquals(ModelGatewayError.TIMEOUT, exception.error());
        assertTrue(exception.retryable());
    }

    private OpenAiCompatibleGateway gateway() {
        return new OpenAiCompatibleGateway(JsonMapper.builder().build());
    }

    private ChatModelRequest request(Duration timeout) {
        return new ChatModelRequest(
            new ModelRuntimeConfig(1L, 2L, URI.create("http://localhost:" + server.getAddress().getPort() + "/v1"),
                "sk-test", "test-model"),
            List.of(new ChatMessage(ChatMessage.Role.USER, "你好")),
            0.2,
            128,
            false,
            timeout,
            Map.of()
        );
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> handler.handle(exchange));
        server.start();
    }

    private void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        assertNotNull(exchange);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
