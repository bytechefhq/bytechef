/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.facade.A2aProjectFacade;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.config.EnvironmentPromotionIntTestConfiguration;
import com.bytechef.ee.automation.promotion.config.EnvironmentPromotionIntTestConfigurationSharedMocks;
import com.bytechef.ee.automation.promotion.config.RecordingPermissionService;
import com.bytechef.ee.automation.promotion.config.RecordingPermissionService.ResourceScopeCheck;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.handler.EnvironmentPromotionHandler;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = EnvironmentPromotionIntTestConfiguration.class, properties = "bytechef.webhook-url=/webhooks/{id}")
@Import(PostgreSQLContainerConfiguration.class)
@EnvironmentPromotionIntTestConfigurationSharedMocks
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
class EnvironmentPromotionFacadeIntTest {

    private static final String COMPONENT_NAME = "slack";
    private static final int CONNECTION_VERSION = 1;
    private static final String CONNECTION_NAME = "slack-account";
    private static final String WORKFLOW_CONNECTION_KEY = "slack";
    private static final String WORKFLOW_NODE_NAME = "slack_1";

    @Autowired
    private A2aProjectFacade a2aProjectFacade;

    @Autowired
    private A2aProjectWorkflowService a2aProjectWorkflowService;

    @Autowired
    private A2aServerService a2aServerService;

    @Autowired
    private ApiCollectionEndpointService apiCollectionEndpointService;

    @Autowired
    private ApiCollectionFacade apiCollectionFacade;

    @Autowired
    private ApiCollectionService apiCollectionService;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private EnvironmentPromotionFacade environmentPromotionFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpComponentService mcpComponentService;

    @Autowired
    private McpProjectFacade mcpProjectFacade;

    @Autowired
    private McpProjectWorkflowService mcpProjectWorkflowService;

    @Autowired
    private McpServerFacade mcpServerFacade;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private RecordingPermissionService permissionService;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @Autowired
    private WorkspaceMcpServerFacade workspaceMcpServerFacade;

    private long developmentConnectionId;
    private long projectId;
    private long stagingConnectionId;
    private String workflowUuid;
    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        permissionService.reset();

