/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.workflow.coordinator.config;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class EmbeddedIntegrationFixtures {

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowRepository integrationInstanceConfigurationWorkflowRepository;

    @Autowired
    private IntegrationInstanceRepository integrationInstanceRepository;

    @Autowired
    private IntegrationInstanceWorkflowRepository integrationInstanceWorkflowRepository;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    public void deleteAll() {
        integrationInstanceWorkflowRepository.deleteAll();
        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationWorkflowRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    public Integration createIntegration(String componentName) {
        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setComponentVersion(1);
        integration.setName(componentName);

        return integrationRepository.save(integration);
    }

    public IntegrationInstance createIntegrationInstance(
        IntegrationInstanceConfiguration integrationInstanceConfiguration, long connectedUserId, long connectionId) {

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectedUserId(connectedUserId);
        integrationInstance.setConnectionId(connectionId);
        integrationInstance.setEnabled(true);
        integrationInstance.setIntegrationInstanceConfigurationId(integrationInstanceConfiguration.getId());

        return integrationInstanceRepository.save(integrationInstance);
    }

    public IntegrationInstanceConfiguration createIntegrationInstanceConfiguration(
        Integration integration, String name) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setEnabled(true);
        integrationInstanceConfiguration.setEnvironment(Environment.DEVELOPMENT);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName(name);

        return integrationInstanceConfigurationRepository.save(integrationInstanceConfiguration);
    }

    public IntegrationInstanceConfigurationWorkflow createIntegrationInstanceConfigurationWorkflow(
        IntegrationInstanceConfiguration integrationInstanceConfiguration, String workflowId,
        List<IntegrationInstanceConfigurationWorkflowConnection> connections) {

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setConnections(connections);
        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfiguration.getId());
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        return integrationInstanceConfigurationWorkflowRepository.save(integrationInstanceConfigurationWorkflow);
    }

    public IntegrationInstanceWorkflow createIntegrationInstanceWorkflow(
        IntegrationInstance integrationInstance,
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        IntegrationInstanceWorkflow integrationInstanceWorkflow = new IntegrationInstanceWorkflow();

        integrationInstanceWorkflow.setEnabled(true);
        integrationInstanceWorkflow.setInputs(Map.of());
        integrationInstanceWorkflow.setIntegrationInstanceConfigurationWorkflowId(
            integrationInstanceConfigurationWorkflow.getId());
        integrationInstanceWorkflow.setIntegrationInstanceId(integrationInstance.getId());

        return integrationInstanceWorkflowRepository.save(integrationInstanceWorkflow);
    }

    public IntegrationWorkflow createIntegrationWorkflow(Integration integration, String workflowId, UUID uuid) {
        return integrationWorkflowRepository.save(new IntegrationWorkflow(integration.getId(), 1, workflowId, uuid));
    }
}
