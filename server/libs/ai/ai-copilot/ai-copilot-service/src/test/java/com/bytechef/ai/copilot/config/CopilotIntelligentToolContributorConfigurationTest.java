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
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.automation.ai.tool.SkillsTools;
import com.bytechef.automation.ai.tool.WorkflowExecutionTools;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.ai.tool.ComponentTools;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
final class CopilotIntelligentToolContributorConfigurationTest {

    private static final Map<String, String> FIXTURE_KEYS_BY_DEFINITION_NAME = Map.of(
        "buildWorkflow", "workflowEditorBuild",
        "importWorkflow", "converterBuild",
        "configureClusterElement", "clusterElementBuild",
        "writeScript", "codeEditorBuild",
        "authorSkill", "skillsBuild",
        "debugWorkflowExecution", "workflowExecutionBuild");

    private final CopilotIntelligentToolContributorConfiguration configuration =
        new CopilotIntelligentToolContributorConfiguration();

    @Test
    void testContributesSixDefinitionsWithExpectedNames() {
        List<IntelligentToolDefinition> definitions = definitionsWithAllProvidersPresent();

        assertThat(definitions).extracting(IntelligentToolDefinition::name)
            .containsExactly(
                "buildWorkflow", "importWorkflow", "configureClusterElement", "writeScript",
                "authorSkill", "debugWorkflowExecution");
    }

    @Test
    void testChatClientFactoryIsNullWhenProviderHasNoBean() {
        IntelligentToolContributor contributor = configuration.copilotIntelligentToolContributor(
            emptyProvider(), present(mock(IntelligentToolChatClientFactory.class)), emptyProvider(),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)));

        List<IntelligentToolDefinition> definitions = contributor.getIntelligentToolDefinitions();

        assertThat(definitionNamed(definitions, "buildWorkflow").chatClientFactory(IntelligentToolVariant.BUILD))
            .isNull();
        assertThat(
            definitionNamed(definitions, "configureClusterElement").chatClientFactory(IntelligentToolVariant.BUILD))
                .isNull();
        assertThat(definitionNamed(definitions, "importWorkflow").chatClientFactory(IntelligentToolVariant.BUILD))
            .isNotNull();
    }

    @Test
    void testConverterCreateDoesNotResolveTheFactoryEagerly() {
        AtomicInteger getCount = new AtomicInteger();

        IntelligentToolChatClientFactory countingChatClientFactory = () -> {
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
    void testTheFactoryReturnsTheBeanClient() {
        RealDefinitions realDefinitions = buildRealDefinitions();

        for (Map.Entry<String, String> entry : FIXTURE_KEYS_BY_DEFINITION_NAME.entrySet()) {
            IntelligentToolChatClientFactory factory = definitionNamed(realDefinitions.definitions(), entry.getKey())
                .chatClientFactory(IntelligentToolVariant.BUILD);

            assertThat(factory)
                .as("%s chatClientFactory", entry.getKey())
                .isNotNull();

            ChatClient beanClient = realDefinitions.beanClientsByKey()
                .get(entry.getValue());

            assertThat(factory.get())
                .as("%s default client", entry.getKey())
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
            present(mock(IntelligentToolChatClientFactory.class)));

        return contributor.getIntelligentToolDefinitions();
    }

    private RealDefinitions buildRealDefinitions() {
        CopilotConfiguration copilotConfiguration = realCopilotConfiguration();
        ChatModel defaultChatModel = mock(ChatModel.class);

        DelegateFixture workflowEditorBuild = workflowEditorBuildFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture converterBuild = converterBuildFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture clusterElementBuild = clusterElementBuildFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture codeEditorBuild = codeEditorBuildFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture skillsBuild = skillsBuildFixture(copilotConfiguration, defaultChatModel);
        DelegateFixture workflowExecutionBuild = workflowExecutionBuildFixture(copilotConfiguration, defaultChatModel);

        IntelligentToolContributor contributor = configuration.copilotIntelligentToolContributor(
            present(workflowEditorBuild.factory()), present(converterBuild.factory()),
            present(clusterElementBuild.factory()), present(codeEditorBuild.factory()),
            present(skillsBuild.factory()), present(workflowExecutionBuild.factory()));

        Map<String, ChatClient> beanClientsByKey = Map.ofEntries(
            Map.entry("workflowEditorBuild", workflowEditorBuild.beanClient()),
            Map.entry("converterBuild", converterBuild.beanClient()),
            Map.entry("clusterElementBuild", clusterElementBuild.beanClient()),
            Map.entry("codeEditorBuild", codeEditorBuild.beanClient()),
            Map.entry("skillsBuild", skillsBuild.beanClient()),
            Map.entry("workflowExecutionBuild", workflowExecutionBuild.beanClient()));

        return new RealDefinitions(contributor.getIntelligentToolDefinitions(), beanClientsByKey);
    }

    private static DelegateFixture workflowEditorBuildFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ProjectTools projectTools = mock(ProjectTools.class);
        ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);
        TaskTools taskTools = mock(TaskTools.class);
        ScriptTools scriptTools = mock(ScriptTools.class);

        ChatClient beanClient = copilotConfiguration.workflowEditorBuildSubAgentChatClient(
            chatModel, projectTools, projectWorkflowTools, taskTools, scriptTools);

        IntelligentToolChatClientFactory factory =
            copilotConfiguration.workflowEditorBuildSubAgentChatClientFactory(beanClient);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture converterBuildFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ProjectTools projectTools = mock(ProjectTools.class);
        ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);
        TaskTools taskTools = mock(TaskTools.class);
        ScriptTools scriptTools = mock(ScriptTools.class);

        ChatClient beanClient = copilotConfiguration.converterBuildSubAgentChatClient(
            chatModel, projectTools, projectWorkflowTools, taskTools, scriptTools);

        IntelligentToolChatClientFactory factory =
            copilotConfiguration.converterBuildSubAgentChatClientFactory(beanClient);

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

        IntelligentToolChatClientFactory factory =
            copilotConfiguration.clusterElementBuildSubAgentChatClientFactory(beanClient);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture codeEditorBuildFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ScriptTools scriptTools = mock(ScriptTools.class);
        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        ComponentTools componentTools = mock(ComponentTools.class);

        ChatClient beanClient = copilotConfiguration.codeEditorBuildSubAgentChatClient(
            chatModel, scriptTools, readProjectWorkflowTools, componentTools);

        IntelligentToolChatClientFactory factory =
            copilotConfiguration.codeEditorBuildSubAgentChatClientFactory(beanClient);

        return new DelegateFixture(beanClient, factory);
    }

    private static DelegateFixture skillsBuildFixture(
        CopilotConfiguration copilotConfiguration, ChatModel chatModel) {

        ReadProjectTools readProjectTools = mock(ReadProjectTools.class);
        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        SkillsTools skillsTools = mock(SkillsTools.class);

        ChatClient beanClient = copilotConfiguration.skillsBuildSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, skillsTools);

        IntelligentToolChatClientFactory factory =
            copilotConfiguration.skillsBuildSubAgentChatClientFactory(beanClient);

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
            copilotConfiguration.workflowExecutionBuildSubAgentChatClientFactory(beanClient);

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
            promptResource, promptResource, promptResource, promptResource, workflowValidatorTools,
            workflowInstructionTools, connectionDefinitionService, workspaceConnectionFacade,
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
}