        reset(componentConnectionFacade, principalJobService, workspaceConnectionFacade);

        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt()))
            .thenReturn(Page.empty());

        when(componentConnectionFacade.getComponentConnection(anyString(), anyString(), anyString()))
            .thenReturn(
                new ComponentConnection(
                    COMPONENT_NAME, CONNECTION_VERSION, WORKFLOW_NODE_NAME, WORKFLOW_CONNECTION_KEY, true));

        when(workspaceConnectionFacade.getConnections(anyLong(), anyString(), any(), any(), any()))
            .thenAnswer(invocation -> {
                String componentName = invocation.getArgument(1);
                Integer connectionVersion = invocation.getArgument(2);
                Long environmentId = invocation.getArgument(3);

                List<ConnectionDTO> connectionDTOs = new ArrayList<>();

                for (Connection connection : connectionRepository.findAll()) {
                    if (!Objects.equals(connection.getComponentName(), componentName)) {
                        continue;
                    }

                    if (connectionVersion != null && connection.getConnectionVersion() != connectionVersion) {
                        continue;
                    }

                    if (environmentId != null && connection.getEnvironmentId() != environmentId) {
                        continue;
                    }

                    connectionDTOs.add(new ConnectionDTO(true, Map.of(), null, connection, Map.of(), List.of()));
                }

                return connectionDTOs;
            });

        workspaceId = createWorkspace();
        projectId = createPublishedProject();
        workflowUuid = projectWorkflowUuid(1);

        developmentConnectionId = createConnection(Environment.DEVELOPMENT);
        stagingConnectionId = createConnection(Environment.STAGING);
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute(
            "TRUNCATE TABLE a2a_server, api_collection, category, connection, mcp_server, project, " +
                "project_deployment, project_workflow, tag, workflow, workspace CASCADE");
    }

    @Test
    void testA2aServerPromotionCreatesTargetInTargetEnvironmentAndSyncsOnRePromotion() {
        A2aServer sourceA2aServer = createA2aServer("billing-a2a", "Billing agent");

        long sourceA2aServerId = Objects.requireNonNull(sourceA2aServer.getId(), "id");

        A2aProject sourceA2aProject = a2aProjectFacade.createA2aProject(
            sourceA2aServerId, projectId, 1, List.of(projectWorkflowId(1)));

        long sourceProjectDeploymentId =
            Objects.requireNonNull(sourceA2aProject.getProjectDeploymentId(), "projectDeploymentId");

        bindConnection(sourceProjectDeploymentId, developmentConnectionId);

        long sourceA2aProjectWorkflowId = Objects.requireNonNull(
            singleA2aProjectWorkflow(sourceA2aProject.getId()).getId(), "id");

        a2aProjectWorkflowService.updateParameters(
            sourceA2aProjectWorkflowId, Map.of("skillName", "invoice", "skillDescription", "Creates an invoice"));

        EnvironmentPromotionResult firstResult = environmentPromotionFacade.promote(
            PromotionResourceType.A2A_SERVER, sourceA2aServerId, Environment.STAGING.ordinal(), Map.of());

        assertThat(firstResult.created()).isTrue();
        assertThat(firstResult.unresolvedConnectionIds()).isEmpty();

        A2aServer targetA2aServer = a2aServerService.getA2aServer(firstResult.targetId());

        assertThat(targetA2aServer.getUuid()).isEqualTo(sourceA2aServer.getUuid());
        assertThat(targetA2aServer.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(targetA2aServer.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(targetA2aServer.isEnabled()).isFalse();
        assertThat(targetA2aServer.getSecretKey()).isNotEqualTo(sourceA2aServer.getSecretKey());
        assertThat(firstResult.targetUrl())
            .endsWith("/api/automation/a2a/" + targetA2aServer.getSecretKey() + "/.well-known/agent-card.json");

        A2aProjectWorkflow targetA2aProjectWorkflow =
            singleA2aProjectWorkflow(singleA2aProjectId(firstResult.targetId()));

        assertThat(a2aProjectWorkflowParameters(targetA2aProjectWorkflow))
            .containsEntry("skillName", "invoice")
            .containsEntry("skillDescription", "Creates an invoice");

        long stableProjectDeploymentWorkflowId = Objects.requireNonNull(
            targetA2aProjectWorkflow.getProjectDeploymentWorkflowId(), "projectDeploymentWorkflowId");

        ProjectDeploymentWorkflow targetProjectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(stableProjectDeploymentWorkflowId);
        ProjectDeployment targetProjectDeployment = projectDeploymentOfWorkflow(stableProjectDeploymentWorkflowId);

        assertThat(targetProjectDeployment.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(targetProjectDeployment.getProjectVersion()).isEqualTo(1);
        assertThat(connectionIds(targetProjectDeploymentWorkflow)).containsExactly(stagingConnectionId);

        projectFacade.publishProject(projectId, "v2", false);
        changeProjectVersion(sourceProjectDeploymentId, 2);
        a2aProjectWorkflowService.updateParameters(
            sourceA2aProjectWorkflowId, Map.of("skillName", "invoice", "skillDescription", "Creates a new invoice"));

        targetA2aServer.setName("staging-billing-a2a");
        targetA2aServer.setEnabled(true);

        a2aServerService.update(targetA2aServer);

        EnvironmentPromotionResult secondResult = environmentPromotionFacade.promote(
            PromotionResourceType.A2A_SERVER, sourceA2aServerId, Environment.STAGING.ordinal(), Map.of());

        assertThat(secondResult.created()).isFalse();
        assertThat(secondResult.targetId()).isEqualTo(firstResult.targetId());

        A2aServer rePromotedA2aServer = a2aServerService.getA2aServer(firstResult.targetId());

        assertThat(rePromotedA2aServer.getName()).isEqualTo("staging-billing-a2a");
        assertThat(rePromotedA2aServer.isEnabled()).isTrue();
        assertThat(rePromotedA2aServer.getSecretKey()).isEqualTo(targetA2aServer.getSecretKey());

        A2aProjectWorkflow rePromotedA2aProjectWorkflow =
            singleA2aProjectWorkflow(singleA2aProjectId(firstResult.targetId()));

        assertThat(a2aProjectWorkflowParameters(rePromotedA2aProjectWorkflow))
            .containsEntry("skillDescription", "Creates a new invoice");
        assertThat(rePromotedA2aProjectWorkflow.getProjectDeploymentWorkflowId())
            .isEqualTo(stableProjectDeploymentWorkflowId);
        assertThat(projectDeploymentOfWorkflow(stableProjectDeploymentWorkflowId).getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testA2aServerPromotionIsDeniedToANonAdmin() {
        A2aServer sourceA2aServer = createA2aServer("guarded-a2a", null);

        long sourceA2aServerId = Objects.requireNonNull(sourceA2aServer.getId(), "id");

        runAs(
            "user@localhost.com", AuthorityConstants.USER,
            () -> assertThatThrownBy(
                () -> environmentPromotionFacade.promote(
                    PromotionResourceType.A2A_SERVER, sourceA2aServerId, Environment.STAGING.ordinal(), Map.of()))
                        .isInstanceOf(AccessDeniedException.class));

        assertThat(a2aServerService.fetchA2aServer(sourceA2aServer.getUuid(), Environment.STAGING)).isEmpty();
    }

    @Test
    void testApiCollectionFirstPromotionCreatesCounterpartAndRePromotionSyncsInPlace() {
        ApiCollectionDTO sourceApiCollection = createApiCollection("billing", "billing", 1);

        long sourceApiCollectionId = Objects.requireNonNull(sourceApiCollection.id(), "id");

        createApiCollectionEndpoint(sourceApiCollectionId, HttpMethod.GET, "/x");
        bindConnection(sourceApiCollection.projectDeploymentId(), developmentConnectionId);

        EnvironmentPromotionResult firstResult = environmentPromotionFacade.promote(
            PromotionResourceType.API_COLLECTION, sourceApiCollectionId, Environment.STAGING.ordinal(), Map.of());

        assertThat(firstResult.created()).isTrue();
        assertThat(firstResult.unresolvedConnectionIds()).isEmpty();

        ApiCollection targetApiCollection = apiCollectionService.getApiCollection(firstResult.targetId());

        assertThat(targetApiCollection.getUuidAsString()).isEqualTo(sourceApiCollection.uuid());

        ProjectDeployment targetProjectDeployment = projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(targetApiCollection.getProjectDeploymentId(), "projectDeploymentId"));

        assertThat(targetProjectDeployment.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(targetProjectDeployment.isEnabled()).isFalse();
        assertThat(targetProjectDeployment.getProjectVersion()).isEqualTo(1);

        List<ApiCollectionEndpoint> targetEndpoints = apiCollectionEndpointService.getApiEndpoints(
            firstResult.targetId());

        assertThat(targetEndpoints).hasSize(1);

        ApiCollectionEndpoint targetGetEndpoint = targetEndpoints.getFirst();

        assertThat(targetGetEndpoint.getPath()).isEqualTo("/x");

        List<ProjectDeploymentWorkflow> targetProjectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(targetProjectDeployment.getId());

        assertThat(targetProjectDeploymentWorkflows).hasSize(1);

        ProjectDeploymentWorkflow targetProjectDeploymentWorkflow = targetProjectDeploymentWorkflows.getFirst();
        Long stableProjectDeploymentWorkflowId = targetProjectDeploymentWorkflow.getId();

        assertThat(targetGetEndpoint.getProjectDeploymentWorkflowId()).isEqualTo(stableProjectDeploymentWorkflowId);
        assertThat(connectionIds(targetProjectDeploymentWorkflow)).containsExactly(stagingConnectionId);

        projectFacade.publishProject(projectId, "v2", false);
        changeProjectVersion(sourceApiCollection.projectDeploymentId(), 2);
        createApiCollectionEndpoint(sourceApiCollectionId, HttpMethod.POST, "/y");

        EnvironmentPromotionResult secondResult = environmentPromotionFacade.promote(
            PromotionResourceType.API_COLLECTION, sourceApiCollectionId, Environment.STAGING.ordinal(), Map.of());

        assertThat(secondResult.created()).isFalse();
        assertThat(secondResult.targetId()).isEqualTo(firstResult.targetId());

        ProjectDeployment rePromotedProjectDeployment = projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(targetApiCollection.getProjectDeploymentId(), "projectDeploymentId"));

        assertThat(rePromotedProjectDeployment.getProjectVersion()).isEqualTo(2);

        List<ProjectDeploymentWorkflow> rePromotedProjectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(rePromotedProjectDeployment.getId());

        assertThat(rePromotedProjectDeploymentWorkflows).hasSize(1);

        ProjectDeploymentWorkflow rePromotedProjectDeploymentWorkflow =
            rePromotedProjectDeploymentWorkflows.getFirst();

        assertThat(rePromotedProjectDeploymentWorkflow.getId()).isEqualTo(stableProjectDeploymentWorkflowId);
        assertThat(rePromotedProjectDeploymentWorkflow.getWorkflowId())
            .isNotEqualTo(targetProjectDeploymentWorkflow.getWorkflowId());
        assertThat(connectionIds(rePromotedProjectDeploymentWorkflow)).containsExactly(stagingConnectionId);

        List<ApiCollectionEndpoint> rePromotedEndpoints = apiCollectionEndpointService.getApiEndpoints(
            firstResult.targetId());

        assertThat(rePromotedEndpoints).hasSize(2);

        for (ApiCollectionEndpoint rePromotedEndpoint : rePromotedEndpoints) {
            assertThat(rePromotedEndpoint.getProjectDeploymentWorkflowId())
                .isEqualTo(stableProjectDeploymentWorkflowId);
        }

        assertThat(rePromotedEndpoints.stream()
            .map(ApiCollectionEndpoint::getPath)
            .toList()).containsExactlyInAnyOrder("/x", "/y");
    }

    @Test
    void testMcpServerPromotionCopiesParametersAndSyncsOnRePromotion() {
        McpServer sourceMcpServer = createMcpServer("billing-mcp", Environment.DEVELOPMENT);

        long sourceMcpServerId = Objects.requireNonNull(sourceMcpServer.getId(), "id");

        McpProject sourceMcpProject = mcpProjectFacade.createMcpProject(
            sourceMcpServerId, projectId, 1, List.of(projectWorkflowId(1)));

        long sourceProjectDeploymentId =
            Objects.requireNonNull(sourceMcpProject.getProjectDeploymentId(), "projectDeploymentId");

        bindConnection(sourceProjectDeploymentId, developmentConnectionId);

        long sourceMcpProjectWorkflowId = singleMcpProjectWorkflowId(sourceMcpProject.getId());

        mcpProjectWorkflowService.updateParameters(
            sourceMcpProjectWorkflowId, Map.of("toolName", "createInvoice", "toolDescription", "Creates an invoice"));

        mcpServerFacade.create(
            new McpComponent(COMPONENT_NAME, CONNECTION_VERSION, sourceMcpServerId, developmentConnectionId),
            List.of(new McpTool("sendMessage", Map.of())));

        EnvironmentPromotionResult firstResult = environmentPromotionFacade.promote(
            PromotionResourceType.MCP_SERVER, sourceMcpServerId, Environment.STAGING.ordinal(), Map.of());

        assertThat(firstResult.created()).isTrue();
        assertThat(firstResult.unresolvedConnectionIds()).isEmpty();

        McpServer targetMcpServer = mcpServerService.getMcpServer(firstResult.targetId());

        assertThat(targetMcpServer.getUuid()).isEqualTo(sourceMcpServer.getUuid());
        assertThat(targetMcpServer.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(targetMcpServer.isEnabled()).isFalse();
        assertThat(targetMcpServer.getSecretKey()).isNotEqualTo(sourceMcpServer.getSecretKey());

        List<McpComponent> targetMcpComponents = mcpComponentService.getMcpServerMcpComponents(
            firstResult.targetId());

        assertThat(targetMcpComponents).hasSize(1);
        assertThat(targetMcpComponents.getFirst()
            .getConnectionId()).isEqualTo(stagingConnectionId);

        long targetMcpProjectId = singleMcpProjectId(firstResult.targetId());
        long targetMcpProjectWorkflowId = singleMcpProjectWorkflowId(targetMcpProjectId);

        assertThat(mcpProjectWorkflowParameters(targetMcpProjectWorkflowId))
            .containsEntry("toolName", "createInvoice")
            .containsEntry("toolDescription", "Creates an invoice");

        long stableProjectDeploymentWorkflowId = mcpProjectWorkflowDeploymentWorkflowId(targetMcpProjectWorkflowId);
        ProjectDeployment targetProjectDeployment = projectDeploymentOfWorkflow(stableProjectDeploymentWorkflowId);

        assertThat(targetProjectDeployment.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(targetProjectDeployment.getProjectVersion()).isEqualTo(1);

        projectFacade.publishProject(projectId, "v2", false);
        changeProjectVersion(sourceProjectDeploymentId, 2);
        mcpProjectWorkflowService.updateParameters(
            sourceMcpProjectWorkflowId,
            Map.of("toolName", "createInvoice", "toolDescription", "Creates a new invoice"));

        mcpServerService.update(firstResult.targetId(), targetMcpServer.getName(), true);

        EnvironmentPromotionResult secondResult = environmentPromotionFacade.promote(
            PromotionResourceType.MCP_SERVER, sourceMcpServerId, Environment.STAGING.ordinal(), Map.of());

        assertThat(secondResult.created()).isFalse();
        assertThat(secondResult.targetId()).isEqualTo(firstResult.targetId());

        McpServer rePromotedMcpServer = mcpServerService.getMcpServer(firstResult.targetId());

        assertThat(rePromotedMcpServer.isEnabled()).isTrue();

        long rePromotedMcpProjectWorkflowId = singleMcpProjectWorkflowId(singleMcpProjectId(firstResult.targetId()));

        assertThat(mcpProjectWorkflowParameters(rePromotedMcpProjectWorkflowId))
            .containsEntry("toolDescription", "Creates a new invoice");
        assertThat(mcpProjectWorkflowDeploymentWorkflowId(rePromotedMcpProjectWorkflowId))
            .isEqualTo(stableProjectDeploymentWorkflowId);

        assertThat(projectDeploymentOfWorkflow(stableProjectDeploymentWorkflowId).getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testMcpServerPromotionReturnsTheTargetUrlToAnMcpEditorWhoIsNotATenantAdmin() {
        McpServer sourceMcpServer = createMcpServer("creator-mcp", Environment.DEVELOPMENT);

        long sourceMcpServerId = Objects.requireNonNull(sourceMcpServer.getId(), "id");

        mcpProjectFacade.createMcpProject(sourceMcpServerId, projectId, 1, List.of(projectWorkflowId(1)));

        permissionService.reset();
        permissionService.setTenantAdmin(false);

        EnvironmentPromotionResult result = environmentPromotionFacade.promote(
            PromotionResourceType.MCP_SERVER, sourceMcpServerId, Environment.STAGING.ordinal(), Map.of());

        McpServer targetMcpServer = mcpServerService.getMcpServer(result.targetId());

        assertThat(result.created()).isTrue();
        assertThat(result.targetUrl()).endsWith("/api/automation/" + targetMcpServer.getSecretKey() + "/mcp");
        assertThat(permissionService.getResourceScopeChecks())
            .contains(
                new ResourceScopeCheck(workspaceId, "Workspace", "MCP_CREATE"),
                new ResourceScopeCheck(result.targetId(), "McpServer", "MCP_EDIT"));

        assertThat(targetMcpServer.getUuid()).isEqualTo(sourceMcpServer.getUuid());
        assertThat(targetMcpServer.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(singleMcpProjectId(result.targetId())).isPositive();
    }

    @Test
    void testMcpServerPromotionReturnsTheTargetUrlToATenantAdmin() {
        McpServer sourceMcpServer = createMcpServer("admin-mcp", Environment.DEVELOPMENT);

        long sourceMcpServerId = Objects.requireNonNull(sourceMcpServer.getId(), "id");

        EnvironmentPromotionResult result = environmentPromotionFacade.promote(
            PromotionResourceType.MCP_SERVER, sourceMcpServerId, Environment.STAGING.ordinal(), Map.of());

        McpServer targetMcpServer = mcpServerService.getMcpServer(result.targetId());

        assertThat(result.targetUrl()).endsWith("/api/automation/" + targetMcpServer.getSecretKey() + "/mcp");
    }

    @Test
    void testProjectDeploymentPromotionCreatesDisabledCounterpartAndPreservesTargetOnRePromotion() {
        ProjectDeployment sourceProjectDeployment = createProjectDeployment("billing-deployment");

        long sourceProjectDeploymentId = Objects.requireNonNull(sourceProjectDeployment.getId(), "id");

        bindConnection(sourceProjectDeploymentId, developmentConnectionId);

        EnvironmentPromotionResult firstResult = environmentPromotionFacade.promote(
            PromotionResourceType.PROJECT_DEPLOYMENT, sourceProjectDeploymentId, Environment.STAGING.ordinal(),
            Map.of());

        assertThat(firstResult.created()).isTrue();
        assertThat(firstResult.unresolvedConnectionIds()).isEmpty();

        ProjectDeployment targetProjectDeployment =
            projectDeploymentService.getProjectDeployment(firstResult.targetId());

        assertThat(targetProjectDeployment.getUuid()).isEqualTo(sourceProjectDeployment.getUuid());
        assertThat(targetProjectDeployment.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(targetProjectDeployment.isEnabled()).isFalse();
        assertThat(targetProjectDeployment.getProjectVersion()).isEqualTo(1);

        List<ProjectDeploymentWorkflow> targetProjectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(firstResult.targetId());

        assertThat(targetProjectDeploymentWorkflows).hasSize(1);

        Long stableProjectDeploymentWorkflowId = targetProjectDeploymentWorkflows.getFirst()
            .getId();

        assertThat(connectionIds(targetProjectDeploymentWorkflows.getFirst())).containsExactly(stagingConnectionId);

        targetProjectDeployment.setEnabled(true);
        targetProjectDeployment.setName("staging-billing-deployment");

        projectDeploymentService.update(targetProjectDeployment);

        projectFacade.publishProject(projectId, "v2", false);
        changeProjectVersion(sourceProjectDeploymentId, 2);

        EnvironmentPromotionResult secondResult = environmentPromotionFacade.promote(
            PromotionResourceType.PROJECT_DEPLOYMENT, sourceProjectDeploymentId, Environment.STAGING.ordinal(),
            Map.of());

        assertThat(secondResult.created()).isFalse();
        assertThat(secondResult.targetId()).isEqualTo(firstResult.targetId());

        ProjectDeployment rePromotedProjectDeployment =
            projectDeploymentService.getProjectDeployment(firstResult.targetId());

        assertThat(rePromotedProjectDeployment.getProjectVersion()).isEqualTo(2);
        assertThat(rePromotedProjectDeployment.isEnabled()).isTrue();
        assertThat(rePromotedProjectDeployment.getName()).isEqualTo("staging-billing-deployment");

        List<ProjectDeploymentWorkflow> rePromotedProjectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(firstResult.targetId());

        assertThat(rePromotedProjectDeploymentWorkflows).hasSize(1);
        assertThat(rePromotedProjectDeploymentWorkflows.getFirst()
            .getId()).isEqualTo(stableProjectDeploymentWorkflowId);
        assertThat(connectionIds(rePromotedProjectDeploymentWorkflows.getFirst()))
            .containsExactly(stagingConnectionId);
    }

    @Test
    void testSyntheticProjectDeploymentIsNotPromotable() {
        ApiCollectionDTO apiCollection = createApiCollection("synthetic", "synthetic", 1);

        assertThatThrownBy(
            () -> environmentPromotionFacade.promote(
                PromotionResourceType.PROJECT_DEPLOYMENT, apiCollection.projectDeploymentId(),
                Environment.STAGING.ordinal(), Map.of()))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(EnvironmentPromotionErrorType.SYNTHETIC_DEPLOYMENT_NOT_PROMOTABLE.getErrorKey());
    }

    @Test
    void testPromotionAuthorizerBeanReferenceResolvesAndGates() {
        separateApiCollectionIdsFromProjectIds();

        ApiCollectionDTO apiCollection = createApiCollection("guarded", "guarded", 1);

        long apiCollectionId = Objects.requireNonNull(apiCollection.id(), "id");

        assertThat(apiCollectionId).isNotEqualTo(projectId);

        permissionService.reset();

        environmentPromotionFacade.preview(
            PromotionResourceType.API_COLLECTION, apiCollectionId, Environment.STAGING.ordinal());

        assertThat(permissionService.getResourceScopeChecks())
            .contains(new ResourceScopeCheck(projectId, "Project", "DEPLOYMENT_PUSH"));

        permissionService.setGranted(false);

        assertThatThrownBy(
            () -> environmentPromotionFacade.preview(
                PromotionResourceType.API_COLLECTION, apiCollectionId, Environment.STAGING.ordinal()))
                    .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUniqueConstraintsMigrated() {
        List<String> constraintNames = jdbcTemplate.queryForList(
            "SELECT constraint_name FROM information_schema.table_constraints WHERE constraint_type = 'UNIQUE'",
            String.class);

        assertThat(constraintNames).doesNotContain("uk_api_collection_name", "uk_mcp_server_name");
        assertThat(constraintNames).contains(
            "uk_a2a_server_uuid_environment", "uk_mcp_server_name_environment", "uk_mcp_server_uuid_environment",
            "uk_project_deployment_uuid_environment");

        createMcpServer("shared-name", Environment.DEVELOPMENT);
        createMcpServer("shared-name", Environment.STAGING);

        assertThatThrownBy(() -> createMcpServer("shared-name", Environment.STAGING))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void bindConnection(long projectDeploymentId, long connectionId) {
        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId);

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            projectDeploymentWorkflow.setConnections(
                List.of(
                    new ProjectDeploymentWorkflowConnection(
                        connectionId, WORKFLOW_CONNECTION_KEY, WORKFLOW_NODE_NAME)));

            projectDeploymentWorkflowService.update(projectDeploymentWorkflow);
        }
    }

    private void changeProjectVersion(long projectDeploymentId, int projectVersion) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);
        Map<String, String> workflowIdsByUuid = new HashMap<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(projectId, projectVersion)) {
            workflowIdsByUuid.put(projectWorkflow.getUuidAsString(), projectWorkflow.getWorkflowId());
        }

        Map<String, String> uuidsByWorkflowId = new HashMap<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(
            projectId, projectDeployment.getProjectVersion())) {

            uuidsByWorkflowId.put(projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString());
        }

        List<ProjectDeploymentWorkflow> updatedProjectDeploymentWorkflows = new ArrayList<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(projectDeploymentId)) {

            ProjectDeploymentWorkflow updatedProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            updatedProjectDeploymentWorkflow.setConnections(projectDeploymentWorkflow.getConnections());
            updatedProjectDeploymentWorkflow.setEnabled(projectDeploymentWorkflow.isEnabled());
            updatedProjectDeploymentWorkflow.setInputs(projectDeploymentWorkflow.getInputs());
            updatedProjectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
            updatedProjectDeploymentWorkflow.setWorkflowId(
                workflowIdsByUuid.get(uuidsByWorkflowId.get(projectDeploymentWorkflow.getWorkflowId())));

            updatedProjectDeploymentWorkflows.add(updatedProjectDeploymentWorkflow);
        }

        projectDeployment.setProjectVersion(projectVersion);

        projectDeploymentFacade.updateProjectDeployment(
            projectDeployment, updatedProjectDeploymentWorkflows, List.of());
    }

    private static List<Long> connectionIds(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        List<Long> connectionIds = new ArrayList<>();

        for (ProjectDeploymentWorkflowConnection connection : projectDeploymentWorkflow.getConnections()) {
            connectionIds.add(connection.getConnectionId());
        }

        return connectionIds;
    }

    private ApiCollectionDTO createApiCollection(String name, String contextPath, int projectVersion) {
        return apiCollectionFacade.createApiCollection(
            new ApiCollectionDTO(
                1, contextPath, null, null, "description", false, List.of(), Environment.DEVELOPMENT, null, null, null,
                name, null, projectId, null, 0, projectVersion, List.of(), 0, null));
    }

    private void createApiCollectionEndpoint(long apiCollectionId, HttpMethod httpMethod, String path) {
        apiCollectionFacade.createApiCollectionEndpoint(
            new ApiCollectionEndpointDTO(
                apiCollectionId, null, null, false, httpMethod, null, null, null, "endpoint" + path, path, 0, 0,
                workflowUuid));
    }

    private long createConnection(Environment environment) {
        Connection connection = connectionService.create(
            AuthorizationType.BASIC_AUTH, COMPONENT_NAME, CONNECTION_VERSION, environment.ordinal(), CONNECTION_NAME,
            Map.of("username", "user"), PlatformType.AUTOMATION);

        return Objects.requireNonNull(connection.getId(), "id");
    }

    private A2aServer createA2aServer(String name, @Nullable String description) {
        A2aServer a2aServer = new A2aServer(name, description, Environment.DEVELOPMENT);

        a2aServer.setWorkspaceId(workspaceId);

        return a2aServerService.create(a2aServer);
    }

    private McpServer createMcpServer(String name, Environment environment) {
        return workspaceMcpServerFacade.createWorkspaceMcpServer(
            name, PlatformType.AUTOMATION, environment, false, false, false, workspaceId, null);
    }

    private ProjectDeployment createProjectDeployment(String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(1);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setWorkflowId(projectWorkflowId(1));

        long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(
            projectDeployment, List.of(projectDeploymentWorkflow), List.of());

        return projectDeploymentService.getProjectDeployment(projectDeploymentId);
    }

    private long createPublishedProject() {
        Project project = projectService.create(
            Project.builder()
                .description("test project")
                .name("Test Project")
                .workspaceId(workspaceId)
                .build());

        long createdProjectId = Objects.requireNonNull(project.getId(), "id");

        projectWorkflowFacade.addWorkflow(
            createdProjectId,
            "{\"label\": \"Test Workflow\", \"description\": \"Test\", \"triggers\": [{\"name\": " +
                "\"trigger_1\", \"type\": \"workflow/v1/newWorkflowCall\"}], \"tasks\": []}");

        projectFacade.publishProject(createdProjectId, "v1", false);

        return createdProjectId;
    }

    private void separateApiCollectionIdsFromProjectIds() {
        jdbcTemplate.queryForObject(
            "SELECT setval(pg_get_serial_sequence('api_collection', 'id'), ?)", Long.class, projectId + 1000);
    }

    private long createWorkspace() {
        Long createdWorkspaceId = jdbcTemplate.queryForObject(
            "INSERT INTO workspace (name, created_by, created_date, last_modified_by, last_modified_date, version) " +
                "VALUES ('test-workspace', 'test', now(), 'test', now(), 1) RETURNING id",
            Long.class);

        return Objects.requireNonNull(createdWorkspaceId, "id");
    }

    private long mcpProjectWorkflowDeploymentWorkflowId(long mcpProjectWorkflowId) {
        return Objects.requireNonNull(
            mcpProjectWorkflowService.fetchMcpProjectWorkflow(mcpProjectWorkflowId)
                .orElseThrow()
                .getProjectDeploymentWorkflowId(),
            "projectDeploymentWorkflowId");
    }

    private ProjectDeployment projectDeploymentOfWorkflow(long projectDeploymentWorkflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflowId);

        return projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(projectDeploymentWorkflow.getProjectDeploymentId(), "projectDeploymentId"));
    }

    private Map<String, Object> mcpProjectWorkflowParameters(long mcpProjectWorkflowId) {
        return new HashMap<>(
            mcpProjectWorkflowService.fetchMcpProjectWorkflow(mcpProjectWorkflowId)
                .orElseThrow()
                .getParameters());
    }

    private String projectWorkflowId(int projectVersion) {
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(projectId, projectVersion);

        return projectWorkflows.getFirst()
            .getWorkflowId();
    }

    private String projectWorkflowUuid(int projectVersion) {
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(projectId, projectVersion);

        return projectWorkflows.getFirst()
            .getUuidAsString();
    }

    private static Map<String, Object> a2aProjectWorkflowParameters(A2aProjectWorkflow a2aProjectWorkflow) {
        return new HashMap<>(a2aProjectWorkflow.getParameters());
    }

    private long singleA2aProjectId(long a2aServerId) {
        Long a2aProjectId = jdbcTemplate.queryForObject(
            "SELECT id FROM a2a_project WHERE a2a_server_id = ?", Long.class, a2aServerId);

        return Objects.requireNonNull(a2aProjectId, "id");
    }

    private A2aProjectWorkflow singleA2aProjectWorkflow(Long a2aProjectId) {
        List<A2aProjectWorkflow> a2aProjectWorkflows =
            a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(a2aProjectId);

        assertThat(a2aProjectWorkflows).hasSize(1);

        return a2aProjectWorkflows.getFirst();
    }

    private static void runAs(String username, String authority, Runnable runnable) {
        SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority(authority))));

        SecurityContextHolder.setContext(securityContext);

        try {
            runnable.run();
        } finally {
            SecurityContextHolder.setContext(previousSecurityContext);
        }
    }

    private long singleMcpProjectId(long mcpServerId) {
        Long mcpProjectId = jdbcTemplate.queryForObject(
            "SELECT id FROM mcp_project WHERE mcp_server_id = ?", Long.class, mcpServerId);

        return Objects.requireNonNull(mcpProjectId, "id");
    }

    private long singleMcpProjectWorkflowId(Long mcpProjectId) {
        List<McpProjectWorkflow> mcpProjectWorkflows =
            mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProjectId);

        assertThat(mcpProjectWorkflows).hasSize(1);

        return Objects.requireNonNull(
            mcpProjectWorkflows.getFirst()
                .getId(),
            "id");
    }

    @Nested
    class Dispatch {
        private final EnvironmentService environmentService = mock(EnvironmentService.class);
        private final EnvironmentPromotionHandler handler = mock(EnvironmentPromotionHandler.class);
        private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        private EnvironmentPromotionFacadeImpl facade;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void beforeEach() {
            when(handler.getResourceType()).thenReturn(PromotionResourceType.MCP_SERVER);
            when(environmentService.getEnvironments())
                .thenReturn(List.of(Environment.DEVELOPMENT, Environment.STAGING, Environment.PRODUCTION));
            when(environmentService.getEnvironment(Environment.STAGING.ordinal())).thenReturn(Environment.STAGING);

            ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

            when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

            facade = new EnvironmentPromotionFacadeImpl(environmentService, List.of(handler), meterRegistryProvider);
        }

        @Test
        void testPreviewDispatchesToHandlerByResourceType() {
            EnvironmentPromotionPreview preview = new EnvironmentPromotionPreview(
                PromotionResourceType.MCP_SERVER, 9L, Environment.DEVELOPMENT, Environment.STAGING, null, null,
                List.of(), List.of(), List.of());

            when(handler.preview(9L, Environment.STAGING)).thenReturn(preview);

            assertThat(facade.preview(PromotionResourceType.MCP_SERVER, 9L, Environment.STAGING.ordinal()))
                .isSameAs(preview);
        }

        @Test
        void testUnavailableTargetEnvironmentIsRejected() {
            when(environmentService.getEnvironments()).thenReturn(List.of(Environment.DEVELOPMENT));

            assertThatThrownBy(
                () -> facade.preview(PromotionResourceType.MCP_SERVER, 9L, Environment.STAGING.ordinal()))
                    .isInstanceOf(ConfigurationException.class);
        }

        @Test
        void testUnknownResourceTypeIsRejected() {
            assertThatThrownBy(
                () -> facade.preview(PromotionResourceType.PROJECT_DEPLOYMENT, 9L, Environment.STAGING.ordinal()))
                    .isInstanceOf(ConfigurationException.class);
        }

        @Test
        void testPromoteRecordsCreatedOutcome() {
            when(handler.promote(9L, Environment.STAGING, Map.of()))
                .thenReturn(new EnvironmentPromotionResult(77L, true, null, List.of()));

            facade.promote(PromotionResourceType.MCP_SERVER, 9L, Environment.STAGING.ordinal(), Map.of());

            assertThat(
                meterRegistry.counter("bytechef_environment_promotion", "resource", "mcp_server", "outcome", "created")
                    .count()).isEqualTo(1.0);
        }

        @Test
        void testPromoteRecordsUpdatedOutcome() {
            when(handler.promote(9L, Environment.STAGING, Map.of()))
                .thenReturn(new EnvironmentPromotionResult(77L, false, null, List.of()));

            facade.promote(PromotionResourceType.MCP_SERVER, 9L, Environment.STAGING.ordinal(), Map.of());

            assertThat(
                meterRegistry.counter("bytechef_environment_promotion", "resource", "mcp_server", "outcome", "updated")
                    .count()).isEqualTo(1.0);
        }

        @Test
        void testPromoteRecordsFailedOutcomeAndRethrows() {
            when(handler.promote(9L, Environment.STAGING, Map.of())).thenThrow(new IllegalStateException("boom"));

            assertThatThrownBy(
                () -> facade.promote(PromotionResourceType.MCP_SERVER, 9L, Environment.STAGING.ordinal(), Map.of()))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(
                meterRegistry.counter("bytechef_environment_promotion", "resource", "mcp_server", "outcome", "failed")
                    .count()).isEqualTo(1.0);
        }

        @Test
        @SuppressWarnings("unchecked")
        void testDuplicateResourceTypeHandlersFailFast() {
            EnvironmentPromotionHandler duplicateHandler = mock(EnvironmentPromotionHandler.class);

            when(duplicateHandler.getResourceType()).thenReturn(PromotionResourceType.MCP_SERVER);

            ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

            assertThatThrownBy(() -> new EnvironmentPromotionFacadeImpl(
                environmentService, List.of(handler, duplicateHandler), meterRegistryProvider))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @SuppressWarnings("unchecked")
        void testNoRegisteredHandlersRejectsEveryResourceType() {
            ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

            when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

            EnvironmentPromotionFacadeImpl emptyFacade =
                new EnvironmentPromotionFacadeImpl(environmentService, List.of(), meterRegistryProvider);

            assertThatThrownBy(
                () -> emptyFacade.preview(PromotionResourceType.MCP_SERVER, 9L, Environment.STAGING.ordinal()))
                    .isInstanceOf(ConfigurationException.class);
        }
    }
}
