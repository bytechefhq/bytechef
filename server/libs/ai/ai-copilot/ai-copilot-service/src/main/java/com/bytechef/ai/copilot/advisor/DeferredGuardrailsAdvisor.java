/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.copilot.advisor;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.commons.util.NumberUtils;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.ai.guardrails.GuardrailAdvisorOrder;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
final class DeferredGuardrailsAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String NAME = "deferredGuardrails";

    private final ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderProvider;
    private final String surface;

    DeferredGuardrailsAdvisor(
        ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderProvider, String surface) {

        this.aiGuardrailsAdvisorProviderProvider = aiGuardrailsAdvisorProviderProvider;
        this.surface = surface;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return GuardrailAdvisorOrder.WORKSPACE_FLOOR;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        Advisor advisor = resolveAdvisor(chatClientRequest);

        if (advisor instanceof CallAdvisor callAdvisor) {
            return callAdvisor.adviseCall(chatClientRequest, callAdvisorChain);
        }

        return callAdvisorChain.nextCall(chatClientRequest);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        Advisor advisor = resolveAdvisor(chatClientRequest);

        if (advisor instanceof StreamAdvisor streamAdvisor) {
            return streamAdvisor.adviseStream(chatClientRequest, streamAdvisorChain);
        }

        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    private @Nullable Advisor resolveAdvisor(ChatClientRequest chatClientRequest) {
        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = aiGuardrailsAdvisorProviderProvider.getIfAvailable();

        if (aiGuardrailsAdvisorProvider == null) {
            return null;
        }

        return aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(workspaceId(chatClientRequest), surface)
            .orElse(null);
    }

    private static @Nullable Long workspaceId(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        if (!(prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions)) {
            return null;
        }

        Map<String, Object> toolContext = toolCallingChatOptions.getToolContext();

        if (CollectionUtils.isEmpty(toolContext)) {
            return null;
        }

        return NumberUtils.asLong(toolContext.get(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY));
    }
}
