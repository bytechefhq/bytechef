/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.remote.client.EmbeddedConfigurationGrpcTestServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationWorkflowRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
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
class RemoteIntegrationWorkflowServiceClientIntTest {

    private static final long CONNECTED_USER_ID = 1001L;
    private static final long CONNECTION_ID = 42L;

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

    private EmbeddedConfigurationGrpcTestServer embeddedConfigurationGrpcTestServer;
    private RemoteIntegrationWorkflowServiceClient remoteIntegrationWorkflowServiceClient;

    @BeforeEach
    void beforeEach() throws IOException {
        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new IntegrationWorkflowRemoteServiceGrpcServer(
                integrationWorkflowService, new EmbeddedConfigurationProtoMapper(objectMapper)));

        remoteIntegrationWorkflowServiceClient = new RemoteIntegrationWorkflowServiceClient(
            embeddedConfigurationGrpcTestServer.getGrpcChannels(), objectMapper);
    }

    @AfterEach
    void afterEach() {
        embeddedConfigurationGrpcTestServer.shutdown();

        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testGetWorkflowIntegrationWorkflowReturnsTheStoredIntegrationWorkflow() {
        long integrationId = createIntegration();

        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.addWorkflow(
            integrationId, 1, "workflow-1");

        integrationWorkflowService.updatePermissionExpression(integrationWorkflow.getId(), "hasRole('ADMIN')");

        integrationWorkflowService.addWorkflow(integrationId, 1, "workflow-2");

        IntegrationWorkflow remoteIntegrationWorkflow = remoteIntegrationWorkflowServiceClient
            .getWorkflowIntegrationWorkflow("workflow-1");

        assertThat(remoteIntegrationWorkflow).usingRecursiveComparison()
            .isEqualTo(integrationWorkflowService.getIntegrationWorkflow(integrationWorkflow.getId()));
        assertThat(remoteIntegrationWorkflow.getUuidAsString()).isEqualTo(integrationWorkflow.getUuidAsString());
        assertThat(remoteIntegrationWorkflow.getPermissionExpression()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void testGetWorkflowIntegrationWorkflowOfAnUnknownWorkflowIsAnIllegalArgument() {
        assertThatThrownBy(() -> remoteIntegrationWorkflowServiceClient.getWorkflowIntegrationWorkflow("unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Workflow not found for id: unknown");
    }

    @Test
    void testGetIntegrationWorkflowsReturnsAllByIntegrationAndByIntegrations() {
        long airtableIntegrationId = createIntegration();
        long slackIntegrationId = createIntegration();
        long hubspotIntegrationId = createIntegration();

        IntegrationWorkflow airtableIntegrationWorkflow = integrationWorkflowService.addWorkflow(
            airtableIntegrationId, 1, "airtable-workflow");
        IntegrationWorkflow slackIntegrationWorkflow = integrationWorkflowService.addWorkflow(
            slackIntegrationId, 1, "slack-workflow");
        IntegrationWorkflow hubspotIntegrationWorkflow = integrationWorkflowService.addWorkflow(
            hubspotIntegrationId, 1, "hubspot-workflow");

        assertThat(remoteIntegrationWorkflowServiceClient.getIntegrationWorkflows())
            .extracting(IntegrationWorkflow::getId)
            .containsExactlyInAnyOrder(
                airtableIntegrationWorkflow.getId(), slackIntegrationWorkflow.getId(),
                hubspotIntegrationWorkflow.getId());
        assertThat(remoteIntegrationWorkflowServiceClient.getIntegrationWorkflows(slackIntegrationId))
            .singleElement()
            .usingRecursiveComparison()
            .isEqualTo(integrationWorkflowService.getIntegrationWorkflow(slackIntegrationWorkflow.getId()));
        assertThat(remoteIntegrationWorkflowServiceClient.getIntegrationWorkflows(
            List.of(airtableIntegrationId, hubspotIntegrationId)))
                .extracting(IntegrationWorkflow::getWorkflowId)
                .containsExactlyInAnyOrder("airtable-workflow", "hubspot-workflow");
        assertThat(remoteIntegrationWorkflowServiceClient.getIntegrationWorkflows(List.of())).isEmpty();
    }

    @Test
    void testGetLastWorkflowIdReturnsTheWorkflowOfTheHighestIntegrationVersion() {
        long integrationId = createIntegration();

        IntegrationWorkflow version1IntegrationWorkflow = integrationWorkflowService.addWorkflow(
            integrationId, 1, "workflow-v1");

        UUID workflowUuid = UUID.fromString(version1IntegrationWorkflow.getUuidAsString());

        integrationWorkflowRepository.save(new IntegrationWorkflow(integrationId, 2, "workflow-v2", workflowUuid));

        assertThat(remoteIntegrationWorkflowServiceClient.getLastWorkflowId(workflowUuid.toString()))
            .isEqualTo("workflow-v2");
    }

    @Test
    void testGetLastWorkflowIdOfAnUnknownUuidIsAnIllegalArgument() {
        String unknownWorkflowUuid = String.valueOf(UUID.randomUUID());

        assertThatThrownBy(() -> remoteIntegrationWorkflowServiceClient.getLastWorkflowId(unknownWorkflowUuid))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Workflow not found for uuid: " + unknownWorkflowUuid);
    }

    @Test
    void testGetLastWorkflowIdByEnvironmentFollowsTheVersionTheEnvironmentRuns() {
        long integrationId = createIntegration();

        IntegrationWorkflow version1IntegrationWorkflow = integrationWorkflowService.addWorkflow(
            integrationId, 1, "workflow-v1");

        UUID workflowUuid = UUID.fromString(version1IntegrationWorkflow.getUuidAsString());

        integrationWorkflowRepository.save(new IntegrationWorkflow(integrationId, 2, "workflow-v2", workflowUuid));

        long productionConfigurationId = createConfiguration(integrationId, 1, Environment.PRODUCTION);
        long stagingConfigurationId = createConfiguration(integrationId, 2, Environment.STAGING);

        integrationInstanceService.create(CONNECTED_USER_ID, CONNECTION_ID, productionConfigurationId);
        integrationInstanceService.create(CONNECTED_USER_ID, CONNECTION_ID, stagingConfigurationId);

        assertThat(remoteIntegrationWorkflowServiceClient.getLastWorkflowId(
            workflowUuid.toString(), Environment.PRODUCTION)).isEqualTo("workflow-v1");
        assertThat(remoteIntegrationWorkflowServiceClient.getLastWorkflowId(
            workflowUuid.toString(), Environment.STAGING)).isEqualTo("workflow-v2");
        assertThat(remoteIntegrationWorkflowServiceClient.fetchLastWorkflowId(
            workflowUuid.toString(), Environment.PRODUCTION)).contains("workflow-v1");
        assertThat(remoteIntegrationWorkflowServiceClient.fetchLastWorkflowId(
            workflowUuid.toString(), Environment.DEVELOPMENT)).isEmpty();
        assertThatThrownBy(() -> remoteIntegrationWorkflowServiceClient.getLastWorkflowId(
            workflowUuid.toString(), Environment.DEVELOPMENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Workflow not found for uuid: " + workflowUuid);
    }

    @Test
    void testGetWorkflowIdReturnsTheWorkflowOfTheVersionTheInstanceRuns() {
        long integrationId = createIntegration();

        IntegrationWorkflow version1IntegrationWorkflow = integrationWorkflowService.addWorkflow(
            integrationId, 1, "workflow-v1");

        UUID workflowUuid = UUID.fromString(version1IntegrationWorkflow.getUuidAsString());

        integrationWorkflowRepository.save(new IntegrationWorkflow(integrationId, 2, "workflow-v2", workflowUuid));

        IntegrationInstance version1IntegrationInstance = integrationInstanceService.create(
            CONNECTED_USER_ID, CONNECTION_ID, createConfiguration(integrationId, 1, Environment.PRODUCTION));
        IntegrationInstance version2IntegrationInstance = integrationInstanceService.create(
            CONNECTED_USER_ID, CONNECTION_ID, createConfiguration(integrationId, 2, Environment.PRODUCTION));

        assertThat(remoteIntegrationWorkflowServiceClient.getWorkflowId(
            version1IntegrationInstance.getId(), workflowUuid.toString())).isEqualTo("workflow-v1");
        assertThat(remoteIntegrationWorkflowServiceClient.getWorkflowId(
            version2IntegrationInstance.getId(), workflowUuid.toString())).isEqualTo("workflow-v2");

        String unknownWorkflowUuid = String.valueOf(UUID.randomUUID());

        assertThatThrownBy(() -> remoteIntegrationWorkflowServiceClient.getWorkflowId(
            version1IntegrationInstance.getId(), unknownWorkflowUuid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Workflow not found for uuid: " + unknownWorkflowUuid);
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

        integration.setComponentName("airtable");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        return integration.getId();
    }
}
