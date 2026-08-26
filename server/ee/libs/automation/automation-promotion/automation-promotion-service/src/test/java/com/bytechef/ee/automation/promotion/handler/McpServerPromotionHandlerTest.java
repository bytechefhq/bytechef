/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.connection.ConnectionEnvironmentMapper;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SyncResult;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.ee.automation.promotion.dto.PromotionProjectPreview;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class McpServerPromotionHandlerTest {

    private static final long COMPONENT_CONNECTION_ID = 33L;
    private static final long CREATED_COMPONENT_ID = 510L;
    private static final long OTHER_WORKSPACE_ID = 6L;
    private static final long PROJECT_ID = 42L;
    private static final String PUBLIC_URL = "https://app.bytechef.test";
    private static final String SECRET_KEY = "secret-key";
    private static final String SERVER_UUID = "5f2c9a10-4444-4b1c-9d2e-3f4a5b6c7d8e";
    private static final long SOURCE_COMPONENT_ID = 400L;
    private static final long SOURCE_CONNECTION_ID = 11L;
    private static final long SOURCE_ID = 1L;
    private static final long SOURCE_MCP_PROJECT_ID = 10L;
    private static final long SOURCE_MCP_PROJECT_WORKFLOW_ID = 220L;
    private static final Map<String, Object> SOURCE_PARAMETERS = Map.of("toolName", "getOrders");
    private static final long SOURCE_PROJECT_DEPLOYMENT_ID = 200L;
    private static final long SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID = 210L;
    private static final int SOURCE_PROJECT_VERSION = 3;
    private static final String SOURCE_SERVER_NAME = "Orders MCP";
    private static final long SOURCE_TAG_ID = 9L;
    private static final long SOURCE_TOOL_ID = 600L;
    private static final String SOURCE_WORKFLOW_ID = "wf-source-v3";
    private static final long TARGET_COMPONENT_CONNECTION_ID = 44L;
    private static final long TARGET_COMPONENT_ID = 500L;
    private static final long TARGET_CONNECTION_ID = 22L;
    private static final long TARGET_ID = 100L;
    private static final long TARGET_MCP_PROJECT_ID = 20L;
    private static final long TARGET_MCP_PROJECT_WORKFLOW_ID = 320L;
    private static final long TARGET_PROJECT_DEPLOYMENT_ID = 300L;
    private static final long TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID = 310L;
    private static final int TARGET_PROJECT_VERSION = 2;
    private static final long TARGET_TOOL_ID = 700L;
    private static final String TARGET_WORKFLOW_ID = "wf-target-v2";
    private static final String WORKFLOW_UUID = "9a8b7c6d-2222-4e3f-8a1b-0c9d8e7f6a5b";
    private static final long WRITTEN_TOOL_ID = 800L;
    private static final long WORKSPACE_ID = 5L;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ConnectionEnvironmentMapper connectionEnvironmentMapper;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private McpComponentService mcpComponentService;

    @Mock
    private McpProjectFacade mcpProjectFacade;

    @Mock
    private McpProjectService mcpProjectService;

    @Mock
    private McpProjectWorkflowService mcpProjectWorkflowService;

    @Mock
    private McpServerFacade mcpServerFacade;

    @Mock
    private McpServerService mcpServerService;

    @Mock
    private McpToolService mcpToolService;

    @Mock
    private ProjectDeploymentPromoter projectDeploymentPromoter;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkspaceMcpServerFacade workspaceMcpServerFacade;

    @Mock
    private WorkspaceMcpServerService workspaceMcpServerService;

    private McpServerPromotionHandler mcpServerPromotionHandler;

    @BeforeEach
    void setUp() {
        when(applicationProperties.getPublicUrl()).thenReturn(PUBLIC_URL);

        // The project promoter and the reconciler it drives are exercised for real rather than mocked: the
        // delete-then-sync-then-create ordering they own is the part of this handler most likely to break, and
        // mocks would assert nothing about it.
        mcpServerPromotionHandler = new McpServerPromotionHandler(
            applicationProperties, connectionEnvironmentMapper, connectionService, mcpComponentService,
            mcpProjectFacade, mcpProjectService, mcpProjectWorkflowService, mcpServerFacade, mcpServerService,
            mcpToolService, projectDeploymentPromoter, projectDeploymentService, projectDeploymentWorkflowService,
            projectService,
            new ServerProjectPromoter(
                projectDeploymentPromoter,
                new ProjectWorkflowMappingReconciler(projectDeploymentWorkflowService, projectWorkflowService)),
            workspaceMcpServerFacade, workspaceMcpServerService);
    }

    @Test
    void testGetResourceTypeIsMcpServer() {
        assertThat(mcpServerPromotionHandler.getResourceType()).isEqualTo(PromotionResourceType.MCP_SERVER);
    }

    @Test
    void testPreviewOnFreshTargetReportsCreate() {
        stubSource();
        stubSourceProjects(sourceMcpProject(SOURCE_MCP_PROJECT_ID));
        stubSourceBindings();
        stubProject();
        stubSourceComponents(sourceMcpComponent(COMPONENT_CONNECTION_ID));
        stubNoExistingTarget();
        stubSuggestedMappings(Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID));

        when(connectionService.getConnections(List.of(SOURCE_CONNECTION_ID, COMPONENT_CONNECTION_ID)))
            .thenReturn(
                List.of(connection(SOURCE_CONNECTION_ID, "Sheets"), connection(COMPONENT_CONNECTION_ID, "Api")));

        EnvironmentPromotionPreview preview = mcpServerPromotionHandler.preview(SOURCE_ID, Environment.STAGING);

        assertThat(preview.resourceType()).isEqualTo(PromotionResourceType.MCP_SERVER);
        assertThat(preview.sourceEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(preview.existingTargetId()).isNull();
        assertThat(preview.existingTargetName()).isNull();
        assertThat(preview.projects())
            .containsExactly(new PromotionProjectPreview(PROJECT_ID, "Billing", SOURCE_PROJECT_VERSION, null));
        assertThat(preview.warnings())
            .contains("A new server URL and secret key will be generated for the promoted server.");

        // Both halves of the server are listed: the deployment's workflow-node binding and the component's own
        // connection, the latter labelled by component rather than by workflow node.
        assertThat(preview.connections())
            .containsExactly(
                new PromotionConnectionMapping(
                    SOURCE_CONNECTION_ID, "Sheets", "googleSheets", 1, TARGET_CONNECTION_ID,
                    List.of("Orders › googleSheets_1")),
                new PromotionConnectionMapping(
                    COMPONENT_CONNECTION_ID, "Api", "googleSheets", 1, null, List.of("component:googleSheets")));
    }

    /**
     * The lineage uuid is unique per environment, not per workspace, so a same-uuid server can legitimately be sitting
     * in a workspace this caller was never authorized against. Promoting into it would cross that boundary.
     */
    @Test
    void testPreviewTreatsSameUuidServerInAnotherWorkspaceAsCreate() {
        stubSource();
        stubSourceProjects(sourceMcpProject(SOURCE_MCP_PROJECT_ID));
        stubSourceBindings();
        stubProject();
        stubSourceComponents();
        stubSuggestedMappings(Map.of());

        when(mcpServerService.fetchMcpServer(UUID.fromString(SERVER_UUID), Environment.STAGING))
            .thenReturn(Optional.of(targetMcpServer()));
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(TARGET_ID))
            .thenReturn(Optional.of(OTHER_WORKSPACE_ID));
        when(connectionService.getConnections(List.of(SOURCE_CONNECTION_ID)))
            .thenReturn(List.of(connection(SOURCE_CONNECTION_ID, "Sheets")));

        EnvironmentPromotionPreview preview = mcpServerPromotionHandler.preview(SOURCE_ID, Environment.STAGING);

        assertThat(preview.existingTargetId()).isNull();
        assertThat(preview.projects())
            .containsExactly(new PromotionProjectPreview(PROJECT_ID, "Billing", SOURCE_PROJECT_VERSION, null));
    }

    @Test
    void testPreviewOnExistingTargetReportsTheTargetProjectVersion() {
        stubSource();
        stubSourceProjects(sourceMcpProject(SOURCE_MCP_PROJECT_ID));
        stubSourceBindings();
        stubProject();
        stubSourceComponents();
        stubExistingTarget();
        stubTargetProjects(targetMcpProject(TARGET_MCP_PROJECT_ID));
        stubSuggestedMappings(Map.of());

        when(projectDeploymentPromoter.existingTargetBindings(any(), any()))
            .thenReturn(Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID));
        when(mcpComponentService.getMcpServerMcpComponents(TARGET_ID)).thenReturn(List.of());
        when(connectionService.getConnections(List.of(SOURCE_CONNECTION_ID)))
            .thenReturn(List.of(connection(SOURCE_CONNECTION_ID, "Sheets")));

        EnvironmentPromotionPreview preview = mcpServerPromotionHandler.preview(SOURCE_ID, Environment.STAGING);

        assertThat(preview.existingTargetId()).isEqualTo(TARGET_ID);
        assertThat(preview.existingTargetName()).isEqualTo("Target-owned name");
        assertThat(preview.projects())
            .containsExactly(
                new PromotionProjectPreview(PROJECT_ID, "Billing", SOURCE_PROJECT_VERSION, TARGET_PROJECT_VERSION));

        // What the target already wired outranks the suggestion, so the dialog does not propose re-pointing it.
        assertThat(preview.connections())
            .extracting(PromotionConnectionMapping::suggestedTargetConnectionId)
            .containsExactly(TARGET_CONNECTION_ID);
    }

    @Test
    void testPreviewWarnsWhenTheSourceExposesOneProjectTwice() {
        stubSource();
        stubSourceProjects(sourceMcpProject(SOURCE_MCP_PROJECT_ID), sourceMcpProject(SOURCE_MCP_PROJECT_ID + 1));
        stubSourceBindings();
        stubProject();
        stubSourceComponents();
        stubNoExistingTarget();
        stubSuggestedMappings(Map.of());

        when(connectionService.getConnections(List.of(SOURCE_CONNECTION_ID)))
            .thenReturn(List.of(connection(SOURCE_CONNECTION_ID, "Sheets")));

        EnvironmentPromotionPreview preview = mcpServerPromotionHandler.preview(SOURCE_ID, Environment.STAGING);

        assertThat(preview.warnings())
            .contains(
                "The source MCP server exposes project 42 more than once; entries are matched in ascending id order");
        assertThat(preview.projects()).hasSize(2);
    }

    @Test
    void testPromoteCreatesTheTargetServerDisabledWithTheSourceLineageUuid() {
        stubSource();
        stubSourceProjects(sourceMcpProject(SOURCE_MCP_PROJECT_ID));
        stubSourceBindings();
        stubSourceComponents();
        stubConnectionMappings(Map.of(), Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID));
        stubNoExistingTarget();
        stubTargetServerCreation();
        stubTargetProjectCreation();
        stubSourceProjectDeploymentWorkflows();
        stubTargetProjectDeploymentWorkflows(SOURCE_WORKFLOW_ID);
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, SOURCE_WORKFLOW_ID);
        stubTargetMappings(targetMcpProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID));
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());
        stubSecretKey();

        EnvironmentPromotionResult result =
            mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        verify(workspaceMcpServerFacade).createWorkspaceMcpServer(
            SOURCE_SERVER_NAME, PlatformType.AUTOMATION, Environment.STAGING, false, true, true, WORKSPACE_ID,
            UUID.fromString(SERVER_UUID));
        verify(mcpServerFacade).updateMcpServerTags(TARGET_ID, List.of(new Tag(SOURCE_TAG_ID, "source-tag")));
        verify(mcpProjectFacade).createMcpProject(
            TARGET_ID, PROJECT_ID, SOURCE_PROJECT_VERSION, List.of(SOURCE_WORKFLOW_ID));
        verify(projectDeploymentPromoter).sync(
            any(), any(), eq(Map.of()), eq(Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID)), eq(true));
        verify(mcpProjectWorkflowService).updateParameters(TARGET_MCP_PROJECT_WORKFLOW_ID, SOURCE_PARAMETERS);
        verify(mcpServerService, never()).update(any(McpServer.class));

        assertThat(result.created()).isTrue();
        assertThat(result.targetId()).isEqualTo(TARGET_ID);
        assertThat(result.targetUrl()).isEqualTo(PUBLIC_URL + "/api/automation/" + SECRET_KEY + "/mcp");
    }

    @Test
    void testPromoteCreatesComponentsWithTheSuggestedConnectionAndCarriesToolEnabledFromTheSource() {
        stubSource();
        stubSourceProjects();
        stubSourceComponents(sourceMcpComponent(COMPONENT_CONNECTION_ID));
        stubConnectionMappings(Map.of(), Map.of(COMPONENT_CONNECTION_ID, TARGET_COMPONENT_CONNECTION_ID));
        stubNoExistingTarget();
        stubTargetServerCreation();
        stubSecretKey();

        when(mcpToolService.getMcpComponentMcpTools(SOURCE_COMPONENT_ID))
            .thenReturn(List.of(mcpTool(SOURCE_TOOL_ID, "getOrders", SOURCE_COMPONENT_ID, false)));
        when(mcpServerFacade.create(any(McpComponent.class), anyList()))
            .thenReturn(mcpComponent(CREATED_COMPONENT_ID, TARGET_ID, TARGET_COMPONENT_CONNECTION_ID));
        when(mcpToolService.getMcpComponentMcpTools(CREATED_COMPONENT_ID))
            .thenReturn(List.of(mcpTool(WRITTEN_TOOL_ID, "getOrders", CREATED_COMPONENT_ID, true)));

        EnvironmentPromotionResult result =
            mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        ArgumentCaptor<McpComponent> mcpComponentArgumentCaptor = ArgumentCaptor.forClass(McpComponent.class);
        ArgumentCaptor<List<McpTool>> mcpToolsArgumentCaptor = ArgumentCaptor.captor();

        verify(mcpServerFacade).create(mcpComponentArgumentCaptor.capture(), mcpToolsArgumentCaptor.capture());

        McpComponent createdMcpComponent = mcpComponentArgumentCaptor.getValue();

        assertThat(createdMcpComponent.getComponentName()).isEqualTo("googleSheets");
        assertThat(createdMcpComponent.getMcpServerId()).isEqualTo(TARGET_ID);
        assertThat(createdMcpComponent.getConnectionId()).isEqualTo(TARGET_COMPONENT_CONNECTION_ID);

        assertThat(mcpToolsArgumentCaptor.getValue())
            .extracting(McpTool::getName)
            .containsExactly("getOrders");

        // McpServerFacade writes every tool enabled, so the source's disabled flag only survives if it is re-applied.
        verify(mcpToolService).updateEnabled(WRITTEN_TOOL_ID, false);

        assertThat(result.unresolvedConnectionIds()).isEmpty();
    }

    @Test
    void testPromoteReportsAComponentConnectionThatResolvesToNothing() {
        stubSource();
        stubSourceProjects();
        stubSourceComponents(sourceMcpComponent(COMPONENT_CONNECTION_ID));
        stubConnectionMappings(Map.of(), Map.of());
        stubNoExistingTarget();
        stubTargetServerCreation();
        stubSecretKey();

        when(mcpToolService.getMcpComponentMcpTools(SOURCE_COMPONENT_ID)).thenReturn(List.of());
        when(mcpServerFacade.create(any(McpComponent.class), anyList()))
            .thenReturn(mcpComponent(CREATED_COMPONENT_ID, TARGET_ID, null));
        when(mcpToolService.getMcpComponentMcpTools(CREATED_COMPONENT_ID)).thenReturn(List.of());

        EnvironmentPromotionResult result =
            mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        assertThat(result.unresolvedConnectionIds()).containsExactly(COMPONENT_CONNECTION_ID);
    }

    @Test
    void testPromoteAcceptsAConnectionMappingForAComponentOnlyConnection() {
        stubSource();
        stubSourceProjects();
        stubSourceComponents(sourceMcpComponent(COMPONENT_CONNECTION_ID));
        stubConnectionMappings(Map.of(COMPONENT_CONNECTION_ID, TARGET_COMPONENT_CONNECTION_ID), Map.of());
        stubNoExistingTarget();
        stubTargetServerCreation();
        stubSecretKey();

        when(mcpToolService.getMcpComponentMcpTools(SOURCE_COMPONENT_ID)).thenReturn(List.of());
        when(mcpServerFacade.create(any(McpComponent.class), anyList()))
            .thenReturn(mcpComponent(CREATED_COMPONENT_ID, TARGET_ID, TARGET_COMPONENT_CONNECTION_ID));
        when(mcpToolService.getMcpComponentMcpTools(CREATED_COMPONENT_ID)).thenReturn(List.of());

        mcpServerPromotionHandler.promote(
            SOURCE_ID, Environment.STAGING, Map.of(COMPONENT_CONNECTION_ID, TARGET_COMPONENT_CONNECTION_ID));

        // The scope check unions the deployments' bindings with the components' connections; a component-only
        // connection is inside that union and must not be rejected as foreign.
        verify(connectionEnvironmentMapper).validate(
            WORKSPACE_ID, Environment.STAGING, Map.of(COMPONENT_CONNECTION_ID, TARGET_COMPONENT_CONNECTION_ID));
    }

    @Test
    void testPromoteRejectsConnectionMappingForConnectionTheSourceDoesNotUse() {
        stubSource();
        stubSourceProjects(sourceMcpProject(SOURCE_MCP_PROJECT_ID));
        stubSourceBindings();
        stubSourceComponents();

        assertThatThrownBy(
            () -> mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of(999L, 22L)))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());

        verifyNoInteractions(connectionEnvironmentMapper);
        verifyNoInteractions(workspaceMcpServerFacade);
    }

    @Test
    void testPromoteUpdateLeavesTheTargetServerRowAndItsTagsAlone() {
        stubUpdatePath();
        stubTargetMappings(targetMcpProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID));
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());

        EnvironmentPromotionResult result =
            mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        verify(mcpServerService, never()).update(any(McpServer.class));
        verify(mcpServerFacade, never()).updateMcpServerTags(anyLong(), anyList());
        verify(workspaceMcpServerFacade, never()).createWorkspaceMcpServer(
            any(), any(), any(), any(), any(), any(), any(), any());
        verify(projectDeploymentPromoter).sync(any(), any(), anyMap(), anyMap(), eq(false));

        assertThat(result.created()).isFalse();
        assertThat(result.targetId()).isEqualTo(TARGET_ID);
    }

    /**
     * {@code mcp_project_workflow} holds a foreign key to {@code project_deployment_workflow} and the sync deletes the
     * deployment-workflow rows the source no longer exposes, so the child has to go first. The selection-level facade
     * method must not be involved at all — it reconciles deployment-workflow rows itself, blind to the sync's mapping.
     */
    @Test
    void testPromoteUpdateDeletesStaleMappingRowsBeforeSyncingAndNeverCallsUpdateMcpProject() {
        stubUpdatePath();
        stubTargetMappings(
            targetMcpProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID),
            targetMcpProjectWorkflow(TARGET_MCP_PROJECT_WORKFLOW_ID + 1, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID + 1));
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());

        mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        InOrder inOrder = inOrder(mcpProjectWorkflowService, projectDeploymentPromoter);

        inOrder.verify(mcpProjectWorkflowService)
            .delete(TARGET_MCP_PROJECT_WORKFLOW_ID + 1);
        inOrder.verify(projectDeploymentPromoter)
            .sync(any(), any(), anyMap(), anyMap(), eq(false));
        inOrder.verify(mcpProjectWorkflowService)
            .updateParameters(TARGET_MCP_PROJECT_WORKFLOW_ID, SOURCE_PARAMETERS);

        verify(mcpProjectWorkflowService, never()).delete(TARGET_MCP_PROJECT_WORKFLOW_ID);
        verify(mcpProjectFacade, never()).updateMcpProject(anyLong(), anyList());
    }

    @Test
    void testPromoteUpdateCreatesAMappingRowForAWorkflowTheTargetDoesNotExposeYet() {
        stubUpdatePath();
        stubTargetMappings();
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());

        when(mcpProjectWorkflowService.create(TARGET_MCP_PROJECT_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(targetMcpProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID));

        mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        verify(mcpProjectWorkflowService).create(TARGET_MCP_PROJECT_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID);
        verify(mcpProjectWorkflowService).updateParameters(TARGET_MCP_PROJECT_WORKFLOW_ID, SOURCE_PARAMETERS);
    }

    @Test
    void testPromoteUpdateDeletesATargetProjectTheSourceNoLongerExposes() {
        stubSource();
        stubSourceProjects();
        stubSourceComponents();
        stubConnectionMappings(Map.of(), Map.of());
        stubExistingTarget();
        stubTargetProjects(targetMcpProject(TARGET_MCP_PROJECT_ID));
        stubSecretKey();

        when(mcpComponentService.getMcpServerMcpComponents(TARGET_ID)).thenReturn(List.of());

        mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        verify(mcpProjectFacade).deleteMcpProject(TARGET_MCP_PROJECT_ID);
    }

    @Test
    void testPromoteUpdateCreatesATargetProjectForASourceProjectTheTargetIsMissing() {
        stubSource();
        stubSourceProjects(sourceMcpProject(SOURCE_MCP_PROJECT_ID));
        stubSourceBindings();
        stubSourceComponents();
        stubConnectionMappings(Map.of(), Map.of());
        stubExistingTarget();
        stubTargetProjects();
        stubTargetProjectCreation();
        stubSourceProjectDeploymentWorkflows();
        stubTargetProjectDeploymentWorkflows(SOURCE_WORKFLOW_ID);
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, SOURCE_WORKFLOW_ID);
        stubTargetMappings(targetMcpProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID));
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());
        stubSecretKey();

        when(mcpComponentService.getMcpServerMcpComponents(TARGET_ID)).thenReturn(List.of());

        mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        verify(mcpProjectFacade).createMcpProject(
            TARGET_ID, PROJECT_ID, SOURCE_PROJECT_VERSION, List.of(SOURCE_WORKFLOW_ID));

        // A project the target had to be given is new even though the server itself is not, so it adopts the
        // source's inputs and enabled flags rather than preserving values it never had.
        verify(projectDeploymentPromoter).sync(any(), any(), anyMap(), anyMap(), eq(true));
    }

    @Test
    void testPromoteUpdateKeepsTheTargetComponentConnectionAndItsToolEnabledFlags() {
        stubSource();
        stubSourceProjects();
        stubSourceComponents(sourceMcpComponent(COMPONENT_CONNECTION_ID));
        stubConnectionMappings(Map.of(), Map.of(COMPONENT_CONNECTION_ID, 999L));
        stubExistingTarget();
        stubTargetProjects();
        stubSecretKey();

        when(mcpComponentService.getMcpServerMcpComponents(TARGET_ID))
            .thenReturn(
                List.of(
                    mcpComponent(TARGET_COMPONENT_ID, TARGET_ID, TARGET_COMPONENT_CONNECTION_ID),
                    staleTargetMcpComponent()));
        when(mcpToolService.getMcpComponentMcpTools(SOURCE_COMPONENT_ID))
            .thenReturn(List.of(mcpTool(SOURCE_TOOL_ID, "getOrders", SOURCE_COMPONENT_ID, true)));
        when(mcpToolService.getMcpComponentMcpTools(TARGET_COMPONENT_ID))
            .thenReturn(
                List.of(mcpTool(TARGET_TOOL_ID, "getOrders", TARGET_COMPONENT_ID, false)),
                List.of(mcpTool(WRITTEN_TOOL_ID, "getOrders", TARGET_COMPONENT_ID, true)));
        when(mcpServerFacade.update(any(McpComponent.class), anyList()))
            .thenReturn(mcpComponent(TARGET_COMPONENT_ID, TARGET_ID, TARGET_COMPONENT_CONNECTION_ID));

        mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        ArgumentCaptor<McpComponent> mcpComponentArgumentCaptor = ArgumentCaptor.forClass(McpComponent.class);

        verify(mcpServerFacade).update(mcpComponentArgumentCaptor.capture(), anyList());

        McpComponent updatedMcpComponent = mcpComponentArgumentCaptor.getValue();

        assertThat(updatedMcpComponent.getId()).isEqualTo(TARGET_COMPONENT_ID);
        assertThat(updatedMcpComponent.getConnectionId()).isEqualTo(TARGET_COMPONENT_CONNECTION_ID);

        verify(mcpToolService).updateEnabled(WRITTEN_TOOL_ID, false);
        verify(mcpServerFacade).deleteMcpComponent(TARGET_COMPONENT_ID + 1);
    }

    @Test
    void testPromoteRejectsCreatingACounterpartWhoseNameAnotherLineageAlreadyHolds() {
        stubSource();
        stubSourceProjects();
        stubSourceComponents();
        stubConnectionMappings(Map.of(), Map.of());
        stubNoExistingTarget();

        when(
            mcpServerService.existsByNameAndEnvironment(
                SOURCE_SERVER_NAME, Environment.STAGING, UUID.fromString(SERVER_UUID))).thenReturn(true);

        assertThatThrownBy(() -> mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of()))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT.getErrorKey());

        verify(workspaceMcpServerFacade, never()).createWorkspaceMcpServer(
            any(), any(), any(), any(), any(), any(), any(), any());
    }

    /**
     * The lineage uuid is excluded from the conflict check, so a counterpart of the source's own lineage never reads as
     * somebody else's name. Were it counted, promotion would refuse to create a counterpart it is entitled to.
     */
    @Test
    void testPromoteExcludesTheSourceLineageFromTheNameConflictCheck() {
        stubSource();
        stubSourceProjects();
        stubSourceComponents();
        stubConnectionMappings(Map.of(), Map.of());
        stubNoExistingTarget();
        stubTargetServerCreation();
        stubSecretKey();

        EnvironmentPromotionResult result =
            mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        verify(mcpServerService).existsByNameAndEnvironment(
            SOURCE_SERVER_NAME, Environment.STAGING, UUID.fromString(SERVER_UUID));

        assertThat(result.created()).isTrue();
    }

    /**
     * The name is only contested on the create path; a re-promotion writes into a counterpart that already owns its
     * name, so the check must not run at all there.
     */
    @Test
    void testPromoteDoesNotCheckTheNameWhenUpdatingAnExistingCounterpart() {
        stubUpdatePath();
        stubTargetMappings(targetMcpProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID));
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());

        mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        verify(mcpServerService, never()).existsByNameAndEnvironment(any(), any(), any());
    }

    @Test
    void testPromoteRejectsPromotionIntoTheSameEnvironment() {
        when(mcpServerService.getMcpServer(SOURCE_ID)).thenReturn(sourceMcpServer());

        assertThatThrownBy(() -> mcpServerPromotionHandler.promote(SOURCE_ID, Environment.DEVELOPMENT, Map.of()))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey());

        verifyNoInteractions(workspaceMcpServerFacade);
    }

    @Test
    void testPromoteValidatesEveryProjectBeforeWritingAnything() {
        stubSource();
        stubSourceProjects(sourceMcpProject(SOURCE_MCP_PROJECT_ID));
        stubSourceBindings();
        stubSourceComponents();
        stubConnectionMappings(Map.of(), Map.of());
        stubNoExistingTarget();
        stubTargetServerCreation();
        stubTargetProjectCreation();
        stubSourceProjectDeploymentWorkflows();
        stubTargetProjectDeploymentWorkflows(SOURCE_WORKFLOW_ID);
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, SOURCE_WORKFLOW_ID);
        stubTargetMappings(targetMcpProjectWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID));
        stubSync(Map.of(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID), List.of());
        stubSecretKey();

        mcpServerPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        InOrder inOrder = inOrder(projectDeploymentPromoter, workspaceMcpServerFacade);

        inOrder.verify(projectDeploymentPromoter)
            .validatePromotable(PROJECT_ID, SOURCE_PROJECT_VERSION);
        inOrder.verify(workspaceMcpServerFacade)
            .createWorkspaceMcpServer(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    /**
     * The whole update-path fixture: one source project on version 3 matched to a target project still on version 2,
     * the two running different per-version workflow ids under one lineage uuid.
     */
    private void stubUpdatePath() {
        stubSource();
        stubSourceProjects(sourceMcpProject(SOURCE_MCP_PROJECT_ID));
        stubSourceBindings();
        stubSourceComponents();
        stubConnectionMappings(Map.of(), Map.of());
        stubExistingTarget();
        stubTargetProjects(targetMcpProject(TARGET_MCP_PROJECT_ID));
        stubSourceProjectDeploymentWorkflows();
        stubTargetProjectDeploymentWorkflows(TARGET_WORKFLOW_ID);
        stubProjectWorkflows(SOURCE_PROJECT_VERSION, SOURCE_WORKFLOW_ID);
        stubProjectWorkflows(TARGET_PROJECT_VERSION, TARGET_WORKFLOW_ID);
        stubSecretKey();

        when(mcpComponentService.getMcpServerMcpComponents(TARGET_ID)).thenReturn(List.of());
    }

    private void stubConnectionMappings(Map<Long, Long> requestedMappings, Map<Long, Long> suggestedMappings) {
        when(connectionEnvironmentMapper.validate(anyLong(), any(Environment.class), anyMap()))
            .thenReturn(requestedMappings);

        stubSuggestedMappings(suggestedMappings);
    }

    private void stubExistingTarget() {
        when(mcpServerService.fetchMcpServer(UUID.fromString(SERVER_UUID), Environment.STAGING))
            .thenReturn(Optional.of(targetMcpServer()));
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(TARGET_ID)).thenReturn(Optional.of(WORKSPACE_ID));
    }

    private void stubNoExistingTarget() {
        when(mcpServerService.fetchMcpServer(UUID.fromString(SERVER_UUID), Environment.STAGING))
            .thenReturn(Optional.empty());
    }

    private void stubProject() {
        when(projectService.getProject(PROJECT_ID)).thenReturn(project());
    }

    private void stubProjectWorkflows(int projectVersion, String workflowId) {
        ProjectWorkflow projectWorkflow =
            new ProjectWorkflow(PROJECT_ID, projectVersion, workflowId, UUID.fromString(WORKFLOW_UUID));

        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, projectVersion))
            .thenReturn(List.of(projectWorkflow));
    }

    private void stubSecretKey() {
        when(mcpServerService.getMcpServerSecretKey(TARGET_ID)).thenReturn(SECRET_KEY);
    }

    private void stubSource() {
        when(mcpServerService.getMcpServer(SOURCE_ID)).thenReturn(sourceMcpServer());
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(SOURCE_ID)).thenReturn(Optional.of(WORKSPACE_ID));
    }

    private void stubSourceBindings() {
        when(projectDeploymentPromoter.collectSourceBindings(any(ProjectDeployment.class)))
            .thenReturn(
                List.of(
                    new SourceBinding(
                        WORKFLOW_UUID, "Orders", "googleSheets_1", "connectionId", SOURCE_CONNECTION_ID)));
    }

    private void stubSourceComponents(McpComponent... mcpComponents) {
        when(mcpComponentService.getMcpServerMcpComponents(SOURCE_ID)).thenReturn(List.of(mcpComponents));
    }

    private void stubSourceProjectDeploymentWorkflows() {
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(SOURCE_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(projectDeploymentWorkflow(SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID, SOURCE_WORKFLOW_ID)));
    }

    private void stubSourceProjects(McpProject... mcpProjects) {
        when(mcpProjectService.getMcpServerMcpProjects(SOURCE_ID)).thenReturn(List.of(mcpProjects));

        if (mcpProjects.length > 0) {
            when(projectDeploymentService.getProjectDeployment(SOURCE_PROJECT_DEPLOYMENT_ID))
                .thenReturn(
                    projectDeployment(
                        SOURCE_PROJECT_DEPLOYMENT_ID, SOURCE_PROJECT_VERSION, Environment.DEVELOPMENT));
        }

        for (McpProject mcpProject : mcpProjects) {
            when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProject.getId()))
                .thenReturn(
                    List.of(
                        sourceMcpProjectWorkflow(mcpProject.getId(), SOURCE_PROJECT_DEPLOYMENT_WORKFLOW_ID)));
        }
    }

    private void stubSuggestedMappings(Map<Long, Long> suggestedMappings) {
        when(connectionEnvironmentMapper.suggest(anyLong(), anySet(), any(Environment.class)))
            .thenReturn(suggestedMappings);
    }

    private void stubSync(Map<Long, Long> workflowIdMapping, List<Long> unresolvedConnectionIds) {
        when(projectDeploymentPromoter.sync(any(), any(), anyMap(), anyMap(), any(Boolean.class)))
            .thenReturn(new SyncResult(workflowIdMapping, unresolvedConnectionIds, List.of()));
    }

    private void stubTargetMappings(McpProjectWorkflow... mcpProjectWorkflows) {
        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(TARGET_MCP_PROJECT_ID))
            .thenReturn(List.of(mcpProjectWorkflows));
    }

    private void stubTargetProjectCreation() {
        when(
            mcpProjectFacade.createMcpProject(
                TARGET_ID, PROJECT_ID, SOURCE_PROJECT_VERSION, List.of(SOURCE_WORKFLOW_ID)))
                    .thenReturn(targetMcpProject(TARGET_MCP_PROJECT_ID));
        when(projectDeploymentService.getProjectDeployment(TARGET_PROJECT_DEPLOYMENT_ID))
            .thenReturn(projectDeployment(TARGET_PROJECT_DEPLOYMENT_ID, SOURCE_PROJECT_VERSION, Environment.STAGING));
    }

    private void stubTargetProjectDeploymentWorkflows(String workflowId) {
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(TARGET_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(projectDeploymentWorkflow(TARGET_PROJECT_DEPLOYMENT_WORKFLOW_ID, workflowId)));
    }

    private void stubTargetProjects(McpProject... mcpProjects) {
        when(mcpProjectService.getMcpServerMcpProjects(TARGET_ID)).thenReturn(List.of(mcpProjects));

        if (mcpProjects.length > 0) {
            when(projectDeploymentService.getProjectDeployment(TARGET_PROJECT_DEPLOYMENT_ID))
                .thenReturn(
                    projectDeployment(TARGET_PROJECT_DEPLOYMENT_ID, TARGET_PROJECT_VERSION, Environment.STAGING));
        }
    }

    private void stubTargetServerCreation() {
        when(
            workspaceMcpServerFacade.createWorkspaceMcpServer(
                eq(SOURCE_SERVER_NAME), eq(PlatformType.AUTOMATION), eq(Environment.STAGING), eq(false), eq(true),
                eq(true), eq(WORKSPACE_ID), any(UUID.class))).thenReturn(targetMcpServer());
        when(mcpServerFacade.getMcpServerTags(anyList()))
            .thenReturn(Map.of(sourceMcpServer(), List.of(new Tag(SOURCE_TAG_ID, "source-tag"))));
    }

    private static Connection connection(long id, String name) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setName(name);
        connection.setComponentName("googleSheets");
        connection.setConnectionVersion(1);

        return connection;
    }

    private static McpComponent mcpComponent(long id, long mcpServerId, Long connectionId) {
        McpComponent mcpComponent = new McpComponent("googleSheets", 1, mcpServerId, connectionId);

        mcpComponent.setId(id);

        return mcpComponent;
    }

    private static McpProject mcpProject(long id, long mcpServerId, long projectDeploymentId) {
        return new McpProject(id, projectDeploymentId, mcpServerId);
    }

    private static McpTool mcpTool(long id, String name, long mcpComponentId, boolean enabled) {
        McpTool mcpTool = new McpTool(id, name, Map.of(), mcpComponentId);

        mcpTool.setEnabled(enabled);

        return mcpTool;
    }

    private static Project project() {
        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setName("Billing");
        project.setWorkspaceId(WORKSPACE_ID);

        return project;
    }

    private static ProjectDeployment projectDeployment(long id, int projectVersion, Environment environment) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setProjectId(PROJECT_ID);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setEnvironment(environment);

        return projectDeployment;
    }

    private static ProjectDeploymentWorkflow projectDeploymentWorkflow(long id, String workflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(id);
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }

    private static McpComponent sourceMcpComponent(long connectionId) {
        return mcpComponent(SOURCE_COMPONENT_ID, SOURCE_ID, connectionId);
    }

    private static McpProject sourceMcpProject(long id) {
        return mcpProject(id, SOURCE_ID, SOURCE_PROJECT_DEPLOYMENT_ID);
    }

    private static McpProjectWorkflow sourceMcpProjectWorkflow(long mcpProjectId, long projectDeploymentWorkflowId) {
        McpProjectWorkflow mcpProjectWorkflow =
            new McpProjectWorkflow(mcpProjectId, projectDeploymentWorkflowId);

        mcpProjectWorkflow.setId(SOURCE_MCP_PROJECT_WORKFLOW_ID);
        mcpProjectWorkflow.setParameters(SOURCE_PARAMETERS);

        return mcpProjectWorkflow;
    }

    private static McpServer sourceMcpServer() {
        McpServer mcpServer = new McpServer(SOURCE_SERVER_NAME, PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer.setId(SOURCE_ID);
        mcpServer.setUuid(UUID.fromString(SERVER_UUID));
        mcpServer.setAuthenticationRequired(true);
        mcpServer.setEnforceToolAuthorization(true);
        mcpServer.setTagIds(List.of(SOURCE_TAG_ID));

        return mcpServer;
    }

    private static McpComponent staleTargetMcpComponent() {
        McpComponent mcpComponent = new McpComponent("slack", 1, TARGET_ID, null);

        mcpComponent.setId(TARGET_COMPONENT_ID + 1);

        return mcpComponent;
    }

    private static McpProject targetMcpProject(long id) {
        return mcpProject(id, TARGET_ID, TARGET_PROJECT_DEPLOYMENT_ID);
    }

    private static McpProjectWorkflow targetMcpProjectWorkflow(long projectDeploymentWorkflowId) {
        return targetMcpProjectWorkflow(TARGET_MCP_PROJECT_WORKFLOW_ID, projectDeploymentWorkflowId);
    }

    private static McpProjectWorkflow targetMcpProjectWorkflow(long id, long projectDeploymentWorkflowId) {
        McpProjectWorkflow mcpProjectWorkflow =
            new McpProjectWorkflow(TARGET_MCP_PROJECT_ID, projectDeploymentWorkflowId);

        mcpProjectWorkflow.setId(id);

        return mcpProjectWorkflow;
    }

    /**
     * Deliberately named and tagged differently from the source, so every update-path test pins that neither crosses.
     */
    private static McpServer targetMcpServer() {
        McpServer mcpServer = new McpServer("Target-owned name", PlatformType.AUTOMATION, Environment.STAGING);

        mcpServer.setId(TARGET_ID);
        mcpServer.setUuid(UUID.fromString(SERVER_UUID));

        return mcpServer;
    }
}
