/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.remote.client.facade.RemoteProjectFacadeClient;
import com.bytechef.ee.automation.configuration.remote.client.registry.RemoteSubflowResolverClient;
import com.bytechef.ee.automation.configuration.remote.client.service.RemoteProjectDeploymentServiceClient;
import com.bytechef.ee.automation.configuration.remote.client.service.RemoteProjectDeploymentWorkflowServiceClient;
import com.bytechef.ee.automation.configuration.remote.client.service.RemoteProjectServiceClient;
import com.bytechef.ee.automation.configuration.remote.client.service.RemoteProjectWorkflowServiceClient;
import com.bytechef.ee.automation.configuration.remote.client.subflow.RemoteChildJobPrincipalFactoryClient;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.ChildJobPrincipalFactoryRemoteServiceGrpcServer;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectDeploymentRemoteServiceGrpcServer;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectDeploymentWorkflowRemoteServiceGrpcServer;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectRemoteServiceGrpcServer;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectWorkflowRemoteServiceGrpcServer;
import com.bytechef.ee.automation.configuration.remote.grpc.SubflowResolverRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AutomationConfigurationRemoteServiceGrpcWireTest {

    private static final String DEFINITION = """
        {"label":"Live Workflow","tasks":[
            {"name":"logger_1","type":"logger/v1/info","parameters":{"text":"hello"}}]}
        """;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();
    private static final String WORKFLOW_ID = "3e4078b4-87bb-43c0-9cc6-1270f73b131d";
    private static final String WORKFLOW_UUID = "8a1b6f0e-5c2d-4e7f-9a3b-2c1d0e9f8a7b";

    private final ChildJobPrincipalFactory childJobPrincipalFactory = mock(ChildJobPrincipalFactory.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ProjectFacade projectFacade = mock(ProjectFacade.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final SubflowResolver subflowResolver = mock(SubflowResolver.class);

    private ManagedChannel managedChannel;
    private RemoteChildJobPrincipalFactoryClient remoteChildJobPrincipalFactoryClient;
    private RemoteProjectDeploymentServiceClient remoteProjectDeploymentServiceClient;
    private RemoteProjectDeploymentWorkflowServiceClient remoteProjectDeploymentWorkflowServiceClient;
    private RemoteProjectFacadeClient remoteProjectFacadeClient;
    private RemoteProjectServiceClient remoteProjectServiceClient;
    private RemoteProjectWorkflowServiceClient remoteProjectWorkflowServiceClient;
    private RemoteSubflowResolverClient remoteSubflowResolverClient;
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
                    new ChildJobPrincipalFactoryRemoteServiceGrpcServer(
                        childJobPrincipalFactory, automationConfigurationProtoMapper),
                    serverInterceptor))
            .addService(
                ServerInterceptors.intercept(
                    new SubflowResolverRemoteServiceGrpcServer(subflowResolver), serverInterceptor))
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
                    new ProjectFacadeRemoteServiceGrpcServer(projectFacade, automationConfigurationProtoMapper),
                    serverInterceptor))
            .addService(
                ServerInterceptors.intercept(
                    new ProjectWorkflowRemoteServiceGrpcServer(
                        projectWorkflowService, automationConfigurationProtoMapper),
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

        remoteChildJobPrincipalFactoryClient = new RemoteChildJobPrincipalFactoryClient(fixedGrpcChannels, JSON_MAPPER);
        remoteProjectDeploymentServiceClient = new RemoteProjectDeploymentServiceClient(
            fixedGrpcChannels, JSON_MAPPER);
        remoteProjectDeploymentWorkflowServiceClient = new RemoteProjectDeploymentWorkflowServiceClient(
            fixedGrpcChannels, JSON_MAPPER);
        remoteProjectFacadeClient = new RemoteProjectFacadeClient(fixedGrpcChannels, JSON_MAPPER);
        remoteProjectServiceClient = new RemoteProjectServiceClient(fixedGrpcChannels, JSON_MAPPER);
        remoteProjectWorkflowServiceClient = new RemoteProjectWorkflowServiceClient(fixedGrpcChannels, JSON_MAPPER);
        remoteSubflowResolverClient = new RemoteSubflowResolverClient(fixedGrpcChannels);
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

    @Test
    void testDeploymentEnabledChecks() {
        when(projectDeploymentService.isProjectDeploymentEnabled(9L)).thenReturn(true);
        when(projectDeploymentService.isProjectDeploymentEnabled(10L)).thenReturn(false);
        when(projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(9L, WORKFLOW_ID)).thenReturn(true);

        assertThat(remoteProjectDeploymentServiceClient.isProjectDeploymentEnabled(9L)).isTrue();
        assertThat(remoteProjectDeploymentServiceClient.isProjectDeploymentEnabled(10L)).isFalse();
        assertThat(remoteProjectDeploymentWorkflowServiceClient.isProjectDeploymentWorkflowEnabled(9L, WORKFLOW_ID))
            .isTrue();
    }

    @Test
    void testProjectWorkflowIds() {
        when(projectWorkflowService.getProjectWorkflowWorkflowId(1051L, "workflow-uuid")).thenReturn("workflow-1");
        when(projectWorkflowService.getLastWorkflowId("workflow-uuid")).thenReturn("workflow-2");

        assertThat(remoteProjectWorkflowServiceClient.getProjectWorkflowWorkflowId(1051L, "workflow-uuid"))
            .isEqualTo("workflow-1");
        assertThat(remoteProjectWorkflowServiceClient.getLastWorkflowId("workflow-uuid")).isEqualTo("workflow-2");
    }

    @Test
    void testGetWorkspaceProjectWorkflows() {
        ProjectWorkflowDTO projectWorkflowDTO = new ProjectWorkflowDTO(
            workflow(WORKFLOW_ID), projectWorkflow(12L, 7L, WORKFLOW_ID), false);

        when(projectFacade.getWorkspaceProjectWorkflows(1051L)).thenReturn(List.of(projectWorkflowDTO));

        assertThat(remoteProjectFacadeClient.getWorkspaceProjectWorkflows(1051L)).singleElement()
            .usingRecursiveComparison()
            .isEqualTo(projectWorkflowDTO);
    }

    @Test
    void testProjectWorkflowLists() {
        ProjectWorkflow projectWorkflow = projectWorkflow(12L, 7L, WORKFLOW_ID);
        ProjectWorkflow otherProjectWorkflow = projectWorkflow(13L, 8L, "workflow-2");

        when(projectWorkflowService.getProjectWorkflowIds(7L)).thenReturn(List.of(WORKFLOW_ID, "workflow-2"));
        when(projectWorkflowService.getProjectWorkflows(List.of(7L, 8L)))
            .thenReturn(List.of(projectWorkflow, otherProjectWorkflow));
        when(projectWorkflowService.getProjectWorkflows(7L, WORKFLOW_UUID)).thenReturn(List.of(projectWorkflow));
        when(projectWorkflowService.getWorkflowProjectWorkflows(List.of(WORKFLOW_ID)))
            .thenReturn(List.of(projectWorkflow));
        when(projectWorkflowService.getWorkflowUuidProjectWorkflows(WORKFLOW_UUID))
            .thenReturn(List.of(projectWorkflow, otherProjectWorkflow));

        assertThat(remoteProjectWorkflowServiceClient.getProjectWorkflowIds(7L))
            .containsExactly(WORKFLOW_ID, "workflow-2");
        assertThat(remoteProjectWorkflowServiceClient.getProjectWorkflows(List.of(7L, 8L)))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(projectWorkflow, otherProjectWorkflow);
        assertThat(remoteProjectWorkflowServiceClient.getProjectWorkflows(7L, WORKFLOW_UUID))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(projectWorkflow);
        assertThat(remoteProjectWorkflowServiceClient.getWorkflowProjectWorkflows(List.of(WORKFLOW_ID)))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(projectWorkflow);
        assertThat(remoteProjectWorkflowServiceClient.getWorkflowUuidProjectWorkflows(WORKFLOW_UUID))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(projectWorkflow, otherProjectWorkflow);
    }

    @Test
    void testGetProjectWorkflowsWithoutAProject() {
        when(projectWorkflowService.getProjectWorkflows(isNull(), eq(WORKFLOW_UUID))).thenReturn(List.of());

        assertThat(remoteProjectWorkflowServiceClient.getProjectWorkflows(null, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testFetchProjectWorkflowWorkflowId() {
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(9L, WORKFLOW_UUID))
            .thenReturn(Optional.of(WORKFLOW_ID));
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(10L, WORKFLOW_UUID)).thenReturn(Optional.empty());

        assertThat(remoteProjectWorkflowServiceClient.fetchProjectWorkflowWorkflowId(9L, WORKFLOW_UUID))
            .contains(WORKFLOW_ID);
        assertThat(remoteProjectWorkflowServiceClient.fetchProjectWorkflowWorkflowId(10L, WORKFLOW_UUID)).isEmpty();
    }

    @Test
    void testGetProjectDeployments() {
        ProjectDeployment projectDeployment = projectDeployment(9L);
        ProjectDeployment otherProjectDeployment = projectDeployment(10L);

        when(projectDeploymentService.getProjectDeployments(List.of(9L, 10L)))
            .thenReturn(List.of(projectDeployment, otherProjectDeployment));
        when(projectDeploymentService.getProjectDeployments(true, Environment.PRODUCTION, 7L, 3L, 1051L))
            .thenReturn(List.of(projectDeployment));
        when(projectDeploymentService.getProjectDeployments(null, null, null, null, 1051L))
            .thenReturn(List.of(otherProjectDeployment));

        assertThat(remoteProjectDeploymentServiceClient.getProjectDeployments(List.of(9L, 10L)))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(projectDeployment, otherProjectDeployment);
        assertThat(remoteProjectDeploymentServiceClient.getProjectDeployments(
            true, Environment.PRODUCTION, 7L, 3L, 1051L))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(projectDeployment);
        assertThat(remoteProjectDeploymentServiceClient.getProjectDeployments(null, null, null, null, 1051L))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(otherProjectDeployment);
    }

    @Test
    void testResolveSubflow() {
        when(subflowResolver.resolveSubflow(WORKFLOW_UUID, "callWorkflow", true))
            .thenReturn(new SubflowResolver.Subflow(WORKFLOW_ID, "trigger_1"));

        assertThat(remoteSubflowResolverClient.resolveSubflow(WORKFLOW_UUID, "callWorkflow", true))
            .isEqualTo(new SubflowResolver.Subflow(WORKFLOW_ID, "trigger_1"));
    }

    @Test
    void testResolveSubflowWithoutACallableTriggerFailsWithItsMessage() {
        when(subflowResolver.resolveSubflow(WORKFLOW_UUID, "callWorkflow", false))
            .thenThrow(new IllegalStateException("No callable trigger found in workflow '%s'".formatted(WORKFLOW_ID)));

        assertThatThrownBy(() -> remoteSubflowResolverClient.resolveSubflow(WORKFLOW_UUID, "callWorkflow", false))
            .hasMessageContaining("No callable trigger found in workflow '%s'".formatted(WORKFLOW_ID));
    }

    @Test
    void testCreateChildJob() {
        AtomicReference<JobParametersDTO> receivedJobParametersDTO = new AtomicReference<>();
        JobParametersDTO jobParametersDTO = new JobParametersDTO(
            WORKFLOW_ID, Map.of("id", 9_007_199_254_740_993L, "name", "child"));

        when(childJobPrincipalFactory.createChildJob(eq(7L), any()))
            .thenAnswer(invocation -> {
                receivedJobParametersDTO.set(invocation.getArgument(1));

                return 15L;
            });

        assertThat(remoteChildJobPrincipalFactoryClient.createChildJob(7L, jobParametersDTO)).isEqualTo(15L);
        assertThat(receivedJobParametersDTO.get()).usingRecursiveComparison()
            .isEqualTo(jobParametersDTO);
    }

    private static ProjectDeployment projectDeployment(long id) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setId(id);
        projectDeployment.setName("Live Deployment");
        projectDeployment.setProjectId(7L);
        projectDeployment.setProjectVersion(2);
        projectDeployment.setTagIds(List.of(3L));

        return projectDeployment;
    }

    private static ProjectWorkflow projectWorkflow(long id, long projectId, String workflowId) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(projectId, 2, workflowId, UUID.fromString(WORKFLOW_UUID));

        ReflectionTestUtils.setField(projectWorkflow, "id", id);
        ReflectionTestUtils.setField(projectWorkflow, "createdBy", "admin@localhost.com");
        ReflectionTestUtils.setField(projectWorkflow, "createdDate", Instant.parse("2026-10-26T11:00:00Z"));

        projectWorkflow.setVersion(4);

        return projectWorkflow;
    }

    private static Workflow workflow(String id) {
        Workflow workflow = new Workflow(
            id, DEFINITION, Workflow.Format.JSON, Instant.parse("2026-10-26T11:00:00Z"), Map.of("source", "test"));

        workflow.setVersion(3);

        return workflow;
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
