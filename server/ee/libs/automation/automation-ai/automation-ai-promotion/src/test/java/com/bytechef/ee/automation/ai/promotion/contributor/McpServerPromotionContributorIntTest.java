/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.promotion.contributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

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
import com.bytechef.ee.automation.ai.promotion.config.AiServerPromotionIntTestConfiguration;
import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionChange.Kind;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.test.config.PromotionIntTestSharedMocks;
import com.bytechef.ee.automation.promotion.test.config.RecordingPermissionService;
import com.bytechef.ee.automation.promotion.test.config.RecordingPermissionService.ResourceScopeCheck;
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
import com.bytechef.platform.mcp.service.McpToolService;
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
import org.springframework.security.test.context.support.WithMockUser;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AiServerPromotionIntTestConfiguration.class, properties = "bytechef.webhook-url=/webhooks/{id}")
@Import(PostgreSQLContainerConfiguration.class)
@PromotionIntTestSharedMocks
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
class McpServerPromotionContributorIntTest {

    private static final String COMPONENT_NAME = "slack";
    private static final String EMPTY_DESTRUCTIVE_CHANGES_HASH =
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final int CONNECTION_VERSION = 1;
    private static final String CONNECTION_NAME = "slack-account";
    private static final String WORKFLOW_CONNECTION_KEY = "slack";
    private static final String WORKFLOW_NODE_NAME = "slack_1";

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ConnectionService connectionService;

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
    private EnvironmentPromotionContributor mcpServerPromotionContributor;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private McpToolService mcpToolService;

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
        projectId = createPublishedProject("Test Project", 1);

        developmentConnectionId = createConnection(Environment.DEVELOPMENT);
        stagingConnectionId = createConnection(Environment.STAGING);
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute(
            "TRUNCATE TABLE a2a_server, category, connection, mcp_server, project, project_deployment, " +
                "project_workflow, tag, workflow, workspace CASCADE");
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

        EnvironmentPromotionResult firstResult = mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

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

        McpComponent targetMcpComponent = targetMcpComponents.getFirst();

        assertThat(targetMcpComponent.getConnectionId()).isEqualTo(stagingConnectionId);

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

        EnvironmentPromotionResult secondResult = mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        assertThat(secondResult.created()).isFalse();
        assertThat(secondResult.targetId()).isEqualTo(firstResult.targetId());

        McpServer rePromotedMcpServer = mcpServerService.getMcpServer(firstResult.targetId());

        assertThat(rePromotedMcpServer.isEnabled()).isTrue();

        long rePromotedMcpProjectWorkflowId = singleMcpProjectWorkflowId(singleMcpProjectId(firstResult.targetId()));

        assertThat(mcpProjectWorkflowParameters(rePromotedMcpProjectWorkflowId))
            .containsEntry("toolDescription", "Creates a new invoice");
        assertThat(mcpProjectWorkflowDeploymentWorkflowId(rePromotedMcpProjectWorkflowId))
            .isEqualTo(stableProjectDeploymentWorkflowId);

        ProjectDeployment stableProjectDeployment = projectDeploymentOfWorkflow(stableProjectDeploymentWorkflowId);

