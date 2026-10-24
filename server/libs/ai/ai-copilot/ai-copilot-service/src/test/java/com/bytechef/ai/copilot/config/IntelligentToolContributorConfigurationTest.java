/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.tool.ask.SubAgentAskRelay;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.tool.AutomationConfigurationAgentType;
import com.bytechef.automation.configuration.tool.ClusterElementTools;
import com.bytechef.automation.configuration.tool.PersistedWorkflowChecker;
import com.bytechef.automation.configuration.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.configuration.tool.ProjectWorkflowTools;
import com.bytechef.automation.configuration.tool.ReadProjectTools;
import com.bytechef.automation.configuration.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.configuration.tool.ScriptTools;
import com.bytechef.automation.configuration.tool.WorkflowIntelligentToolContributorConfiguration;
import com.bytechef.automation.configuration.tool.model.ProjectWorkflowInfo;
import com.bytechef.automation.data.table.tool.DataTableTools;
import com.bytechef.automation.workflow.execution.tool.WorkflowExecutionAgentType;
import com.bytechef.automation.workflow.execution.tool.WorkflowExecutionIntelligentToolContributorConfiguration;
import com.bytechef.automation.workflow.execution.tool.WorkflowExecutionTools;
import com.bytechef.platform.ai.skill.tool.ReadSkillsTools;
import com.bytechef.platform.ai.skill.tool.SkillAgentType;
import com.bytechef.platform.ai.skill.tool.SkillsIntelligentToolContributorConfiguration;
import com.bytechef.platform.ai.skill.tool.SkillsTools;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.tool.ClusterElementIntelligentToolContributorConfiguration;
import com.bytechef.platform.component.tool.ComponentTools;
import com.bytechef.platform.component.tool.PlatformComponentAgentType;
import com.bytechef.platform.component.tool.PropertyOptionsResolver;
import com.bytechef.platform.component.tool.ScopedConnectionGuard;
import com.bytechef.platform.component.tool.TaskTools;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.tool.CodeEditorIntelligentToolContributorConfiguration;
import com.bytechef.platform.workflow.tool.PlatformWorkflowAgentType;
import com.bytechef.platform.workflow.tool.WorkflowInstructionTools;
import com.bytechef.platform.workflow.tool.WorkflowValidatorTools;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
final class IntelligentToolContributorConfigurationTest {

    private static final List<DelegateCase> DELEGATE_CASES = List.of(
        new DelegateCase("buildWorkflow", IntelligentToolVariant.ASK, "workflowEditorAsk"),
        new DelegateCase("buildWorkflow", IntelligentToolVariant.BUILD, "workflowEditorBuild"),
        new DelegateCase("importWorkflow", IntelligentToolVariant.BUILD, "converterBuild"),
        new DelegateCase("configureClusterElement", IntelligentToolVariant.ASK, "clusterElementAsk"),
        new DelegateCase("configureClusterElement", IntelligentToolVariant.BUILD, "clusterElementBuild"),
        new DelegateCase("writeScript", IntelligentToolVariant.ASK, "codeEditorAsk"),
        new DelegateCase("writeScript", IntelligentToolVariant.BUILD, "codeEditorBuild"),
        new DelegateCase("authorSkill", IntelligentToolVariant.ASK, "skillsAsk"),
        new DelegateCase("authorSkill", IntelligentToolVariant.BUILD, "skillsBuild"),
        new DelegateCase("debugWorkflowExecution", IntelligentToolVariant.BUILD, "workflowExecutionBuild"));

    @Test
    void testContributesSixDefinitionsWithExpectedNamesAndAgentTypeKeys() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        assertThat(definitions).hasSize(6);

        assertThat(definitions).extracting(IntelligentToolDefinition::name)
            .containsExactlyInAnyOrder(
                "buildWorkflow", "importWorkflow", "configureClusterElement", "writeScript",
                "authorSkill", "debugWorkflowExecution");

        assertThat(definitionNamed(definitions, "buildWorkflow").agentType())
            .isEqualTo(AutomationConfigurationAgentType.BUILD_WORKFLOW);
        assertThat(definitionNamed(definitions, "importWorkflow").agentType())
            .isEqualTo(AutomationConfigurationAgentType.IMPORT_WORKFLOW);
        assertThat(definitionNamed(definitions, "configureClusterElement").agentType())
            .isEqualTo(PlatformComponentAgentType.CONFIGURE_CLUSTER_ELEMENT);
        assertThat(definitionNamed(definitions, "writeScript").agentType())
            .isEqualTo(PlatformWorkflowAgentType.WRITE_SCRIPT);
        assertThat(definitionNamed(definitions, "debugWorkflowExecution").agentType())
            .isEqualTo(WorkflowExecutionAgentType.DEBUG_WORKFLOW_EXECUTION);

