/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
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
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
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
        ConnectedUserComponentInputFacadeIntTest.ConnectedUserServiceConfiguration.class
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
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationComponentService.class,
    McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserComponentInputFacadeIntTest {

    private static final AtomicLong NEXT_CONNECTION_ID = new AtomicLong(93000L);

    @Autowired
    private ComponentDefinitionFacade componentDefinitionFacade;

    @Autowired
    private ConnectedUserComponentInputFacade connectedUserComponentInputFacade;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

    private final Map<Long, Connection> connections = new HashMap<>();
    private final Map<Long, String> connectionComponentNames = new HashMap<>();

    private Long foreignConnectionId;
    private Long githubConnectionId;
    private String ownerExternalUserId;
    private Long slackConnectionId;

    @BeforeEach
    void beforeEach() {
        when(connectionService.getConnections(anyList())).thenAnswer(invocation -> {
            List<Long> requestedConnectionIds = invocation.getArgument(0);

            return requestedConnectionIds.stream()
                .map(connections::get)
                .filter(Objects::nonNull)
                .toList();
        });

        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED))).thenAnswer(invocation -> {
            List<Long> requestedConnectionIds = invocation.getArgument(0);

            return requestedConnectionIds.stream()
                .map(connectionId -> ConnectionDTO.builder()
                    .componentName(connectionComponentNames.get(connectionId))
                    .id(connectionId)
                    .build())
                .toList();
        });

        foreignConnectionId = NEXT_CONNECTION_ID.incrementAndGet();
        githubConnectionId = NEXT_CONNECTION_ID.incrementAndGet();
        ownerExternalUserId = "owner-" + UUID.randomUUID();
        slackConnectionId = NEXT_CONNECTION_ID.incrementAndGet();

        ConnectedUser ownerConnectedUser = connectedUserService.createConnectedUser(
            ownerExternalUserId, Environment.PRODUCTION);
        ConnectedUser otherConnectedUser = connectedUserService.createConnectedUser(
            "other-" + UUID.randomUUID(), Environment.PRODUCTION);

        createConnectedUserConnection(ownerConnectedUser.getId(), slackConnectionId, "slack");
        createConnectedUserConnection(ownerConnectedUser.getId(), githubConnectionId, "github");
        createConnectedUserConnection(otherConnectedUser.getId(), foreignConnectionId, "slack");
    }

    @Test
    void testGetComponentInputOptionsRunsTheOptionsFunctionOnTheConnectedUsersConnection() {
        Option option = mock(Option.class);

        when(componentDefinitionFacade.executeWorkflowInputOptions(
            eq("slack"), eq(1), eq("channel"), eq("channel"), any(), any(), any(), anyLong()))
                .thenReturn(List.of(option));

        List<Option> options = connectedUserComponentInputFacade.getComponentInputOptions(
            ownerExternalUserId, Environment.PRODUCTION, "slack", 1, "channel", "channel", Map.of("team", "T1"), "gen",
            slackConnectionId);

        assertThat(options).containsExactly(option);

        verify(componentDefinitionFacade).executeWorkflowInputOptions(
            "slack", 1, "channel", "channel", Map.of("team", "T1"), List.of("team"), "gen", slackConnectionId);
    }

    @Test
    void testGetComponentInputOptionsWithAnotherConnectedUsersConnectionIsEmpty() {
        List<Option> options = connectedUserComponentInputFacade.getComponentInputOptions(
            ownerExternalUserId, Environment.PRODUCTION, "slack", 1, "channel", "channel", Map.of(), null,
            foreignConnectionId);

        assertThat(options).isEmpty();

        verifyNoInteractions(componentDefinitionFacade);
    }

    @Test
    void testGetComponentInputOptionsWithAConnectionOfAnotherComponentIsEmpty() {
        List<Option> options = connectedUserComponentInputFacade.getComponentInputOptions(
            ownerExternalUserId, Environment.PRODUCTION, "slack", 1, "channel", "channel", Map.of(), null,
            githubConnectionId);

        assertThat(options).isEmpty();

        verifyNoInteractions(componentDefinitionFacade);
    }

    @Test
    void testGetComponentInputOptionsInAnEnvironmentWithoutTheConnectedUserIsEmpty() {
        List<Option> options = connectedUserComponentInputFacade.getComponentInputOptions(
            ownerExternalUserId, Environment.DEVELOPMENT, "slack", 1, "channel", "channel", Map.of(), null,
            slackConnectionId);

        assertThat(options).isEmpty();

        verifyNoInteractions(componentDefinitionFacade);
    }

    private void createConnectedUserConnection(long connectedUserId, long connectionId, String componentName) {
        Connection connection = new Connection();

        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setId(connectionId);
        connection.setType(PlatformType.EMBEDDED);

        connections.put(connectionId, connection);
        connectionComponentNames.put(connectionId, componentName);

        connectedUserConnectionService.create(connectedUserId, connectionId);
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }
}
