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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.domain.Option;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        ConnectedUserIntegrationInstanceFacadeIntTest.ConnectedUserServiceConfiguration.class
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
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserIntegrationInstanceFacadeIntTest {

    private static final long CONNECTION_ID = 42L;

    private static final String WORKFLOW_DEFINITION = """
        {"label":"Sync contacts","inputs":[],"triggers":[],"tasks":[]}
        """;

    @Autowired
    private ComponentDefinitionFacade componentDefinitionFacade;

    @Autowired
    private ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private WorkflowService workflowService;

    private long integrationInstanceConfigurationWorkflowId;
    private long integrationInstanceId;
    private String otherExternalUserId;
    private String ownerExternalUserId;
    private String workflowId;
    private String workflowUuid;

    @BeforeEach
    void beforeEach() {
        ownerExternalUserId = "owner-" + UUID.randomUUID();
        otherExternalUserId = "other-" + UUID.randomUUID();

        ConnectedUser ownerConnectedUser = connectedUserService.createConnectedUser(
            ownerExternalUserId, Environment.PRODUCTION);

        connectedUserService.createConnectedUser(otherExternalUserId, Environment.PRODUCTION);

        Integration integration = new Integration();

        integration.setComponentName("slack");
        integration.setName("Slack " + UUID.randomUUID());

        integration = integrationService.create(integration);

        Workflow workflow = workflowService.create(WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        workflowId = workflow.getId();

        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.addWorkflow(
            integration.getId(), 1, workflowId);

        workflowUuid = integrationWorkflow.getUuidAsString();

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

        integrationInstanceConfigurationWorkflow = integrationInstanceConfigurationWorkflowService.create(
            integrationInstanceConfigurationWorkflow);

        integrationInstanceConfigurationWorkflowId = integrationInstanceConfigurationWorkflow.getId();

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            ownerConnectedUser.getId(), CONNECTION_ID, integrationInstanceConfiguration.getId());

        integrationInstanceId = integrationInstance.getId();
    }

    @Test
    void testGetComponentInputOptionsAllowedForOwner() {
        Option option = mock(Option.class);

        when(componentDefinitionFacade.executeWorkflowInputOptions(
            anyString(), anyInt(), anyString(), anyString(), any(), any(), any(), anyLong()))
                .thenReturn(List.of(option));

        List<Option> options = connectedUserIntegrationInstanceFacade.getComponentInputOptions(
            ownerExternalUserId, integrationInstanceId, "slack", 1, "channel", "channelId", Map.of(), null);

        assertThat(options).containsExactly(option);

        verify(componentDefinitionFacade).executeWorkflowInputOptions(
            eq("slack"), eq(1), eq("channel"), eq("channelId"), any(), any(), any(), eq(CONNECTION_ID));
    }

    @Test
    void testGetComponentInputOptionsDeniedForNonOwner() {
        List<Option> options = connectedUserIntegrationInstanceFacade.getComponentInputOptions(
            otherExternalUserId, integrationInstanceId, "slack", 1, "channel", "channelId", Map.of(), null);

        assertThat(options).isEmpty();

        verifyNoInteractions(componentDefinitionFacade);
    }

    @Test
    void testGetComponentInputOptionsDeniedWhenConnectedUserAbsent() {
        String developmentOnlyExternalUserId = createDevelopmentOnlyConnectedUser();

        List<Option> options = connectedUserIntegrationInstanceFacade.getComponentInputOptions(
            developmentOnlyExternalUserId, integrationInstanceId, "slack", 1, "channel", "channelId", Map.of(),
            null);

        assertThat(options).isEmpty();

        verifyNoInteractions(componentDefinitionFacade);
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowAllowedForOwner() {
        connectedUserIntegrationInstanceFacade.updateIntegrationInstanceWorkflow(
            ownerExternalUserId, integrationInstanceId, workflowUuid, Map.of("key", "value"));

        IntegrationInstanceWorkflow integrationInstanceWorkflow = integrationInstanceWorkflowService
            .getIntegrationInstanceWorkflow(integrationInstanceId, workflowId);

        assertThat(integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId())
            .isEqualTo(integrationInstanceConfigurationWorkflowId);
        assertThat(integrationInstanceWorkflow.getInputs()).isEqualTo(Map.of("key", "value"));
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowDeniedForNonOwner() {
        assertThatThrownBy(
            () -> connectedUserIntegrationInstanceFacade.updateIntegrationInstanceWorkflow(
                otherExternalUserId, integrationInstanceId, workflowUuid, Map.of("key", "value")))
                    .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        assertThat(fetchIntegrationInstanceWorkflow()).isEmpty();
    }

    @Test
    void testEnableIntegrationInstanceWorkflowAllowedForOwner() {
        connectedUserIntegrationInstanceFacade.enableIntegrationInstanceWorkflow(
            ownerExternalUserId, integrationInstanceId, workflowUuid);

        assertThat(fetchIntegrationInstanceWorkflow())
            .hasValueSatisfying(integrationInstanceWorkflow -> assertThat(integrationInstanceWorkflow.isEnabled())
                .isTrue());
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDeniedForNonOwner() {
        assertThatThrownBy(
            () -> connectedUserIntegrationInstanceFacade.enableIntegrationInstanceWorkflow(
                otherExternalUserId, integrationInstanceId, workflowUuid))
                    .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        assertThat(fetchIntegrationInstanceWorkflow()).isEmpty();
    }

    @Test
    void testDisableIntegrationInstanceWorkflowAllowedForOwner() {
        createEnabledIntegrationInstanceWorkflow();

        connectedUserIntegrationInstanceFacade.disableIntegrationInstanceWorkflow(
            ownerExternalUserId, integrationInstanceId, workflowUuid);

        assertThat(fetchIntegrationInstanceWorkflow())
            .hasValueSatisfying(integrationInstanceWorkflow -> assertThat(integrationInstanceWorkflow.isEnabled())
                .isFalse());
    }

    @Test
    void testDisableIntegrationInstanceWorkflowDeniedForNonOwner() {
        createEnabledIntegrationInstanceWorkflow();

        assertThatThrownBy(
            () -> connectedUserIntegrationInstanceFacade.disableIntegrationInstanceWorkflow(
                otherExternalUserId, integrationInstanceId, workflowUuid))
                    .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        assertThat(fetchIntegrationInstanceWorkflow())
            .hasValueSatisfying(integrationInstanceWorkflow -> assertThat(integrationInstanceWorkflow.isEnabled())
                .isTrue());
    }

    @Test
    void testValidateIntegrationInstanceOwnershipAllowedForOwner() {
        assertThatCode(
            () -> connectedUserIntegrationInstanceFacade.validateIntegrationInstanceOwnership(
                ownerExternalUserId, integrationInstanceId))
                    .doesNotThrowAnyException();
    }

    @Test
    void testValidateIntegrationInstanceOwnershipDeniedForNonOwner() {
        assertThatThrownBy(
            () -> connectedUserIntegrationInstanceFacade.validateIntegrationInstanceOwnership(
                otherExternalUserId, integrationInstanceId))
                    .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);
    }

    @Test
    void testValidateIntegrationInstanceOwnershipDeniedWhenConnectedUserAbsent() {
        String developmentOnlyExternalUserId = createDevelopmentOnlyConnectedUser();

        assertThatThrownBy(
            () -> connectedUserIntegrationInstanceFacade.validateIntegrationInstanceOwnership(
                developmentOnlyExternalUserId, integrationInstanceId))
                    .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);
    }

    private String createDevelopmentOnlyConnectedUser() {
        String developmentOnlyExternalUserId = "development-only-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(developmentOnlyExternalUserId, Environment.DEVELOPMENT);

        return developmentOnlyExternalUserId;
    }

    private void createEnabledIntegrationInstanceWorkflow() {
        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                integrationInstanceId, integrationInstanceConfigurationWorkflowId);

        integrationInstanceWorkflowService.updateEnabled(integrationInstanceWorkflow.getId(), true);
    }

    private Optional<IntegrationInstanceWorkflow> fetchIntegrationInstanceWorkflow() {
        return integrationInstanceWorkflowService.fetchIntegrationInstanceWorkflow(integrationInstanceId, workflowId);
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }
}
