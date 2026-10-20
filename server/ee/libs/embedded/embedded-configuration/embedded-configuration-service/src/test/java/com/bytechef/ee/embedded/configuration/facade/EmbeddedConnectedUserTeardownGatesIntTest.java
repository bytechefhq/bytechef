/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
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
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectPublishedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectUserProjectRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.facade.ConnectedUserFacade;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
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
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
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
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        EmbeddedConnectedUserTeardownGatesIntTest.ConnectedUserConfiguration.class,
        EmbeddedConnectedUserTeardownGatesIntTest.RealPermissionConfiguration.class
    },
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    ClusterElementDefinitionService.class, ComponentConnectionFacade.class, ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionLifecycleFacade.class,
    ConnectionService.class, EmbeddedPermissionEvaluator.class, EnvironmentService.class, GitHubProxyClient.class,
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, TaskDispatcherDefinitionService.class,
    TaskExecutionService.class, TriggerDefinitionFacade.class, TriggerDefinitionService.class,
    TriggerExecutionService.class, TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class,
    WorkflowTestConfigurationService.class, WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class EmbeddedConnectedUserTeardownGatesIntTest {

    private static final String ADMIN_IN_PRODUCTION = "teardown-admin-in-production";
    private static final long ENVIRONMENT_ID = Environment.PRODUCTION.ordinal();
    private static final long INSTANCE_CONNECTION_ID = 9202L;
    private static final long OWN_CONNECTION_ID = 9201L;
    private static final String NON_MEMBER = "teardown-non-member";
    private static final String TENANT_ADMIN = "teardown-tenant-admin";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        TENANT_ADMIN, 9800L, ADMIN_IN_PRODUCTION, 9801L, NON_MEMBER, 9802L);

    private static final String SLACK_TRIGGER_WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[{"name":"newMessage1","type":"slack/v1/newMessage","parameters":{}}],
         "tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @MockitoBean
    private AutomationWorkflowProjectPublishedEventListener automationWorkflowProjectPublishedEventListener;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectUserProjectRepository connectUserProjectRepository;

    @Autowired
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserFacade connectedUserFacade;

    @Autowired
    private ConnectedUserProjectAdminFacade connectedUserProjectAdminFacade;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager;

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private long automationWorkflowProjectId;
    private long connectedUserId;
    private ConnectedUserProjectWorkflow draftCopy;
    private ConnectedUserProjectWorkflow enabledCopy;
    private String enabledCopyWorkflowId;
    private String externalUserId;
    private ConnectedUserProjectWorkflow reference;

    @BeforeEach
    void beforeEach() {
        for (Map.Entry<String, Long> entry : USER_IDS_BY_LOGIN.entrySet()) {
            User user = new User();

            user.setId(entry.getValue());
            user.setLogin(entry.getKey());

            when(userService.fetchUserByLogin(entry.getKey())).thenReturn(Optional.of(user));
            when(userService.getUser(entry.getKey())).thenReturn(user);
            when(userService.getUser(entry.getValue())).thenReturn(user);
        }

        embeddedUserRepository.save(
            EmbeddedUser.forRole(USER_IDS_BY_LOGIN.get(ADMIN_IN_PRODUCTION), WorkspaceRole.ADMIN,
                Environment.PRODUCTION));

        permissionService.evictAllEmbeddedScopeCache();

        stubCollaborators();

        externalUserId = "teardown-user-" + UUID.randomUUID();

        authenticate(TENANT_ADMIN, true);

        ConnectedUser connectedUser = connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        connectedUserId = connectedUser.getId();

        connectedUserConnectionFacade.createConnectedUserConnection(
            connectedUserId, ConnectionDTO.builder()
                .componentName("slack")
                .name("Slack")
                .build());

        automationWorkflowProjectId = automationWorkflowProjectFacade.createProject(
            "Teardown " + UUID.randomUUID(), "", null, List.of(), null, null);

        String automationWorkflowUuid = automationWorkflowProjectFacade.createProjectWorkflow(
            automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION, null);

        String secondAutomationWorkflowUuid = automationWorkflowProjectFacade.createProjectWorkflow(
            automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION, null);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationWorkflowUuid, Environment.PRODUCTION);

        String enabledCopyUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            externalUserId, automationWorkflowUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.publishProjectWorkflow(externalUserId, enabledCopyUuid, "v1", ENVIRONMENT_ID);
        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, enabledCopyUuid, true, ENVIRONMENT_ID);

        enabledCopyWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(enabledCopyUuid);

        connectedUserProjectFacade.copyWorkflowTemplate(
            externalUserId, secondAutomationWorkflowUuid, Environment.PRODUCTION);

        List<ConnectedUserProjectWorkflow> copies = connectedUserProjectWorkflowRepository
            .findAllByConnectedUserId(connectedUserId)
            .stream()
            .filter(row -> row.getAutomationWorkflowUuid() == null)
            .sorted(java.util.Comparator.comparing(ConnectedUserProjectWorkflow::getId))
            .toList();

        assertThat(copies).hasSize(2);

        enabledCopy = copies.get(0);
        draftCopy = copies.get(1);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            embeddedUserRepository.deleteByUserId(userId);
        }

        permissionService.evictAllEmbeddedScopeCache();
    }

    @Test
    void testAProductionAdminTearsDownAConnectedUserThroughTheRealAutomationGates() {
        authenticate(ADMIN_IN_PRODUCTION, false);

        assertThatThrownBy(() -> projectWorkflowFacade.deleteWorkflow(enabledCopyWorkflowId))
            .isInstanceOf(AccessDeniedException.class);

        long copyProjectId = connectUserProjectRepository.findByConnectedUserId(connectedUserId)
            .orElseThrow()
            .getProjectId();

        assertThatCode(() -> connectedUserProjectAdminFacade.enableProjectWorkflow(enabledCopy.getId(), false))
            .doesNotThrowAnyException();

        connectedUserProjectAdminFacade.deleteProjectWorkflow(draftCopy.getId());

        assertThat(connectedUserProjectWorkflowRepository.findById(draftCopy.getId())).isEmpty();

        connectedUserFacade.deleteConnectedUser(connectedUserId);

        assertThat(connectedUserService.fetchConnectedUser(connectedUserId)).isEmpty();
        assertThat(connectedUserProjectWorkflowRepository.findAllByConnectedUserId(connectedUserId)).isEmpty();
        assertThat(connectUserProjectRepository.findByConnectedUserId(connectedUserId)).isEmpty();
        assertThat(projectService.fetchProject(copyProjectId)).isEmpty();
    }

    @Test
    void testANonMemberCannotEnableOrDeleteARealProjectWorkflow() {
        authenticate(NON_MEMBER, false);

        assertThatThrownBy(() -> connectedUserProjectAdminFacade.enableProjectWorkflow(enabledCopy.getId(), false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserProjectAdminFacade.deleteProjectWorkflow(enabledCopy.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserProjectAdminFacade.deleteProjectWorkflow(draftCopy.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectedUserFacade.deleteConnectedUser(connectedUserId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(connectedUserProjectWorkflowRepository.findById(enabledCopy.getId()))
            .hasValueSatisfying(row -> assertThat(row.isEnabled()).isTrue());
        assertThat(connectedUserProjectWorkflowRepository.findById(draftCopy.getId())).isPresent();
        assertThat(connectedUserService.fetchConnectedUser(connectedUserId)).isPresent();
    }

    @Test
    void testARolloutOnAThreadWithoutAnAuthenticationStillMovesTheReferenceForward() {
        int versionBefore = projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion();

        authenticate(TENANT_ADMIN, true);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        SecurityContextHolder.clearContext();

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId);

        assertThat(
            projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
                .getProjectVersion()).isGreaterThan(versionBefore);
    }

    private void authenticate(String login, boolean tenantAdmin) {
        String authority = tenantAdmin ? "ROLE_ADMIN" : "ROLE_USER";

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private void stubCollaborators() {
        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setId(OWN_CONNECTION_ID);
        connection.setType(PlatformType.EMBEDDED);

        ComponentConnection slot = new ComponentConnection("slack", 1, "postMessage1", "slack", true);

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class))).thenReturn(List.of(slot));
        when(componentConnectionFacade.getComponentConnection(anyString(), eq("postMessage1"), eq("slack")))
            .thenReturn(slot);
        when(componentDefinitionService.getComponentDefinition(anyString(), anyInt()))
            .thenReturn(new ComponentDefinition("slack"));
        when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED)))
            .thenReturn(OWN_CONNECTION_ID);
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED)))
            .thenReturn(List.of(
                ConnectionDTO.builder()
                    .componentName("slack")
                    .id(OWN_CONNECTION_ID)
                    .build()));
        when(connectionService.getConnection(OWN_CONNECTION_ID)).thenReturn(connection);
        when(connectionService.getConnections(anyList())).thenReturn(List.of(connection));
        when(connectionService.getConnections(PlatformType.EMBEDDED)).thenReturn(List.of());
        when(embeddedPermissionEvaluator.evaluate(any(), any())).thenReturn(true);
        when(environmentService.getEnvironment(ENVIRONMENT_ID)).thenReturn(Environment.PRODUCTION);
        when(principalJobService.getJobIds(any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
            .thenReturn(Page.empty());

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

        workflowTestConfiguration.setConnections(
            List.of(new WorkflowTestConfigurationConnection(OWN_CONNECTION_ID, "slack", "postMessage1")));

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(anyString(), eq(ENVIRONMENT_ID)))
            .thenReturn(Optional.of(workflowTestConfiguration));
    }

    @Configuration
    @ComponentScan({
        "com.bytechef.ee.embedded.connected.user.facade", "com.bytechef.ee.embedded.connected.user.security",
        "com.bytechef.ee.embedded.connected.user.service"
    })
    static class ConnectedUserConfiguration {
    }

    @TestConfiguration
    @EnableMethodSecurity
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
        @Primary
        PermissionService permissionService(PermissionServiceImpl permissionServiceImpl) {
            return permissionServiceImpl;
        }

        @Bean
        static RoleHierarchy roleHierarchy() {
            return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN")
                .implies("USER")
                .build();
        }

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService, RoleHierarchy roleHierarchy) {

            AutomationMethodSecurityExpressionHandler handler =
                new AutomationMethodSecurityExpressionHandler(permissionService);

            handler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));
            handler.setRoleHierarchy(roleHierarchy);

            return handler;
        }
    }
}
