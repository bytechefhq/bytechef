/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

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
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AutomationIntelligentToolContributorConfigurationTest {

    private final AutomationIntelligentToolContributorConfiguration configuration =
        new AutomationIntelligentToolContributorConfiguration();

    @Test
    void testContributesTwoDefinitionsWithExpectedNamesAndAgentTypeKeys() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        assertThat(definitions).hasSize(2);

        assertThat(definitions).extracting(IntelligentToolDefinition::name)
            .containsExactly("buildCustomComponent", "buildCodeWorkflow");

        assertThat(definitionNamed(definitions, "buildCustomComponent").agentTypeKey())
            .isEqualTo(CopilotAgentType.BUILD_CUSTOM_COMPONENT.key());
        assertThat(definitionNamed(definitions, "buildCodeWorkflow").agentTypeKey())
            .isEqualTo(CopilotAgentType.BUILD_CODE_WORKFLOW.key());
    }

    @Test
    void testPanelScopesAreEmptyForBothAgents() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        for (IntelligentToolDefinition definition : definitions) {
            assertThat(definition.panelScopes()).isEmpty();
        }
    }

    @Test
    void testChatClientFactoryIsPresentForBothVariantsWhenBeansPresent() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        for (IntelligentToolDefinition definition : definitions) {
            assertThat(definition.chatClientFactory(IntelligentToolVariant.ASK)).isNotNull();
            assertThat(definition.chatClientFactory(IntelligentToolVariant.BUILD)).isNotNull();
        }
    }

    @Test
    void testChatClientFactoryIsNullWhenProviderHasNoBean() {
        IntelligentToolContributor contributor = configuration.automationIntelligentToolContributor(
            emptyProvider(), present(mock(IntelligentToolChatClientFactory.class)), emptyProvider(),
            present(mock(IntelligentToolChatClientFactory.class)), present(mock(SubAgentChatModelResolver.class)));

        List<IntelligentToolDefinition> definitions = contributor.getIntelligentToolDefinitions();

        IntelligentToolDefinition customComponentDefinition = definitionNamed(definitions, "buildCustomComponent");

        assertThat(customComponentDefinition.chatClientFactory(IntelligentToolVariant.ASK)).isNull();
        assertThat(customComponentDefinition.chatClientFactory(IntelligentToolVariant.BUILD)).isNotNull();

        IntelligentToolDefinition codeWorkflowDefinition = definitionNamed(definitions, "buildCodeWorkflow");

        assertThat(codeWorkflowDefinition.chatClientFactory(IntelligentToolVariant.ASK)).isNull();
        assertThat(codeWorkflowDefinition.chatClientFactory(IntelligentToolVariant.BUILD)).isNotNull();
    }

    @Test
    void testCustomComponentCreateBuildsCustomComponentAgentToolCallback() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        IntelligentToolDefinition customComponentDefinition = definitionNamed(definitions, "buildCustomComponent");

        assertThat(customComponentDefinition.create(chatModel -> mock(ChatClient.class))
            .getToolDefinition()
            .name()).isEqualTo("buildCustomComponent");
    }

    @Test
    void testCodeWorkflowCreateBuildsCodeWorkflowAgentToolCallback() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        IntelligentToolDefinition codeWorkflowDefinition = definitionNamed(definitions, "buildCodeWorkflow");

        assertThat(codeWorkflowDefinition.create(chatModel -> mock(ChatClient.class))
            .getToolDefinition()
            .name()).isEqualTo("buildCodeWorkflow");
    }

    private List<IntelligentToolDefinition> definitionsWithAllProvidersPresent() {
        IntelligentToolContributor contributor = configuration.automationIntelligentToolContributor(
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)), present(mock(SubAgentChatModelResolver.class)));

        return contributor.getIntelligentToolDefinitions();
    }

    private static IntelligentToolDefinition definitionNamed(
        List<IntelligentToolDefinition> definitions, String name) {

        return definitions.stream()
            .filter(definition -> definition.name()
                .equals(name))
            .findFirst()
            .orElseThrow();
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
