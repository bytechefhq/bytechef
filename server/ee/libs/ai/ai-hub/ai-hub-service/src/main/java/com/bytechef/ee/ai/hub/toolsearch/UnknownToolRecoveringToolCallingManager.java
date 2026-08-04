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
 * A {@link ToolCallingManager} decorator that converts an unresolvable tool name into a recoverable tool-error response
 * instead of letting it crash the whole chat turn.
 *
 * <p>
 * {@code DefaultToolCallingManager} throws {@code IllegalStateException("No ToolCallback found for tool name: ...")}
 * when the model calls a tool that is neither on the request's options list nor known to the resolver. On the streaming
 * chat surface that exception propagates through the tool-calling loop and kills the entire SSE turn — the user sees a
 * dead stream rather than an answer. The model can legitimately produce such a call: the system prompt or an earlier
 * conversation (rehydrated from session memory) may reference a tool that is conditionally registered and absent in
 * this deployment (e.g. {@code research} without a Firecrawl key), or the model may adapt/truncate a tool name.
 * </p>
 *
 * <p>
 * This decorator catches exactly that failure and instead returns a {@link ToolExecutionResult} whose conversation
 * history answers <em>every</em> tool call of the round with an error payload naming the unavailable tool, so the model
 * observes the failure and self-corrects on its next iteration. Trade-off, deliberate: when a round mixes resolvable
 * and unresolvable calls, the resolvable ones are not executed either — they receive the same error response and the
 * model simply re-issues them. Partially executing the round would require re-implementing the delegate's execution
 * internals here, which is not worth it for a rare failure mode.
 * </p>
 *
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

    /**
     * Mirrors the delegate's generation selection: the tool responses must answer the generation that carries the tool
     * calls, which is not necessarily the first one.
     */
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
