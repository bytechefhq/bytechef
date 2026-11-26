/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.promotion.contributor;

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
import com.bytechef.ee.automation.apiplatform.promotion.config.ApiCollectionPromotionIntTestConfiguration;
import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
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
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ApiCollectionPromotionIntTestConfiguration.class, properties = "bytechef.webhook-url=/webhooks/{id}")
@Import(PostgreSQLContainerConfiguration.class)
@PromotionIntTestSharedMocks
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
class ApiCollectionPromotionContributorIntTest {

    private static final String EMPTY_DESTRUCTIVE_CHANGES_HASH =
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String COMPONENT_NAME = "slack";
    private static final int CONNECTION_VERSION = 1;
    private static final String CONNECTION_NAME = "slack-account";
    private static final String WORKFLOW_CONNECTION_KEY = "slack";
    private static final String WORKFLOW_NODE_NAME = "slack_1";

    @Autowired
    private ApiCollectionEndpointService apiCollectionEndpointService;

    @Autowired
    private ApiCollectionFacade apiCollectionFacade;

    @Autowired
    private EnvironmentPromotionContributor apiCollectionPromotionContributor;

    @Autowired
    private ApiCollectionService apiCollectionService;

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
            "TRUNCATE TABLE api_collection, category, connection, project, project_deployment, project_workflow, " +
                "tag, workflow, workspace CASCADE");
    }

    @Test
    void testApiCollectionFirstPromotionCreatesCounterpartAndRePromotionSyncsInPlace() {
        ApiCollectionDTO sourceApiCollection = createApiCollection("billing", "billing", 1);

        long sourceApiCollectionId = Objects.requireNonNull(sourceApiCollection.id(), "id");

        createApiCollectionEndpoint(sourceApiCollectionId, HttpMethod.GET, "/x");
        bindConnection(sourceApiCollection.projectDeploymentId(), developmentConnectionId);

        EnvironmentPromotionResult firstResult = apiCollectionPromotionContributor.promote(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

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

        EnvironmentPromotionResult secondResult = apiCollectionPromotionContributor.promote(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

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
    void testPromotionAuthorizerBeanReferenceResolvesAndGates() {
        separateApiCollectionIdsFromProjectIds();

        ApiCollectionDTO apiCollection = createApiCollection("guarded", "guarded", 1);

        long apiCollectionId = Objects.requireNonNull(apiCollection.id(), "id");

        assertThat(apiCollectionId).isNotEqualTo(projectId);

        permissionService.reset();

        apiCollectionPromotionContributor.preview(apiCollectionId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(permissionService.getResourceScopeChecks())
            .contains(new ResourceScopeCheck(projectId, "Project", "DEPLOYMENT_PUSH"));

        permissionService.setGranted(false);

        assertThatThrownBy(
            () -> apiCollectionPromotionContributor.preview(
                apiCollectionId, Environment.DEVELOPMENT, Environment.STAGING))
                    .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testApiCollectionBackingProjectDeploymentIsNotPromotableAsAProjectDeployment() {
        ApiCollectionDTO apiCollection = createApiCollection("synthetic", "synthetic", 1);

        long backingProjectDeploymentId = apiCollection.projectDeploymentId();

        assertThatThrownBy(
            () -> projectDeploymentPromotionContributor.promote(
                backingProjectDeploymentId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(EnvironmentPromotionErrorType.SYNTHETIC_DEPLOYMENT_NOT_PROMOTABLE.getErrorKey());
    }

    @Test
    void testPreviewDeletesEqualPromoteDeletes() {
        ApiCollectionDTO sourceApiCollection = createApiCollection("billing", "billing", 1);

        long sourceApiCollectionId = Objects.requireNonNull(sourceApiCollection.id(), "id");

        createApiCollectionEndpoint(sourceApiCollectionId, HttpMethod.GET, "/x");

        EnvironmentPromotionResult firstResult = apiCollectionPromotionContributor.promote(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        long targetId = firstResult.targetId();

        createApiCollectionEndpoint(targetId, HttpMethod.DELETE, "/extra");

        EnvironmentPromotionPreview preview = apiCollectionPromotionContributor.preview(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(preview.changes()).containsExactly(
            new PromotionChange(
                PromotionChange.Kind.DELETE, "ENDPOINT", "DELETE /extra", "billing", "endpoint/extra", true));
        assertThat(preview.destructiveChangesHash()).isNotEqualTo(EMPTY_DESTRUCTIVE_CHANGES_HASH);

        apiCollectionPromotionContributor.promote(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(),
            preview.destructiveChangesHash());

        assertThat(endpointKeys(targetId)).containsExactly("GET /x");
    }

    @Test
    void testRenamedEndpointIsUpdate() {
        ApiCollectionDTO sourceApiCollection = createApiCollection("billing", "billing", 1);

        long sourceApiCollectionId = Objects.requireNonNull(sourceApiCollection.id(), "id");

        createApiCollectionEndpoint(sourceApiCollectionId, HttpMethod.GET, "/x");

        apiCollectionPromotionContributor.promote(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        List<ApiCollectionEndpoint> sourceEndpoints = apiCollectionEndpointService.getApiEndpoints(
            sourceApiCollectionId);

        ApiCollectionEndpoint sourceEndpoint = sourceEndpoints.getFirst();

        apiCollectionFacade.updateApiCollectionEndpoint(
            new ApiCollectionEndpointDTO(
                sourceApiCollectionId, null, null, false, HttpMethod.GET, sourceEndpoint.getId(), null, null,
                "renamed", "/x", Objects.requireNonNull(sourceEndpoint.getProjectDeploymentWorkflowId(), "id"),
                sourceEndpoint.getVersion(), workflowUuid));

        EnvironmentPromotionPreview preview = apiCollectionPromotionContributor.preview(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(preview.changes()).containsExactly(
            new PromotionChange(PromotionChange.Kind.UPDATE, "ENDPOINT", "GET /x", "billing", "renamed", false));
        assertThat(preview.destructiveChangesHash()).isEqualTo(EMPTY_DESTRUCTIVE_CHANGES_HASH);
    }

    @Test
    void testNewTargetYieldsOnlyCreates() {
        ApiCollectionDTO sourceApiCollection = createApiCollection("billing", "billing", 1);

        long sourceApiCollectionId = Objects.requireNonNull(sourceApiCollection.id(), "id");

        createApiCollectionEndpoint(sourceApiCollectionId, HttpMethod.GET, "/x");

        EnvironmentPromotionPreview preview = apiCollectionPromotionContributor.preview(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(preview.changes()).isNotEmpty();
        assertThat(preview.changes()).allMatch(change -> change.kind() == PromotionChange.Kind.CREATE);
        assertThat(preview.changes()).contains(
            new PromotionChange(
                PromotionChange.Kind.CREATE, "API_COLLECTION", sourceApiCollection.uuid(), null, "billing", false));
        assertThat(preview.destructiveChangesHash()).isEqualTo(EMPTY_DESTRUCTIVE_CHANGES_HASH);
    }

    @Test
    void testUnchangedCollectionHasNoChanges() {
        ApiCollectionDTO sourceApiCollection = createApiCollection("billing", "billing", 1);

        long sourceApiCollectionId = Objects.requireNonNull(sourceApiCollection.id(), "id");

        createApiCollectionEndpoint(sourceApiCollectionId, HttpMethod.GET, "/x");

        apiCollectionPromotionContributor.promote(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        EnvironmentPromotionPreview preview = apiCollectionPromotionContributor.preview(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(preview.changes()).isEmpty();
        assertThat(preview.destructiveChangesHash()).isEqualTo(EMPTY_DESTRUCTIVE_CHANGES_HASH);
    }

    @Test
    void testCollectionOnOtherProjectVersionIsUpdate() {
        ApiCollectionDTO sourceApiCollection = createApiCollection("billing", "billing", 1);

        long sourceApiCollectionId = Objects.requireNonNull(sourceApiCollection.id(), "id");

        createApiCollectionEndpoint(sourceApiCollectionId, HttpMethod.GET, "/x");

        apiCollectionPromotionContributor.promote(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        projectFacade.publishProject(projectId, "v2", false);
        changeProjectVersion(sourceApiCollection.projectDeploymentId(), 2);

        EnvironmentPromotionPreview preview = apiCollectionPromotionContributor.preview(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING);

        assertThat(preview.changes())
            .filteredOn(change -> Objects.equals(change.assetType(), "API_COLLECTION"))
            .containsExactly(
                new PromotionChange(
                    PromotionChange.Kind.UPDATE, "API_COLLECTION", sourceApiCollection.uuid(), null, "billing",
                    false));
        assertThat(preview.destructiveChangesHash()).isEqualTo(EMPTY_DESTRUCTIVE_CHANGES_HASH);
    }

    @Test
    void testPromoteWithStaleHashWritesNothing() {
        ApiCollectionDTO sourceApiCollection = createApiCollection("billing", "billing", 1);

        long sourceApiCollectionId = Objects.requireNonNull(sourceApiCollection.id(), "id");

        createApiCollectionEndpoint(sourceApiCollectionId, HttpMethod.GET, "/x");

        EnvironmentPromotionResult firstResult = apiCollectionPromotionContributor.promote(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null);

        long targetId = firstResult.targetId();

        EnvironmentPromotionPreview preview = apiCollectionPromotionContributor.preview(
            sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING);

        createApiCollectionEndpoint(targetId, HttpMethod.DELETE, "/extra");

        assertThatThrownBy(
            () -> apiCollectionPromotionContributor.promote(
                sourceApiCollectionId, Environment.DEVELOPMENT, Environment.STAGING, Map.of(),
                preview.destructiveChangesHash()))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(EnvironmentPromotionErrorType.PROMOTION_PREVIEW_STALE.getErrorKey());

        assertThat(endpointKeys(targetId)).containsExactlyInAnyOrder("GET /x", "DELETE /extra");
    }

    private List<String> endpointKeys(long apiCollectionId) {
        List<String> keys = new ArrayList<>();

        for (ApiCollectionEndpoint endpoint : apiCollectionEndpointService.getApiEndpoints(apiCollectionId)) {
            keys.add(endpoint.getHttpMethod() + " " + endpoint.getPath());
        }

        return keys;
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

    private String projectWorkflowUuid(int projectVersion) {
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(projectId, projectVersion);

        ProjectWorkflow projectWorkflow = projectWorkflows.getFirst();

        return projectWorkflow.getUuidAsString();
    }
}
