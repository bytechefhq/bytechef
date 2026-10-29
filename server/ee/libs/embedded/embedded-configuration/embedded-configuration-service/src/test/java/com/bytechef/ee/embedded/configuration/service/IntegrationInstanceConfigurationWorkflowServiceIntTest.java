/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

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
class IntegrationInstanceConfigurationWorkflowServiceIntTest {

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

    @AfterEach
    void afterEach() {
        integrationInstanceConfigurationWorkflowRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testFetchIntegrationInstanceConfigurationWorkflowConnectionReturnsTheConnectionOfTheNodeAndKey() {
        long integrationInstanceConfigurationId = createConfiguration();

        createWorkflow(
            integrationInstanceConfigurationId, "workflow-1",
            List.of(
                new IntegrationInstanceConfigurationWorkflowConnection(4L, "airtable", "airtable_1"),
                new IntegrationInstanceConfigurationWorkflowConnection(5L, "airtable", "airtable_2"),
                new IntegrationInstanceConfigurationWorkflowConnection(6L, "slack", "airtable_2")));

        assertThat(
            integrationInstanceConfigurationWorkflowService.fetchIntegrationInstanceConfigurationWorkflowConnection(
                integrationInstanceConfigurationId, "workflow-1", "airtable_2", "airtable"))
                    .map(IntegrationInstanceConfigurationWorkflowConnection::getConnectionId)
                    .contains(5L);
        assertThat(
            integrationInstanceConfigurationWorkflowService.fetchIntegrationInstanceConfigurationWorkflowConnection(
                integrationInstanceConfigurationId, "workflow-1", "airtable_3", "airtable"))
                    .isEmpty();
        assertThat(
            integrationInstanceConfigurationWorkflowService.fetchIntegrationInstanceConfigurationWorkflowConnection(
                integrationInstanceConfigurationId, "workflow-2", "airtable_1", "airtable"))
                    .isEmpty();
    }

    @Test
    void testGetIntegrationInstanceConfigurationWorkflowConnectionReturnsTheConnectionOfTheNodeAndKey() {
        long integrationInstanceConfigurationId = createConfiguration();

        createWorkflow(
            integrationInstanceConfigurationId, "workflow-1",
            List.of(
                new IntegrationInstanceConfigurationWorkflowConnection(4L, "airtable", "airtable_1"),
                new IntegrationInstanceConfigurationWorkflowConnection(6L, "slack", "airtable_1")));

        IntegrationInstanceConfigurationWorkflowConnection integrationInstanceConfigurationWorkflowConnection =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflowConnection(
                integrationInstanceConfigurationId, "workflow-1", "airtable_1", "slack");

        assertThat(integrationInstanceConfigurationWorkflowConnection.getConnectionId()).isEqualTo(6L);
        assertThat(integrationInstanceConfigurationWorkflowConnection.getWorkflowConnectionKey()).isEqualTo("slack");
        assertThat(integrationInstanceConfigurationWorkflowConnection.getWorkflowNodeName()).isEqualTo("airtable_1");
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

    private void createWorkflow(
        long integrationInstanceConfigurationId, String workflowId,
        List<IntegrationInstanceConfigurationWorkflowConnection> connections) {

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setConnections(connections);
        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setInputs(Map.of());
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);
    }
}
