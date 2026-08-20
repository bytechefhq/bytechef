/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class EmbeddedIntelligentToolContributorConfigurationTest {

    private final EmbeddedIntelligentToolContributorConfiguration configuration =
        new EmbeddedIntelligentToolContributorConfiguration();

    @Test
    void testContributesOneDefinitionWithExpectedNameAndAgentTypeKey() {
        List<IntelligentToolDefinition> definitions = definitionsWithProviderPresent();

        assertThat(definitions).hasSize(1);

        IntelligentToolDefinition definition = definitions.get(0);

        assertThat(definition.name()).isEqualTo("buildIntegrationWorkflow");
        assertThat(definition.agentTypeKey()).isEqualTo(CopilotAgentType.BUILD_INTEGRATION_WORKFLOW.key());
        assertThat(definition.panelScopes()).isEmpty();
    }

    @Test
    void testChatClientFactoryIsNullForAskButPresentForBuild() {
        List<IntelligentToolDefinition> definitions = definitionsWithProviderPresent();

        IntelligentToolDefinition definition = definitions.get(0);

        assertThat(definition.chatClientFactory(IntelligentToolVariant.ASK)).isNull();
        assertThat(definition.chatClientFactory(IntelligentToolVariant.BUILD)).isNotNull();
    }

    @Test
    void testChatClientFactoryIsNullForBuildWhenProviderHasNoBean() {
        IntelligentToolContributor contributor =
            configuration.embeddedIntelligentToolContributor(emptyProvider(), emptyProvider());

        List<IntelligentToolDefinition> definitions = contributor.getIntelligentToolDefinitions();

        IntelligentToolDefinition definition = definitions.get(0);

        assertThat(definition.chatClientFactory(IntelligentToolVariant.BUILD)).isNull();
    }

    @Test
    void testCreateBuildsProjectWorkflowAgentToolCallbackWithTheEmbeddedToolName() {
        List<IntelligentToolDefinition> definitions = definitionsWithProviderPresent();

        IntelligentToolDefinition definition = definitions.get(0);

        ToolCallback toolCallback = definition.create(chatModel -> mock(ChatClient.class));

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("buildIntegrationWorkflow");
    }

    @Test
    void testCreateBuildsProjectWorkflowAgentToolCallbackWithTheEmbeddedDescription() {
        List<IntelligentToolDefinition> definitions = definitionsWithProviderPresent();

        IntelligentToolDefinition definition = definitions.get(0);

        ToolCallback toolCallback = definition.create(chatModel -> mock(ChatClient.class));

        String description = toolCallback.getToolDefinition()
            .description();

        // DESCRIPTION is LLM-facing routing text that this branch MOVED between classes; pinning both the exact
        // constant and its distinguishing phrases catches a botched move, not just a missing description.
        assertThat(description).isEqualTo(EmbeddedIntelligentToolContributorConfiguration.DESCRIPTION);
        assertThat(description).contains("buildWorkflow");
        assertThat(description).contains("embedded INTEGRATION workflows");
    }

    private List<IntelligentToolDefinition> definitionsWithProviderPresent() {
        IntelligentToolContributor contributor = configuration.embeddedIntelligentToolContributor(
            present(mock(IntelligentToolChatClientFactory.class)), present(mock(SubAgentChatModelResolver.class)));

        return contributor.getIntelligentToolDefinitions();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> present(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(value);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
