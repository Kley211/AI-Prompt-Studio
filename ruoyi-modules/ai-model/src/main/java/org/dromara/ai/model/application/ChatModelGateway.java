package org.dromara.ai.model.application;

import java.util.concurrent.Flow;

public interface ChatModelGateway {
    ChatModelResponse chat(ChatModelRequest request);

    Flow.Publisher<ChatStreamEvent> stream(ChatModelRequest request);
}
