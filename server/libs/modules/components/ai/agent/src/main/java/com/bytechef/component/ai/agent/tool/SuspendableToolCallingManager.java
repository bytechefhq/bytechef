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

package com.bytechef.component.ai.agent.tool;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
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
 * A {@link ToolCallingManager} that lets a tool suspend the agent. A tool suspends by calling
 * {@link ActionContext#suspend} on the agent's action context and returning {@link ToolSuspension}'s suspended tool
 * result; this manager then stores the conversation and the pending tool call in the suspend's continue parameters
 * under {@link AgentToolSuspension#CONTINUE_PARAMETER_KEY}, which a suspending tool must not use, and ends the turn.
 *
 * <p>
 * When the model calls several tools in one round, they run one at a time, because a tool that suspends cannot be
 * identified before it runs. After a tool suspends, the remaining calls of the round are not executed and get
 * {@link #NOT_EXECUTED_TOOL_RESULT} as their result.
 *
 * @author Ivica Cardic
 */
public final class SuspendableToolCallingManager implements ToolCallingManager {

    public static final String NOT_EXECUTED_TOOL_RESULT =
        "Not executed: another tool call in this turn paused the conversation for a human response. Call this " +
            "tool again if it is still needed.";

    private static final Logger log = LoggerFactory.getLogger(SuspendableToolCallingManager.class);

    private final ToolCallingManager delegate;
    private final ActionContextAware actionContext;

    public SuspendableToolCallingManager(ToolCallingManager delegate, ActionContextAware actionContext) {
        this.delegate = delegate;
        this.actionContext = actionContext;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        ToolExecutionResult result;

        try {
            result = executeToolCallsUntilSuspended(prompt, chatResponse);
        } catch (RuntimeException exception) {
            if (actionContext.getSuspend() != null) {
                log.warn(
                    "Tool execution failed after a tool suspended the agent. The suspend is not persisted because " +
                        "the exception fails the agent.",
                    exception);
            }

            throw exception;
        }

        List<Message> conversation = result.conversationHistory();

        ActionContext.Suspend suspend = actionContext.getSuspend();

        if (suspend == null) {
            if (hasSuspendedToolResponse(conversation)) {
                throw new IllegalStateException(
                    "A tool returned the suspended tool result without suspending the agent; the agent's action " +
                        "context did not reach the tool.");
            }

            return result;
        }

        Map<String, Object> continueParameters = new HashMap<>(suspend.continueParameters());

        if (continueParameters.containsKey(AgentToolSuspension.CONTINUE_PARAMETER_KEY)) {
            throw new IllegalStateException(
                "The suspending tool's continue parameters already contain the reserved key '" +
                    AgentToolSuspension.CONTINUE_PARAMETER_KEY + "'.");
        }

        continueParameters.put(
            AgentToolSuspension.CONTINUE_PARAMETER_KEY,
            new AgentToolSuspension(
                ConversationState.from(conversation), findSuspendedToolCallId(conversation)));

        actionContext.suspend(new ActionContext.Suspend(continueParameters, suspend.expiresAt()));

        return ToolExecutionResult.builder()
            .conversationHistory(withEmptiedLastToolResponse(conversation))
            .returnDirect(true)
            .build();
    }

    private ToolExecutionResult executeToolCallsUntilSuspended(Prompt prompt, ChatResponse chatResponse) {
        Generation toolCallGeneration = chatResponse.getResults()
            .stream()
            .filter(generation -> generation.getOutput()
                .hasToolCalls())
            .findFirst()
            .orElse(null);

        if (toolCallGeneration == null) {
            return delegate.executeToolCalls(prompt, chatResponse);
        }

        AssistantMessage assistantMessage = toolCallGeneration.getOutput();

        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();

        if (toolCalls.size() < 2) {
            return delegate.executeToolCalls(prompt, chatResponse);
        }

        List<Message> previousMessages = null;
        boolean returnDirect = true;
        List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
        Map<String, Object> toolResponseMetadata = Map.of();

        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            if (actionContext.getSuspend() != null) {
                toolResponses.add(
                    new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), NOT_EXECUTED_TOOL_RESULT));

                continue;
            }

            AssistantMessage singleToolCallAssistantMessage = assistantMessage.mutate()
                .toolCalls(List.of(toolCall))
                .build();

            ToolExecutionResult toolExecutionResult = delegate.executeToolCalls(
                prompt,
                new ChatResponse(
                    List.of(new Generation(singleToolCallAssistantMessage, toolCallGeneration.getMetadata())),
                    chatResponse.getMetadata()));

            List<Message> conversation = toolExecutionResult.conversationHistory();

            if (previousMessages == null) {
                previousMessages = conversation.subList(0, conversation.size() - 2);
            }

            if (conversation.getLast() instanceof ToolResponseMessage toolResponseMessage) {
                toolResponses.addAll(toolResponseMessage.getResponses());

                toolResponseMetadata = toolResponseMessage.getMetadata();
            }

            returnDirect = returnDirect && toolExecutionResult.returnDirect();
        }

        List<Message> conversation = new ArrayList<>(previousMessages == null ? List.of() : previousMessages);

        conversation.add(assistantMessage);
        conversation.add(
            ToolResponseMessage.builder()
                .responses(toolResponses)
                .metadata(toolResponseMetadata)
                .build());

        return ToolExecutionResult.builder()
            .conversationHistory(conversation)
            .returnDirect(returnDirect)
            .build();
    }

    private static boolean hasSuspendedToolResponse(List<Message> conversation) {
        for (Message message : conversation) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                    if (ToolSuspension.isSuspendedToolResult(toolResponse.responseData())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static List<Message> withEmptiedLastToolResponse(List<Message> conversation) {
        if (conversation.isEmpty() || !(conversation.getLast() instanceof ToolResponseMessage toolResponseMessage)) {
            return conversation;
        }

        List<Message> messages = new ArrayList<>(conversation.subList(0, conversation.size() - 1));

        messages.add(
            ToolResponseMessage.builder()
                .responses(List.of())
                .metadata(toolResponseMessage.getMetadata())
                .build());

        return messages;
    }

    private static String findSuspendedToolCallId(List<Message> conversation) {
        String suspendedToolCallId = null;
        List<String> otherToolResponses = new ArrayList<>();

        for (Message message : conversation) {
            if (!(message instanceof ToolResponseMessage toolResponseMessage)) {
                continue;
            }

            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                if (!ToolSuspension.isSuspendedToolResult(toolResponse.responseData())) {
                    otherToolResponses.add(
                        toolResponse.name() + " (id '" + toolResponse.id() + "'): " +
                            abbreviate(toolResponse.responseData()));

                    continue;
                }

                if (suspendedToolCallId != null) {
                    throw new IllegalStateException(
                        "More than one suspended tool response found in the conversation, which is not supported. " +
                            "Ensure the model calls at most one suspending tool per turn.");
                }

                suspendedToolCallId = toolResponse.id();
            }
        }

        if (suspendedToolCallId == null) {
            throw new IllegalStateException(
                "A tool suspended the agent but no tool response carried the suspended tool result; the tool most " +
                    "likely failed after it suspended. Other tool responses: " + otherToolResponses);
        }

        return suspendedToolCallId;
    }

    private static String abbreviate(@Nullable String text) {
        if (text == null) {
            return "null";
        }

        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }
}
