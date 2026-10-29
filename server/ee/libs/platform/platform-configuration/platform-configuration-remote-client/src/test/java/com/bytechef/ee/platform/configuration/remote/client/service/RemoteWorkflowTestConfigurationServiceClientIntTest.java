/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.configuration.remote.client.config.PlatformConfigurationRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.configuration.remote.grpc.PlatformConfigurationProtoMapper;
import com.bytechef.ee.platform.configuration.remote.grpc.WorkflowTestConfigurationRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
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
class RemoteWorkflowTestConfigurationServiceClientIntTest {

    private static final long DEVELOPMENT_ENVIRONMENT_ID = 0L;
    private static final long PRODUCTION_ENVIRONMENT_ID = 2L;
    private static final String WORKFLOW_ID = "3e4078b4-87bb-43c0-9cc6-1270f73b131d";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowTestConfigurationRepository workflowTestConfigurationRepository;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private ManagedChannel managedChannel;
    private RemoteWorkflowTestConfigurationServiceClient remoteWorkflowTestConfigurationServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                new WorkflowTestConfigurationRemoteServiceGrpcServer(
                    workflowTestConfigurationService, new PlatformConfigurationProtoMapper(objectMapper)))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteWorkflowTestConfigurationServiceClient = new RemoteWorkflowTestConfigurationServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        workflowTestConfigurationRepository.deleteAll();
    }

    @Test
    void testIsConnectionUsedReflectsStoredTestConfigurationConnections() {
        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            WORKFLOW_ID, "airtable_1", "airtable", 7L, false, DEVELOPMENT_ENVIRONMENT_ID);

        assertThat(remoteWorkflowTestConfigurationServiceClient.isConnectionUsed(7L)).isTrue();
        assertThat(remoteWorkflowTestConfigurationServiceClient.isConnectionUsed(8L)).isFalse();

        workflowTestConfigurationService.deleteWorkflowTestConfigurationConnection(7L);

        assertThat(remoteWorkflowTestConfigurationServiceClient.isConnectionUsed(7L)).isFalse();
    }

    @Test
    void testGetWorkflowTestConfigurationInputsReturnsTheStoredInputsOfThatEnvironment() {
        workflowTestConfigurationService.saveWorkflowTestConfigurationInputs(
            WORKFLOW_ID, "id", 9_007_199_254_740_993L, DEVELOPMENT_ENVIRONMENT_ID);
        workflowTestConfigurationService.saveWorkflowTestConfigurationInputs(
            WORKFLOW_ID, "name", "Ada", DEVELOPMENT_ENVIRONMENT_ID);
        workflowTestConfigurationService.saveWorkflowTestConfigurationInputs(
            WORKFLOW_ID, "name", "Grace", PRODUCTION_ENVIRONMENT_ID);

        Map<String, ?> developmentInputs =
            remoteWorkflowTestConfigurationServiceClient.getWorkflowTestConfigurationInputs(
                WORKFLOW_ID, DEVELOPMENT_ENVIRONMENT_ID);

        assertThat(developmentInputs).isEqualTo(Map.of("id", 9_007_199_254_740_993L, "name", "Ada"));
        assertThat(developmentInputs.get("id")).isInstanceOf(Long.class);
        assertThat(
            remoteWorkflowTestConfigurationServiceClient.getWorkflowTestConfigurationInputs(
                WORKFLOW_ID, PRODUCTION_ENVIRONMENT_ID))
                    .isEqualTo(Map.of("name", "Grace"));
    }

    @Test
    void testGetWorkflowTestConfigurationInputsWithoutConfigurationIsEmpty() {
        assertThat(
            remoteWorkflowTestConfigurationServiceClient.getWorkflowTestConfigurationInputs(
                WORKFLOW_ID, DEVELOPMENT_ENVIRONMENT_ID))
                    .isEmpty();
    }
}
