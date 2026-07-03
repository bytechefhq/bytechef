/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Verifies that a user-selected catalog provider/model is resolved via {@link CatalogChatClientResolver}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiHubChatClientResolverCatalogTest {

    @Test
    void testCatalogSelectionResolved() {
        CatalogChatClientResolver catalogChatClientResolver = mock(CatalogChatClientResolver.class);
        ChatClient catalogChatClient = mock(ChatClient.class);

        when(catalogChatClientResolver.resolve("ai.provider.openAi", "gpt-4o", 1)).thenReturn(catalogChatClient);

        AiHubChatClientResolver resolver = new AiHubChatClientResolver(catalogChatClientResolver);

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 10L);
        state.set(AiHubStateKeys.VERIFIED_ENVIRONMENT_ID, 1);
        state.set(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, "ai.provider.openAi");
        state.set(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY, "gpt-4o");

        assertThat(resolver.resolve(state)).isSameAs(catalogChatClient);
    }
}
