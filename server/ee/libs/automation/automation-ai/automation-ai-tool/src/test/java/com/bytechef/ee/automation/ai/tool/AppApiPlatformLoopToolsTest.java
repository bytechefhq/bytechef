/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.exception.ProjectDeploymentErrorType;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.ai.tool.AppApiPlatformLoopTools.ApiCollectionListResult;
import com.bytechef.ee.automation.ai.tool.AppApiPlatformLoopTools.ApiCollectionResult;
import com.bytechef.ee.automation.ai.tool.AppApiPlatformLoopTools.ApiCollectionVersionResult;
import com.bytechef.ee.automation.ai.tool.AppApiPlatformLoopTools.ApiEndpointResult;
import com.bytechef.ee.automation.ai.tool.AppApiPlatformLoopTools.ConnectionCandidate;
import com.bytechef.ee.automation.ai.tool.AppApiPlatformLoopTools.DataTableColumnInput;
import com.bytechef.ee.automation.ai.tool.AppApiPlatformLoopTools.DataTableResult;
import com.bytechef.ee.automation.ai.tool.AppApiPlatformLoopTools.EndpointConnectionChoice;
import com.bytechef.ee.automation.ai.tool.AppApiPlatformLoopTools.UnresolvedEndpointConnection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.exception.ApiCollectionErrorType;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AppApiPlatformLoopToolsTest {

    private static final long API_COLLECTION_ID = 7L;
    private static final long ENDPOINT_ID = 77L;
    private static final long PROJECT_DEPLOYMENT_ID = 70L;
    private static final long PROJECT_ID = 5L;
    private static final int PROJECT_VERSION = 2;
    private static final String WORKFLOW_ID = "wf-api";
    private static final String WORKFLOW_UUID = "33333333-3333-4333-8333-333333333333";
    private static final long WORKSPACE_ID = 1049L;

    private ApiCollectionFacade apiCollectionFacade;
    private AppApiPlatformLoopTools appApiPlatformLoopTools;
    private AppFacade appFacade;
    private ComponentConnectionFacade componentConnectionFacade;
    private ProjectDeploymentFacade projectDeploymentFacade;
    private ProjectFacade projectFacade;
    private ProjectWorkflowFacade projectWorkflowFacade;
    private WorkspaceDataTableFacade workspaceDataTableFacade;

    @BeforeEach
    void beforeEach() {
        apiCollectionFacade = mock(ApiCollectionFacade.class);
        appFacade = mock(AppFacade.class);
        componentConnectionFacade = mock(ComponentConnectionFacade.class);
        projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
        projectFacade = mock(ProjectFacade.class);
        projectWorkflowFacade = mock(ProjectWorkflowFacade.class);
        workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        appApiPlatformLoopTools = new AppApiPlatformLoopTools(
            apiCollectionFacade, appFacade, componentConnectionFacade, projectDeploymentFacade, projectFacade,
            projectWorkflowFacade, workspaceDataTableFacade);
    }

    @Test
    void testCreateAppApiCollectionEnablesDeploymentAndReturnsBindCommand() {
        when(projectFacade.getProject(PROJECT_ID)).thenReturn(project());
        when(apiCollectionFacade.createApiCollection(any())).thenReturn(apiCollection(Environment.DEVELOPMENT));

        ApiCollectionResult apiCollectionResult = appApiPlatformLoopTools.createAppApiCollection(
            PROJECT_ID, PROJECT_VERSION, "Orders API", "orders", null, null, toolContext(Environment.DEVELOPMENT));

        assertThat(apiCollectionResult.status()).isEqualTo(AppLoopTools.STATUS_DEPLOYED);
        assertThat(apiCollectionResult.apiCollectionId()).isEqualTo(API_COLLECTION_ID);
        assertThat(apiCollectionResult.contextPath()).isEqualTo("orders");
        assertThat(apiCollectionResult.collectionVersion()).isEqualTo(1);
        assertThat(apiCollectionResult.bindCommand())
            .isEqualTo("bytechef automation app bind api-collection --api-collection-id 7");

        ArgumentCaptor<ApiCollectionDTO> apiCollectionDTOArgumentCaptor = ArgumentCaptor.forClass(
            ApiCollectionDTO.class);

        verify(apiCollectionFacade).createApiCollection(apiCollectionDTOArgumentCaptor.capture());

        ApiCollectionDTO apiCollectionDTO = apiCollectionDTOArgumentCaptor.getValue();

        assertThat(apiCollectionDTO.environment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(apiCollectionDTO.projectId()).isEqualTo(PROJECT_ID);
        assertThat(apiCollectionDTO.projectVersion()).isEqualTo(PROJECT_VERSION);
        assertThat(apiCollectionDTO.collectionVersion()).isEqualTo(1);
        assertThat(apiCollectionDTO.contextPath()).isEqualTo("orders");

        verify(projectDeploymentFacade).enableProjectDeployment(PROJECT_DEPLOYMENT_ID, true);
    }

    @Test
    void testCreateAppApiCollectionRefusesProjectOfAnotherWorkspace() {
        ProjectDTO otherProject = ProjectDTO.builder()
            .id(PROJECT_ID)
            .workspaceId(1L)
            .build();

        when(projectFacade.getProject(PROJECT_ID)).thenReturn(otherProject);

        ApiCollectionResult apiCollectionResult = appApiPlatformLoopTools.createAppApiCollection(
            PROJECT_ID, PROJECT_VERSION, "Orders API", "orders", null, null, toolContext(Environment.DEVELOPMENT));

        assertThat(apiCollectionResult.status()).isEqualTo(AppLoopTools.STATUS_REFUSED);

        verifyNoInteractions(apiCollectionFacade, projectDeploymentFacade);
    }

    @Test
    void testCreateAppApiCollectionArmingFailedForAccessDeniedStillReturnsBindCommand() {
        when(projectFacade.getProject(PROJECT_ID)).thenReturn(project());
        when(apiCollectionFacade.createApiCollection(any())).thenReturn(apiCollection(Environment.DEVELOPMENT));
        doThrow(new AccessDeniedException("Access Denied"))
            .when(projectDeploymentFacade)
            .enableProjectDeployment(PROJECT_DEPLOYMENT_ID, true);

        ApiCollectionResult apiCollectionResult = appApiPlatformLoopTools.createAppApiCollection(
            PROJECT_ID, PROJECT_VERSION, "Orders API", "orders", null, null, toolContext(Environment.DEVELOPMENT));

        assertThat(apiCollectionResult.status()).isEqualTo(AppLoopTools.STATUS_ARMING_FAILED);
        assertThat(apiCollectionResult.apiCollectionId()).isEqualTo(API_COLLECTION_ID);
        assertThat(apiCollectionResult.message()).contains("DEPLOYMENT_EDIT", "bindCommand", "409");
        assertThat(apiCollectionResult.bindCommand())
            .isEqualTo("bytechef automation app bind api-collection --api-collection-id 7");
    }

    @Test
    void testCreateAppApiCollectionArmingFailedForConfigurationExceptionStillReturnsBindCommand() {
        when(projectFacade.getProject(PROJECT_ID)).thenReturn(project());
        when(apiCollectionFacade.createApiCollection(any())).thenReturn(apiCollection(Environment.DEVELOPMENT));
        doThrow(new ConfigurationException("trigger rejected", ProjectDeploymentErrorType.INVALID_DEPLOYMENT))
            .when(projectDeploymentFacade)
            .enableProjectDeployment(PROJECT_DEPLOYMENT_ID, true);

        ApiCollectionResult apiCollectionResult = appApiPlatformLoopTools.createAppApiCollection(
            PROJECT_ID, PROJECT_VERSION, "Orders API", "orders", null, null, toolContext(Environment.DEVELOPMENT));

        assertThat(apiCollectionResult.status()).isEqualTo(AppLoopTools.STATUS_ARMING_FAILED);
        assertThat(apiCollectionResult.message()).contains("trigger rejected");
        assertThat(apiCollectionResult.bindCommand())
            .isEqualTo("bytechef automation app bind api-collection --api-collection-id 7");
    }

    @Test
    void testCreateAppApiEndpointRefusesNonApiPlatformTrigger() {
        stubEndpointWorkflow("schedule/v1/everyDay");

        ApiEndpointResult apiEndpointResult = appApiPlatformLoopTools.createAppApiEndpoint(
            API_COLLECTION_ID, "getOrders", "GET", "/orders", WORKFLOW_UUID, null, null,
            toolContext(Environment.DEVELOPMENT));

        assertThat(apiEndpointResult.status()).isEqualTo(AppLoopTools.STATUS_REFUSED);
        assertThat(apiEndpointResult.message()).contains("schedule/v1/everyDay", "apiPlatform/v1/newApiRequest");

        verify(apiCollectionFacade, never()).createApiCollectionEndpoint(any(), any(), any());
        verify(apiCollectionFacade, never()).enableApiCollectionEndpoint(anyLong(), anyBoolean());
    }

    @Test
    void testCreateAppApiEndpointRefusesCollectionOutsideDevelopment() {
        when(apiCollectionFacade.getApiCollection(API_COLLECTION_ID))
            .thenReturn(apiCollection(Environment.PRODUCTION));

        ApiEndpointResult apiEndpointResult = appApiPlatformLoopTools.createAppApiEndpoint(
            API_COLLECTION_ID, "getOrders", "GET", "/orders", WORKFLOW_UUID, null, null,
            toolContext(Environment.DEVELOPMENT));

        assertThat(apiEndpointResult.status()).isEqualTo(AppLoopTools.STATUS_REFUSED);
        assertThat(apiEndpointResult.message()).contains("DEVELOPMENT");

        verify(apiCollectionFacade, never()).createApiCollectionEndpoint(any(), any(), any());
    }

    @Test
    void testCreateAppApiEndpointPicksOnlyUsableCandidate() {
        stubEndpointWorkflow("apiPlatform/v1/newApiRequest");
        stubConnections(
            connection(101L, "Slack A", "slack", true), connection(102L, "Slack B", "slack", false),
            connection(103L, "Gmail", "gmail", true));
        stubCreatedEndpoint();

        ApiEndpointResult apiEndpointResult = appApiPlatformLoopTools.createAppApiEndpoint(
            API_COLLECTION_ID, "getOrders", "GET", "/orders", WORKFLOW_UUID, null, Map.of("limit", 10),
            toolContext(Environment.DEVELOPMENT));

        assertThat(apiEndpointResult.status()).isEqualTo(AppLoopTools.STATUS_DEPLOYED);
        assertThat(apiEndpointResult.apiCollectionEndpointId()).isEqualTo(ENDPOINT_ID);
        assertThat(apiEndpointResult.url()).isEqualTo("/api/app/v1/api/v1/orders/orders");
        assertThat(apiEndpointResult.unresolvedConnections()).isEmpty();

        assertThat(captureCreatedConnections())
            .extracting(
                ProjectDeploymentWorkflowConnection::getConnectionId,
                ProjectDeploymentWorkflowConnection::getWorkflowNodeName,
                ProjectDeploymentWorkflowConnection::getWorkflowConnectionKey)
            .containsExactly(tuple(101L, "slack_1", "slack"));

        verify(apiCollectionFacade).enableApiCollectionEndpoint(ENDPOINT_ID, true);
    }

    @Test
    void testCreateAppApiEndpointArmingFailedForAccessDeniedNamesBothPermissions() {
        stubEndpointWorkflow("apiPlatform/v1/newApiRequest");
        stubConnections(connection(101L, "Slack A", "slack", true));
        when(apiCollectionFacade.createApiCollectionEndpoint(any(), anyList(), any())).thenReturn(endpoint(false));
        when(apiCollectionFacade.enableApiCollectionEndpoint(ENDPOINT_ID, true))
            .thenThrow(new AccessDeniedException("Access Denied"));

        ApiEndpointResult apiEndpointResult = appApiPlatformLoopTools.createAppApiEndpoint(
            API_COLLECTION_ID, "getOrders", "GET", "/orders", WORKFLOW_UUID, null, null,
            toolContext(Environment.DEVELOPMENT));

        assertThat(apiEndpointResult.status()).isEqualTo(AppLoopTools.STATUS_ARMING_FAILED);
        assertThat(apiEndpointResult.apiCollectionEndpointId()).isEqualTo(ENDPOINT_ID);
        assertThat(apiEndpointResult.enabled()).isFalse();
        assertThat(apiEndpointResult.message())
            .contains(
                "API_PLATFORM_EDIT on the endpoint and DEPLOYMENT_EDIT on the collection's deployment",
                "do not call createAppApiEndpoint again for it");
    }

    @Test
    void testCreateAppApiEndpointArmingFailedForConfigurationException() {
        stubEndpointWorkflow("apiPlatform/v1/newApiRequest");
        stubConnections(connection(101L, "Slack A", "slack", true));
        when(apiCollectionFacade.createApiCollectionEndpoint(any(), anyList(), any())).thenReturn(endpoint(false));
        when(apiCollectionFacade.enableApiCollectionEndpoint(ENDPOINT_ID, true))
            .thenThrow(new ConfigurationException("trigger rejected", ProjectDeploymentErrorType.INVALID_DEPLOYMENT));

        ApiEndpointResult apiEndpointResult = appApiPlatformLoopTools.createAppApiEndpoint(
            API_COLLECTION_ID, "getOrders", "GET", "/orders", WORKFLOW_UUID, null, null,
            toolContext(Environment.DEVELOPMENT));

        assertThat(apiEndpointResult.status()).isEqualTo(AppLoopTools.STATUS_ARMING_FAILED);
        assertThat(apiEndpointResult.apiCollectionEndpointId()).isEqualTo(ENDPOINT_ID);
        assertThat(apiEndpointResult.message())
            .contains("trigger rejected", "do not call createAppApiEndpoint again for it");
    }

    @Test
    void testCreateAppApiEndpointLeavesOptionalSlotEmptyUnlessChosen() {
        stubEndpointWorkflow("apiPlatform/v1/newApiRequest");
        when(componentConnectionFacade.getWorkflowComponentConnections(WORKFLOW_ID))
            .thenReturn(
                List.of(
                    new ComponentConnection("slack", 1, "slack_1", "slack", true),
                    new ComponentConnection("gmail", 1, "slack_1", "gmail", false)));
        stubConnections(connection(101L, "Slack A", "slack", true), connection(103L, "Gmail", "gmail", true));
        stubCreatedEndpoint();

        ApiEndpointResult apiEndpointResult = appApiPlatformLoopTools.createAppApiEndpoint(
            API_COLLECTION_ID, "getOrders", "GET", "/orders", WORKFLOW_UUID, null, null,
            toolContext(Environment.DEVELOPMENT));

        assertThat(apiEndpointResult.status()).isEqualTo(AppLoopTools.STATUS_DEPLOYED);

        assertThat(captureCreatedConnections())
            .extracting(
                ProjectDeploymentWorkflowConnection::getConnectionId,
                ProjectDeploymentWorkflowConnection::getWorkflowConnectionKey)
            .containsExactly(tuple(101L, "slack"));
    }

    @Test
    void testCreateAppApiEndpointFillsOptionalSlotWhenChosen() {
        stubEndpointWorkflow("apiPlatform/v1/newApiRequest");
        when(componentConnectionFacade.getWorkflowComponentConnections(WORKFLOW_ID))
            .thenReturn(
                List.of(
                    new ComponentConnection("slack", 1, "slack_1", "slack", true),
                    new ComponentConnection("gmail", 1, "slack_1", "gmail", false)));
        stubConnections(connection(101L, "Slack A", "slack", true), connection(103L, "Gmail", "gmail", true));
        stubCreatedEndpoint();

        ApiEndpointResult apiEndpointResult = appApiPlatformLoopTools.createAppApiEndpoint(
            API_COLLECTION_ID, "getOrders", "GET", "/orders", WORKFLOW_UUID,
            List.of(new EndpointConnectionChoice("slack_1", "gmail", 103L)), null,
            toolContext(Environment.DEVELOPMENT));

        assertThat(apiEndpointResult.status()).isEqualTo(AppLoopTools.STATUS_DEPLOYED);

        assertThat(captureCreatedConnections())
            .extracting(
                ProjectDeploymentWorkflowConnection::getConnectionId,
                ProjectDeploymentWorkflowConnection::getWorkflowConnectionKey)
            .containsExactlyInAnyOrder(tuple(101L, "slack"), tuple(103L, "gmail"));
    }

    @Test
    void testCreateAppApiEndpointUnresolvedWhenSeveralCandidates() {
        stubEndpointWorkflow("apiPlatform/v1/newApiRequest");
        stubConnections(connection(101L, "Slack A", "slack", true), connection(104L, "Slack C", "slack", true));

        ApiEndpointResult apiEndpointResult = appApiPlatformLoopTools.createAppApiEndpoint(
            API_COLLECTION_ID, "getOrders", "GET", "/orders", WORKFLOW_UUID, null, null,
            toolContext(Environment.DEVELOPMENT));

        assertThat(apiEndpointResult.status()).isEqualTo(AppLoopTools.STATUS_UNRESOLVED);
        assertThat(apiEndpointResult.unresolvedConnections())
            .extracting(
                UnresolvedEndpointConnection::workflowNodeName, UnresolvedEndpointConnection::key,
                UnresolvedEndpointConnection::componentName)
            .containsExactly(tuple("slack_1", "slack", "slack"));

        UnresolvedEndpointConnection unresolvedEndpointConnection = apiEndpointResult.unresolvedConnections()
            .getFirst();

        assertThat(unresolvedEndpointConnection.candidates())
            .extracting(ConnectionCandidate::id)
            .containsExactly(101L, 104L);

        verify(apiCollectionFacade, never()).createApiCollectionEndpoint(any(), any(), any());
        verify(apiCollectionFacade, never()).enableApiCollectionEndpoint(anyLong(), anyBoolean());
    }

    @Test
    void testCreateAppApiEndpointUnresolvedWhenNoCandidate() {
        stubEndpointWorkflow("apiPlatform/v1/newApiRequest");
        stubConnections(connection(102L, "Slack B", "slack", false));

        ApiEndpointResult apiEndpointResult = appApiPlatformLoopTools.createAppApiEndpoint(
            API_COLLECTION_ID, "getOrders", "GET", "/orders", WORKFLOW_UUID, null, null,
            toolContext(Environment.DEVELOPMENT));

        assertThat(apiEndpointResult.status()).isEqualTo(AppLoopTools.STATUS_UNRESOLVED);
        assertThat(apiEndpointResult.unresolvedConnections()).hasSize(1);

        UnresolvedEndpointConnection unresolvedEndpointConnection = apiEndpointResult.unresolvedConnections()
            .getFirst();

        assertThat(unresolvedEndpointConnection.candidates()).isEmpty();

        verify(apiCollectionFacade, never()).createApiCollectionEndpoint(any(), any(), any());
    }

    @Test
    void testCreateAppApiEndpointExplicitChoiceWins() {
        stubEndpointWorkflow("apiPlatform/v1/newApiRequest");
        stubConnections(connection(101L, "Slack A", "slack", true), connection(104L, "Slack C", "slack", true));
        stubCreatedEndpoint();

        ApiEndpointResult apiEndpointResult = appApiPlatformLoopTools.createAppApiEndpoint(
            API_COLLECTION_ID, "getOrders", "GET", "/orders", WORKFLOW_UUID,
            List.of(new EndpointConnectionChoice("slack_1", "slack", 104L)), null,
            toolContext(Environment.DEVELOPMENT));

        assertThat(apiEndpointResult.status()).isEqualTo(AppLoopTools.STATUS_DEPLOYED);

        assertThat(captureCreatedConnections())
            .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactly(104L);
    }

    @Test
    void testCreateAppApiEndpointRefusesChoiceThatIsNotAUsableCandidate() {
        stubEndpointWorkflow("apiPlatform/v1/newApiRequest");
        stubConnections(connection(101L, "Slack A", "slack", true), connection(102L, "Slack B", "slack", false));

        ApiEndpointResult apiEndpointResult = appApiPlatformLoopTools.createAppApiEndpoint(
            API_COLLECTION_ID, "getOrders", "GET", "/orders", WORKFLOW_UUID,
            List.of(new EndpointConnectionChoice("slack_1", "slack", 102L)), null,
            toolContext(Environment.DEVELOPMENT));

        assertThat(apiEndpointResult.status()).isEqualTo(AppLoopTools.STATUS_REFUSED);
        assertThat(apiEndpointResult.message()).contains("102");

        verify(apiCollectionFacade, never()).createApiCollectionEndpoint(any(), any(), any());
    }

    @Test
    void testUpdateAppApiCollectionVersionReportsRefusal() {
        when(apiCollectionFacade.getApiCollection(API_COLLECTION_ID))
            .thenReturn(apiCollection(Environment.DEVELOPMENT));
        when(apiCollectionFacade.updateApiCollectionProjectVersion(API_COLLECTION_ID, 3, false))
            .thenThrow(
                new ConfigurationException(
                    "Project version 3 no longer contains the workflows of endpoints [getOrders]",
                    Map.of("endpointNames", List.of("getOrders")), ApiCollectionErrorType.ENDPOINT_WORKFLOW_REMOVED));

        ApiCollectionVersionResult apiCollectionVersionResult = appApiPlatformLoopTools.updateAppApiCollectionVersion(
            API_COLLECTION_ID, 3, null, toolContext(Environment.DEVELOPMENT));

        assertThat(apiCollectionVersionResult.status()).isEqualTo(AppLoopTools.STATUS_REFUSED);
        assertThat(apiCollectionVersionResult.message()).contains("getOrders", "removeMissingEndpoints");
    }

    @Test
    void testListAppApiCollectionsReturnsDevelopmentCollectionsWithEndpoints() {
        ApiCollectionDTO apiCollectionDTO = apiCollection(Environment.DEVELOPMENT, List.of(endpoint(true)));

        when(apiCollectionFacade.getApiCollections(
            WORKSPACE_ID, (long) Environment.DEVELOPMENT.ordinal(), PROJECT_ID, null))
                .thenReturn(List.of(apiCollectionDTO));

        ApiCollectionListResult apiCollectionListResult = appApiPlatformLoopTools.listAppApiCollections(
            PROJECT_ID, toolContext(Environment.DEVELOPMENT));

        assertThat(apiCollectionListResult.error()).isNull();
        assertThat(apiCollectionListResult.apiCollections()).hasSize(1);

        AppApiPlatformLoopTools.ApiCollectionSummary apiCollectionSummary = apiCollectionListResult.apiCollections()
            .getFirst();

        assertThat(apiCollectionSummary.apiCollectionId()).isEqualTo(API_COLLECTION_ID);
        assertThat(apiCollectionSummary.endpoints())
            .extracting(
                AppApiPlatformLoopTools.ApiEndpointSummary::method, AppApiPlatformLoopTools.ApiEndpointSummary::path,
                AppApiPlatformLoopTools.ApiEndpointSummary::workflowUuid,
                AppApiPlatformLoopTools.ApiEndpointSummary::enabled)
            .containsExactly(tuple("GET", "/orders", WORKFLOW_UUID, true));
    }

    @Test
    void testCreateAppDataTableReturnsBaseName() {
        DataTableResult dataTableResult = appApiPlatformLoopTools.createAppDataTable(
            "orders", List.of(new DataTableColumnInput("total", "number")), null,
            toolContext(Environment.DEVELOPMENT));

        assertThat(dataTableResult.status()).isEqualTo(AppApiPlatformLoopTools.STATUS_CREATED);
        assertThat(dataTableResult.baseName()).isEqualTo("orders");

        verify(workspaceDataTableFacade).createTable(
            "orders", "", List.of(new ColumnSpec("total", ColumnType.NUMBER)), WORKSPACE_ID,
            Environment.DEVELOPMENT.ordinal());
    }

    @Test
    void testCreateAppDataTableRefusesUnknownColumnType() {
        DataTableResult dataTableResult = appApiPlatformLoopTools.createAppDataTable(
            "orders", List.of(new DataTableColumnInput("total", "money")), null,
            toolContext(Environment.DEVELOPMENT));

        assertThat(dataTableResult.status()).isEqualTo(AppLoopTools.STATUS_REFUSED);
        assertThat(dataTableResult.message()).contains("money", "STRING");

        verifyNoInteractions(workspaceDataTableFacade);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "createAppApiCollection", "createAppApiEndpoint", "updateAppApiCollectionVersion",
            "listAppApiCollections", "createAppDataTable"
        })
    void testEveryToolRefusesProduction(String toolName) {
        ToolContext toolContext = toolContext(Environment.PRODUCTION);

        String refusal = switch (toolName) {
            case "createAppApiCollection" -> appApiPlatformLoopTools.createAppApiCollection(
                PROJECT_ID, PROJECT_VERSION, "Orders API", "orders", null, null, toolContext)
                .message();
            case "createAppApiEndpoint" -> appApiPlatformLoopTools.createAppApiEndpoint(
                API_COLLECTION_ID, "getOrders", "GET", "/orders", WORKFLOW_UUID, null, null, toolContext)
                .message();
            case "updateAppApiCollectionVersion" -> appApiPlatformLoopTools.updateAppApiCollectionVersion(
                API_COLLECTION_ID, 3, true, toolContext)
                .message();
            case "listAppApiCollections" -> appApiPlatformLoopTools.listAppApiCollections(null, toolContext)
                .error();
            case "createAppDataTable" -> appApiPlatformLoopTools.createAppDataTable(
                "orders", List.of(new DataTableColumnInput("total", "NUMBER")), null, toolContext)
                .message();
            default -> throw new IllegalArgumentException(toolName);
        };

        assertThat(refusal).isEqualTo(AppLoopTools.DEVELOPMENT_ONLY);

        verifyNoInteractions(
            apiCollectionFacade, appFacade, componentConnectionFacade, projectDeploymentFacade, projectFacade,
            projectWorkflowFacade, workspaceDataTableFacade);
    }

    private static ApiCollectionDTO apiCollection(Environment environment) {
        return apiCollection(environment, List.of());
    }

    private static ApiCollectionDTO apiCollection(
        Environment environment, List<ApiCollectionEndpointDTO> apiCollectionEndpointDTOs) {

        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setWorkspaceId(WORKSPACE_ID);

        return new ApiCollectionDTO(
            1, "orders", null, null, null, true, apiCollectionEndpointDTOs, environment, API_COLLECTION_ID, null, null,
            "Orders API", project, PROJECT_ID, null, PROJECT_DEPLOYMENT_ID, PROJECT_VERSION, List.of(), 0);
    }

    @SuppressWarnings("unchecked")
    private List<ProjectDeploymentWorkflowConnection> captureCreatedConnections() {
        ArgumentCaptor<List<ProjectDeploymentWorkflowConnection>> connectionsArgumentCaptor = ArgumentCaptor.forClass(
            List.class);

        verify(apiCollectionFacade).createApiCollectionEndpoint(any(), connectionsArgumentCaptor.capture(), any());

        return connectionsArgumentCaptor.getValue();
    }

    private static AppWorkspaceConnectionDTO connection(long id, String name, String componentName, boolean usable) {
        return new AppWorkspaceConnectionDTO(id, name, componentName, 1, null, "VALID", false, usable);
    }

    private static ApiCollectionEndpointDTO endpoint(boolean enabled) {
        return new ApiCollectionEndpointDTO(
            API_COLLECTION_ID, null, null, enabled, HttpMethod.GET, ENDPOINT_ID, null, null, "getOrders", "/orders",
            700L, 0, WORKFLOW_UUID);
    }

    private static ProjectDTO project() {
        return ProjectDTO.builder()
            .id(PROJECT_ID)
            .name("Orders")
            .workspaceId(WORKSPACE_ID)
            .build();
    }

    private void stubConnections(AppWorkspaceConnectionDTO... connections) {
        when(appFacade.getConnections(eq(WORKSPACE_ID), eq(Environment.DEVELOPMENT), isNull()))
            .thenReturn(List.of(connections));
    }

    private void stubCreatedEndpoint() {
        when(apiCollectionFacade.createApiCollectionEndpoint(any(), anyList(), any())).thenReturn(endpoint(false));
        when(apiCollectionFacade.enableApiCollectionEndpoint(ENDPOINT_ID, true)).thenReturn(endpoint(true));
    }

    private void stubEndpointWorkflow(String triggerType) {
        when(apiCollectionFacade.getApiCollection(API_COLLECTION_ID))
            .thenReturn(apiCollection(Environment.DEVELOPMENT));

        Workflow workflow = mock(Workflow.class);

        when(workflow.getExtensions(WorkflowExtConstants.TRIGGERS, WorkflowTrigger.class, List.of()))
            .thenReturn(List.of(new WorkflowTrigger(Map.of("name", "trigger_1", "type", triggerType))));

        ProjectWorkflowDTO projectWorkflowDTO = mock(ProjectWorkflowDTO.class);

        when(projectWorkflowDTO.getId()).thenReturn(WORKFLOW_ID);
        when(projectWorkflowDTO.getWorkflow()).thenReturn(workflow);
        when(projectWorkflowDTO.getWorkflowUuid()).thenReturn(WORKFLOW_UUID);

        when(projectWorkflowFacade.getProjectVersionWorkflows(PROJECT_ID, PROJECT_VERSION, false))
            .thenReturn(List.of(projectWorkflowDTO));
        when(componentConnectionFacade.getWorkflowComponentConnections(WORKFLOW_ID))
            .thenReturn(List.of(new ComponentConnection("slack", 1, "slack_1", "slack", true)));
    }

    private static ToolContext toolContext(Environment environment) {
        return new ToolContext(
            Map.of(
                AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID,
                AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, (long) environment.ordinal()));
    }
}
