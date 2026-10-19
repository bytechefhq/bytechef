/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

/**
 * Removes every tool the AI Hub gates by default ({@link AiHubToolApprovalDefaults#isGatedByDefault}) from the requests
 * of a sub-agent the hub delegates to, such as the {@code buildWorkflow} and {@code importWorkflow} specialists. Only
 * the hub's top-level tool callbacks pass the approval gate, so a gated tool must never be reachable from inside a
 * delegation.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubGatedToolFilterAdvisor implements CallAdvisor, StreamAdvisor {

    private static final AiHubGatedToolFilterAdvisor INSTANCE = new AiHubGatedToolFilterAdvisor();

    private AiHubGatedToolFilterAdvisor() {
    }

    public static ChatClient filter(ChatClient chatClient) {
        return chatClient.mutate()
            .defaultAdvisors(INSTANCE)
            .build();
    }

    @Override
    public String getName() {
        return "ai-hub-gated-tool-filter";
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return callAdvisorChain.nextCall(withoutGatedTools(chatClientRequest));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        return streamAdvisorChain.nextStream(withoutGatedTools(chatClientRequest));
    }

    static ChatClientRequest withoutGatedTools(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        ChatOptions chatOptions = prompt.getOptions();

        if (!(chatOptions instanceof ToolCallingChatOptions toolCallingChatOptions)) {
            return chatClientRequest;
        }

        List<ToolCallback> toolCallbacks = toolCallingChatOptions.getToolCallbacks();

        if (toolCallbacks == null || toolCallbacks.isEmpty()) {
            return chatClientRequest;
        }

        List<ToolCallback> ungatedToolCallbacks = toolCallbacks.stream()
            .filter(toolCallback -> !AiHubToolApprovalDefaults.isGatedByDefault(
                toolCallback.getToolDefinition()
                    .name()))
            .toList();

        if (ungatedToolCallbacks.size() == toolCallbacks.size()) {
            return chatClientRequest;
        }

        ToolCallingChatOptions filteredChatOptions = toolCallingChatOptions.mutate()
            .toolCallbacks(ungatedToolCallbacks)
            .build();

        Prompt filteredPrompt = prompt.mutate()
            .chatOptions(filteredChatOptions)
            .build();

        return chatClientRequest.mutate()
            .prompt(filteredPrompt)
            .build();
    }
}
