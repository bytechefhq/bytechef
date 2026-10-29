/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationIntTestData;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationRemoteClientIntTestConfiguration;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import io.grpc.ManagedChannel;
import io.grpc.Server;
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
class RemoteProjectFacadeClientIntTest {

    @Autowired
    private AutomationConfigurationIntTestData automationConfigurationIntTestData;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectFacade projectFacade;

    private ManagedChannel managedChannel;
    private RemoteProjectFacadeClient remoteProjectFacadeClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                new ProjectFacadeRemoteServiceGrpcServer(
                    projectFacade, new AutomationConfigurationProtoMapper(objectMapper)))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteProjectFacadeClient = new RemoteProjectFacadeClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        automationConfigurationIntTestData.deleteAll();
    }

    @Test
    void testGetWorkspaceProjectWorkflowsReturnsTheWorkflowsOfTheWorkspaceProjects() {
        Workspace workspace = automationConfigurationIntTestData.createWorkspace("workspace");
        Workspace otherWorkspace = automationConfigurationIntTestData.createWorkspace("other workspace");

        Project project = automationConfigurationIntTestData.createProject("Live Project", workspace.getId());
        Project secondProject = automationConfigurationIntTestData.createProject("Second Project", workspace.getId());
        Project systemProject = automationConfigurationIntTestData.createProject(
            "__EMBEDDED_AUTOMATION__System", workspace.getId());
        Project otherWorkspaceProject = automationConfigurationIntTestData.createProject(
            "Other Workspace Project", otherWorkspace.getId());

        ProjectWorkflow firstProjectWorkflow = automationConfigurationIntTestData.createProjectWorkflow(
            project.getId(), 1, "First", UUID.randomUUID());
        ProjectWorkflow secondProjectWorkflow = automationConfigurationIntTestData.createProjectWorkflow(
            project.getId(), 1, "Second", UUID.randomUUID());
        ProjectWorkflow thirdProjectWorkflow = automationConfigurationIntTestData.createProjectWorkflow(
            secondProject.getId(), 1, "Third", UUID.randomUUID());

        automationConfigurationIntTestData.createProjectWorkflow(systemProject.getId(), 1, "System", UUID.randomUUID());
        automationConfigurationIntTestData.createProjectWorkflow(
            otherWorkspaceProject.getId(), 1, "Other", UUID.randomUUID());

        List<ProjectWorkflowDTO> projectWorkflowDTOs = remoteProjectFacadeClient.getWorkspaceProjectWorkflows(
            workspace.getId());

        assertThat(projectWorkflowDTOs)
            .extracting(
                ProjectWorkflowDTO::getId, ProjectWorkflowDTO::getLabel, ProjectWorkflowDTO::getProjectWorkflowId,
                ProjectWorkflowDTO::getProjectVersion, ProjectWorkflowDTO::getWorkflowUuid)
            .containsExactlyInAnyOrder(
                tuple(
                    firstProjectWorkflow.getWorkflowId(), "First", firstProjectWorkflow.getId(), 1,
                    firstProjectWorkflow.getUuidAsString()),
                tuple(
                    secondProjectWorkflow.getWorkflowId(), "Second", secondProjectWorkflow.getId(), 1,
                    secondProjectWorkflow.getUuidAsString()),
                tuple(
                    thirdProjectWorkflow.getWorkflowId(), "Third", thirdProjectWorkflow.getId(), 1,
                    thirdProjectWorkflow.getUuidAsString()));
        assertThat(projectWorkflowDTOs)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                "createdBy", "createdDate", "lastModifiedBy", "workflow.createdBy", "workflow.createdDate",
                "workflow.lastModifiedBy")
            .containsExactlyInAnyOrderElementsOf(projectFacade.getWorkspaceProjectWorkflows(workspace.getId()));
    }

    @Test
    void testGetWorkspaceProjectWorkflowsOfAnEmptyWorkspaceIsEmpty() {
        Workspace workspace = automationConfigurationIntTestData.createWorkspace("workspace");

        assertThat(remoteProjectFacadeClient.getWorkspaceProjectWorkflows(workspace.getId())).isEmpty();
    }
}
