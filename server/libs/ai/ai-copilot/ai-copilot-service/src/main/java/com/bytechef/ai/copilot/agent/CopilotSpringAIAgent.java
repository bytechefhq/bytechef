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

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.EventFactory;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.NumberUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * @author Ivica Cardic
 */
public abstract class CopilotSpringAIAgent extends SpringAIAgent {

    private static final String AWAITING_ANSWER_KEY = "awaitingAnswer";

    private static final Logger log = LoggerFactory.getLogger(CopilotSpringAIAgent.class);

    private final @Nullable OverrideChatClientResolver overrideChatClientResolver;

    protected CopilotSpringAIAgent(
        SpringAIAgent.Builder builder, @Nullable OverrideChatClientResolver overrideChatClientResolver)
        throws AGUIException {

        super(builder);

        this.overrideChatClientResolver = overrideChatClientResolver;
    }

    @Override
    protected final void run(RunAgentInput input, AgentSubscriber subscriber) {
        AgentSubscriber decoratedSubscriber = decorateSubscriber(subscriber);

        String tenantId = getTenantId(input);

        if (tenantId == null) {
            log.error(
                "{}: run state carries no '{}'; refusing to run the agent on the ambient tenant",
                getClass().getSimpleName(), CopilotConstants.STATE_TENANT_ID);

            emitEvent(
                EventFactory.runErrorEvent("The Copilot run carries no tenant, so it cannot run."),
                decoratedSubscriber);

            return;
        }

        runWithTenant(tenantId, () -> runWithEnvironment(input, () -> super.run(input, decoratedSubscriber)));
    }

    protected AgentSubscriber decorateSubscriber(AgentSubscriber subscriber) {
        return subscriber;
    }

    void runWithTenant(String tenantId, Runnable action) {
        String previousTenantId = TenantContext.getCurrentTenantId();

        TenantContext.setCurrentTenantId(tenantId);

        try {
            action.run();
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }

    void runWithEnvironment(RunAgentInput input, Runnable action) {
        Integer environmentId = getEnvironmentId(input);

        Environment previousEnvironment = EnvironmentContext.fetchCurrentEnvironment();

        if (environmentId != null) {
            EnvironmentContext.set(environmentId);
        }

        try {
            action.run();
        } finally {
            if (environmentId != null) {
                if (previousEnvironment == null) {
                    EnvironmentContext.clear();
                } else {
                    EnvironmentContext.set(previousEnvironment);
                }
            }
        }
    }

    @Override
    protected ChatClient resolveChatClient(RunAgentInput input) throws AGUIException {
        if (overrideChatClientResolver == null) {
            return super.resolveChatClient(input);
        }

        ChatClient override;

        try {
            override = overrideChatClientResolver.resolve(input.state());
        } catch (ChatModelUnavailableException exception) {
            log.warn("{}: {}", getClass().getSimpleName(), exception.getMessage());

            throw new AGUIException(exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            log.warn("{}: resolving the chat model failed", getClass().getSimpleName(), exception);

            throw new AGUIException(
                "Could not resolve the AI model for this Copilot run: " + exception.getMessage(), exception);
        }

        if (override != null) {
            return override;
        }

        return super.resolveChatClient(input);
    }

    @Override
    protected String conversationId(RunAgentInput input) {
        State state = input.state();

        String conversationScope = state == null
            ? null : StringUtils.asString(state.get(CopilotConstants.STATE_CONVERSATION_SCOPE));

        if (conversationScope == null || conversationScope.isBlank()) {
            return super.conversationId(input);
        }

        String scopedConversationId = conversationScope + ":" + input.threadId();

        UUID uuid = UUID.nameUUIDFromBytes(scopedConversationId.getBytes(StandardCharsets.UTF_8));

        return uuid.toString();
    }

    @Override
    protected boolean endsTurn(ToolResponseMessage.ToolResponse toolResponse) {
        String responseData = toolResponse.responseData();

        if (responseData == null || !responseData.contains(AWAITING_ANSWER_KEY)) {
            return false;
        }

        try {
            return JsonUtils.read(responseData) instanceof Map<?, ?> response
                && Boolean.TRUE.equals(response.get(AWAITING_ANSWER_KEY));
        } catch (RuntimeException exception) {
            log.warn(
                "Tool {} returned an {} payload that isn't valid JSON; continuing the turn", toolResponse.name(),
                AWAITING_ANSWER_KEY, exception);

            return false;
        }
    }

    @Override
    protected boolean splitsTextAtToolCalls() {
        return true;
    }

    @Override
    protected void prepareChatMemory(RunAgentInput input, ChatMemory chatMemory, String conversationId) {
        State state = input.state();

        if (state == null || state.get(CopilotConstants.STATE_SCOPED_PROJECT_ID) == null) {
            return;
        }

        List<String> userTexts = input.messages()
            .stream()
            .filter(message -> message instanceof UserMessage)
            .map(BaseMessage::getContent)
            .toList();

        if (userTexts.isEmpty()) {
            return;
        }

        ChatMemoryRewinder.rewind(chatMemory, conversationId, userTexts.subList(0, userTexts.size() - 1));
    }

    @Override
    protected String runErrorMessage(RunAgentInput input, Throwable throwable) {
        State state = input.state();

        if (state != null && state.get(CopilotConstants.STATE_SCOPED_PROJECT_ID) != null) {
            String description = CopilotRunFailures.describe(throwable);

            if (description != null) {
                return description + " Reference: " + input.runId();
            }

            return "The copilot run failed. Reference: " + input.runId();
        }

        String message = throwable.getMessage();

        return message == null ? "The copilot run failed. Reference: " + input.runId() : message;
    }

    private @Nullable Integer getEnvironmentId(RunAgentInput input) {
        State state = input.state();

        if (state == null) {
            return null;
        }

        Object environmentIdValue = state.get(CopilotConstants.STATE_ENVIRONMENT_ID);

        if (environmentIdValue == null) {
            return null;
        }

        Long environmentId = NumberUtils.asLong(environmentIdValue);

        if (environmentId == null || environmentId < 0 || environmentId >= Environment.values().length) {
            log.warn(
                "{}: run state carries an invalid '{}' value '{}'; leaving the environment context unbound",
                getClass().getSimpleName(), CopilotConstants.STATE_ENVIRONMENT_ID, environmentIdValue);

            return null;
        }

        return environmentId.intValue();
    }

    private static @Nullable String getTenantId(RunAgentInput input) {
        State state = input.state();

        if (state == null) {
            return null;
        }

        String tenantId = StringUtils.asString(state.get(CopilotConstants.STATE_TENANT_ID));

        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }

        return tenantId;
    }
}
