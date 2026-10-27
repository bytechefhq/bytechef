/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.platform.configuration.remote.client.facade.RemoteComponentConnectionFacadeClient;
import com.bytechef.ee.platform.configuration.remote.client.service.RemoteWorkflowServiceClient;
import com.bytechef.ee.platform.configuration.remote.client.service.RemoteWorkflowTestConfigurationServiceClient;
import com.bytechef.ee.platform.configuration.remote.grpc.ComponentConnectionFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.configuration.remote.grpc.PlatformConfigurationProtoMapper;
import com.bytechef.ee.platform.configuration.remote.grpc.WorkflowRemoteServiceGrpcServer;
import com.bytechef.ee.platform.configuration.remote.grpc.WorkflowTestConfigurationRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class PlatformConfigurationRemoteServiceGrpcWireTest {

    private static final String DEFINITION = """
        {"label":"Live Workflow","tasks":[
            {"name":"randomHelper_1","type":"randomHelper/v1/randomFloat","parameters":{"from":1,"to":100}},
            {"name":"logger_1","type":"logger/v1/info","parameters":{"text":"${randomHelper_1}"}}]}
        """;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();
    private static final String WORKFLOW_ID = "3e4078b4-87bb-43c0-9cc6-1270f73b131d";

    private final ComponentConnectionFacade componentConnectionFacade = mock(ComponentConnectionFacade.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WorkflowTestConfigurationService workflowTestConfigurationService =
        mock(WorkflowTestConfigurationService.class);

    private ManagedChannel managedChannel;
    private RemoteComponentConnectionFacadeClient remoteComponentConnectionFacadeClient;
    private RemoteWorkflowServiceClient remoteWorkflowServiceClient;
    private RemoteWorkflowTestConfigurationServiceClient remoteWorkflowTestConfigurationServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();
        PlatformConfigurationProtoMapper platformConfigurationProtoMapper =
            new PlatformConfigurationProtoMapper(JSON_MAPPER);
        ServerInterceptor serverInterceptor = new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler());

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ComponentConnectionFacadeRemoteServiceGrpcServer(
                        componentConnectionFacade, platformConfigurationProtoMapper),
                    serverInterceptor))
            .addService(
                ServerInterceptors.intercept(
                    new WorkflowRemoteServiceGrpcServer(workflowService, platformConfigurationProtoMapper),
                    serverInterceptor))
            .addService(
                ServerInterceptors.intercept(
                    new WorkflowTestConfigurationRemoteServiceGrpcServer(
                        workflowTestConfigurationService, platformConfigurationProtoMapper),
                    serverInterceptor))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        FixedGrpcChannels fixedGrpcChannels = new FixedGrpcChannels(managedChannel);

        remoteComponentConnectionFacadeClient = new RemoteComponentConnectionFacadeClient(
            fixedGrpcChannels, JSON_MAPPER);
        remoteWorkflowServiceClient = new RemoteWorkflowServiceClient(fixedGrpcChannels, JSON_MAPPER);
        remoteWorkflowTestConfigurationServiceClient = new RemoteWorkflowTestConfigurationServiceClient(
            fixedGrpcChannels, JSON_MAPPER);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testWorkflowRoundTripKeepsDefinitionAndTasks() {
        Workflow workflow = workflow(WORKFLOW_ID);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);

        Workflow readWorkflow = remoteWorkflowServiceClient.getWorkflow(WORKFLOW_ID);

        assertThat(readWorkflow.getDefinition()).isEqualTo(DEFINITION);
        assertThat(readWorkflow.getLastModifiedDate()).isEqualTo(workflow.getLastModifiedDate());
        assertThat(readWorkflow.getMetadata()).isEqualTo(workflow.getMetadata());
        assertThat(readWorkflow.getVersion()).isEqualTo(3);
        assertThat(readWorkflow.getTasks()).hasSize(2)
            .extracting(WorkflowTask::toMap)
            .containsExactlyElementsOf(
                workflow.getTasks()
                    .stream()
                    .map(WorkflowTask::toMap)
                    .toList());
    }

    @Test
    void testCreateWorkflow() {
        Workflow workflow = workflow(WORKFLOW_ID);

        workflow.setSourceType(Workflow.SourceType.JDBC);

        when(workflowService.create(DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC)).thenReturn(workflow);

        Workflow createdWorkflow = remoteWorkflowServiceClient.create(
            DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        assertThat(createdWorkflow.getId()).isEqualTo(WORKFLOW_ID);
        assertThat(createdWorkflow.getDefinition()).isEqualTo(DEFINITION);
        assertThat(createdWorkflow.getSourceType()).isEqualTo(Workflow.SourceType.JDBC);
        assertThat(createdWorkflow.getVersion()).isEqualTo(3);
    }

    @Test
    void testCreateWorkflowWithoutASourceTypeFailsWithItsMessage() {
        when(workflowService.create(DEFINITION, Workflow.Format.JSON, null))
            .thenThrow(new IllegalArgumentException("'sourceType' must not be null"));

        assertThatThrownBy(() -> remoteWorkflowServiceClient.create(DEFINITION, Workflow.Format.JSON, null))
            .hasMessageContaining("'sourceType' must not be null");
    }

    @Test
    void testFetchUnknownWorkflowIsEmpty() {
        when(workflowService.fetchWorkflow("unknown")).thenReturn(Optional.empty());
        when(workflowService.fetchWorkflow(WORKFLOW_ID)).thenReturn(Optional.of(workflow(WORKFLOW_ID)));

        assertThat(remoteWorkflowServiceClient.fetchWorkflow("unknown")).isEmpty();
        assertThat(remoteWorkflowServiceClient.fetchWorkflow(WORKFLOW_ID)).get()
            .extracting(Workflow::getId)
            .isEqualTo(WORKFLOW_ID);
    }

    @Test
    void testGetUnknownWorkflowFailsWithItsMessage() {
        when(workflowService.getWorkflow("abc")).thenThrow(new IllegalArgumentException("Workflow abc not found"));

        assertThatThrownBy(() -> remoteWorkflowServiceClient.getWorkflow("abc"))
            .hasMessageContaining("Workflow abc not found");
    }

    @Test
    void testGetWorkflowsKeepsRequestOrder() {
        when(workflowService.getWorkflows(List.of("b", "a"))).thenReturn(List.of(workflow("b"), workflow("a")));

        assertThat(remoteWorkflowServiceClient.getWorkflows(List.of("b", "a"))).extracting(Workflow::getId)
            .containsExactly("b", "a");
    }

    @Test
    void testIsConnectionUsed() {
        when(workflowTestConfigurationService.isConnectionUsed(7L)).thenReturn(true);
        when(workflowTestConfigurationService.isConnectionUsed(8L)).thenReturn(false);

        assertThat(remoteWorkflowTestConfigurationServiceClient.isConnectionUsed(7L)).isTrue();
        assertThat(remoteWorkflowTestConfigurationServiceClient.isConnectionUsed(8L)).isFalse();
    }

    @Test
    void testGetWorkflowTestConfigurationInputs() {
        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(WORKFLOW_ID, 2L))
            .thenAnswer(invocation -> Map.of("id", 9_007_199_254_740_993L));

        Map<String, ?> inputs = remoteWorkflowTestConfigurationServiceClient.getWorkflowTestConfigurationInputs(
            WORKFLOW_ID, 2L);

        assertThat(inputs).isEqualTo(Map.of("id", 9_007_199_254_740_993L));
        assertThat(inputs.get("id")).isInstanceOf(Long.class);
    }

    @Test
    void testGetWorkflowTestConfigurationInputsWithoutConfigurationIsNull() {
        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(WORKFLOW_ID, 2L)).thenReturn(null);
        when(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(WORKFLOW_ID, 3L))
            .thenAnswer(invocation -> Map.of());

        assertThat(remoteWorkflowTestConfigurationServiceClient.getWorkflowTestConfigurationInputs(WORKFLOW_ID, 2L))
            .isNull();
        assertThat(remoteWorkflowTestConfigurationServiceClient.getWorkflowTestConfigurationInputs(WORKFLOW_ID, 3L))
            .isEmpty();
    }

    @Test
    void testGetComponentConnectionsForTaskAndTrigger() {
        WorkflowTask workflowTask = new WorkflowTask(
            Map.of("name", "airtable_1", "type", "airtable/v1/create", "parameters", Map.of("count", 5)));
        WorkflowTrigger workflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "type", "webhook/v1/autoRespondWithHTTP200", "parameters", Map.of()));
        List<ComponentConnection> componentConnections = List.of(
            new ComponentConnection("airtable", 1, "airtable_1", "airtable", true));
        AtomicReference<WorkflowTask> receivedWorkflowTask = new AtomicReference<>();
        AtomicReference<WorkflowTrigger> receivedWorkflowTrigger = new AtomicReference<>();

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class))).thenAnswer(invocation -> {
            receivedWorkflowTask.set(invocation.getArgument(0));

            return componentConnections;
        });
        when(componentConnectionFacade.getComponentConnections(any(WorkflowTrigger.class))).thenAnswer(invocation -> {
            receivedWorkflowTrigger.set(invocation.getArgument(0));

            return List.of();
        });

        assertThat(remoteComponentConnectionFacadeClient.getComponentConnections(workflowTask))
            .isEqualTo(componentConnections);
        assertThat(remoteComponentConnectionFacadeClient.getComponentConnections(workflowTrigger)).isEmpty();
        assertThat(receivedWorkflowTask.get()
            .toMap()).isEqualTo(workflowTask.toMap());
        assertThat(receivedWorkflowTrigger.get()
            .toMap()).isEqualTo(workflowTrigger.toMap());
    }

    private static Workflow workflow(String id) {
        Workflow workflow = new Workflow(
            id, DEFINITION, Workflow.Format.JSON, Instant.parse("2026-10-26T11:00:00Z"), Map.of("source", "test"));

        workflow.setVersion(3);

        return workflow;
    }
}
