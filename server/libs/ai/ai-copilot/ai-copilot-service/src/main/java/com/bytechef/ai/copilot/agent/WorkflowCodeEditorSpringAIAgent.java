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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
public class WorkflowCodeEditorSpringAIAgent extends SpringAIAgent {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCodeEditorSpringAIAgent.class);

    private static final String ADDITIONAL_RULES =
        """
            ## Additional Rules

            - You assist with the full workflow definition (its triggers, tasks, component `type` references, `parameters`, and `${...}` datapill references), NOT with a single script component's code.
            - The assistant must not produce visual representations of any kind, including diagrams, charts, UI sketches, images, or pseudo-visuals.
            - When you produce a workflow definition, validate it first and return it as a single fenced code block in the editor's format. If it's impossible to resolve an error, instruct the user to raise an issue on our GitHub https://github.com/bytechefhq/bytechef/issues.
            """;

    private final @Nullable OverrideChatClientResolver overrideChatClientResolver;

    protected WorkflowCodeEditorSpringAIAgent(final Builder builder) throws AGUIException {
        super(builder);

        this.overrideChatClientResolver = builder.overrideChatClientResolver;
    }

    public static Builder builder() {
        return new Builder();
    }

    static String formatInstruction(@Nullable String format) {
        if (Objects.equals(format, "yaml")) {
            return "The workflow definition format you have to assist with is YAML.";
        }

        return "The workflow definition format you have to assist with is JSON.";
    }

    /**
     * Returns the per-request {@link ChatClient}. Consults the override resolver first (for the user-selected
     * (provider, model) supplied via AG-UI state); falls back to the builder-time default whenever the resolver is
     * absent, returns {@code null}, or throws. Mirrors the same hook on
     * {@code CodeEditorSpringAIAgent.resolveChatClient}.
     */
    @Override
    protected ChatClient resolveChatClient(RunAgentInput input) {
        if (overrideChatClientResolver == null) {
            return super.resolveChatClient(input);
        }

        try {
            ChatClient override = overrideChatClientResolver.resolve(input.state());

            if (override != null) {
                return override;
            }
        } catch (RuntimeException exception) {
            // The override path is best-effort: any failure (missing provider, factory throw, malformed state) must
            // fall back to the workspace default rather than failing the turn. Absence of an override simply means
            // "use the configured default."
            log.warn(
                "WorkflowCodeEditorSpringAIAgent: override ChatClient resolver threw; falling back to default. {}",
                exception.getMessage());
        }

        return super.resolveChatClient(input);
    }

    @Override
    protected Map<String, Object> toolContext(RunAgentInput input) {
        return CopilotToolContextUtils.toToolContext(input.state());
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Map<?, ?> parameters = (Map<?, ?>) state.get("parameters");

        String formatInstruction = formatInstruction(parameters == null ? null : (String) parameters.get("format"));

        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String resolvedMessage = Objects.nonNull(this.systemMessageProvider)
            ? this.systemMessageProvider.apply(this) : this.systemMessage;

        String message = "%s%n%s%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            resolvedMessage, formatInstruction, ADDITIONAL_RULES, state, String.join("\n", contextStrings));

        SystemMessage systemMessage = new SystemMessage();

        systemMessage.setId(String.valueOf(UUID.randomUUID()));
        systemMessage.setContent(message);

        return systemMessage;
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

        public WorkflowCodeEditorSpringAIAgent build() throws AGUIException {

            return new WorkflowCodeEditorSpringAIAgent(this);
        }
    }
}
