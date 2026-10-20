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
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.copilot.tool.ask.SubAgentAskRelay;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadSkillsTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.automation.ai.tool.SkillsTools;
import com.bytechef.automation.ai.tool.WorkflowExecutionTools;
import com.bytechef.automation.ai.tool.model.ProjectWorkflowInfo;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.ai.tool.BraveWebSearchTools;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.FirecrawlTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.ai.tool.WorkflowInstructionTools;
import com.bytechef.platform.ai.tool.WorkflowValidatorTools;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
final class CopilotIntelligentToolContributorConfigurationTest {

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

    private final CopilotIntelligentToolContributorConfiguration configuration =
        new CopilotIntelligentToolContributorConfiguration();

    @Test
    void testContributesSixDefinitionsWithExpectedNamesAndAgentTypeKeys() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        assertThat(definitions).hasSize(6);

        assertThat(definitions).extracting(IntelligentToolDefinition::name)
            .containsExactly(
                "buildWorkflow", "importWorkflow", "configureClusterElement", "writeScript",
                "authorSkill", "debugWorkflowExecution");

        assertThat(definitionNamed(definitions, "buildWorkflow").agentType())
            .isEqualTo(CopilotAgentType.BUILD_WORKFLOW);
        assertThat(definitionNamed(definitions, "importWorkflow").agentType())
            .isEqualTo(CopilotAgentType.IMPORT_WORKFLOW);
        assertThat(definitionNamed(definitions, "configureClusterElement").agentType())
            .isEqualTo(CopilotAgentType.CONFIGURE_CLUSTER_ELEMENT);
        assertThat(definitionNamed(definitions, "writeScript").agentType())
            .isEqualTo(CopilotAgentType.WRITE_SCRIPT);
        assertThat(definitionNamed(definitions, "debugWorkflowExecution").agentType())
            .isEqualTo(CopilotAgentType.DEBUG_WORKFLOW_EXECUTION);

        assertThat(definitionNamed(definitions, "authorSkill").agentType())
            .isEqualTo(CopilotAgentType.SKILLS);
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

        IntelligentToolContributor contributor = configuration.copilotIntelligentToolContributor(
            emptyProvider(), emptyProvider(), present((IntelligentToolChatClientFactory) chatModel -> chatClient),
            emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(),
            emptyProvider(), emptyProvider(), present(new ProjectWorkflowLifecycleTools(projectWorkflowTools)));

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

