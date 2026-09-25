/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.agent.SelectedChatModelUnavailableException;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CopilotChatClientResolverTest {

    private static final String OPENAI_KEY = "ai.provider.openAi";
    private static final String OPENAI_MODEL = "gpt-4o";

    private CatalogChatClientResolver catalogChatClientResolver;

    private CopilotChatClientResolver resolver;

    @BeforeEach
    void setUp() {
        catalogChatClientResolver = mock(CatalogChatClientResolver.class);

        resolver = new CopilotChatClientResolver(catalogChatClientResolver, "");
    }

    @Test
    void testUserSelectedWithEnvironmentDelegatesToCatalog() {
        ChatClient catalogChatClient = mock(ChatClient.class);

        when(catalogChatClientResolver.resolve(OPENAI_KEY, OPENAI_MODEL, 3)).thenReturn(catalogChatClient);

        State state = new State();

        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER, OPENAI_KEY);
        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_MODEL, OPENAI_MODEL);
        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, "3");

        assertSame(catalogChatClient, resolver.resolve(state));
    }

    @Test
    void testUnresolvableUserSelectionFailsInsteadOfFallingBack() {
        when(catalogChatClientResolver.resolve(OPENAI_KEY, OPENAI_MODEL, 3)).thenReturn(null);

        State state = new State();

        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER, OPENAI_KEY);
        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_MODEL, OPENAI_MODEL);
        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, "3");

        SelectedChatModelUnavailableException exception = assertThrows(
            SelectedChatModelUnavailableException.class, () -> resolver.resolve(state));

        assertTrue(exception.getMessage()
            .contains(OPENAI_MODEL));
        verify(catalogChatClientResolver, never()).resolveDefault(anyInt());
    }

    @Test
    void testFailingCatalogLookupOfTheUserSelectionFailsInsteadOfFallingBack() {
        IllegalStateException cause = new IllegalStateException("cannot decrypt the API key");

        when(catalogChatClientResolver.resolve(OPENAI_KEY, OPENAI_MODEL, 3)).thenThrow(cause);

        State state = new State();

        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER, OPENAI_KEY);
        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_MODEL, OPENAI_MODEL);
        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, "3");

        SelectedChatModelUnavailableException exception = assertThrows(
            SelectedChatModelUnavailableException.class, () -> resolver.resolve(state));

        assertSame(cause, exception.getCause());
        verify(catalogChatClientResolver, never()).resolveDefault(anyInt());
    }

    @Test
    void testUserSelectedWithoutEnvironmentFails() {
        State state = new State();

        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER, OPENAI_KEY);
        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_MODEL, OPENAI_MODEL);

        assertThrows(ChatModelUnavailableException.class, () -> resolver.resolve(state));
        verify(catalogChatClientResolver, never()).resolve(anyString(), anyString(), anyInt());
    }

    @Test
    void testNullStateFails() {
        assertThrows(ChatModelUnavailableException.class, () -> resolver.resolve(null));
    }

    @Test
    void testHalfSetUserSelectionFails() {
        State state = new State();

        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER, OPENAI_KEY);
        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, "1");

        ChatModelUnavailableException exception = assertThrows(
            ChatModelUnavailableException.class, () -> resolver.resolve(state));

        assertTrue(exception.getMessage()
            .contains("incomplete"));
        verify(catalogChatClientResolver, never()).resolveDefault(anyInt());
    }

    @Test
    void testNoEnabledProviderFailsInsteadOfUsingTheStartupModel() {
        when(catalogChatClientResolver.resolveDefault(1)).thenReturn(null);

        State state = new State();

        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, "1");

        ChatModelUnavailableException exception = assertThrows(
            ChatModelUnavailableException.class, () -> resolver.resolve(state));

        assertTrue(exception.getMessage()
            .contains("No AI provider is enabled in environment 1"));
    }

    @Test
    void testFailingCatalogDefaultLookupFails() {
        IllegalStateException cause = new IllegalStateException("catalog down");

        when(catalogChatClientResolver.resolveDefault(1)).thenThrow(cause);

        State state = new State();

        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, "1");

        ChatModelUnavailableException exception = assertThrows(
            ChatModelUnavailableException.class, () -> resolver.resolve(state));

        assertSame(cause, exception.getCause());
    }

    @Test
    void testResolveDefaultChatModelUsesThePreferredProvider() {
        ChatModel chatModel = mock(ChatModel.class);

        when(catalogChatClientResolver.resolveDefaultChatModel("anthropic", 1)).thenReturn(chatModel);

        CopilotChatClientResolver preferringResolver =
            new CopilotChatClientResolver(catalogChatClientResolver, "anthropic");

        assertSame(chatModel, preferringResolver.resolveDefaultChatModel(1));
    }

    @Test
    void testResolveDefaultChatModelFailsWhenNothingIsResolvable() {
        assertThrows(ChatModelUnavailableException.class, () -> resolver.resolveDefaultChatModel(1));
    }

    @Test
    void testDefaultProviderPreferredWhenNoUserSelection() {
        ChatClient preferredChatClient = mock(ChatClient.class);

        when(catalogChatClientResolver.resolvePreferred("anthropic", 3)).thenReturn(preferredChatClient);

        CopilotChatClientResolver preferringResolver =
            new CopilotChatClientResolver(catalogChatClientResolver, "anthropic");

        State state = new State();

        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, "3");

        assertSame(preferredChatClient, preferringResolver.resolve(state));

        verify(catalogChatClientResolver, never()).resolveDefault(anyInt());
    }

    @Test
    void testDefaultProviderFallsBackWhenNotResolvable() {
        ChatClient defaultChatClient = mock(ChatClient.class);

        when(catalogChatClientResolver.resolvePreferred("anthropic", 3)).thenReturn(null);
        when(catalogChatClientResolver.resolveDefault(3)).thenReturn(defaultChatClient);

        CopilotChatClientResolver preferringResolver =
            new CopilotChatClientResolver(catalogChatClientResolver, "anthropic");

        State state = new State();

        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, "3");

        assertSame(defaultChatClient, preferringResolver.resolve(state));
    }

    @Test
    void testNoDefaultProviderUsesEnvironmentDefault() {
        ChatClient defaultChatClient = mock(ChatClient.class);

        when(catalogChatClientResolver.resolveDefault(3)).thenReturn(defaultChatClient);

        State state = new State();

        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, "3");

        assertSame(defaultChatClient, resolver.resolve(state));

        verify(catalogChatClientResolver, never()).resolvePreferred(anyString(), anyInt());
    }
}
