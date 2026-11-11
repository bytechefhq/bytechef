/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class RunCatalogToolCallingManager implements ToolCallingManager {

    static final String RUN_CATALOG_TOOL_CALLBACKS_KEY =
        RunCatalogToolCallingManager.class.getName() + ".runCatalogToolCallbacks";

    private final ToolCallingManager delegate;

    RunCatalogToolCallingManager(ToolCallingManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        return delegate.executeToolCalls(withRunCatalogToolCallbacks(prompt, chatResponse), chatResponse);
    }

    private static Prompt withRunCatalogToolCallbacks(Prompt prompt, ChatResponse chatResponse) {
        if (!(prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions)) {
            return prompt;
        }

        Map<String, Object> toolContext = toolCallingChatOptions.getToolContext();

        if (toolContext == null || !(toolContext.get(RUN_CATALOG_TOOL_CALLBACKS_KEY) instanceof Map<?, ?> runCatalog)) {
            return prompt;
        }

        List<ToolCallback> currentToolCallbacks = toolCallingChatOptions.getToolCallbacks();

        List<ToolCallback> toolCallbacks =
            currentToolCallbacks == null ? new ArrayList<>() : new ArrayList<>(currentToolCallbacks);

        Set<String> presentToolNames = new HashSet<>();

        for (ToolCallback toolCallback : toolCallbacks) {
            presentToolNames.add(toolCallback.getToolDefinition()
                .name());
        }

        for (Generation generation : chatResponse.getResults()) {
            AssistantMessage assistantMessage = generation.getOutput();

            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                if (!presentToolNames.contains(toolCall.name()) &&
                    runCatalog.get(toolCall.name()) instanceof ToolCallback toolCallback) {

                    toolCallbacks.add(toolCallback);
                    presentToolNames.add(toolCall.name());
                }
            }
        }

        Map<String, Object> remainingToolContext = new HashMap<>(toolContext);

        remainingToolContext.remove(RUN_CATALOG_TOOL_CALLBACKS_KEY);

        ToolCallingChatOptions.Builder<?> builder = (ToolCallingChatOptions.Builder<?>) toolCallingChatOptions.mutate();

        builder.toolCallbacks(toolCallbacks);
        builder.toolContext((Map<String, Object>) null);
        builder.toolContext(remainingToolContext);

        return prompt.mutate()
            .chatOptions(builder.build())
            .build();
    }
}
