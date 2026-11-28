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

package com.bytechef.automation.workflow.execution.tool.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.tool.ask.SubAgentAskRelay;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.tool.ProjectWorkflowTools;
import com.bytechef.automation.configuration.tool.ScriptTools;
import com.bytechef.automation.connection.tool.config.InteractivePickerToolCallbacksImpl;
import com.bytechef.automation.data.table.tool.DataTableTools;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.tool.ComponentTools;
import com.bytechef.platform.component.tool.PropertyOptionsResolver;
import com.bytechef.platform.component.tool.ScopedConnectionGuard;
import com.bytechef.platform.component.tool.TaskTools;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.tool.WorkflowInstructionTools;
import com.bytechef.platform.workflow.tool.WorkflowValidatorTools;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowEditorSubAgentConfigurationTest {

    @Test
    void testTheBuildWorkflowFactoryAddsTheWebSearchToolsOfTheCurrentEnvironment() {
        WorkflowEditorAgentConfiguration workflowEditorAgentConfiguration = realCopilotConfiguration();
        ChatModel chatModel = mock(ChatModel.class);
        ProjectWorkflowTools projectWorkflowTools = mock(ProjectWorkflowTools.class);
        ScriptTools scriptTools = mock(ScriptTools.class);
        TaskTools taskTools = mock(TaskTools.class);
        WebSearchToolCallbackProvider webSearchToolCallbackProvider = mock(WebSearchToolCallbackProvider.class);
        ObjectProvider<SubAgentAskRelay> subAgentAskRelayProvider = emptySubAgentAskRelayProvider();

        when(webSearchToolCallbackProvider.getToolCallbacks(Environment.PRODUCTION.ordinal()))
            .thenReturn(List.of(mock(ToolCallback.class)));

        ChatClient beanClient = workflowEditorAgentConfiguration.workflowEditorBuildSubAgentChatClient(
            chatModel, projectWorkflowTools, taskTools, scriptTools, mock(ComponentTools.class),
            subAgentAskRelayProvider);

        IntelligentToolChatClientFactory factory =
            workflowEditorAgentConfiguration.workflowEditorBuildSubAgentChatClientFactory(
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

    @Test
    void testTheBuildWorkflowSubAgentGetsOnlyTheClusterElementLookups() {
        List<String> toolNames =
            WorkflowEditorAgentConfiguration.clusterElementLookupToolCallbacks(mock(ComponentTools.class))
                .stream()
                .map(toolCallback -> toolCallback.getToolDefinition()
                    .name())
                .toList();

        assertThat(toolNames).containsExactlyInAnyOrder("getAction", "getClusterElement", "searchClusterElements");
    }

    private static WorkflowEditorAgentConfiguration realCopilotConfiguration() {
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
        ScopedConnectionGuard scopedConnectionGuard =
            new ScopedConnectionGuard(connectionListerProvider, emptyProvider());

        return new WorkflowEditorAgentConfiguration(promptResource, promptResource, promptResource,
            workflowValidatorTools, workflowInstructionTools, new InteractivePickerToolCallbacksImpl(
                connectionDefinitionService, workspaceConnectionFacade, componentDefinitionService,
                actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
                mock(ClusterElementDefinitionService.class), mock(ClusterElementDefinitionFacade.class),
                propertyOptionsResolver, connectionListerProvider, scopedConnectionGuard),
            actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
            propertyOptionsResolver, scopedConnectionGuard, emptyDataTableToolsProvider());
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<SubAgentAskRelay> emptySubAgentAskRelayProvider() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DataTableTools> emptyDataTableToolsProvider() {
        return mock(ObjectProvider.class);
    }
}
