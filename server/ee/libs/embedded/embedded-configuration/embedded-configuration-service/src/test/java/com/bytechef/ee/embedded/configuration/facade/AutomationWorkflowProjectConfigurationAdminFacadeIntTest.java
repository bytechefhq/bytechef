/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationDeletedEvent;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationUpdatedEvent;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationWorkflowProjectFacadeIntTestConfiguration.class,
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@RecordApplicationEvents
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    ClusterElementDefinitionService.class, TriggerDefinitionFacade.class,
    ComponentConnectionFacade.class,
    ComponentDefinitionService.class, ConnectedUserService.class, ConnectionDefinitionService.class,
    ConnectionFacade.class, ConnectionLifecycleFacade.class, ConnectionService.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class,
    GitHubProxyClient.class, JobFacade.class, JobService.class, McpComponentService.class,
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
class AutomationWorkflowProjectConfigurationAdminFacadeIntTest {

    private static final long ENVIRONMENT_ID = 0L;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private AutomationWorkflowProjectConfigurationAdminFacade automationWorkflowProjectConfigurationAdminFacade;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;

    @Test
    void testCreateConfigurationWithUnpublishedVersionThrows() {
        long projectId = createPublishedProject("Unpublished Version", 1);

        assertThatThrownBy(
            () -> automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
                projectId, ENVIRONMENT_ID, 2, true, true)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testGetConfigurationsListsPinnedVersionWorkflows() {
        long projectId = createPublishedProject("Pinned Version", 1);

        automationWorkflowProjectFacade.createProjectWorkflow(projectId, null, null);
        automationWorkflowProjectFacade.publishProject(projectId);

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        List<AutomationWorkflowProjectConfigurationDTO> configurations =
            automationWorkflowProjectConfigurationAdminFacade.getConfigurations(projectId);

        assertThat(configurations).hasSize(1);

        AutomationWorkflowProjectConfigurationDTO configuration = configurations.getFirst();

        assertThat(configuration.id()).isEqualTo(configurationId);
        assertThat(configuration.projectVersion()).isEqualTo(1);
        assertThat(configuration.workflows()).hasSize(1);
        assertThat(configuration.workflows()
            .getFirst()
            .enabled()).isTrue();

        automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(configurationId, 2, true, true);

        assertThat(automationWorkflowProjectConfigurationAdminFacade.getConfigurations(projectId)
            .getFirst()
            .workflows()).hasSize(2);
    }

    @Test
    void testEnableConfigurationWorkflowIsReflectedInTheListing() {
        long projectId = createPublishedProject("Workflow Switch", 1);

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        String workflowUuid = automationWorkflowProjectConfigurationAdminFacade.getConfigurations(projectId)
            .getFirst()
            .workflows()
            .getFirst()
            .workflowUuid();

        automationWorkflowProjectConfigurationAdminFacade.enableConfigurationWorkflow(
            configurationId, workflowUuid, false);

        assertThat(automationWorkflowProjectConfigurationAdminFacade.getConfigurations(projectId)
            .getFirst()
            .workflows()
            .getFirst()
            .enabled()).isFalse();
        assertThat(applicationEvents.stream(AutomationWorkflowProjectConfigurationUpdatedEvent.class)
            .count()).isEqualTo(2);
    }

    @Test
    void testUpdateConfigurationPublishesUpdatedEvent() {
        long projectId = createPublishedProject("Updated Event", 2);

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        applicationEvents.clear();

        automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(configurationId, 2, false, false);

        assertThat(applicationEvents.stream(AutomationWorkflowProjectConfigurationUpdatedEvent.class)
            .toList())
                .containsExactly(new AutomationWorkflowProjectConfigurationUpdatedEvent(projectId, ENVIRONMENT_ID));

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.getConfiguration(configurationId);

        assertThat(configuration.getProjectVersion()).isEqualTo(2);
        assertThat(configuration.isEnabled()).isFalse();
        assertThat(configuration.isAutomationHubVisible()).isFalse();
    }

    @Test
    void testDeleteConfigurationPublishesDeletedEvent() {
        long projectId = createPublishedProject("Deleted Event", 1);

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        automationWorkflowProjectConfigurationAdminFacade.deleteConfiguration(configurationId);

        assertThat(automationWorkflowProjectConfigurationAdminFacade.getConfigurations(projectId)).isEmpty();
        assertThat(applicationEvents.stream(AutomationWorkflowProjectConfigurationDeletedEvent.class)
            .toList())
                .containsExactly(new AutomationWorkflowProjectConfigurationDeletedEvent(projectId, ENVIRONMENT_ID));
    }

    private long createPublishedProject(String name, int publishedVersionCount) {
        long projectId = automationWorkflowProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), null);

        automationWorkflowProjectFacade.createProjectWorkflow(projectId, null, null);

        for (int i = 0; i < publishedVersionCount; i++) {
            automationWorkflowProjectFacade.publishProject(projectId);
        }

        return projectId;
    }
}
