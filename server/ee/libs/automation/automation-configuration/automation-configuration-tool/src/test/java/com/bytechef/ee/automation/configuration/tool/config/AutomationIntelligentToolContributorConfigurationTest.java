/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.tool.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.ee.automation.configuration.tool.CodeWorkflowAgentType;
import com.bytechef.ee.automation.configuration.tool.CodeWorkflowIntelligentToolContributorConfiguration;
import com.bytechef.ee.platform.customcomponent.tool.CustomComponentAgentType;
import com.bytechef.ee.platform.customcomponent.tool.CustomComponentIntelligentToolContributorConfiguration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 * @author Ivica Cardic
 */
final class AutomationIntelligentToolContributorConfigurationTest {

    @Test
    void testContributesTheBuildAndReadOnlyDefinitionsWithExpectedNamesAndAgentTypeKeys() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        assertThat(definitions).extracting(IntelligentToolDefinition::name)
            .containsExactly("buildCustomComponent", "buildCodeWorkflow", "explainCustomComponent",
                "explainCodeWorkflow");

        assertThat(definitionNamed(definitions, "explainCustomComponent").agentType())
            .isEqualTo(CustomComponentAgentType.EXPLAIN_CUSTOM_COMPONENT);
        assertThat(definitionNamed(definitions, "explainCodeWorkflow").agentType())
            .isEqualTo(CodeWorkflowAgentType.EXPLAIN_CODE_WORKFLOW);

        assertThat(definitionNamed(definitions, "buildCustomComponent").agentType())
            .isEqualTo(CustomComponentAgentType.BUILD_CUSTOM_COMPONENT);
        assertThat(definitionNamed(definitions, "buildCodeWorkflow").agentType())
            .isEqualTo(CodeWorkflowAgentType.BUILD_CODE_WORKFLOW);
    }

    @Test
    void testPanelScopesAreEmptyForBothAgents() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        for (IntelligentToolDefinition definition : definitions) {
            assertThat(definition.panelScopes()).isEmpty();
        }
    }

    @Test
    void testChatClientFactoryIsPresentForBothVariantsOfTheBuildDefinitionsWhenBeansPresent() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        for (String name : List.of("buildCustomComponent", "buildCodeWorkflow")) {
            IntelligentToolDefinition definition = definitionNamed(definitions, name);

            assertThat(definition.chatClientFactory(IntelligentToolVariant.ASK)).isNotNull();
            assertThat(definition.chatClientFactory(IntelligentToolVariant.BUILD)).isNotNull();
        }
    }

    @Test
    void testReadOnlyDefinitionsRouteOnlyToTheAskSubAgents() {
        IntelligentToolChatClientFactory customComponentAskFactory = mock(IntelligentToolChatClientFactory.class);
        IntelligentToolChatClientFactory codeWorkflowAskFactory = mock(IntelligentToolChatClientFactory.class);

        IntelligentToolContributor contributor = automationIntelligentToolContributor(
            present(customComponentAskFactory), present(mock(IntelligentToolChatClientFactory.class)),
            present(codeWorkflowAskFactory), present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(SubAgentChatModelResolver.class)));

        List<IntelligentToolDefinition> definitions = contributor.getIntelligentToolDefinitions();

        IntelligentToolDefinition explainCustomComponent = definitionNamed(definitions, "explainCustomComponent");

        assertThat(explainCustomComponent.chatClientFactory(IntelligentToolVariant.ASK))
            .isSameAs(customComponentAskFactory);
        assertThat(explainCustomComponent.chatClientFactory(IntelligentToolVariant.BUILD)).isNull();

        IntelligentToolDefinition explainCodeWorkflow = definitionNamed(definitions, "explainCodeWorkflow");

        assertThat(explainCodeWorkflow.chatClientFactory(IntelligentToolVariant.ASK)).isSameAs(codeWorkflowAskFactory);
        assertThat(explainCodeWorkflow.chatClientFactory(IntelligentToolVariant.BUILD)).isNull();
    }

    @Test
    void testReadOnlyDefinitionsCreateReadOnlyNamedToolCallbacks() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        assertThat(definitionNamed(definitions, "explainCustomComponent").create(chatModel -> mock(ChatClient.class))
            .getToolDefinition()
            .name()).isEqualTo("explainCustomComponent");
        assertThat(definitionNamed(definitions, "explainCodeWorkflow").create(chatModel -> mock(ChatClient.class))
            .getToolDefinition()
            .name()).isEqualTo("explainCodeWorkflow");
    }

    @Test
    void testChatClientFactoryIsNullWhenProviderHasNoBean() {
        IntelligentToolContributor contributor = automationIntelligentToolContributor(
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
        IntelligentToolContributor contributor = automationIntelligentToolContributor(
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

    private static IntelligentToolContributor automationIntelligentToolContributor(
        ObjectProvider<IntelligentToolChatClientFactory> customComponentAskFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> customComponentBuildFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> codeWorkflowAskFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> codeWorkflowBuildFactoryProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider) {

        IntelligentToolContributor contributor1 =
            new CustomComponentIntelligentToolContributorConfiguration().customComponentIntelligentToolContributor(
                customComponentAskFactoryProvider, customComponentBuildFactoryProvider, chatModelResolverProvider);
        IntelligentToolContributor contributor2 =
            new CodeWorkflowIntelligentToolContributorConfiguration().codeWorkflowIntelligentToolContributor(
                codeWorkflowAskFactoryProvider, codeWorkflowBuildFactoryProvider, chatModelResolverProvider);

        List<String> order = List.of(
            "buildCustomComponent", "buildCodeWorkflow", "explainCustomComponent", "explainCodeWorkflow");

        return () -> Stream.of(contributor1, contributor2)
            .flatMap(contributor -> contributor.getIntelligentToolDefinitions()
                .stream())
            .sorted(Comparator.comparingInt(definition -> order.indexOf(definition.name())))
            .toList();
    }
}
