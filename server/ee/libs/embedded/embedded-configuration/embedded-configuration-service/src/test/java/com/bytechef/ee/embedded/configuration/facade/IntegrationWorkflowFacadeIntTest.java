/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.exception.ConfigurationException;
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
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class, IntegrationWorkflowFacadeIntTest.MethodSecurityConfiguration.class
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
class IntegrationWorkflowFacadeIntTest {

    private static final String INPUT_NAMED_VARS_DEFINITION = """
        {"label":"t","inputs":[{"name":"vars","type":"STRING"}],"triggers":[],"tasks":[]}
        """;

    private static final String INPUT_NAMED_VARS_COUNT_DEFINITION = """
        {"label":"t","inputs":[{"name":"varsCount","type":"STRING"}],"triggers":[],"tasks":[]}
        """;

    private static final String NODE_NAMED_VARS_DEFINITION = """
        {"label":"t","triggers":[],"tasks":[{"name":"vars","type":"logger/v1/info"}]}
        """;

    private static final String ORIGINAL_DEFINITION = """
        {"label":"original","triggers":[],"tasks":[]}
        """;

    private static final String UPDATED_DEFINITION = """
        {"label":"updated","triggers":[],"tasks":[]}
        """;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowFacade integrationWorkflowFacade;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private WorkflowValidatorFacade workflowValidatorFacade;

    @BeforeEach
    void beforeEach() {
        authenticate(tenantAdmin());
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }
    }

    @Test
    void testAddWorkflowRejectsInputNamedVars() {
        long integrationId = createIntegration();
        int workflowCount = getWorkflowCount();

        assertThatThrownBy(() -> integrationWorkflowFacade.addWorkflow(integrationId, INPUT_NAMED_VARS_DEFINITION))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Reserved input names: vars");

        assertThat(integrationWorkflowRepository.findAllByIntegrationId(integrationId)).isEmpty();
        assertThat(getWorkflowCount()).isEqualTo(workflowCount);
    }

    @Test
    void testAddWorkflowRejectsNodeNamedVars() {
        long integrationId = createIntegration();
        int workflowCount = getWorkflowCount();

        assertThatThrownBy(() -> integrationWorkflowFacade.addWorkflow(integrationId, NODE_NAMED_VARS_DEFINITION))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Reserved node names: vars");

        assertThat(integrationWorkflowRepository.findAllByIntegrationId(integrationId)).isEmpty();
        assertThat(getWorkflowCount()).isEqualTo(workflowCount);
    }

    @Test
    void testAddWorkflowAcceptsInputNamedVarsCount() {
        long integrationId = createIntegration();

        long integrationWorkflowId = integrationWorkflowFacade.addWorkflow(
            integrationId, INPUT_NAMED_VARS_COUNT_DEFINITION);

        IntegrationWorkflow integrationWorkflow = integrationWorkflowRepository.findById(integrationWorkflowId)
            .orElseThrow();

        assertThat(integrationWorkflow.getIntegrationId()).isEqualTo(integrationId);
        assertThat(getStoredDefinition(integrationWorkflow.getWorkflowId()))
            .isEqualTo(INPUT_NAMED_VARS_COUNT_DEFINITION);
    }

    @Test
    void testAddWorkflowRequiresTenantAdmin() {
        long integrationId = createIntegration();
        int workflowCount = getWorkflowCount();

        for (Authentication authentication : List.of(nonAdmin(), connectedUser())) {
            authenticate(authentication);

            assertThatThrownBy(() -> integrationWorkflowFacade.addWorkflow(integrationId, ORIGINAL_DEFINITION))
                .as("denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
        }

        assertThat(integrationWorkflowRepository.findAllByIntegrationId(integrationId)).isEmpty();
        assertThat(getWorkflowCount()).isEqualTo(workflowCount);
    }

    @Test
    void testUpdateWorkflowRejectsInputNamedVars() {
        Workflow workflow = addOriginalWorkflow();

        assertThatThrownBy(
            () -> integrationWorkflowFacade.updateWorkflow(
                workflow.getId(), INPUT_NAMED_VARS_DEFINITION, workflow.getVersion()))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Reserved input names: vars");

        assertThat(getStoredDefinition(workflow.getId())).isEqualTo(ORIGINAL_DEFINITION);
    }

    @Test
    void testUpdateWorkflowRejectsNodeNamedVars() {
        Workflow workflow = addOriginalWorkflow();

        assertThatThrownBy(
            () -> integrationWorkflowFacade.updateWorkflow(
                workflow.getId(), NODE_NAMED_VARS_DEFINITION, workflow.getVersion()))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Reserved node names: vars");

        assertThat(getStoredDefinition(workflow.getId())).isEqualTo(ORIGINAL_DEFINITION);
    }

    @Test
    void testUpdateWorkflowRequiresTenantAdmin() {
        Workflow workflow = addOriginalWorkflow();

        for (Authentication authentication : List.of(nonAdmin(), connectedUser())) {
            authenticate(authentication);

            assertThatThrownBy(
                () -> integrationWorkflowFacade.updateWorkflow(
                    workflow.getId(), UPDATED_DEFINITION, workflow.getVersion()))
                        .as("denied to %s", authentication.getName())
                        .isInstanceOf(AccessDeniedException.class);
        }

        assertThat(getStoredDefinition(workflow.getId())).isEqualTo(ORIGINAL_DEFINITION);

        authenticate(tenantAdmin());

        integrationWorkflowFacade.updateWorkflow(workflow.getId(), UPDATED_DEFINITION, workflow.getVersion());

        assertThat(getStoredDefinition(workflow.getId())).isEqualTo(UPDATED_DEFINITION);
    }

    private Workflow addOriginalWorkflow() {
        long integrationWorkflowId = integrationWorkflowFacade.addWorkflow(createIntegration(), ORIGINAL_DEFINITION);

        IntegrationWorkflow integrationWorkflow = integrationWorkflowRepository.findById(integrationWorkflowId)
            .orElseThrow();

        return workflowCrudRepository.findById(integrationWorkflow.getWorkflowId())
            .orElseThrow();
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    private static Authentication connectedUser() {
        return new EmbeddedApiKeyAuthenticationToken(
            Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false);
    }

    private long createIntegration() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        return integration.getId();
    }

    private String getStoredDefinition(String workflowId) {
        Workflow workflow = workflowCrudRepository.findById(workflowId)
            .orElseThrow();

        return workflow.getDefinition();
    }

    private int getWorkflowCount() {
        List<Workflow> workflows = workflowCrudRepository.findAll();

        return workflows.size();
    }

    private static Authentication nonAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "user", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    private static Authentication tenantAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "admin", "n/a",
            List.of(
                new SimpleGrantedAuthority(AuthorityConstants.ADMIN),
                new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class MethodSecurityConfiguration {

        @Bean("permissionService")
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.isTenantAdmin())
                .thenAnswer(invocation -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));

            return permissionService;
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
