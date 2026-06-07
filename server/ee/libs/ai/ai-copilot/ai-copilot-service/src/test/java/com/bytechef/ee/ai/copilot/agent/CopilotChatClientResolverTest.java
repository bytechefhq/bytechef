/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import java.util.List;
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

    private static final long WORKSPACE_ID = 42L;
    private static final long OPENAI_PROVIDER_ID = 100L;
    private static final String OPENAI_MODEL = "gpt-4o";

    private AiGatewayChatModelFactory aiGatewayChatModelFactory;

    private AiGatewayProvider openAiProvider;

    private CopilotChatClientResolver resolver;

    @BeforeEach
    void setUp() {
        WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService = mock(
            WorkspaceAiGatewayProviderService.class);
        AiGatewayProviderService aiGatewayProviderService = mock(AiGatewayProviderService.class);
        aiGatewayChatModelFactory = mock(AiGatewayChatModelFactory.class);

        openAiProvider = mock(AiGatewayProvider.class);

        when(openAiProvider.getId()).thenReturn(OPENAI_PROVIDER_ID);
        when(openAiProvider.getType()).thenReturn(AiGatewayProviderType.OPENAI);
        when(openAiProvider.isEnabled()).thenReturn(true);

        WorkspaceAiGatewayProvider workspaceOpenAi = mock(WorkspaceAiGatewayProvider.class);

        when(workspaceOpenAi.getProviderId()).thenReturn(OPENAI_PROVIDER_ID);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(WORKSPACE_ID))
            .thenReturn(List.of(workspaceOpenAi));

        when(aiGatewayProviderService.getProvider(OPENAI_PROVIDER_ID)).thenReturn(openAiProvider);

        ChatModel openAiChatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(openAiProvider)).thenReturn(openAiChatModel);

        resolver = new CopilotChatClientResolver(
            workspaceAiGatewayProviderService, aiGatewayProviderService, aiGatewayChatModelFactory);
    }

    @Test
    void testUserSelectedResolvesUserSelected() {
        State state = newStateWithWorkspace();

        state.set(CopilotChatClientResolver.USER_SELECTED_LLM_PROVIDER_KEY, "OPENAI");
        state.set(CopilotChatClientResolver.USER_SELECTED_LLM_MODEL_KEY, OPENAI_MODEL);

        ChatClient client = resolver.resolve(state);

        assertNotNull(client);
        verify(aiGatewayChatModelFactory).getChatModel(openAiProvider);
    }

    @Test
    void testNoUserSelectedReturnsNull() {
        // No user override → resolver returns null and the caller uses its workspace @Primary ChatModel.
        State state = newStateWithWorkspace();

        assertNull(resolver.resolve(state));
        verify(aiGatewayChatModelFactory, never()).getChatModel(openAiProvider);
    }

    @Test
    void testHalfSetUserStateReturnsNull() {
        // Transient client state (e.g. user mid-picking): one half arrived, the other didn't. Copilot has no
        // secondary layer to fall through to, so the resolver returns null with a warning and the caller uses the
        // workspace default.
        State state = newStateWithWorkspace();

        state.set(CopilotChatClientResolver.USER_SELECTED_LLM_PROVIDER_KEY, "OPENAI");

        assertNull(resolver.resolve(state));
        verify(aiGatewayChatModelFactory, never()).getChatModel(openAiProvider);
    }

    @Test
    void testUnknownUserProviderReturnsNull() {
        // User picked a provider the workspace doesn't have enabled (admin disabled it mid-session, or stale client
        // catalog). Resolver returns null + warn-logs; caller uses workspace default.
        State state = newStateWithWorkspace();

        state.set(CopilotChatClientResolver.USER_SELECTED_LLM_PROVIDER_KEY, "GROQ");
        state.set(CopilotChatClientResolver.USER_SELECTED_LLM_MODEL_KEY, "llama-3");

        assertNull(resolver.resolve(state));
        verify(aiGatewayChatModelFactory, never()).getChatModel(openAiProvider);
    }

    @Test
    void testMissingWorkspaceIdReturnsNull() {
        // Defensive: user override is set but workspaceId is missing from state. Resolver returns null instead of
        // throwing or hitting the DB with null. Caller uses workspace default.
        State state = new State();

        state.set(CopilotChatClientResolver.USER_SELECTED_LLM_PROVIDER_KEY, "OPENAI");
        state.set(CopilotChatClientResolver.USER_SELECTED_LLM_MODEL_KEY, OPENAI_MODEL);

        assertNull(resolver.resolve(state));
    }

    @Test
    void testNullStateReturnsNull() {
        assertNull(resolver.resolve(null));
    }

    private static State newStateWithWorkspace() {
        State state = new State();

        state.set(CopilotChatClientResolver.WORKSPACE_ID_KEY, WORKSPACE_ID);

        return state;
    }
}
