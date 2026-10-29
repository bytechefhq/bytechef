/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationIntTestData;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationRemoteClientIntTestConfiguration;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectWorkflowRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.configuration.domain.Environment;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationConfigurationRemoteClientIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true",
        "spring.liquibase.contexts=configuration,connection,execution,user"
    })
class RemoteProjectWorkflowServiceClientIntTest {

    private static final UUID OTHER_WORKFLOW_UUID = UUID.fromString("3e4078b4-87bb-43c0-9cc6-1270f73b131d");
    private static final UUID WORKFLOW_UUID = UUID.fromString("8a1b6f0e-5c2d-4e7f-9a3b-2c1d0e9f8a7b");

    @Autowired
    private AutomationConfigurationIntTestData automationConfigurationIntTestData;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    private ProjectWorkflow draftProjectWorkflow;
    private ProjectDeployment draftProjectDeployment;
    private ManagedChannel managedChannel;
    private Project otherProject;
    private ProjectWorkflow otherProjectWorkflow;
    private Project project;
    private ProjectWorkflow publishedProjectWorkflow;
    private ProjectDeployment publishedProjectDeployment;
    private RemoteProjectWorkflowServiceClient remoteProjectWorkflowServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ProjectWorkflowRemoteServiceGrpcServer(
                        projectWorkflowService, new AutomationConfigurationProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteProjectWorkflowServiceClient = new RemoteProjectWorkflowServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);

        Workspace workspace = automationConfigurationIntTestData.createWorkspace("workspace");

        project = automationConfigurationIntTestData.createProject("Live Project", workspace.getId());

        publishedProjectWorkflow = automationConfigurationIntTestData.createProjectWorkflow(
            project.getId(), 1, "Published", WORKFLOW_UUID);

        int draftProjectVersion = automationConfigurationIntTestData.publishProject(project.getId());

        draftProjectWorkflow = automationConfigurationIntTestData.createProjectWorkflow(
            project.getId(), draftProjectVersion, "Draft", WORKFLOW_UUID);

        publishedProjectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Published", project.getId(), 1, Environment.PRODUCTION, List.of());
        draftProjectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Draft", project.getId(), draftProjectVersion, Environment.STAGING, List.of());

        otherProject = automationConfigurationIntTestData.createProject("Other Project", workspace.getId());

