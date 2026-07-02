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
import com.bytechef.ee.ai.copilot.util.CopilotStateKeys;
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

        resolver = new CopilotChatClientResolver(catalogChatClientResolver);
    }

    @Test
    void testUserSelectedWithEnvironmentDelegatesToCatalog() {
        ChatClient catalogChatClient = mock(ChatClient.class);

        when(catalogChatClientResolver.resolve(OPENAI_KEY, OPENAI_MODEL, 3)).thenReturn(catalogChatClient);

        State state = new State();

        state.set(CopilotStateKeys.STATE_USER_SELECTED_LLM_PROVIDER, OPENAI_KEY);
        state.set(CopilotStateKeys.STATE_USER_SELECTED_LLM_MODEL, OPENAI_MODEL);
        state.set(CopilotStateKeys.STATE_ENVIRONMENT_ID, "3");

        assertSame(catalogChatClient, resolver.resolve(state));
    }

    @Test
    void testUserSelectedWithoutEnvironmentReturnsNull() {
        // No environment id → the catalog API key can't be resolved, so the resolver returns null without delegating.
        State state = new State();

        state.set(CopilotStateKeys.STATE_USER_SELECTED_LLM_PROVIDER, OPENAI_KEY);
        state.set(CopilotStateKeys.STATE_USER_SELECTED_LLM_MODEL, OPENAI_MODEL);

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

        state.set(CopilotStateKeys.STATE_USER_SELECTED_LLM_PROVIDER, OPENAI_KEY);

        assertNull(resolver.resolve(state));
    }

    @Test
    void testNullStateReturnsNull() {
        assertNull(resolver.resolve(null));
    }
}
