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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
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
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationDeletedEvent;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationUpdatedEvent;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final String CHANNEL_REQUIRED_DEFINITION = """
        {"label":"%s","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
         "triggers":[],"tasks":[]}
        """;

    private static final String CHANNEL_OPTIONAL_DEFINITION = """
        {"label":"%s","inputs":[{"name":"channel","label":"Channel","type":"string","required":false}],
         "triggers":[],"tasks":[]}
        """;

    private static final String NO_INPUT_DEFINITION = """
        {"label":"%s","triggers":[],"tasks":[]}
        """;

    private static final String SLACK_DEFINITION = """
        {"label":"%s","triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_WITH_CHANNEL_DEFINITION = """
        {"label":"%s","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
         "triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_AND_JIRA_DEFINITION = """
        {"label":"%s","triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}},
         {"name":"createIssue1","type":"jira/v1/createIssue","parameters":{}}]}
        """;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private AutomationWorkflowProjectConfigurationAdminFacade automationWorkflowProjectConfigurationAdminFacade;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ConnectedUserWorkflowReferenceAdminFacade connectedUserWorkflowReferenceAdminFacade;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private WorkflowService workflowService;

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

    @Test
    void testGetVersionImpactCountsOnlyReferencesLackingTheNewRequiredInput() {
        long projectId = createProject("Impact New Input");

        String workflowUuid = addWorkflow(projectId, withLabel(NO_INPUT_DEFINITION, "Notify Slack"));

        publish(projectId);
        redefineDraftWorkflow(workflowUuid, withLabel(CHANNEL_REQUIRED_DEFINITION, "Notify Slack"));
        publish(projectId);

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        String environmentName = Environment.values()[(int) ENVIRONMENT_ID].name();

        givenStoredInputs(101L, projectId, 1, workflowUuid, Map.of("channel", "general"));
        givenStoredInputs(102L, projectId, 1, workflowUuid, Map.of());
        givenStoredInputs(103L, projectId, 1, workflowUuid, Map.of("channel", "  "));
        givenReferences(
            newReference(workflowUuid, environmentName, false, 101L),
            newReference(workflowUuid, environmentName, false, 102L),
            newReference(workflowUuid, environmentName, false, 103L));

        AutomationWorkflowProjectConfigurationVersionImpactDTO versionImpact =
            automationWorkflowProjectConfigurationAdminFacade.getVersionImpact(configurationId, 2);

        assertThat(versionImpact.workflows()).containsExactly(
            new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
                workflowUuid, "Notify Slack", List.of("channel"), List.of(), 2));
    }

    @Test
    void testGetVersionImpactReportsAnOptionalInputThatBecameRequired() {
        long projectId = createProject("Impact Optional To Required");

        String workflowUuid = addWorkflow(projectId, withLabel(CHANNEL_OPTIONAL_DEFINITION, "Optional Flow"));

        publish(projectId);
        redefineDraftWorkflow(workflowUuid, withLabel(CHANNEL_REQUIRED_DEFINITION, "Optional Flow"));
        publish(projectId);

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        givenStoredInputs(101L, projectId, 1, workflowUuid, Map.of());
        givenReferences(
            newReference(workflowUuid, Environment.values()[(int) ENVIRONMENT_ID].name(), false, 101L));

        AutomationWorkflowProjectConfigurationVersionImpactDTO versionImpact =
            automationWorkflowProjectConfigurationAdminFacade.getVersionImpact(configurationId, 2);

        assertThat(versionImpact.workflows()).containsExactly(
            new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
                workflowUuid, "Optional Flow", List.of("channel"), List.of(), 1));
    }

    @Test
    void testGetVersionImpactOmitsWorkflowsWithoutNewRequirements() {
        long projectId = createProject("Impact No Change");

        String relaxedWorkflowUuid = addWorkflow(projectId, withLabel(CHANNEL_REQUIRED_DEFINITION, "Relaxed"));
        String unchangedWorkflowUuid = addWorkflow(projectId, withLabel(CHANNEL_REQUIRED_DEFINITION, "Unchanged"));

        publish(projectId);
        redefineDraftWorkflow(relaxedWorkflowUuid, withLabel(CHANNEL_OPTIONAL_DEFINITION, "Relaxed"));
        addWorkflow(projectId, withLabel(CHANNEL_REQUIRED_DEFINITION, "Only In New Version"));
        publish(projectId);

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        String environmentName = Environment.values()[(int) ENVIRONMENT_ID].name();

        givenStoredInputs(101L, projectId, 1, relaxedWorkflowUuid, Map.of());
        givenStoredInputs(101L, projectId, 1, unchangedWorkflowUuid, Map.of());
        givenReferences(
            newReference(relaxedWorkflowUuid, environmentName, false, 101L),
            newReference(unchangedWorkflowUuid, environmentName, false, 101L));

        assertThat(automationWorkflowProjectConfigurationAdminFacade.getVersionImpact(configurationId, 2)
            .workflows()).isEmpty();
    }

    @Test
    void testGetVersionImpactReportsANewRequiredConnectionAndCountsEveryReference() {
        long projectId = createProject("Impact New Connection");

        String workflowUuid = addWorkflow(projectId, withLabel(SLACK_DEFINITION, "Sync Issues"));

        publish(projectId);
        redefineDraftWorkflow(workflowUuid, withLabel(SLACK_AND_JIRA_DEFINITION, "Sync Issues"));
        publish(projectId);

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenAnswer(invocation -> switch (invocation.<WorkflowTask>getArgument(0)
                .getName()) {
                case "postMessage1" -> List.of(new ComponentConnection("slack", 1, "postMessage1", "slack", true));
                case "createIssue1" -> List.of(new ComponentConnection("jira", 1, "createIssue1", "jira", true));
                default -> List.of();
            });

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        String environmentName = Environment.values()[(int) ENVIRONMENT_ID].name();

        givenStoredInputs(101L, projectId, 1, workflowUuid, Map.of());
        givenStoredInputs(102L, projectId, 1, workflowUuid, Map.of());
        givenReferences(
            newReference(workflowUuid, environmentName, false, 101L),
            newReference(workflowUuid, environmentName, false, 102L));

        assertThat(automationWorkflowProjectConfigurationAdminFacade.getVersionImpact(configurationId, 2)
            .workflows()).containsExactly(
                new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
                    workflowUuid, "Sync Issues", List.of(), List.of("jira"), 2));
    }

    @Test
    void testGetVersionImpactIgnoresDanglingAndOtherEnvironmentReferences() {
        long projectId = createProject("Impact Ignored References");

        String workflowUuid = addWorkflow(projectId, withLabel(NO_INPUT_DEFINITION, "Ignored References"));

        publish(projectId);
        redefineDraftWorkflow(workflowUuid, withLabel(CHANNEL_REQUIRED_DEFINITION, "Ignored References"));
        publish(projectId);

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        String environmentName = Environment.values()[(int) ENVIRONMENT_ID].name();
        Environment otherEnvironment = Environment.values()[(int) ENVIRONMENT_ID + 1];

        givenStoredInputs(101L, projectId, 1, workflowUuid, Map.of());
        givenStoredInputs(102L, projectId, 1, workflowUuid, Map.of());
        givenStoredInputs(103L, projectId, 1, workflowUuid, Map.of());
        givenReferences(
            newReference(workflowUuid, environmentName, false, 101L),
            newReference(workflowUuid, environmentName, true, 102L),
            newReference(workflowUuid, otherEnvironment.name(), false, 103L));

        assertThat(automationWorkflowProjectConfigurationAdminFacade.getVersionImpact(configurationId, 2)
            .workflows()
            .getFirst()
            .affectedConnectedUserCount()).isEqualTo(1);
    }

    @Test
    void testGetProvidableValuesExposesTheSlotsAndInputsOfThePinnedVersionNotTheDraft() {
        long projectId = createProject("Pinned Slots");

        String workflowUuid = addWorkflow(projectId, withLabel(SLACK_WITH_CHANNEL_DEFINITION, "Pinned Slots"));

        publish(projectId);
        redefineDraftWorkflow(workflowUuid, withLabel(NO_INPUT_DEFINITION, "Pinned Slots"));

        givenSlots(new ComponentConnection("slack", 1, "postMessage1", "slack", true));

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO providableValues =
            automationWorkflowProjectConfigurationAdminFacade.getConfigurationWorkflowProvidableValues(
                configurationId, workflowUuid);

        assertThat(providableValues.connectionSlots()).extracting(
            AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ConnectionSlot::componentName)
            .containsExactly("slack");
        assertThat(providableValues.inputs()).containsExactly(
            new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.Input(
                "channel", "Channel", "string", true));
    }

    @Test
    void testGetProvidableValuesExposesAClusterElementSlotOnceWithItsRequiredFlag() {
        long projectId = createProject("Cluster Slots");

        String workflowUuid = addWorkflow(projectId, withLabel(SLACK_DEFINITION, "Cluster Slots"));

        publish(projectId);

        givenSlots(
            new ComponentConnection("slack", 1, "postMessage1", "slack", false),
            new ComponentConnection("openai", 1, "postMessage1", "openai", true),
            new ComponentConnection("slack", 1, "postMessage1", "slack2", true));

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ConnectionSlot> connectionSlots =
            automationWorkflowProjectConfigurationAdminFacade
                .getConfigurationWorkflowProvidableValues(configurationId, workflowUuid)
                .connectionSlots();

        assertThat(connectionSlots).extracting(
            AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ConnectionSlot::componentName)
            .containsExactly("slack", "openai");
        assertThat(connectionSlots)
            .extracting(AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ConnectionSlot::required)
            .containsExactly(true, true);
    }

    @Test
    void testGetProvidableValuesExcludesConnectedUserAndOtherEnvironmentConnectionsFromTheProvidableOnes() {
        ProvidedValuesFixture fixture = createProvidedValuesFixture("Providable Connections");

        givenConnections(
            "slack", newConnection(21L, "slack", ENVIRONMENT_ID, "Company Slack"),
            newConnection(22L, "slack", ENVIRONMENT_ID, "Customer Slack"),
            newConnection(23L, "slack", ENVIRONMENT_ID + 1, "Production Slack"));

        connectedUserConnectionService.create(9002L, 22L);

        try {
            List<AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection> providable =
                automationWorkflowProjectConfigurationAdminFacade
                    .getConfigurationWorkflowProvidableValues(fixture.configurationId(), fixture.workflowUuid())
                    .connectionSlots()
                    .getFirst()
                    .providableConnections();

            assertThat(providable).containsExactly(
                new AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO.ProvidableConnection(
                    21L, "Company Slack"));
        } finally {
            connectedUserConnectionService.deleteByConnectionId(22L);
        }
    }

    @Test
    void testGetProvidableValuesReturnsTheStoredProvidedValues() {
        ProvidedValuesFixture fixture = createProvidedValuesFixture("Providable Stored Values");

        givenConnection(11L, "slack", ENVIRONMENT_ID);

        automationWorkflowProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            fixture.configurationId(), fixture.workflowUuid(), Map.of("slack", 11L), Map.of("channel", "general"));

        AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO providableValues =
            automationWorkflowProjectConfigurationAdminFacade.getConfigurationWorkflowProvidableValues(
                fixture.configurationId(), fixture.workflowUuid());

        assertThat(providableValues.providedConnections()).isEqualTo(Map.of("slack", 11L));
        assertThat(providableValues.providedInputs()).isEqualTo(Map.of("channel", "general"));
    }

    @Test
    void testUpdateConfigurationWorkflowProvidesValuesVisibleInTheListing() {
        ProvidedValuesFixture fixture = createProvidedValuesFixture("Provided Values");

        givenConnection(11L, "slack", ENVIRONMENT_ID);

        applicationEvents.clear();

        automationWorkflowProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            fixture.configurationId(), fixture.workflowUuid(), Map.of("slack", 11L), Map.of("channel", "general"));

        AutomationWorkflowProjectConfigurationDTO.Workflow workflow =
            automationWorkflowProjectConfigurationAdminFacade.getConfigurations(fixture.projectId())
                .getFirst()
                .workflows()
                .getFirst();

        assertThat(workflow.providedConnections()).isEqualTo(Map.of("slack", 11L));
        assertThat(workflow.providedInputs()).isEqualTo(Map.of("channel", "general"));
        assertThat(workflow.enabled()).isTrue();
        assertThat(applicationEvents.stream(AutomationWorkflowProjectConfigurationUpdatedEvent.class)
            .toList())
                .containsExactly(
                    new AutomationWorkflowProjectConfigurationUpdatedEvent(fixture.projectId(), ENVIRONMENT_ID));
    }

    @Test
    void testUpdateConfigurationWorkflowWithEmptyMapsClearsTheValues() {
        ProvidedValuesFixture fixture = createProvidedValuesFixture("Provided Values Cleared");

        givenConnection(11L, "slack", ENVIRONMENT_ID);

        automationWorkflowProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            fixture.configurationId(), fixture.workflowUuid(), Map.of("slack", 11L), Map.of("channel", "general"));
        automationWorkflowProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            fixture.configurationId(), fixture.workflowUuid(), Map.of(), Map.of());

        AutomationWorkflowProjectConfigurationDTO.Workflow workflow =
            automationWorkflowProjectConfigurationAdminFacade.getConfigurations(fixture.projectId())
                .getFirst()
                .workflows()
                .getFirst();

        assertThat(workflow.providedConnections()).isEmpty();
        assertThat(workflow.providedInputs()).isEmpty();
    }

    @Test
    void testUpdateConfigurationWorkflowRejectsAComponentThatIsNotAConnectionSlot() {
        ProvidedValuesFixture fixture = createProvidedValuesFixture("Provided Unknown Slot");

        givenConnection(11L, "jira", ENVIRONMENT_ID);

        assertInvalidProvidedValue(fixture, Map.of("jira", 11L), Map.of(), "jira");
    }

    @Test
    void testUpdateConfigurationWorkflowRejectsAConnectionOfAnotherComponent() {
        ProvidedValuesFixture fixture = createProvidedValuesFixture("Provided Other Component");

        givenConnection(12L, "jira", ENVIRONMENT_ID);

        assertInvalidProvidedValue(fixture, Map.of("slack", 12L), Map.of(),
            "Connection id=12 belongs to component jira");
    }

    @Test
    void testUpdateConfigurationWorkflowRejectsAConnectionOfAnotherEnvironment() {
        ProvidedValuesFixture fixture = createProvidedValuesFixture("Provided Other Environment");

        givenConnection(13L, "slack", ENVIRONMENT_ID + 1);

        assertInvalidProvidedValue(
            fixture, Map.of("slack", 13L), Map.of(), "Connection id=13 for component slack belongs to environment");
    }

    @Test
    void testUpdateConfigurationWorkflowRejectsAMissingConnection() {
        ProvidedValuesFixture fixture = createProvidedValuesFixture("Provided Missing Connection");

        when(connectionService.fetchConnection(14L)).thenReturn(Optional.empty());

        assertInvalidProvidedValue(
            fixture, Map.of("slack", 14L), Map.of(), "Connection id=14 for component slack does not exist");
    }

    @Test
    void testUpdateConfigurationWorkflowRejectsAnUnknownInput() {
        ProvidedValuesFixture fixture = createProvidedValuesFixture("Provided Unknown Input");

        assertInvalidProvidedValue(fixture, Map.of(), Map.of("topic", "news"), "Input topic");
    }

    @Test
    void testUpdateConfigurationWorkflowRejectsAConnectionOwnedByAConnectedUser() {
        ProvidedValuesFixture fixture = createProvidedValuesFixture("Provided Connected User Connection");

        givenConnection(15L, "slack", ENVIRONMENT_ID);

        connectedUserConnectionService.create(9001L, 15L);

        try {
            assertInvalidProvidedValue(
                fixture, Map.of("slack", 15L), Map.of(),
                "Connection id=15 for component slack belongs to a connected user");
        } finally {
            connectedUserConnectionService.deleteByConnectionId(15L);
        }
    }

    @Test
    void testUpdateConfigurationWorkflowRejectsANullConnectionId() {
        ProvidedValuesFixture fixture = createProvidedValuesFixture("Provided Null Connection");

        Map<String, Long> providedConnections = new HashMap<>();

        providedConnections.put("slack", null);

        assertInvalidProvidedValue(fixture, providedConnections, Map.of(), "Component slack has no connection id");
    }

    @Test
    void testUpdateConfigurationWorkflowRejectsAWorkflowOutsideThePinnedVersion() {
        ProvidedValuesFixture fixture = createProvidedValuesFixture("Provided Unpinned Workflow");

        String unpinnedWorkflowUuid = addWorkflow(
            fixture.projectId(), withLabel(SLACK_WITH_CHANNEL_DEFINITION, "Provided Unpinned Workflow Draft"));

        publish(fixture.projectId());

        ProvidedValuesFixture unpinnedFixture = new ProvidedValuesFixture(
            fixture.projectId(), fixture.configurationId(), unpinnedWorkflowUuid);

        assertInvalidProvidedValue(
            unpinnedFixture, Map.of(), Map.of("channel", "general"),
            "Workflow uuid=%s is not part of version 1".formatted(unpinnedWorkflowUuid));
    }

    private void assertInvalidProvidedValue(
        ProvidedValuesFixture fixture, Map<String, Long> providedConnections, Map<String, Object> providedInputs,
        String expectedMessage) {

        applicationEvents.clear();

        assertThatThrownBy(
            () -> automationWorkflowProjectConfigurationAdminFacade.updateConfigurationWorkflow(
                fixture.configurationId(), fixture.workflowUuid(), providedConnections, providedInputs))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> {
                        assertThat(configurationException.getMessage()).contains(expectedMessage);
                        assertThat(configurationException.getErrorKey()).isEqualTo(
                            AutomationWorkflowProjectConfigurationErrorType.INVALID_PROVIDED_VALUE.getErrorKey());
                    });

        AutomationWorkflowProjectConfigurationDTO.Workflow workflow =
            automationWorkflowProjectConfigurationAdminFacade.getConfigurations(fixture.projectId())
                .getFirst()
                .workflows()
                .getFirst();

        assertThat(workflow.providedConnections()).isEmpty();
        assertThat(workflow.providedInputs()).isEmpty();
        assertThat(automationWorkflowProjectConfigurationService.getConfiguration(fixture.configurationId())
            .fetchWorkflow(UUID.fromString(fixture.workflowUuid()))).isEmpty();
        assertThat(applicationEvents.stream(AutomationWorkflowProjectConfigurationUpdatedEvent.class)
            .count()).isZero();
    }

    private ProvidedValuesFixture createProvidedValuesFixture(String name) {
        long projectId = createProject(name);

        String workflowUuid = addWorkflow(projectId, withLabel(SLACK_WITH_CHANNEL_DEFINITION, name));

        publish(projectId);

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenReturn(List.of(new ComponentConnection("slack", 1, "postMessage1", "slack", true)));

        long configurationId = automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, ENVIRONMENT_ID, 1, true, true);

        return new ProvidedValuesFixture(projectId, configurationId, workflowUuid);
    }

    private void givenSlots(ComponentConnection... componentConnections) {
        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenReturn(List.of(componentConnections));
    }

    private static Connection newConnection(long id, String componentName, long environmentId, String name) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setComponentName(componentName);
        connection.setEnvironmentId((int) environmentId);
        connection.setName(name);

        return connection;
    }

    private void givenConnections(String componentName, Connection... connections) {
        when(connectionService.getConnections(eq(componentName), any(), any(), any(), any()))
            .thenAnswer(invocation -> List.of(connections)
                .stream()
                .filter(connection -> Long.valueOf(connection.getEnvironmentId())
                    .equals(invocation.getArgument(3)))
                .toList());
    }

    private void givenConnection(long connectionId, String componentName, long environmentId) {
        Connection connection = new Connection();

        connection.setId(connectionId);
        connection.setComponentName(componentName);
        connection.setEnvironmentId((int) environmentId);

        when(connectionService.fetchConnection(connectionId)).thenReturn(Optional.of(connection));
    }

    private String addWorkflow(long projectId, String definition) {
        return automationWorkflowProjectFacade.createProjectWorkflow(projectId, definition, null);
    }

    private long createProject(String name) {
        return automationWorkflowProjectFacade.createProject(name + " " + UUID.randomUUID(), "", null, List.of(), null);
    }

    private void givenStoredInputs(
        long projectDeploymentId, long projectId, int projectVersion, String workflowUuid, Map<String, ?> inputs) {

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(projectDeploymentId);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);

        when(projectDeploymentService.fetchProjectDeployment(projectDeploymentId))
            .thenReturn(Optional.of(projectDeployment));

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setInputs(inputs);

        String workflowId = projectWorkflowService.fetchProjectWorkflow(projectId, projectVersion, workflowUuid)
            .orElseThrow()
            .getWorkflowId();

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(projectDeploymentId, workflowId))
            .thenReturn(Optional.of(projectDeploymentWorkflow));
    }

    private static String withLabel(String definitionTemplate, String label) {
        return definitionTemplate.replace("%s", label);
    }

    private void publish(long projectId) {
        automationWorkflowProjectFacade.publishProject(projectId);
    }

    private void redefineDraftWorkflow(String workflowUuid, String definition) {
        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(draftWorkflowId, definition, draftWorkflow.getVersion());
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

        return newReference(automationWorkflowUuid, environment, dangling, null);
    }

    private static ConnectedUserWorkflowReferenceDTO newReference(
        String automationWorkflowUuid, String environment, boolean dangling, Long projectDeploymentId) {

        return new ConnectedUserWorkflowReferenceDTO(
            automationWorkflowUuid, "user-" + UUID.randomUUID(), environment, true, dangling, null, null,
            projectDeploymentId);
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

    private record ProvidedValuesFixture(long projectId, long configurationId, String workflowUuid) {
    }
}
