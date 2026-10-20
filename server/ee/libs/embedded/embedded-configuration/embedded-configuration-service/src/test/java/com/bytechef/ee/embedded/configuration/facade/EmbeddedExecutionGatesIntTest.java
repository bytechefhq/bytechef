/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowCreator;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.security.scope.ApiKeyPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ApiPlatformPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ConnectionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DataTablePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DeploymentPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ExecutionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.KnowledgeBasePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.McpPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ProjectPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkflowPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkspacePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.AppEventPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.ConnectedUserPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.EmbeddedPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.EmbeddedSharedPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.IntegrationInstanceConfigurationPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.IntegrationPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.CustomRoleScopeResolverImpl;
import com.bytechef.ee.automation.configuration.service.EmbeddedScopeCacheService;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.ee.embedded.workflow.execution.facade.IntegrationWorkflowExecutionFacade;
import com.bytechef.ee.embedded.workflow.execution.facade.IntegrationWorkflowExecutionFacadeImpl;
import com.bytechef.ee.embedded.workflow.execution.security.EmbeddedJobEnvironmentResolver;
import com.bytechef.ee.embedded.workflow.execution.security.EmbeddedJobOwnershipResolver;
import com.bytechef.ee.embedded.workflow.execution.security.EmbeddedPrincipalEnvironmentResolver;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacadeImpl;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class, EmbeddedExecutionGatesIntTest.ConnectedUserServiceConfiguration.class,
        EmbeddedExecutionGatesIntTest.RealPermissionConfiguration.class
    },
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true", "spring.liquibase.contexts=configuration,user"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, ClusterElementDefinitionService.class,
    ComponentConnectionFacade.class, ComponentDefinitionFacade.class, ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionService.class, ContextService.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class, JobFacade.class, JobService.class,
    McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectDeploymentFacade.class,
    ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class, ProjectFacade.class, ProjectService.class,
    ProjectWorkflowCreator.class, ProjectWorkflowFacade.class, ProjectWorkflowService.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class, TaskFileStorage.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerFileStorage.class, TriggerLifecycleFacade.class, WebhookTriggerTestFacade.class,
    WorkflowCacheManager.class, WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class,
    WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class
})
class EmbeddedExecutionGatesIntTest {

