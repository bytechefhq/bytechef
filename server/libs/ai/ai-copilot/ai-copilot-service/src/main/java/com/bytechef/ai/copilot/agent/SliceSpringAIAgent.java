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
import com.agui.core.message.Role;
import com.agui.core.message.SystemMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.util.CopilotToolContextUtils;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.commons.util.NumberUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
public class SliceSpringAIAgent extends CopilotSpringAIAgent {

    private static final String ADDITIONAL_RULES =
        """
            ## Additional Rules

            - The assistant must not produce visual representations of any kind, including diagrams, charts, UI sketches, images, or pseudo-visuals.
            - If state.workflowExecutionError is not empty, there is an error and you must instruct the user on how to fix it. The user can't modify the code, only the input parameters. If it's impossible to fix the error, instruct the user to raise an issue on our GitHub https://github.com/bytechefhq/bytechef/issues.
            """;

    private final @Nullable Short sourceOrdinal;

    protected SliceSpringAIAgent(final Builder builder) throws AGUIException {
        super(builder, builder.overrideChatClientResolver);

        this.sourceOrdinal = builder.sourceOrdinal;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Map<String, Object> toolContext(RunAgentInput input) {
        State state = input.state();

        Map<String, Object> toolContext = new HashMap<>(CopilotToolContextUtils.toToolContext(state));

        // The copilot tool-context helper writes only the bytechef.agentTool.* family, but the deployment,
        // MCP-server and API-collection tools read AutomationToolInvocationContext, which is keyed on
        // bytechef.assetFile.*. Both families must be written or those tools resolve no workspace and fail
        // the turn with "Workspace context unavailable" — the same duplicate-key-in-lockstep contract
        // WorkspaceScopedSubAgentToolCallback and WorkflowExecutionSpringAIAgent already honour.
        putIfNotNull(toolContext, AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY,
            NumberUtils.asLong(state.get(CopilotConstants.STATE_WORKSPACE_ID)));
        putIfNotNull(toolContext, AutomationToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY,
            NumberUtils.asLong(state.get(CopilotConstants.STATE_AUTHENTICATED_USER_ID)));
        putIfNotNull(toolContext, AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY,
            NumberUtils.asLong(state.get(CopilotConstants.STATE_ENVIRONMENT_ID)));

        // sourceOrdinal is a property of the surface (e.g. Source.FILES for the asset-file slice), supplied by the
        // owning *AgentConfiguration via Builder#sourceOrdinal, not hardcoded here — this class is shared by every
        // slice (asset-file, MCP server, project deployment, API collection), and most of them create no
        // AI-generated rows, so they leave it unset.
        putIfNotNull(toolContext, AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY, sourceOrdinal);
        putIfNotNull(toolContext, AutomationToolInvocationContext.TOOL_CONTEXT_LAST_USER_PROMPT_KEY,
            lastUserPrompt(input.messages()));

        return toolContext;
    }

    private static void putIfNotNull(Map<String, Object> toolContext, String key, @Nullable Object value) {
        if (value != null) {
            toolContext.put(key, value);
        }
    }

    /**
     * Returns the content of the most recent user message, or {@code null} when there is none. Mirrors
     * {@code AiHubSpringAIAgent#lastUserPrompt}, which this CE agent cannot reference directly.
     */
    private static @Nullable String lastUserPrompt(@Nullable List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        for (int i = messages.size() - 1; i >= 0; i--) {
            BaseMessage message = messages.get(i);

            if (message instanceof UserMessage userMessage && Role.user.equals(userMessage.getRole())) {
                return userMessage.getContent();
            }
        }

        return null;
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String resolvedMessage = Objects.nonNull(this.systemMessageProvider)
            ? this.systemMessageProvider.apply(this) : this.systemMessage;

        String message = "%s%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            resolvedMessage, ADDITIONAL_RULES, state, String.join("\n", contextStrings));

        SystemMessage systemMessage = new SystemMessage();

        systemMessage.setId(String.valueOf(UUID.randomUUID()));
        systemMessage.setContent(message);

        return systemMessage;
    }

    public static class Builder extends SpringAIAgent.Builder {

        private @Nullable OverrideChatClientResolver overrideChatClientResolver;
        private @Nullable Short sourceOrdinal;

        public Builder overrideChatClientResolver(@Nullable OverrideChatClientResolver overrideChatClientResolver) {
            this.overrideChatClientResolver = overrideChatClientResolver;

            return this;
        }

        /**
         * Supplies the {@code AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY} value this slice writes
         * into every tool context, so AI-generated asset files created through it are attributed to the correct
         * surface. Leave unset for slices that never create asset files.
         */
        public Builder sourceOrdinal(@Nullable Short sourceOrdinal) {
            this.sourceOrdinal = sourceOrdinal;

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

        public SliceSpringAIAgent build() throws AGUIException {

            return new SliceSpringAIAgent(this);
        }
    }
}
