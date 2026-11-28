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

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.facade.A2aProjectFacade;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
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
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
    classes = AiServerPromotionIntTestConfiguration.class, properties = "bytechef.webhook-url=/webhooks/{id}")
@Import(PostgreSQLContainerConfiguration.class)
@PromotionIntTestSharedMocks
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
class A2aServerPromotionContributorIntTest {

    private static final String COMPONENT_NAME = "slack";
    private static final int CONNECTION_VERSION = 1;
    private static final String EMPTY_DESTRUCTIVE_CHANGES_HASH =
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String CONNECTION_NAME = "slack-account";
    private static final String WORKFLOW_CONNECTION_KEY = "slack";
    private static final String WORKFLOW_NODE_NAME = "slack_1";

    @Autowired
    private A2aProjectFacade a2aProjectFacade;

    @Autowired
    private A2aProjectWorkflowService a2aProjectWorkflowService;

    @Autowired
    private EnvironmentPromotionContributor a2aServerPromotionContributor;

    @Autowired
    private A2aServerService a2aServerService;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        projectId = createPublishedProject();

        developmentConnectionId = createConnection(Environment.DEVELOPMENT);
        stagingConnectionId = createConnection(Environment.STAGING);
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute(
            "TRUNCATE TABLE a2a_server, category, connection, project, project_deployment, project_workflow, " +
                "tag, workflow, workspace CASCADE");
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

        A2aProjectWorkflow sourceA2aProjectWorkflow = singleA2aProjectWorkflow(sourceA2aProject.getId());

        long sourceA2aProjectWorkflowId = Objects.requireNonNull(sourceA2aProjectWorkflow.getId(), "id");

        a2aProjectWorkflowService.updateParameters(
            sourceA2aProjectWorkflowId, Map.of("skillName", "invoice", "skillDescription", "Creates an invoice"));

