/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.configuration.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.ee.platform.configuration.facade.AiProviderFacade;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CatalogChatModelTest {

    private static final String ANTHROPIC_MODEL = "claude-sonnet-4-5";
    private static final AiDefaultModelWithApiKeyDTO ANTHROPIC_DEFAULT_MODEL =
        new AiDefaultModelWithApiKeyDTO(Provider.ANTHROPIC, ANTHROPIC_MODEL, "sk-anthropic");

    private final AiProviderFacade aiProviderFacade = mock(AiProviderFacade.class);
    private final CatalogChatModelFactory catalogChatModelFactory = mock(CatalogChatModelFactory.class);

    private final CatalogChatModel catalogChatModel = new CatalogChatModel(
        aiProviderFacade, catalogChatModelFactory);

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
    }

    @Test
    void testDelegatesCallToDefaultProviderWhenModelResolved() {
        ChatModel delegate = mock(ChatModel.class);
        Prompt prompt = new Prompt("hello");
        ChatResponse response = mock(ChatResponse.class);

        when(delegate.call(prompt)).thenReturn(response);
        when(aiProviderFacade.getAiDefaultChatModelApiKey(Environment.PRODUCTION.ordinal()))
            .thenReturn(ANTHROPIC_DEFAULT_MODEL);
        when(catalogChatModelFactory.createChatModel(Provider.ANTHROPIC, ANTHROPIC_MODEL, "sk-anthropic"))
            .thenReturn(delegate);

        assertThat(catalogChatModel.call(prompt)).isSameAs(response);
    }

    @Test
    void testThrowsActionableErrorWhenNoProviderActivated() {
        when(aiProviderFacade.getAiDefaultChatModelApiKey(Environment.PRODUCTION.ordinal()))
            .thenReturn(null);

        assertThatThrownBy(() -> catalogChatModel.call(new Prompt("hello")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No chat provider is activated")
            .hasMessageContaining("PRODUCTION")
            .hasMessageContaining("bytechef.ai.provider.anthropic.api-key");
    }

    @Test
    void testReadsEnvironmentFromContext() {
        ChatModel delegate = mock(ChatModel.class);
        Prompt prompt = new Prompt("hello");

        EnvironmentContext.set(Environment.STAGING);

        when(delegate.call(prompt)).thenReturn(mock(ChatResponse.class));
        when(aiProviderFacade.getAiDefaultChatModelApiKey(Environment.STAGING.ordinal()))
            .thenReturn(ANTHROPIC_DEFAULT_MODEL);
        when(catalogChatModelFactory.createChatModel(Provider.ANTHROPIC, ANTHROPIC_MODEL, "sk-anthropic"))
            .thenReturn(delegate);

        catalogChatModel.call(prompt);

        verify(aiProviderFacade).getAiDefaultChatModelApiKey(Environment.STAGING.ordinal());
    }

    @Test
    void testCachesDelegatePerEnvironmentAndKey() {
        ChatModel delegate = mock(ChatModel.class);

        when(delegate.call(org.mockito.ArgumentMatchers.any(Prompt.class)))
            .thenReturn(mock(ChatResponse.class));
        when(aiProviderFacade.getAiDefaultChatModelApiKey(Environment.PRODUCTION.ordinal()))
            .thenReturn(ANTHROPIC_DEFAULT_MODEL);
        when(catalogChatModelFactory.createChatModel(Provider.ANTHROPIC, ANTHROPIC_MODEL, "sk-anthropic"))
            .thenReturn(delegate);

        catalogChatModel.call(new Prompt("a"));
        catalogChatModel.call(new Prompt("b"));

        verify(catalogChatModelFactory, times(1))
            .createChatModel(Provider.ANTHROPIC, ANTHROPIC_MODEL, "sk-anthropic");
    }
}
