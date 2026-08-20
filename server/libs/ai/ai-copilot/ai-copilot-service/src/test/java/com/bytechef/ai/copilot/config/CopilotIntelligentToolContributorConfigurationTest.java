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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadSkillsTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.automation.ai.tool.SkillsTools;
import com.bytechef.automation.ai.tool.WorkflowExecutionTools;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.ai.tool.BraveWebSearchTools;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.FirecrawlTools;
import com.bytechef.platform.ai.tool.SimulationTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.ai.tool.WorkflowInstructionTools;
import com.bytechef.platform.ai.tool.WorkflowValidatorTools;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
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
        new DelegateCase("debugWorkflowExecution", IntelligentToolVariant.ASK, "workflowExecutionAsk"),
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

        assertThat(definitionNamed(definitions, "buildWorkflow").agentTypeKey())
            .isEqualTo(CopilotAgentType.BUILD_WORKFLOW.key());
        assertThat(definitionNamed(definitions, "importWorkflow").agentTypeKey())
            .isEqualTo(CopilotAgentType.IMPORT_WORKFLOW.key());
        assertThat(definitionNamed(definitions, "configureClusterElement").agentTypeKey())
            .isEqualTo(CopilotAgentType.CONFIGURE_CLUSTER_ELEMENT.key());
        assertThat(definitionNamed(definitions, "writeScript").agentTypeKey())
            .isEqualTo(CopilotAgentType.WRITE_SCRIPT.key());
        assertThat(definitionNamed(definitions, "debugWorkflowExecution").agentTypeKey())
            .isEqualTo(CopilotAgentType.DEBUG_WORKFLOW_EXECUTION.key());

        // Deliberately CopilotAgentType.SKILLS.key() ("skills"), NOT the callback's own "authorSkill" tool
        // name -- there is no "authorSkill" AgentType registered with AgentTypeRegistry, and keying the
        // per-conversation session memory on an unregistered key would leave a session that task delete's
        // registry-driven purge could never reconstruct and clean up.
        assertThat(definitionNamed(definitions, "authorSkill").agentTypeKey())
            .isEqualTo(CopilotAgentType.SKILLS.key())
            .isEqualTo("skills");
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
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)), present(mock(SubAgentChatModelResolver.class)));

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

    /**
     * End-to-end proof that Task 5 landed: each real {@link CopilotConfiguration} {@code *SubAgentChatClientFactory}
     * bean, reached through the real {@link CopilotIntelligentToolContributorConfiguration} wiring, rebuilds a DISTINCT
     * {@link ChatClient} when handed an override {@link ChatModel} — for every ASK/BUILD variant of all six CE
     * definitions that {@link CopilotConfiguration} backs.
     */
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

    /**
     * Companion to {@link #testTheFactoryBuildsADistinctClientForAnOverrideModel()}: a {@code null} model must yield
     * literally the pre-built default {@link ChatClient} bean instance, not a fresh rebuild — matching
     * {@link IntelligentToolChatClientFactory#get(ChatModel)}'s documented "or {@code null} for the contributor's
     * default client" contract.
     */
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
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)), present(mock(SubAgentChatModelResolver.class)));

        return contributor.getIntelligentToolDefinitions();
    }

    /**
     * Wires eleven real {@code *SubAgentChatClientFactory} beans (built from a real, hand-constructed
     * {@link CopilotConfiguration}) into a real {@link CopilotIntelligentToolContributorConfiguration}, and returns
     * both the resulting definitions and the eleven default {@link ChatClient} bean instances, keyed by fixture key, so
     * a test can assert against them.
     */
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
        DelegateFixture workflowExecutionAsk = workflowExecutionAskFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture workflowExecutionBuild = workflowExecutionBuildFixture(copilotConfiguration, defaultChatModel);

        IntelligentToolContributor contributor = configuration.copilotIntelligentToolContributor(
            present(workflowEditorAsk.factory()), present(workflowEditorBuild.factory()),
            present(converterBuild.factory()), present(clusterElementAsk.factory()),
            present(clusterElementBuild.factory()), present(codeEditorAsk.factory()),
            present(codeEditorBuild.factory()), present(skillsAsk.factory()), present(skillsBuild.factory()),
            present(workflowExecutionAsk.factory()), present(workflowExecutionBuild.factory()),
            present(mock(SubAgentChatModelResolver.class)));

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
            Map.entry("workflowExecutionAsk", workflowExecutionAsk.beanClient()),
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
        SimulationTools simulationTools = mock(SimulationTools.class);

        ChatClient beanClient = copilotConfiguration.workflowEditorBuildSubAgentChatClient(
            chatModel, projectWorkflowTools, taskTools, scriptTools, simulationTools);

        IntelligentToolChatClientFactory factory = copilotConfiguration.workflowEditorBuildSubAgentChatClientFactory(
            beanClient, projectWorkflowTools, taskTools, scriptTools, simulationTools);

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

    private static DelegateFixture workflowExecutionAskFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        WorkflowExecutionTools workflowExecutionTools = mock(WorkflowExecutionTools.class);
        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        ComponentTools componentTools = mock(ComponentTools.class);
        Optional<FirecrawlTools> firecrawlTools = Optional.empty();
        Optional<BraveWebSearchTools> braveWebSearchTools = Optional.empty();

        ChatClient beanClient = copilotConfiguration.workflowExecutionAskSubAgentChatClient(
            chatModel, workflowExecutionTools, readProjectWorkflowTools, componentTools, firecrawlTools,
            braveWebSearchTools);

        IntelligentToolChatClientFactory factory =
            copilotConfiguration.workflowExecutionAskSubAgentChatClientFactory(
                beanClient, workflowExecutionTools, readProjectWorkflowTools, componentTools, firecrawlTools,
                braveWebSearchTools);

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

        return new CopilotConfiguration(
            promptResource, promptResource, promptResource, promptResource, promptResource, promptResource,
            promptResource, promptResource, promptResource, promptResource, promptResource, promptResource,
            promptResource, promptResource, promptResource, workflowValidatorTools, workflowInstructionTools,
            promptResource, promptResource, connectionDefinitionService, workspaceConnectionFacade,
            componentDefinitionService, actionDefinitionService, actionDefinitionFacade, triggerDefinitionService,
            triggerDefinitionFacade, propertyOptionsResolver, connectionListerProvider);
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

    private record DelegateFixture(ChatClient beanClient, IntelligentToolChatClientFactory factory) {
    }

    private record RealDefinitions(List<IntelligentToolDefinition> definitions,
        Map<String, ChatClient> beanClientsByKey) {
    }

    private record DelegateCase(String definitionName, IntelligentToolVariant variant, String fixtureKey) {
    }
}
