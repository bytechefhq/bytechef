/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

/**
 * Removes every tool the AI Hub gates from the requests of a sub-agent the hub delegates to, such as the
 * {@code buildWorkflow} and {@code importWorkflow} specialists: each tool gated by default
 * ({@link AiHubToolApprovalDefaults#isGatedByDefault}) and each tool the workspace's rules gate
 * ({@link AiHubToolApprovalPolicy#decide}), resolved from the workspace id in the sub-agent's tool context. Only the
 * hub's top-level tool callbacks pass the approval gate, so a gated tool must never be reachable from inside a
 * delegation. A workspace {@code EXEMPT} rule lifts a default for the hub's own calls only; it never exposes a
 * default-gated tool to a sub-agent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubGatedToolFilterAdvisor implements CallAdvisor, StreamAdvisor {

    private final AiHubToolApprovalPolicy policy;

    private AiHubGatedToolFilterAdvisor(AiHubToolApprovalPolicy policy) {
        this.policy = policy;
    }

    public static ChatClient filter(ChatClient chatClient, AiHubToolApprovalPolicy policy) {
        return chatClient.mutate()
            .defaultAdvisors(new AiHubGatedToolFilterAdvisor(policy))
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

    ChatClientRequest withoutGatedTools(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        ChatOptions chatOptions = prompt.getOptions();

        if (!(chatOptions instanceof ToolCallingChatOptions toolCallingChatOptions)) {
            return chatClientRequest;
        }

        List<ToolCallback> toolCallbacks = toolCallingChatOptions.getToolCallbacks();

        if (toolCallbacks == null || toolCallbacks.isEmpty()) {
            return chatClientRequest;
        }

        Predicate<String> gatedToolPredicate = gatedToolPredicate(toolCallingChatOptions.getToolContext());

        List<ToolCallback> ungatedToolCallbacks = toolCallbacks.stream()
            .filter(toolCallback -> !gatedToolPredicate.test(
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

    private Predicate<String> gatedToolPredicate(@Nullable Map<String, Object> toolContextMap) {
        Long workspaceId = null;

        if (toolContextMap != null && !toolContextMap.isEmpty()) {
            AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(
                new ToolContext(toolContextMap));

            if (invocationContext != null) {
                workspaceId = invocationContext.workspaceId();
            }
        }

        if (workspaceId == null) {
            return AiHubToolApprovalDefaults::isGatedByDefault;
        }

        AiHubToolApprovalPolicy.Decision decision = policy.decide(workspaceId);

        return toolName -> AiHubToolApprovalDefaults.isGatedByDefault(toolName) || decision.isGated(toolName);
    }
}
