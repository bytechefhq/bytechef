/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.state.State;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent.SelectedLlm;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import org.junit.jupiter.api.Test;

/**
 * Pins {@link AiHubSpringAIAgent#resolveSelectedLlm(State)} against the same precedence
 * {@link AiHubChatClientResolver#resolve(State)} uses: the user's per-conversation selection, and a null fallback to
 * the workspace default whenever that pair is absent or half-set. A mismatch here would let a delegate subagent run on
 * a different model than the one its caller resolved for the turn.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubToolContextModelPropagationTest {

    private static final String OPENAI_KEY = "ai.provider.openAi";
    private static final String OPENAI_MODEL = "gpt-4o";

    @Test
    void testResolveSelectedLlmPropagatesUserSelected() {
        State state = new State();

        state.set(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, OPENAI_KEY);
        state.set(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY, OPENAI_MODEL);

        SelectedLlm selectedLlm = AiHubSpringAIAgent.resolveSelectedLlm(state);

        assertThat(selectedLlm).isEqualTo(new SelectedLlm(OPENAI_KEY, OPENAI_MODEL));
    }

    @Test
    void testResolveSelectedLlmReturnsNullWhenHalfSet() {
        State state = new State();

        state.set(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, OPENAI_KEY);

        SelectedLlm selectedLlm = AiHubSpringAIAgent.resolveSelectedLlm(state);

        assertThat(selectedLlm).isNull();
    }

    @Test
    void testResolveSelectedLlmReturnsNullWhenAbsent() {
        assertThat(AiHubSpringAIAgent.resolveSelectedLlm(new State())).isNull();
        assertThat(AiHubSpringAIAgent.resolveSelectedLlm(null)).isNull();
    }
}
