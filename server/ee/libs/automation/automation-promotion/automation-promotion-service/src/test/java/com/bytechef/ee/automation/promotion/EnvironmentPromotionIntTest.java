/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.bytechef.ee.automation.promotion.config.EnvironmentPromotionIntTestConfiguration;
import com.bytechef.ee.automation.promotion.config.EnvironmentPromotionIntTestConfigurationSharedMocks;
import com.bytechef.ee.automation.promotion.config.RecordingPermissionService;
import com.bytechef.ee.automation.promotion.config.RecordingPermissionService.ResourceScopeCheck;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * End-to-end promotion coverage against a real PostgreSQL schema and the real beans of every module a promotion
 * touches. Three things are only checkable here, and every unit test written so far takes them on trust:
 *
 * <ol>
 * <li>the {@code @promotionAuthorizer} bean reference inside each handler's {@code @PreAuthorize} expression actually
 * resolves and evaluates — see {@link #testPromotionAuthorizerBeanReferenceResolvesAndGates()};</li>
 * <li>{@code ProjectDeploymentFacade#updateProjectDeployment} updates {@code project_deployment_workflow} rows IN
 * PLACE, keeping the ids that {@code api_collection_endpoint}, {@code mcp_project_workflow} and
 * {@code a2a_project_workflow} hold foreign keys to;</li>
 * <li>the lineage-uuid changesets and the constraint migration they carry actually applied.</li>
 * </ol>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EnvironmentPromotionIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@EnvironmentPromotionIntTestConfigurationSharedMocks
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
public class EnvironmentPromotionIntTest {

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

        // Re-pointing an enabled deployment workflow stops the jobs already running against it.
        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt()))
            .thenReturn(Page.empty());

        // The MCP and A2A facades mint their deployment workflows enabled, and an enabled deployment workflow is
        // validated against the component connection declared on its workflow node. No component definition exists
        // in this context, so the node's declaration is supplied here.
        when(componentConnectionFacade.getComponentConnection(anyString(), anyString(), anyString()))
            .thenReturn(
                new ComponentConnection(
                    COMPONENT_NAME, CONNECTION_VERSION, WORKFLOW_NODE_NAME, WORKFLOW_CONNECTION_KEY, true));

        // Stands in for the workspace-scoped connection listing: the real facade projects a Connection through a
        // component's ConnectionDefinition, which no component supplies here. The filtering it performs is the
        // filtering ConnectionEnvironmentMapper depends on, so it is reproduced over the real connection rows.
        //
        // It deliberately ignores workspaceId and tagId: this fixture has exactly one workspace and no connection
        // tags, so there is nothing for either to exclude. What "connections re-bound" therefore proves is
        // component/version/environment/name matching plus the deployment facade's environment validation — NOT
        // workspace-scoped visibility, which the real facade resolves through workspace_connection rows and
        // ResourceVisibilityResolver and which no assertion here reaches.
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
        // CASCADE reaches every child table; identities deliberately keep counting up so a stale WorkflowService
        // cache entry can never be hit by a re-used workflow id in a later test.
        jdbcTemplate.execute(
            "TRUNCATE TABLE a2a_server, api_collection, category, connection, mcp_server, project, " +
                "project_deployment, project_workflow, tag, workflow, workspace CASCADE");
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

        // A second published version, the source collection moved onto it, and a second endpoint on the same
        // workflow: the shape a re-promotion has to survive without recreating a single mapping row.
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

        // The claim the whole non-destructive design rests on: same row, new workflow id.
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

        // Re-promotion after a version bump and a changed tool description.
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

        // Without this the unchanged mapping FK above would be equally consistent with nothing having moved at all.
        assertThat(projectDeploymentOfWorkflow(stableProjectDeploymentWorkflowId).getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testA2aServerPromotionCreatesTargetInTargetEnvironment() {
        A2aServer sourceA2aServer = a2aServerService.create(
            new A2aServer("billing-a2a", "Billing agent", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        long sourceA2aServerId = Objects.requireNonNull(sourceA2aServer.getId(), "id");

        A2aProject sourceA2aProject = a2aProjectFacade.createA2aProject(
            sourceA2aServerId, projectId, 1, List.of(projectWorkflowId(1)));

        bindConnection(
            Objects.requireNonNull(sourceA2aProject.getProjectDeploymentId(), "projectDeploymentId"),
            developmentConnectionId);

        A2aProjectWorkflow sourceA2aProjectWorkflow = singleA2aProjectWorkflow(sourceA2aProject.getId());

        a2aProjectWorkflowService.updateParameters(
            Objects.requireNonNull(sourceA2aProjectWorkflow.getId(), "id"),
            Map.of("skillName", "invoice", "skillDescription", "Creates an invoice"));

        EnvironmentPromotionResult result = environmentPromotionFacade.promote(
            PromotionResourceType.A2A_SERVER, sourceA2aServerId, Environment.STAGING.ordinal(), Map.of());

        assertThat(result.created()).isTrue();
        assertThat(result.unresolvedConnectionIds()).isEmpty();

        A2aServer targetA2aServer = a2aServerService.getA2aServer(result.targetId());

        assertThat(targetA2aServer.getUuid()).isEqualTo(sourceA2aServer.getUuid());
        assertThat(targetA2aServer.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(targetA2aServer.isEnabled()).isFalse();
        assertThat(targetA2aServer.getSecretKey()).isNotEqualTo(sourceA2aServer.getSecretKey());

        long targetA2aProjectId = singleA2aProjectId(result.targetId());
        A2aProjectWorkflow targetA2aProjectWorkflow = singleA2aProjectWorkflow(targetA2aProjectId);

        Map<String, Object> targetParameters = new HashMap<>(targetA2aProjectWorkflow.getParameters());

        assertThat(targetParameters)
            .containsEntry("skillName", "invoice")
            .containsEntry("skillDescription", "Creates an invoice");

        ProjectDeploymentWorkflow targetProjectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                Objects.requireNonNull(
                    targetA2aProjectWorkflow.getProjectDeploymentWorkflowId(), "projectDeploymentWorkflowId"));

        ProjectDeployment targetProjectDeployment = projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(targetProjectDeploymentWorkflow.getProjectDeploymentId(), "projectDeploymentId"));

        // The Task 1 fix: the counterpart's deployment lands in the TARGET environment, not the source's.
        assertThat(targetProjectDeployment.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(connectionIds(targetProjectDeploymentWorkflow)).containsExactly(stagingConnectionId);
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

        // What the target environment owns and a re-promotion must not overwrite.
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

        // Load-bearing, do not delete as redundant. The assertion below only tells "the authorizer resolved the
        // OWNING PROJECT" apart from "the raw #sourceId was passed straight through" while the two ids differ, and
        // without separateApiCollectionIdsFromProjectIds() above they are equal whenever this test happens to run
        // first. That would leave the assertion passing while proving nothing.
        assertThat(apiCollectionId).isNotEqualTo(projectId);

        permissionService.reset();

        environmentPromotionFacade.preview(
            PromotionResourceType.API_COLLECTION, apiCollectionId, Environment.STAGING.ordinal());

        // @promotionAuthorizer.projectIdOfApiCollection(#sourceId) must have run and produced this project's id;
        // an unresolved bean reference would have failed the expression instead of reaching the evaluator.
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
            "uk_mcp_server_name_environment", "uk_mcp_server_uuid_environment", "uk_a2a_server_uuid_environment",
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

    /**
     * Moves a deployment onto another published project version exactly the way the "Change Project Version" action
     * does — through the same facade call promotion itself uses.
     */
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
            createdProjectId, "{\"label\": \"Test Workflow\", \"description\": \"Test\", \"tasks\": []}");

        projectFacade.publishProject(createdProjectId, "v1", false);

        return createdProjectId;
    }

    /**
     * Pushes the {@code api_collection} identity sequence well past the current project id, so the collection this test
     * promotes and the project it belongs to CANNOT share an id. JUnit's hash-based method ordering plus a
     * {@code TRUNCATE} that deliberately does not restart identities otherwise leaves that distinctness to chance, and
     * it is the whole basis of the authorizer assertion.
     */
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

    private A2aProjectWorkflow singleA2aProjectWorkflow(Long a2aProjectId) {
        List<A2aProjectWorkflow> a2aProjectWorkflows =
            a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(a2aProjectId);

        assertThat(a2aProjectWorkflows).hasSize(1);

        return a2aProjectWorkflows.getFirst();
    }

    private long singleA2aProjectId(long a2aServerId) {
        Long a2aProjectId = jdbcTemplate.queryForObject(
            "SELECT id FROM a2a_project WHERE a2a_server_id = ?", Long.class, a2aServerId);

        return Objects.requireNonNull(a2aProjectId, "id");
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
}
