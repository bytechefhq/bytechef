/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.contributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.exception.ProjectDeploymentErrorType;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.automation.configuration.promotion.config.ApiCollectionClaimedProjectDeploymentIds;
import com.bytechef.ee.automation.configuration.promotion.config.ProjectDeploymentPromotionIntTestConfiguration;
import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectDeploymentPromotionIntTestConfiguration.class, properties = "bytechef.webhook-url=/webhooks/{id}")
@Import(PostgreSQLContainerConfiguration.class)
@PromotionIntTestSharedMocks
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
class ProjectDeploymentPromotionContributorIntTest {

    private static final String COMPONENT_NAME = "slack";
    private static final String EMPTY_DESTRUCTIVE_CHANGES_HASH =
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String EXTRA_WORKFLOW_LABEL = "Extra Workflow";
    private static final int CONNECTION_VERSION = 1;
    private static final String CONNECTION_NAME = "slack-account";
    private static final String WORKFLOW_CONNECTION_KEY = "slack";
    private static final String WORKFLOW_NODE_NAME = "slack_1";

    @Autowired
    private ApiCollectionClaimedProjectDeploymentIds apiCollectionClaimedProjectDeploymentIds;

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
    private EnvironmentPromotionContributor projectDeploymentPromotionContributor;

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
            "TRUNCATE TABLE category, connection, project, project_deployment, project_workflow, tag, workflow, " +
                "workspace CASCADE");

        apiCollectionClaimedProjectDeploymentIds.clear();
    }

    @Test
    void testProjectDeploymentPromotionCreatesDisabledCounterpartAndPreservesTargetOnRePromotion() {
        ProjectDeployment sourceProjectDeployment = createProjectDeployment("billing-deployment");

        long sourceProjectDeploymentId = Objects.requireNonNull(sourceProjectDeployment.getId(), "id");

        bindConnection(sourceProjectDeploymentId, developmentConnectionId);

        EnvironmentPromotionResult firstResult = projectDeploymentPromotionContributor.promote(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

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

        ProjectDeploymentWorkflow targetProjectDeploymentWorkflow = targetProjectDeploymentWorkflows.getFirst();

        Long stableProjectDeploymentWorkflowId = targetProjectDeploymentWorkflow.getId();

        assertThat(connectionIds(targetProjectDeploymentWorkflows.getFirst())).containsExactly(stagingConnectionId);

        targetProjectDeployment.setEnabled(true);
        targetProjectDeployment.setName("staging-billing-deployment");

        projectDeploymentService.update(targetProjectDeployment);

        projectFacade.publishProject(projectId, "v2", false);
        changeProjectVersion(sourceProjectDeploymentId, 2);

        EnvironmentPromotionResult secondResult = projectDeploymentPromotionContributor.promote(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

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

        ProjectDeploymentWorkflow rePromotedProjectDeploymentWorkflow = rePromotedProjectDeploymentWorkflows.getFirst();

        assertThat(rePromotedProjectDeploymentWorkflow.getId()).isEqualTo(stableProjectDeploymentWorkflowId);
        assertThat(connectionIds(rePromotedProjectDeploymentWorkflow)).containsExactly(stagingConnectionId);
    }

    @Test
    void testSyntheticProjectDeploymentIsNotPromotable() {
        ProjectDeployment syntheticProjectDeployment = createProjectDeployment("synthetic-deployment");

        long syntheticProjectDeploymentId = Objects.requireNonNull(syntheticProjectDeployment.getId(), "id");

        apiCollectionClaimedProjectDeploymentIds.claim(syntheticProjectDeploymentId);

        assertThatThrownBy(
            () -> projectDeploymentPromotionContributor.promote(
                syntheticProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(EnvironmentPromotionErrorType.SYNTHETIC_DEPLOYMENT_NOT_PROMOTABLE.getErrorKey());
    }

    @Test
    void testPreviewDeletesEqualPromoteDeletes() {
        long sourceProjectDeploymentId = createProjectDeploymentWithExtraProjectWorkflow("billing-deployment");

        EnvironmentPromotionResult firstResult = projectDeploymentPromotionContributor.promote(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        long targetProjectDeploymentId = firstResult.targetId();

        addExtraWorkflowToTarget(targetProjectDeploymentId);

        EnvironmentPromotionPreview preview = projectDeploymentPromotionContributor.preview(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING);

        Set<String> previewDeleteKeys = new HashSet<>();

        for (PromotionChange change : preview.changes()) {
            if (change.kind() == PromotionChange.Kind.DELETE) {
                assertThat(change.destructive()).isTrue();
                assertThat(change.assetType()).isEqualTo("DEPLOYMENT_WORKFLOW");
                assertThat(change.name()).isEqualTo(EXTRA_WORKFLOW_LABEL);

                previewDeleteKeys.add(change.key());
            }
        }

        Set<String> uuidsBeforePromote = deploymentWorkflowUuids(targetProjectDeploymentId);

        projectDeploymentPromotionContributor.promote(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        Set<String> removedUuids = new HashSet<>(uuidsBeforePromote);

        removedUuids.removeAll(deploymentWorkflowUuids(targetProjectDeploymentId));

        assertThat(previewDeleteKeys).hasSize(1);
        assertThat(previewDeleteKeys).isEqualTo(removedUuids);
        assertThat(preview.destructiveChangesHash()).isNotEqualTo(EMPTY_DESTRUCTIVE_CHANGES_HASH);
    }

    @Test
    void testPreviewOfUnchangedTargetHasNoChanges() {
        ProjectDeployment sourceProjectDeployment = createProjectDeployment("billing-deployment");

        long sourceProjectDeploymentId = Objects.requireNonNull(sourceProjectDeployment.getId(), "id");

        projectDeploymentPromotionContributor.promote(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        EnvironmentPromotionPreview preview = projectDeploymentPromotionContributor.preview(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(preview.changes()).isEmpty();
        assertThat(preview.destructiveChangesHash()).isEqualTo(EMPTY_DESTRUCTIVE_CHANGES_HASH);
    }

    @Test
    void testPreviewOfTargetOnOtherProjectVersionHasProjectDeploymentUpdate() {
        ProjectDeployment sourceProjectDeployment = createProjectDeployment("billing-deployment");

        long sourceProjectDeploymentId = Objects.requireNonNull(sourceProjectDeployment.getId(), "id");

        EnvironmentPromotionResult firstResult = projectDeploymentPromotionContributor.promote(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        projectFacade.publishProject(projectId, "v2", false);
        changeProjectVersion(sourceProjectDeploymentId, 2);

        EnvironmentPromotionPreview preview = projectDeploymentPromotionContributor.preview(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING);

        ProjectDeployment targetProjectDeployment =
            projectDeploymentService.getProjectDeployment(firstResult.targetId());
        Project project = projectService.getProject(projectId);

        assertThat(preview.changes())
            .filteredOn(change -> Objects.equals(change.assetType(), "PROJECT_DEPLOYMENT"))
            .containsExactly(
                new PromotionChange(
                    PromotionChange.Kind.UPDATE, "PROJECT_DEPLOYMENT",
                    String.valueOf(targetProjectDeployment.getUuid()), null, project.getName(), false));
        assertThat(preview.destructiveChangesHash()).isEqualTo(EMPTY_DESTRUCTIVE_CHANGES_HASH);
    }

    @Test
    void testPromoteWithStaleHashWritesNothing() {
        long sourceProjectDeploymentId = createProjectDeploymentWithExtraProjectWorkflow("billing-deployment");

        EnvironmentPromotionResult firstResult = projectDeploymentPromotionContributor.promote(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        long targetProjectDeploymentId = firstResult.targetId();

        EnvironmentPromotionPreview preview = projectDeploymentPromotionContributor.preview(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING);

        addExtraWorkflowToTarget(targetProjectDeploymentId);

        Map<Long, Boolean> enabledFlagsBeforePromote = enabledFlagsById(targetProjectDeploymentId);

        assertThatThrownBy(
            () -> projectDeploymentPromotionContributor.promote(
                sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(),
                preview.destructiveChangesHash()))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(EnvironmentPromotionErrorType.PROMOTION_PREVIEW_STALE.getErrorKey());

        assertThat(enabledFlagsById(targetProjectDeploymentId)).isEqualTo(enabledFlagsBeforePromote);
        assertThat(enabledFlagsBeforePromote).hasSize(2);
    }

    @Test
    void testPromoteWithNullHashPromotes() {
        long sourceProjectDeploymentId = createProjectDeploymentWithExtraProjectWorkflow("billing-deployment");

        EnvironmentPromotionResult firstResult = projectDeploymentPromotionContributor.promote(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        long targetProjectDeploymentId = firstResult.targetId();

        addExtraWorkflowToTarget(targetProjectDeploymentId);

        EnvironmentPromotionResult secondResult = projectDeploymentPromotionContributor.promote(
            sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        assertThat(secondResult.targetId()).isEqualTo(targetProjectDeploymentId);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(targetProjectDeploymentId))
            .hasSize(1);
    }

    @Test
    void testPreviewRunsValidatePromotable() {
        ProjectDeployment sourceProjectDeployment = createProjectDeployment("billing-deployment");

        long sourceProjectDeploymentId = Objects.requireNonNull(sourceProjectDeployment.getId(), "id");

        Project project = projectService.getProject(projectId);

        jdbcTemplate.update(
            "UPDATE project_deployment SET project_version = ? WHERE id = ?", project.getLastProjectVersion(),
            sourceProjectDeploymentId);

        assertThatThrownBy(
            () -> projectDeploymentPromotionContributor.preview(
                sourceProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(ProjectDeploymentErrorType.INVALID_PROJECT_VERSION.getErrorKey());
    }

    private void addExtraWorkflowToTarget(long targetProjectDeploymentId) {
        ProjectDeployment targetProjectDeployment =
            projectDeploymentService.getProjectDeployment(targetProjectDeploymentId);

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = new ArrayList<>();

        for (ProjectDeploymentWorkflow existingProjectDeploymentWorkflow : projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(targetProjectDeploymentId)) {

            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setConnections(existingProjectDeploymentWorkflow.getConnections());
            projectDeploymentWorkflow.setEnabled(existingProjectDeploymentWorkflow.isEnabled());
            projectDeploymentWorkflow.setInputs(existingProjectDeploymentWorkflow.getInputs());
            projectDeploymentWorkflow.setProjectDeploymentId(targetProjectDeploymentId);
            projectDeploymentWorkflow.setWorkflowId(existingProjectDeploymentWorkflow.getWorkflowId());

            projectDeploymentWorkflows.add(projectDeploymentWorkflow);
        }

        ProjectDeploymentWorkflow extraProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        extraProjectDeploymentWorkflow.setProjectDeploymentId(targetProjectDeploymentId);
        extraProjectDeploymentWorkflow.setWorkflowId(
            extraProjectWorkflowId(targetProjectDeployment.getProjectVersion()));

        projectDeploymentWorkflows.add(extraProjectDeploymentWorkflow);

        projectDeploymentFacade.updateProjectDeployment(targetProjectDeployment, projectDeploymentWorkflows, List.of());

        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(targetProjectDeploymentId))
            .hasSize(2);
    }

    private long createProjectDeploymentWithExtraProjectWorkflow(String name) {
        String firstWorkflowUuid = projectWorkflowUuid(projectWorkflowId(1));

        projectWorkflowFacade.addWorkflow(
            projectId,
            "{\"label\": \"" + EXTRA_WORKFLOW_LABEL + "\", \"description\": \"Extra\", \"triggers\": " +
                "[{\"name\": \"trigger_1\", \"type\": \"workflow/v1/newWorkflowCall\"}], \"tasks\": []}");

        projectFacade.publishProject(projectId, "v2", false);

        String firstWorkflowId = null;

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(projectId, 2)) {
            if (Objects.equals(projectWorkflow.getUuidAsString(), firstWorkflowUuid)) {
                firstWorkflowId = projectWorkflow.getWorkflowId();
            }
        }

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(2);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setWorkflowId(Objects.requireNonNull(firstWorkflowId, "firstWorkflowId"));

        return projectDeploymentFacade.createProjectDeployment(
            projectDeployment, List.of(projectDeploymentWorkflow), List.of());
    }

    private Set<String> deploymentWorkflowUuids(long projectDeploymentId) {
        Set<String> uuids = new HashSet<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(projectDeploymentId)) {

            uuids.add(projectWorkflowUuid(projectDeploymentWorkflow.getWorkflowId()));
        }

        return uuids;
    }

    private Map<Long, Boolean> enabledFlagsById(long projectDeploymentId) {
        Map<Long, Boolean> enabledFlagsById = new HashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(projectDeploymentId)) {

            enabledFlagsById.put(projectDeploymentWorkflow.getId(), projectDeploymentWorkflow.isEnabled());
        }

        return enabledFlagsById;
    }

    private String extraProjectWorkflowId(int projectVersion) {
        String firstWorkflowUuid = projectWorkflowUuid(projectWorkflowId(1));

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(projectId, projectVersion)) {
            if (!Objects.equals(projectWorkflow.getUuidAsString(), firstWorkflowUuid)) {
                return projectWorkflow.getWorkflowId();
            }
        }

        throw new IllegalStateException("Extra workflow not found in version " + projectVersion);
    }

    private String projectWorkflowUuid(String workflowId) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        return projectWorkflow.getUuidAsString();
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

    private long createWorkspace() {
        Long createdWorkspaceId = jdbcTemplate.queryForObject(
            "INSERT INTO workspace (name, created_by, created_date, last_modified_by, last_modified_date, version) " +
                "VALUES ('test-workspace', 'test', now(), 'test', now(), 1) RETURNING id",
            Long.class);

        return Objects.requireNonNull(createdWorkspaceId, "id");
    }

    private String projectWorkflowId(int projectVersion) {
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(projectId, projectVersion);

        ProjectWorkflow projectWorkflow = projectWorkflows.getFirst();

        return projectWorkflow.getWorkflowId();
    }
}
