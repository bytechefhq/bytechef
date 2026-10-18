/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.agent.SelectedChatModelUnavailableException;
import com.bytechef.ee.ai.copilot.agent.CopilotChatClientResolver;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Pins {@link AiHubChatClientResolver#resolve(State)} to the Copilot's model resolution: it runs over the real
 * {@link CopilotChatClientResolver} with a mocked catalog, reading the hub's verified environment and selection keys.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatClientResolverTest {

    private static final int ENVIRONMENT_ID = 1;
    private static final long WORKSPACE_ID = 42L;
    private static final String ANTHROPIC_KEY = "anthropic";
    private static final String OPENAI_KEY = "ai.provider.openAi";
    private static final String OPENAI_MODEL = "gpt-4o";

    private CatalogChatClientResolver catalogChatClientResolver;

    private ChatClient defaultChatClient;
    private ChatClient openAiChatClient;
    private ChatClient preferredChatClient;

    private AiHubChatClientResolver resolver;

    @BeforeEach
    void setUp() {
        catalogChatClientResolver = mock(CatalogChatClientResolver.class);

        defaultChatClient = mock(ChatClient.class);
        openAiChatClient = mock(ChatClient.class);
        preferredChatClient = mock(ChatClient.class);

        resolver = newResolver("");
    }

    @Test
    void testUserSelectedResolvesViaCatalog() {
        when(catalogChatClientResolver.resolve(OPENAI_KEY, OPENAI_MODEL, ENVIRONMENT_ID)).thenReturn(openAiChatClient);

        assertThat(resolver.resolve(selectedState())).isSameAs(openAiChatClient);
        verify(catalogChatClientResolver, never()).resolveDefault(anyInt());
    }

    @Test
    void testNoSelectionResolvesTheEnvironmentDefault() {
        when(catalogChatClientResolver.resolveDefault(ENVIRONMENT_ID)).thenReturn(defaultChatClient);

        assertThat(resolver.resolve(newState())).isSameAs(defaultChatClient);
        verify(catalogChatClientResolver, never()).resolve(anyString(), anyString(), anyInt());
    }

    @Test
    void testNoSelectionPrefersTheConfiguredCopilotProvider() {
        when(catalogChatClientResolver.resolvePreferred(ANTHROPIC_KEY, ENVIRONMENT_ID)).thenReturn(preferredChatClient);
        when(catalogChatClientResolver.resolveDefault(ENVIRONMENT_ID)).thenReturn(defaultChatClient);

        assertThat(newResolver(ANTHROPIC_KEY).resolve(newState())).isSameAs(preferredChatClient);
    }

    @Test
    void testUnusablePreferredProviderResolvesTheEnvironmentDefault() {
        when(catalogChatClientResolver.resolveDefault(ENVIRONMENT_ID)).thenReturn(defaultChatClient);

        assertThat(newResolver(ANTHROPIC_KEY).resolve(newState())).isSameAs(defaultChatClient);
    }

    @Test
    void testHalfSetSelectionFails() {
        State state = newState();

        state.set(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, OPENAI_KEY);

        assertThatThrownBy(() -> resolver.resolve(state))
            .isInstanceOf(ChatModelUnavailableException.class)
            .hasMessageContaining("The model selection is incomplete");
        verify(catalogChatClientResolver, never()).resolveDefault(anyInt());
    }

    @Test
    void testUnresolvableSelectionFails() {
        when(catalogChatClientResolver.resolveDefault(ENVIRONMENT_ID)).thenReturn(defaultChatClient);

        assertThatThrownBy(() -> resolver.resolve(selectedState()))
            .isInstanceOf(SelectedChatModelUnavailableException.class)
            .hasMessageContaining(
                "The selected model " + OPENAI_MODEL + " (" + OPENAI_KEY + ") is not available in environment " +
                    ENVIRONMENT_ID);
        verify(catalogChatClientResolver, never()).resolveDefault(anyInt());
    }

    @Test
    void testSelectionResolutionFailureIsWrapped() {
        IllegalStateException cause = new IllegalStateException("factory failed");

        when(catalogChatClientResolver.resolve(OPENAI_KEY, OPENAI_MODEL, ENVIRONMENT_ID)).thenThrow(cause);

        assertThatThrownBy(() -> resolver.resolve(selectedState()))
            .isExactlyInstanceOf(ChatModelUnavailableException.class)
            .hasCause(cause)
            .hasMessageContaining("factory failed");
    }

    @Test
    void testNoEnabledProviderFails() {
        assertThatThrownBy(() -> resolver.resolve(newState()))
            .isInstanceOf(ChatModelUnavailableException.class)
            .hasMessageContaining("No AI provider is enabled in environment " + ENVIRONMENT_ID);
    }

    @Test
    void testDefaultResolutionFailureFails() {
        IllegalStateException cause = new IllegalStateException("catalog down");

        when(catalogChatClientResolver.resolveDefault(ENVIRONMENT_ID)).thenThrow(cause);

        assertThatThrownBy(() -> resolver.resolve(newState()))
            .isInstanceOf(ChatModelUnavailableException.class)
            .hasCause(cause);
    }

    @Test
    void testClientEnvironmentIsIgnoredInFavourOfTheVerifiedOne() {
        when(catalogChatClientResolver.resolveDefault(ENVIRONMENT_ID)).thenReturn(defaultChatClient);

        State state = newState();

        state.set(AiHubStateKeys.ENVIRONMENT_ID, ENVIRONMENT_ID + 1);

        assertThat(resolver.resolve(state)).isSameAs(defaultChatClient);
    }

    @Test
    void testMissingEnvironmentFails() {
        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, WORKSPACE_ID);
        state.set(AiHubStateKeys.ENVIRONMENT_ID, ENVIRONMENT_ID);
        state.set(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, OPENAI_KEY);
        state.set(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY, OPENAI_MODEL);

        assertThatThrownBy(() -> resolver.resolve(state))
            .isInstanceOf(ChatModelUnavailableException.class)
            .hasMessageContaining("The AI Hub run carries no environment");
        verify(catalogChatClientResolver, never()).resolve(anyString(), anyString(), anyInt());
    }

    @Test
    void testNullStateFails() {
        assertThatThrownBy(() -> resolver.resolve(null)).isInstanceOf(ChatModelUnavailableException.class);
    }

    private AiHubChatClientResolver newResolver(String defaultProvider) {
        return new AiHubChatClientResolver(new CopilotChatClientResolver(catalogChatClientResolver, defaultProvider));
    }

    private static State newState() {
        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, WORKSPACE_ID);
        state.set(AiHubStateKeys.VERIFIED_ENVIRONMENT_ID, ENVIRONMENT_ID);

        return state;
    }

    private static State selectedState() {
        State state = newState();

        state.set(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, OPENAI_KEY);
        state.set(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY, OPENAI_MODEL);

        return state;
    }
}
