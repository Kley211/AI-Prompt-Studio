package org.dromara.ai.model.infrastructure;

import org.dromara.ai.model.application.ChatMessage;
import org.dromara.ai.model.application.ChatModelGateway;
import org.dromara.ai.model.application.ChatModelRequest;
import org.dromara.ai.model.application.ChatModelResponse;
import org.dromara.ai.model.application.ChatStreamEvent;
import org.dromara.ai.model.application.ChatUsage;
import org.dromara.ai.model.application.ModelGatewayError;
import org.dromara.ai.model.application.ModelGatewayException;
import org.dromara.ai.model.application.ModelRuntimeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * OpenAI Chat Completions 兼容协议适配器。
 */
@Component
public class OpenAiCompatibleGateway implements ChatModelGateway {
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;

    @Autowired
    public OpenAiCompatibleGateway(JsonMapper jsonMapper) {
        this(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build(), jsonMapper);
    }

    OpenAiCompatibleGateway(HttpClient httpClient, JsonMapper jsonMapper) {
        this.httpClient = httpClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ChatModelResponse chat(ChatModelRequest request) {
        validate(request);
        HttpRequest httpRequest = createRequest(request, false);
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response.statusCode());
            return parseResponse(response.body());
        } catch (HttpTimeoutException exception) {
            throw gatewayException(ModelGatewayError.TIMEOUT, "模型服务响应超时", null, exception);
        } catch (ConnectException exception) {
            throw gatewayException(ModelGatewayError.PROVIDER_UNAVAILABLE, "无法连接模型服务", null, exception);
        } catch (IOException exception) {
            throw gatewayException(ModelGatewayError.PROVIDER_UNAVAILABLE, "模型服务通信失败", null, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw gatewayException(ModelGatewayError.PROVIDER_UNAVAILABLE, "模型调用被中断", null, exception);
        }
    }

    @Override
    public Flow.Publisher<ChatStreamEvent> stream(ChatModelRequest request) {
        validate(request);
        return subscriber -> {
            SubmissionPublisher<ChatStreamEvent> publisher = new SubmissionPublisher<>();
            publisher.subscribe(subscriber);
            CompletableFuture.runAsync(() -> readStream(request, publisher));
        };
    }

