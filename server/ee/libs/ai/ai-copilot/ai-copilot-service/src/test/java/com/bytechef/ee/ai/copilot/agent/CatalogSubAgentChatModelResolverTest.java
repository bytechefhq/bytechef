/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CatalogSubAgentChatModelResolverTest {

    private CatalogChatClientResolver catalogChatClientResolver;

    private CatalogSubAgentChatModelResolver resolver;

    @BeforeEach
    void setUp() {
        catalogChatClientResolver = mock(CatalogChatClientResolver.class);

        resolver = new CatalogSubAgentChatModelResolver(catalogChatClientResolver);
    }

    @Test
    void testResolvesThePickedModel() {
        ChatModel chatModel = mock(ChatModel.class);

        when(catalogChatClientResolver.resolveChatModel("anthropic", "claude-opus-4", 1))
            .thenReturn(chatModel);

        Map<String, Object> toolContext = Map.of(
            AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY, "anthropic",
            AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY, "claude-opus-4",
            AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);

        assertThat(resolver.resolve(toolContext)).isSameAs(chatModel);
    }

    @Test
    void testReturnsNullWhenNoPickIsPresent() {
        assertThat(resolver.resolve(Map.of())).isNull();

        verifyNoInteractions(catalogChatClientResolver);
    }

    @Test
    void testReturnsNullWhenOnlyTheProviderIsPresent() {
        Map<String, Object> toolContext = Map.of(
            AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY, "anthropic",
            AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);

        assertThat(resolver.resolve(toolContext)).isNull();

        verifyNoInteractions(catalogChatClientResolver);
    }

    @Test
    void testReturnsNullWhenTheEnvironmentIsMissing() {
        Map<String, Object> toolContext = Map.of(
            AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY, "anthropic",
            AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY, "claude-opus-4");

        assertThat(resolver.resolve(toolContext)).isNull();

        verifyNoInteractions(catalogChatClientResolver);
    }

    @Test
    void testReturnsNullWhenTheCatalogCannotResolveThePair() {
        when(catalogChatClientResolver.resolveChatModel(anyString(), anyString(), anyInt()))
            .thenReturn(null);

        assertThat(resolver.resolve(fullToolContext())).isNull();
    }

    @Test
    void testReturnsNullWhenTheCatalogThrows() {
        when(catalogChatClientResolver.resolveChatModel(anyString(), anyString(), anyInt()))
            .thenThrow(new IllegalStateException("provider exploded"));

        assertThat(resolver.resolve(fullToolContext())).isNull();
    }

    private static Map<String, Object> fullToolContext() {
        return Map.of(
            AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY, "anthropic",
            AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY, "claude-opus-4",
            AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);
    }
}
