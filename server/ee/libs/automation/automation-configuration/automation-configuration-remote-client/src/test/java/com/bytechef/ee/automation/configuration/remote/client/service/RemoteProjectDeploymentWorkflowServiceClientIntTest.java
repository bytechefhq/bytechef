/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationIntTestData;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationRemoteClientIntTestConfiguration;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.ProjectDeploymentWorkflowRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
class RemoteProjectDeploymentWorkflowServiceClientIntTest {

    @Autowired
    private AutomationConfigurationIntTestData automationConfigurationIntTestData;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    private Connection airtableConnection;
    private ManagedChannel managedChannel;
    private ProjectDeployment projectDeployment;
    private RemoteProjectDeploymentWorkflowServiceClient remoteProjectDeploymentWorkflowServiceClient;
    private Server server;
    private Connection slackConnection;
    private String workflowId;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                new ProjectDeploymentWorkflowRemoteServiceGrpcServer(
                    projectDeploymentWorkflowService, new AutomationConfigurationProtoMapper(objectMapper)))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteProjectDeploymentWorkflowServiceClient = new RemoteProjectDeploymentWorkflowServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);

        Workspace workspace = automationConfigurationIntTestData.createWorkspace("workspace");

        Project project = automationConfigurationIntTestData.createProject("Live Project", workspace.getId());

        ProjectWorkflow projectWorkflow = automationConfigurationIntTestData.createProjectWorkflow(
            project.getId(), 1, "Live Workflow", UUID.randomUUID());

        workflowId = projectWorkflow.getWorkflowId();

        projectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Live Deployment", project.getId(), 1, Environment.PRODUCTION, List.of());

        airtableConnection = automationConfigurationIntTestData.createConnection("airtable");
        slackConnection = automationConfigurationIntTestData.createConnection("slack");
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        automationConfigurationIntTestData.deleteAll();
    }

    @Test
    void testGetProjectDeploymentWorkflowReturnsTheStoredDeploymentWorkflow() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = createProjectDeploymentWorkflow(true);

        ProjectDeploymentWorkflow fetchedProjectDeploymentWorkflow =
            remoteProjectDeploymentWorkflowServiceClient.getProjectDeploymentWorkflow(
                projectDeployment.getId(), workflowId);

        assertThat(fetchedProjectDeploymentWorkflow.getId()).isEqualTo(projectDeploymentWorkflow.getId());
        assertThat(fetchedProjectDeploymentWorkflow.getProjectDeploymentId()).isEqualTo(projectDeployment.getId());
        assertThat(fetchedProjectDeploymentWorkflow.getWorkflowId()).isEqualTo(workflowId);
        assertThat(fetchedProjectDeploymentWorkflow.isEnabled()).isTrue();
        Map<String, ?> inputs = fetchedProjectDeploymentWorkflow.getInputs();

        assertThat(inputs).isEqualTo(Map.of("id", 9_007_199_254_740_993L, "name", "Ada"));
        assertThat(inputs.get("id")).isInstanceOf(Long.class);
        assertThat(fetchedProjectDeploymentWorkflow.getConnections())
            .extracting(
                ProjectDeploymentWorkflowConnection::getConnectionId,
                ProjectDeploymentWorkflowConnection::getWorkflowConnectionKey,
                ProjectDeploymentWorkflowConnection::getWorkflowNodeName)
            .containsExactlyInAnyOrder(
                tuple(airtableConnection.getId(), "airtable", "airtable_1"),
                tuple(slackConnection.getId(), "slack", "airtable_1"),
                tuple(slackConnection.getId(), "slack", "slack_1"));
    }

    @Test
    void testFetchProjectDeploymentWorkflowConnectionMatchesNodeAndKey() {
        createProjectDeploymentWorkflow(true);

        assertThat(
            remoteProjectDeploymentWorkflowServiceClient.fetchProjectDeploymentWorkflowConnection(
                projectDeployment.getId(), workflowId, "airtable_1", "airtable"))
                    .hasValueSatisfying(projectDeploymentWorkflowConnection -> {
                        assertThat(projectDeploymentWorkflowConnection.getConnectionId())
                            .isEqualTo(airtableConnection.getId());
                        assertThat(projectDeploymentWorkflowConnection.getWorkflowConnectionKey())
                            .isEqualTo("airtable");
                        assertThat(projectDeploymentWorkflowConnection.getWorkflowNodeName()).isEqualTo("airtable_1");
                    });
        assertThat(
            remoteProjectDeploymentWorkflowServiceClient.fetchProjectDeploymentWorkflowConnection(
                projectDeployment.getId(), workflowId, "slack_1", "airtable"))
                    .isEmpty();
    }

    @Test
    void testGetProjectDeploymentWorkflowConnectionsReturnsTheConnectionsOfTheNode() {
        createProjectDeploymentWorkflow(true);

        assertThat(
            remoteProjectDeploymentWorkflowServiceClient.getProjectDeploymentWorkflowConnections(
                projectDeployment.getId(), workflowId, "airtable_1"))
                    .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
                    .containsExactlyInAnyOrder(airtableConnection.getId(), slackConnection.getId());
        assertThat(
            remoteProjectDeploymentWorkflowServiceClient.getProjectDeploymentWorkflowConnections(
                projectDeployment.getId(), workflowId, "logger_1"))
                    .isEmpty();
    }

    @Test
    void testIsConnectionUsedReflectsStoredDeploymentWorkflowConnections() {
        Connection unusedConnection = automationConfigurationIntTestData.createConnection("unused");

        createProjectDeploymentWorkflow(true);

        assertThat(remoteProjectDeploymentWorkflowServiceClient.isConnectionUsed(airtableConnection.getId()))
            .isTrue();
        assertThat(remoteProjectDeploymentWorkflowServiceClient.isConnectionUsed(unusedConnection.getId()))
            .isFalse();
    }

    @Test
    void testDisableProjectDeploymentWorkflowByConnectionIdDisablesTheStoredWorkflow() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = createProjectDeploymentWorkflow(true);

        remoteProjectDeploymentWorkflowServiceClient.disableProjectDeploymentWorkflowByConnectionId(
            airtableConnection.getId());

        ProjectDeploymentWorkflow storedProjectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflow.getId());

        assertThat(storedProjectDeploymentWorkflow.isEnabled()).isFalse();
        assertThat(storedProjectDeploymentWorkflow.getConnections()).hasSize(3);
    }

    @Test
    void testDisableProjectDeploymentWorkflowByConnectionIdLeavesOtherWorkflowsEnabled() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = createProjectDeploymentWorkflow(true);
        Connection unusedConnection = automationConfigurationIntTestData.createConnection("unused");

        remoteProjectDeploymentWorkflowServiceClient.disableProjectDeploymentWorkflowByConnectionId(
            unusedConnection.getId());

        ProjectDeploymentWorkflow storedProjectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflow.getId());

        assertThat(storedProjectDeploymentWorkflow.isEnabled()).isTrue();
    }

    @Test
    void testIsProjectDeploymentWorkflowEnabledReflectsTheStoredFlag() {
        createProjectDeploymentWorkflow(false);

        assertThat(
            remoteProjectDeploymentWorkflowServiceClient.isProjectDeploymentWorkflowEnabled(
                projectDeployment.getId(), workflowId))
                    .isFalse();

        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeployment.getId(), workflowId);

        projectDeploymentWorkflowService.updateEnabled(projectDeploymentWorkflow.getId(), true);

        assertThat(
            remoteProjectDeploymentWorkflowServiceClient.isProjectDeploymentWorkflowEnabled(
                projectDeployment.getId(), workflowId))
                    .isTrue();
        assertThat(
            remoteProjectDeploymentWorkflowServiceClient.isProjectDeploymentWorkflowEnabled(
                projectDeployment.getId(), "unknown-workflow"))
                    .isFalse();
    }

    private ProjectDeploymentWorkflow createProjectDeploymentWorkflow(boolean enabled) {
        return automationConfigurationIntTestData.createProjectDeploymentWorkflow(
            projectDeployment.getId(), workflowId, enabled, Map.of("id", 9_007_199_254_740_993L, "name", "Ada"),
            List.of(
                new ProjectDeploymentWorkflowConnection(airtableConnection.getId(), "airtable", "airtable_1"),
                new ProjectDeploymentWorkflowConnection(slackConnection.getId(), "slack", "airtable_1"),
                new ProjectDeploymentWorkflowConnection(slackConnection.getId(), "slack", "slack_1")));
    }
}