    private void readStream(ChatModelRequest request, SubmissionPublisher<ChatStreamEvent> publisher) {
        try {
            HttpResponse<Stream<String>> response = httpClient.send(
                createRequest(request, true), HttpResponse.BodyHandlers.ofLines());
            ensureSuccess(response.statusCode());
            AtomicReference<ChatUsage> usage = new AtomicReference<>(ChatUsage.unknown());
            AtomicReference<String> finishReason = new AtomicReference<>();
            boolean completed = false;
            try (Stream<String> lines = response.body()) {
                for (String line : (Iterable<String>) lines::iterator) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty()) {
                        continue;
                    }
                    if ("[DONE]".equals(data)) {
                        publishCompleted(publisher, usage.get(), finishReason.get());
                        completed = true;
                        break;
                    }
                    parseStreamChunk(data, publisher, usage, finishReason);
                }
            }
            if (!completed) {
                publishCompleted(publisher, usage.get(), finishReason.get());
            }
            publisher.close();
        } catch (HttpTimeoutException exception) {
            publisher.closeExceptionally(gatewayException(ModelGatewayError.TIMEOUT,
                "模型流式响应超时", null, exception));
        } catch (ConnectException exception) {
            publisher.closeExceptionally(gatewayException(ModelGatewayError.PROVIDER_UNAVAILABLE,
                "无法连接模型服务", null, exception));
        } catch (IOException exception) {
            publisher.closeExceptionally(gatewayException(ModelGatewayError.PROVIDER_UNAVAILABLE,
                "模型流式通信失败", null, exception));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            publisher.closeExceptionally(gatewayException(ModelGatewayError.PROVIDER_UNAVAILABLE,
                "模型流式调用被中断", null, exception));
        } catch (ModelGatewayException exception) {
            publisher.closeExceptionally(exception);
        } catch (RuntimeException exception) {
            publisher.closeExceptionally(gatewayException(ModelGatewayError.INVALID_RESPONSE,
                "模型服务返回了无效的流式响应", null, exception));
        }
    }

    private HttpRequest createRequest(ChatModelRequest request, boolean stream) {
        ModelRuntimeConfig runtime = request.runtime();
        HttpRequest.Builder builder = HttpRequest.newBuilder(chatCompletionsUri(runtime.baseUrl()))
            .timeout(request.timeout())
            .header("Accept", stream ? "text/event-stream" : "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(payload(request, stream))));
        if (runtime.apiKey() != null && !runtime.apiKey().isBlank()) {
            builder.header("Authorization", "Bearer " + runtime.apiKey());
        }
        return builder.build();
    }

    private Map<String, Object> payload(ChatModelRequest request, boolean stream) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", request.runtime().modelCode());
        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage message : request.messages()) {
            messages.add(Map.of(
                "role", message.role().name().toLowerCase(Locale.ROOT),
                "content", message.content()
            ));
        }
        payload.put("messages", messages);
        payload.put("stream", stream);
        if (stream) {
            payload.put("stream_options", Map.of("include_usage", true));
        }
        if (request.temperature() != null) {
            payload.put("temperature", request.temperature());
        }
        if (request.maxTokens() != null) {
            payload.put("max_tokens", request.maxTokens());
        }
        if (request.jsonMode()) {
            payload.put("response_format", Map.of("type", "json_object"));
        }
        return payload;
    }

    private ChatModelResponse parseResponse(String body) {
        try {
            JsonNode root = jsonMapper.readTree(body);
            JsonNode choice = firstChoice(root);
            JsonNode content = choice.path("message").path("content");
            if (!content.isTextual()) {
                throw new IllegalArgumentException("choices[0].message.content 缺失");
            }
            return new ChatModelResponse(
                content.asText(),
                parseUsage(root.path("usage")),
                textOrNull(root.path("id")),
                textOrNull(choice.path("finish_reason"))
            );
        } catch (RuntimeException exception) {
            throw gatewayException(ModelGatewayError.INVALID_RESPONSE, "模型服务返回了无效响应", null, exception);
        }
    }

    private void parseStreamChunk(String data, SubmissionPublisher<ChatStreamEvent> publisher,
                                  AtomicReference<ChatUsage> usage, AtomicReference<String> finishReason) {
        JsonNode root = jsonMapper.readTree(data);
        JsonNode usageNode = root.path("usage");
        if (!usageNode.isMissingNode() && !usageNode.isNull()) {
            usage.set(parseUsage(usageNode));
        }
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return;
        }
        JsonNode choice = choices.get(0);
        String reason = textOrNull(choice.path("finish_reason"));
        if (reason != null) {
            finishReason.set(reason);
        }
        JsonNode content = choice.path("delta").path("content");
        if (content.isTextual() && !content.asText().isEmpty()) {
            publisher.submit(new ChatStreamEvent(ChatStreamEvent.Type.CONTENT,
                content.asText(), ChatUsage.unknown(), null));
        }
    }

    private JsonNode firstChoice(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalArgumentException("choices 缺失");
        }
        return choices.get(0);
    }

    private ChatUsage parseUsage(JsonNode usage) {
        if (usage == null || usage.isMissingNode() || usage.isNull()) {
            return ChatUsage.unknown();
        }
        return new ChatUsage(usage.path("prompt_tokens").asInt(0), usage.path("completion_tokens").asInt(0));
    }

    private void publishCompleted(SubmissionPublisher<ChatStreamEvent> publisher,
                                  ChatUsage usage, String finishReason) {
        publisher.submit(new ChatStreamEvent(ChatStreamEvent.Type.COMPLETED, null, usage, finishReason));
    }

    private void ensureSuccess(int status) {
        if (status >= 200 && status < 300) {
            return;
        }
        ModelGatewayError error = switch (status) {
            case 400, 409, 422 -> ModelGatewayError.INVALID_REQUEST;
            case 401, 403 -> ModelGatewayError.AUTHENTICATION_FAILED;
            case 404 -> ModelGatewayError.MODEL_NOT_FOUND;
            case 408, 504 -> ModelGatewayError.TIMEOUT;
            case 429 -> ModelGatewayError.RATE_LIMITED;
            default -> status >= 500
                ? ModelGatewayError.PROVIDER_UNAVAILABLE
                : ModelGatewayError.INVALID_RESPONSE;
        };
        throw gatewayException(error, "模型服务调用失败（HTTP " + status + "）", status, null);
    }

    private void validate(ChatModelRequest request) {
        if (request == null || request.runtime() == null) {
            throw new ModelGatewayException(ModelGatewayError.INVALID_REQUEST, "模型运行配置不能为空");
        }
        ModelRuntimeConfig runtime = request.runtime();
        if (runtime.baseUrl() == null || runtime.modelCode() == null || runtime.modelCode().isBlank()) {
            throw new ModelGatewayException(ModelGatewayError.INVALID_REQUEST, "模型地址和模型编码不能为空");
        }
        if (request.messages().isEmpty()) {
            throw new ModelGatewayException(ModelGatewayError.INVALID_REQUEST, "模型消息不能为空");
        }
        for (ChatMessage message : request.messages()) {
            if (message == null || message.role() == null || message.content() == null) {
                throw new ModelGatewayException(ModelGatewayError.INVALID_REQUEST, "模型消息格式无效");
            }
        }
        if (request.timeout().isZero() || request.timeout().isNegative()) {
            throw new ModelGatewayException(ModelGatewayError.INVALID_REQUEST, "模型调用超时必须大于零");
        }
    }

    private URI chatCompletionsUri(URI baseUrl) {
        String base = baseUrl.toString().replaceAll("/+$", "");
        return URI.create(base.endsWith(CHAT_COMPLETIONS_PATH) ? base : base + CHAT_COMPLETIONS_PATH);
    }

    private String textOrNull(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private ModelGatewayException gatewayException(ModelGatewayError error, String message,
                                                   Integer status, Throwable cause) {
        return new ModelGatewayException(error, message, status, cause);
    }
}
