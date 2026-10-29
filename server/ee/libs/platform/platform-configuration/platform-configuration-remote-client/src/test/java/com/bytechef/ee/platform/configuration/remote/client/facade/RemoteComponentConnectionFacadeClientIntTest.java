/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.ee.platform.configuration.remote.client.config.PlatformConfigurationRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.configuration.remote.grpc.ComponentConnectionFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.configuration.remote.grpc.PlatformConfigurationProtoMapper;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
    classes = PlatformConfigurationRemoteClientIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true", "spring.liquibase.contexts=configuration"
    })
class RemoteComponentConnectionFacadeClientIntTest {

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ObjectMapper objectMapper;

    private ManagedChannel managedChannel;
    private RemoteComponentConnectionFacadeClient remoteComponentConnectionFacadeClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                new ComponentConnectionFacadeRemoteServiceGrpcServer(
                    componentConnectionFacade, new PlatformConfigurationProtoMapper(objectMapper)))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteComponentConnectionFacadeClient = new RemoteComponentConnectionFacadeClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testGetComponentConnectionsForTaskResolvesTheComponentConnection() {
        WorkflowTask workflowTask = new WorkflowTask(
            Map.of(
                "name", "airtable_1", "type", "airtable/v1/createRecord", "parameters", Map.of("baseId", "base")));

        List<ComponentConnection> componentConnections =
            remoteComponentConnectionFacadeClient.getComponentConnections(workflowTask);

        assertThat(componentConnections)
            .containsExactly(new ComponentConnection("airtable", 1, "airtable_1", "airtable", true));
        assertThat(componentConnections).isEqualTo(componentConnectionFacade.getComponentConnections(workflowTask));
    }

    @Test
    void testGetComponentConnectionsForTaskWithoutAConnectionIsEmpty() {
        WorkflowTask unknownComponentWorkflowTask = new WorkflowTask(
            Map.of("name", "unknown_1", "type", "unknown/v1/doSomething", "parameters", Map.of()));
        WorkflowTask flowControlWorkflowTask = new WorkflowTask(
            Map.of("name", "condition_1", "type", "condition/v1", "parameters", Map.of()));

        assertThat(remoteComponentConnectionFacadeClient.getComponentConnections(unknownComponentWorkflowTask))
            .isEmpty();
        assertThat(remoteComponentConnectionFacadeClient.getComponentConnections(flowControlWorkflowTask))
            .isEmpty();
    }

    @Test
    void testGetComponentConnectionsForTriggerResolvesTheComponentConnection() {
        WorkflowTrigger workflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "type", "airtable/v1/newRecord", "parameters", Map.of()));

        List<ComponentConnection> componentConnections =
            remoteComponentConnectionFacadeClient.getComponentConnections(workflowTrigger);

        assertThat(componentConnections)
            .containsExactly(new ComponentConnection("airtable", 1, "trigger_1", "airtable", true));
    }

    @Test
    void testGetComponentConnectionsForTriggerWithoutAConnectionIsEmpty() {
        WorkflowTrigger workflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "type", "manual/v1/manual", "parameters", Map.of()));

        assertThat(remoteComponentConnectionFacadeClient.getComponentConnections(workflowTrigger)).isEmpty();
    }
}
