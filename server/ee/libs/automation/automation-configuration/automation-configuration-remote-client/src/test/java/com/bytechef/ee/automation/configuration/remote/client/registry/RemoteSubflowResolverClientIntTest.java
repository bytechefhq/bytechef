/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationIntTestData;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationRemoteClientIntTestConfiguration;
import com.bytechef.ee.automation.configuration.remote.grpc.SubflowResolverRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;

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
class RemoteSubflowResolverClientIntTest {

    private static final UUID WORKFLOW_UUID = UUID.fromString("8a1b6f0e-5c2d-4e7f-9a3b-2c1d0e9f8a7b");

    @Autowired
    private AutomationConfigurationIntTestData automationConfigurationIntTestData;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private SubflowResolver subflowResolver;

    @Autowired
    private WorkflowService workflowService;

    private ManagedChannel managedChannel;
    private Project project;
    private RemoteSubflowResolverClient remoteSubflowResolverClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new SubflowResolverRemoteServiceGrpcServer(subflowResolver),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteSubflowResolverClient = new RemoteSubflowResolverClient(new FixedGrpcChannels(managedChannel));

        Workspace workspace = automationConfigurationIntTestData.createWorkspace("workspace");

        project = automationConfigurationIntTestData.createProject("Live Project", workspace.getId());
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        automationConfigurationIntTestData.deleteAll();
    }

    @Test
    void testResolveSubflowInTheEditorUsesTheLatestWorkflowVersion() {
        ProjectWorkflow publishedProjectWorkflow = automationConfigurationIntTestData.createProjectWorkflow(
            project.getId(), 1, "Published", WORKFLOW_UUID);

        int draftProjectVersion = automationConfigurationIntTestData.publishProject(project.getId());

        ProjectWorkflow draftProjectWorkflow = automationConfigurationIntTestData.createProjectWorkflow(
            project.getId(), draftProjectVersion, "Draft", WORKFLOW_UUID);

        assertThat(remoteSubflowResolverClient.resolveSubflow(WORKFLOW_UUID.toString(), "callWorkflow", true))
            .isEqualTo(new Subflow(draftProjectWorkflow.getWorkflowId(), "trigger_1"));
        assertThat(remoteSubflowResolverClient.resolveSubflow(WORKFLOW_UUID.toString(), "callWorkflow", false))
            .isEqualTo(new Subflow(publishedProjectWorkflow.getWorkflowId(), "trigger_1"));
    }

    @Test
    void testResolveSubflowWithoutACallableTriggerFailsWithItsMessage() {
        Workflow workflow = workflowService.create(
            """
                {"label":"No Trigger","tasks":[{"name":"logger_1","type":"logger/v1/info","parameters":{}}]}
                """,
            Workflow.Format.JSON, Workflow.SourceType.JDBC);

        projectWorkflowService.addWorkflow(project.getId(), 1, workflow.getId(), WORKFLOW_UUID);

        assertThatThrownBy(
            () -> remoteSubflowResolverClient.resolveSubflow(WORKFLOW_UUID.toString(), "callWorkflow", true))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage(
                    "INTERNAL: IllegalStateException: No callable trigger found in workflow '%s'"
                        .formatted(workflow.getId()));
    }

    @Test
    void testResolveSubflowWithoutAPublishedVersionFailsWithItsMessage() {
        automationConfigurationIntTestData.createProjectWorkflow(project.getId(), 1, "Draft", WORKFLOW_UUID);

        assertThatThrownBy(
            () -> remoteSubflowResolverClient.resolveSubflow(WORKFLOW_UUID.toString(), "callWorkflow", false))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage("INVALID_ARGUMENT: No published workflow found for workflow uuid " + WORKFLOW_UUID);
    }
}
