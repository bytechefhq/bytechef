/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.remote.client.service.RemoteProjectDeploymentServiceClient;
import com.bytechef.ee.automation.configuration.remote.client.service.RemoteProjectDeploymentWorkflowServiceClient;
import com.bytechef.ee.automation.configuration.remote.client.service.RemoteProjectServiceClient;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectDeploymentRemoteServiceGrpcServer;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectDeploymentWorkflowRemoteServiceGrpcServer;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.configuration.domain.Environment;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationConfigurationRemoteServiceGrpcWireTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();
    private static final String WORKFLOW_ID = "3e4078b4-87bb-43c0-9cc6-1270f73b131d";

    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ProjectService projectService = mock(ProjectService.class);

    private ManagedChannel managedChannel;
    private RemoteProjectDeploymentServiceClient remoteProjectDeploymentServiceClient;
    private RemoteProjectDeploymentWorkflowServiceClient remoteProjectDeploymentWorkflowServiceClient;
    private RemoteProjectServiceClient remoteProjectServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();
        AutomationConfigurationProtoMapper automationConfigurationProtoMapper =
            new AutomationConfigurationProtoMapper(JSON_MAPPER);
        ServerInterceptor serverInterceptor = new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler());

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ProjectRemoteServiceGrpcServer(projectService, automationConfigurationProtoMapper),
                    serverInterceptor))
            .addService(
                ServerInterceptors.intercept(
                    new ProjectDeploymentRemoteServiceGrpcServer(
                        projectDeploymentService, automationConfigurationProtoMapper),
                    serverInterceptor))
            .addService(
                ServerInterceptors.intercept(
                    new ProjectDeploymentWorkflowRemoteServiceGrpcServer(
                        projectDeploymentWorkflowService, automationConfigurationProtoMapper),
                    serverInterceptor))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        FixedGrpcChannels fixedGrpcChannels = new FixedGrpcChannels(managedChannel);

        remoteProjectDeploymentServiceClient = new RemoteProjectDeploymentServiceClient(
            fixedGrpcChannels, JSON_MAPPER);
        remoteProjectDeploymentWorkflowServiceClient = new RemoteProjectDeploymentWorkflowServiceClient(
            fixedGrpcChannels, JSON_MAPPER);
        remoteProjectServiceClient = new RemoteProjectServiceClient(fixedGrpcChannels, JSON_MAPPER);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testProjectCalls() {
        Project project = project(7L);

        when(projectService.getProject(7L)).thenReturn(project);
        when(projectService.getProjectDeploymentProject(9L)).thenReturn(project);
        when(projectService.getProjects()).thenReturn(List.of(project, project(8L)));
        when(projectService.getWorkflowProject(WORKFLOW_ID)).thenReturn(project);

        assertThat(remoteProjectServiceClient.getProject(7L)).usingRecursiveComparison()
            .isEqualTo(project);
        assertThat(remoteProjectServiceClient.getProjectDeploymentProject(9L)).usingRecursiveComparison()
            .isEqualTo(project);
        assertThat(remoteProjectServiceClient.getProjects()).extracting(Project::getId)
            .containsExactly(7L, 8L);
        assertThat(remoteProjectServiceClient.getWorkflowProject(WORKFLOW_ID)).usingRecursiveComparison()
            .isEqualTo(project);
    }

    @Test
    void testGetUnknownProjectFailsWithItsMessage() {
        when(projectService.getProject(7L)).thenThrow(new IllegalArgumentException("Project 7 not found"));

        assertThatThrownBy(() -> remoteProjectServiceClient.getProject(7L))
            .hasMessageContaining("Project 7 not found");
    }

    @Test
    void testGetProjectDeployment() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setId(9L);
        projectDeployment.setName("Live Deployment");
        projectDeployment.setProjectId(7L);

        when(projectDeploymentService.getProjectDeployment(9L)).thenReturn(projectDeployment);

        assertThat(remoteProjectDeploymentServiceClient.getProjectDeployment(9L)).usingRecursiveComparison()
            .isEqualTo(projectDeployment);
    }

    @Test
    void testGetProjectDeploymentWorkflow() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(List.of(connection(4L)));
        projectDeploymentWorkflow.setId(11L);
        projectDeploymentWorkflow.setInputs(Map.of("id", 9_007_199_254_740_993L));
        projectDeploymentWorkflow.setProjectDeploymentId(9L);
        projectDeploymentWorkflow.setWorkflowId(WORKFLOW_ID);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(9L, WORKFLOW_ID))
            .thenReturn(projectDeploymentWorkflow);

        assertThat(remoteProjectDeploymentWorkflowServiceClient.getProjectDeploymentWorkflow(9L, WORKFLOW_ID))
            .usingRecursiveComparison()
            .isEqualTo(projectDeploymentWorkflow);
    }

    @Test
    void testProjectDeploymentWorkflowConnections() {
        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflowConnection(
            9L, WORKFLOW_ID, "airtable_1", "airtable"))
                .thenReturn(Optional.of(connection(4L)));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflowConnections(9L, WORKFLOW_ID, "airtable_1"))
            .thenReturn(List.of(connection(4L), connection(5L)));

        assertThat(remoteProjectDeploymentWorkflowServiceClient.fetchProjectDeploymentWorkflowConnection(
            9L, WORKFLOW_ID, "airtable_1", "airtable"))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(connection(4L));
        assertThat(remoteProjectDeploymentWorkflowServiceClient.getProjectDeploymentWorkflowConnections(
            9L, WORKFLOW_ID, "airtable_1"))
                .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
                .containsExactly(4L, 5L);
    }

    @Test
    void testFetchMissingConnectionIsEmpty() {
        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflowConnection(
            9L, WORKFLOW_ID, "airtable_1", "airtable"))
                .thenReturn(Optional.empty());

        assertThat(remoteProjectDeploymentWorkflowServiceClient.fetchProjectDeploymentWorkflowConnection(
            9L, WORKFLOW_ID, "airtable_1", "airtable")).isEmpty();
    }

    @Test
    void testIsConnectionUsed() {
        when(projectDeploymentWorkflowService.isConnectionUsed(7L)).thenReturn(true);
        when(projectDeploymentWorkflowService.isConnectionUsed(8L)).thenReturn(false);

        assertThat(remoteProjectDeploymentWorkflowServiceClient.isConnectionUsed(7L)).isTrue();
        assertThat(remoteProjectDeploymentWorkflowServiceClient.isConnectionUsed(8L)).isFalse();
    }

    private static ProjectDeploymentWorkflowConnection connection(long connectionId) {
        return new ProjectDeploymentWorkflowConnection(connectionId, "airtable", "airtable_1");
    }

    private static Project project(long id) {
        Project project = new Project();

        project.setId(id);
        project.setName("Live Project");
        project.setUuid(WORKFLOW_ID);

        return project;
    }
}
