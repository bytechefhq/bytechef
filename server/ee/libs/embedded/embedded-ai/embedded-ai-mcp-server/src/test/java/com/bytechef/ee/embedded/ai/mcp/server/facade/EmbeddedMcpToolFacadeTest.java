/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider.Decision;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationToken;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserAccessDeciderImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class EmbeddedMcpToolFacadeTest {

    private static final String CLUSTER_ELEMENT_DESCRIPTION =
        "The POST method submits an entity to the specified resource.";
    private static final String EXTERNAL_USER_ID = "alice";
    private static final long OWN_PROJECT_ID = 7L;
    private static final String TENANT_ID = "000042";

    private final Authentication authentication = new EmbeddedMcpServerApiKeyAuthenticationToken(
        Environment.PRODUCTION.ordinal(), 11L, new User(EXTERNAL_USER_ID, "", List.of()));
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade =
        mock(ClusterElementDefinitionFacade.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final ConnectedUserProjectService connectedUserProjectService = mock(ConnectedUserProjectService.class);
    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService =
        mock(IntegrationInstanceConfigurationWorkflowService.class);
    private final IntegrationService integrationService = mock(IntegrationService.class);
    private final McpComponentService mcpComponentService = mock(McpComponentService.class);
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService =
        mock(McpIntegrationInstanceConfigurationWorkflowService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final EmbeddedMcpToolFacade embeddedMcpToolFacade = new EmbeddedMcpToolFacade(
        clusterElementDefinitionFacade, clusterElementDefinitionService, componentDefinitionService,
        connectedUserService, mock(Evaluator.class), integrationInstanceConfigurationService,
        integrationInstanceConfigurationWorkflowService, mock(IntegrationInstanceService.class),
        mock(IntegrationInstanceWorkflowService.class), integrationService, mock(JobSyncExecutor.class),
        mock(JwtTokenService.class), mcpComponentService, mcpIntegrationInstanceConfigurationWorkflowService,
        mock(McpIntegrationInstanceToolService.class), mcpServerService, mock(PrincipalJobFacade.class),
        "http://localhost:8080", mock(TaskExecutionService.class), mock(TaskFileStorage.class), workflowService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testToolCallReadsTheServerThroughTheUngatedEnabledEmbeddedServersLookup() {
        McpServer mcpServer = new McpServer();

        mcpServer.setId(1L);
        mcpServer.setEnabled(true);

        when(mcpServerService.getEnabledMcpServers(PlatformType.EMBEDDED)).thenReturn(List.of(mcpServer));

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinitionService.getComponentDefinition("httpClient", 1)).thenReturn(componentDefinition);
        when(clusterElementDefinitionFacade.executeTool(eq("httpClient"), eq(1), eq("post"), anyMap(), isNull()))
            .thenReturn("posted");

        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback = getFunctionToolCallback(Map.of());

        assertEquals("\"posted\"", functionToolCallback.call("{}"));
        verify(mcpServerService, never()).getMcpServer(anyLong());
    }

    @Test
    void testToolCallThrowsWhenTheMcpServerIsNotAmongTheEnabledEmbeddedServers() {
        when(mcpServerService.getEnabledMcpServers(PlatformType.EMBEDDED)).thenReturn(List.of());

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinitionService.getComponentDefinition("httpClient", 1)).thenReturn(componentDefinition);

        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback = getFunctionToolCallback(Map.of());

        ToolExecutionException toolExecutionException = assertThrows(
            ToolExecutionException.class, () -> functionToolCallback.call("{}"));

        assertEquals("MCP server is disabled", toolExecutionException.getCause()
            .getMessage());
        verify(mcpServerService, never()).getMcpServer(anyLong());
    }

    // The tool name is optional, so a tool configured without one still has to reach the model under a callable
    // name derived from the component and the cluster element.
    @Test
    void testToolNameFallsBackToTheClusterElement() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of());

        assertEquals("HTTPCLIENT_POST", toolDefinition.name());
    }

    @Test
    void testToolNameUsesTheConfiguredName() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolName", "postThing"));

        assertEquals("postThing", toolDefinition.name());
    }

    // The description is optional, so a tool configured without one is described to the model by the cluster
    // element's own description.
    @Test
    void testToolDescriptionFallsBackToTheClusterElement() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of());

        assertEquals(CLUSTER_ELEMENT_DESCRIPTION, toolDefinition.description());
    }

    @Test
    void testToolDescriptionUsesTheConfiguredDescription() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", "Posts a thing"));

        assertEquals("Posts a thing", toolDefinition.description());
    }

    @Test
    void testToolDescriptionFallsBackWhenTheConfiguredDescriptionIsBlank() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", "   "));

        assertEquals(CLUSTER_ELEMENT_DESCRIPTION, toolDefinition.description());
    }

    private ToolDefinition getToolDefinition(Map<String, Object> parameters) {
        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback = getFunctionToolCallback(parameters);

        return functionToolCallback.getToolDefinition();
    }

    private FunctionToolCallback<Map<String, Object>, Object> getFunctionToolCallback(Map<String, Object> parameters) {
        McpTool mcpTool = new McpTool();

        mcpTool.setMcpComponentId(1L);
        mcpTool.setName("post");
        mcpTool.setParameters(parameters);

        McpComponent mcpComponent = new McpComponent();

        mcpComponent.setComponentName("httpClient");
        mcpComponent.setComponentVersion(1);
        mcpComponent.setMcpServerId(1L);

        when(mcpComponentService.getMcpComponent(1L)).thenReturn(mcpComponent);
        when(connectedUserService.fetchConnectedUser(anyString(), any())).thenReturn(Optional.empty());

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getComponentName()).thenReturn("httpClient");
        when(clusterElementDefinition.getComponentVersion()).thenReturn(1);
        when(clusterElementDefinition.getName()).thenReturn("post");
        when(clusterElementDefinition.getDescription()).thenReturn(CLUSTER_ELEMENT_DESCRIPTION);
        when(clusterElementDefinitionService.getClusterElementDefinition("httpClient", 1, "post"))
            .thenReturn(clusterElementDefinition);

        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback =
            embeddedMcpToolFacade.getFunctionToolCallback(
                mcpTool, "externalUserId", Environment.PRODUCTION, "tenant", null);

        assertNotNull(functionToolCallback);

        return functionToolCallback;
    }

    @Test
    void testClusterElementToolRunsOnAnotherThreadAsTheConnectedUserInTheTenant() throws Exception {
        ConnectedUserAccessDeciderImpl connectedUserAccessDecider = newConnectedUserAccessDecider();
        AtomicReference<Authentication> authenticationInside = new AtomicReference<>();
        AtomicReference<Decision> decisionInside = new AtomicReference<>();
        AtomicReference<String> tenantIdInside = new AtomicReference<>();

        givenEnabledMcpServer();
        givenWorkflowBuilderTool();

        when(clusterElementDefinitionFacade.executeTool(
            eq("embeddedWorkflowBuilder"), eq(1), eq("createConnectedUserWorkflowFromPrompt"), anyMap(), isNull()))
                .thenAnswer(invocation -> {
                    authenticationInside.set(getCurrentAuthentication());
                    tenantIdInside.set(TenantContext.getCurrentTenantId());
                    decisionInside.set(connectedUserAccessDecider.decide(OWN_PROJECT_ID, "Project", "WORKFLOW_CREATE"));

                    return "created";
                });

        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback =
            embeddedMcpToolFacade.getFunctionToolCallback(
                newMcpTool(), EXTERNAL_USER_ID, Environment.PRODUCTION, TENANT_ID, authentication);

        assertNotNull(functionToolCallback);
        assertEquals("\"created\"", callOnAnotherThread(functionToolCallback));

        assertSame(authentication, authenticationInside.get());
        assertEquals(TENANT_ID, tenantIdInside.get());
        assertEquals(Decision.GRANT, decisionInside.get());
        assertNull(getCurrentAuthentication());
    }

    @Test
    void testWorkflowToolRunsOnAnotherThreadAsTheConnectedUserInTheTenant() throws Exception {
        AtomicReference<Authentication> authenticationInside = new AtomicReference<>();
        AtomicReference<String> tenantIdInside = new AtomicReference<>();
        McpServer mcpServer = new McpServer();

        mcpServer.setId(1L);

        when(mcpServerService.getEnabledMcpServers(PlatformType.EMBEDDED)).thenAnswer(invocation -> {
            authenticationInside.set(getCurrentAuthentication());
            tenantIdInside.set(TenantContext.getCurrentTenantId());

            return List.of(mcpServer);
        });

        List<ToolCallback> toolCallbacks = embeddedMcpToolFacade.getFunctionToolCallbacks(
            givenIntegrationWorkflowTool(), EXTERNAL_USER_ID, Environment.PRODUCTION, TENANT_ID, authentication);

        assertEquals(1, toolCallbacks.size());

        callOnAnotherThread(toolCallbacks.getFirst());

        assertSame(authentication, authenticationInside.get());
        assertEquals(TENANT_ID, tenantIdInside.get());
    }

    @Test
    void testToolWithoutACapturedAuthenticationRunsUnderAnEmptySecurityContextAndRestoresThePooledThread()
        throws Exception {

        AtomicReference<Authentication> authenticationInside = new AtomicReference<>();
        AtomicReference<String> tenantIdInside = new AtomicReference<>();

        givenEnabledMcpServer();
        givenWorkflowBuilderTool();

        when(clusterElementDefinitionFacade.executeTool(
            eq("embeddedWorkflowBuilder"), eq(1), eq("createConnectedUserWorkflowFromPrompt"), anyMap(), isNull()))
                .thenAnswer(invocation -> {
                    authenticationInside.set(getCurrentAuthentication());
                    tenantIdInside.set(TenantContext.getCurrentTenantId());

                    return "created";
                });

        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback =
            embeddedMcpToolFacade.getFunctionToolCallback(
                newMcpTool(), EXTERNAL_USER_ID, Environment.PRODUCTION, TENANT_ID, null);

        assertNotNull(functionToolCallback);

        Authentication staleAuthentication = new EmbeddedMcpServerApiKeyAuthenticationToken(
            Environment.PRODUCTION.ordinal(), 99L, new User("stale", "", List.of()));
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            List<Object> pooledThreadStateAfterCall = executorService.submit(() -> {
                SecurityContextHolder.getContext()
                    .setAuthentication(staleAuthentication);
                TenantContext.setCurrentTenantId("stale-tenant");

                functionToolCallback.call("{}");

                return List.<Object>of(getCurrentAuthentication(), TenantContext.getCurrentTenantId());
            })
                .get();

            assertSame(staleAuthentication, pooledThreadStateAfterCall.get(0));
            assertEquals("stale-tenant", pooledThreadStateAfterCall.get(1));
        } finally {
            executorService.shutdown();
        }

        assertNull(authenticationInside.get());
        assertEquals(TENANT_ID, tenantIdInside.get());
    }

    private static String callOnAnotherThread(ToolCallback toolCallback) throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            return executorService.submit(() -> toolCallback.call("{}"))
                .get();
        } finally {
            executorService.shutdown();
        }
    }

    private static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext()
            .getAuthentication();
    }

    private void givenEnabledMcpServer() {
        McpServer mcpServer = new McpServer();

        mcpServer.setId(1L);

        when(mcpServerService.getEnabledMcpServers(PlatformType.EMBEDDED)).thenReturn(List.of(mcpServer));
    }

    private McpIntegrationInstanceConfiguration givenIntegrationWorkflowTool() {
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            new McpIntegrationInstanceConfiguration();

        mcpIntegrationInstanceConfiguration.setId(1L);
        mcpIntegrationInstanceConfiguration.setMcpServerId(1L);

        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            new McpIntegrationInstanceConfigurationWorkflow();

        mcpIntegrationInstanceConfigurationWorkflow.setId(1L);
        mcpIntegrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationWorkflowId(1L);
        mcpIntegrationInstanceConfigurationWorkflow.setParameters(Map.of());

        when(
            mcpIntegrationInstanceConfigurationWorkflowService
                .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(1L))
                    .thenReturn(List.of(mcpIntegrationInstanceConfigurationWorkflow));

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            mock(IntegrationInstanceConfigurationWorkflow.class);

        when(integrationInstanceConfigurationWorkflow.isEnabled()).thenReturn(true);
        when(integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId()).thenReturn(1L);
        when(integrationInstanceConfigurationWorkflow.getWorkflowId()).thenReturn("workflow-1");
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(1L))
            .thenReturn(integrationInstanceConfigurationWorkflow);

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            mock(IntegrationInstanceConfiguration.class);

        when(integrationInstanceConfiguration.getIntegrationId()).thenReturn(1L);
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(1L))
            .thenReturn(integrationInstanceConfiguration);

        Integration integration = mock(Integration.class);

        when(integration.getComponentName()).thenReturn("googleMail");
        when(integration.getId()).thenReturn(1L);
        when(integrationService.getIntegration(1L)).thenReturn(integration);

        WorkflowTrigger workflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "parameters", Map.of(), "type", "workflow/v1/newWorkflowCall"));

        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn("Send Email");
        when(workflow.getExtensions(anyString(), eq(WorkflowTrigger.class), any()))
            .thenReturn(List.of(workflowTrigger));
        when(workflowService.getWorkflow("workflow-1")).thenReturn(workflow);

        return mcpIntegrationInstanceConfiguration;
    }

    private void givenWorkflowBuilderTool() {
        McpComponent mcpComponent = new McpComponent();

        mcpComponent.setComponentName("embeddedWorkflowBuilder");
        mcpComponent.setComponentVersion(1);
        mcpComponent.setMcpServerId(1L);

        when(mcpComponentService.getMcpComponent(1L)).thenReturn(mcpComponent);

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getComponentName()).thenReturn("embeddedWorkflowBuilder");
        when(clusterElementDefinition.getComponentVersion()).thenReturn(1);
        when(clusterElementDefinition.getName()).thenReturn("createConnectedUserWorkflowFromPrompt");
        when(clusterElementDefinitionService.getClusterElementDefinition(
            "embeddedWorkflowBuilder", 1, "createConnectedUserWorkflowFromPrompt"))
                .thenReturn(clusterElementDefinition);
        when(componentDefinitionService.getComponentDefinition("embeddedWorkflowBuilder", 1))
            .thenReturn(mock(ComponentDefinition.class));
    }

    private ConnectedUserAccessDeciderImpl newConnectedUserAccessDecider() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setProjectId(OWN_PROJECT_ID);

        when(connectedUserProjectService.fetchConnectUserProject(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUserProject));

        ResourceOwnershipResolver projectOwnershipResolver = mock(ResourceOwnershipResolver.class);

        when(projectOwnershipResolver.resourceType()).thenReturn("Project");
        when(projectOwnershipResolver.resolveProjectId(OWN_PROJECT_ID)).thenReturn(OptionalLong.of(OWN_PROJECT_ID));

        return new ConnectedUserAccessDeciderImpl(
            mock(AutomationWorkflowProjectFacade.class), mock(ConnectedUserConnectionService.class),
            connectedUserProjectService, mock(IntegrationInstanceService.class), mock(ProjectService.class),
            mock(ProjectWorkflowService.class), List.of(), List.of(projectOwnershipResolver));
    }

    private static McpTool newMcpTool() {
        McpTool mcpTool = new McpTool();

        mcpTool.setMcpComponentId(1L);
        mcpTool.setName("createConnectedUserWorkflowFromPrompt");
        mcpTool.setParameters(Map.of());

        return mcpTool;
    }
}
