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
import com.bytechef.ee.embedded.configuration.remote.client.EmbeddedConfigurationGrpcTestServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationRemoteServiceGrpcServer;
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
class RemoteIntegrationServiceClientIntTest {

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
    private RemoteIntegrationServiceClient remoteIntegrationServiceClient;

    @BeforeEach
    void beforeEach() throws IOException {
        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new IntegrationRemoteServiceGrpcServer(
                integrationService, new EmbeddedConfigurationProtoMapper(objectMapper)));

        remoteIntegrationServiceClient = new RemoteIntegrationServiceClient(
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
    void testGetIntegrationReturnsTheStoredIntegration() {
        Integration integration = createIntegration("airtable");

        Integration remoteIntegration = remoteIntegrationServiceClient.getIntegration(integration.getId());

        assertThat(remoteIntegration).usingRecursiveComparison()
            .isEqualTo(integrationService.getIntegration(integration.getId()));
        assertThat(remoteIntegration.getComponentName()).isEqualTo("airtable");
        assertThat(remoteIntegration.getName()).isEqualTo(integration.getName());
        assertThat(remoteIntegration.getVersion()).isEqualTo(integration.getVersion());
    }

    @Test
    void testGetIntegrationOfAnUnknownIdFailsWithTheServiceException() {
        assertThatThrownBy(() -> remoteIntegrationServiceClient.getIntegration(Long.MAX_VALUE))
            .isInstanceOfSatisfying(
                StatusRuntimeException.class,
                statusRuntimeException -> {
                    Status status = statusRuntimeException.getStatus();

                    assertThat(status.getCode()).isEqualTo(Status.Code.INTERNAL);
                    assertThat(status.getDescription()).isEqualTo("NoSuchElementException: No value present");
                });
    }

    @Test
    void testGetIntegrationInstanceIntegrationReturnsTheIntegrationOfTheInstance() {
        Integration airtableIntegration = createIntegration("airtable");

        createIntegration("slack");

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            1001L, 42L, createConfiguration(airtableIntegration.getId()));

        assertThat(remoteIntegrationServiceClient.getIntegrationInstanceIntegration(integrationInstance.getId()))
            .usingRecursiveComparison()
            .isEqualTo(integrationService.getIntegration(airtableIntegration.getId()));
    }

    @Test
    void testGetIntegrationsReturnsEveryStoredIntegrationOrderedByComponentName() {
        Integration slackIntegration = createIntegration("slack");
        Integration airtableIntegration = createIntegration("airtable");

        assertThat(remoteIntegrationServiceClient.getIntegrations())
            .extracting(Integration::getId, Integration::getComponentName)
            .containsExactly(
                tuple(airtableIntegration.getId(), "airtable"),
                tuple(slackIntegration.getId(), "slack"));
    }

    @Test
    void testGetIntegrationsByIdsReturnsOnlyTheRequestedIntegrations() {
        Integration airtableIntegration = createIntegration("airtable");
        Integration slackIntegration = createIntegration("slack");

        createIntegration("hubspot");

        assertThat(remoteIntegrationServiceClient.getIntegrations(
            List.of(airtableIntegration.getId(), slackIntegration.getId())))
                .extracting(Integration::getId)
                .containsExactlyInAnyOrder(airtableIntegration.getId(), slackIntegration.getId());
        assertThat(remoteIntegrationServiceClient.getIntegrations(List.of())).isEmpty();
    }

    @Test
    void testGetWorkflowIntegrationReturnsTheIntegrationOwningTheWorkflow() {
        Integration airtableIntegration = createIntegration("airtable");
        Integration slackIntegration = createIntegration("slack");

        integrationWorkflowService.addWorkflow(airtableIntegration.getId(), 1, "airtable-workflow");
        integrationWorkflowService.addWorkflow(slackIntegration.getId(), 1, "slack-workflow");

        assertThat(remoteIntegrationServiceClient.getWorkflowIntegration("slack-workflow"))
            .usingRecursiveComparison()
            .isEqualTo(integrationService.getIntegration(slackIntegration.getId()));
    }

    private long createConfiguration(long integrationId) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        return integrationInstanceConfiguration.getId();
    }

    private Integration createIntegration(String componentName) {
        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setDescription(componentName + " description");
        integration.setName(componentName + "-" + UUID.randomUUID());

        return integrationService.create(integration);
    }
}
