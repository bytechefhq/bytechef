/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class UnknownToolRecoveringToolCallingManager implements ToolCallingManager {

    private static final String NO_TOOL_CALLBACK_MESSAGE_PREFIX = "No ToolCallback found for tool name";

    private static final Logger log = LoggerFactory.getLogger(UnknownToolRecoveringToolCallingManager.class);

    private final ToolCallingManager delegate;

    UnknownToolRecoveringToolCallingManager(ToolCallingManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        try {
            return delegate.executeToolCalls(prompt, chatResponse);
        } catch (IllegalStateException exception) {
            String message = exception.getMessage();

            if (message == null || !message.startsWith(NO_TOOL_CALLBACK_MESSAGE_PREFIX)) {
                throw exception;
            }

            log.warn(
                "Recovering from unresolvable tool call ({}); answering the round with a tool error so the model can "
                    + "self-correct",
                message);

            return buildUnknownToolResult(prompt, chatResponse, message);
        }
    }

    private ToolExecutionResult buildUnknownToolResult(Prompt prompt, ChatResponse chatResponse, String message) {
        AssistantMessage assistantMessage = findToolCallMessage(chatResponse);

        List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

        for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
            toolResponses.add(
                new ToolResponseMessage.ToolResponse(
                    toolCall.id(), toolCall.name(),
                    "Error: " + message + ". The tool is not available in this deployment. Do not call it again; " +
                        "answer with the tools that are available."));
        }

        List<Message> conversationHistory = new ArrayList<>(prompt.getInstructions());

        conversationHistory.add(assistantMessage);
        conversationHistory.add(
            ToolResponseMessage.builder()
                .responses(toolResponses)
                .build());

        return ToolExecutionResult.builder()
            .conversationHistory(conversationHistory)
            .build();
    }

    private AssistantMessage findToolCallMessage(ChatResponse chatResponse) {
        for (Generation generation : chatResponse.getResults()) {
            AssistantMessage output = generation.getOutput();

            if (!output.getToolCalls()
                .isEmpty()) {

                return output;
            }
        }

        throw new IllegalStateException("No tool-call generation found in the chat response");
    }
}
