/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.agent.tool.CurrentAgentContext;
import com.bytechef.ai.agent.tool.CurrentAgentContext.AgentBinding;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolution;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class SubAgentCalls {

    private SubAgentCalls() {
    }

    static Map<String, Object> parentContext(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return Map.of();
        }

        Map<String, Object> context = toolContext.getContext();

        return context == null ? Map.of() : context;
    }

    static @Nullable String call(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver,
        AiHubAgentType agentType, String prompt, List<ToolCallback> toolCallbacks, Map<String, Object> parentContext) {

        ChatClient chatClient = chatClientFactory.get(
            SubAgentChatModelResolution.resolve(chatModelResolver, parentContext));

        AgentBinding parent = CurrentAgentContext.current();
        AgentType parentAgent = parent != null ? parent.agentName() : null;

        return CurrentAgentContext.callWith(
            agentType, parentAgent,
            () -> chatClient.prompt(prompt)
                .tools(toolCallbacks)
                .toolContext(parentContext)
                .call()
                .content());
    }
}
