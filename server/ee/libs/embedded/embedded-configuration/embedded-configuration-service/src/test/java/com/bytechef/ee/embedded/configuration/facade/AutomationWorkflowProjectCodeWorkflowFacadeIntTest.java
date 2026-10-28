/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    ClusterElementDefinitionService.class, TriggerDefinitionFacade.class,
    ComponentConnectionFacade.class,
    ComponentDefinitionService.class, ConnectedUserService.class, ConnectionDefinitionService.class,
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
class AutomationWorkflowProjectCodeWorkflowFacadeIntTest {

    @Autowired
    private AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;

    @Autowired
    private AutomationWorkflowProjectConfigurationAdminFacade automationWorkflowProjectConfigurationAdminFacade;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private ProjectService projectService;

    @Test
    void testSaveWithReleaseCreatesAnEnabledConfigurationWhenNoneExists() {
        String name = "Release Create " + UUID.randomUUID();
        long projectId = createDeployedProject(name);

        automationWorkflowProjectCodeWorkflowFacade.save(
            codeProject(name), Language.JAVASCRIPT, Environment.PRODUCTION, true);

        AutomationWorkflowProjectConfigurationDTO configuration = getConfiguration(projectId);

        assertThat(configuration.environmentId()).isEqualTo(Environment.PRODUCTION.ordinal());
        assertThat(configuration.projectVersion()).isEqualTo(getLastPublishedVersion(projectId));
        assertThat(configuration.enabled()).isTrue();
        assertThat(configuration.automationHubVisible()).isTrue();
    }

    @Test
    void testSaveWithReleaseUpdatesAnExistingConfigurationWithoutEnablingIt() {
        String name = "Release Update " + UUID.randomUUID();
        long projectId = createDeployedProject(name);

        automationWorkflowProjectFacade.publishProject(projectId);

        int firstVersion = getLastPublishedVersion(projectId);

        automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, Environment.PRODUCTION.ordinal(), firstVersion, false, false);

        automationWorkflowProjectCodeWorkflowFacade.save(
            codeProject(name), Language.JAVASCRIPT, Environment.PRODUCTION, true);

        int newVersion = getLastPublishedVersion(projectId);

        assertThat(newVersion).isGreaterThan(firstVersion);

        AutomationWorkflowProjectConfigurationDTO configuration = getConfiguration(projectId);

        assertThat(configuration.projectVersion()).isEqualTo(newVersion);
        assertThat(configuration.enabled()).isFalse();
        assertThat(configuration.automationHubVisible()).isFalse();
    }

    @Test
    void testSaveWithoutReleaseLeavesTheConfigurationAlone() {
        String name = "Release Off " + UUID.randomUUID();
        long projectId = createDeployedProject(name);

        automationWorkflowProjectFacade.publishProject(projectId);

        int firstVersion = getLastPublishedVersion(projectId);

        automationWorkflowProjectConfigurationAdminFacade.createConfiguration(
            projectId, Environment.PRODUCTION.ordinal(), firstVersion, true, true);

        automationWorkflowProjectCodeWorkflowFacade.save(
            codeProject(name), Language.JAVASCRIPT, Environment.PRODUCTION, false);

        assertThat(getLastPublishedVersion(projectId)).isGreaterThan(firstVersion);
        assertThat(getConfiguration(projectId).projectVersion()).isEqualTo(firstVersion);
    }

    @Test
    void testSaveWithoutReleaseCreatesNoConfiguration() {
        String name = "Release None " + UUID.randomUUID();
        long projectId = createDeployedProject(name);

        automationWorkflowProjectCodeWorkflowFacade.save(
            codeProject(name), Language.JAVASCRIPT, Environment.PRODUCTION, false);

        assertThat(automationWorkflowProjectConfigurationAdminFacade.getConfigurations(projectId)).isEmpty();
    }

    private long createDeployedProject(String name) {
        long projectId = automationWorkflowProjectFacade.createProject(name, "", null, List.of(), null);

        automationWorkflowProjectFacade.createProjectWorkflow(projectId, null, null);

        return projectId;
    }

    private AutomationWorkflowProjectConfigurationDTO getConfiguration(long projectId) {
        List<AutomationWorkflowProjectConfigurationDTO> configurations =
            automationWorkflowProjectConfigurationAdminFacade.getConfigurations(projectId);

        assertThat(configurations).hasSize(1);

        return configurations.getFirst();
    }

    private int getLastPublishedVersion(long projectId) {
        return projectService.getProject(projectId)
            .getLastPublishedProjectVersion()
            .getVersion();
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static byte[] codeProject(String name) {
        String source = """
            ({
                name: "%s",
                version: "1",
                description: "A code workflow.",
                workflows: [
                    {
                        name: "charge",
                        label: "Charge",
                        triggers: [],
                        tasks: [
                            {
                                name: "my-task",
                                label: "My Task",
                                perform: function () {
                                    return "hello";
                                }
                            }
                        ]
                    }
                ]
            })
            """.formatted(name);

        return source.getBytes(StandardCharsets.UTF_8);
    }
}
