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

package com.bytechef.automation.connection.tool.config;

import com.bytechef.ai.copilot.tool.ToolStateVisibilityMetrics;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceConnectionScopedToolCallback;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.connection.tool.ConnectionTools;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.tool.ComponentTools;
import com.bytechef.platform.component.tool.LookupComponentPropertyOptionsToolCallback;
import com.bytechef.platform.component.tool.PropertyOptionsResolver;
import com.bytechef.platform.component.tool.ScopedConnectionGuard;
import com.bytechef.platform.component.tool.TaskTools;
import com.bytechef.platform.workflow.tool.WorkflowInstructionTools;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkflowAuthoringMcpContributorConfiguration {

    private static final Set<String> EXCLUDED_AUTHORING_TOOL_NAMES = Set.of("validateTask");

    @Bean
    McpServerToolCallbackContributor workflowAuthoringMcpContributor(
        ObjectProvider<ComponentTools> componentToolsProvider, ObjectProvider<TaskTools> taskToolsProvider,
        ObjectProvider<WorkflowInstructionTools> workflowInstructionToolsProvider,
        ObjectProvider<ConnectionTools> connectionToolsProvider,
        ObjectProvider<ActionDefinitionService> actionDefinitionServiceProvider,
        ObjectProvider<ActionDefinitionFacade> actionDefinitionFacadeProvider,
        ObjectProvider<TriggerDefinitionService> triggerDefinitionServiceProvider,
        ObjectProvider<TriggerDefinitionFacade> triggerDefinitionFacadeProvider,
        ObjectProvider<PropertyOptionsResolver> propertyOptionsResolverProvider,
        ObjectProvider<ScopedConnectionGuard> scopedConnectionGuardProvider,
        ObjectProvider<WorkspaceConnectionFacade> workspaceConnectionFacadeProvider, WorkspaceFacade workspaceFacade) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            ComponentTools componentTools = componentToolsProvider.getIfAvailable();
            TaskTools taskTools = taskToolsProvider.getIfAvailable();
            WorkflowInstructionTools workflowInstructionTools = workflowInstructionToolsProvider.getIfAvailable();

            if (componentTools != null && taskTools != null && workflowInstructionTools != null) {
                for (ToolCallback toolCallback : ToolCallbacks.from(
                    componentTools, taskTools, workflowInstructionTools)) {

                    ToolDefinition toolDefinition = toolCallback.getToolDefinition();

                    if (!EXCLUDED_AUTHORING_TOOL_NAMES.contains(toolDefinition.name())) {
                        toolCallbacks.add(toolCallback);
                    }
                }
            }

            ConnectionTools connectionTools = connectionToolsProvider.getIfAvailable();

            if (connectionTools != null) {
                for (ToolCallback toolCallback : ToolCallbacks.from(connectionTools)) {
                    toolCallbacks.add(new WorkspaceScopedFlatToolCallback(toolCallback, workspaceFacade));
                }
            }

            ActionDefinitionService actionDefinitionService = actionDefinitionServiceProvider.getIfAvailable();
            ActionDefinitionFacade actionDefinitionFacade = actionDefinitionFacadeProvider.getIfAvailable();
            TriggerDefinitionService triggerDefinitionService = triggerDefinitionServiceProvider.getIfAvailable();
            TriggerDefinitionFacade triggerDefinitionFacade = triggerDefinitionFacadeProvider.getIfAvailable();
            PropertyOptionsResolver propertyOptionsResolver = propertyOptionsResolverProvider.getIfAvailable();
            ScopedConnectionGuard scopedConnectionGuard = scopedConnectionGuardProvider.getIfAvailable();
            WorkspaceConnectionFacade workspaceConnectionFacade = workspaceConnectionFacadeProvider.getIfAvailable();

            if (actionDefinitionService != null && actionDefinitionFacade != null && triggerDefinitionService != null &&
                triggerDefinitionFacade != null && propertyOptionsResolver != null && scopedConnectionGuard != null &&
                workspaceConnectionFacade != null) {

                ToolCallback lookupToolCallback = new LookupComponentPropertyOptionsToolCallback(
                    actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
                    propertyOptionsResolver, ToolStateVisibilityMetrics.NOOP, scopedConnectionGuard);

                toolCallbacks.add(
                    new WorkspaceScopedFlatToolCallback(
                        new WorkspaceConnectionScopedToolCallback(lookupToolCallback, workspaceConnectionFacade),
                        workspaceFacade));
            }

            return toolCallbacks;
        };
    }
}
