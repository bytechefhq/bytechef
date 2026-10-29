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
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.remote.client.EmbeddedConfigurationGrpcTestServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationInstanceConfigurationWorkflowRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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
class RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClientIntTest {

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowRepository integrationInstanceConfigurationWorkflowRepository;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private ObjectMapper objectMapper;

    private EmbeddedConfigurationGrpcTestServer embeddedConfigurationGrpcTestServer;
    private RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient remoteClient;

    @BeforeEach
    void beforeEach() throws IOException {
        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new IntegrationInstanceConfigurationWorkflowRemoteServiceGrpcServer(
                integrationInstanceConfigurationWorkflowService, new EmbeddedConfigurationProtoMapper(objectMapper)));

        remoteClient = new RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient(
            embeddedConfigurationGrpcTestServer.getGrpcChannels(), objectMapper);
    }

    @AfterEach
    void afterEach() {
        embeddedConfigurationGrpcTestServer.shutdown();

        integrationInstanceConfigurationWorkflowRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testGetIntegrationInstanceConfigurationWorkflowReturnsTheStoredWorkflowWithItsInputsAndConnections() {
        long integrationInstanceConfigurationId = createConfiguration();

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow = createWorkflow(
            integrationInstanceConfigurationId, "workflow-1", true,
            Map.of("id", 9_007_199_254_740_993L, "ratio", 0.5, "name", "leads"),
            List.of(
                new IntegrationInstanceConfigurationWorkflowConnection(4L, "airtable", "airtable_1"),
                new IntegrationInstanceConfigurationWorkflowConnection(5L, "slack", "slack_1")));

        createWorkflow(integrationInstanceConfigurationId, "workflow-2", false, Map.of(), List.of());

        IntegrationInstanceConfigurationWorkflow storedIntegrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfigurationWorkflow.getId());

        IntegrationInstanceConfigurationWorkflow remoteIntegrationInstanceConfigurationWorkflow =
            remoteClient.getIntegrationInstanceConfigurationWorkflow(integrationInstanceConfigurationId, "workflow-1");

        assertThat(remoteIntegrationInstanceConfigurationWorkflow).usingRecursiveComparison()
            .isEqualTo(storedIntegrationInstanceConfigurationWorkflow);
        assertThat(remoteIntegrationInstanceConfigurationWorkflow.getInputs())
            .isEqualTo(Map.of("id", 9_007_199_254_740_993L, "ratio", 0.5, "name", "leads"));
        assertThat(remoteIntegrationInstanceConfigurationWorkflow.getConnections())
            .extracting(
                IntegrationInstanceConfigurationWorkflowConnection::getConnectionId,
                IntegrationInstanceConfigurationWorkflowConnection::getWorkflowConnectionKey,
                IntegrationInstanceConfigurationWorkflowConnection::getWorkflowNodeName)
            .containsExactlyInAnyOrder(tuple(4L, "airtable", "airtable_1"), tuple(5L, "slack", "slack_1"));
    }

    @Test
    void testGetIntegrationInstanceConfigurationWorkflowByIdReturnsTheStoredWorkflow() {
        long integrationInstanceConfigurationId = createConfiguration();

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow = createWorkflow(
            integrationInstanceConfigurationId, "workflow-1", true, Map.of("count", 5),
            List.of(new IntegrationInstanceConfigurationWorkflowConnection(4L, "airtable", "airtable_1")));

        assertThat(remoteClient.getIntegrationInstanceConfigurationWorkflow(
            integrationInstanceConfigurationWorkflow.getId()))
                .usingRecursiveComparison()
                .isEqualTo(
                    integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                        integrationInstanceConfigurationWorkflow.getId()));
    }

    @Test
    void testGetIntegrationInstanceConfigurationWorkflowByUnknownIdIsAnIllegalArgument() {
        assertThatThrownBy(() -> remoteClient.getIntegrationInstanceConfigurationWorkflow(Long.MAX_VALUE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Integration instance configuration workflow not found");
    }

    @Test
    void testGetIntegrationInstanceConfigurationWorkflowsReturnsTheWorkflowsOfTheConfiguration() {
        long integrationInstanceConfigurationId = createConfiguration();
        long otherIntegrationInstanceConfigurationId = createConfiguration();

        IntegrationInstanceConfigurationWorkflow firstIntegrationInstanceConfigurationWorkflow = createWorkflow(
            integrationInstanceConfigurationId, "workflow-1", true, Map.of(), List.of());
        IntegrationInstanceConfigurationWorkflow secondIntegrationInstanceConfigurationWorkflow = createWorkflow(
            integrationInstanceConfigurationId, "workflow-2", false, Map.of(), List.of());

        createWorkflow(otherIntegrationInstanceConfigurationId, "workflow-3", true, Map.of(), List.of());

        assertThat(remoteClient.getIntegrationInstanceConfigurationWorkflows(integrationInstanceConfigurationId))
            .extracting(IntegrationInstanceConfigurationWorkflow::getId,
                IntegrationInstanceConfigurationWorkflow::isEnabled)
            .containsExactlyInAnyOrder(
                tuple(firstIntegrationInstanceConfigurationWorkflow.getId(), true),
                tuple(secondIntegrationInstanceConfigurationWorkflow.getId(), false));
        assertThat(remoteClient.getIntegrationInstanceConfigurationWorkflows(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void testGetIntegrationInstanceConfigurationWorkflowConnectionReturnsTheConnectionOfTheNode() {
        long integrationInstanceConfigurationId = createConfiguration();

        createWorkflow(
            integrationInstanceConfigurationId, "workflow-1", true, Map.of(),
            List.of(
                new IntegrationInstanceConfigurationWorkflowConnection(4L, "airtable", "airtable_1"),
                new IntegrationInstanceConfigurationWorkflowConnection(5L, "airtable", "airtable_2")));

        IntegrationInstanceConfigurationWorkflowConnection integrationInstanceConfigurationWorkflowConnection =
            remoteClient.getIntegrationInstanceConfigurationWorkflowConnection(
                integrationInstanceConfigurationId, "workflow-1", "airtable_2", "airtable");

        assertThat(integrationInstanceConfigurationWorkflowConnection.getConnectionId()).isEqualTo(5L);
        assertThat(integrationInstanceConfigurationWorkflowConnection.getWorkflowConnectionKey()).isEqualTo("airtable");
        assertThat(integrationInstanceConfigurationWorkflowConnection.getWorkflowNodeName()).isEqualTo("airtable_2");
    }

    @Test
    void testFetchIntegrationInstanceConfigurationWorkflowConnectionIsEmptyForAnUnconnectedNode() {
        long integrationInstanceConfigurationId = createConfiguration();

        createWorkflow(
            integrationInstanceConfigurationId, "workflow-1", true, Map.of(),
            List.of(new IntegrationInstanceConfigurationWorkflowConnection(4L, "airtable", "airtable_1")));

        assertThat(remoteClient.fetchIntegrationInstanceConfigurationWorkflowConnection(
            integrationInstanceConfigurationId, "workflow-1", "airtable_1", "airtable"))
                .map(IntegrationInstanceConfigurationWorkflowConnection::getConnectionId)
                .contains(4L);
        assertThat(remoteClient.fetchIntegrationInstanceConfigurationWorkflowConnection(
            integrationInstanceConfigurationId, "workflow-1", "airtable_2", "airtable"))
                .isEmpty();
        assertThat(remoteClient.fetchIntegrationInstanceConfigurationWorkflowConnection(
            integrationInstanceConfigurationId, "workflow-2", "airtable_1", "airtable"))
                .isEmpty();
    }

    @Test
    void testGetIntegrationInstanceConfigurationWorkflowConnectionsReturnsEveryConnectionOfTheNode() {
        long integrationInstanceConfigurationId = createConfiguration();

        createWorkflow(
            integrationInstanceConfigurationId, "workflow-1", true, Map.of(),
            List.of(
                new IntegrationInstanceConfigurationWorkflowConnection(4L, "airtable", "airtable_1"),
                new IntegrationInstanceConfigurationWorkflowConnection(5L, "airtableSecondary", "airtable_1"),
                new IntegrationInstanceConfigurationWorkflowConnection(6L, "slack", "slack_1")));

        assertThat(remoteClient.getIntegrationInstanceConfigurationWorkflowConnections(
            integrationInstanceConfigurationId, "workflow-1", "airtable_1"))
                .extracting(IntegrationInstanceConfigurationWorkflowConnection::getConnectionId)
                .containsExactlyInAnyOrder(4L, 5L);
        assertThat(remoteClient.getIntegrationInstanceConfigurationWorkflowConnections(
            integrationInstanceConfigurationId, "workflow-1", "hubspot_1"))
                .isEmpty();
    }

    @Test
    void testIsConnectionUsedReflectsWhetherAnyWorkflowUsesTheConnection() {
        long integrationInstanceConfigurationId = createConfiguration();

        createWorkflow(
            integrationInstanceConfigurationId, "workflow-1", true, Map.of(),
            List.of(new IntegrationInstanceConfigurationWorkflowConnection(4L, "airtable", "airtable_1")));

        assertThat(remoteClient.isConnectionUsed(4L)).isTrue();
        assertThat(remoteClient.isConnectionUsed(5L)).isFalse();
    }

    @Test
    void testIsIntegrationInstanceWorkflowEnabledReflectsTheStoredFlag() {
        long integrationInstanceConfigurationId = createConfiguration();

        createWorkflow(integrationInstanceConfigurationId, "workflow-1", true, Map.of(), List.of());
        createWorkflow(integrationInstanceConfigurationId, "workflow-2", false, Map.of(), List.of());

        assertThat(remoteClient.isIntegrationInstanceWorkflowEnabled(integrationInstanceConfigurationId, "workflow-1"))
            .isTrue();
        assertThat(remoteClient.isIntegrationInstanceWorkflowEnabled(integrationInstanceConfigurationId, "workflow-2"))
            .isFalse();
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

    private IntegrationInstanceConfigurationWorkflow createWorkflow(
        long integrationInstanceConfigurationId, String workflowId, boolean enabled, Map<String, ?> inputs,
        List<IntegrationInstanceConfigurationWorkflowConnection> connections) {

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setConnections(connections);
        integrationInstanceConfigurationWorkflow.setEnabled(enabled);
        integrationInstanceConfigurationWorkflow.setInputs(inputs);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        return integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);
    }
}
