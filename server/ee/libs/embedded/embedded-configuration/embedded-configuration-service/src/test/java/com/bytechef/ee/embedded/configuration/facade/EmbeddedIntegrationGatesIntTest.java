/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
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
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
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
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
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
import java.util.List;
import java.util.Map;
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
import org.springframework.context.annotation.Import;
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
        IntegrationIntTestConfiguration.class, EmbeddedIntegrationGatesIntTest.RealPermissionConfiguration.class
    },
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, ClusterElementDefinitionService.class,
    ComponentConnectionFacade.class, ComponentDefinitionFacade.class, ComponentDefinitionService.class,
    ConnectedUserService.class, ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionService.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class, JobFacade.class, JobService.class,
    McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectDeploymentFacade.class,
    ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class, ProjectFacade.class, ProjectService.class,
    ProjectWorkflowFacade.class, ProjectWorkflowService.class, TaskDispatcherDefinitionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, WebhookTriggerTestFacade.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class,
    WorkflowTestConfigurationService.class
})
class EmbeddedIntegrationGatesIntTest {

    private static final String DEFINITION = """
        {"label":"original","triggers":[],"tasks":[]}
        """;
    private static final String EDITOR_IN_DEVELOPMENT = "editor-in-development";
    private static final String EDITOR_IN_PRODUCTION = "editor-in-production";
    private static final String EMBEDDED_ADMIN = "embedded-admin";
    private static final String EMBEDDED_VIEWER = "embedded-viewer";
    private static final String NON_MEMBER = "non-member";
    private static final String TENANT_ADMIN = "tenant-admin";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        TENANT_ADMIN, 9500L, EMBEDDED_VIEWER, 9501L, EDITOR_IN_PRODUCTION, 9502L, EDITOR_IN_DEVELOPMENT, 9503L,
        EMBEDDED_ADMIN, 9504L, NON_MEMBER, 9505L);

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Autowired
    private IntegrationFacade integrationFacade;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowFacade integrationWorkflowFacade;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WebhookTriggerTestAdminFacade webhookTriggerTestAdminFacade;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private WorkflowValidatorFacade workflowValidatorFacade;

    @BeforeEach
    void beforeEach() {
        when(workflowValidatorFacade.getReservedNodeNames(anyString()))
            .thenAnswer(invocation -> WorkflowValidator.getReservedNodeNames(invocation.getArgument(0)));

        embeddedUserRepository.save(EmbeddedUser.forRole(userId(EMBEDDED_VIEWER), WorkspaceRole.VIEWER));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(EDITOR_IN_PRODUCTION), WorkspaceRole.EDITOR, Environment.PRODUCTION));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(EDITOR_IN_DEVELOPMENT), WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId(EMBEDDED_ADMIN), WorkspaceRole.ADMIN));

        permissionService.evictAllEmbeddedScopeCache();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            embeddedUserRepository.deleteByUserId(userId);
        }

        permissionService.evictAllEmbeddedScopeCache();

        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }
    }

    @Test
    void testAViewerReadsButCannotCreate() {
        long integrationId = createIntegration();

        authenticate(EMBEDDED_VIEWER, false);

        assertThat(integrationFacade.getIntegration(integrationId)).isNotNull();
        assertThat(integrationFacade.getIntegrations(null, false, null, null, false)).isNotEmpty();

        IntegrationDTO integrationDTO = IntegrationDTO.builder()
            .componentName("componentName")
            .name("created-by-a-viewer")
            .build();

        assertThatThrownBy(() -> integrationFacade.createIntegration(integrationDTO))
            .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> integrationFacade.deleteIntegration(integrationId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(integrationRepository.findAll()).hasSize(1);
    }

    @Test
    void testAnEditorBuildsWorkflowsOnlyWithADevelopmentRole() {
        long integrationId = createIntegration();

        authenticate(TENANT_ADMIN, true);

        long integrationWorkflowId = integrationWorkflowFacade.addWorkflow(integrationId, DEFINITION);

        IntegrationWorkflow integrationWorkflow = integrationWorkflowRepository.findById(integrationWorkflowId)
            .orElseThrow();

        String workflowId = integrationWorkflow.getWorkflowId();

        Workflow workflow = workflowCrudRepository.findById(workflowId)
            .orElseThrow();

        authenticate(EDITOR_IN_PRODUCTION, false);

        assertThatThrownBy(
            () -> integrationWorkflowFacade.updateWorkflow(workflowId, DEFINITION, workflow.getVersion()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationWorkflowFacade.addWorkflow(integrationId, DEFINITION))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(EDITOR_IN_DEVELOPMENT, false);

        integrationWorkflowFacade.updateWorkflow(workflowId, DEFINITION, workflow.getVersion());

        assertThat(integrationWorkflowFacade.addWorkflow(integrationId, DEFINITION)).isPositive();
    }

    @Test
    void testOnlyAnAdminChangesPermissionExpressions() {
        long integrationId = createIntegration();

        authenticate(TENANT_ADMIN, true);

        long integrationWorkflowId = integrationWorkflowFacade.addWorkflow(integrationId, DEFINITION);

        authenticate(EDITOR_IN_DEVELOPMENT, false);

        assertThatThrownBy(() -> integrationWorkflowFacade.updatePermissionExpression(integrationWorkflowId, "true"))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(EMBEDDED_ADMIN, false);

        integrationWorkflowFacade.updatePermissionExpression(integrationWorkflowId, "true");

        IntegrationWorkflow integrationWorkflow = integrationWorkflowRepository.findById(integrationWorkflowId)
            .orElseThrow();

        assertThat(integrationWorkflow.getPermissionExpression()).isEqualTo("true");
    }

    @Test
    void testATenantAdminWithoutARowStillPasses() {
        long integrationId = createIntegration();

        authenticate(TENANT_ADMIN, true);

        assertThat(integrationFacade.getIntegration(integrationId)).isNotNull();

        long integrationWorkflowId = integrationWorkflowFacade.addWorkflow(integrationId, DEFINITION);

        integrationWorkflowFacade.updatePermissionExpression(integrationWorkflowId, "true");

        assertThat(integrationFacade.createIntegration(
            IntegrationDTO.builder()
                .componentName("componentName")
                .name("created-by-a-tenant-admin")
                .build())).isPositive();
    }

    @Test
    void testANonMemberIsDenied() {
        long integrationId = createIntegration();

        authenticate(NON_MEMBER, false);

        assertThatThrownBy(() -> integrationFacade.getIntegration(integrationId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationFacade.getIntegrations(null, false, null, null, false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationWorkflowFacade.getIntegrationWorkflows())
            .isInstanceOf(AccessDeniedException.class);

        IntegrationDTO integrationDTO = IntegrationDTO.builder()
            .componentName("componentName")
            .name("created-by-a-non-member")
            .build();

        assertThatThrownBy(() -> integrationFacade.createIntegration(integrationDTO))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationFacade.deleteIntegration(integrationId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationWorkflowFacade.updateWorkflow("workflow-1", DEFINITION, 0))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> integrationWorkflowFacade.updatePermissionExpression(1L, "true"))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(integrationRepository.findAll()).hasSize(1);
    }

    @Test
    void testADevelopmentEditorStartsAWebhookTestOnlyInDevelopment() {
        long integrationId = createIntegration();

        authenticate(TENANT_ADMIN, true);

        long integrationWorkflowId = integrationWorkflowFacade.addWorkflow(integrationId, DEFINITION);

        IntegrationWorkflow integrationWorkflow = integrationWorkflowRepository.findById(integrationWorkflowId)
            .orElseThrow();

        String workflowId = integrationWorkflow.getWorkflowId();

        authenticate(EDITOR_IN_DEVELOPMENT, false);

        assertThatThrownBy(
            () -> webhookTriggerTestAdminFacade.startWebhookTriggerTest(
                workflowId, "trigger_1", Environment.PRODUCTION.ordinal()))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> webhookTriggerTestAdminFacade.stopWebhookTriggerTest(
                workflowId, "trigger_1", Environment.PRODUCTION.ordinal()))
                    .isInstanceOf(AccessDeniedException.class);

        webhookTriggerTestAdminFacade.startWebhookTriggerTest(
            workflowId, "trigger_1", Environment.DEVELOPMENT.ordinal());
        webhookTriggerTestAdminFacade.stopWebhookTriggerTest(
            workflowId, "trigger_1", Environment.DEVELOPMENT.ordinal());
    }

    private long createIntegration() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        return integration.getId();
    }

    private static long userId(String login) {
        return USER_IDS_BY_LOGIN.get(login);
    }

    private static void authenticate(String login, boolean tenantAdmin) {
        String authority = tenantAdmin ? "ROLE_ADMIN" : "ROLE_USER";

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
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