    private static final String DEFINITION = """
        {"label":"Sync contacts","inputs":[],"triggers":[],"tasks":[]}
        """;
    private static final String EDITOR = "embedded-editor";
    private static final String MIXED = "editor-in-production-viewer-in-development";
    private static final String NON_MEMBER = "non-member";
    private static final String PRODUCTION_VIEWER = "production-viewer";
    private static final String TENANT_ADMIN = "tenant-admin";
    private static final String VIEWER = "embedded-viewer";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        TENANT_ADMIN, 9700L, VIEWER, 9701L, EDITOR, 9702L, PRODUCTION_VIEWER, 9703L, NON_MEMBER, 9704L, MIXED, 9705L);

    private static final long DEVELOPMENT_JOB_ID = 7002L;
    private static final long PRODUCTION_JOB_ID = 7001L;
    private static final long PRODUCTION_TASK_EXECUTION_ID = 8001L;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectionDefinitionService connectionDefinitionService;

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceRepository integrationInstanceRepository;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private JobService jobService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private TriggerExecutionService triggerExecutionService;

    @Autowired
    private TriggerFileStorage triggerFileStorage;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkflowService workflowService;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private WorkflowValidatorFacade workflowValidatorFacade;

    private ConnectedUser developmentConnectedUser;
    private long developmentConfigurationId;
    private long developmentInstanceId;
    private ConnectedUser productionConnectedUser;
    private long productionConfigurationId;
    private long productionInstanceId;
    private String workflowId;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getEnvironment(anyLong()))
            .thenAnswer(invocation -> Environment.values()[((Long) invocation.getArgument(0)).intValue()]);
        when(componentDefinitionService.hasComponentDefinition(anyString(), any()))
            .thenReturn(true);
        when(componentDefinitionService.getComponentDefinition(anyString(), any()))
            .thenReturn(new ComponentDefinition("componentName"));
        when(connectionDefinitionService.getConnectionDefinition(anyString(), any()))
            .thenReturn(new ConnectionDefinition(ComponentDsl.connection(), "componentName", null, null));

        embeddedUserRepository.save(EmbeddedUser.forRole(userId(VIEWER), WorkspaceRole.VIEWER));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId(EDITOR), WorkspaceRole.EDITOR));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(PRODUCTION_VIEWER), WorkspaceRole.VIEWER, Environment.PRODUCTION));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId(MIXED), WorkspaceRole.EDITOR, Environment.PRODUCTION));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId(MIXED), WorkspaceRole.VIEWER, Environment.DEVELOPMENT));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId(MIXED), WorkspaceRole.VIEWER, Environment.STAGING));

        permissionService.evictAllEmbeddedScopeCache();

        String suffix = UUID.randomUUID()
            .toString();

        productionConnectedUser =
            connectedUserService.createConnectedUser("production-" + suffix, Environment.PRODUCTION);
        developmentConnectedUser = connectedUserService.createConnectedUser(
            "development-" + suffix, Environment.DEVELOPMENT);

        Integration integration = createPublishedIntegration();

        Workflow workflow = workflowService.create(DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        workflowId = workflow.getId();

        integrationWorkflowService.addWorkflow(integration.getId(), 1, workflowId);

        productionConfigurationId = createConfiguration(integration.getId(), Environment.PRODUCTION);
        developmentConfigurationId = createConfiguration(integration.getId(), Environment.DEVELOPMENT);

        IntegrationInstance productionInstance = integrationInstanceService.create(
            productionConnectedUser.getId(), 41L, productionConfigurationId);
        IntegrationInstance developmentInstance = integrationInstanceService.create(
            developmentConnectedUser.getId(), 42L, developmentConfigurationId);

        productionInstanceId = productionInstance.getId();
        developmentInstanceId = developmentInstance.getId();

        stubRuns();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            embeddedUserRepository.deleteByUserId(userId);
        }

        permissionService.evictAllEmbeddedScopeCache();

        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();

        connectedUserService.deleteConnectedUser(productionConnectedUser.getId());
        connectedUserService.deleteConnectedUser(developmentConnectedUser.getId());

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }
    }

    @Test
    void testAViewerSeesTheRunButNotItsInputsOutputsOrTrigger() {
        authenticate(VIEWER, false);

        for (long jobId : List.of(PRODUCTION_JOB_ID, DEVELOPMENT_JOB_ID)) {
            WorkflowExecutionDTO workflowExecutionDTO = integrationWorkflowExecutionFacade.getWorkflowExecution(jobId);

            assertThat(workflowExecutionDTO.id()).isEqualTo(jobId);
            assertThat(workflowExecutionDTO.job()
                .inputs()).isEmpty();
            assertThat(workflowExecutionDTO.triggerExecution()).isNotNull();
            assertThat(workflowExecutionDTO.triggerExecution()
                .input()).isNull();
            assertThat(workflowExecutionDTO.triggerExecution()
                .output()).isNull();
            assertThat(workflowExecutionDTO.triggerExecution()
                .state()).isNull();
        }

        TaskExecutionDTO taskExecutionDTO = integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecution(
            PRODUCTION_JOB_ID, PRODUCTION_TASK_EXECUTION_ID);

        assertThat(taskExecutionDTO.input()).isNull();
        assertThat(taskExecutionDTO.output()).isNull();
        assertThat(taskExecutionDTO.error()).isNull();

        verifyNoInteractions(taskFileStorage);

        Page<WorkflowExecutionDTO> page = integrationWorkflowExecutionFacade.getWorkflowExecutions(
            null, null, null, null, null, null, null, null, 0);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allSatisfy(
            row -> assertThat(row.job()
                .inputs()).isEmpty());
    }

    @Test
    void testAnEditorSeesRunDetails() {
        authenticate(EDITOR, false);

        WorkflowExecutionDTO workflowExecutionDTO = integrationWorkflowExecutionFacade.getWorkflowExecution(
            PRODUCTION_JOB_ID);

        assertThat(workflowExecutionDTO.job()
            .inputs()).isEqualTo(Map.of("jobInput", "value"));
        assertThat(workflowExecutionDTO.triggerExecution()
            .input()).isEqualTo(Map.of("evaluated", true));
        assertThat(workflowExecutionDTO.triggerExecution()
            .output()).isEqualTo("trigger-output");

        TaskExecutionDTO taskExecutionDTO = integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecution(
            PRODUCTION_JOB_ID, PRODUCTION_TASK_EXECUTION_ID);

        assertThat(taskExecutionDTO.output()).isEqualTo("task-output");

        Page<WorkflowExecutionDTO> page = integrationWorkflowExecutionFacade.getWorkflowExecutions(
            null, null, null, null, null, null, null, null, 0);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allSatisfy(
            row -> assertThat(row.job()
                .inputs()).isEqualTo(Map.of("jobInput", "value")));
    }

    @Test
    void testRunListsRedactPerConfigurationEnvironment() {
        authenticate(MIXED, false);

        Page<WorkflowExecutionDTO> page = integrationWorkflowExecutionFacade.getWorkflowExecutions(
            null, null, null, null, null, null, null, null, 0);

        assertThat(page.getContent()).hasSize(2);

        for (WorkflowExecutionDTO workflowExecutionDTO : page.getContent()) {
            if (workflowExecutionDTO.id() == PRODUCTION_JOB_ID) {
                assertThat(workflowExecutionDTO.job()
                    .inputs()).isEqualTo(Map.of("jobInput", "value"));
            } else {
                assertThat(workflowExecutionDTO.job()
                    .inputs()).isEmpty();
            }
        }
    }

    @Test
    void testAProductionOnlyViewerSeesOnlyProductionRuns() {
        authenticate(PRODUCTION_VIEWER, false);

        Page<WorkflowExecutionDTO> page = integrationWorkflowExecutionFacade.getWorkflowExecutions(
            (long) Environment.PRODUCTION.ordinal(), null, null, null, null, null, null, null, 0);

        assertThat(page.getContent())
            .extracting(WorkflowExecutionDTO::id)
            .containsExactly(PRODUCTION_JOB_ID);
        assertThat(page.getContent()
            .getFirst()
            .job()
            .inputs()).isEmpty();

        Page<WorkflowExecutionDTO> developmentConfigurationPage =
            integrationWorkflowExecutionFacade.getWorkflowExecutions(
                (long) Environment.PRODUCTION.ordinal(), null, null, null, null, developmentConfigurationId, null, null,
                0);

        assertThat(developmentConfigurationPage.getContent()).isEmpty();

        assertThat(integrationWorkflowExecutionFacade.getWorkflowExecution(PRODUCTION_JOB_ID)).isNotNull();

        assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecution(DEVELOPMENT_JOB_ID))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationWorkflowExecutionFacade.getWorkflowExecutions(
                (long) Environment.DEVELOPMENT.ordinal(), null, null, null, null, null, null, null, 0))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationWorkflowExecutionFacade.getWorkflowExecutions(
                null, null, null, null, null, null, null, null, 0))
                    .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testANonMemberIsDeniedEveryRunRead() {
        authenticate(NON_MEMBER, false);

        assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecution(PRODUCTION_JOB_ID))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationWorkflowExecutionFacade.getWorkflowExecutionTaskExecution(
                PRODUCTION_JOB_ID, PRODUCTION_TASK_EXECUTION_ID))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationWorkflowExecutionFacade.getWorkflowExecutions(
                (long) Environment.PRODUCTION.ordinal(), null, null, null, null, null, null, null, 0))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationWorkflowExecutionFacade.getWorkflowExecutions(
                null, null, null, null, null, null, null, null, 0))
                    .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testATenantAdminSeesRunDetailsInEveryEnvironment() {
        authenticate(TENANT_ADMIN, true);

        assertThat(integrationWorkflowExecutionFacade.getWorkflowExecution(DEVELOPMENT_JOB_ID)
            .job()
            .inputs()).isEqualTo(Map.of("jobInput", "value"));

        Page<WorkflowExecutionDTO> page = integrationWorkflowExecutionFacade.getWorkflowExecutions(
            null, null, null, null, null, null, null, null, 0);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allSatisfy(
            row -> assertThat(row.job()
                .inputs()).isEqualTo(Map.of("jobInput", "value")));
    }

    @Test
    void testNoCallerEverSeesConnectionParameters() {
        for (String login : List.of(EDITOR, TENANT_ADMIN)) {
            authenticate(login, TENANT_ADMIN.equals(login));

            WorkflowExecutionDTO workflowExecutionDTO = integrationWorkflowExecutionFacade.getWorkflowExecution(
                PRODUCTION_JOB_ID);

            assertThat(workflowExecutionDTO.integrationInstanceConfiguration()
                .getConnectionParameters()).isEmpty();
            assertThat(workflowExecutionDTO.integrationInstanceConfiguration()
                .getId()).isEqualTo(productionConfigurationId);

            Page<WorkflowExecutionDTO> page = integrationWorkflowExecutionFacade.getWorkflowExecutions(
                null, null, null, null, null, null, null, null, 0);

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent()).allSatisfy(
                row -> assertThat(row.integrationInstanceConfiguration()
                    .getConnectionParameters()).isEmpty());
        }

        assertThat(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
            productionConfigurationId)
            .getConnectionParameters()
            .get("client_secret")).isEqualTo("s3cret");
    }

    private void authenticate(String login, boolean tenantAdmin) {
        String authority = tenantAdmin ? "ROLE_ADMIN" : "ROLE_USER";

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private long createConfiguration(long integrationId, Environment environment) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of("client_secret", "s3cret"));
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        return integrationInstanceConfiguration.getId();
    }

    private Integration createPublishedIntegration() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        integrationService.publishIntegration(integration.getId(), null);

        return integrationService.getIntegration(integration.getId());
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private void stubRuns() {
        Map<Long, Long> instanceIdsByJobId = Map.of(
            PRODUCTION_JOB_ID, productionInstanceId, DEVELOPMENT_JOB_ID, developmentInstanceId);

        TriggerExecution triggerExecution = triggerExecution();

        for (Map.Entry<Long, Long> entry : instanceIdsByJobId.entrySet()) {
            Job job = new Job(entry.getKey());

            job.setInputs(Map.of("jobInput", "value"));
            job.setWorkflowId(workflowId);

            when(jobService.getJob(entry.getKey())).thenReturn(job);
            when(jobService.fetchJob(entry.getKey())).thenReturn(Optional.of(job));
            when(principalJobService.fetchJobPrincipalId(entry.getKey(), PlatformType.EMBEDDED))
                .thenReturn(Optional.of(entry.getValue()));
            when(principalJobService.getJobPrincipalId(entry.getKey(), PlatformType.EMBEDDED))
                .thenReturn(entry.getValue());
            when(triggerExecutionService.fetchJobTriggerExecution(entry.getKey()))
                .thenReturn(Optional.of(triggerExecution));
        }

        when(jobService.getJobs(anyList()))
            .thenAnswer(invocation -> ((List<Long>) invocation.getArgument(0)).stream()
                .filter(instanceIdsByJobId::containsKey)
                .map(jobService::getJob)
                .toList());
        when(principalJobService.getPrincipalJobs(anyList(), any()))
            .thenAnswer(invocation -> ((List<Long>) invocation.getArgument(0)).stream()
                .filter(instanceIdsByJobId::containsKey)
                .map(jobId -> new PrincipalJob(instanceIdsByJobId.get(jobId), jobId, PlatformType.EMBEDDED))
                .toList());
        when(principalJobService.getJobIds(any(), any(), any(), anyList(), any(), anyList(), any(Boolean.class),
            any(Integer.class)))
                .thenAnswer(invocation -> {
                    List<Long> instanceIds = invocation.getArgument(3);

                    return new PageImpl<>(
                        instanceIdsByJobId.entrySet()
                            .stream()
                            .filter(entry -> instanceIds.contains(entry.getValue()))
                            .map(Map.Entry::getKey)
                            .sorted()
                            .toList());
                });

        WorkflowTask workflowTask = new WorkflowTask(Map.of("name", "task", "type", "componentName/v1/action"));
        FileEntry taskOutput = mock(FileEntry.class);

        TaskExecution taskExecution = TaskExecution.builder()
            .id(PRODUCTION_TASK_EXECUTION_ID)
            .jobId(PRODUCTION_JOB_ID)
            .output(taskOutput)
            .workflowTask(workflowTask)
            .build();

        when(taskExecutionService.getTaskExecution(PRODUCTION_TASK_EXECUTION_ID)).thenReturn(taskExecution);

        doReturn(Map.of()).when(taskFileStorage)
            .readContextValue(any());
        when(taskFileStorage.readTaskExecutionOutput(any())).thenReturn("task-output");
        when(triggerFileStorage.readTriggerExecutionOutput(any())).thenReturn("trigger-output");
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private static TriggerExecution triggerExecution() {
        WorkflowTrigger workflowTrigger = mock(WorkflowTrigger.class);
        FileEntry triggerOutput = mock(FileEntry.class);

        when(workflowTrigger.getParameters()).thenReturn(Map.of());
        when(workflowTrigger.evaluateParameters(any(), any())).thenReturn((Map) Map.of("evaluated", true));

        TriggerExecution triggerExecution = mock(TriggerExecution.class);

        when(triggerExecution.getType()).thenReturn("componentName/v1/trigger");
        when(triggerExecution.getWorkflowTrigger()).thenReturn(workflowTrigger);
        when(triggerExecution.getOutput()).thenReturn(triggerOutput);

        return triggerExecution;
    }

    private static long userId(String login) {
        return USER_IDS_BY_LOGIN.get(login);
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }

    @TestConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ApiKeyPermissionScopeProvider.class, ApiPlatformPermissionScopeProvider.class,
        AppEventPermissionScopeProvider.class, ConnectedUserPermissionScopeProvider.class,
        ConnectionPermissionScopeProvider.class, CurrentUserResolver.class, CustomRoleScopeResolverImpl.class,
        DataTablePermissionScopeProvider.class, DeploymentPermissionScopeProvider.class,
        EmbeddedJobEnvironmentResolver.class, EmbeddedJobOwnershipResolver.class, EmbeddedPermissionScopeProvider.class,
        EmbeddedPrincipalEnvironmentResolver.class, EmbeddedScopeCacheService.class,
        EmbeddedSharedPermissionScopeProvider.class, ExecutionPermissionScopeProvider.class,
        IntegrationInstanceConfigurationPermissionScopeProvider.class, IntegrationPermissionScopeProvider.class,
        IntegrationWorkflowExecutionFacadeImpl.class, KnowledgeBasePermissionScopeProvider.class,
        McpPermissionScopeProvider.class, PermissionScopeRegistry.class, PermissionServiceImpl.class,
        ProjectPermissionScopeProvider.class, WorkflowPermissionScopeProvider.class,
        WorkspacePermissionScopeProvider.class, WorkspaceScopeCacheService.class
    })
    static class RealPermissionConfiguration {

        @Bean
        ResourceVisibilityResolver resourceVisibilityResolver() {
            return (resourceType, workspaceId, candidates) -> candidates.stream()
                .map(VisibilityRecord::id)
                .collect(Collectors.toSet());
        }

        @Bean
        UserService userService() {
            UserService userService = mock(UserService.class);

            for (Map.Entry<String, Long> entry : USER_IDS_BY_LOGIN.entrySet()) {
                User user = new User();

                user.setId(entry.getValue());
                user.setLogin(entry.getKey());

                when(userService.fetchUserByLogin(entry.getKey())).thenReturn(Optional.of(user));
                when(userService.getUser(entry.getKey())).thenReturn(user);
                when(userService.getUser(entry.getValue())).thenReturn(user);
            }

            return userService;
        }

        @Bean
        WorkflowFacade workflowFacade(
            ComponentConnectionFacade componentConnectionFacade,
            ComponentDefinitionService componentDefinitionService, WorkflowValidatorFacade workflowValidatorFacade,
            WorkflowService workflowService) {

            return new WorkflowFacadeImpl(
                componentConnectionFacade, componentDefinitionService, workflowValidatorFacade, workflowService);
        }

        @Bean
        WorkflowService workflowService(
            CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
            List<WorkflowRepository> workflowRepositories) {

            return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
        }
    }
}
