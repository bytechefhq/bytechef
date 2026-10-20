/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
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
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.ai.mcp.security.McpComponentEnvironmentResolver;
import com.bytechef.automation.ai.mcp.security.McpServerEnvironmentResolver;
import com.bytechef.automation.ai.mcp.security.McpToolEnvironmentResolver;
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
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.config.ApplicationProperties;
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
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.facade.ConnectedUserMcpServerFacade;
import com.bytechef.ee.embedded.ai.mcp.facade.EmbeddedMcpServerFacade;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceConfigurationWorkflowFacade;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceToolFacade;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceWorkflowFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
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
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.facade.McpServerFacadeImpl;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import com.bytechef.platform.mcp.service.McpComponentServiceImpl;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpServerServiceImpl;
import com.bytechef.platform.mcp.service.McpToolServiceImpl;
import com.bytechef.platform.mcp.web.graphql.McpServerGraphQlController;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
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
        IntegrationIntTestConfiguration.class, EmbeddedMcpGatesIntTest.ConnectedUserServiceConfiguration.class,
        EmbeddedMcpGatesIntTest.EmbeddedMcpConfiguration.class,
        EmbeddedMcpGatesIntTest.RealPermissionConfiguration.class
    },
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true", "spring.liquibase.contexts=configuration,user"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, ClusterElementDefinitionService.class,
    ComponentConnectionFacade.class, ComponentDefinitionFacade.class, ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionService.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class, JobFacade.class, JobService.class,
    OAuth2ParametersFacade.class, OAuth2Service.class, PrincipalJobFacade.class, PrincipalJobService.class,
    ProjectDeploymentFacade.class, ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class,
    ProjectFacade.class, ProjectService.class, ProjectWorkflowCreator.class, ProjectWorkflowFacade.class,
    ProjectWorkflowService.class, TaskDispatcherDefinitionService.class, TriggerDefinitionFacade.class,
    TriggerDefinitionService.class, TriggerExecutionService.class, TriggerLifecycleFacade.class,
    WebhookTriggerTestFacade.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class
})
class EmbeddedMcpGatesIntTest {

