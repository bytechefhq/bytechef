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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacadeImpl;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.security.ConnectionEnvironmentResolver;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowCreator;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
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
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserConnectionRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.facade.ConnectedUserFacade;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
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
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        IntegrationIntTestConfiguration.class,
        EmbeddedConnectedUserGatesIntTest.ConnectedUserServiceConfiguration.class,
        EmbeddedConnectedUserGatesIntTest.RealPermissionConfiguration.class
    },
    properties = {
        "bytechef.edition=EE", "bytechef.workflow.repository.jdbc.enabled=true",
        "spring.liquibase.contexts=configuration,user"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, ClusterElementDefinitionService.class,
    ComponentConnectionFacade.class, ComponentDefinitionFacade.class, ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionService.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class, JobFacade.class, JobService.class,
    McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectDeploymentFacade.class,
    ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class, ProjectFacade.class, ProjectService.class,
    ProjectWorkflowCreator.class, ProjectWorkflowFacade.class, ProjectWorkflowService.class,
    TaskDispatcherDefinitionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, WebhookTriggerTestFacade.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class,
    WorkflowTestConfigurationService.class
})
class EmbeddedConnectedUserGatesIntTest {

    private static final long AUTOMATION_CONNECTION_ID = 93003L;
    private static final long CONNECTED_USER_CREATED_CONNECTION_ID = 93030L;
    private static final long EMBEDDED_DEVELOPMENT_CONNECTION_ID = 93002L;
    private static final long EMBEDDED_PRODUCTION_CONNECTION_ID = 93001L;
    private static final long INSTANCE_CONNECTION_ID = 93010L;
    private static final long OWN_CONNECTION_ID = 93020L;
    private static final String ADMIN_IN_PRODUCTION = "admin-in-production";
    private static final String EDITOR_IN_PRODUCTION = "editor-in-production";
    private static final String EDITOR_IN_DEVELOPMENT = "editor-in-development-viewer-elsewhere";
    private static final String EMBEDDED_ADMIN = "embedded-admin";
    private static final String NON_MEMBER = "non-member";
    private static final String TENANT_ADMIN = "tenant-admin";
    private static final String VIEWER_IN_PRODUCTION = "viewer-in-production";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        TENANT_ADMIN, 9700L, VIEWER_IN_PRODUCTION, 9701L, EDITOR_IN_PRODUCTION, 9702L, ADMIN_IN_PRODUCTION, 9703L,
        EMBEDDED_ADMIN, 9704L, NON_MEMBER, 9705L, EDITOR_IN_DEVELOPMENT, 9706L);

    @Autowired
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserConnectionRepository connectedUserConnectionRepository;

    @Autowired
    private ConnectedUserFacade connectedUserFacade;

    @Autowired
    private ConnectedUserProjectAdminFacade connectedUserProjectAdminFacade;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserWorkflowReferenceAdminFacade connectedUserWorkflowReferenceAdminFacade;

    @Autowired
    private ConnectionAdminFacade connectionAdminFacade;

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    private ConnectedUser developmentConnectedUser;
    private String externalUserId;
    private ConnectedUser productionConnectedUser;
    private Long referenceProjectId;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getEnvironment(anyLong()))
            .thenAnswer(invocation -> Environment.values()[((Long) invocation.getArgument(0)).intValue()]);

        when(connectionService.fetchConnection(AUTOMATION_CONNECTION_ID))
            .thenReturn(
                Optional.of(connection(AUTOMATION_CONNECTION_ID, PlatformType.AUTOMATION, Environment.PRODUCTION)));
        when(connectionService.fetchConnection(EMBEDDED_DEVELOPMENT_CONNECTION_ID))
            .thenReturn(
                Optional.of(
                    connection(EMBEDDED_DEVELOPMENT_CONNECTION_ID, PlatformType.EMBEDDED, Environment.DEVELOPMENT)));
        when(connectionService.fetchConnection(EMBEDDED_PRODUCTION_CONNECTION_ID))
            .thenReturn(
                Optional.of(
                    connection(EMBEDDED_PRODUCTION_CONNECTION_ID, PlatformType.EMBEDDED, Environment.PRODUCTION)));
        when(connectionService.getConnections(anyList()))
            .thenAnswer(invocation -> {
                List<Long> connectionIds = invocation.getArgument(0);

                return connectionIds.stream()
                    .map(connectionId -> connection(connectionId, PlatformType.EMBEDDED, Environment.PRODUCTION))
                    .toList();
            });
        when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED)))
            .thenReturn(OWN_CONNECTION_ID);
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED)))
            .thenAnswer(invocation -> {
                List<Long> connectionIds = invocation.getArgument(0);

                return connectionIds.stream()
                    .map(connectionId -> ConnectionDTO.builder()
                        .componentName("slack")
                        .id(connectionId)
                        .build())
                    .toList();
            });

        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(VIEWER_IN_PRODUCTION), WorkspaceRole.VIEWER, Environment.PRODUCTION));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(EDITOR_IN_PRODUCTION), WorkspaceRole.EDITOR, Environment.PRODUCTION));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(ADMIN_IN_PRODUCTION), WorkspaceRole.ADMIN, Environment.PRODUCTION));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId(EMBEDDED_ADMIN), WorkspaceRole.ADMIN));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(EDITOR_IN_DEVELOPMENT), WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(EDITOR_IN_DEVELOPMENT), WorkspaceRole.VIEWER, Environment.PRODUCTION));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(EDITOR_IN_DEVELOPMENT), WorkspaceRole.VIEWER, Environment.STAGING));

        permissionService.evictAllEmbeddedScopeCache();

        externalUserId = "external-user-" + UUID.randomUUID();

        productionConnectedUser = connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);
        developmentConnectedUser = connectedUserService.createConnectedUser(externalUserId, Environment.DEVELOPMENT);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            embeddedUserRepository.deleteByUserId(userId);
        }

        permissionService.evictAllEmbeddedScopeCache();

        jdbcTemplate.update("DELETE FROM connected_user_project_workflow");
        jdbcTemplate.update("DELETE FROM connected_user_project");

        if (referenceProjectId != null) {
            jdbcTemplate.update("DELETE FROM project WHERE id = ?", referenceProjectId);

            referenceProjectId = null;
        }

        connectedUserConnectionRepository.deleteAll();
        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();

        for (ConnectedUser connectedUser : List.of(productionConnectedUser, developmentConnectedUser)) {
            if (connectedUserService.fetchConnectedUser(connectedUser.getId())
                .isPresent()) {

                connectedUserService.deleteConnectedUser(connectedUser.getId());
            }
        }
    }

    @Test
    void testAViewerListsConnectedUsersOnlyInTheirEnvironment() {
        authenticate(VIEWER_IN_PRODUCTION, false);

        long productionEnvironmentId = Environment.PRODUCTION.ordinal();
        long developmentEnvironmentId = Environment.DEVELOPMENT.ordinal();

        Page<ConnectedUser> connectedUsers = connectedUserFacade.getConnectedUsers(
            productionEnvironmentId, null, null, null, null, 0);

        assertThat(connectedUsers.getContent())
            .extracting(ConnectedUser::getId)
            .contains(productionConnectedUser.getId())
            .doesNotContain(developmentConnectedUser.getId());
        assertThat(
            connectedUserFacade.getConnectedUserDTOs(productionEnvironmentId, null, null, null, null, null, 0)
                .getContent()).isNotEmpty();
        assertThat(connectedUserFacade.getConnectedUser(productionConnectedUser.getId())).isNotNull();
        assertThat(connectedUserFacade.getConnectedUserDTO(productionConnectedUser.getId())).isNotNull();
        assertThat(connectedUserConnectionFacade.getConnectedUserConnections(
            productionConnectedUser.getId(), null, List.of())).isEmpty();
        assertThat(connectedUserWorkflowReferenceAdminFacade.getReferences(Set.of("workflow-uuid"))).isEmpty();
        assertThat(connectionAdminFacade.getConnectionTags()).isEmpty();
        assertThat(connectionAdminFacade.getConnections(null, null, productionEnvironmentId, null)).isEmpty();

        assertThatThrownBy(
            () -> connectedUserFacade.getConnectedUsers(developmentEnvironmentId, null, null, null, null, 0))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserFacade.getConnectedUsers(null, null, null, null, null, 0))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserFacade.getConnectedUser(developmentConnectedUser.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserFacade.getConnectedUserDTO(developmentConnectedUser.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserConnectionFacade.getConnectedUserConnections(
                developmentConnectedUser.getId(), null, List.of()))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserProjectAdminFacade.getConnectedUserProjects(null, null))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserProjectAdminFacade.getConnectedUserProjects(
                developmentConnectedUser.getId(), Environment.DEVELOPMENT))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectionAdminFacade.getConnections(null, null, developmentEnvironmentId, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserFacade.deleteConnectedUser(productionConnectedUser.getId()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testOnlyAnAdminDeletesAConnectedUser() {
        long instanceId = createInstance(productionConnectedUser.getId());

        authenticate(EDITOR_IN_PRODUCTION, false);

        assertThatThrownBy(() -> connectedUserFacade.deleteConnectedUser(productionConnectedUser.getId()))
            .isInstanceOf(AccessDeniedException.class);

        connectedUserFacade.enableConnectedUser(productionConnectedUser.getId(), false);

        assertThatThrownBy(() -> connectedUserFacade.enableConnectedUser(developmentConnectedUser.getId(), false))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(ADMIN_IN_PRODUCTION, false);

        assertThatThrownBy(() -> connectedUserFacade.deleteConnectedUser(developmentConnectedUser.getId()))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(connectedUserService.fetchConnectedUser(developmentConnectedUser.getId())).isPresent();
        assertThat(integrationInstanceRepository.findById(instanceId)).isPresent();

        connectedUserFacade.deleteConnectedUser(productionConnectedUser.getId());

        assertThat(connectedUserService.fetchConnectedUser(productionConnectedUser.getId())).isEmpty();
        assertThat(integrationInstanceRepository.findById(instanceId)).isEmpty();
        assertThat(connectedUserService.fetchConnectedUser(developmentConnectedUser.getId())).isPresent();

        verify(connectionFacade).delete(INSTANCE_CONNECTION_ID);
    }

    @Test
    void testAConnectedUserStillManagesTheirOwnConnection() {
        authenticateConnectedUser(productionConnectedUser);

        long productionConnectedUserId = productionConnectedUser.getId();

        long connectionId = connectedUserConnectionFacade.createConnectedUserConnection(
            productionConnectedUserId, connectionDTO(Environment.PRODUCTION));

        assertThat(connectionId).isEqualTo(OWN_CONNECTION_ID);
        assertThat(connectedUserConnectionFacade.getOwnedConnectionIds(productionConnectedUserId))
            .containsExactly(OWN_CONNECTION_ID);
        assertThat(connectedUserConnectionFacade.getConnections(productionConnectedUserId, null, List.of()))
            .hasSize(1);
        assertThat(
            connectedUserConnectionFacade.getConnectedUserConnections(productionConnectedUserId, null, List.of()))
                .hasSize(1);
        assertThat(connectedUserConnectionFacade.getSharedConnectionIds()).isEmpty();

        connectedUserConnectionFacade.reauthorizeConnectedUserConnection(
            productionConnectedUserId, OWN_CONNECTION_ID, Map.of("apiKey", "new"));
        connectedUserConnectionFacade.deleteConnectedUserConnection(productionConnectedUserId, OWN_CONNECTION_ID);

        verify(connectionFacade).delete(OWN_CONNECTION_ID);

        assertThat(connectedUserConnectionRepository.findAllByConnectedUserId(productionConnectedUserId)).isEmpty();

        assertThatThrownBy(
            () -> connectedUserConnectionFacade.createConnectedUserConnection(
                developmentConnectedUser.getId(), connectionDTO(Environment.DEVELOPMENT)))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserConnectionFacade.getConnectedUserConnections(
                developmentConnectedUser.getId(), null, List.of()))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserFacade.deleteConnectedUser(productionConnectedUserId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserFacade.getConnectedUser(productionConnectedUserId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getConnection(EMBEDDED_PRODUCTION_CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getSharedConnectionIds())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserWorkflowReferenceAdminFacade.getReferences(Set.of("workflow-uuid")))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAnEmbeddedEditorEditsAnEmbeddedConnectionButNotAnAutomationOne() {
        authenticate(EDITOR_IN_PRODUCTION, false);

        assertThat(connectionAdminFacade.createConnection(connectionDTO(Environment.PRODUCTION), false))
            .isEqualTo(OWN_CONNECTION_ID);

        connectionAdminFacade.getConnection(EMBEDDED_PRODUCTION_CONNECTION_ID);
        connectionAdminFacade.updateConnectionTags(EMBEDDED_PRODUCTION_CONNECTION_ID, List.of());
        connectionAdminFacade.updateConnection(EMBEDDED_PRODUCTION_CONNECTION_ID, "name", List.of(), null, 0);
        connectionAdminFacade.deleteConnection(EMBEDDED_PRODUCTION_CONNECTION_ID);

        verify(connectionFacade).getConnection(EMBEDDED_PRODUCTION_CONNECTION_ID);
        verify(connectionFacade).update(EMBEDDED_PRODUCTION_CONNECTION_ID, List.of());
        verify(connectionFacade).update(EMBEDDED_PRODUCTION_CONNECTION_ID, "name", List.of(), 0);
        verify(connectionFacade).delete(EMBEDDED_PRODUCTION_CONNECTION_ID);

        assertThatThrownBy(() -> connectionAdminFacade.createConnection(connectionDTO(Environment.DEVELOPMENT), false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectionAdminFacade.updateConnectionTags(EMBEDDED_DEVELOPMENT_CONNECTION_ID, List.of()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getConnection(EMBEDDED_DEVELOPMENT_CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.updateConnectionTags(AUTOMATION_CONNECTION_ID, List.of()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.deleteConnection(AUTOMATION_CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(TENANT_ADMIN, true);

        assertThatThrownBy(() -> connectionAdminFacade.updateConnectionTags(AUTOMATION_CONNECTION_ID, List.of()))
            .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> connectionAdminFacade.deleteConnection(AUTOMATION_CONNECTION_ID))
            .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getConnection(AUTOMATION_CONNECTION_ID))
            .isInstanceOf(NoSuchElementException.class);

        verify(connectionFacade, never()).update(AUTOMATION_CONNECTION_ID, List.of());
        verify(connectionFacade, never()).delete(AUTOMATION_CONNECTION_ID);
    }

    @Test
    void testAnEmbeddedEditorManagesAConnectionAConnectedUserCreatedAndAViewerIsRefused() {
        Connection connectedUserConnection = connection(
            CONNECTED_USER_CREATED_CONNECTION_ID, PlatformType.EMBEDDED, Environment.PRODUCTION);

        connectedUserConnection.setCreatedBy(externalUserId);

        when(connectionService.fetchConnection(CONNECTED_USER_CREATED_CONNECTION_ID))
            .thenReturn(Optional.of(connectedUserConnection));

        authenticate(VIEWER_IN_PRODUCTION, false);

        assertThatThrownBy(
            () -> connectionAdminFacade.updateConnectionTags(CONNECTED_USER_CREATED_CONNECTION_ID, List.of()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectionAdminFacade.updateConnection(
                CONNECTED_USER_CREATED_CONNECTION_ID, "name", List.of(), null, 0))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.deleteConnection(CONNECTED_USER_CREATED_CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).update(CONNECTED_USER_CREATED_CONNECTION_ID, List.of());
        verify(connectionFacade, never()).delete(CONNECTED_USER_CREATED_CONNECTION_ID);

        authenticate(EDITOR_IN_PRODUCTION, false);

        connectionAdminFacade.updateConnectionTags(CONNECTED_USER_CREATED_CONNECTION_ID, List.of());
        connectionAdminFacade.updateConnection(CONNECTED_USER_CREATED_CONNECTION_ID, "name", List.of(), null, 0);
        connectionAdminFacade.deleteConnection(CONNECTED_USER_CREATED_CONNECTION_ID);

        verify(connectionFacade).update(CONNECTED_USER_CREATED_CONNECTION_ID, List.of());
        verify(connectionFacade).update(CONNECTED_USER_CREATED_CONNECTION_ID, "name", List.of(), 0);
        verify(connectionFacade).delete(CONNECTED_USER_CREATED_CONNECTION_ID);
    }

    @Test
    void testATenantAdminWithoutARowStillPasses() {
        long instanceId = createInstance(developmentConnectedUser.getId());

        authenticate(TENANT_ADMIN, true);

        assertThat(connectedUserFacade.getConnectedUser(developmentConnectedUser.getId())).isNotNull();
        assertThat(connectedUserFacade.getConnectedUserDTO(productionConnectedUser.getId())).isNotNull();
        assertThat(connectedUserFacade.getConnectedUsers(null, null, null, null, null, 0)
            .getContent())
                .extracting(ConnectedUser::getId)
                .contains(productionConnectedUser.getId(), developmentConnectedUser.getId());
        assertThat(connectedUserProjectAdminFacade.getConnectedUserProjects(null, null)).isEmpty();
        assertThat(connectedUserWorkflowReferenceAdminFacade.getReferences(Set.of("workflow-uuid"))).isEmpty();
        assertThat(connectionAdminFacade.getConnections(null, null, null, null)).isEmpty();
        assertThat(connectionAdminFacade.getConnectionTags()).isEmpty();
        assertThat(connectionAdminFacade.getSharedConnectionIds()).isEmpty();

        connectionAdminFacade.getConnection(EMBEDDED_DEVELOPMENT_CONNECTION_ID);
        connectionAdminFacade.updateConnectionTags(EMBEDDED_DEVELOPMENT_CONNECTION_ID, List.of());

        assertThat(connectionAdminFacade.createConnection(connectionDTO(Environment.STAGING), false))
            .isEqualTo(OWN_CONNECTION_ID);
        assertThat(
            connectedUserConnectionFacade.createConnectedUserConnection(
                productionConnectedUser.getId(), connectionDTO(Environment.PRODUCTION)))
                    .isEqualTo(OWN_CONNECTION_ID);

        connectedUserConnectionFacade.deleteConnectedUserConnection(productionConnectedUser.getId(), OWN_CONNECTION_ID);
        connectedUserFacade.enableConnectedUser(developmentConnectedUser.getId(), false);
        connectedUserFacade.deleteConnectedUser(developmentConnectedUser.getId());

        assertThat(connectedUserService.fetchConnectedUser(developmentConnectedUser.getId())).isEmpty();
        assertThat(integrationInstanceRepository.findById(instanceId)).isEmpty();
    }

    @Test
    void testANonMemberIsDenied() {
        long instanceId = createInstance(productionConnectedUser.getId());

        authenticate(NON_MEMBER, false);

        long productionConnectedUserId = productionConnectedUser.getId();
        long productionEnvironmentId = Environment.PRODUCTION.ordinal();

        assertThatThrownBy(() -> connectedUserFacade.deleteConnectedUser(productionConnectedUserId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserFacade.enableConnectedUser(productionConnectedUserId, false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserFacade.getConnectedUser(productionConnectedUserId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserFacade.getConnectedUsers(productionEnvironmentId, null, null, null, null, 0))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserFacade.getConnectedUserDTOs(productionEnvironmentId, null, null, null, null, null, 0))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserProjectAdminFacade.getConnectedUserProjects(
                productionConnectedUserId, Environment.PRODUCTION))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserWorkflowReferenceAdminFacade.getReferences(Set.of("workflow-uuid")))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserConnectionFacade.createConnectedUserConnection(
                productionConnectedUserId, connectionDTO(Environment.PRODUCTION)))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserConnectionFacade.deleteConnectedUserConnection(
                productionConnectedUserId, OWN_CONNECTION_ID))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectedUserConnectionFacade.getConnectedUserConnections(
                productionConnectedUserId, null, List.of()))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserConnectionFacade.getOwnedConnectionIds(productionConnectedUserId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserConnectionFacade.getSharedConnectionIds())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.createConnection(connectionDTO(Environment.PRODUCTION), false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.deleteConnection(EMBEDDED_PRODUCTION_CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getConnection(EMBEDDED_PRODUCTION_CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getConnectionTags())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getConnections(null, null, productionEnvironmentId, null))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getSharedConnectionIds())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectionAdminFacade.updateConnection(
                EMBEDDED_PRODUCTION_CONNECTION_ID, "name", List.of(), null, 0))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> connectionAdminFacade.updateConnectionTags(EMBEDDED_PRODUCTION_CONNECTION_ID, List.of()))
                .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).create(any(ConnectionDTO.class), any());
        verify(connectionFacade, never()).delete(anyLong());
        verify(connectionFacade, never()).update(anyLong(), anyList());
        verify(connectionFacade, never()).update(anyLong(), anyString(), anyList(), anyInt());

        assertThat(connectedUserService.fetchConnectedUser(productionConnectedUserId)).isPresent();
        assertThat(integrationInstanceRepository.findById(instanceId)).isPresent();
    }

    @Test
    void testTheAutomationConnectionEndpointsRefuseAnEmbeddedConnection() {
        for (String login : List.of(EMBEDDED_ADMIN, TENANT_ADMIN)) {
            authenticate(login, TENANT_ADMIN.equals(login));

            assertThatThrownBy(() -> workspaceConnectionFacade.delete(EMBEDDED_PRODUCTION_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);
            assertThatThrownBy(
                () -> workspaceConnectionFacade.update(EMBEDDED_PRODUCTION_CONNECTION_ID, "name", List.of(), 0))
                    .isInstanceOf(NoSuchElementException.class);
            assertThatThrownBy(() -> workspaceConnectionFacade.updateTags(EMBEDDED_PRODUCTION_CONNECTION_ID, List.of()))
                .isInstanceOf(NoSuchElementException.class);
            assertThatThrownBy(() -> workspaceConnectionFacade.getConnection(EMBEDDED_PRODUCTION_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);
        }

        assertThatThrownBy(() -> workspaceConnectionFacade.disconnectConnection(EMBEDDED_PRODUCTION_CONNECTION_ID))
            .isInstanceOf(NoSuchElementException.class);

        verify(connectionFacade, never()).delete(anyLong());
        verify(connectionFacade, never()).update(anyLong(), anyList());
        verify(connectionFacade, never()).update(anyLong(), anyString(), anyList(), anyInt());
        verify(connectionFacade, never()).getConnection(anyLong());

        workspaceConnectionFacade.updateTags(AUTOMATION_CONNECTION_ID, List.of());
        workspaceConnectionFacade.delete(AUTOMATION_CONNECTION_ID);

        verify(connectionFacade).update(AUTOMATION_CONNECTION_ID, List.of());
        verify(connectionFacade).delete(AUTOMATION_CONNECTION_ID);
    }

    @Test
    void testWorkflowReferencesShowOnlyConnectedUsersOfViewableEnvironments() {
        String automationWorkflowUuid = UUID.randomUUID()
            .toString();

        referenceProjectId = jdbcTemplate.queryForObject(
            "INSERT INTO project (name, workspace_id, uuid, created_date, created_by, last_modified_date, "
                + "last_modified_by, version) VALUES (?, ?, gen_random_uuid(), now(), 'system', now(), 'system', 0) "
                + "RETURNING id",
            Long.class, "reference-" + automationWorkflowUuid, Workspace.DEFAULT_WORKSPACE_ID);

        for (ConnectedUser connectedUser : List.of(productionConnectedUser, developmentConnectedUser)) {
            Long connectedUserProjectId = jdbcTemplate.queryForObject(
                "INSERT INTO connected_user_project (connected_user_id, project_id) VALUES (?, ?) RETURNING id",
                Long.class, connectedUser.getId(), referenceProjectId);

            jdbcTemplate.update(
                "INSERT INTO connected_user_project_workflow (connected_user_project_id, automation_workflow_uuid) "
                    + "VALUES (?, ?)",
                connectedUserProjectId, automationWorkflowUuid);
        }

        Set<String> automationWorkflowUuids = Set.of(automationWorkflowUuid);

        authenticate(VIEWER_IN_PRODUCTION, false);

        assertThat(connectedUserWorkflowReferenceAdminFacade.getReferences(automationWorkflowUuids))
            .extracting(ConnectedUserWorkflowReferenceDTO::environment)
            .containsExactly(Environment.PRODUCTION.name());

        authenticate(EDITOR_IN_DEVELOPMENT, false);

        assertThat(connectedUserWorkflowReferenceAdminFacade.getReferences(automationWorkflowUuids))
            .extracting(ConnectedUserWorkflowReferenceDTO::environment)
            .containsExactlyInAnyOrder(Environment.PRODUCTION.name(), Environment.DEVELOPMENT.name());

        authenticate(TENANT_ADMIN, true);

        assertThat(connectedUserWorkflowReferenceAdminFacade.getReferences(automationWorkflowUuids))
            .extracting(ConnectedUserWorkflowReferenceDTO::environment)
            .containsExactlyInAnyOrder(Environment.PRODUCTION.name(), Environment.DEVELOPMENT.name());
    }

    @Test
    void testOnlyAnEditorOfTheConnectionSeesItsCredentials() {
        createInstance(productionConnectedUser.getId());

        when(connectionFacade.getConnection(EMBEDDED_PRODUCTION_CONNECTION_ID))
            .thenReturn(credentialedConnectionDTO(EMBEDDED_PRODUCTION_CONNECTION_ID, Environment.PRODUCTION));
        when(connectionFacade.getConnections(any(), any(), anyList(), any(), any(), eq(PlatformType.EMBEDDED)))
            .thenReturn(
                List.of(
                    credentialedConnectionDTO(EMBEDDED_PRODUCTION_CONNECTION_ID, Environment.PRODUCTION),
                    credentialedConnectionDTO(EMBEDDED_DEVELOPMENT_CONNECTION_ID, Environment.DEVELOPMENT)));
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED)))
            .thenAnswer(invocation -> {
                List<Long> connectionIds = invocation.getArgument(0);

                return connectionIds.stream()
                    .map(connectionId -> credentialedConnectionDTO(connectionId, Environment.PRODUCTION))
                    .toList();
            });

        long productionConnectedUserId = productionConnectedUser.getId();
        long productionEnvironmentId = Environment.PRODUCTION.ordinal();

        authenticate(VIEWER_IN_PRODUCTION, false);

        assertCredentials(connectionAdminFacade.getConnection(EMBEDDED_PRODUCTION_CONNECTION_ID), false);
        assertThat(connectionAdminFacade.getConnections(null, null, productionEnvironmentId, null))
            .hasSize(2)
            .allSatisfy(connectionDTO -> assertCredentials(connectionDTO, false));
        assertThat(
            connectedUserConnectionFacade.getConnectedUserConnections(productionConnectedUserId, null, List.of()))
                .hasSize(1)
                .allSatisfy(connectionDTO -> assertCredentials(connectionDTO, false));

        authenticate(EDITOR_IN_PRODUCTION, false);

        assertCredentials(connectionAdminFacade.getConnection(EMBEDDED_PRODUCTION_CONNECTION_ID), true);
        assertThat(
            connectedUserConnectionFacade.getConnectedUserConnections(productionConnectedUserId, null, List.of()))
                .hasSize(1)
                .allSatisfy(connectionDTO -> assertCredentials(connectionDTO, true));

        authenticate(EDITOR_IN_DEVELOPMENT, false);

        assertCredentials(connectionAdminFacade.getConnection(EMBEDDED_PRODUCTION_CONNECTION_ID), false);
        assertThat(
            connectedUserConnectionFacade.getConnectedUserConnections(productionConnectedUserId, null, List.of()))
                .hasSize(1)
                .allSatisfy(connectionDTO -> assertCredentials(connectionDTO, false));

        for (ConnectionDTO connectionDTO : connectionAdminFacade.getConnections(null, null, null, null)) {
            assertCredentials(connectionDTO, connectionDTO.id() == EMBEDDED_DEVELOPMENT_CONNECTION_ID);
        }

        authenticate(TENANT_ADMIN, true);

        assertCredentials(connectionAdminFacade.getConnection(EMBEDDED_PRODUCTION_CONNECTION_ID), true);
        assertThat(connectionAdminFacade.getConnections(null, null, null, null))
            .hasSize(2)
            .allSatisfy(connectionDTO -> assertCredentials(connectionDTO, true));
        assertThat(
            connectedUserConnectionFacade.getConnectedUserConnections(productionConnectedUserId, null, List.of()))
                .hasSize(1)
                .allSatisfy(connectionDTO -> assertCredentials(connectionDTO, true));

        authenticateConnectedUser(productionConnectedUser);

        assertThat(
            connectedUserConnectionFacade.getConnectedUserConnections(productionConnectedUserId, null, List.of()))
                .hasSize(1)
                .allSatisfy(connectionDTO -> assertCredentials(connectionDTO, true));
        assertThat(connectedUserConnectionFacade.getConnections(productionConnectedUserId, null, List.of()))
            .hasSize(1)
            .allSatisfy(connectionDTO -> assertCredentials(connectionDTO, true));
    }

    private static void assertCredentials(ConnectionDTO connectionDTO, boolean expected) {
        if (expected) {
            assertThat(connectionDTO.parameters()).isEqualTo(Map.of("api_key", "k3y"));
            assertThat(connectionDTO.authorizationParameters()).isEqualTo(Map.of("access_token", "t0k3n"));
            assertThat(connectionDTO.connectionParameters()).isEqualTo(Map.of("client_secret", "s3cret"));
        } else {
            assertThat(connectionDTO.parameters()).isEmpty();
            assertThat(connectionDTO.authorizationParameters()).isEmpty();
            assertThat(connectionDTO.connectionParameters()).isEmpty();
        }

        assertThat(connectionDTO.componentName()).isEqualTo("slack");
    }

    private static ConnectionDTO credentialedConnectionDTO(long id, Environment environment) {
        return ConnectionDTO.builder()
            .authorizationParameters(Map.of("access_token", "t0k3n"))
            .componentName("slack")
            .connectionParameters(Map.of("client_secret", "s3cret"))
            .environmentId(environment.ordinal())
            .id(id)
            .parameters(Map.of("api_key", "k3y"))
            .build();
    }

    private void authenticate(String login, boolean tenantAdmin) {
        String authority = tenantAdmin ? "ROLE_ADMIN" : "ROLE_USER";

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private void authenticateConnectedUser(ConnectedUser connectedUser) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new EmbeddedApiKeyAuthenticationToken(
                    connectedUser.getEnvironment()
                        .ordinal(),
                    connectedUser.getId(),
                    new org.springframework.security.core.userdetails.User(externalUserId, "", List.of()), false));
    }

    private long createInstance(long connectedUserId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        integrationService.publishIntegration(integration.getId(), null);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(connectedUser.getEnvironment());
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            connectedUserId, INSTANCE_CONNECTION_ID, integrationInstanceConfiguration.getId());

        integrationInstanceService.updateEnabled(integrationInstance.getId(), true);

        return integrationInstance.getId();
    }

    private static Connection connection(long id, PlatformType type, Environment environment) {
        Connection connection = new Connection();

        connection.setEnvironmentId(environment.ordinal());
        connection.setId(id);
        connection.setType(type);

        return connection;
    }

    private static ConnectionDTO connectionDTO(Environment environment) {
        return ConnectionDTO.builder()
            .componentName("slack")
            .environmentId(environment.ordinal())
            .name("connection-" + UUID.randomUUID())
            .build();
    }

    private static long userId(String login) {
        return USER_IDS_BY_LOGIN.get(login);
    }

    @Configuration
    @ComponentScan({
        "com.bytechef.ee.embedded.connected.user.facade", "com.bytechef.ee.embedded.connected.user.security",
        "com.bytechef.ee.embedded.connected.user.service"
    })
    static class ConnectedUserServiceConfiguration {
    }

    @TestConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ApiKeyPermissionScopeProvider.class, ApiPlatformPermissionScopeProvider.class,
        ConnectionEnvironmentResolver.class,
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
        WorkspaceConnectionFacade workspaceConnectionFacade(
            ConnectionFacade connectionFacade, ConnectionService connectionService,
            PermissionService permissionService, ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
            WorkflowTestConfigurationService workflowTestConfigurationService) {

            return new WorkspaceConnectionFacadeImpl(
                connectionFacade, mock(ConnectionLifecycleFacade.class), connectionService, permissionService,
                projectDeploymentWorkflowService, workflowTestConfigurationService,
                mock(WorkspaceConnectionService.class));
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
