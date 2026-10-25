/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.validator;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.validator.WorkflowDeploymentConnectionResolver;
import com.bytechef.platform.workflow.validator.WorkflowDeploymentConnectionResolver.WorkflowNodeConnection;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class IntegrationInstanceWorkflowConnectionResolverIntTest {

    private static final String AI_AGENT_NODE_NAME = "aiAgent_1";
    private static final String COMPONENT_NAME = "slack";

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
    private List<WorkflowDeploymentConnectionResolver> workflowDeploymentConnectionResolvers;

    private Integration integration;
    private String workflowId;

    @BeforeEach
    void beforeEach() {
        Integration newIntegration = new Integration();

        newIntegration.setComponentName(COMPONENT_NAME);
        newIntegration.setName("Slack " + UUID.randomUUID());

        integration = integrationService.create(newIntegration);

        workflowId = UUID.randomUUID()
            .toString();
    }

    @AfterEach
    void afterEach() {
        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationWorkflowRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testGetWorkflowNodeConnectionsReturnsTheConnectionsAnEnabledIntegrationInstanceBindsToTheNode() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = createIntegrationInstanceConfiguration(
            Environment.PRODUCTION, true, true,
            new IntegrationInstanceConfigurationWorkflowConnection(7L, "openAi_1", AI_AGENT_NODE_NAME),
            new IntegrationInstanceConfigurationWorkflowConnection(8L, "affinity", "affinity_1"));

        createIntegrationInstance(integrationInstanceConfiguration, 41L, true);
        createIntegrationInstance(integrationInstanceConfiguration, 42L, false);

        assertThat(getWorkflowNodeConnections(AI_AGENT_NODE_NAME, Environment.PRODUCTION))
            .containsExactlyInAnyOrder(
                new WorkflowNodeConnection(7L, "openAi_1"), new WorkflowNodeConnection(41L, COMPONENT_NAME));
    }

    @Test
    void testGetWorkflowNodeConnectionsIgnoresAnotherEnvironment() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = createIntegrationInstanceConfiguration(
            Environment.PRODUCTION, true, true,
            new IntegrationInstanceConfigurationWorkflowConnection(7L, "openAi_1", AI_AGENT_NODE_NAME));

        createIntegrationInstance(integrationInstanceConfiguration, 41L, true);

        assertThat(getWorkflowNodeConnections(AI_AGENT_NODE_NAME, Environment.DEVELOPMENT)).isEmpty();
    }

    @Test
    void testGetWorkflowNodeConnectionsIgnoresADisabledIntegrationInstanceConfiguration() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = createIntegrationInstanceConfiguration(
            Environment.PRODUCTION, false, true,
            new IntegrationInstanceConfigurationWorkflowConnection(7L, "openAi_1", AI_AGENT_NODE_NAME));

        createIntegrationInstance(integrationInstanceConfiguration, 41L, true);

        assertThat(getWorkflowNodeConnections(AI_AGENT_NODE_NAME, Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testGetWorkflowNodeConnectionsIgnoresADisabledIntegrationInstanceConfigurationWorkflow() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = createIntegrationInstanceConfiguration(
            Environment.PRODUCTION, true, false,
            new IntegrationInstanceConfigurationWorkflowConnection(7L, "openAi_1", AI_AGENT_NODE_NAME));

        createIntegrationInstance(integrationInstanceConfiguration, 41L, true);

        assertThat(getWorkflowNodeConnections(AI_AGENT_NODE_NAME, Environment.PRODUCTION)).isEmpty();
    }

    private void createIntegrationInstance(
        IntegrationInstanceConfiguration integrationInstanceConfiguration, long connectionId, boolean enabled) {

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            1L, connectionId, integrationInstanceConfiguration.getId());

        integrationInstanceService.updateEnabled(integrationInstance.getId(), enabled);
    }

    private IntegrationInstanceConfiguration createIntegrationInstanceConfiguration(
        Environment environment, boolean enabled, boolean workflowEnabled,
        IntegrationInstanceConfigurationWorkflowConnection... connections) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("Slack");

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfiguration.getId(), enabled);

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setConnections(List.of(connections));
        integrationInstanceConfigurationWorkflow.setEnabled(workflowEnabled);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfiguration.getId());
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

        return integrationInstanceConfiguration;
    }

    private List<WorkflowNodeConnection> getWorkflowNodeConnections(
        String workflowNodeName, Environment environment) {

        List<WorkflowNodeConnection> workflowNodeConnections = new ArrayList<>();

        for (WorkflowDeploymentConnectionResolver workflowDeploymentConnectionResolver : workflowDeploymentConnectionResolvers) {
            workflowNodeConnections.addAll(
                workflowDeploymentConnectionResolver.getWorkflowNodeConnections(
                    workflowId, workflowNodeName, environment.ordinal()));
        }

        return workflowNodeConnections;
    }
}