    private static final String DEFINITION = """
        {"label":"Sync contacts","inputs":[],"triggers":[],"tasks":[]}
        """;
    private static final String DEVELOPMENT_EDITOR = "development-editor";
    private static final String EDITOR_IN_PRODUCTION = "editor-in-production";
    private static final String EMBEDDED_VIEWER = "embedded-viewer";
    private static final String NON_MEMBER = "non-member";
    private static final String PRODUCTION_ADMIN = "production-admin";
    private static final String TENANT_ADMIN = "tenant-admin";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        TENANT_ADMIN, 9700L, EMBEDDED_VIEWER, 9701L, EDITOR_IN_PRODUCTION, 9702L, NON_MEMBER, 9703L,
        PRODUCTION_ADMIN, 9704L, DEVELOPMENT_EDITOR, 9705L);

    @Autowired
    private ConnectedUserMcpServerFacade connectedUserMcpServerFacade;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private EmbeddedMcpServerFacade embeddedMcpServerFacade;

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowRepository integrationInstanceConfigurationWorkflowRepository;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationInstanceRepository integrationInstanceRepository;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationInstanceWorkflowRepository integrationInstanceWorkflowRepository;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpIntegrationInstanceConfigurationFacade mcpIntegrationInstanceConfigurationFacade;

    @Autowired
    private McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;

    @Autowired
    private McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade;

    @Autowired
    private McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @Autowired
    private McpIntegrationInstanceToolFacade mcpIntegrationInstanceToolFacade;

    @Autowired
    private McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @Autowired
    private McpIntegrationInstanceWorkflowFacade mcpIntegrationInstanceWorkflowFacade;

    @Autowired
    private McpServerFacade mcpServerFacade;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private McpToolRepository mcpToolRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private TagService tagService;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectionDefinitionService connectionDefinitionService;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private WorkflowValidatorFacade workflowValidatorFacade;

    private ConnectedUser connectedUser;
    private long developmentConfigurationId;
    private long developmentIntegrationInstanceConfigurationWorkflowId;
    private long developmentMcpComponentId;
    private long developmentMcpConfigurationId;
    private long developmentMcpWorkflowId;
    private long developmentServerId;
    private String externalUserId;
    private long integrationInstanceId;
    private long developmentMcpToolId;
    private long mcpToolId;
    private long productionConfigurationId;
    private long productionIntegrationInstanceConfigurationWorkflowId;
    private long productionMcpComponentId;
    private long productionMcpConfigurationId;
    private long productionMcpWorkflowId;
    private long productionServerId;
    private String workflowUuid;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getEnvironment(anyLong()))
            .thenAnswer(invocation -> Environment.values()[((Long) invocation.getArgument(0)).intValue()]);
        when(componentDefinitionService.getComponentDefinition(anyString(), any()))
            .thenReturn(new ComponentDefinition("componentName"));
        when(connectionDefinitionService.getConnectionDefinition(anyString(), any()))
            .thenReturn(new ConnectionDefinition(ComponentDsl.connection(), "componentName", null, null));

        embeddedUserRepository.save(EmbeddedUser.forRole(userId(EMBEDDED_VIEWER), WorkspaceRole.VIEWER));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(EDITOR_IN_PRODUCTION), WorkspaceRole.EDITOR, Environment.PRODUCTION));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(DEVELOPMENT_EDITOR), WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(PRODUCTION_ADMIN), WorkspaceRole.ADMIN, Environment.PRODUCTION));

        permissionService.evictAllEmbeddedScopeCache();

        externalUserId = "external-user-" + UUID.randomUUID();

        connectedUser = connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        Integration integration = createPublishedIntegration();

        Workflow workflow = workflowService.create(DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.addWorkflow(
            integration.getId(), 1, workflow.getId());

        workflowUuid = integrationWorkflow.getUuidAsString();

        productionConfigurationId = createConfiguration(integration.getId(), Environment.PRODUCTION, 1);
        developmentConfigurationId = createConfiguration(integration.getId(), Environment.DEVELOPMENT, 1);

        productionIntegrationInstanceConfigurationWorkflowId = createConfigurationWorkflow(
            productionConfigurationId, workflow.getId());

        Workflow developmentWorkflow = workflowService.create(
            DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        developmentIntegrationInstanceConfigurationWorkflowId = createConfigurationWorkflow(
            developmentConfigurationId, developmentWorkflow.getId());

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            connectedUser.getId(), 41L, productionConfigurationId);

        integrationInstanceId = integrationInstance.getId();

        integrationInstanceService.updateEnabled(integrationInstanceId, true);
        integrationInstanceConfigurationService.updateEnabled(productionConfigurationId, true);
        integrationInstanceConfigurationService.updateEnabled(developmentConfigurationId, true);

        productionServerId = createServer(Environment.PRODUCTION);
        developmentServerId = createServer(Environment.DEVELOPMENT);

        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent("componentName", 1, productionServerId, null));

        productionMcpComponentId = mcpComponent.getId();

        McpTool mcpTool = mcpToolRepository.save(new McpTool("tool", Map.of(), productionMcpComponentId));

        mcpToolId = mcpTool.getId();

        McpComponent developmentMcpComponent = mcpComponentRepository.save(
            new McpComponent("componentName", 1, developmentServerId, null));

        developmentMcpComponentId = developmentMcpComponent.getId();
        developmentMcpToolId = mcpToolRepository.save(new McpTool("tool", Map.of(), developmentMcpComponentId))
            .getId();

        productionMcpConfigurationId = createMcpConfiguration(productionServerId, productionConfigurationId);
        developmentMcpConfigurationId = createMcpConfiguration(developmentServerId, developmentConfigurationId);

        productionMcpWorkflowId = mcpIntegrationInstanceConfigurationWorkflowService.create(
            productionMcpConfigurationId, productionIntegrationInstanceConfigurationWorkflowId)
            .getId();
        developmentMcpWorkflowId = mcpIntegrationInstanceConfigurationWorkflowService.create(
            developmentMcpConfigurationId, developmentIntegrationInstanceConfigurationWorkflowId)
            .getId();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            embeddedUserRepository.deleteByUserId(userId);
        }

        permissionService.evictAllEmbeddedScopeCache();

        jdbcTemplate.update("DELETE FROM mcp_integration_instance_tool");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration");

        integrationInstanceWorkflowRepository.deleteAll();
        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationWorkflowRepository.deleteAll();
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
    void testTheServerListShowsOnlyEnvironmentsTheMemberCanView() {
        authenticate(EDITOR_IN_PRODUCTION, false);

        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServers())
            .extracting(McpServer::getId)
            .containsExactly(productionServerId);
        assertThat(mcpIntegrationInstanceConfigurationFacade.getMcpIntegrationInstanceConfigurations())
            .extracting(McpIntegrationInstanceConfiguration::getId)
            .containsExactly(productionMcpConfigurationId);
        assertThat(mcpIntegrationInstanceConfigurationWorkflowFacade.getMcpIntegrationInstanceConfigurationWorkflows())
            .extracting(McpIntegrationInstanceConfigurationWorkflow::getId)
            .containsExactly(productionMcpWorkflowId);

        authenticate(DEVELOPMENT_EDITOR, false);

        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServers())
            .extracting(McpServer::getId)
            .containsExactly(developmentServerId);
        assertThat(mcpIntegrationInstanceConfigurationFacade.getMcpIntegrationInstanceConfigurations())
            .extracting(McpIntegrationInstanceConfiguration::getId)
            .containsExactly(developmentMcpConfigurationId);
        assertThat(mcpIntegrationInstanceConfigurationWorkflowFacade.getMcpIntegrationInstanceConfigurationWorkflows())
            .extracting(McpIntegrationInstanceConfigurationWorkflow::getId)
            .containsExactly(developmentMcpWorkflowId);

        authenticate(EMBEDDED_VIEWER, false);

        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServers())
            .extracting(McpServer::getId)
            .containsExactlyInAnyOrder(productionServerId, developmentServerId);
        assertThat(mcpIntegrationInstanceConfigurationFacade.getMcpIntegrationInstanceConfigurations())
            .extracting(McpIntegrationInstanceConfiguration::getId)
            .containsExactlyInAnyOrder(productionMcpConfigurationId, developmentMcpConfigurationId);
        assertThat(mcpIntegrationInstanceConfigurationWorkflowFacade.getMcpIntegrationInstanceConfigurationWorkflows())
            .extracting(McpIntegrationInstanceConfigurationWorkflow::getId)
            .containsExactlyInAnyOrder(productionMcpWorkflowId, developmentMcpWorkflowId);
        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServerTags()).isEmpty();
        assertThat(embeddedMcpServerFacade.getMcpComponentDefinitions()).isEmpty();
    }

    @Test
    void testAnEditorEditsButOnlyAnAdminDeletesAServer() {
        authenticate(EDITOR_IN_PRODUCTION, false);

        embeddedMcpServerFacade.updateEmbeddedMcpServer(productionServerId, "renamed", null, null, null);
        embeddedMcpServerFacade.updateEmbeddedMcpServerTags(productionServerId, List.of());
        embeddedMcpServerFacade.updateEmbeddedMcpToolEnabled(mcpToolId, false);

        McpComponent createdMcpComponent = embeddedMcpServerFacade.createEmbeddedMcpComponent(
            new McpComponent("otherComponent", 1, productionServerId, null), List.of());

        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServerMcpComponents(productionServerId))
            .hasSize(2);
        assertThat(embeddedMcpServerFacade.getEmbeddedMcpComponentMcpTools(productionMcpComponentId))
            .hasSize(1);
        assertThat(mcpServerService.getMcpServer(productionServerId)
            .getName()).isEqualTo("renamed");

        embeddedMcpServerFacade.deleteEmbeddedMcpComponent(createdMcpComponent.getId());

        McpServer createdMcpServer = embeddedMcpServerFacade.createEmbeddedMcpServer(
            "created", Environment.PRODUCTION, null);

        assertThat(createdMcpServer.getId()).isNotNull();

        assertThatThrownBy(
            () -> embeddedMcpServerFacade.updateEmbeddedMcpServer(developmentServerId, "renamed", null, null, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.updateEmbeddedMcpServerTags(developmentServerId, List.of()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> embeddedMcpServerFacade.createEmbeddedMcpComponent(
                new McpComponent("otherComponent", 1, developmentServerId, null), List.of()))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> embeddedMcpServerFacade.createEmbeddedMcpServer("created", Environment.DEVELOPMENT, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.getEmbeddedMcpServerMcpComponents(developmentServerId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.deleteEmbeddedMcpServer(createdMcpServer.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.updateEmbeddedMcpServerSecretKey(productionServerId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                productionServerId, developmentConfigurationId, List.of()))
                    .isInstanceOf(AccessDeniedException.class);

        assertThat(mcpServerRepository.findById(createdMcpServer.getId())).isPresent();

        authenticate(PRODUCTION_ADMIN, false);

        embeddedMcpServerFacade.deleteEmbeddedMcpServer(createdMcpServer.getId());

        assertThat(mcpServerRepository.findById(createdMcpServer.getId())).isEmpty();

        assertThatThrownBy(() -> embeddedMcpServerFacade.deleteEmbeddedMcpServer(developmentServerId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.updateEmbeddedMcpServerSecretKey(productionServerId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(mcpServerRepository.findById(developmentServerId)).isPresent();
    }

    @Test
    void testTheServerTagsShowOnlyEnvironmentsTheMemberCanView() {
        Tag productionTag = tagService.save(List.of(new Tag("production-tag")))
            .getFirst();
        Tag developmentTag = tagService.save(List.of(new Tag("development-tag")))
            .getFirst();

        McpServer productionServer = mcpServerRepository.findById(productionServerId)
            .orElseThrow();
        McpServer developmentServer = mcpServerRepository.findById(developmentServerId)
            .orElseThrow();

        productionServer.setTagIds(List.of(productionTag.getId()));
        developmentServer.setTagIds(List.of(developmentTag.getId()));

        mcpServerRepository.save(productionServer);
        mcpServerRepository.save(developmentServer);

        authenticate(EDITOR_IN_PRODUCTION, false);

        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServerTags())
            .extracting(Tag::getName)
            .containsExactly("production-tag");

        authenticate(EMBEDDED_VIEWER, false);

        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServerTags())
            .extracting(Tag::getName)
            .containsExactlyInAnyOrder("production-tag", "development-tag");

        authenticate(TENANT_ADMIN, true);

        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServerTags())
            .extracting(Tag::getName)
            .containsExactlyInAnyOrder("production-tag", "development-tag");
    }

    @Test
    void testAnEditorEditsToolsAndComponentsOnlyInTheirEnvironment() {
        authenticate(EDITOR_IN_PRODUCTION, false);

        McpTool productionMcpTool = mcpToolRepository.findById(mcpToolId)
            .orElseThrow();
        McpTool developmentMcpTool = mcpToolRepository.findById(developmentMcpToolId)
            .orElseThrow();

        embeddedMcpServerFacade.updateEmbeddedMcpTool(productionMcpTool);

        assertThatThrownBy(() -> embeddedMcpServerFacade.updateEmbeddedMcpTool(developmentMcpTool))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.deleteEmbeddedMcpTool(developmentMcpToolId))
            .isInstanceOf(AccessDeniedException.class);

        McpComponent productionMcpComponent = mcpComponentRepository.findById(productionMcpComponentId)
            .orElseThrow();
        McpComponent developmentMcpComponent = mcpComponentRepository.findById(developmentMcpComponentId)
            .orElseThrow();

        assertThatThrownBy(
            () -> embeddedMcpServerFacade.updateEmbeddedMcpComponent(developmentMcpComponent, null, List.of()))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(mcpToolRepository.findById(developmentMcpToolId)).isPresent();

        embeddedMcpServerFacade.deleteEmbeddedMcpTool(mcpToolId);

        assertThat(mcpToolRepository.findById(mcpToolId)).isEmpty();

        embeddedMcpServerFacade.updateEmbeddedMcpComponent(productionMcpComponent, null, List.of());
    }

    @Test
    void testAConnectedUserStillTogglesTheirTools() {
        authenticateConnectedUser();

        mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId, true);

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(McpIntegrationInstanceTool::isEnabled)
            .containsExactly(true);

        mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId, false);

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(McpIntegrationInstanceTool::isEnabled)
            .containsExactly(false);

        mcpIntegrationInstanceWorkflowFacade.enableMcpIntegrationInstanceWorkflow(
            integrationInstanceId, workflowUuid, true);
        mcpIntegrationInstanceWorkflowFacade.updateMcpIntegrationInstanceWorkflow(
            integrationInstanceId, workflowUuid, Map.of("key", "value"));

        assertThatThrownBy(() -> embeddedMcpServerFacade.getEmbeddedMcpServers())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.deleteEmbeddedMcpServer(productionServerId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUser.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> mcpIntegrationInstanceConfigurationFacade.getMcpIntegrationInstanceConfigurations())
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAMemberTogglesToolsAndConnectedUserServersInTheirEnvironment() {
        McpIntegrationInstanceTool mcpIntegrationInstanceTool = mcpIntegrationInstanceToolService
            .createMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId, false);

        authenticate(EDITOR_IN_PRODUCTION, false);

        mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId, true);
        connectedUserMcpServerFacade.enableMcpTool(mcpIntegrationInstanceTool.getId(), false);

        assertThat(connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUser.getId()))
            .hasSize(1);

        connectedUserMcpServerFacade.enableConnectedUserMcpServer(connectedUser.getId(), productionServerId, true);

        assertThatThrownBy(
            () -> connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUser.getId(), productionServerId))
                .isInstanceOf(AccessDeniedException.class);

        authenticate(PRODUCTION_ADMIN, false);

        connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUser.getId(), productionServerId);

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .isEmpty();

        authenticate(DEVELOPMENT_EDITOR, false);

        assertThatThrownBy(() -> connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUser.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool(
                integrationInstanceId, mcpToolId, true))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> mcpIntegrationInstanceWorkflowFacade.enableMcpIntegrationInstanceWorkflow(
                integrationInstanceId, workflowUuid, true))
                    .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testATenantAdminWithoutARowStillPasses() {
        authenticate(TENANT_ADMIN, true);

        assertThat(embeddedMcpServerFacade.getEmbeddedMcpServers())
            .extracting(McpServer::getId)
            .containsExactlyInAnyOrder(productionServerId, developmentServerId);
        assertThat(mcpIntegrationInstanceConfigurationFacade.getMcpIntegrationInstanceConfigurations())
            .hasSize(2);
        assertThat(mcpIntegrationInstanceConfigurationWorkflowFacade.getMcpIntegrationInstanceConfigurationWorkflows())
            .hasSize(2);
        assertThat(mcpIntegrationInstanceConfigurationFacade.getMcpIntegrationInstanceConfiguration(
            developmentMcpConfigurationId)).isNotNull();
        assertThat(mcpIntegrationInstanceConfigurationFacade.getMcpServerMcpIntegrationInstanceConfigurations(
            developmentServerId)).hasSize(1);
        assertThat(connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUser.getId())).hasSize(1);

        embeddedMcpServerFacade.updateEmbeddedMcpServer(developmentServerId, "renamed", null, null, null);
        embeddedMcpServerFacade.updateEmbeddedMcpServerSecretKey(developmentServerId);

        McpServer stagingMcpServer = embeddedMcpServerFacade.createEmbeddedMcpServer(
            "staging", Environment.STAGING, null);
        long stagingConfigurationId = createConfiguration(
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(productionConfigurationId)
                .getIntegrationId(),
            Environment.STAGING, 1);

        assertThat(
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                stagingMcpServer.getId(), stagingConfigurationId, List.of()))
                    .isNotNull();

        mcpIntegrationInstanceConfigurationFacade.deleteMcpIntegrationInstanceConfiguration(
            developmentMcpConfigurationId);

        embeddedMcpServerFacade.deleteEmbeddedMcpServer(stagingMcpServer.getId());
        embeddedMcpServerFacade.deleteEmbeddedMcpServer(developmentServerId);

        assertThat(mcpServerRepository.findById(developmentServerId)).isEmpty();
    }

    @Test
    void testANonMemberIsDenied() {
        McpIntegrationInstanceTool mcpIntegrationInstanceTool = mcpIntegrationInstanceToolService
            .createMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId, true);

        authenticate(NON_MEMBER, false);

        assertThatThrownBy(
            () -> embeddedMcpServerFacade.updateEmbeddedMcpServer(productionServerId, "renamed", null, null, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> embeddedMcpServerFacade.createEmbeddedMcpServer("created", Environment.PRODUCTION, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.deleteEmbeddedMcpServer(productionServerId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.getEmbeddedMcpServers())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.getEmbeddedMcpServerTags())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.updateEmbeddedMcpToolEnabled(mcpToolId, false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedMcpServerFacade.getEmbeddedMcpComponentMcpTools(productionMcpComponentId))
            .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                productionServerId, productionConfigurationId, List.of()))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationFacade.deleteMcpIntegrationInstanceConfiguration(
                productionMcpConfigurationId))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationFacade.getMcpIntegrationInstanceConfiguration(
                productionMcpConfigurationId))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> mcpIntegrationInstanceConfigurationFacade.getMcpIntegrationInstanceConfigurations())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationFacade.getMcpServerMcpIntegrationInstanceConfigurations(
                productionServerId))
                    .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationWorkflowFacade.createMcpIntegrationInstanceConfigurationWorkflow(
                productionMcpConfigurationId, productionIntegrationInstanceConfigurationWorkflowId))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationWorkflowFacade.deleteMcpIntegrationInstanceConfigurationWorkflow(
                productionMcpWorkflowId))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationWorkflowFacade.getMcpIntegrationInstanceConfigurationWorkflow(
                productionMcpWorkflowId))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationWorkflowFacade.getMcpIntegrationInstanceConfigurationWorkflows())
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationWorkflowFacade
                .getToolEligibleIntegrationInstanceConfigurationWorkflows(
                    productionConfigurationId))
                        .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(
            () -> mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool(
                integrationInstanceId, mcpToolId, false))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> mcpIntegrationInstanceWorkflowFacade.enableMcpIntegrationInstanceWorkflow(
                integrationInstanceId, workflowUuid, true))
                    .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUser.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserMcpServerFacade.enableMcpTool(mcpIntegrationInstanceTool.getId(), false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserMcpServerFacade.enableConnectedUserMcpServer(
                connectedUser.getId(), productionServerId, false))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUser.getId(), productionServerId))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(mcpServerRepository.findAll()).hasSize(2);
        assertThat(mcpIntegrationInstanceConfigurationService.getMcpIntegrationInstanceConfigurations()).hasSize(2);
        assertThat(mcpIntegrationInstanceConfigurationWorkflowService.getMcpIntegrationInstanceConfigurationWorkflows())
            .hasSize(2);
        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(McpIntegrationInstanceTool::isEnabled)
            .containsExactly(true);
        assertThat(mcpToolRepository.findById(mcpToolId))
            .hasValueSatisfying(mcpTool -> assertThat(mcpTool.isEnabled()).isTrue());
    }

    @Test
    void testAConfigurationFromAnotherEnvironmentIsRejected() {
        authenticate(TENANT_ADMIN, true);

        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                productionServerId, developmentConfigurationId, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("environment");
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                developmentServerId, productionConfigurationId, List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("environment");
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationWorkflowFacade.createMcpIntegrationInstanceConfigurationWorkflow(
                productionMcpConfigurationId, developmentIntegrationInstanceConfigurationWorkflowId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("environment");
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationWorkflowFacade.updateMcpIntegrationInstanceConfigurationWorkflow(
                productionMcpWorkflowId, null, developmentIntegrationInstanceConfigurationWorkflowId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("environment");
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationWorkflowFacade.updateMcpIntegrationInstanceConfigurationWorkflow(
                productionMcpWorkflowId, developmentMcpConfigurationId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("environment");

        assertThat(mcpIntegrationInstanceConfigurationService.getMcpIntegrationInstanceConfigurations()).hasSize(2);
        assertThat(mcpIntegrationInstanceConfigurationWorkflowService.getMcpIntegrationInstanceConfigurationWorkflows())
            .extracting(
                McpIntegrationInstanceConfigurationWorkflow::getMcpIntegrationInstanceConfigurationId,
                McpIntegrationInstanceConfigurationWorkflow::getIntegrationInstanceConfigurationWorkflowId)
            .containsExactlyInAnyOrder(
                tuple(
                    productionMcpConfigurationId, productionIntegrationInstanceConfigurationWorkflowId),
                tuple(
                    developmentMcpConfigurationId, developmentIntegrationInstanceConfigurationWorkflowId));

        mcpIntegrationInstanceConfigurationWorkflowFacade.updateMcpIntegrationInstanceConfigurationWorkflow(
            productionMcpWorkflowId, productionMcpConfigurationId,
            productionIntegrationInstanceConfigurationWorkflowId);
    }

    @Test
    void testOnlyAnEditorOfAnEmbeddedServerReadsItsSecretKey() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.setPublicUrl("http://localhost");

        McpServerGraphQlController mcpServerGraphQlController = new McpServerGraphQlController(
            applicationProperties, mcpServerFacade, mcpServerService);

        McpServer productionServer = mcpServerRepository.findById(productionServerId)
            .orElseThrow();

        String secretKey = productionServer.getSecretKey();

        assertThat(secretKey).isNotBlank();

        for (String login : List.of(EMBEDDED_VIEWER, DEVELOPMENT_EDITOR)) {
            authenticate(login, false);

            assertThat(mcpServerGraphQlController.secretKey(productionServer)).isNull();
            assertThat(mcpServerGraphQlController.url(List.of(productionServer)))
                .containsEntry(productionServer, "http://localhost/api/embedded/********/mcp");
        }

        for (String login : List.of(EDITOR_IN_PRODUCTION, TENANT_ADMIN)) {
            authenticate(login, TENANT_ADMIN.equals(login));

            assertThat(mcpServerGraphQlController.secretKey(productionServer)).isEqualTo(secretKey);
            assertThat(mcpServerGraphQlController.url(List.of(productionServer)))
                .containsEntry(productionServer, "http://localhost/api/embedded/" + secretKey + "/mcp");
        }
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

    private long createConfiguration(long integrationId, Environment environment, int integrationVersion) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(integrationVersion);
        integrationInstanceConfiguration.setName("configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        return integrationInstanceConfiguration.getId();
    }

    private long createConfigurationWorkflow(long integrationInstanceConfigurationId, String workflowId) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        integrationInstanceConfigurationWorkflow = integrationInstanceConfigurationWorkflowService.create(
            integrationInstanceConfigurationWorkflow);

        return integrationInstanceConfigurationWorkflow.getId();
    }

    private long createMcpConfiguration(long mcpServerId, long integrationInstanceConfigurationId) {
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationService.create(
                new McpIntegrationInstanceConfiguration(integrationInstanceConfigurationId, mcpServerId));

        return mcpIntegrationInstanceConfiguration.getId();
    }

    private Integration createPublishedIntegration() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        integrationService.publishIntegration(integration.getId(), null);

        return integrationService.getIntegration(integration.getId());
    }

    private long createServer(Environment environment) {
        McpServer mcpServer = mcpServerService.create(
            "server-" + UUID.randomUUID(), PlatformType.EMBEDDED, environment, true);

        return Objects.requireNonNull(mcpServer.getId());
    }

    private static long userId(String login) {
        return USER_IDS_BY_LOGIN.get(login);
    }

    @Configuration
    @ComponentScan({
        "com.bytechef.ee.embedded.connected.user.security", "com.bytechef.ee.embedded.connected.user.service"
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
        ApiKeyPermissionScopeProvider.class, ApiPlatformPermissionScopeProvider.class,
        AppEventPermissionScopeProvider.class, ConnectedUserPermissionScopeProvider.class,
        ConnectionPermissionScopeProvider.class, CurrentUserResolver.class, CustomRoleScopeResolverImpl.class,
        DataTablePermissionScopeProvider.class, DeploymentPermissionScopeProvider.class,
        EmbeddedPermissionScopeProvider.class, EmbeddedScopeCacheService.class,
        EmbeddedSharedPermissionScopeProvider.class, ExecutionPermissionScopeProvider.class,
        IntegrationInstanceConfigurationPermissionScopeProvider.class, IntegrationPermissionScopeProvider.class,
        KnowledgeBasePermissionScopeProvider.class, McpPermissionScopeProvider.class, PermissionScopeRegistry.class,
        PermissionServiceImpl.class, ProjectPermissionScopeProvider.class, WorkflowPermissionScopeProvider.class,
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
