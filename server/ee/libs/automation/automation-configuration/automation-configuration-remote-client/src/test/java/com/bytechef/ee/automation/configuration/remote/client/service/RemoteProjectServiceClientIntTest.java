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
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationIntTestData;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationRemoteClientIntTestConfiguration;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectRemoteServiceGrpcServer;
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
class RemoteProjectServiceClientIntTest {

    @Autowired
    private AutomationConfigurationIntTestData automationConfigurationIntTestData;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectService projectService;

    private ManagedChannel managedChannel;
    private RemoteProjectServiceClient remoteProjectServiceClient;
    private Server server;
    private Workspace workspace;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ProjectRemoteServiceGrpcServer(
                        projectService, new AutomationConfigurationProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteProjectServiceClient = new RemoteProjectServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);

        workspace = automationConfigurationIntTestData.createWorkspace("workspace");
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        automationConfigurationIntTestData.deleteAll();
    }

    @Test
    void testGetProjectReturnsTheStoredProject() {
        Project project = automationConfigurationIntTestData.createProject("Live Project", workspace.getId());

        automationConfigurationIntTestData.publishProject(project.getId());

        Project storedProject = projectService.getProject(project.getId());

        Project fetchedProject = remoteProjectServiceClient.getProject(project.getId());

        assertThat(fetchedProject.getId()).isEqualTo(project.getId());
        assertThat(fetchedProject.getName()).isEqualTo("Live Project");
        assertThat(fetchedProject.getUuid()).isEqualTo(storedProject.getUuid());
        assertThat(fetchedProject.getWorkspaceId()).isEqualTo(workspace.getId());
        assertThat(fetchedProject.getLastProjectVersion()).isEqualTo(2);
        assertThat(fetchedProject.isPublished()).isTrue();
        assertThat(fetchedProject.getVersion()).isEqualTo(storedProject.getVersion());
    }

    @Test
    void testGetUnknownProjectFailsWithTheServerError() {
        assertThatThrownBy(() -> remoteProjectServiceClient.getProject(Long.MAX_VALUE))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessage("INTERNAL: NoSuchElementException: Project " + Long.MAX_VALUE + " not found");
    }

    @Test
    void testGetProjectDeploymentProjectReturnsTheDeployedProject() {
        Project project = automationConfigurationIntTestData.createProject("Deployed Project", workspace.getId());

        automationConfigurationIntTestData.createProject("Other Project", workspace.getId());

        ProjectDeployment projectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Live Deployment", project.getId(), 1, Environment.PRODUCTION, List.of());

        assertThat(remoteProjectServiceClient.getProjectDeploymentProject(projectDeployment.getId()))
            .extracting(Project::getId, Project::getName)
            .containsExactly(project.getId(), "Deployed Project");
    }

    @Test
    void testGetProjectsReturnsEveryProjectSortedByName() {
        Project betaProject = automationConfigurationIntTestData.createProject("Beta", workspace.getId());
        Project alphaProject = automationConfigurationIntTestData.createProject("Alpha", workspace.getId());

        assertThat(remoteProjectServiceClient.getProjects())
            .extracting(Project::getId)
            .containsExactly(alphaProject.getId(), betaProject.getId());
    }

    @Test
    void testGetWorkflowProjectReturnsTheProjectHoldingTheWorkflow() {
        Project project = automationConfigurationIntTestData.createProject("Workflow Project", workspace.getId());

        automationConfigurationIntTestData.createProject("Other Project", workspace.getId());

        ProjectWorkflow projectWorkflow = automationConfigurationIntTestData.createProjectWorkflow(
            project.getId(), 1, "Live Workflow", UUID.randomUUID());

        assertThat(remoteProjectServiceClient.getWorkflowProject(projectWorkflow.getWorkflowId()))
            .extracting(Project::getId, Project::getName)
            .containsExactly(project.getId(), "Workflow Project");
    }
}
