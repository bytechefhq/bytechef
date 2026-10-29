/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationDeletedEvent;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationUpdatedEvent;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowProjectConfigurationErrorType;
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
    ConnectedUserWorkflowReferenceAdminFacade.class,
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
    private ConnectedUserWorkflowReferenceAdminFacade connectedUserWorkflowReferenceAdminFacade;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Test
    void testCreateConfigurationWithUnpublishedVersionThrows() {
        long projectId = createPublishedProject("Unpublished Version", 1);

        assertThatThrownBy(
            () -> automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
                projectId, ENVIRONMENT_ID, 2, true, true)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testCreateConfigurationWithAnUnknownEnvironmentIdThrows() {
        long projectId = createPublishedProject("Unknown Environment", 1);

        assertThatThrownBy(
            () -> automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
                projectId, Environment.values().length, 1, true, true))
                    .isInstanceOf(ConfigurationException.class);
        assertThatThrownBy(
            () -> automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
                projectId, -1, 1, true, true))
                    .isInstanceOf(ConfigurationException.class);
        assertThat(automationWorkflowProjectConfigurationAdminFacade.getConfigurations(projectId)).isEmpty();
    }

    @Test
    void testCreateConfigurationTwiceForTheSameEnvironmentThrows() {
        long projectId = createPublishedProject("Duplicate Environment", 1);

        automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        assertThatThrownBy(
            () -> automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
                projectId, ENVIRONMENT_ID, 1, true, true))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                        configurationException.getMessage()).contains("already has a configuration"));
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
        assertThat(configuration.workflows()
            .getFirst()
            .workflowId()).isEqualTo(
                projectWorkflowService.getProjectWorkflows(projectId, 1)
                    .getFirst()
                    .getWorkflowId());

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
    void testEnableConfigurationTogglesTheFlag() {
        long projectId = createPublishedProject("Configuration Switch", 1);

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        applicationEvents.clear();

        automationWorkflowProjectConfigurationAdminFacade.enableConfiguration(configurationId, false);

        assertThat(automationWorkflowProjectConfigurationService.getConfiguration(configurationId)
            .isEnabled()).isFalse();

        automationWorkflowProjectConfigurationAdminFacade.enableConfiguration(configurationId, true);

        assertThat(automationWorkflowProjectConfigurationService.getConfiguration(configurationId)
            .isEnabled()).isTrue();
        assertThat(applicationEvents.stream(AutomationWorkflowProjectConfigurationUpdatedEvent.class)
            .toList())
                .containsExactly(
                    new AutomationWorkflowProjectConfigurationUpdatedEvent(projectId, ENVIRONMENT_ID),
                    new AutomationWorkflowProjectConfigurationUpdatedEvent(projectId, ENVIRONMENT_ID));
    }

    @Test
    void testUpdateConfigurationWithUnpublishedVersionThrows() {
        long projectId = createPublishedProject("Update Unpublished Version", 1);

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        assertThatThrownBy(
            () -> automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(
                configurationId, 2, true, true)).isInstanceOf(ConfigurationException.class);
        assertThat(automationWorkflowProjectConfigurationService.getConfiguration(configurationId)
            .getProjectVersion()).isEqualTo(1);
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

    @Test
    void testUpdateConfigurationRejectsOlderVersionLackingReferencedWorkflow() {
        long projectId = createPublishedProject("Rollback Blocked", 1);

        String workflowUuid = addWorkflowAndPublish(projectId, "Rollback Blocked Workflow");

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 2, true, true);

        givenReferences(newReference(workflowUuid, Environment.values()[(int) ENVIRONMENT_ID].name(), false));

        assertThatThrownBy(
            () -> automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(
                configurationId, 1, true, true))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> {
                        assertThat(configurationException.getMessage())
                            .contains("Version 1 lacks workflows connected users still use: Rollback Blocked Workflow")
                            .contains("Turn them off in this configuration or remove them from those users first.");
                        assertThat(configurationException.getErrorKey())
                            .isEqualTo(
                                AutomationWorkflowProjectConfigurationErrorType.VERSION_LACKS_REFERENCED_WORKFLOWS
                                    .getErrorKey());
                    });
        assertThat(automationWorkflowProjectConfigurationService.getConfiguration(configurationId)
            .getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testUpdateConfigurationAllowsOlderVersionContainingReferencedWorkflows() {
        long projectId = createPublishedProject("Rollback Allowed", 1);

        addWorkflowAndPublish(projectId, "Rollback Allowed Workflow");

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 2, true, true);

        String firstWorkflowUuid = projectWorkflowService.getProjectWorkflows(projectId, 1)
            .getFirst()
            .getUuidAsString();

        givenReferences(newReference(firstWorkflowUuid, Environment.values()[(int) ENVIRONMENT_ID].name(), false));

        automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(configurationId, 1, true, true);

        assertThat(automationWorkflowProjectConfigurationService.getConfiguration(configurationId)
            .getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testUpdateConfigurationIgnoresDanglingAndOtherEnvironmentReferences() {
        long projectId = createPublishedProject("Rollback Ignored References", 1);

        String workflowUuid = addWorkflowAndPublish(projectId, "Rollback Ignored Workflow");

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 2, true, true);

        Environment otherEnvironment = Environment.values()[(int) ENVIRONMENT_ID + 1];

        givenReferences(
            newReference(workflowUuid, Environment.values()[(int) ENVIRONMENT_ID].name(), true),
            newReference(workflowUuid, otherEnvironment.name(), false));

        automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(configurationId, 1, true, true);

        assertThat(automationWorkflowProjectConfigurationService.getConfiguration(configurationId)
            .getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testUpdateConfigurationStillBlocksWhenWorkflowIsTurnedOff() {
        long projectId = createPublishedProject("Rollback Turned Off", 1);

        String workflowUuid = addWorkflowAndPublish(projectId, "Rollback Turned Off Workflow");

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 2, true, true);

        automationWorkflowProjectConfigurationAdminFacade.enableConfigurationWorkflow(
            configurationId, workflowUuid, false);

        givenReferences(newReference(workflowUuid, Environment.values()[(int) ENVIRONMENT_ID].name(), false));

        assertThatThrownBy(
            () -> automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(
                configurationId, 1, true, true))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                        configurationException.getErrorKey())
                            .isEqualTo(
                                AutomationWorkflowProjectConfigurationErrorType.VERSION_LACKS_REFERENCED_WORKFLOWS
                                    .getErrorKey()));
        assertThat(automationWorkflowProjectConfigurationService.getConfiguration(configurationId)
            .getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testUpdateConfigurationAllowsNewerVersionRemovingReferencedWorkflow() {
        long projectId = createPublishedProject("Roll Forward", 1);

        String workflowUuid = addWorkflowAndPublish(projectId, "Roll Forward Workflow");

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 2, true, true);

        automationWorkflowProjectFacade.deleteProjectWorkflow(workflowUuid);
        automationWorkflowProjectFacade.publishProject(projectId);

        givenReferences(newReference(workflowUuid, Environment.values()[(int) ENVIRONMENT_ID].name(), false));

        automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(configurationId, 3, true, true);

        assertThat(automationWorkflowProjectConfigurationService.getConfiguration(configurationId)
            .getProjectVersion()).isEqualTo(3);
    }

    private String addWorkflowAndPublish(long projectId, String label) {
        String workflowUuid = automationWorkflowProjectFacade.createProjectWorkflow(
            projectId, "{\"label\": \"" + label + "\", \"description\": \"\", \"tasks\": []}", null);

        automationWorkflowProjectFacade.publishProject(projectId);

        return workflowUuid;
    }

    private void givenReferences(ConnectedUserWorkflowReferenceDTO... references) {
        when(connectedUserWorkflowReferenceAdminFacade.getReferences(anySet()))
            .thenReturn(List.of(references));
    }

    private static ConnectedUserWorkflowReferenceDTO newReference(
        String automationWorkflowUuid, String environment, boolean dangling) {

        return new ConnectedUserWorkflowReferenceDTO(
            automationWorkflowUuid, "user-" + UUID.randomUUID(), environment, true, dangling, null, null);
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
