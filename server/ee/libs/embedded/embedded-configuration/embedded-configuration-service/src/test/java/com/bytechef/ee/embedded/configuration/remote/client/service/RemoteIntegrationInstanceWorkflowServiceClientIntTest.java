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
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.remote.client.EmbeddedConfigurationGrpcTestServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationInstanceWorkflowRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
class RemoteIntegrationInstanceWorkflowServiceClientIntTest {

    private static final long CONNECTED_USER_ID = 1001L;
    private static final long CONNECTION_ID = 42L;

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
    private IntegrationInstanceWorkflowRepository integrationInstanceWorkflowRepository;

    @Autowired
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private ObjectMapper objectMapper;

    private EmbeddedConfigurationGrpcTestServer embeddedConfigurationGrpcTestServer;
    private RemoteIntegrationInstanceWorkflowServiceClient remoteIntegrationInstanceWorkflowServiceClient;

    @BeforeEach
    void beforeEach() throws IOException {
        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new IntegrationInstanceWorkflowRemoteServiceGrpcServer(
                integrationInstanceWorkflowService, new EmbeddedConfigurationProtoMapper(objectMapper)));

        remoteIntegrationInstanceWorkflowServiceClient = new RemoteIntegrationInstanceWorkflowServiceClient(
            embeddedConfigurationGrpcTestServer.getGrpcChannels(), objectMapper);
    }

    @AfterEach
    void afterEach() {
        embeddedConfigurationGrpcTestServer.shutdown();

        integrationInstanceWorkflowRepository.deleteAll();
        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationWorkflowRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testGetIntegrationInstanceWorkflowReturnsTheStoredWorkflowWithItsInputs() {
        long integrationInstanceConfigurationId = createConfiguration();

        long firstConfigurationWorkflowId = createConfigurationWorkflow(
            integrationInstanceConfigurationId, "workflow-1");
        long secondConfigurationWorkflowId = createConfigurationWorkflow(
            integrationInstanceConfigurationId, "workflow-2");

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            CONNECTED_USER_ID, CONNECTION_ID, integrationInstanceConfigurationId);

        IntegrationInstanceWorkflow integrationInstanceWorkflow = createInstanceWorkflow(
            integrationInstance.getId(), firstConfigurationWorkflowId,
            Map.of("count", 5, "id", 9_007_199_254_740_993L, "sheet", "Leads"));

        createInstanceWorkflow(integrationInstance.getId(), secondConfigurationWorkflowId, Map.of());

        IntegrationInstanceWorkflow remoteIntegrationInstanceWorkflow =
            remoteIntegrationInstanceWorkflowServiceClient.getIntegrationInstanceWorkflow(
                integrationInstance.getId(), "workflow-1");

        assertThat(remoteIntegrationInstanceWorkflow).usingRecursiveComparison()
            .isEqualTo(
                integrationInstanceWorkflowService.getIntegrationInstanceWorkflow(
                    integrationInstance.getId(), "workflow-1"));
        assertThat(remoteIntegrationInstanceWorkflow.getId()).isEqualTo(integrationInstanceWorkflow.getId());
        assertThat(remoteIntegrationInstanceWorkflow.getInputs())
            .isEqualTo(Map.of("count", 5, "id", 9_007_199_254_740_993L, "sheet", "Leads"));
        assertThat(remoteIntegrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId())
            .isEqualTo(firstConfigurationWorkflowId);
    }

    @Test
    void testGetIntegrationInstanceWorkflowOfAnUnknownWorkflowFailsWithTheServiceMessage() {
        long integrationInstanceConfigurationId = createConfiguration();

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            CONNECTED_USER_ID, CONNECTION_ID, integrationInstanceConfigurationId);

        assertThatThrownBy(
            () -> remoteIntegrationInstanceWorkflowServiceClient.getIntegrationInstanceWorkflow(
                integrationInstance.getId(), "workflow-1"))
                    .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        statusRuntimeException -> {
                            Status status = statusRuntimeException.getStatus();

                            assertThat(status.getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                            assertThat(status.getDescription()).isEqualTo("Integration instance workflow not found");
                        });
    }

    @Test
    void testFetchIntegrationInstanceWorkflowIsEmptyForAWorkflowTheInstanceDoesNotRun() {
        long integrationInstanceConfigurationId = createConfiguration();

        long configurationWorkflowId = createConfigurationWorkflow(integrationInstanceConfigurationId, "workflow-1");

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            CONNECTED_USER_ID, CONNECTION_ID, integrationInstanceConfigurationId);
        IntegrationInstance otherIntegrationInstance = integrationInstanceService.create(
            CONNECTED_USER_ID, CONNECTION_ID, integrationInstanceConfigurationId);

        IntegrationInstanceWorkflow integrationInstanceWorkflow = createInstanceWorkflow(
            integrationInstance.getId(), configurationWorkflowId, Map.of());

        assertThat(remoteIntegrationInstanceWorkflowServiceClient.fetchIntegrationInstanceWorkflow(
            integrationInstance.getId(), "workflow-1"))
                .map(IntegrationInstanceWorkflow::getId)
                .contains(integrationInstanceWorkflow.getId());
        assertThat(remoteIntegrationInstanceWorkflowServiceClient.fetchIntegrationInstanceWorkflow(
            integrationInstance.getId(), "workflow-2"))
                .isEmpty();
        assertThat(remoteIntegrationInstanceWorkflowServiceClient.fetchIntegrationInstanceWorkflow(
            otherIntegrationInstance.getId(), "workflow-1"))
                .isEmpty();
    }

    @Test
    void testGetIntegrationInstanceWorkflowsReturnsTheWorkflowsOfTheInstance() {
        long integrationInstanceConfigurationId = createConfiguration();

        long firstConfigurationWorkflowId = createConfigurationWorkflow(
            integrationInstanceConfigurationId, "workflow-1");
        long secondConfigurationWorkflowId = createConfigurationWorkflow(
            integrationInstanceConfigurationId, "workflow-2");

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            CONNECTED_USER_ID, CONNECTION_ID, integrationInstanceConfigurationId);
        IntegrationInstance otherIntegrationInstance = integrationInstanceService.create(
            CONNECTED_USER_ID, CONNECTION_ID, integrationInstanceConfigurationId);

        IntegrationInstanceWorkflow firstIntegrationInstanceWorkflow = createInstanceWorkflow(
            integrationInstance.getId(), firstConfigurationWorkflowId, Map.of());
        IntegrationInstanceWorkflow secondIntegrationInstanceWorkflow = createInstanceWorkflow(
            integrationInstance.getId(), secondConfigurationWorkflowId, Map.of());

        integrationInstanceWorkflowService.updateEnabled(secondIntegrationInstanceWorkflow.getId(), true);

        createInstanceWorkflow(otherIntegrationInstance.getId(), firstConfigurationWorkflowId, Map.of());

        assertThat(remoteIntegrationInstanceWorkflowServiceClient.getIntegrationInstanceWorkflows(
            integrationInstance.getId()))
                .extracting(IntegrationInstanceWorkflow::getId, IntegrationInstanceWorkflow::isEnabled)
                .containsExactlyInAnyOrder(
                    tuple(firstIntegrationInstanceWorkflow.getId(), false),
                    tuple(secondIntegrationInstanceWorkflow.getId(), true));
        assertThat(remoteIntegrationInstanceWorkflowServiceClient.getIntegrationInstanceWorkflows(Long.MAX_VALUE))
            .isEmpty();
    }

    private long createConfiguration() {
        Integration integration = new Integration();

        integration.setComponentName("airtable");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        return integrationInstanceConfiguration.getId();
    }

    private long createConfigurationWorkflow(long integrationInstanceConfigurationId, String workflowId) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setInputs(Map.of());
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        integrationInstanceConfigurationWorkflow = integrationInstanceConfigurationWorkflowService.create(
            integrationInstanceConfigurationWorkflow);

        return integrationInstanceConfigurationWorkflow.getId();
    }

    private IntegrationInstanceWorkflow createInstanceWorkflow(
        long integrationInstanceId, long integrationInstanceConfigurationWorkflowId, Map<String, ?> inputs) {

        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                integrationInstanceId, integrationInstanceConfigurationWorkflowId);

        integrationInstanceWorkflow.setInputs(inputs);

        integrationInstanceWorkflowService.update(integrationInstanceWorkflow);

        return integrationInstanceWorkflow;
    }
}
