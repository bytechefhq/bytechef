/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

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
    void testUserSelectedWithoutEnvironmentReturnsNull() {
        // No environment id → the catalog API key can't be resolved, so the resolver returns null without delegating.
        State state = new State();

        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER, OPENAI_KEY);
        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_MODEL, OPENAI_MODEL);

        assertNull(resolver.resolve(state));
        verify(catalogChatClientResolver, never()).resolve(anyString(), anyString(), anyInt());
    }

    @Test
    void testNoUserSelectedReturnsNull() {
        // No user override → resolver returns null and the caller uses its workspace @Primary ChatModel.
        assertNull(resolver.resolve(new State()));
    }

    @Test
    void testHalfSetUserStateReturnsNull() {
        // Transient client state (e.g. user mid-picking): one half arrived, the other didn't. Copilot has no
        // secondary layer to fall through to, so the resolver returns null with a warning.
        State state = new State();

        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER, OPENAI_KEY);

        assertNull(resolver.resolve(state));
    }

    @Test
    void testNullStateReturnsNull() {
        assertNull(resolver.resolve(null));
    }

    @Test
    void testDefaultProviderPreferredWhenNoUserSelection() {
        // bytechef.ai.copilot.provider is set and resolvable → Copilot uses it as the environment default instead of
        // the first-enabled-provider pick.
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
        // bytechef.ai.copilot.provider names a provider that is disabled or has no configured model in this
        // environment → Copilot falls back to the first enabled chat provider.
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
        // bytechef.ai.copilot.provider is blank → keep the existing first-enabled-provider behavior.
        ChatClient defaultChatClient = mock(ChatClient.class);

        when(catalogChatClientResolver.resolveDefault(3)).thenReturn(defaultChatClient);

        State state = new State();

        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, "3");

        assertSame(defaultChatClient, resolver.resolve(state));

        verify(catalogChatClientResolver, never()).resolvePreferred(anyString(), anyInt());
    }
}
