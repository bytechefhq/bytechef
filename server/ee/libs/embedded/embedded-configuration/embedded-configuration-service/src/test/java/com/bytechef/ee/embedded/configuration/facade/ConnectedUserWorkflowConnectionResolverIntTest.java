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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.exception.ConnectionNotEntitledException;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
        ConnectedUserWorkflowConnectionResolverIntTest.ConnectedUserServiceConfiguration.class
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
    ClusterElementDefinitionService.class, ComponentConnectionFacade.class, ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionLifecycleFacade.class,
    ConnectionService.class, EmbeddedPermissionEvaluator.class, EnvironmentService.class, GitHubProxyClient.class,
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserWorkflowConnectionResolverIntTest {

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver;

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private WorkflowService workflowService;

    private final Map<Long, String> componentNamesByConnectionId = new ConcurrentHashMap<>();
    private final Map<Long, Connection> embeddedConnections = new ConcurrentHashMap<>();
    private long connectedUserId;

    @BeforeEach
    void setUp() {
        connectedUserId = createConnectedUser();

        when(connectionService.getConnections(anyList()))
            .thenAnswer(invocation -> {
                List<Long> connectionIds = invocation.getArgument(0);

                return connectionIds.stream()
                    .map(embeddedConnections::get)
                    .filter(Objects::nonNull)
                    .toList();
            });
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED)))
            .thenAnswer(invocation -> {
                List<Long> connectionIds = invocation.getArgument(0);

                return connectionIds.stream()
                    .filter(componentNamesByConnectionId::containsKey)
                    .map(connectionId -> ConnectionDTO.builder()
                        .componentName(componentNamesByConnectionId.get(connectionId))
                        .id(connectionId)
                        .build())
                    .toList();
            });
    }

    @Test
    void testResolveUsesComponentConnectionKeyAndNodeName() {
        String workflowId = givenWorkflowWithSlots(
            new WorkflowNode("postMessage1", "slack/v1/postMessage",
                new ComponentConnection("slack", 1, "postMessage1", "slack", true)));

        givenOwnedConnection(connectedUserId, 11L, "slack");

        ResolvedWorkflowConnections resolved = connectedUserWorkflowConnectionResolver.resolve(
            workflowId, connectedUserId, Map.of(), List.of());

        assertThat(resolved.connections())
            .containsExactly(new ProjectDeploymentWorkflowConnection(11L, "slack", "postMessage1"));
        assertThat(resolved.isComplete()).isTrue();
    }

    @Test
    void testResolveUsesClusterElementKeyUnderRootNode() {
        String workflowId = givenWorkflowWithSlots(
            new WorkflowNode("aiAgent1", "aiAgent/v1/chat",
                new ComponentConnection("openAi", 1, "aiAgent1", "openAi_1", true)));

        givenOwnedConnection(connectedUserId, 21L, "openAi");

        ResolvedWorkflowConnections resolved = connectedUserWorkflowConnectionResolver.resolve(
            workflowId, connectedUserId, Map.of(), List.of());

        assertThat(resolved.connections())
            .containsExactly(new ProjectDeploymentWorkflowConnection(21L, "openAi_1", "aiAgent1"));
    }

    @Test
    void testResolvePrefersRequestedConnection() {
        String workflowId = givenWorkflowWithSlots(
            new WorkflowNode("postMessage1", "slack/v1/postMessage",
                new ComponentConnection("slack", 1, "postMessage1", "slack", true)));

        givenOwnedConnection(connectedUserId, 11L, "slack");
        givenOwnedConnection(connectedUserId, 12L, "slack");

        ResolvedWorkflowConnections resolved = connectedUserWorkflowConnectionResolver.resolve(
            workflowId, connectedUserId, Map.of("slack", 12L),
            List.of(new ProjectDeploymentWorkflowConnection(11L, "slack", "postMessage1")));

        assertThat(resolved.connections())
            .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactly(12L);
    }

    @Test
    void testResolveKeepsCurrentlyWiredConnectionForTheComponent() {
        String workflowId = givenWorkflowWithSlots(
            new WorkflowNode("postMessage1", "slack/v1/postMessage",
                new ComponentConnection("slack", 1, "postMessage1", "slack", true)),
            new WorkflowNode("postMessage2", "slack/v1/postMessage",
                new ComponentConnection("slack", 1, "postMessage2", "slack", true)));

        givenOwnedConnection(connectedUserId, 11L, "slack");
        givenOwnedConnection(connectedUserId, 12L, "slack");

        ResolvedWorkflowConnections resolved = connectedUserWorkflowConnectionResolver.resolve(
            workflowId, connectedUserId, Map.of(),
            List.of(new ProjectDeploymentWorkflowConnection(12L, "slack", "renamedNode")));

        assertThat(resolved.connections())
            .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactly(12L, 12L);
    }

    @Test
    void testResolveIgnoresAConnectionCurrentlyWiredToAnotherComponent() {
        String workflowId = givenWorkflowWithSlots(
            new WorkflowNode("createIssue1", "jira/v1/createIssue",
                new ComponentConnection("jira", 1, "createIssue1", "jira", true)),
            new WorkflowNode("postMessage1", "slack/v1/postMessage",
                new ComponentConnection("slack", 1, "postMessage1", "slack", true)));

        givenOwnedConnection(connectedUserId, 31L, "jira");
        givenOwnedConnection(connectedUserId, 11L, "slack");
        givenOwnedConnection(connectedUserId, 12L, "slack");

        ResolvedWorkflowConnections resolved = connectedUserWorkflowConnectionResolver.resolve(
            workflowId, connectedUserId, Map.of(),
            List.of(
                new ProjectDeploymentWorkflowConnection(31L, "jira", "createIssue1"),
                new ProjectDeploymentWorkflowConnection(12L, "slack", "postMessage1")));

        assertThat(resolved.connections()).containsExactly(
            new ProjectDeploymentWorkflowConnection(31L, "jira", "createIssue1"),
            new ProjectDeploymentWorkflowConnection(12L, "slack", "postMessage1"));
    }

    @Test
    void testResolveFallsThroughWhenCurrentlyWiredConnectionIsNoLongerEntitled() {
        String workflowId = givenWorkflowWithSlots(
            new WorkflowNode("postMessage1", "slack/v1/postMessage",
                new ComponentConnection("slack", 1, "postMessage1", "slack", true)));

        givenOwnedConnection(connectedUserId, 11L, "slack");

        ResolvedWorkflowConnections resolved = connectedUserWorkflowConnectionResolver.resolve(
            workflowId, connectedUserId, Map.of(),
            List.of(new ProjectDeploymentWorkflowConnection(99L, "slack", "postMessage1")));

        assertThat(resolved.connections())
            .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactly(11L);
    }

    @Test
    void testResolveRejectsRequestedConnectionTheUserIsNotEntitledTo() {
        String workflowId = givenWorkflowWithSlots(
            new WorkflowNode("postMessage1", "slack/v1/postMessage",
                new ComponentConnection("slack", 1, "postMessage1", "slack", true)));

        givenOwnedConnection(connectedUserId, 11L, "slack");
        givenOwnedConnection(createConnectedUser(), 99L, "slack");

        assertThatThrownBy(() -> connectedUserWorkflowConnectionResolver.resolve(
            workflowId, connectedUserId, Map.of("slack", 99L), List.of()))
                .isInstanceOf(ConnectionNotEntitledException.class);
    }

    @Test
    void testResolveReportsMissingRequiredComponentAndSkipsOptionalOne() {
        String workflowId = givenWorkflowWithSlots(
            new WorkflowNode("postMessage1", "slack/v1/postMessage",
                new ComponentConnection("slack", 1, "postMessage1", "slack", true)),
            new WorkflowNode("get1", "httpClient/v1/get",
                new ComponentConnection("httpClient", 1, "get1", "httpClient", false)));

        ResolvedWorkflowConnections resolved = connectedUserWorkflowConnectionResolver.resolve(
            workflowId, connectedUserId, Map.of(), List.of());

        assertThat(resolved.missingComponentNames()).containsExactly("slack");
        assertThat(resolved.firstMissingComponentName()).isEqualTo("slack");
        assertThat(resolved.connections()).isEmpty();
    }

    private long createConnectedUser() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "resolver-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        return Objects.requireNonNull(connectedUser.getId());
    }

    private void givenOwnedConnection(long ownerConnectedUserId, long connectionId, String componentName) {
        componentNamesByConnectionId.put(connectionId, componentName);

        registerEmbeddedConnection(connectionId, Environment.PRODUCTION);

        connectedUserConnectionService.create(ownerConnectedUserId, connectionId);
    }

    private void registerEmbeddedConnection(long connectionId, Environment environment) {
        Connection connection = new Connection();

        connection.setEnvironmentId(environment.ordinal());
        connection.setId(connectionId);
        connection.setType(PlatformType.EMBEDDED);

        embeddedConnections.put(connectionId, connection);
    }

    private String givenWorkflowWithSlots(WorkflowNode... workflowNodes) {
        String tasks = Stream.of(workflowNodes)
            .map(workflowNode -> """
                {"name":"%s","type":"%s","parameters":{}}""".formatted(workflowNode.name(), workflowNode.type()))
            .collect(Collectors.joining(","));

        Workflow workflow = workflowService.create(
            "{\"label\":\"Resolver\",\"triggers\":[],\"tasks\":[" + tasks + "]}",
            Workflow.Format.JSON, Workflow.SourceType.JDBC);

        Map<String, ComponentConnection> slotsByWorkflowNodeName = Stream.of(workflowNodes)
            .collect(Collectors.toMap(WorkflowNode::name, WorkflowNode::slot));

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenAnswer(invocation -> {
                WorkflowTask workflowTask = invocation.getArgument(0);

                return List.of(slotsByWorkflowNodeName.get(workflowTask.getName()));
            });

        return workflow.getId();
    }

    private record WorkflowNode(String name, String type, ComponentConnection slot) {
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }
}