        otherProjectWorkflow = automationConfigurationIntTestData.createProjectWorkflow(
            otherProject.getId(), 1, "Other", OTHER_WORKFLOW_UUID);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        automationConfigurationIntTestData.deleteAll();
    }

    @Test
    void testFetchProjectWorkflowWorkflowIdResolvesTheDeployedVersion() {
        assertThat(
            remoteProjectWorkflowServiceClient.fetchProjectWorkflowWorkflowId(
                publishedProjectDeployment.getId(), WORKFLOW_UUID.toString()))
                    .contains(publishedProjectWorkflow.getWorkflowId());
        assertThat(
            remoteProjectWorkflowServiceClient.fetchProjectWorkflowWorkflowId(
                draftProjectDeployment.getId(), WORKFLOW_UUID.toString()))
                    .contains(draftProjectWorkflow.getWorkflowId());
        assertThat(
            remoteProjectWorkflowServiceClient.fetchProjectWorkflowWorkflowId(
                publishedProjectDeployment.getId(), OTHER_WORKFLOW_UUID.toString()))
                    .isEmpty();
    }

    @Test
    void testGetProjectWorkflowWorkflowIdResolvesTheDeployedVersion() {
        assertThat(
            remoteProjectWorkflowServiceClient.getProjectWorkflowWorkflowId(
                draftProjectDeployment.getId(), WORKFLOW_UUID.toString()))
                    .isEqualTo(draftProjectWorkflow.getWorkflowId());

        assertThatThrownBy(
            () -> remoteProjectWorkflowServiceClient.getProjectWorkflowWorkflowId(
                draftProjectDeployment.getId(), OTHER_WORKFLOW_UUID.toString()))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessage("INVALID_ARGUMENT: ProjectWorkflow not found");
    }

    @Test
    void testGetLastWorkflowIdReturnsTheHighestProjectVersion() {
        assertThat(remoteProjectWorkflowServiceClient.getLastWorkflowId(WORKFLOW_UUID.toString()))
            .isEqualTo(draftProjectWorkflow.getWorkflowId());

        String unknownWorkflowUuid = UUID.randomUUID()
            .toString();

        assertThatThrownBy(() -> remoteProjectWorkflowServiceClient.getLastWorkflowId(unknownWorkflowUuid))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessage("INVALID_ARGUMENT: No workflow found for workflow uuid " + unknownWorkflowUuid);
    }

    @Test
    void testGetProjectWorkflowsByProjectAndUuidReturnsEveryVersion() {
        assertThat(remoteProjectWorkflowServiceClient.getProjectWorkflows(project.getId(), WORKFLOW_UUID.toString()))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                storedProjectWorkflow(publishedProjectWorkflow), storedProjectWorkflow(draftProjectWorkflow));
        assertThat(
            remoteProjectWorkflowServiceClient.getProjectWorkflows(otherProject.getId(), WORKFLOW_UUID.toString()))
                .isEmpty();
        assertThat(remoteProjectWorkflowServiceClient.getProjectWorkflows(null, WORKFLOW_UUID.toString()))
            .isEmpty();
    }

    @Test
    void testGetProjectWorkflowIdsReturnsTheWorkflowsOfEveryProjectVersion() {
        assertThat(remoteProjectWorkflowServiceClient.getProjectWorkflowIds(project.getId()))
            .containsExactlyInAnyOrder(publishedProjectWorkflow.getWorkflowId(), draftProjectWorkflow.getWorkflowId());
        assertThat(remoteProjectWorkflowServiceClient.getProjectWorkflowIds(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void testGetProjectWorkflowsByProjectIdsReturnsTheWorkflowsOfThoseProjects() {
        assertThat(remoteProjectWorkflowServiceClient.getProjectWorkflows(List.of(project.getId())))
            .extracting(ProjectWorkflow::getId)
            .containsExactlyInAnyOrder(publishedProjectWorkflow.getId(), draftProjectWorkflow.getId());
        assertThat(
            remoteProjectWorkflowServiceClient.getProjectWorkflows(List.of(project.getId(), otherProject.getId())))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                    storedProjectWorkflow(publishedProjectWorkflow), storedProjectWorkflow(draftProjectWorkflow),
                    storedProjectWorkflow(otherProjectWorkflow));
    }

    @Test
    void testGetWorkflowProjectWorkflowsReturnsTheProjectWorkflowsOfThoseWorkflows() {
        assertThat(
            remoteProjectWorkflowServiceClient.getWorkflowProjectWorkflows(
                List.of(draftProjectWorkflow.getWorkflowId(), otherProjectWorkflow.getWorkflowId())))
                    .extracting(ProjectWorkflow::getId)
                    .containsExactlyInAnyOrder(draftProjectWorkflow.getId(), otherProjectWorkflow.getId());
        assertThat(remoteProjectWorkflowServiceClient.getWorkflowProjectWorkflows(List.of())).isEmpty();
    }

    @Test
    void testGetWorkflowUuidProjectWorkflowsReturnsEveryVersionOfTheWorkflow() {
        assertThat(remoteProjectWorkflowServiceClient.getWorkflowUuidProjectWorkflows(WORKFLOW_UUID.toString()))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                storedProjectWorkflow(publishedProjectWorkflow), storedProjectWorkflow(draftProjectWorkflow));
        assertThat(remoteProjectWorkflowServiceClient.getWorkflowUuidProjectWorkflows("not-a-uuid")).isEmpty();
    }

    private ProjectWorkflow storedProjectWorkflow(ProjectWorkflow projectWorkflow) {
        return projectWorkflowService.getProjectWorkflow(projectWorkflow.getId());
    }
}
