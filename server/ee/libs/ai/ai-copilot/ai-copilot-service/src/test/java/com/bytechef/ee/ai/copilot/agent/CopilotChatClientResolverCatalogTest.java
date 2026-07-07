/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class CopilotChatClientResolverCatalogTest {

    @Test
    void testCatalogSelectionResolved() {
        CatalogChatClientResolver catalogChatClientResolver = mock(CatalogChatClientResolver.class);
        ChatClient catalogChatClient = mock(ChatClient.class);

        when(catalogChatClientResolver.resolve("ai.provider.openAi", "gpt-4o", 3)).thenReturn(catalogChatClient);

        CopilotChatClientResolver resolver = new CopilotChatClientResolver(catalogChatClientResolver);

        State state = new State();

        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER, "ai.provider.openAi");
        state.set(CopilotConstants.STATE_USER_SELECTED_LLM_MODEL, "gpt-4o");
        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, "3");

        assertThat(resolver.resolve(state)).isSameAs(catalogChatClient);
    }
}
