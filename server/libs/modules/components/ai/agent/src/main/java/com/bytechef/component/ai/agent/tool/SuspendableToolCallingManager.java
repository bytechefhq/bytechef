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
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * A {@link ToolCallingManager} decorator that makes the agent's tool-calling loop resumable. After delegating tool
 * execution, it inspects the agent {@link ActionContext} for a suspend; if a tool suspended, it captures the
 * conversation into the suspend's {@code continueParameters} and returns {@code returnDirect=true} so Spring AI's
 * {@code ToolCallAdvisor} halts its loop at the suspend point.
 *
 * @author Ivica Cardic
 */
public final class SuspendableToolCallingManager implements ToolCallingManager {

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
            result = delegate.executeToolCalls(prompt, chatResponse);
        } catch (RuntimeException exception) {
            // If a delegate tool throws *after* a suspending tool has already set a suspend on the context, the
            // conversation cannot be captured (the sentinel response was never produced) and a downstream resume
            // would fail when reconstructing the conversation. The exception itself unwinds before
            // ActionDefinitionServiceImpl.checkSuspend runs, so the orphan suspend is not persisted — but log a
            // WARN so the partial-suspend state is diagnosable from the action's failure timeline.
            if (actionContext.getSuspend() != null) {
                log.warn(
                    "Delegate tool execution threw after a suspending tool set a suspend on the agent context. " +
                        "The suspend will not be persisted (the exception unwinds before checkSuspend); the agent " +
                        "fails with the underlying exception.",
                    exception);
            }

            throw exception;
        }

        ActionContext.Suspend suspend = actionContext.getSuspend();

        if (suspend == null) {
            return result;
        }

        List<Message> conversation = result.conversationHistory();

        String pendingToolCallId = findSentinelToolResponseId(conversation);

        Map<String, Object> continueParameters = new HashMap<>(suspend.continueParameters());

        continueParameters.put(ToolSuspendConstants.CONVERSATION_STATE, ConversationState.from(conversation));
        continueParameters.put(ToolSuspendConstants.PENDING_TOOL_CALL_ID, pendingToolCallId);

        actionContext.suspend(new ActionContext.Suspend(continueParameters, suspend.expiresAt()));

        return ToolExecutionResult.builder()
            .conversationHistory(conversation)
            .returnDirect(true)
            .build();
    }

    private static String findSentinelToolResponseId(List<Message> conversation) {
        String pendingToolCallId = null;

        for (Message message : conversation) {
            if (!(message instanceof ToolResponseMessage toolResponseMessage)) {
                continue;
            }

            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                if (!ToolSuspendConstants.SUSPENDED_SENTINEL.equals(toolResponse.responseData())) {
                    continue;
                }

                if (pendingToolCallId != null) {
                    throw new IllegalStateException(
                        "More than one tool suspended in a single agent turn, which is not supported. " +
                            "Ensure the model calls at most one suspending tool per turn.");
                }

                pendingToolCallId = toolResponse.id();
            }
        }

        if (pendingToolCallId == null) {
            throw new IllegalStateException(
                "A tool set a suspend on the agent context but no tool response carried the suspend sentinel.");
        }

        return pendingToolCallId;
    }
}
