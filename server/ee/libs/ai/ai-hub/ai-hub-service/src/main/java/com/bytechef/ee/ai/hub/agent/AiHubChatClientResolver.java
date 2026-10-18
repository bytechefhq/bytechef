/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.commons.util.NumberUtils;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Resolves the per-request {@link ChatClient} for AI Hub conversations through the Copilot's
 * {@link OverrideChatClientResolver}, so a hub turn picks its model exactly as a Copilot turn does: the user-selected
 * pair ({@link AiHubStateKeys#USER_SELECTED_LLM_PROVIDER_KEY} + {@link AiHubStateKeys#USER_SELECTED_LLM_MODEL_KEY}),
 * otherwise the environment default honouring {@code bytechef.ai.copilot.provider}. The environment is always the
 * server-verified {@link AiHubStateKeys#VERIFIED_ENVIRONMENT_ID}.
 *
 * <p>
 * Never returns {@code null}. A missing environment, an incomplete or unavailable selection, or an environment without
 * an enabled chat provider throws a {@link ChatModelUnavailableException}, which ends the turn with a RUN_ERROR.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
@ConditionalOnEEVersion
public class AiHubChatClientResolver implements AiHubSpringAIAgent.OverrideChatClientResolver {

    private static final String NO_ENVIRONMENT_MESSAGE =
        "The AI Hub run carries no environment, so no AI provider can be resolved for it.";

    private final OverrideChatClientResolver copilotChatClientResolver;

    @SuppressFBWarnings("EI")
    public AiHubChatClientResolver(OverrideChatClientResolver copilotChatClientResolver) {
        this.copilotChatClientResolver = copilotChatClientResolver;
    }

    @Override
    public ChatClient resolve(@Nullable State state) {
        if (state == null) {
            throw new ChatModelUnavailableException(NO_ENVIRONMENT_MESSAGE);
        }

        Long environmentId = NumberUtils.asLong(state.get(AiHubStateKeys.VERIFIED_ENVIRONMENT_ID));

        if (environmentId == null) {
            throw new ChatModelUnavailableException(NO_ENVIRONMENT_MESSAGE);
        }

        State copilotState = new State();

        copilotState.set(CopilotConstants.STATE_ENVIRONMENT_ID, environmentId);
        copilotState.set(
            CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER,
            state.get(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY));
        copilotState.set(
            CopilotConstants.STATE_USER_SELECTED_LLM_MODEL, state.get(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY));

        ChatClient chatClient = copilotChatClientResolver.resolve(copilotState);

        if (chatClient == null) {
            throw ChatModelUnavailableException.noProviderEnabled(environmentId);
        }

        return chatClient;
    }
}
