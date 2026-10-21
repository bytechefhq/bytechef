/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopeResolver;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the Apps agent loop's tools on the management MCP server, each wrapped in
 * {@link WorkspaceScopedFlatToolCallback}: {@code deployProject}, {@code listConnections} and
 * {@code listAppCallableWorkflows}, plus the API Platform and data-table tools of {@link AppApiPlatformLoopTools}.
 * Every collaborator is optional: a deployment without the app module, or CE, contributes nothing, and one without the
 * API Platform or data tables contributes only the deploy tools.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class AppLoopMcpContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor appLoopMcpContributor(
        ObjectProvider<ApiCollectionFacade> apiCollectionFacadeProvider, ObjectProvider<AppFacade> appFacadeProvider,
        ObjectProvider<AppWorkflowFacade> appWorkflowFacadeProvider,
        ObjectProvider<CallableWorkflowChecker> callableWorkflowCheckerProvider,
        ObjectProvider<ComponentConnectionFacade> componentConnectionFacadeProvider,
        ObjectProvider<ProjectDeploymentFacade> projectDeploymentFacadeProvider,
        ObjectProvider<ProjectFacade> projectFacadeProvider,
        ObjectProvider<ProjectWorkflowFacade> projectWorkflowFacadeProvider,
        ObjectProvider<WorkspaceDataTableFacade> workspaceDataTableFacadeProvider,
        WorkspaceScopeResolver workspaceScopeResolver) {

        return () -> {
            AppFacade appFacade = appFacadeProvider.getIfAvailable();
            AppWorkflowFacade appWorkflowFacade = appWorkflowFacadeProvider.getIfAvailable();
            CallableWorkflowChecker callableWorkflowChecker = callableWorkflowCheckerProvider.getIfAvailable();
            ProjectDeploymentFacade projectDeploymentFacade = projectDeploymentFacadeProvider.getIfAvailable();
            ProjectFacade projectFacade = projectFacadeProvider.getIfAvailable();
            ProjectWorkflowFacade projectWorkflowFacade = projectWorkflowFacadeProvider.getIfAvailable();

            if (appFacade == null || appWorkflowFacade == null || callableWorkflowChecker == null ||
                projectDeploymentFacade == null || projectFacade == null || projectWorkflowFacade == null) {

                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            addToolCallbacks(
                toolCallbacks,
                new AppLoopTools(
                    appFacade, appWorkflowFacade, callableWorkflowChecker, projectDeploymentFacade, projectFacade,
                    projectWorkflowFacade),
                workspaceScopeResolver);

            ApiCollectionFacade apiCollectionFacade = apiCollectionFacadeProvider.getIfAvailable();
            ComponentConnectionFacade componentConnectionFacade = componentConnectionFacadeProvider.getIfAvailable();
            WorkspaceDataTableFacade workspaceDataTableFacade = workspaceDataTableFacadeProvider.getIfAvailable();

            if (apiCollectionFacade != null && componentConnectionFacade != null && workspaceDataTableFacade != null) {
                addToolCallbacks(
                    toolCallbacks,
                    new AppApiPlatformLoopTools(
                        apiCollectionFacade, appFacade, componentConnectionFacade, projectDeploymentFacade,
                        projectFacade, projectWorkflowFacade, workspaceDataTableFacade),
                    workspaceScopeResolver);
            }

            return toolCallbacks;
        };
    }

    private static void addToolCallbacks(
        List<ToolCallback> toolCallbacks, Object toolObject, WorkspaceScopeResolver workspaceScopeResolver) {

        for (ToolCallback toolCallback : ToolCallbacks.from(toolObject)) {
            toolCallbacks.add(new WorkspaceScopedFlatToolCallback(toolCallback, workspaceScopeResolver));
        }
    }
}
