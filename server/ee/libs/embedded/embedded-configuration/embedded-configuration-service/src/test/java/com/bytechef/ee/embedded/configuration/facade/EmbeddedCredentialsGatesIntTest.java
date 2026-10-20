/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.bytechef.component.definition.Authorization.AuthorizationType;
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
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
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
import com.bytechef.platform.file.storage.TriggerFileStorage;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
        IntegrationIntTestConfiguration.class, EmbeddedCredentialsGatesIntTest.ConnectedUserServiceConfiguration.class,
        EmbeddedCredentialsGatesIntTest.RealPermissionConfiguration.class
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
class EmbeddedCredentialsGatesIntTest {

    private static final String DEFINITION = """
        {"label":"Sync contacts","inputs":[],"triggers":[],"tasks":[]}
        """;
    private static final String PRODUCTION_EDITOR = "production-editor";
    private static final String TENANT_ADMIN = "tenant-admin";
    private static final String VIEWER = "embedded-viewer";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        TENANT_ADMIN, 9800L, VIEWER, 9801L, PRODUCTION_EDITOR, 9802L);

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectionDefinitionService connectionDefinitionService;

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkflowService workflowService;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private WorkflowValidatorFacade workflowValidatorFacade;

    private long developmentConfigurationId;
    private long productionConfigurationId;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getEnvironment(anyLong()))
            .thenAnswer(invocation -> Environment.values()[((Long) invocation.getArgument(0)).intValue()]);
        when(componentDefinitionService.getComponentDefinition(anyString(), any()))
            .thenReturn(new ComponentDefinition("componentName"));
        when(connectionDefinitionService.getConnectionDefinition(anyString(), any()))
            .thenReturn(
                new ConnectionDefinition(
                    ComponentDsl.connection()
                        .authorizations(ComponentDsl.authorization(AuthorizationType.API_KEY)
                            .properties(ComponentDsl.string("api_key")))
                        .properties(ComponentDsl.string("region")),
                    "componentName", null, null));

        embeddedUserRepository.save(EmbeddedUser.forRole(userId(VIEWER), WorkspaceRole.VIEWER));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(PRODUCTION_EDITOR), WorkspaceRole.EDITOR, Environment.PRODUCTION));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(PRODUCTION_EDITOR), WorkspaceRole.VIEWER, Environment.DEVELOPMENT));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(PRODUCTION_EDITOR), WorkspaceRole.VIEWER, Environment.STAGING));

        permissionService.evictAllEmbeddedScopeCache();

        Integration integration = createPublishedIntegration();

        Workflow workflow = workflowService.create(DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        integrationWorkflowService.addWorkflow(integration.getId(), 1, workflow.getId());

        productionConfigurationId = createConfiguration(integration.getId(), Environment.PRODUCTION);
        developmentConfigurationId = createConfiguration(integration.getId(), Environment.DEVELOPMENT);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            embeddedUserRepository.deleteByUserId(userId);
        }

        permissionService.evictAllEmbeddedScopeCache();

        integrationInstanceConfigurationRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }
    }

    @Test
    void testAViewerGetsNoCredentialsOnAnyRead() {
        authenticate(VIEWER, false);

        assertCredentialsHidden(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(productionConfigurationId));
        assertCredentialsHidden(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(developmentConfigurationId));

        for (boolean includeAllFields : List.of(true, false)) {
            List<IntegrationInstanceConfigurationDTO> integrationInstanceConfigurationDTOs =
                integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurations(
                    null, null, null, includeAllFields);

            assertThat(integrationInstanceConfigurationDTOs).hasSize(2);

            integrationInstanceConfigurationDTOs.forEach(EmbeddedCredentialsGatesIntTest::assertCredentialsHidden);
        }
    }

    @Test
    void testAProductionEditorGetsCredentialsOnlyForProductionConfigurations() {
        authenticate(PRODUCTION_EDITOR, false);

        assertCredentialsVisible(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(productionConfigurationId));
        assertCredentialsHidden(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(developmentConfigurationId));

        List<IntegrationInstanceConfigurationDTO> integrationInstanceConfigurationDTOs =
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurations(null, null, null, true);

        assertThat(integrationInstanceConfigurationDTOs).hasSize(2);

        for (IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO : integrationInstanceConfigurationDTOs) {

            if (integrationInstanceConfigurationDTO.id() == productionConfigurationId) {
                assertCredentialsVisible(integrationInstanceConfigurationDTO);
            } else {
                assertCredentialsHidden(integrationInstanceConfigurationDTO);
            }
        }
    }

    @Test
    void testATenantAdminGetsCredentialsInEveryEnvironment() {
        authenticate(TENANT_ADMIN, true);

        assertCredentialsVisible(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(productionConfigurationId));
        assertCredentialsVisible(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(developmentConfigurationId));

        integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurations(null, null, null, true)
            .forEach(EmbeddedCredentialsGatesIntTest::assertCredentialsVisible);
    }

    private static void assertCredentialsHidden(IntegrationInstanceConfigurationDTO dto) {
        assertThat(dto.connectionAuthorizationParameters()).isEmpty();
        assertThat(dto.connectionConnectionParameters()).isEmpty();
        assertThat(dto.connectionParameters()).isEmpty();
    }

    private static void assertCredentialsVisible(IntegrationInstanceConfigurationDTO dto) {
        assertThat(dto.connectionAuthorizationParameters()
            .get("api_key")).isEqualTo("secret-api-key-12345678");
        assertThat(dto.connectionConnectionParameters()
            .get("region")).isEqualTo("eu");
        assertThat(dto.connectionParameters()
            .get("api_key")).isEqualTo("secret-api-key-12345678");
    }

    private void authenticate(String login, boolean tenantAdmin) {
        String authority = tenantAdmin ? "ROLE_ADMIN" : "ROLE_USER";

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private long createConfiguration(long integrationId, Environment environment) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setAuthorizationType(AuthorizationType.API_KEY);
        integrationInstanceConfiguration.setConnectionParameters(
            Map.of("api_key", "secret-api-key-12345678", "region", "eu", "extra", "x"));
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
        EmbeddedPermissionScopeProvider.class, EmbeddedScopeCacheService.class,
        EmbeddedSharedPermissionScopeProvider.class, ExecutionPermissionScopeProvider.class,
        IntegrationInstanceConfigurationPermissionScopeProvider.class, IntegrationPermissionScopeProvider.class,
        KnowledgeBasePermissionScopeProvider.class, McpPermissionScopeProvider.class, PermissionScopeRegistry.class,
        PermissionServiceImpl.class,
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