        assertThat(stableProjectDeployment.getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testMcpServerPromotionReturnsTheTargetUrlToAnMcpEditorWhoIsNotATenantAdmin() {
        McpServer sourceMcpServer = createMcpServer("creator-mcp", Environment.DEVELOPMENT);

        long sourceMcpServerId = Objects.requireNonNull(sourceMcpServer.getId(), "id");

        mcpProjectFacade.createMcpProject(sourceMcpServerId, projectId, 1, List.of(projectWorkflowId(1)));

        permissionService.reset();
        permissionService.setTenantAdmin(false);

        EnvironmentPromotionResult result = mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

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

        EnvironmentPromotionResult result = mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        McpServer targetMcpServer = mcpServerService.getMcpServer(result.targetId());

        assertThat(result.targetUrl()).endsWith("/api/automation/" + targetMcpServer.getSecretKey() + "/mcp");
    }

    @Test
    void testPreviewDeletesEqualPromoteDeletes() {
        long twoWorkflowProjectId = createPublishedProject("Two Workflow Project", 2);
        long extraProjectId = createPublishedProject("Extra Project", 1);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(twoWorkflowProjectId, 1);

        ProjectWorkflow keptProjectWorkflow = projectWorkflows.get(0);
        ProjectWorkflow extraProjectWorkflow = projectWorkflows.get(1);

        McpServer sourceMcpServer = createMcpServer("deletes-mcp", Environment.DEVELOPMENT);

        long sourceMcpServerId = Objects.requireNonNull(sourceMcpServer.getId(), "id");

        mcpProjectFacade.createMcpProject(
            sourceMcpServerId, twoWorkflowProjectId, 1, List.of(keptProjectWorkflow.getWorkflowId()));
        mcpServerFacade.create(
            new McpComponent(COMPONENT_NAME, CONNECTION_VERSION, sourceMcpServerId, developmentConnectionId),
            List.of(new McpTool("sendMessage", Map.of())));

        EnvironmentPromotionResult firstResult = mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        long targetId = firstResult.targetId();
        long keptTargetMcpProjectId = singleMcpProjectId(targetId);

        mcpProjectFacade.updateMcpProject(
            keptTargetMcpProjectId,
            List.of(keptProjectWorkflow.getWorkflowId(), extraProjectWorkflow.getWorkflowId()));

        McpProject extraTargetMcpProject = mcpProjectFacade.createMcpProject(
            targetId, extraProjectId, 1, List.of(projectWorkflowId(extraProjectId, 1)));

        ProjectDeployment extraTargetProjectDeployment = projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(extraTargetMcpProject.getProjectDeploymentId(), "projectDeploymentId"));

        mcpServerFacade.create(
            new McpComponent("github", 1, targetId, null), List.of(new McpTool("createIssue", Map.of())));

        long keptTargetMcpComponentId = mcpComponentId(targetId, COMPONENT_NAME);

        mcpServerFacade.createMcpTool(new McpTool("extraTool", Map.of(), keptTargetMcpComponentId));

        EnvironmentPromotionPreview preview = mcpServerPromotionContributor.preview(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING);

        ProjectDeployment keptTargetMcpProjectDeployment = mcpProjectDeployment(keptTargetMcpProjectId);

        String extraWorkflowKey =
            keptTargetMcpProjectDeployment.getUuid() + ":" + extraProjectWorkflow.getUuidAsString();

        assertThat(destructiveChangeLines(preview)).containsExactlyInAnyOrder(
            "MCP_PROJECT||" + extraProjectId + ":" + extraTargetProjectDeployment.getUuid(),
            "MCP_COMPONENT||github@1",
            "MCP_TOOL|slack@1|extraTool",
            "DEPLOYMENT_WORKFLOW|Two Workflow Project|" + extraWorkflowKey,
            "EXPOSED_WORKFLOW|Two Workflow Project|" + extraWorkflowKey);

        mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(),
            preview.destructiveChangesHash());

        assertThat(singleMcpProjectId(targetId)).isEqualTo(keptTargetMcpProjectId);
        assertThat(mcpComponentNames(targetId)).containsExactly(COMPONENT_NAME);
        assertThat(mcpToolNames(keptTargetMcpComponentId)).containsExactly("sendMessage");

        long keptTargetMcpProjectWorkflowId = singleMcpProjectWorkflowId(keptTargetMcpProjectId);
        long keptTargetProjectDeploymentWorkflowId =
            mcpProjectWorkflowDeploymentWorkflowId(keptTargetMcpProjectWorkflowId);
        ProjectDeployment keptTargetProjectDeployment =
            projectDeploymentOfWorkflow(keptTargetProjectDeploymentWorkflowId);