        EnvironmentPromotionResult firstResult = a2aServerPromotionContributor.promote(
            sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

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

        EnvironmentPromotionResult secondResult = a2aServerPromotionContributor.promote(
            sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

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

        ProjectDeployment stableProjectDeployment = projectDeploymentOfWorkflow(stableProjectDeploymentWorkflowId);

        assertThat(stableProjectDeployment.getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testA2aServerPromotionIsDeniedToANonAdmin() {
        A2aServer sourceA2aServer = createA2aServer("guarded-a2a", null);

        long sourceA2aServerId = Objects.requireNonNull(sourceA2aServer.getId(), "id");

        runAs(
            "user@localhost.com", AuthorityConstants.USER,
            () -> assertThatThrownBy(
                () -> a2aServerPromotionContributor.promote(
                    sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null))
                        .isInstanceOf(AccessDeniedException.class));

        assertThat(a2aServerService.fetchA2aServer(sourceA2aServer.getUuid(), Environment.STAGING)).isEmpty();
    }

    @Test
    void testPreviewDeletesEqualPromoteDeletes() {
        long twoWorkflowProjectId = createPublishedProject("Two Workflow Project", 2);
        long extraProjectId = createPublishedProject("Extra Project", 1);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(twoWorkflowProjectId, 1);

        ProjectWorkflow keptProjectWorkflow = projectWorkflows.get(0);
        ProjectWorkflow extraProjectWorkflow = projectWorkflows.get(1);

        A2aServer sourceA2aServer = createA2aServer("deletes-a2a", null);

        long sourceA2aServerId = Objects.requireNonNull(sourceA2aServer.getId(), "id");

        a2aProjectFacade.createA2aProject(
            sourceA2aServerId, twoWorkflowProjectId, 1, List.of(keptProjectWorkflow.getWorkflowId()));

        EnvironmentPromotionResult firstResult = a2aServerPromotionContributor.promote(
            sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        long targetId = firstResult.targetId();
        long keptTargetA2aProjectId = singleA2aProjectId(targetId);

        a2aProjectFacade.updateA2aProject(
            keptTargetA2aProjectId,
            List.of(keptProjectWorkflow.getWorkflowId(), extraProjectWorkflow.getWorkflowId()));

        A2aProject extraTargetA2aProject = a2aProjectFacade.createA2aProject(
            targetId, extraProjectId, 1, List.of(projectWorkflowId(extraProjectId, 1)));

        ProjectDeployment extraTargetProjectDeployment = projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(extraTargetA2aProject.getProjectDeploymentId(), "projectDeploymentId"));

        EnvironmentPromotionPreview preview = a2aServerPromotionContributor.preview(
            sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING);

        ProjectDeployment keptTargetProjectDeployment = a2aProjectDeployment(keptTargetA2aProjectId);

        String extraWorkflowKey =
            keptTargetProjectDeployment.getUuid() + ":" + extraProjectWorkflow.getUuidAsString();

        assertThat(destructiveChangeLines(preview)).containsExactlyInAnyOrder(
            "A2A_PROJECT||" + extraProjectId + ":" + extraTargetProjectDeployment.getUuid(),
            "DEPLOYMENT_WORKFLOW|Two Workflow Project|" + extraWorkflowKey,
            "EXPOSED_WORKFLOW|Two Workflow Project|" + extraWorkflowKey);

        a2aServerPromotionContributor.promote(
            sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(),
            preview.destructiveChangesHash());

        assertThat(singleA2aProjectId(targetId)).isEqualTo(keptTargetA2aProjectId);

        ProjectDeployment promotedTargetProjectDeployment = a2aProjectDeployment(keptTargetA2aProjectId);

        assertThat(
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
                Objects.requireNonNull(promotedTargetProjectDeployment.getId(), "id")))
                    .hasSize(1);

        EnvironmentPromotionPreview secondPreview = a2aServerPromotionContributor.preview(
            sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(destructiveChangeLines(secondPreview)).isEmpty();
        assertThat(secondPreview.destructiveChangesHash()).isEqualTo(EMPTY_DESTRUCTIVE_CHANGES_HASH);
    }

    @Test
    void testUnchangedServerHasNoChanges() {
        A2aServer sourceA2aServer = createA2aServer("unchanged-a2a", null);

        long sourceA2aServerId = Objects.requireNonNull(sourceA2aServer.getId(), "id");

        a2aProjectFacade.createA2aProject(sourceA2aServerId, projectId, 1, List.of(projectWorkflowId(1)));

        EnvironmentPromotionPreview freshPreview = a2aServerPromotionContributor.preview(
            sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(freshPreview.changes()).extracting(PromotionChange::kind)
            .containsOnly(Kind.CREATE);
        assertThat(changesOfType(freshPreview, "A2A_PROJECT")).hasSize(1);

        a2aServerPromotionContributor.promote(
            sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        EnvironmentPromotionPreview preview = a2aServerPromotionContributor.preview(
            sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(preview.changes()).isEmpty();
        assertThat(preview.destructiveChangesHash()).isEqualTo(EMPTY_DESTRUCTIVE_CHANGES_HASH);
    }

    @Test
    void testPromoteWithStaleHashWritesNothing() {
        long extraProjectId = createPublishedProject("Extra Project", 1);

        A2aServer sourceA2aServer = createA2aServer("stale-a2a", null);

        long sourceA2aServerId = Objects.requireNonNull(sourceA2aServer.getId(), "id");

        A2aProject sourceA2aProject = a2aProjectFacade.createA2aProject(
            sourceA2aServerId, projectId, 1, List.of(projectWorkflowId(1)));

        A2aProjectWorkflow sourceA2aProjectWorkflow = singleA2aProjectWorkflow(sourceA2aProject.getId());

        long sourceA2aProjectWorkflowId = Objects.requireNonNull(sourceA2aProjectWorkflow.getId(), "id");

        a2aProjectWorkflowService.updateParameters(sourceA2aProjectWorkflowId, Map.of("skillName", "invoice"));

        EnvironmentPromotionResult result = a2aServerPromotionContributor.promote(
            sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        long targetId = result.targetId();

        EnvironmentPromotionPreview preview = a2aServerPromotionContributor.preview(
            sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING);

        a2aProjectFacade.createA2aProject(targetId, extraProjectId, 1, List.of(projectWorkflowId(extraProjectId, 1)));
        a2aProjectWorkflowService.updateParameters(sourceA2aProjectWorkflowId, Map.of("skillName", "renamed"));

        assertThatThrownBy(
            () -> a2aServerPromotionContributor.promote(
                sourceA2aServerId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(),
                preview.destructiveChangesHash()))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(EnvironmentPromotionErrorType.PROMOTION_PREVIEW_STALE.getErrorKey());

        assertThat(a2aProjectCount(targetId)).isEqualTo(2);

        A2aProjectWorkflow targetA2aProjectWorkflow = singleA2aProjectWorkflow(
            projectA2aProjectId(targetId, projectId));

        assertThat(a2aProjectWorkflowParameters(targetA2aProjectWorkflow)).containsEntry("skillName", "invoice");
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

    private ProjectDeployment a2aProjectDeployment(long a2aProjectId) {
        Long projectDeploymentId = jdbcTemplate.queryForObject(
            "SELECT project_deployment_id FROM a2a_project WHERE id = ?", Long.class, a2aProjectId);

        return projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(projectDeploymentId, "projectDeploymentId"));
    }

    private int a2aProjectCount(long a2aServerId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM a2a_project WHERE a2a_server_id = ?", Integer.class, a2aServerId);

        return Objects.requireNonNull(count, "count");
    }

    private long projectA2aProjectId(long a2aServerId, long sourceProjectId) {
        Long a2aProjectId = jdbcTemplate.queryForObject(
            "SELECT a.id FROM a2a_project a JOIN project_deployment d ON d.id = a.project_deployment_id " +
                "WHERE a.a2a_server_id = ? AND d.project_id = ?",
            Long.class, a2aServerId, sourceProjectId);

        return Objects.requireNonNull(a2aProjectId, "id");
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

    private String projectWorkflowId(long workflowProjectId, int projectVersion) {
        List<ProjectWorkflow> projectWorkflows =
            projectWorkflowService.getProjectWorkflows(workflowProjectId, projectVersion);

        ProjectWorkflow projectWorkflow = projectWorkflows.getFirst();

        return projectWorkflow.getWorkflowId();
    }

    private long createWorkspace() {
        Long createdWorkspaceId = jdbcTemplate.queryForObject(
            "INSERT INTO workspace (name, created_by, created_date, last_modified_by, last_modified_date, version) " +
                "VALUES ('test-workspace', 'test', now(), 'test', now(), 1) RETURNING id",
            Long.class);

        return Objects.requireNonNull(createdWorkspaceId, "id");
    }

    private ProjectDeployment projectDeploymentOfWorkflow(long projectDeploymentWorkflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflowId);

        return projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(projectDeploymentWorkflow.getProjectDeploymentId(), "projectDeploymentId"));
    }

    private String projectWorkflowId(int projectVersion) {
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(projectId, projectVersion);

        ProjectWorkflow projectWorkflow = projectWorkflows.getFirst();

        return projectWorkflow.getWorkflowId();
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
}