        IntelligentToolContributor contributor = configuration.copilotIntelligentToolContributor(
            emptyProvider(), emptyProvider(), present((IntelligentToolChatClientFactory) chatModel -> chatClient),
            emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(),
            emptyProvider(), emptyProvider(), present(new ProjectWorkflowLifecycleTools(projectWorkflowTools)));

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
        IntelligentToolContributor contributor = configuration.copilotIntelligentToolContributor(
            emptyProvider(), present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)), emptyProvider(),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)), present(mock(SubAgentChatModelResolver.class)),
            emptyProvider());

        List<IntelligentToolDefinition> definitions = contributor.getIntelligentToolDefinitions();

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

    private List<IntelligentToolDefinition> definitionsWithAllProvidersPresent() {
        IntelligentToolContributor contributor = configuration.copilotIntelligentToolContributor(
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)), present(mock(SubAgentChatModelResolver.class)),
            emptyProvider());

        return contributor.getIntelligentToolDefinitions();
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

        IntelligentToolContributor contributor = configuration.copilotIntelligentToolContributor(
            present(workflowEditorAsk.factory()), present(workflowEditorBuild.factory()),
            present(converterBuild.factory()), present(clusterElementAsk.factory()),
            present(clusterElementBuild.factory()), present(codeEditorAsk.factory()),
            present(codeEditorBuild.factory()), present(skillsAsk.factory()), present(skillsBuild.factory()),
            present(workflowExecutionBuild.factory()),
            present(mock(SubAgentChatModelResolver.class)),
            emptyProvider());

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

        return new RealDefinitions(contributor.getIntelligentToolDefinitions(), beanClientsByKey);
    }

    private static DelegateFixture workflowEditorAskFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ReadProjectTools readProjectTools = mock(ReadProjectTools.class);
        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        ComponentTools componentTools = mock(ComponentTools.class);
        TaskTools taskTools = mock(TaskTools.class);
        Optional<FirecrawlTools> firecrawlTools = Optional.empty();
        Optional<BraveWebSearchTools> braveWebSearchTools = Optional.empty();
        Advisor questionAnswerAdvisor = mock(Advisor.class);

        ChatClient beanClient = copilotConfiguration.workflowEditorAskSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, componentTools, taskTools, firecrawlTools,
            braveWebSearchTools, questionAnswerAdvisor);

        IntelligentToolChatClientFactory factory = copilotConfiguration.workflowEditorAskSubAgentChatClientFactory(
            beanClient, readProjectTools, readProjectWorkflowTools, componentTools, taskTools, firecrawlTools,
            braveWebSearchTools, questionAnswerAdvisor);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture workflowEditorBuildFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);
        TaskTools taskTools = mock(TaskTools.class);
        ScriptTools scriptTools = mock(ScriptTools.class);

        ObjectProvider<SubAgentAskRelay> subAgentAskRelayProvider = emptySubAgentAskRelayProvider();

        ChatClient beanClient = copilotConfiguration.workflowEditorBuildSubAgentChatClient(
            chatModel, projectWorkflowTools, taskTools, scriptTools, subAgentAskRelayProvider);

        IntelligentToolChatClientFactory factory = copilotConfiguration.workflowEditorBuildSubAgentChatClientFactory(
            beanClient, projectWorkflowTools, taskTools, scriptTools, subAgentAskRelayProvider);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture converterBuildFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);
        TaskTools taskTools = mock(TaskTools.class);
        ScriptTools scriptTools = mock(ScriptTools.class);

        ChatClient beanClient = copilotConfiguration.converterBuildSubAgentChatClient(
            chatModel, projectWorkflowTools, taskTools, scriptTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.converterBuildSubAgentChatClientFactory(
            beanClient, projectWorkflowTools, taskTools, scriptTools);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture clusterElementAskFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        ComponentTools componentTools = mock(ComponentTools.class);
        TaskTools taskTools = mock(TaskTools.class);
        Optional<FirecrawlTools> firecrawlTools = Optional.empty();
        Optional<BraveWebSearchTools> braveWebSearchTools = Optional.empty();

        ChatClient beanClient = copilotConfiguration.clusterElementAskSubAgentChatClient(
            chatModel, readProjectWorkflowTools, componentTools, taskTools, firecrawlTools, braveWebSearchTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.clusterElementAskSubAgentChatClientFactory(
            beanClient, readProjectWorkflowTools, componentTools, taskTools, firecrawlTools, braveWebSearchTools);

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
        Optional<FirecrawlTools> firecrawlTools = Optional.empty();
        Optional<BraveWebSearchTools> braveWebSearchTools = Optional.empty();

        ChatClient beanClient = copilotConfiguration.codeEditorAskSubAgentChatClient(
            chatModel, readProjectWorkflowTools, componentTools, firecrawlTools, braveWebSearchTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.codeEditorAskSubAgentChatClientFactory(
            beanClient, readProjectWorkflowTools, componentTools, firecrawlTools, braveWebSearchTools);

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
        Optional<FirecrawlTools> firecrawlTools = Optional.empty();
        Optional<BraveWebSearchTools> braveWebSearchTools = Optional.empty();

        ChatClient beanClient = copilotConfiguration.skillsAskSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, readSkillsTools, firecrawlTools,
            braveWebSearchTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.skillsAskSubAgentChatClientFactory(
            beanClient, readProjectTools, readProjectWorkflowTools, readSkillsTools, firecrawlTools,
            braveWebSearchTools);

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
            promptResource, promptResource, promptResource, promptResource, promptResource, promptResource,
            promptResource, promptResource, promptResource, promptResource, promptResource, workflowValidatorTools,
            workflowInstructionTools,
            new InteractivePickerToolCallbacks(
                connectionDefinitionService, workspaceConnectionFacade, componentDefinitionService,
                actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
                propertyOptionsResolver, connectionListerProvider, scopedConnectionGuard),
            actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
            propertyOptionsResolver, scopedConnectionGuard);
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
}
