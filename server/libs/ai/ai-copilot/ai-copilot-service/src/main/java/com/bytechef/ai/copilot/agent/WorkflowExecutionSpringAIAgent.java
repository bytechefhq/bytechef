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

package com.bytechef.ai.copilot.agent;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.context.Context;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.SystemMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ai.copilot.util.CopilotToolContextUtils;
import com.bytechef.automation.ai.tool.WorkflowExecutionToolContextKeys;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
public class WorkflowExecutionSpringAIAgent extends CopilotSpringAIAgent {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionSpringAIAgent.class);

    private static final String ADDITIONAL_RULES =
        """
            ## Additional Rules

            - You are diagnosing and (in BUILD mode) helping fix workflow executions. Always inspect the actual run before drawing conclusions: call getWorkflowExecution with the workflowExecutionId from state, or listWorkflowExecutions to resolve a run the user describes.
            - Ground every claim about a failure in the task execution's real error / input / output. Do not speculate about causes you have not read from the execution data.
            - In ASK mode, explain the failure and the fix in plain language; do not mutate the workflow.
            - In BUILD mode, you may modify the workflow definition to fix the problem using the workflow / script tools, then summarise the change and why it addresses the observed error.
            - Do not produce diagrams, charts, or other visual representations.
            """;

    protected WorkflowExecutionSpringAIAgent(final Builder builder) throws AGUIException {
        super(builder, builder.overrideChatClientResolver);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Object parametersObject = state.get("parameters");

        String executionLine = "";

        if (parametersObject instanceof Map<?, ?> parameters) {
            Object workflowExecutionId = parameters.get("workflowExecutionId");

            if (workflowExecutionId != null) {
                executionLine =
                    "The workflow execution currently on screen has id %s. Inspect it with getWorkflowExecution."
                        .formatted(workflowExecutionId);
            }
        }

        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String resolvedMessage = Objects.nonNull(this.systemMessageProvider)
            ? this.systemMessageProvider.apply(this) : this.systemMessage;

        String message = "%s%n%s%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            resolvedMessage, executionLine, ADDITIONAL_RULES, state, String.join("\n", contextStrings));

        SystemMessage systemMessage = new SystemMessage();

        systemMessage.setId(String.valueOf(UUID.randomUUID()));
        systemMessage.setContent(message);

        return systemMessage;
    }

    @Override
    protected Map<String, Object> toolContext(RunAgentInput input) {
        State state = input.state();

        Map<String, Object> toolContext = new HashMap<>(CopilotToolContextUtils.toToolContext(state));

        Object parametersObject = state.get("parameters");

        if (parametersObject instanceof Map<?, ?> parameters) {
            putLong(toolContext, WorkflowExecutionToolContextKeys.WORKSPACE_ID, parameters.get("workspaceId"));
            putLong(toolContext, WorkflowExecutionToolContextKeys.ENVIRONMENT_ID, parameters.get("environmentId"));
        }

        return toolContext;
    }

    private static void putLong(Map<String, Object> target, String key, @Nullable Object value) {
        if (value instanceof Number number) {
            target.put(key, number.longValue());
        } else if (value instanceof String string && !string.isBlank()) {
            try {
                target.put(key, Long.parseLong(string));
            } catch (NumberFormatException numberFormatException) {
                // Leave the key unset — a malformed workspace/environment id degrades to "unscoped",
                // which listWorkflowExecutions treats as an empty result rather than a hard failure.
                log.debug("Ignoring malformed long value '{}' for key '{}'", string, key);
            }
        }
    }

    public static class Builder extends SpringAIAgent.Builder {

        private @Nullable OverrideChatClientResolver overrideChatClientResolver;

        public Builder overrideChatClientResolver(@Nullable OverrideChatClientResolver overrideChatClientResolver) {
            this.overrideChatClientResolver = overrideChatClientResolver;

            return this;
        }

        public Builder chatModel(ChatModel chatModel) {
            super.chatModel(chatModel);

            return this;
        }

        public Builder advisors(List<Advisor> advisors) {
            super.advisors(advisors);

            return this;
        }

        public Builder advisor(Advisor advisor) {
            super.advisor(advisor);

            return this;
        }

        public Builder tools(List<Object> tools) {
            super.tools(tools);

            return this;
        }

        public Builder tool(Object tool) {
            super.tool(tool);

            return this;
        }

        public Builder agentId(String agentId) {
            super.agentId(agentId);

            return this;
        }

        public Builder state(State state) {
            super.state(state);

            return this;
        }

        public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
            super.toolCallbacks(toolCallbacks);

            return this;
        }

        public Builder toolCallback(ToolCallback toolCallback) {
            super.toolCallback(toolCallback);

            return this;
        }

        public Builder systemMessage(String systemMessage) {
            super.systemMessage(systemMessage);

            return this;
        }

        public Builder systemMessageProvider(Function<LocalAgent, String> systemMessageProvider) {
            super.systemMessageProvider(systemMessageProvider);

            return this;
        }

        public Builder chatMemory(ChatMemory chatMemory) {
            super.chatMemory(chatMemory);

            return this;
        }

        public Builder messages(List<BaseMessage> messages) {
            super.messages(messages);

            return this;
        }

        public WorkflowExecutionSpringAIAgent build() throws AGUIException {

            return new WorkflowExecutionSpringAIAgent(this);
        }
    }
}
