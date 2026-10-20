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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.mcp.security.McpComponentEnvironmentResolver;
import com.bytechef.automation.ai.mcp.security.McpServerEnvironmentResolver;
import com.bytechef.automation.ai.mcp.security.McpServerOwnershipResolver;
import com.bytechef.automation.ai.mcp.security.McpToolEnvironmentResolver;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.security.ApiKeyEnvironmentResolver;
import com.bytechef.automation.configuration.security.ApiKeyOwnershipResolver;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.security.ConnectionOwnershipResolver;
import com.bytechef.automation.configuration.security.EmbeddedResourceResolver;
import com.bytechef.automation.configuration.security.ProjectOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowCreator;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.workflow.execution.security.JobOwnershipResolver;
import com.bytechef.automation.workflow.execution.security.TriggerExecutionOwnershipResolver;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
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
import com.bytechef.ee.embedded.ai.mcp.facade.EmbeddedMcpServerFacade;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceToolFacade;
import com.bytechef.ee.embedded.ai.mcp.security.EmbeddedMcpServerOwnershipResolver;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserAccessDeciderImpl;
import com.bytechef.ee.embedded.configuration.security.EmbeddedConnectionOwnershipResolver;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.facade.ConnectedUserFacade;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.facade.EmbeddedApiKeyFacade;
import com.bytechef.ee.embedded.security.facade.EmbeddedApiKeyFacadeImpl;
import com.bytechef.ee.embedded.security.security.EmbeddedApiKeyOwnershipResolver;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.ee.embedded.workflow.execution.facade.IntegrationWorkflowExecutionFacade;
import com.bytechef.ee.embedded.workflow.execution.facade.IntegrationWorkflowExecutionFacadeImpl;
import com.bytechef.ee.embedded.workflow.execution.security.EmbeddedJobEnvironmentResolver;
import com.bytechef.ee.embedded.workflow.execution.security.EmbeddedJobOwnershipResolver;
import com.bytechef.ee.embedded.workflow.execution.security.EmbeddedPrincipalEnvironmentResolver;
import com.bytechef.ee.embedded.workflow.execution.security.EmbeddedTriggerExecutionOwnershipResolver;
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
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacadeImpl;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import com.bytechef.platform.mcp.service.McpComponentServiceImpl;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpServerServiceImpl;
import com.bytechef.platform.mcp.service.McpToolServiceImpl;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.audit.ApiKeyAuditPublisher;
import com.bytechef.platform.security.facade.ApiKeyFacadeImpl;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.service.ApiKeyServiceImpl;
import com.bytechef.platform.security.service.OwnedApiKeyCreatorImpl;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidator;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.domain.PageImpl;
import org.springframework.jdbc.core.JdbcTemplate;
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
        IntegrationIntTestConfiguration.class, EmbeddedGatesIntTest.ConnectedUserServiceConfiguration.class,
        EmbeddedGatesIntTest.EmbeddedMcpConfiguration.class, EmbeddedGatesIntTest.RealPermissionConfiguration.class
    },
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true", "spring.liquibase.contexts=configuration,user"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ClusterElementDefinitionService.class, ComponentConnectionFacade.class,
    ComponentDefinitionFacade.class, ComponentDefinitionService.class, ConnectionDefinitionService.class,
    ConnectionFacade.class, ConnectionService.class, ContextService.class, EmbeddedPermissionEvaluator.class,
    EnvironmentService.class, JobFacade.class, JobService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectDeploymentFacade.class, ProjectDeploymentService.class,
    ProjectDeploymentWorkflowService.class, ProjectFacade.class, ProjectService.class, ProjectWorkflowCreator.class,
    ProjectWorkflowFacade.class, ProjectWorkflowService.class, TaskDispatcherDefinitionService.class,
    TaskExecutionService.class, TaskFileStorage.class, TriggerDefinitionFacade.class, TriggerDefinitionService.class,
    TriggerExecutionService.class, TriggerFileStorage.class, TriggerLifecycleFacade.class,
    WebhookTriggerTestFacade.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class
})
class EmbeddedGatesIntTest {