        assertThat(
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
                Objects.requireNonNull(keptTargetProjectDeployment.getId(), "id")))
                    .hasSize(1);

        EnvironmentPromotionPreview secondPreview = mcpServerPromotionContributor.preview(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(destructiveChangeLines(secondPreview)).isEmpty();
        assertThat(secondPreview.destructiveChangesHash()).isEqualTo(EMPTY_DESTRUCTIVE_CHANGES_HASH);
    }

    @Test
    void testComponentVersionChangeIsDeleteAndCreate() {
        McpServer sourceMcpServer = createMcpServer("version-mcp", Environment.DEVELOPMENT);

        long sourceMcpServerId = Objects.requireNonNull(sourceMcpServer.getId(), "id");

        McpComponent sourceMcpComponent = mcpServerFacade.create(
            new McpComponent(COMPONENT_NAME, 1, sourceMcpServerId, developmentConnectionId),
            List.of(new McpTool("sendMessage", Map.of())));

        mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        mcpServerFacade.deleteMcpComponent(Objects.requireNonNull(sourceMcpComponent.getId(), "id"));
        mcpServerFacade.create(
            new McpComponent(COMPONENT_NAME, 2, sourceMcpServerId, developmentConnectionId),
            List.of(new McpTool("sendMessage", Map.of())));

        EnvironmentPromotionPreview preview = mcpServerPromotionContributor.preview(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(changeLines(preview)).containsExactlyInAnyOrder(
            "DELETE|MCP_COMPONENT||slack@1",
            "CREATE|MCP_COMPONENT||slack@2",
            "CREATE|MCP_TOOL|slack@2|sendMessage");
        assertThat(destructiveChangeLines(preview)).containsExactly("MCP_COMPONENT||slack@1");
    }

    @Test
    void testDeletedComponentDoesNotListItsTools() {
        McpServer sourceMcpServer = createMcpServer("component-mcp", Environment.DEVELOPMENT);

        long sourceMcpServerId = Objects.requireNonNull(sourceMcpServer.getId(), "id");

        mcpServerFacade.create(
            new McpComponent(COMPONENT_NAME, CONNECTION_VERSION, sourceMcpServerId, developmentConnectionId),
            List.of(new McpTool("sendMessage", Map.of())));

        EnvironmentPromotionResult result = mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        mcpServerFacade.create(
            new McpComponent("github", 1, result.targetId(), null),
            List.of(new McpTool("createIssue", Map.of()), new McpTool("closeIssue", Map.of())));

        EnvironmentPromotionPreview preview = mcpServerPromotionContributor.preview(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(changeLines(preview)).containsExactly("DELETE|MCP_COMPONENT||github@1");
    }

    @Test
    void testDuplicateProjectsAreSeparateChanges() {
        McpServer sourceMcpServer = createMcpServer("duplicate-mcp", Environment.DEVELOPMENT);

        long sourceMcpServerId = Objects.requireNonNull(sourceMcpServer.getId(), "id");

        mcpProjectFacade.createMcpProject(sourceMcpServerId, projectId, 1, List.of(projectWorkflowId(1)));

        McpProject secondSourceMcpProject = mcpProjectFacade.createMcpProject(
            sourceMcpServerId, projectId, 1, List.of(projectWorkflowId(1)));

        EnvironmentPromotionPreview freshPreview = mcpServerPromotionContributor.preview(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING);

        List<PromotionChange> createdProjectChanges = changesOfType(freshPreview, "MCP_PROJECT");

        assertThat(createdProjectChanges).hasSize(2);
        assertThat(createdProjectChanges).extracting(PromotionChange::kind)
            .containsOnly(Kind.CREATE);
        assertThat(createdProjectChanges).extracting(PromotionChange::key)
            .doesNotHaveDuplicates()
            .allMatch(key -> key.startsWith(projectId + ":"));

        EnvironmentPromotionResult result = mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        List<Long> targetMcpProjectIds = mcpProjectIds(result.targetId());

        assertThat(targetMcpProjectIds).hasSize(2);

        EnvironmentPromotionPreview unchangedPreview = mcpServerPromotionContributor.preview(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(changesOfType(unchangedPreview, "MCP_PROJECT")).isEmpty();

        mcpProjectFacade.deleteMcpProject(Objects.requireNonNull(secondSourceMcpProject.getId(), "id"));

        long secondTargetMcpProjectId = targetMcpProjectIds.get(1);

        ProjectDeployment secondTargetProjectDeployment = mcpProjectDeployment(secondTargetMcpProjectId);

        EnvironmentPromotionPreview preview = mcpServerPromotionContributor.preview(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(destructiveChangeLines(preview))
            .containsExactly("MCP_PROJECT||" + projectId + ":" + secondTargetProjectDeployment.getUuid());

        mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(),
            preview.destructiveChangesHash());

        assertThat(mcpProjectIds(result.targetId())).containsExactly(targetMcpProjectIds.get(0));
    }

    @Test
    void testDuplicateProjectsKeyTheirWorkflowChangesByDeployment() {
        long twoWorkflowProjectId = createPublishedProject("Two Workflow Project", 2);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(twoWorkflowProjectId, 1);

        ProjectWorkflow keptProjectWorkflow = projectWorkflows.get(0);
        ProjectWorkflow extraProjectWorkflow = projectWorkflows.get(1);

        List<String> keptWorkflowIds = List.of(keptProjectWorkflow.getWorkflowId());
        List<String> bothWorkflowIds =
            List.of(keptProjectWorkflow.getWorkflowId(), extraProjectWorkflow.getWorkflowId());

        McpServer sourceMcpServer = createMcpServer("duplicate-workflows-mcp", Environment.DEVELOPMENT);

        long sourceMcpServerId = Objects.requireNonNull(sourceMcpServer.getId(), "id");

        mcpProjectFacade.createMcpProject(sourceMcpServerId, twoWorkflowProjectId, 1, keptWorkflowIds);
        mcpProjectFacade.createMcpProject(sourceMcpServerId, twoWorkflowProjectId, 1, keptWorkflowIds);

        EnvironmentPromotionResult result = mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        List<Long> targetMcpProjectIds = mcpProjectIds(result.targetId());

        long copyAMcpProjectId = targetMcpProjectIds.get(0);
        long copyBMcpProjectId = targetMcpProjectIds.get(1);

        ProjectDeployment copyAProjectDeployment = mcpProjectDeployment(copyAMcpProjectId);
        ProjectDeployment copyBProjectDeployment = mcpProjectDeployment(copyBMcpProjectId);

        mcpProjectFacade.updateMcpProject(copyBMcpProjectId, bothWorkflowIds);

        EnvironmentPromotionPreview copyBPreview = mcpServerPromotionContributor.preview(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING);

        List<PromotionChange> copyBExposedWorkflowChanges = changesOfType(copyBPreview, "EXPOSED_WORKFLOW");

        assertThat(copyBExposedWorkflowChanges).hasSize(1);

        PromotionChange copyBExposedWorkflowChange = copyBExposedWorkflowChanges.getFirst();

        assertThat(copyBExposedWorkflowChange.kind()).isEqualTo(Kind.DELETE);
        assertThat(copyBExposedWorkflowChange.destructive()).isTrue();
        assertThat(copyBExposedWorkflowChange.key())
            .isEqualTo(copyBProjectDeployment.getUuid() + ":" + extraProjectWorkflow.getUuidAsString());

        mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(),
            copyBPreview.destructiveChangesHash());

        mcpProjectFacade.updateMcpProject(copyAMcpProjectId, bothWorkflowIds);

        EnvironmentPromotionPreview copyAPreview = mcpServerPromotionContributor.preview(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING);

        List<PromotionChange> copyAExposedWorkflowChanges = changesOfType(copyAPreview, "EXPOSED_WORKFLOW");

        assertThat(copyAExposedWorkflowChanges).hasSize(1);

        PromotionChange copyAExposedWorkflowChange = copyAExposedWorkflowChanges.getFirst();

        assertThat(copyAExposedWorkflowChange.key())
            .isEqualTo(copyAProjectDeployment.getUuid() + ":" + extraProjectWorkflow.getUuidAsString());
        assertThat(copyAPreview.destructiveChangesHash()).isNotEqualTo(copyBPreview.destructiveChangesHash());
    }

    @Test
    void testUnchangedServerHasNoChanges() {
        McpServer sourceMcpServer = createMcpServer("unchanged-mcp", Environment.DEVELOPMENT);

        long sourceMcpServerId = Objects.requireNonNull(sourceMcpServer.getId(), "id");

        McpProject sourceMcpProject = mcpProjectFacade.createMcpProject(
            sourceMcpServerId, projectId, 1, List.of(projectWorkflowId(1)));

        mcpProjectWorkflowService.updateParameters(
            singleMcpProjectWorkflowId(sourceMcpProject.getId()), Map.of("toolName", "createInvoice"));
        mcpServerFacade.create(
            new McpComponent(COMPONENT_NAME, CONNECTION_VERSION, sourceMcpServerId, developmentConnectionId),
            List.of(new McpTool("sendMessage", Map.of("channel", "general"))));

        EnvironmentPromotionPreview freshPreview = mcpServerPromotionContributor.preview(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(freshPreview.changes()).extracting(PromotionChange::kind)
            .containsOnly(Kind.CREATE);
        assertThat(changeLines(freshPreview)).contains(
            "CREATE|MCP_COMPONENT||slack@1", "CREATE|MCP_TOOL|slack@1|sendMessage");

        mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        EnvironmentPromotionPreview preview = mcpServerPromotionContributor.preview(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(preview.changes()).isEmpty();
        assertThat(preview.destructiveChangesHash()).isEqualTo(EMPTY_DESTRUCTIVE_CHANGES_HASH);
    }

    @Test
    void testPromoteWithStaleHashWritesNothing() {
        McpServer sourceMcpServer = createMcpServer("stale-mcp", Environment.DEVELOPMENT);

        long sourceMcpServerId = Objects.requireNonNull(sourceMcpServer.getId(), "id");

        McpProject sourceMcpProject = mcpProjectFacade.createMcpProject(
            sourceMcpServerId, projectId, 1, List.of(projectWorkflowId(1)));

        long sourceMcpProjectWorkflowId = singleMcpProjectWorkflowId(sourceMcpProject.getId());

        mcpProjectWorkflowService.updateParameters(sourceMcpProjectWorkflowId, Map.of("toolName", "createInvoice"));
        mcpServerFacade.create(
            new McpComponent(COMPONENT_NAME, CONNECTION_VERSION, sourceMcpServerId, developmentConnectionId),
            List.of(new McpTool("sendMessage", Map.of())));

        EnvironmentPromotionResult result = mcpServerPromotionContributor.promote(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        long targetId = result.targetId();

        EnvironmentPromotionPreview preview = mcpServerPromotionContributor.preview(
            sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING);

        long targetMcpComponentId = mcpComponentId(targetId, COMPONENT_NAME);

        mcpServerFacade.createMcpTool(new McpTool("extraTool", Map.of(), targetMcpComponentId));
        mcpProjectWorkflowService.updateParameters(sourceMcpProjectWorkflowId, Map.of("toolName", "renamedTool"));

        assertThatThrownBy(
            () -> mcpServerPromotionContributor.promote(
                sourceMcpServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(),
                preview.destructiveChangesHash()))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(EnvironmentPromotionErrorType.PROMOTION_PREVIEW_STALE.getErrorKey());

        assertThat(mcpToolNames(targetMcpComponentId)).containsExactlyInAnyOrder("sendMessage", "extraTool");

        long targetMcpProjectWorkflowId = singleMcpProjectWorkflowId(singleMcpProjectId(targetId));

        assertThat(mcpProjectWorkflowParameters(targetMcpProjectWorkflowId))
            .containsEntry("toolName", "createInvoice");
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

    private static List<String> changeLines(EnvironmentPromotionPreview preview) {
        List<String> changeLines = new ArrayList<>();

        for (PromotionChange change : preview.changes()) {
            changeLines.add(change.kind() + "|" + changeLine(change));
        }

        return changeLines;
    }

    private static String changeLine(PromotionChange change) {
        String parentName = change.parentName();

        return change.assetType() + "|" + (parentName == null ? "" : parentName) + "|" + change.key();
    }

    private static List<PromotionChange> changesOfType(EnvironmentPromotionPreview preview, String assetType) {
        List<PromotionChange> changes = new ArrayList<>();

        for (PromotionChange change : preview.changes()) {
            if (Objects.equals(change.assetType(), assetType)) {
                changes.add(change);
            }
        }

        return changes;
    }

    private static List<String> destructiveChangeLines(EnvironmentPromotionPreview preview) {
        List<String> destructiveChangeLines = new ArrayList<>();

        for (PromotionChange change : preview.changes()) {
            if (change.destructive()) {
                destructiveChangeLines.add(changeLine(change));
            }
        }

        return destructiveChangeLines;
    }

    private long mcpComponentId(long mcpServerId, String componentName) {
        for (McpComponent mcpComponent : mcpComponentService.getMcpServerMcpComponents(mcpServerId)) {
            if (Objects.equals(mcpComponent.getComponentName(), componentName)) {
                return Objects.requireNonNull(mcpComponent.getId(), "id");
            }
        }

        throw new AssertionError("No MCP component " + componentName + " on server " + mcpServerId);
    }

    private List<String> mcpComponentNames(long mcpServerId) {
        List<String> componentNames = new ArrayList<>();

        for (McpComponent mcpComponent : mcpComponentService.getMcpServerMcpComponents(mcpServerId)) {
            componentNames.add(mcpComponent.getComponentName());
        }

        return componentNames;
    }

    private ProjectDeployment mcpProjectDeployment(long mcpProjectId) {
        Long projectDeploymentId = jdbcTemplate.queryForObject(
            "SELECT project_deployment_id FROM mcp_project WHERE id = ?", Long.class, mcpProjectId);

        return projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(projectDeploymentId, "projectDeploymentId"));
    }

    private List<Long> mcpProjectIds(long mcpServerId) {
        return jdbcTemplate.queryForList(
            "SELECT id FROM mcp_project WHERE mcp_server_id = ? ORDER BY id", Long.class, mcpServerId);
    }

    private List<String> mcpToolNames(long mcpComponentId) {
        List<String> toolNames = new ArrayList<>();

        for (McpTool mcpTool : mcpToolService.getMcpComponentMcpTools(mcpComponentId)) {
            toolNames.add(mcpTool.getName());
        }

        return toolNames;
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

    private long createPublishedProject(String name, int workflowCount) {
        Project project = projectService.create(
            Project.builder()
                .description("test project")
                .name(name)
                .workspaceId(workspaceId)
                .build());

        long createdProjectId = Objects.requireNonNull(project.getId(), "id");

        for (int index = 1; index <= workflowCount; index++) {
            projectWorkflowFacade.addWorkflow(
                createdProjectId,
                "{\"label\": \"Test Workflow " + index + "\", \"description\": \"Test\", \"triggers\": " +
                    "[{\"name\": \"trigger_1\", \"type\": \"workflow/v1/newWorkflowCall\"}], \"tasks\": []}");
        }

        projectFacade.publishProject(createdProjectId, "v1", false);

        return createdProjectId;
    }

    private long createWorkspace() {
        Long createdWorkspaceId = jdbcTemplate.queryForObject(
            "INSERT INTO workspace (name, created_by, created_date, last_modified_by, last_modified_date, version) " +
                "VALUES ('test-workspace', 'test', now(), 'test', now(), 1) RETURNING id",
            Long.class);

        return Objects.requireNonNull(createdWorkspaceId, "id");
    }

    private long mcpProjectWorkflowDeploymentWorkflowId(long mcpProjectWorkflowId) {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService.fetchMcpProjectWorkflow(mcpProjectWorkflowId)
            .orElseThrow();

        return Objects.requireNonNull(
            mcpProjectWorkflow.getProjectDeploymentWorkflowId(), "projectDeploymentWorkflowId");
    }

    private ProjectDeployment projectDeploymentOfWorkflow(long projectDeploymentWorkflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflowId);

        return projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(projectDeploymentWorkflow.getProjectDeploymentId(), "projectDeploymentId"));
    }

    private Map<String, Object> mcpProjectWorkflowParameters(long mcpProjectWorkflowId) {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService.fetchMcpProjectWorkflow(mcpProjectWorkflowId)
            .orElseThrow();

        return new HashMap<>(mcpProjectWorkflow.getParameters());
    }

    private String projectWorkflowId(int projectVersion) {
        return projectWorkflowId(projectId, projectVersion);
    }

    private String projectWorkflowId(long workflowProjectId, int projectVersion) {
        List<ProjectWorkflow> projectWorkflows =
            projectWorkflowService.getProjectWorkflows(workflowProjectId, projectVersion);

        ProjectWorkflow projectWorkflow = projectWorkflows.getFirst();

        return projectWorkflow.getWorkflowId();
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

        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflows.getFirst();

        return Objects.requireNonNull(mcpProjectWorkflow.getId(), "id");
    }
}
