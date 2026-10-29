/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationInstanceRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class,
    properties = {
        "bytechef.edition=EE", "spring.liquibase.contexts=configuration"
    })
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class RemoteIntegrationInstanceServiceClientIntTest {

    private static final long CONNECTION_ID = 42L;
    private static final long USER_A_ID = 1001L;
    private static final long USER_B_ID = 1002L;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceRepository integrationInstanceRepository;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private ObjectMapper objectMapper;

    private ManagedChannel managedChannel;
    private RemoteIntegrationInstanceServiceClient remoteIntegrationInstanceServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                new IntegrationInstanceRemoteServiceGrpcServer(
                    integrationInstanceService, new EmbeddedConfigurationProtoMapper(objectMapper)))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteIntegrationInstanceServiceClient = new RemoteIntegrationInstanceServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testGetIntegrationInstancesByWorkflowUuidReturnsTheConnectedUsersInstancesOfThatWorkflow() {
        long integrationId = createIntegration();

        long version1ConfigurationId = createConfiguration(integrationId, 1, Environment.PRODUCTION);
        long version2ConfigurationId = createConfiguration(integrationId, 2, Environment.PRODUCTION);
        long stagingConfigurationId = createConfiguration(integrationId, 1, Environment.STAGING);

        String version1WorkflowUuid = integrationWorkflowService.addWorkflow(integrationId, 1, "workflow-v1")
            .getUuidAsString();
        String version2WorkflowUuid = integrationWorkflowService.addWorkflow(integrationId, 2, "workflow-v2")
            .getUuidAsString();

        IntegrationInstance olderInstance = integrationInstanceService.create(
            USER_A_ID, CONNECTION_ID, version1ConfigurationId);
        IntegrationInstance newerInstance = integrationInstanceService.create(
            USER_A_ID, CONNECTION_ID, version1ConfigurationId);

        integrationInstanceService.create(USER_A_ID, CONNECTION_ID, stagingConfigurationId);
        integrationInstanceService.create(USER_B_ID, CONNECTION_ID, version2ConfigurationId);

        assertThat(
            remoteIntegrationInstanceServiceClient.getIntegrationInstancesByWorkflowUuid(
                USER_A_ID, version1WorkflowUuid, Environment.PRODUCTION))
                    .extracting(IntegrationInstance::getId, IntegrationInstance::getConnectedUserId)
                    .containsExactly(
                        tuple(newerInstance.getId(), USER_A_ID),
                        tuple(olderInstance.getId(), USER_A_ID));
        assertThat(
            remoteIntegrationInstanceServiceClient.getIntegrationInstancesByWorkflowUuid(
                USER_A_ID, version2WorkflowUuid, Environment.PRODUCTION))
                    .isEmpty();
        assertThat(
            remoteIntegrationInstanceServiceClient.getIntegrationInstancesByWorkflowUuid(
                USER_B_ID, version1WorkflowUuid, Environment.PRODUCTION))
                    .isEmpty();
        assertThat(
            remoteIntegrationInstanceServiceClient.getIntegrationInstancesByWorkflowUuid(
                USER_A_ID, version1WorkflowUuid, Environment.STAGING))
                    .hasSize(1);
        assertThat(
            remoteIntegrationInstanceServiceClient.getIntegrationInstancesByWorkflowUuid(
                USER_A_ID, "not-a-uuid", Environment.PRODUCTION))
                    .isEmpty();
    }

    private long createConfiguration(long integrationId, int integrationVersion, Environment environment) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(integrationVersion);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        return integrationInstanceConfiguration.getId();
    }

    private long createIntegration() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        return integration.getId();
    }
}