    private static final String AUTOMATION_EDITOR = "automation-editor";
    private static final String DEFINITION = """
        {"label":"Sync contacts","inputs":[],"triggers":[],"tasks":[]}
        """;
    private static final String DEVELOPMENT_EDITOR = "development-editor";
    private static final String EMBEDDED_VIEWER = "embedded-viewer";
    private static final String PRODUCTION_ADMIN = "production-admin";
    private static final String TENANT_ADMIN = "tenant-admin";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        TENANT_ADMIN, 9900L, EMBEDDED_VIEWER, 9901L, DEVELOPMENT_EDITOR, 9902L, PRODUCTION_ADMIN, 9903L,
        AUTOMATION_EDITOR, 9904L);

    @Autowired
    private AutomationWorkflowProjectAdminFacade automationWorkflowProjectAdminFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectedUserFacade connectedUserFacade;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectionDefinitionService connectionDefinitionService;

    @Autowired
    private EmbeddedApiKeyFacade embeddedApiKeyFacade;

    @Autowired
    private EmbeddedMcpServerFacade embeddedMcpServerFacade;

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private IntegrationFacade integrationFacade;

    @Autowired
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

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
    private IntegrationWorkflowFacade integrationWorkflowFacade;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpIntegrationInstanceToolFacade mcpIntegrationInstanceToolFacade;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private McpToolRepository mcpToolRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private TagService tagService;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private WorkflowValidatorFacade workflowValidatorFacade;

    private ConnectedUser connectedUser;
    private long connectedUserProjectId;
    private long developmentConfigurationId;
    private long developmentServerId;
    private String externalUserId;
    private long hubProjectId;
    private long integrationId;
    private long integrationInstanceId;
    private long mcpToolId;
    private long plainProjectId;
    private long productionConfigurationId;
    private long productionServerId;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getEnvironment(anyLong()))
            .thenAnswer(invocation -> Environment.values()[((Long) invocation.getArgument(0)).intValue()]);
        when(componentDefinitionService.getComponentDefinition(anyString(), any()))
            .thenReturn(new ComponentDefinition("componentName"));
        when(connectionDefinitionService.getConnectionDefinition(anyString(), any()))
            .thenReturn(new ConnectionDefinition(ComponentDsl.connection(), "componentName", null, null));
        when(workflowValidatorFacade.getReservedNodeNames(anyString()))
            .thenAnswer(invocation -> WorkflowValidator.getReservedNodeNames(invocation.getArgument(0)));
        when(principalJobService.getJobIds(
            any(), any(), any(), anyList(), any(), anyList(), any(Boolean.class), any(Integer.class)))
                .thenAnswer(invocation -> new PageImpl<Long>(List.of()));

        embeddedUserRepository.save(EmbeddedUser.forRole(userId(EMBEDDED_VIEWER), WorkspaceRole.VIEWER));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(DEVELOPMENT_EDITOR), WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(PRODUCTION_ADMIN), WorkspaceRole.ADMIN, Environment.PRODUCTION));

        workspaceUserRepository.save(
            WorkspaceUser.forRole(userId(AUTOMATION_EDITOR), Workspace.DEFAULT_WORKSPACE_ID, WorkspaceRole.EDITOR,
                null));

        permissionService.evictAllEmbeddedScopeCache();
        permissionService.evictAllWorkspaceScopeCache();

        externalUserId = "external-user-" + UUID.randomUUID();

        connectedUser = connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        Integration integration = createPublishedIntegration();

        integrationId = integration.getId();

        productionConfigurationId = createConfiguration(integrationId, Environment.PRODUCTION);
        developmentConfigurationId = createConfiguration(integrationId, Environment.DEVELOPMENT);

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            connectedUser.getId(), 41L, productionConfigurationId);

        integrationInstanceId = integrationInstance.getId();

        integrationInstanceService.updateEnabled(integrationInstanceId, true);

        productionServerId = createServer(Environment.PRODUCTION);
        developmentServerId = createServer(Environment.DEVELOPMENT);

        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent("componentName", 1, productionServerId, null));

        mcpToolId = mcpToolRepository.save(new McpTool("tool", Map.of(), mcpComponent.getId()))
            .getId();

        plainProjectId = createProject("plain");
        hubProjectId = createProject("hub");
        connectedUserProjectId = createProject("connected-user");

        jdbcTemplate.update("INSERT INTO automation_workflow_project (project_id) VALUES (?)", hubProjectId);
        jdbcTemplate.update(
            "INSERT INTO connected_user_project (connected_user_id, project_id) VALUES (?, ?)", connectedUser.getId(),
            connectedUserProjectId);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            embeddedUserRepository.deleteByUserId(userId);
            workspaceUserRepository.deleteByUserId(userId);
        }

        permissionService.evictAllEmbeddedScopeCache();
        permissionService.evictAllWorkspaceScopeCache();

        jdbcTemplate.update("DELETE FROM connected_user_project");
        jdbcTemplate.update("DELETE FROM automation_workflow_project");
        jdbcTemplate.update("DELETE FROM project WHERE id IN (?, ?, ?)", plainProjectId, hubProjectId,
            connectedUserProjectId);

        jdbcTemplate.update("DELETE FROM mcp_integration_instance_tool");

        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();

        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();

        connectedUserService.deleteConnectedUser(connectedUser.getId());

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }
    }

    @Test
    void testTheAutomationEditorReachesNoEmbeddedResourceOrEmbeddedProject() {
        authenticate(AUTOMATION_EDITOR, false);

        assertThat(permissionService.hasWorkspaceScopeForProject(plainProjectId, "PROJECT_PUBLISH")).isTrue();
        assertThat(permissionService.hasWorkspaceScopeForProject(hubProjectId, "PROJECT_PUBLISH")).isFalse();
        assertThat(permissionService.hasWorkspaceScopeForProject(connectedUserProjectId, "PROJECT_PUBLISH")).isFalse();

        assertThatThrownBy(() -> integrationFacade.getIntegration(integrationId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationFacade.getIntegrations(null, false, null, null, false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationFacade.createIntegration(newIntegrationDTO()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationWorkflowFacade.addWorkflow(integrationId, DEFINITION))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(
                productionConfigurationId))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
                developmentConfigurationId, List.of()))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserFacade.getConnectedUser(connectedUser.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.getEmbeddedMcpServers())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.deleteEmbeddedMcpServer(productionServerId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedApiKeyFacade.getApiKeys(Environment.PRODUCTION.ordinal()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecutions(
            (long) Environment.PRODUCTION.ordinal(), null, null, null, null, null, null, null, 0))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.getProjects())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.deleteProject(hubProjectId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(integrationRepository.findAll()).hasSize(1);
        assertThat(mcpServerRepository.findAll()).hasSize(2);
    }

    @Test
    void testTheDevelopmentEditorBuildsWorkflowsButCannotTouchProduction() {
        authenticate(DEVELOPMENT_EDITOR, false);

        assertThat(integrationWorkflowFacade.addWorkflow(integrationId, DEFINITION)).isPositive();

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
            developmentConfigurationId, List.of());
        embeddedMcpServerFacade.updateEmbeddedMcpServer(developmentServerId, "renamed", null, null, null);

        assertThat(mcpServerService.getMcpServer(developmentServerId)
            .getName()).isEqualTo("renamed");
        assertThat(embeddedApiKeyFacade.getApiKeys(Environment.DEVELOPMENT.ordinal())).isEmpty();
        assertThat(integrationWorkflowExecutionFacade.getWorkflowExecutions(
            (long) Environment.DEVELOPMENT.ordinal(), null, null, null, null, null, null, null, 0)
            .getContent()).isEmpty();
        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServers())
            .extracting(McpServer::getId)
            .containsExactly(developmentServerId);
        assertThat(permissionService.hasResourceScopeInEnvironment(
            hubProjectId, "AutomationWorkflowProject", "WORKFLOW_CREATE", Environment.DEVELOPMENT)).isTrue();

        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
                productionConfigurationId, List.of()))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> embeddedMcpServerFacade.updateEmbeddedMcpServer(productionServerId, "renamed", null, null, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedApiKeyFacade.getApiKeys(Environment.PRODUCTION.ordinal()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecutions(
            (long) Environment.PRODUCTION.ordinal(), null, null, null, null, null, null, null, 0))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.deleteEmbeddedMcpServer(developmentServerId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.deleteProject(hubProjectId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(permissionService.hasResourceScopeInEnvironment(
            hubProjectId, "AutomationWorkflowProject", "WORKFLOW_CREATE", Environment.PRODUCTION)).isFalse();
        assertThat(mcpServerRepository.findById(productionServerId)
            .orElseThrow()
            .getName()).startsWith("server-");
    }

    @Test
    void testTheProductionAdminManagesOnlyProduction() {
        authenticate(PRODUCTION_ADMIN, false);

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
            productionConfigurationId, List.of());
        embeddedMcpServerFacade.updateEmbeddedMcpServer(productionServerId, "renamed", null, null, null);

        assertThat(embeddedApiKeyFacade.getApiKeys(Environment.PRODUCTION.ordinal())).isEmpty();
        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServers())
            .extracting(McpServer::getId)
            .containsExactly(productionServerId);

        McpServer createdMcpServer = embeddedMcpServerFacade.createEmbeddedMcpServer(
            "created", Environment.PRODUCTION, null);

        embeddedMcpServerFacade.deleteEmbeddedMcpServer(createdMcpServer.getId());

        assertThat(mcpServerRepository.findById(createdMcpServer.getId())).isEmpty();

        assertThatThrownBy(() -> embeddedMcpServerFacade.deleteEmbeddedMcpServer(developmentServerId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> embeddedMcpServerFacade.createEmbeddedMcpServer("created", Environment.DEVELOPMENT, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
                developmentConfigurationId, List.of()))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedApiKeyFacade.getApiKeys(Environment.DEVELOPMENT.ordinal()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecutions(
            (long) Environment.DEVELOPMENT.ordinal(), null, null, null, null, null, null, null, 0))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationWorkflowFacade.addWorkflow(integrationId, DEFINITION))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.updateEmbeddedMcpServerSecretKey(productionServerId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(mcpServerRepository.findById(developmentServerId)).isPresent();
    }

    @Test
    void testTheViewerReadsEverywhereAndChangesNothing() {
        authenticate(EMBEDDED_VIEWER, false);

        assertThat(integrationFacade.getIntegration(integrationId)).isNotNull();
        assertThat(integrationFacade.getIntegrations(null, false, null, null, false)).isNotEmpty();
        assertThat(integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(
            developmentConfigurationId)).isNotNull();
        assertThat(integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(
            productionConfigurationId)).isNotNull();
        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServers())
            .extracting(McpServer::getId)
            .containsExactlyInAnyOrder(productionServerId, developmentServerId);
        assertThat(integrationWorkflowExecutionFacade.getWorkflowExecutions(
            (long) Environment.PRODUCTION.ordinal(), null, null, null, null, null, null, null, 0)
            .getContent()).isEmpty();
        assertThat(connectedUserFacade.getConnectedUser(connectedUser.getId())).isNotNull();
        assertThat(permissionService.hasResourceScope(hubProjectId, "AutomationWorkflowProject", "WORKFLOW_VIEW"))
            .isTrue();

        assertThatThrownBy(() -> integrationFacade.createIntegration(newIntegrationDTO()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationFacade.deleteIntegration(integrationId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationWorkflowFacade.addWorkflow(integrationId, DEFINITION))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
                productionConfigurationId, List.of()))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> embeddedMcpServerFacade.updateEmbeddedMcpServer(productionServerId, "renamed", null, null, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.deleteEmbeddedMcpServer(developmentServerId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedApiKeyFacade.getApiKeys(Environment.PRODUCTION.ordinal()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserFacade.deleteConnectedUser(connectedUser.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.deleteProject(hubProjectId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(permissionService.hasResourceScopeInEnvironment(
            hubProjectId, "AutomationWorkflowProject", "WORKFLOW_CREATE", Environment.DEVELOPMENT)).isFalse();
        assertThat(integrationRepository.findAll()).hasSize(1);
        assertThat(mcpServerRepository.findAll()).hasSize(2);
    }

    @Test
    void testTheTenantAdminPassesEveryConvertedGate() {
        authenticate(TENANT_ADMIN, true);

        assertThat(integrationFacade.getIntegration(integrationId)).isNotNull();
        assertThat(integrationFacade.createIntegration(newIntegrationDTO())).isPositive();
        assertThat(integrationWorkflowFacade.addWorkflow(integrationId, DEFINITION)).isPositive();

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
            developmentConfigurationId, List.of());
        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
            productionConfigurationId, List.of());

        assertThat(connectedUserFacade.getConnectedUser(connectedUser.getId())).isNotNull();
        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServers())
            .extracting(McpServer::getId)
            .containsExactlyInAnyOrder(productionServerId, developmentServerId);

        embeddedMcpServerFacade.updateEmbeddedMcpServer(developmentServerId, "renamed", null, null, null);
        embeddedMcpServerFacade.updateEmbeddedMcpServerSecretKey(developmentServerId);

        assertThat(embeddedApiKeyFacade.getApiKeys(Environment.DEVELOPMENT.ordinal())).isEmpty();
        assertThat(embeddedApiKeyFacade.getApiKeys(Environment.PRODUCTION.ordinal())).isEmpty();
        assertThat(integrationWorkflowExecutionFacade.getWorkflowExecutions(
            (long) Environment.PRODUCTION.ordinal(), null, null, null, null, null, null, null, 0)
            .getContent()).isEmpty();
        assertThat(permissionService.hasWorkspaceScopeForProject(hubProjectId, "PROJECT_SETTINGS")).isTrue();
        assertThat(permissionService.hasWorkspaceScopeForProject(connectedUserProjectId, "PROJECT_SETTINGS"))
            .isTrue();
        assertThat(permissionService.hasResourceScope(hubProjectId, "AutomationWorkflowProject", "INTEGRATION_DELETE"))
            .isTrue();

        embeddedMcpServerFacade.deleteEmbeddedMcpServer(developmentServerId);

        assertThat(mcpServerRepository.findById(developmentServerId)).isEmpty();
    }

    @Test
    void testTheConnectedUserKeepsTheirOwnPaths() {
        authenticateConnectedUser();

        mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId, true);

        assertThat(permissionService.hasWorkspaceScopeForProject(connectedUserProjectId, "PROJECT_PUBLISH")).isTrue();
        assertThat(permissionService.hasWorkspaceScopeForProject(hubProjectId, "PROJECT_PUBLISH")).isFalse();
        assertThat(permissionService.hasWorkspaceScopeForProject(plainProjectId, "PROJECT_PUBLISH")).isFalse();

        assertThatThrownBy(() -> embeddedMcpServerFacade.getEmbeddedMcpServers())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationFacade.getIntegrations(null, false, null, null, false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(
            productionConfigurationId))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserFacade.deleteConnectedUser(connectedUser.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedApiKeyFacade.getApiKeys(Environment.PRODUCTION.ordinal()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationWorkflowExecutionFacade.getWorkflowExecutions(
            (long) Environment.PRODUCTION.ordinal(), null, null, null, null, null, null, null, 0))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.getProjects())
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testTheApplicationContextStartsWithChainedResolvers() {
        StandardEnvironment environment = new StandardEnvironment();

        MutablePropertySources propertySources = environment.getPropertySources();

        propertySources.addFirst(new MapPropertySource("edition", Map.of("bytechef.edition", "ee")));

        try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
            applicationContext.setEnvironment(environment);
            applicationContext.register(ChainedResolversConfiguration.class);
            applicationContext.refresh();

            assertThat(applicationContext.getBean(PermissionServiceImpl.class)).isNotNull();
            assertThat(applicationContext.getBean(ConnectedUserAccessDeciderImpl.class)).isNotNull();

            Map<String, ResourceOwnershipResolver> ownershipResolversByName =
                applicationContext.getBeansOfType(ResourceOwnershipResolver.class);

            for (String resourceType : List.of("ApiKey", "Connection", "Job", "McpServer", "TriggerExecution")) {
                List<String> resolverClassNames = new ArrayList<>();

                for (ResourceOwnershipResolver resourceOwnershipResolver : ownershipResolversByName.values()) {
                    if (resourceType.equals(resourceOwnershipResolver.resourceType())) {
                        Class<?> resolverClass = resourceOwnershipResolver.getClass();

                        resolverClassNames.add(resolverClass.getName());
                    }
                }

                assertThat(resolverClassNames)
                    .as("%s is chained across the automation and the embedded side", resourceType)
                    .hasSize(2)
                    .anyMatch(className -> className.startsWith("com.bytechef.ee.embedded."))
                    .anyMatch(className -> className.startsWith("com.bytechef.automation."));
            }

            McpServer embeddedServer = mock(McpServer.class);

            when(embeddedServer.getType()).thenReturn(PlatformType.EMBEDDED);

            McpServer automationServer = mock(McpServer.class);

            when(automationServer.getType()).thenReturn(PlatformType.AUTOMATION);

            McpServerRepository mcpServerRepository = applicationContext.getBean(McpServerRepository.class);

            when(mcpServerRepository.findById(1L)).thenReturn(Optional.of(embeddedServer));
            when(mcpServerRepository.findById(2L)).thenReturn(Optional.of(automationServer));

            WorkspaceMcpServerService workspaceMcpServerService = applicationContext.getBean(
                WorkspaceMcpServerService.class);

            when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(2L)).thenReturn(Optional.of(77L));

            PrincipalJobService chainedPrincipalJobService = applicationContext.getBean(PrincipalJobService.class);

            when(chainedPrincipalJobService.fetchJobPrincipalId(7L, PlatformType.EMBEDDED))
                .thenReturn(Optional.of(5L));

            assertThat(resolveChainedOwner(ownershipResolversByName, "McpServer", 1L)
                .embedded()).isTrue();

            ResourceOwner automationOwner = resolveChainedOwner(ownershipResolversByName, "McpServer", 2L);

            assertThat(automationOwner.embedded()).isFalse();
            assertThat(automationOwner.workspaceId()).hasValue(77L);

            assertThat(resolveChainedOwner(ownershipResolversByName, "Job", 7L)
                .embedded()).isTrue();
            assertThat(resolveChainedOwner(ownershipResolversByName, "Job", 8L)
                .isUnknown()).isTrue();
        }
    }

    private static ResourceOwner resolveChainedOwner(
        Map<String, ResourceOwnershipResolver> ownershipResolversByName, String resourceType, long id) {

        for (ResourceOwnershipResolver resourceOwnershipResolver : ownershipResolversByName.values()) {
            if (resourceType.equals(resourceOwnershipResolver.resourceType())) {
                ResourceOwner resourceOwner = resourceOwnershipResolver.resolveOwner(id);

                if (!resourceOwner.isUnknown()) {
                    return resourceOwner;
                }
            }
        }

        return ResourceOwner.unknown();
    }

    private void authenticate(String login, boolean tenantAdmin) {
        String authority = tenantAdmin ? "ROLE_ADMIN" : "ROLE_USER";

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private void authenticateConnectedUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new EmbeddedApiKeyAuthenticationToken(
                    Environment.PRODUCTION.ordinal(), connectedUser.getId(),
                    new org.springframework.security.core.userdetails.User(externalUserId, "", List.of()), false));
    }

    private long createConfiguration(long integrationId, Environment environment) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
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

    private long createProject(String name) {
        Long projectId = jdbcTemplate.queryForObject(
            "INSERT INTO project (name, workspace_id, uuid, created_date, created_by, last_modified_date, "
                + "last_modified_by, version) VALUES (?, ?, gen_random_uuid(), now(), 'system', now(), 'system', 0) "
                + "RETURNING id",
            Long.class, name + "-" + UUID.randomUUID(), Workspace.DEFAULT_WORKSPACE_ID);

        return Objects.requireNonNull(projectId);
    }

    private long createServer(Environment environment) {
        McpServer mcpServer = mcpServerService.create(
            "server-" + UUID.randomUUID(), PlatformType.EMBEDDED, environment, true);

        return Objects.requireNonNull(mcpServer.getId());
    }

    private static IntegrationDTO newIntegrationDTO() {
        return IntegrationDTO.builder()
            .componentName("componentName")
            .name("integration-" + UUID.randomUUID())
            .build();
    }

    private static long userId(String login) {
        return USER_IDS_BY_LOGIN.get(login);
    }

    @Configuration
    @Import({
        ApiKeyOwnershipResolver.class, ConnectedUserAccessDeciderImpl.class, ConnectionOwnershipResolver.class,
        EmbeddedApiKeyOwnershipResolver.class, EmbeddedConnectionOwnershipResolver.class,
        EmbeddedJobOwnershipResolver.class, EmbeddedMcpServerOwnershipResolver.class,
        EmbeddedTriggerExecutionOwnershipResolver.class, JobOwnershipResolver.class, McpServerOwnershipResolver.class,
        PermissionServiceImpl.class, TriggerExecutionOwnershipResolver.class
    })
    static class ChainedResolversConfiguration {

        @Bean
        ApiKeyService apiKeyService() {
            return mock(ApiKeyService.class);
        }

        @Bean
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade() {
            return mock(AutomationWorkflowProjectFacade.class);
        }

        @Bean
        ConnectedUserConnectionService connectedUserConnectionService() {
            return mock(ConnectedUserConnectionService.class);
        }

        @Bean
        ConnectedUserProjectService connectedUserProjectService() {
            return mock(ConnectedUserProjectService.class);
        }

        @Bean
        ConnectionService connectionService() {
            return mock(ConnectionService.class);
        }

        @Bean
        CurrentUserResolver currentUserResolver() {
            return mock(CurrentUserResolver.class);
        }

        @Bean
        EmbeddedResourceResolver embeddedResourceResolver() {
            return mock(EmbeddedResourceResolver.class);
        }

        @Bean
        EmbeddedScopeCacheService embeddedScopeCacheService() {
            return mock(EmbeddedScopeCacheService.class);
        }

        @Bean
        EmbeddedUserRepository embeddedUserRepository() {
            return mock(EmbeddedUserRepository.class);
        }

        @Bean
        IntegrationInstanceService integrationInstanceService() {
            return mock(IntegrationInstanceService.class);
        }

        @Bean
        JobService jobService() {
            return mock(JobService.class);
        }

        @Bean
        McpServerRepository mcpServerRepository() {
            return mock(McpServerRepository.class);
        }

        @Bean
        PermissionScopeRegistry permissionScopeRegistry() {
            return mock(PermissionScopeRegistry.class);
        }

        @Bean
        PrincipalJobService principalJobService() {
            return mock(PrincipalJobService.class);
        }

        @Bean
        ProjectDeploymentService projectDeploymentService() {
            return mock(ProjectDeploymentService.class);
        }

        @Bean
        ProjectRepository projectRepository() {
            return mock(ProjectRepository.class);
        }

        @Bean
        ProjectService projectService() {
            return mock(ProjectService.class);
        }

        @Bean
        ProjectWorkflowService projectWorkflowService() {
            return mock(ProjectWorkflowService.class);
        }

        @Bean
        TriggerExecutionService triggerExecutionService() {
            return mock(TriggerExecutionService.class);
        }

        @Bean
        WorkspaceApiKeyRepository workspaceApiKeyRepository() {
            return mock(WorkspaceApiKeyRepository.class);
        }

        @Bean
        WorkspaceConnectionRepository workspaceConnectionRepository() {
            return mock(WorkspaceConnectionRepository.class);
        }

        @Bean
        WorkspaceMcpServerService workspaceMcpServerService() {
            return mock(WorkspaceMcpServerService.class);
        }

        @Bean
        WorkspaceScopeCacheService workspaceScopeCacheService() {
            return mock(WorkspaceScopeCacheService.class);
        }

        @Bean
        WorkspaceUserRepository workspaceUserRepository() {
            return mock(WorkspaceUserRepository.class);
        }
    }

    @Configuration
    @ComponentScan({
        "com.bytechef.ee.embedded.connected.user.facade", "com.bytechef.ee.embedded.connected.user.security",
        "com.bytechef.ee.embedded.connected.user.service"
    })
    static class ConnectedUserServiceConfiguration {
    }

    @Configuration
    @ComponentScan(
        basePackages = "com.bytechef.ee.embedded.ai.mcp",
        excludeFilters = @Filter(
            type = FilterType.REGEX, pattern = "com\\.bytechef\\.ee\\.embedded\\.ai\\.mcp\\.config\\..*"))
    @Import({
        McpComponentEnvironmentResolver.class, McpComponentServiceImpl.class, McpServerEnvironmentResolver.class,
        McpServerFacadeImpl.class, McpServerServiceImpl.class, McpToolEnvironmentResolver.class,
        McpToolServiceImpl.class
    })
    static class EmbeddedMcpConfiguration {
    }

    @TestConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ApiKeyAuditPublisher.class, ApiKeyEnvironmentResolver.class, ApiKeyFacadeImpl.class,
        ApiKeyOwnershipResolver.class, ApiKeyPermissionScopeProvider.class, ApiKeyServiceImpl.class,
        ApiPlatformPermissionScopeProvider.class, AppEventPermissionScopeProvider.class,
        ConnectedUserPermissionScopeProvider.class, ConnectionPermissionScopeProvider.class,
        CurrentUserResolver.class, CustomRoleScopeResolverImpl.class, DataTablePermissionScopeProvider.class,
        DeploymentPermissionScopeProvider.class, EmbeddedApiKeyFacadeImpl.class,
        EmbeddedApiKeyOwnershipResolver.class, EmbeddedJobEnvironmentResolver.class, EmbeddedJobOwnershipResolver.class,
        EmbeddedPermissionScopeProvider.class, EmbeddedPrincipalEnvironmentResolver.class,
        EmbeddedScopeCacheService.class, EmbeddedSharedPermissionScopeProvider.class,
        ExecutionPermissionScopeProvider.class, IntegrationInstanceConfigurationPermissionScopeProvider.class,
        IntegrationPermissionScopeProvider.class, IntegrationWorkflowExecutionFacadeImpl.class,
        KnowledgeBasePermissionScopeProvider.class, McpPermissionScopeProvider.class, OwnedApiKeyCreatorImpl.class,
        PermissionScopeRegistry.class, PermissionServiceImpl.class, ProjectOwnershipResolver.class,
        ProjectPermissionScopeProvider.class, WorkflowPermissionScopeProvider.class,
        WorkspacePermissionScopeProvider.class, WorkspaceScopeCacheService.class
    })
    static class RealPermissionConfiguration {

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
