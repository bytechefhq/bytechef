/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.workflow.coordinator.trigger.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.embedded.workflow.coordinator.config.EmbeddedIntegrationFixtures;
import com.bytechef.embedded.workflow.coordinator.config.EmbeddedWorkflowCoordinatorIntTestConfiguration;
import com.bytechef.embedded.workflow.coordinator.config.EmbeddedWorkflowCoordinatorIntTestSharedMocks;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
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
@EmbeddedWorkflowCoordinatorIntTestSharedMocks
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = EmbeddedWorkflowCoordinatorIntTestConfiguration.class)
class IntegrationTriggerDispatcherPreSendProcessorIntTest {

    private static final long CONFIGURATION_CONNECTION_ID = 77L;
    private static final long CONNECTED_USER_CONNECTION_ID = 88L;
    private static final long CONNECTED_USER_ID = 5L;
    private static final long DECOY_CONNECTION_ID = 66L;
    private static final String TRIGGER_NAME = "slack_1";
    private static final String WORKFLOW_ID = "workflow1";
    private static final UUID WORKFLOW_UUID = UUID.fromString("6f1d3c1e-7c9b-4a3f-9a4e-2d8f0b1c5e77");

    @Autowired
    private EmbeddedIntegrationFixtures embeddedIntegrationFixtures;

    @Autowired
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Autowired
    private IntegrationTriggerDispatcherPreSendProcessor processor;

    @AfterEach
    void afterEach() {
        embeddedIntegrationFixtures.deleteAll();
    }

    @Test
    void processResolvesConfigurationLevelConnectionByConfigurationId() {
        Integration integration = embeddedIntegrationFixtures.createIntegration("hubspot");

        embeddedIntegrationFixtures.createIntegrationWorkflow(integration, WORKFLOW_ID, WORKFLOW_UUID);

        IntegrationInstanceConfiguration decoyIntegrationInstanceConfiguration =
            embeddedIntegrationFixtures.createIntegrationInstanceConfiguration(integration, "decoy");

        embeddedIntegrationFixtures.createIntegrationInstanceConfigurationWorkflow(
            decoyIntegrationInstanceConfiguration, WORKFLOW_ID,
            List.of(
                new IntegrationInstanceConfigurationWorkflowConnection(DECOY_CONNECTION_ID, "slack", TRIGGER_NAME)));

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            embeddedIntegrationFixtures.createIntegrationInstanceConfiguration(integration, "configuration");

        embeddedIntegrationFixtures.createIntegrationInstanceConfigurationWorkflow(
            integrationInstanceConfiguration, WORKFLOW_ID,
            List.of(
                new IntegrationInstanceConfigurationWorkflowConnection(
                    CONFIGURATION_CONNECTION_ID, "slack", TRIGGER_NAME)));

        IntegrationInstance integrationInstance = embeddedIntegrationFixtures.createIntegrationInstance(
            integrationInstanceConfiguration, CONNECTED_USER_ID, CONNECTED_USER_CONNECTION_ID);

        long integrationInstanceId = integrationInstance.getId();

        assertThat(integrationInstanceId).isNotEqualTo(integrationInstanceConfiguration.getId());

        WorkflowTrigger workflowTrigger = mock(WorkflowTrigger.class);

        when(workflowTrigger.getName()).thenReturn(TRIGGER_NAME);

        TriggerExecution triggerExecution = TriggerExecution.builder()
            .workflowExecutionId(
                WorkflowExecutionId.of(
                    PlatformType.EMBEDDED, integrationInstanceId, WORKFLOW_UUID.toString(), TRIGGER_NAME))
            .workflowTrigger(workflowTrigger)
            .build();

        JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.EMBEDDED))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getEnvironmentId(integrationInstanceId)).thenReturn(1L);

        TriggerExecution result = processor.process(triggerExecution);

        assertThat(result.getMetadata()
            .get(MetadataConstants.CONNECTION_IDS)).isEqualTo(Map.of("slack", CONFIGURATION_CONNECTION_ID));
    }
}
