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
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.audit.AutomationWorkflowProjectAuditEvents;
import com.bytechef.ee.embedded.configuration.audit.WorkflowAuditEvents;
import com.bytechef.ee.embedded.configuration.dto.ProvisionConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectService;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
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
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        AutomationWorkflowProjectAdminFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    ClusterElementDefinitionService.class, TriggerDefinitionFacade.class,
    ComponentConnectionFacade.class,
    ComponentDefinitionService.class, ConnectionDefinitionService.class,
    ConnectionFacade.class, ConnectionLifecycleFacade.class, ConnectionService.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class,
    GitHubProxyClient.class, JobFacade.class, JobService.class, McpComponentService.class,
    McpIntegrationComponentService.class,
    McpIntegrationInstanceConfigurationService.class, McpIntegrationInstanceConfigurationWorkflowService.class,
    McpIntegrationInstanceToolService.class, McpServerService.class, McpToolService.class,
    OAuth2ParametersFacade.class,
    OAuth2Service.class, PrincipalJobFacade.class, PrincipalJobService.class, ProjectDeploymentFacade.class,
    ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class, TriggerDefinitionService.class,
    TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class,
    WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class AutomationWorkflowProjectAdminFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final String PERMISSION_EXPRESSION = "metadata['tier'] == 'pro'";

    @Autowired
    private AutomationWorkflowProjectAdminFacade automationWorkflowProjectAdminFacade;

    @Autowired
    private AutomationWorkflowProjectConfigurationAdminFacade automationWorkflowProjectConfigurationAdminFacade;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private AutomationWorkflowProjectService automationWorkflowProjectService;

    @Autowired
    private ConnectedUserProjectAdminFacade connectedUserProjectAdminFacade;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.setContext(
            new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(
                    ADMIN_LOGIN, "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN)))));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testUpdateProjectWorkflowPermissionExpressionRecordsTheWorkflowAndTheExpression() {
        long projectId = automationWorkflowProjectFacade.createProject(
            "audit-" + UUID.randomUUID(), "", null, List.of(), null);

        Project project = projectService.getProject(projectId);

        Workflow workflow = workflowService.create(
            "{\"label\": \"New Workflow\", \"tasks\": []}", Workflow.Format.JSON, Workflow.SourceType.JDBC);

        ProjectWorkflow projectWorkflow = projectWorkflowService.addWorkflow(
            projectId, project.getLastProjectVersion(), workflow.getId());

        UUID projectWorkflowUuid = projectWorkflow.getUuid();

        String workflowUuid = projectWorkflowUuid.toString();

        automationWorkflowProjectAdminFacade.updateProjectWorkflowPermissionExpression(
            workflowUuid, PERMISSION_EXPRESSION);

        assertThat(automationWorkflowProjectService.getWorkflowPermissionExpressions(projectId))
            .containsEntry(projectWorkflowUuid, PERMISSION_EXPRESSION);

        assertThat(findSingleEvent(WorkflowAuditEvents.AUTOMATION_WORKFLOW_PERMISSION_EXPRESSION_UPDATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workflowUuid", workflowUuid)
            .containsEntry("permissionExpression", PERMISSION_EXPRESSION);
    }

    @Test
    void testUpdateProjectWorkflowPermissionExpressionOfAnUnknownWorkflowRecordsAFailure() {
        String workflowUuid = String.valueOf(UUID.randomUUID());

        assertThatThrownBy(
            () -> automationWorkflowProjectAdminFacade.updateProjectWorkflowPermissionExpression(
                workflowUuid, PERMISSION_EXPRESSION))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No workflow found for workflow uuid " + workflowUuid);

        assertThat(findSingleEvent(WorkflowAuditEvents.AUTOMATION_WORKFLOW_PERMISSION_EXPRESSION_UPDATED).getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("workflowUuid", workflowUuid)
            .containsEntry("permissionExpression", PERMISSION_EXPRESSION);
    }

    @Test
    void testProjectLifecycleRecordsEveryChange() {
        String name = "audit-" + UUID.randomUUID();

        long projectId = automationWorkflowProjectAdminFacade.createProject(
            name, "", null, List.of(), PERMISSION_EXPRESSION);

        assertThat(findSingleEvent(AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CREATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("projectId", String.valueOf(projectId))
            .containsEntry("name", name)
            .containsEntry("permissionExpression", PERMISSION_EXPRESSION);

        automationWorkflowProjectAdminFacade.updateProject(projectId, "renamed", "", null, List.of(), null);

        assertThat(findSingleEvent(AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_UPDATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("projectId", String.valueOf(projectId))
            .containsEntry("name", "renamed");

        String workflowUuid = automationWorkflowProjectAdminFacade.createProjectWorkflow(projectId, null, null);

        assertThat(findSingleEvent(AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_CREATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("projectId", String.valueOf(projectId))
            .containsEntry("workflowUuid", workflowUuid);

        String duplicatedWorkflowUuid = automationWorkflowProjectAdminFacade.duplicateProjectWorkflow(workflowUuid);

        assertThat(findSingleEvent(AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_DUPLICATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("sourceWorkflowUuid", workflowUuid)
            .containsEntry("workflowUuid", duplicatedWorkflowUuid);

        automationWorkflowProjectAdminFacade.deleteProjectWorkflow(duplicatedWorkflowUuid);

        assertThat(findSingleEvent(AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_DELETED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workflowUuid", duplicatedWorkflowUuid);

        int projectVersion = automationWorkflowProjectAdminFacade.publishProject(projectId, null);

        assertThat(
            findSingleEvent(AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_PUBLISHED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("projectId", String.valueOf(projectId))
                .containsEntry("projectVersion", String.valueOf(projectVersion));

        long duplicatedProjectId = automationWorkflowProjectAdminFacade.duplicateProject(projectId);

        assertThat(
            findSingleEvent(AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_DUPLICATED).getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("sourceProjectId", String.valueOf(projectId))
                .containsEntry("projectId", String.valueOf(duplicatedProjectId));

        automationWorkflowProjectAdminFacade.deleteProject(duplicatedProjectId);

        assertThat(findSingleEvent(AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_DELETED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("projectId", String.valueOf(duplicatedProjectId));
    }

    @Test
    void testConfigurationLifecycleRecordsEveryChange() {
        long projectId = automationWorkflowProjectFacade.createProject(
            "audit-" + UUID.randomUUID(), "", null, List.of(), null);

        String workflowUuid = automationWorkflowProjectFacade.createProjectWorkflow(projectId, null, null);

        automationWorkflowProjectFacade.publishProject(projectId);

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, 0L, 1, true, false);

        assertThat(
            findSingleEvent(AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CONFIGURATION_CREATED)
                .getData())
                    .containsEntry("result", "SUCCESS")
                    .containsEntry("configurationId", String.valueOf(configurationId))
                    .containsEntry("projectId", String.valueOf(projectId))
                    .containsEntry("environmentId", "0")
                    .containsEntry("projectVersion", "1")
                    .containsEntry("automationHubVisible", "false");

        automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(configurationId, 1, true, true);

        assertThat(
            findSingleEvent(AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CONFIGURATION_UPDATED)
                .getData())
                    .containsEntry("result", "SUCCESS")
                    .containsEntry("configurationId", String.valueOf(configurationId))
                    .containsEntry("automationHubVisible", "true");

        automationWorkflowProjectConfigurationAdminFacade.enableConfigurationWorkflow(
            configurationId, workflowUuid, false);

        assertThat(
            findSingleEvent(
                AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CONFIGURATION_WORKFLOW_ENABLED_UPDATED)
                    .getData())
                        .containsEntry("result", "SUCCESS")
                        .containsEntry("configurationId", String.valueOf(configurationId))
                        .containsEntry("workflowUuid", workflowUuid)
                        .containsEntry("enabled", "false");

        automationWorkflowProjectConfigurationAdminFacade.enableConfiguration(configurationId, false);

        assertThat(
            findSingleEvent(
                AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CONFIGURATION_ENABLED_UPDATED)
                    .getData())
                        .containsEntry("result", "SUCCESS")
                        .containsEntry("configurationId", String.valueOf(configurationId))
                        .containsEntry("enabled", "false");

        automationWorkflowProjectConfigurationAdminFacade.deleteConfiguration(configurationId);

        assertThat(
            findSingleEvent(AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CONFIGURATION_DELETED)
                .getData())
                    .containsEntry("result", "SUCCESS")
                    .containsEntry("configurationId", String.valueOf(configurationId));
    }

    @Test
    void testConnectedUserProjectChangesOfUnknownTargetsRecordFailures() {
        String workflowUuid = String.valueOf(UUID.randomUUID());

        assertThatThrownBy(
            () -> connectedUserProjectAdminFacade.provisionProjectWorkflows(
                Long.MAX_VALUE, List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null))))
                    .isInstanceOf(RuntimeException.class);

        assertThat(
            findSingleEvent(AutomationWorkflowProjectAuditEvents.CONNECTED_USER_PROJECT_WORKFLOWS_PROVISIONED)
                .getData())
                    .containsEntry("result", "ERROR")
                    .containsEntry("connectedUserId", String.valueOf(Long.MAX_VALUE))
                    .containsEntry("workflowUuids", "[" + workflowUuid + "]");

        assertThatThrownBy(() -> connectedUserProjectAdminFacade.enableProjectWorkflow(Long.MAX_VALUE, true))
            .isInstanceOf(RuntimeException.class);

        assertThat(
            findSingleEvent(AutomationWorkflowProjectAuditEvents.CONNECTED_USER_PROJECT_WORKFLOW_ENABLED_UPDATED)
                .getData())
                    .containsEntry("result", "ERROR")
                    .containsEntry("connectedUserProjectWorkflowId", String.valueOf(Long.MAX_VALUE))
                    .containsEntry("enabled", "true");

        assertThatThrownBy(() -> connectedUserProjectAdminFacade.deleteProjectWorkflow(Long.MAX_VALUE))
            .isInstanceOf(RuntimeException.class);

        assertThat(
            findSingleEvent(AutomationWorkflowProjectAuditEvents.CONNECTED_USER_PROJECT_WORKFLOW_DELETED).getData())
                .containsEntry("result", "ERROR")
                .containsEntry("connectedUserProjectWorkflowId", String.valueOf(Long.MAX_VALUE));
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 100));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent()
            .stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.EMBEDDED);

        return persistentAuditEvent;
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    @EnableMethodSecurity
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService,
            ObjectProvider<MethodSecurityExpressionContributor> methodSecurityExpressionContributorProvider) {

            AutomationMethodSecurityExpressionHandler automationMethodSecurityExpressionHandler =
                new AutomationMethodSecurityExpressionHandler(
                    permissionService, methodSecurityExpressionContributorProvider);

            automationMethodSecurityExpressionHandler.setPermissionEvaluator(
                new AutomationPermissionEvaluator(permissionService));

            return automationMethodSecurityExpressionHandler;
        }

        @Bean("permissionService")
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.isTenantAdmin())
                .thenAnswer(invocation -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));

            return permissionService;
        }
    }
}
