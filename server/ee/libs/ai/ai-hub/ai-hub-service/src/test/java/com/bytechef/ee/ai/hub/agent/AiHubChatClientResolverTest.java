/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Pins the precedence rules for {@link AiHubChatClientResolver#resolve(State)} against the catalog resolver:
 *
 * <pre>
 *   user-selected (USER_SELECTED_LLM_*_KEY)        ← highest
 *     ↓ absent
 *   null (caller falls back to workspace @Primary ChatModel)   ← lowest
 * </pre>
 *
 * Half-set states fall back to the workspace default. The resolver never throws — every defensive branch returns null
 * so the conversation continues on the workspace default. The AI Gateway fallback path is disabled for now (see the
 * commented block in the resolver).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatClientResolverTest {

    private static final int ENVIRONMENT_ID = 1;
    private static final long WORKSPACE_ID = 42L;
    private static final String OPENAI_KEY = "ai.provider.openAi";
    private static final String OPENAI_MODEL = "gpt-4o";

    private CatalogChatClientResolver catalogChatClientResolver;

    private ChatClient openAiChatClient;

    private AiHubChatClientResolver resolver;

    @BeforeEach
    void setUp() {
        catalogChatClientResolver = mock(CatalogChatClientResolver.class);

        openAiChatClient = mock(ChatClient.class);

        resolver = new AiHubChatClientResolver(catalogChatClientResolver);
    }

    @Test
    void testUserSelectedResolvesViaCatalog() {
        when(catalogChatClientResolver.resolve(OPENAI_KEY, OPENAI_MODEL, ENVIRONMENT_ID)).thenReturn(openAiChatClient);

        State state = newState();

        state.set(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, OPENAI_KEY);
        state.set(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY, OPENAI_MODEL);

        assertSame(openAiChatClient, resolver.resolve(state));
    }

    @Test
    void testHalfSetUserStateReturnsNull() {
        // Client sent provider but not model (transient state during user picking). There is no lower override tier,
        // so the resolver returns null and the caller falls back to the workspace default.
        State state = newState();

        state.set(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, OPENAI_KEY);

        assertNull(resolver.resolve(state));
        verify(catalogChatClientResolver, never()).resolve(anyString(), anyString(), anyInt());
    }

    @Test
    void testNoOverridesReturnsNull() {
        // No state keys set → resolver returns null so the caller falls back to the workspace @Primary ChatModel.
        assertNull(resolver.resolve(newState()));
        verify(catalogChatClientResolver, never()).resolve(anyString(), anyString(), anyInt());
    }

    @Test
    void testUserSelectedWithoutEnvironmentReturnsNull() {
        // No environment id → the catalog API key can't be resolved, so the resolver returns null without delegating.
        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, WORKSPACE_ID);
        state.set(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, OPENAI_KEY);
        state.set(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY, OPENAI_MODEL);

        assertNull(resolver.resolve(state));
        verify(catalogChatClientResolver, never()).resolve(anyString(), anyString(), anyInt());
    }

    @Test
    void testMissingWorkspaceIdReturnsNull() {
        // Defensive: the controller normally injects VERIFIED_WORKSPACE_ID, but if it didn't, the resolver returns
        // null instead of NPE-ing.
        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_ENVIRONMENT_ID, ENVIRONMENT_ID);
        state.set(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, OPENAI_KEY);
        state.set(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY, OPENAI_MODEL);

        assertNull(resolver.resolve(state));
    }

    @Test
    void testNullStateReturnsNull() {
        assertNull(resolver.resolve(null));
    }

    private static State newState() {
        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, WORKSPACE_ID);
        state.set(AiHubStateKeys.VERIFIED_ENVIRONMENT_ID, ENVIRONMENT_ID);

        return state;
    }
}
