/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
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
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        IntegrationInstanceFacadeIntTest.ConnectedUserServiceConfiguration.class,
        IntegrationInstanceFacadeIntTest.MethodSecurityConfiguration.class
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
    ClusterElementDefinitionService.class, ComponentConnectionFacade.class, ComponentDefinitionFacade.class,
    ComponentDefinitionService.class, ConnectionDefinitionService.class, ConnectionFacade.class,
    ConnectionLifecycleFacade.class, ConnectionService.class, EnvironmentService.class, GitHubProxyClient.class,
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PermissionService.class, PrincipalJobFacade.class, PrincipalJobService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class IntegrationInstanceFacadeIntTest {

    private static final String WORKFLOW_DEFINITION = """
        {"label":"Sync contacts","inputs":[],"triggers":[],"tasks":[]}
        """;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationInstanceFacade integrationInstanceFacade;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkflowService workflowService;

    private ConnectedUser connectedUserA;
    private ConnectedUser developmentConnectedUserA;
    private String externalUserAId;
    private long integrationInstanceAId;
    private long integrationInstanceBId;
    private String workflowId;

    @BeforeEach
    void beforeEach() {
        externalUserAId = "external-user-a-" + UUID.randomUUID();

        connectedUserA = connectedUserService.createConnectedUser(externalUserAId, Environment.PRODUCTION);
        developmentConnectedUserA = connectedUserService.createConnectedUser(
            externalUserAId, Environment.DEVELOPMENT);

        ConnectedUser connectedUserB = connectedUserService.createConnectedUser(
            "external-user-b-" + UUID.randomUUID(), Environment.PRODUCTION);

        Integration integration = new Integration();

        integration.setComponentName("slack");
        integration.setName("Slack " + UUID.randomUUID());

        integration = integrationService.create(integration);

        Workflow workflow = workflowService.create(WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        workflowId = workflow.getId();

        integrationWorkflowService.addWorkflow(integration.getId(), 1, workflowId);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("Slack");

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfiguration.getId(), true);

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfiguration.getId());
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

        IntegrationInstance integrationInstanceA = integrationInstanceService.create(
            connectedUserA.getId(), 41L, integrationInstanceConfiguration.getId());

        integrationInstanceAId = integrationInstanceA.getId();

        IntegrationInstance integrationInstanceB = integrationInstanceService.create(
            connectedUserB.getId(), 42L, integrationInstanceConfiguration.getId());

        integrationInstanceBId = integrationInstanceB.getId();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDeniesConnectedUserOnAnotherUsersInstance() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

        assertThatThrownBy(() -> integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            integrationInstanceBId, workflowId, true))
                .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceBId)).isEmpty();
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDeniesConnectedUserFromAnotherEnvironment() {
        authenticate(createConnectedUserAuthentication(Environment.DEVELOPMENT, developmentConnectedUserA));

        assertThatThrownBy(() -> integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            integrationInstanceAId, workflowId, true))
                .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceAId)).isEmpty();
    }

    @Test
    void testEnableIntegrationInstanceWorkflowAllowsConnectedUserOnOwnInstance() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(integrationInstanceAId, workflowId, true);

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceAId))
            .hasValueSatisfying(integrationInstanceWorkflow -> assertThat(integrationInstanceWorkflow.isEnabled())
                .isTrue());
    }

    @Test
    void testEnableIntegrationInstanceWorkflowAllowsTenantAdminOnAnyInstance() {
        authenticate(createTenantAdminAuthentication());

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(integrationInstanceBId, workflowId, true);

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceBId))
            .hasValueSatisfying(integrationInstanceWorkflow -> assertThat(integrationInstanceWorkflow.isEnabled())
                .isTrue());
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDeniesCallerWhoIsNeitherTenantAdminNorConnectedUser() {
        authenticate(
            new UsernamePasswordAuthenticationToken(
                "user@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThatThrownBy(() -> integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            integrationInstanceAId, workflowId, true))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceAId)).isEmpty();
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowDeniesConnectedUserOnAnotherUsersInstance() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

        assertThatThrownBy(() -> integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            integrationInstanceBId, workflowId, Map.of("key", "value")))
                .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceBId)).isEmpty();
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowAllowsConnectedUserOnOwnInstance() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

        integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            integrationInstanceAId, workflowId, Map.of("key", "value"));

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceAId))
            .hasValueSatisfying(integrationInstanceWorkflow -> assertThat(integrationInstanceWorkflow.getInputs())
                .isEqualTo(Map.of("key", "value")));
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowAllowsTenantAdminOnAnyInstance() {
        authenticate(createTenantAdminAuthentication());

        integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            integrationInstanceBId, workflowId, Map.of("key", "value"));

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceBId))
            .hasValueSatisfying(integrationInstanceWorkflow -> assertThat(integrationInstanceWorkflow.getInputs())
                .isEqualTo(Map.of("key", "value")));
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private EmbeddedApiKeyAuthenticationToken createConnectedUserAuthentication(
        Environment environment, ConnectedUser connectedUser) {

        return new EmbeddedApiKeyAuthenticationToken(
            environment.ordinal(), connectedUser.getId(), new User(externalUserAId, "", List.of()), false);
    }

    private Authentication createTenantAdminAuthentication() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        return new UsernamePasswordAuthenticationToken(
            "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private Optional<IntegrationInstanceWorkflow> fetchIntegrationInstanceWorkflow(long integrationInstanceId) {
        return integrationInstanceWorkflowService.fetchIntegrationInstanceWorkflow(integrationInstanceId, workflowId);
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService) {

            return new AutomationMethodSecurityExpressionHandler(permissionService);
        }
    }
}
