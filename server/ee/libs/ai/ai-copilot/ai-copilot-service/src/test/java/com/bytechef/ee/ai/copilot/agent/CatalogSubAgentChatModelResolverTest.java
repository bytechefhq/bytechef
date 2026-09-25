/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.agent.SelectedChatModelUnavailableException;
import com.bytechef.ai.copilot.tool.catalog.SubAgentModelRequest;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @version ee
 * @author Ivica Cardic
 */
class CatalogSubAgentChatModelResolverTest {

    private CatalogChatClientResolver catalogChatClientResolver;

    private CatalogSubAgentChatModelResolver resolver;

    @BeforeEach
    void setUp() {
        catalogChatClientResolver = mock(CatalogChatClientResolver.class);

        resolver = new CatalogSubAgentChatModelResolver(catalogChatClientResolver, "anthropic");
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

        assertThat(resolver.resolve(SubAgentModelRequest.fromToolContext(toolContext))).isSameAs(chatModel);
    }

    @Test
    void testResolvesTheEnvironmentDefaultWhenNoPickIsPresent() {
        ChatModel chatModel = mock(ChatModel.class);

        when(catalogChatClientResolver.resolveDefaultChatModel("anthropic", 1)).thenReturn(chatModel);

        Map<String, Object> toolContext = Map.of(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);

        assertThat(resolver.resolve(SubAgentModelRequest.fromToolContext(toolContext))).isSameAs(chatModel);
    }

    @Test
    void testThrowsWhenNoPickIsPresentAndNoProviderIsEnabled() {
        Map<String, Object> toolContext = Map.of(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);

        assertThatThrownBy(() -> resolver.resolve(SubAgentModelRequest.fromToolContext(toolContext)))
            .isInstanceOf(ChatModelUnavailableException.class)
            .hasMessageContaining("No AI provider is enabled in environment 1");
    }

    @Test
    void testThrowsWhenOnlyTheProviderIsPresent() {
        Map<String, Object> toolContext = Map.of(
            AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY, "anthropic",
            AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);

        assertThatThrownBy(() -> resolver.resolve(SubAgentModelRequest.fromToolContext(toolContext)))
            .isInstanceOf(ChatModelUnavailableException.class)
            .hasMessageContaining("incomplete");

        verifyNoInteractions(catalogChatClientResolver);
    }

    @Test
    void testThrowsWhenTheEnvironmentIsMissing() {
        Map<String, Object> toolContext = Map.of(
            AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY, "anthropic",
            AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY, "claude-opus-4");

        assertThatThrownBy(() -> resolver.resolve(SubAgentModelRequest.fromToolContext(toolContext)))
            .isInstanceOf(ChatModelUnavailableException.class);

        verifyNoInteractions(catalogChatClientResolver);
    }

    @Test
    void testThrowsWhenTheCatalogCannotResolveThePair() {
        when(catalogChatClientResolver.resolveChatModel(anyString(), anyString(), anyInt()))
            .thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve(SubAgentModelRequest.fromToolContext(fullToolContext())))
            .isInstanceOf(SelectedChatModelUnavailableException.class)
            .hasMessageContaining("claude-opus-4");
    }

    @Test
    void testThrowsWhenTheCatalogThrows() {
        IllegalStateException cause = new IllegalStateException("provider exploded");

        when(catalogChatClientResolver.resolveChatModel(anyString(), anyString(), anyInt()))
            .thenThrow(cause);

        assertThatThrownBy(() -> resolver.resolve(SubAgentModelRequest.fromToolContext(fullToolContext())))
            .isInstanceOf(SelectedChatModelUnavailableException.class)
            .hasCause(cause);
    }

    private static Map<String, Object> fullToolContext() {
        return Map.of(
            AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY, "anthropic",
            AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY, "claude-opus-4",
            AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);
    }
}
