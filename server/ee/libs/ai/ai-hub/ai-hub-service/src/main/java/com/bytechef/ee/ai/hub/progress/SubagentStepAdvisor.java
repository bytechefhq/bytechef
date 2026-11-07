/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

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
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubagentStepAdvisor implements CallAdvisor, StreamAdvisor {

    private static final SubagentStepAdvisor INSTANCE = new SubagentStepAdvisor();

    private SubagentStepAdvisor() {
    }

    public static ChatClient attach(ChatClient chatClient) {
        return chatClient.mutate()
            .defaultAdvisors(INSTANCE)
            .build();
    }

    @Override
    public String getName() {
        return "ai-hub-subagent-step-progress";
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return callAdvisorChain.nextCall(withStepProgress(chatClientRequest));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        return streamAdvisorChain.nextStream(withStepProgress(chatClientRequest));
    }

    static ChatClientRequest withStepProgress(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        ChatOptions chatOptions = prompt.getOptions();

        if (!(chatOptions instanceof ToolCallingChatOptions toolCallingChatOptions)) {
            return chatClientRequest;
        }

        List<ToolCallback> toolCallbacks = toolCallingChatOptions.getToolCallbacks();

        if (toolCallbacks == null || toolCallbacks.isEmpty()) {
            return chatClientRequest;
        }

        List<ToolCallback> stepToolCallbacks = toolCallbacks.stream()
            .map(SubagentStepToolCallback::wrap)
            .toList();

        ToolCallingChatOptions stepChatOptions = toolCallingChatOptions.mutate()
            .toolCallbacks(stepToolCallbacks)
            .build();

        Prompt stepPrompt = prompt.mutate()
            .chatOptions(stepChatOptions)
            .build();

        return chatClientRequest.mutate()
            .prompt(stepPrompt)
            .build();
    }
}
