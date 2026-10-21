/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopeResolver;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.workflow.CallableWorkflowChecker;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AppLoopMcpContributorConfigurationTest {

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    private final AppLoopMcpContributorConfiguration configuration = new AppLoopMcpContributorConfiguration();

    @Test
    void testContributesEightWrappedTools() {
        McpServerToolCallbackContributor contributor = configuration.appLoopMcpContributor(
            present(mock(ApiCollectionFacade.class)), present(mock(AppFacade.class)),
            present(mock(AppWorkflowFacade.class)), present(mock(CallableWorkflowChecker.class)),
            present(mock(ComponentConnectionFacade.class)), present(mock(ProjectDeploymentFacade.class)),
            present(mock(ProjectFacade.class)), present(mock(ProjectWorkflowFacade.class)),
            present(mock(WorkspaceDataTableFacade.class)), mock(WorkspaceScopeResolver.class));

        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder(
                "deployProject", "listConnections", "listAppCallableWorkflows", "createAppApiCollection",
                "createAppApiEndpoint", "updateAppApiCollectionVersion", "listAppApiCollections",
                "createAppDataTable");
        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }

    @Test
    void testDeployProjectSchemaRequiresEveryConnectionChoiceField() {
        McpServerToolCallbackContributor contributor = configuration.appLoopMcpContributor(
            present(mock(ApiCollectionFacade.class)), present(mock(AppFacade.class)),
            present(mock(AppWorkflowFacade.class)), present(mock(CallableWorkflowChecker.class)),
            present(mock(ComponentConnectionFacade.class)), present(mock(ProjectDeploymentFacade.class)),
            present(mock(ProjectFacade.class)), present(mock(ProjectWorkflowFacade.class)),
            present(mock(WorkspaceDataTableFacade.class)), mock(WorkspaceScopeResolver.class));

        ToolCallback deployProjectToolCallback = contributor.getToolCallbacks()
            .stream()
            .filter(toolCallback -> Objects.equals(
                toolCallback.getToolDefinition()
                    .name(),
                "deployProject"))
            .findFirst()
            .orElseThrow();

        JsonNode connectionChoiceSchemaNode = JSON_MAPPER.readTree(
            deployProjectToolCallback.getToolDefinition()
                .inputSchema())
            .at("/properties/connections/items");

        assertThat(connectionChoiceSchemaNode.get("required"))
            .extracting(JsonNode::asString)
            .containsExactlyInAnyOrder("workflowUuid", "workflowNodeName", "key", "connectionId");
    }

    @Test
    void testContributesNothingWithoutTheAppModule() {
        McpServerToolCallbackContributor contributor = configuration.appLoopMcpContributor(
            present(mock(ApiCollectionFacade.class)), absent(), absent(), absent(),
            present(mock(ComponentConnectionFacade.class)), present(mock(ProjectDeploymentFacade.class)),
            present(mock(ProjectFacade.class)), present(mock(ProjectWorkflowFacade.class)),
            present(mock(WorkspaceDataTableFacade.class)), mock(WorkspaceScopeResolver.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void testContributesOnlyTheDeployToolsWithoutApiPlatformOrDataTables() {
        McpServerToolCallbackContributor contributor = configuration.appLoopMcpContributor(
            absent(), present(mock(AppFacade.class)), present(mock(AppWorkflowFacade.class)),
            present(mock(CallableWorkflowChecker.class)), present(mock(ComponentConnectionFacade.class)),
            present(mock(ProjectDeploymentFacade.class)), present(mock(ProjectFacade.class)),
            present(mock(ProjectWorkflowFacade.class)), absent(), mock(WorkspaceScopeResolver.class));

        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder("deployProject", "listConnections", "listAppCallableWorkflows");
    }

    @Test
    void testEndpointConnectionChoiceSchemaRequiresFields() {
        McpServerToolCallbackContributor contributor = configuration.appLoopMcpContributor(
            present(mock(ApiCollectionFacade.class)), present(mock(AppFacade.class)),
            present(mock(AppWorkflowFacade.class)), present(mock(CallableWorkflowChecker.class)),
            present(mock(ComponentConnectionFacade.class)), present(mock(ProjectDeploymentFacade.class)),
            present(mock(ProjectFacade.class)), present(mock(ProjectWorkflowFacade.class)),
            present(mock(WorkspaceDataTableFacade.class)), mock(WorkspaceScopeResolver.class));

        ToolCallback createAppApiEndpointToolCallback = contributor.getToolCallbacks()
            .stream()
            .filter(toolCallback -> Objects.equals(
                toolCallback.getToolDefinition()
                    .name(),
                "createAppApiEndpoint"))
            .findFirst()
            .orElseThrow();

        JsonNode endpointConnectionChoiceSchemaNode = JSON_MAPPER.readTree(
            createAppApiEndpointToolCallback.getToolDefinition()
                .inputSchema())
            .at("/properties/connections/items");

        assertThat(endpointConnectionChoiceSchemaNode.get("required"))
            .extracting(JsonNode::asString)
            .containsExactlyInAnyOrder("workflowNodeName", "key", "connectionId");
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> present(T value) {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(value);

        return objectProvider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> absent() {
        return mock(ObjectProvider.class);
    }
}