        assertThat(definitionNamed(definitions, "authorSkill").agentType())
            .isEqualTo(SkillAgentType.SKILLS);
    }

    @Test
    void testImportWorkflowWithAProjectIdCreatesTheWorkflowThroughProjectWorkflowTools() {
        ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);

        stubCreateProjectWorkflowRecordingACapture(projectWorkflowTools);

        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);

        when(chatClient.prompt(anyString())
            .toolContext(anyMap())
            .call()
            .content()).thenReturn("converted");

        IntelligentToolContributor contributor = workflowIntelligentToolContributor(
            emptyProvider(), emptyProvider(), present((IntelligentToolChatClientFactory) chatModel -> chatClient),
            emptyProvider(), emptyProvider(), present(new ProjectWorkflowLifecycleTools(projectWorkflowTools)),
            emptyProvider());

        IntelligentToolDefinition converterDefinition = definitionNamed(
            contributor.getIntelligentToolDefinitions(), "importWorkflow");

        ToolCallback toolCallback = converterDefinition.create(
            converterDefinition.chatClientFactory(IntelligentToolVariant.BUILD));

        String result = toolCallback.call("{\"request\":\"convert this n8n workflow\",\"projectId\":7}");

        assertThat(result).startsWith("converted");

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);

        verify(projectWorkflowTools).createProjectWorkflow(
            eq(7L), definitionCaptor.capture(), any(ToolContext.class));

        assertThat(definitionCaptor.getValue()).contains("Imported workflow");

        verify(chatClient).prompt("Target workflowId: wf-new\n\nconvert this n8n workflow");
    }

    @Test
    void testImportWorkflowDeletesTheCreatedWorkflowThroughProjectWorkflowToolsWhenTheImportFails() {
        ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);

        stubCreateProjectWorkflowRecordingACapture(projectWorkflowTools);

        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);

        when(chatClient.prompt(anyString())
            .toolContext(anyMap())
            .call()
            .content()).thenReturn("{\"error\":\"could not map the n8n nodes\"}");

        IntelligentToolContributor contributor = workflowIntelligentToolContributor(
            emptyProvider(), emptyProvider(), present((IntelligentToolChatClientFactory) chatModel -> chatClient),
            emptyProvider(), emptyProvider(), present(new ProjectWorkflowLifecycleTools(projectWorkflowTools)),
            emptyProvider());

        IntelligentToolDefinition converterDefinition = definitionNamed(
            contributor.getIntelligentToolDefinitions(), "importWorkflow");

        String result = converterDefinition.create(converterDefinition.chatClientFactory(IntelligentToolVariant.BUILD))
            .call("{\"request\":\"convert this n8n workflow\",\"projectId\":7}");

        assertThat(result).isEqualTo("{\"error\":\"could not map the n8n nodes\"}");

        verify(projectWorkflowTools).deleteWorkflow(eq("wf-new"), any(ToolContext.class));
    }

    @Test
    void testConverterAgentHasNoAskVariantButHasBuildVariant() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        IntelligentToolDefinition converterDefinition = definitionNamed(definitions, "importWorkflow");

        assertThat(converterDefinition.chatClientFactory(IntelligentToolVariant.ASK)).isNull();
        assertThat(converterDefinition.chatClientFactory(IntelligentToolVariant.BUILD)).isNotNull();
    }

    @Test
    void testChatClientFactoryIsNullWhenProviderHasNoBean() {
        List<IntelligentToolDefinition> definitions = contributedDefinitions(
            workflowIntelligentToolContributor(emptyProvider(),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)), present(mock(SubAgentChatModelResolver.class)),
                emptyProvider(), emptyProvider()),
            platformIntelligentToolContributor(
                emptyProvider(), present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)), present(mock(SubAgentChatModelResolver.class))));

        IntelligentToolDefinition projectWorkflowDefinition = definitionNamed(definitions, "buildWorkflow");

        assertThat(projectWorkflowDefinition.chatClientFactory(IntelligentToolVariant.ASK)).isNull();
        assertThat(projectWorkflowDefinition.chatClientFactory(IntelligentToolVariant.BUILD)).isNotNull();

        IntelligentToolDefinition clusterElementDefinition = definitionNamed(definitions, "configureClusterElement");

        assertThat(clusterElementDefinition.chatClientFactory(IntelligentToolVariant.ASK)).isNull();
        assertThat(clusterElementDefinition.chatClientFactory(IntelligentToolVariant.BUILD)).isNotNull();
    }

    @Test
    void testPanelScopesAreProjectOnlyForProjectWorkflowAndConverterAgents() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        for (IntelligentToolDefinition definition : definitions) {
            if (definition.name()
                .equals("buildWorkflow")
                || definition.name()
                    .equals("importWorkflow")) {

                assertThat(definition.panelScopes()).containsExactly(IntelligentToolScope.PROJECT);
            } else {
                assertThat(definition.panelScopes()).isEmpty();
            }
        }
    }

    @Test
    void testConverterCreateDoesNotResolveTheFactoryEagerly() {
        AtomicInteger getCount = new AtomicInteger();

        IntelligentToolChatClientFactory countingChatClientFactory = chatModel -> {
            getCount.incrementAndGet();

            return mock(ChatClient.class);
        };

        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        IntelligentToolDefinition converterDefinition = definitionNamed(definitions, "importWorkflow");

        ToolCallback toolCallback = converterDefinition.create(countingChatClientFactory);

        assertThat(toolCallback).isNotNull();
        assertThat(getCount).hasValue(0);
    }

    @Test
    void testTheFactoryBuildsADistinctClientForAnOverrideModel() {
        RealDefinitions realDefinitions = buildRealDefinitions();

        for (DelegateCase delegateCase : DELEGATE_CASES) {
            IntelligentToolChatClientFactory factory =
                definitionNamed(realDefinitions.definitions(), delegateCase.definitionName())
                    .chatClientFactory(delegateCase.variant());

            assertThat(factory)
                .as("%s (%s) chatClientFactory", delegateCase.definitionName(), delegateCase.variant())
                .isNotNull();

            ChatClient defaultChatClient = factory.get(null);
            ChatClient overriddenChatClient = factory.get(mock(ChatModel.class));

            assertThat(overriddenChatClient)
                .as(
                    "%s (%s) rebuilt client for an override model", delegateCase.definitionName(),
                    delegateCase.variant())
                .isNotSameAs(defaultChatClient);
        }
    }

    @Test
    void testTheFactoryReturnsTheBeanClientForANullModel() {
        RealDefinitions realDefinitions = buildRealDefinitions();

        for (DelegateCase delegateCase : DELEGATE_CASES) {
            IntelligentToolChatClientFactory factory =
                definitionNamed(realDefinitions.definitions(), delegateCase.definitionName())
                    .chatClientFactory(delegateCase.variant());

            ChatClient beanClient = realDefinitions.beanClientsByKey()
                .get(delegateCase.fixtureKey());

            assertThat(factory.get(null))
                .as("%s (%s) default client", delegateCase.definitionName(), delegateCase.variant())
                .isSameAs(beanClient);
        }
    }

    @Test
    void testTheAskFactoryAddsTheWebSearchToolsOfTheCurrentEnvironment() {
        CopilotConfiguration copilotConfiguration = realCopilotConfiguration();
        ChatModel chatModel = mock(ChatModel.class);
        ComponentTools componentTools = mock(ComponentTools.class);
        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        WebSearchToolCallbackProvider webSearchToolCallbackProvider = mock(WebSearchToolCallbackProvider.class);

        when(webSearchToolCallbackProvider.getToolCallbacks(Environment.STAGING.ordinal()))
            .thenReturn(List.of(mock(ToolCallback.class)));

        ChatClient beanClient = copilotConfiguration.codeEditorAskSubAgentChatClient(
            chatModel, readProjectWorkflowTools, componentTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.codeEditorAskSubAgentChatClientFactory(
            beanClient, chatModel, readProjectWorkflowTools, componentTools, webSearchToolCallbackProvider);

        EnvironmentContext.set(Environment.STAGING);

        try {
            assertThat(factory.get(null)).isNotSameAs(beanClient);
        } finally {
            EnvironmentContext.clear();
        }

        assertThat(factory.get(null)).isSameAs(beanClient);

        verify(webSearchToolCallbackProvider).getToolCallbacks(Environment.STAGING.ordinal());
    }

    @Test
    void testTheBuildWorkflowFactoryAddsTheWebSearchToolsOfTheCurrentEnvironment() {
        CopilotConfiguration copilotConfiguration = realCopilotConfiguration();
        ChatModel chatModel = mock(ChatModel.class);
        ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);
        ScriptTools scriptTools = mock(ScriptTools.class);
        TaskTools taskTools = mock(TaskTools.class);
        WebSearchToolCallbackProvider webSearchToolCallbackProvider = mock(WebSearchToolCallbackProvider.class);
        ObjectProvider<SubAgentAskRelay> subAgentAskRelayProvider = emptySubAgentAskRelayProvider();

        when(webSearchToolCallbackProvider.getToolCallbacks(Environment.PRODUCTION.ordinal()))
            .thenReturn(List.of(mock(ToolCallback.class)));

        ChatClient beanClient = copilotConfiguration.workflowEditorBuildSubAgentChatClient(
            chatModel, projectWorkflowTools, taskTools, scriptTools, mock(ComponentTools.class),
            subAgentAskRelayProvider);

        IntelligentToolChatClientFactory factory = copilotConfiguration.workflowEditorBuildSubAgentChatClientFactory(
            beanClient, chatModel, projectWorkflowTools, taskTools, scriptTools, mock(ComponentTools.class),
            subAgentAskRelayProvider,
            webSearchToolCallbackProvider);

        EnvironmentContext.set(Environment.PRODUCTION);

        try {
            assertThat(factory.get(null)).isNotSameAs(beanClient);
        } finally {
            EnvironmentContext.clear();
        }

        assertThat(factory.get(null)).isSameAs(beanClient);
    }

    private List<IntelligentToolDefinition> definitionsWithAllProvidersPresent() {
        List<IntelligentToolDefinition> definitions = contributedDefinitions(
            workflowIntelligentToolContributor(
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)), present(mock(SubAgentChatModelResolver.class)),
                emptyProvider(), emptyProvider()),
            platformIntelligentToolContributor(
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)), present(mock(SubAgentChatModelResolver.class))));

        return definitions;
    }

    private RealDefinitions buildRealDefinitions() {
        CopilotConfiguration copilotConfiguration = realCopilotConfiguration();
        ChatModel defaultChatModel = mock(ChatModel.class);

        DelegateFixture workflowEditorAsk = workflowEditorAskFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture workflowEditorBuild = workflowEditorBuildFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture converterBuild = converterBuildFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture clusterElementAsk = clusterElementAskFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture clusterElementBuild = clusterElementBuildFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture codeEditorAsk = codeEditorAskFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture codeEditorBuild = codeEditorBuildFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture skillsAsk = skillsAskFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture skillsBuild = skillsBuildFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture workflowExecutionBuild = workflowExecutionBuildFixture(copilotConfiguration, defaultChatModel);

        List<IntelligentToolDefinition> definitions = contributedDefinitions(
            workflowIntelligentToolContributor(present(workflowEditorAsk.factory()),
                present(workflowEditorBuild.factory()), present(converterBuild.factory()),
                present(workflowExecutionBuild.factory()), present(mock(SubAgentChatModelResolver.class)),
                emptyProvider(), emptyProvider()),
            platformIntelligentToolContributor(
                present(clusterElementAsk.factory()), present(clusterElementBuild.factory()),
                present(codeEditorAsk.factory()), present(codeEditorBuild.factory()), present(skillsAsk.factory()),
                present(skillsBuild.factory()), present(mock(SubAgentChatModelResolver.class))));

        Map<String, ChatClient> beanClientsByKey = Map.ofEntries(
            Map.entry("workflowEditorAsk", workflowEditorAsk.beanClient()),
            Map.entry("workflowEditorBuild", workflowEditorBuild.beanClient()),
            Map.entry("converterBuild", converterBuild.beanClient()),
            Map.entry("clusterElementAsk", clusterElementAsk.beanClient()),
            Map.entry("clusterElementBuild", clusterElementBuild.beanClient()),
            Map.entry("codeEditorAsk", codeEditorAsk.beanClient()),
            Map.entry("codeEditorBuild", codeEditorBuild.beanClient()),
            Map.entry("skillsAsk", skillsAsk.beanClient()),
            Map.entry("skillsBuild", skillsBuild.beanClient()),
            Map.entry("workflowExecutionBuild", workflowExecutionBuild.beanClient()));

        return new RealDefinitions(definitions, beanClientsByKey);
    }

    private static DelegateFixture workflowEditorAskFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ReadProjectTools readProjectTools = mock(ReadProjectTools.class);
        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        ComponentTools componentTools = mock(ComponentTools.class);
        TaskTools taskTools = mock(TaskTools.class);
        WebSearchToolCallbackProvider webSearchToolCallbackProvider = mock(WebSearchToolCallbackProvider.class);
        Advisor questionAnswerAdvisor = mock(Advisor.class);

        ChatClient beanClient = copilotConfiguration.workflowEditorAskSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, componentTools, taskTools, questionAnswerAdvisor);

        IntelligentToolChatClientFactory factory = copilotConfiguration.workflowEditorAskSubAgentChatClientFactory(
            beanClient, chatModel, readProjectTools, readProjectWorkflowTools, componentTools, taskTools,
            webSearchToolCallbackProvider, questionAnswerAdvisor);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture workflowEditorBuildFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);
        TaskTools taskTools = mock(TaskTools.class);
        ScriptTools scriptTools = mock(ScriptTools.class);

        ObjectProvider<SubAgentAskRelay> subAgentAskRelayProvider = emptySubAgentAskRelayProvider();

        ChatClient beanClient = copilotConfiguration.workflowEditorBuildSubAgentChatClient(
            chatModel, projectWorkflowTools, taskTools, scriptTools, mock(ComponentTools.class),
            subAgentAskRelayProvider);

        IntelligentToolChatClientFactory factory = copilotConfiguration.workflowEditorBuildSubAgentChatClientFactory(
            beanClient, chatModel, projectWorkflowTools, taskTools, scriptTools, mock(ComponentTools.class),
            subAgentAskRelayProvider,
            mock(WebSearchToolCallbackProvider.class));

        return new DelegateFixture(beanClient, factory);
    }

    @Test
    void testTheBuildWorkflowSubAgentGetsOnlyTheClusterElementLookups() {
        List<String> toolNames = CopilotConfiguration.clusterElementLookupToolCallbacks(mock(ComponentTools.class))
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).containsExactlyInAnyOrder("getAction", "getClusterElement", "searchClusterElements");
    }

    private static DelegateFixture converterBuildFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);
        TaskTools taskTools = mock(TaskTools.class);
        ScriptTools scriptTools = mock(ScriptTools.class);

        ChatClient beanClient = copilotConfiguration.converterBuildSubAgentChatClient(
            chatModel, projectWorkflowTools, taskTools, scriptTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.converterBuildSubAgentChatClientFactory(
            beanClient, chatModel, projectWorkflowTools, taskTools, scriptTools,
            mock(WebSearchToolCallbackProvider.class));

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture clusterElementAskFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        ComponentTools componentTools = mock(ComponentTools.class);
        TaskTools taskTools = mock(TaskTools.class);
        WebSearchToolCallbackProvider webSearchToolCallbackProvider = mock(WebSearchToolCallbackProvider.class);

        ChatClient beanClient = copilotConfiguration.clusterElementAskSubAgentChatClient(
            chatModel, readProjectWorkflowTools, componentTools, taskTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.clusterElementAskSubAgentChatClientFactory(
            beanClient, chatModel, readProjectWorkflowTools, componentTools, taskTools, webSearchToolCallbackProvider);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture clusterElementBuildFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ClusterElementTools clusterElementTools = mock(ClusterElementTools.class);
        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        ComponentTools componentTools = mock(ComponentTools.class);
        TaskTools taskTools = mock(TaskTools.class);

        ChatClient beanClient = copilotConfiguration.clusterElementBuildSubAgentChatClient(
            chatModel, clusterElementTools, readProjectWorkflowTools, componentTools, taskTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.clusterElementBuildSubAgentChatClientFactory(
            beanClient, clusterElementTools, readProjectWorkflowTools, componentTools, taskTools);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture codeEditorAskFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        ComponentTools componentTools = mock(ComponentTools.class);
        WebSearchToolCallbackProvider webSearchToolCallbackProvider = mock(WebSearchToolCallbackProvider.class);

        ChatClient beanClient = copilotConfiguration.codeEditorAskSubAgentChatClient(
            chatModel, readProjectWorkflowTools, componentTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.codeEditorAskSubAgentChatClientFactory(
            beanClient, chatModel, readProjectWorkflowTools, componentTools, webSearchToolCallbackProvider);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture codeEditorBuildFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ScriptTools scriptTools = mock(ScriptTools.class);
        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        ComponentTools componentTools = mock(ComponentTools.class);

        ChatClient beanClient = copilotConfiguration.codeEditorBuildSubAgentChatClient(
            chatModel, scriptTools, readProjectWorkflowTools, componentTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.codeEditorBuildSubAgentChatClientFactory(
            beanClient, scriptTools, readProjectWorkflowTools, componentTools);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture skillsAskFixture(CopilotConfiguration copilotConfiguration, ChatModel chatModel) {
        ReadProjectTools readProjectTools = mock(ReadProjectTools.class);
        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        ReadSkillsTools readSkillsTools = mock(ReadSkillsTools.class);
        WebSearchToolCallbackProvider webSearchToolCallbackProvider = mock(WebSearchToolCallbackProvider.class);

        ChatClient beanClient = copilotConfiguration.skillsAskSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, readSkillsTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.skillsAskSubAgentChatClientFactory(
            beanClient, chatModel, readProjectTools, readProjectWorkflowTools, readSkillsTools,
            webSearchToolCallbackProvider);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture skillsBuildFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ReadProjectTools readProjectTools = mock(ReadProjectTools.class);
        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        SkillsTools skillsTools = mock(SkillsTools.class);
        ComponentTools componentTools = mock(ComponentTools.class);

        ChatClient beanClient = copilotConfiguration.skillsBuildSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, skillsTools, componentTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.skillsBuildSubAgentChatClientFactory(
            beanClient, readProjectTools, readProjectWorkflowTools, skillsTools, componentTools);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture workflowExecutionBuildFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        WorkflowExecutionTools workflowExecutionTools = mock(WorkflowExecutionTools.class);
        ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);
        ScriptTools scriptTools = mock(ScriptTools.class);
        TaskTools taskTools = mock(TaskTools.class);

        ChatClient beanClient = copilotConfiguration.workflowExecutionBuildSubAgentChatClient(
            chatModel, workflowExecutionTools, projectWorkflowTools, scriptTools, taskTools);

        IntelligentToolChatClientFactory factory =
            copilotConfiguration.workflowExecutionBuildSubAgentChatClientFactory(
                beanClient, workflowExecutionTools, projectWorkflowTools, scriptTools, taskTools);

        return new DelegateFixture(beanClient, factory);
    }

    private static CopilotConfiguration realCopilotConfiguration() {
        Resource promptResource = new ByteArrayResource("prompt".getBytes(StandardCharsets.UTF_8));

        WorkflowValidatorTools workflowValidatorTools = mock(WorkflowValidatorTools.class);
        WorkflowInstructionTools workflowInstructionTools = mock(WorkflowInstructionTools.class);
        ConnectionDefinitionService connectionDefinitionService = mock(ConnectionDefinitionService.class);
        WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
        ActionDefinitionService actionDefinitionService = mock(ActionDefinitionService.class);
        ActionDefinitionFacade actionDefinitionFacade = mock(ActionDefinitionFacade.class);
        TriggerDefinitionService triggerDefinitionService = mock(TriggerDefinitionService.class);
        TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);
        PropertyOptionsResolver propertyOptionsResolver = mock(PropertyOptionsResolver.class);
        ObjectProvider<CopilotConnectionLister> connectionListerProvider = emptyProvider();
        ScopedConnectionGuard scopedConnectionGuard = new ScopedConnectionGuard(connectionListerProvider);

        return new CopilotConfiguration(
            promptResource, promptResource, promptResource, promptResource, promptResource, promptResource,
            promptResource, promptResource, promptResource, promptResource, promptResource, workflowValidatorTools,
            workflowInstructionTools,
            new InteractivePickerToolCallbacksImpl(
                connectionDefinitionService, workspaceConnectionFacade, componentDefinitionService,
                actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
                mock(ClusterElementDefinitionService.class), mock(ClusterElementDefinitionFacade.class),
                propertyOptionsResolver, connectionListerProvider, scopedConnectionGuard),
            actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
            propertyOptionsResolver, scopedConnectionGuard, emptyDataTableToolsProvider());
    }

    private static List<IntelligentToolDefinition> contributedDefinitions(
        IntelligentToolContributor... intelligentToolContributors) {

        return Arrays.stream(intelligentToolContributors)
            .flatMap(intelligentToolContributor -> intelligentToolContributor.getIntelligentToolDefinitions()
                .stream())
            .toList();
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
    private static void stubCreateProjectWorkflowRecordingACapture(ProjectWorkflowTools projectWorkflowTools) {
        when(projectWorkflowTools.createProjectWorkflow(eq(7L), anyString(), any(ToolContext.class)))
            .thenAnswer(invocation -> {
                ToolContext toolContext = invocation.getArgument(2);

                List<Map<String, Object>> captures = (List<Map<String, Object>>) toolContext.getContext()
                    .get("bytechef.workflowEditor.persistedWorkflows");

                captures.add(
                    Map.of(
                        "created", true, "workflowId", "wf-new", "projectId", 7L, "projectWorkflowId", 1L, "name",
                        "Imported workflow"));

                return new ProjectWorkflowInfo(1L, 7L, 1, "wf-new", "uuid", null, null);
            });
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }

    private record DelegateFixture(ChatClient beanClient, IntelligentToolChatClientFactory factory) {
    }

    private record RealDefinitions(List<IntelligentToolDefinition> definitions,
        Map<String, ChatClient> beanClientsByKey) {
    }

    private record DelegateCase(String definitionName, IntelligentToolVariant variant, String fixtureKey) {
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<SubAgentAskRelay> emptySubAgentAskRelayProvider() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DataTableTools> emptyDataTableToolsProvider() {
        return mock(ObjectProvider.class);
    }

    private static IntelligentToolContributor platformIntelligentToolContributor(
        ObjectProvider<IntelligentToolChatClientFactory> clusterElementAskFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> clusterElementBuildFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> codeEditorAskFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> codeEditorBuildFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> skillsAskFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> skillsBuildFactoryProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider) {

        IntelligentToolContributor contributor1 =
            new ClusterElementIntelligentToolContributorConfiguration().clusterElementIntelligentToolContributor(
                clusterElementAskFactoryProvider, clusterElementBuildFactoryProvider, chatModelResolverProvider);
        IntelligentToolContributor contributor2 =
            new CodeEditorIntelligentToolContributorConfiguration().codeEditorIntelligentToolContributor(
                codeEditorAskFactoryProvider, codeEditorBuildFactoryProvider, chatModelResolverProvider);
        IntelligentToolContributor contributor3 =
            new SkillsIntelligentToolContributorConfiguration().skillsIntelligentToolContributor(
                skillsAskFactoryProvider, skillsBuildFactoryProvider, chatModelResolverProvider);

        List<String> order = List.of("configureClusterElement", "writeScript", "authorSkill");

        return () -> Stream.of(contributor1, contributor2, contributor3)
            .flatMap(contributor -> contributor.getIntelligentToolDefinitions()
                .stream())
            .sorted(Comparator.comparingInt(definition -> order.indexOf(definition.name())))
            .toList();
    }

    private static IntelligentToolContributor workflowIntelligentToolContributor(
        ObjectProvider<IntelligentToolChatClientFactory> workflowEditorAskFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> workflowEditorBuildFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> converterBuildFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> workflowExecutionBuildFactoryProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider,
        ObjectProvider<ProjectWorkflowLifecycleTools> projectWorkflowLifecycleToolsProvider,
        ObjectProvider<PersistedWorkflowChecker> persistedWorkflowCheckerProvider) {

        IntelligentToolContributor contributor1 =
            new WorkflowExecutionIntelligentToolContributorConfiguration().workflowExecutionIntelligentToolContributor(
                workflowExecutionBuildFactoryProvider, chatModelResolverProvider);
        IntelligentToolContributor contributor2 =
            new WorkflowIntelligentToolContributorConfiguration().workflowIntelligentToolContributor(
                workflowEditorAskFactoryProvider, workflowEditorBuildFactoryProvider, converterBuildFactoryProvider,
                chatModelResolverProvider, projectWorkflowLifecycleToolsProvider, persistedWorkflowCheckerProvider);

        List<String> order = List.of("buildWorkflow", "importWorkflow", "debugWorkflowExecution");

        return () -> Stream.of(contributor1, contributor2)
            .flatMap(contributor -> contributor.getIntelligentToolDefinitions()
                .stream())
            .sorted(Comparator.comparingInt(definition -> order.indexOf(definition.name())))
            .toList();
    }
}
