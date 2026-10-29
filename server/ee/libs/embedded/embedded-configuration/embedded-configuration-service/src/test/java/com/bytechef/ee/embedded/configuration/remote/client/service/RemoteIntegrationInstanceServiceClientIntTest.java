/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.remote.client.EmbeddedConfigurationGrpcTestServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationInstanceRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserIntegrationInstancesRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IntegrationInstanceRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.List;
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
    private IntegrationInstanceConfigurationWorkflowRepository integrationInstanceConfigurationWorkflowRepository;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

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

    private EmbeddedConfigurationGrpcTestServer embeddedConfigurationGrpcTestServer;
    private RemoteIntegrationInstanceServiceClient remoteIntegrationInstanceServiceClient;

    @BeforeEach
    void beforeEach() throws IOException {
        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new IntegrationInstanceRemoteServiceGrpcServer(
                integrationInstanceService, new EmbeddedConfigurationProtoMapper(objectMapper)));

        remoteIntegrationInstanceServiceClient = new RemoteIntegrationInstanceServiceClient(
            embeddedConfigurationGrpcTestServer.getGrpcChannels(), objectMapper);
    }

    @AfterEach
    void afterEach() {
        embeddedConfigurationGrpcTestServer.shutdown();

        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationWorkflowRepository.deleteAll();
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

    @Test
    void testGetIntegrationInstanceReturnsTheStoredInstance() {
        long configurationId = createConfiguration(createIntegration(), 1, Environment.PRODUCTION);

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            USER_A_ID, CONNECTION_ID, configurationId);

        IntegrationInstance remoteIntegrationInstance = remoteIntegrationInstanceServiceClient.getIntegrationInstance(
            integrationInstance.getId());

        assertThat(remoteIntegrationInstance).usingRecursiveComparison()
            .isEqualTo(integrationInstanceService.getIntegrationInstance(integrationInstance.getId()));
        assertThat(remoteIntegrationInstance.getConnectedUserId()).isEqualTo(USER_A_ID);
        assertThat(remoteIntegrationInstance.getConnectionId()).isEqualTo(CONNECTION_ID);
        assertThat(remoteIntegrationInstance.getIntegrationInstanceConfigurationId()).isEqualTo(configurationId);
        assertThat(remoteIntegrationInstance.isEnabled()).isTrue();
    }

    @Test
    void testGetIntegrationInstanceOfAnUnknownIdIsAnIllegalArgument() {
        assertThatThrownBy(() -> remoteIntegrationInstanceServiceClient.getIntegrationInstance(Long.MAX_VALUE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Integration instance not found");
    }

    @Test
    void testFetchIntegrationInstanceReturnsTheConnectedUsersInstanceOfTheComponentInThatEnvironment() {
        long airtableIntegrationId = createIntegration("airtable");

        long productionConfigurationId = createConfiguration(airtableIntegrationId, 1, Environment.PRODUCTION);
        long stagingConfigurationId = createConfiguration(airtableIntegrationId, 1, Environment.STAGING);

        IntegrationInstance productionIntegrationInstance = integrationInstanceService.create(
            USER_A_ID, CONNECTION_ID, productionConfigurationId);

        integrationInstanceService.create(USER_A_ID, CONNECTION_ID, stagingConfigurationId);
        integrationInstanceService.create(USER_B_ID, CONNECTION_ID, productionConfigurationId);

        assertThat(remoteIntegrationInstanceServiceClient.fetchIntegrationInstance(
            USER_A_ID, "Airtable", Environment.PRODUCTION))
                .map(IntegrationInstance::getId)
                .contains(productionIntegrationInstance.getId());
        assertThat(remoteIntegrationInstanceServiceClient.fetchIntegrationInstance(
            USER_A_ID, "slack", Environment.PRODUCTION))
                .isEmpty();
        assertThat(remoteIntegrationInstanceServiceClient.fetchIntegrationInstance(
            USER_A_ID, "airtable", Environment.DEVELOPMENT))
                .isEmpty();
    }

    @Test
    void testGetConnectedUserIntegrationInstancesFiltersByEnabledAndByEnvironment() {
        long integrationId = createIntegration();

        long productionConfigurationId = createConfiguration(integrationId, 1, Environment.PRODUCTION);
        long stagingConfigurationId = createConfiguration(integrationId, 1, Environment.STAGING);

        IntegrationInstance enabledProductionIntegrationInstance = integrationInstanceService.create(
            USER_A_ID, CONNECTION_ID, productionConfigurationId);
        IntegrationInstance disabledStagingIntegrationInstance = integrationInstanceService.create(
            USER_A_ID, CONNECTION_ID, stagingConfigurationId);

        integrationInstanceService.updateEnabled(disabledStagingIntegrationInstance.getId(), false);

        IntegrationInstance otherUserIntegrationInstance = integrationInstanceService.create(
            USER_B_ID, CONNECTION_ID, productionConfigurationId);

        assertThat(remoteIntegrationInstanceServiceClient.getConnectedUserIntegrationInstances(USER_A_ID))
            .extracting(IntegrationInstance::getId)
            .containsExactlyInAnyOrder(
                enabledProductionIntegrationInstance.getId(), disabledStagingIntegrationInstance.getId());
        assertThat(remoteIntegrationInstanceServiceClient.getConnectedUserIntegrationInstances(USER_A_ID, true))
            .extracting(IntegrationInstance::getId)
            .containsExactly(enabledProductionIntegrationInstance.getId());
        assertThat(remoteIntegrationInstanceServiceClient.getConnectedUserIntegrationInstances(USER_A_ID, false))
            .extracting(IntegrationInstance::getId)
            .containsExactly(disabledStagingIntegrationInstance.getId());
        assertThat(remoteIntegrationInstanceServiceClient.getConnectedUserIntegrationInstances(
            USER_A_ID, Environment.STAGING))
                .extracting(IntegrationInstance::getId)
                .containsExactly(disabledStagingIntegrationInstance.getId());
        assertThat(remoteIntegrationInstanceServiceClient.getConnectedUserIntegrationInstances(
            USER_B_ID, Environment.PRODUCTION))
                .extracting(IntegrationInstance::getId)
                .containsExactly(otherUserIntegrationInstance.getId());
        assertThat(remoteIntegrationInstanceServiceClient.getConnectedUserIntegrationInstances(
            USER_B_ID, Environment.STAGING))
                .isEmpty();
    }

    @Test
    void testGetConnectedUserIntegrationInstancesWithUnknownEnvironmentIsAnInvalidArgument() {
        IntegrationInstanceRemoteServiceGrpc.IntegrationInstanceRemoteServiceBlockingStub stub =
            IntegrationInstanceRemoteServiceGrpc.newBlockingStub(
                embeddedConfigurationGrpcTestServer.getManagedChannel());
        ConnectedUserIntegrationInstancesRequest connectedUserIntegrationInstancesRequest =
            ConnectedUserIntegrationInstancesRequest.newBuilder()
                .setConnectedUserId(USER_A_ID)
                .setEnvironment(Environment.values().length)
                .build();

        assertThatThrownBy(() -> stub.getConnectedUserIntegrationInstances(connectedUserIntegrationInstancesRequest))
            .isInstanceOfSatisfying(
                StatusRuntimeException.class,
                statusRuntimeException -> {
                    Status status = statusRuntimeException.getStatus();

                    assertThat(status.getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                    assertThat(status.getDescription()).isEqualTo(
                        "Unknown environment: " + Environment.values().length);
                });
    }

    @Test
    void testGetIntegrationInstancesByIdsAndByConfigurationIdsReturnOnlyTheRequestedInstances() {
        long integrationId = createIntegration();

        long firstConfigurationId = createConfiguration(integrationId, 1, Environment.PRODUCTION);
        long secondConfigurationId = createConfiguration(integrationId, 1, Environment.STAGING);

        IntegrationInstance firstIntegrationInstance = integrationInstanceService.create(
            USER_A_ID, CONNECTION_ID, firstConfigurationId);
        IntegrationInstance secondIntegrationInstance = integrationInstanceService.create(
            USER_B_ID, CONNECTION_ID, firstConfigurationId);
        IntegrationInstance thirdIntegrationInstance = integrationInstanceService.create(
            USER_A_ID, CONNECTION_ID, secondConfigurationId);

        assertThat(remoteIntegrationInstanceServiceClient.getIntegrationInstances(
            List.of(firstIntegrationInstance.getId(), thirdIntegrationInstance.getId())))
                .extracting(IntegrationInstance::getId)
                .containsExactlyInAnyOrder(firstIntegrationInstance.getId(), thirdIntegrationInstance.getId());
        assertThat(remoteIntegrationInstanceServiceClient.getIntegrationInstanceConfigurationIntegrationInstances(
            List.of(firstConfigurationId)))
                .extracting(IntegrationInstance::getId)
                .containsExactlyInAnyOrder(firstIntegrationInstance.getId(), secondIntegrationInstance.getId());
        assertThat(remoteIntegrationInstanceServiceClient.getIntegrationInstanceConfigurationIntegrationInstances(
            List.of()))
                .isEmpty();
    }

    @Test
    void testGetIntegrationInstanceByWorkflowIdReturnsTheConnectedUsersInstanceRunningTheWorkflow() {
        long integrationId = createIntegration();

        long productionConfigurationId = createConfiguration(integrationId, 1, Environment.PRODUCTION);
        long stagingConfigurationId = createConfiguration(integrationId, 1, Environment.STAGING);

        createConfigurationWorkflow(productionConfigurationId, "workflow-1");
        createConfigurationWorkflow(stagingConfigurationId, "workflow-1");

        IntegrationInstance productionIntegrationInstance = integrationInstanceService.create(
            USER_A_ID, CONNECTION_ID, productionConfigurationId);

        integrationInstanceService.create(USER_A_ID, CONNECTION_ID, stagingConfigurationId);
        integrationInstanceService.create(USER_B_ID, CONNECTION_ID, productionConfigurationId);

        assertThat(remoteIntegrationInstanceServiceClient.getIntegrationInstance(
            USER_A_ID, "workflow-1", Environment.PRODUCTION)
            .getId()).isEqualTo(productionIntegrationInstance.getId());
        assertThatThrownBy(() -> remoteIntegrationInstanceServiceClient.getIntegrationInstance(
            USER_A_ID, "workflow-2", Environment.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Integration instance not found");
    }

    private void createConfigurationWorkflow(long integrationInstanceConfigurationId, String workflowId) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setInputs(Map.of());
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);
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
        return createIntegration("componentName");
    }

    private long createIntegration(String componentName) {
        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        return integration.getId();
    }
}
