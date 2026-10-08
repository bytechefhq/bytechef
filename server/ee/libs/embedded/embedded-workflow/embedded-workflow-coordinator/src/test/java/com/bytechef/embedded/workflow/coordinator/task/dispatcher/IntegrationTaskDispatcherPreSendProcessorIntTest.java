/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.workflow.coordinator.task.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.embedded.workflow.coordinator.config.EmbeddedIntegrationFixtures;
import com.bytechef.embedded.workflow.coordinator.config.EmbeddedWorkflowCoordinatorIntTestConfiguration;
import com.bytechef.embedded.workflow.coordinator.config.EmbeddedWorkflowCoordinatorIntTestSharedMocks;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@EmbeddedWorkflowCoordinatorIntTestSharedMocks
@ExtendWith(ObjectMapperSetupExtension.class)
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = EmbeddedWorkflowCoordinatorIntTestConfiguration.class)
class IntegrationTaskDispatcherPreSendProcessorIntTest {

    private static final long CONFIGURATION_CONNECTION_ID = 77L;
    private static final long CONNECTED_USER_CONNECTION_ID = 88L;
    private static final long CONNECTED_USER_ID = 5L;
    private static final long DECOY_CONNECTION_ID = 66L;
    private static final String INTEGRATION_COMPONENT_NAME = "hubspot";
    private static final long JOB_ID = 1L;
    private static final String WORKFLOW_ID = "workflow1";

    @Autowired
    private EmbeddedIntegrationFixtures embeddedIntegrationFixtures;

    @Autowired
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Autowired
    private JobService jobService;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private IntegrationTaskDispatcherPreSendProcessor processor;

    private IntegrationInstanceConfiguration integrationInstanceConfiguration;
    private IntegrationInstance integrationInstance;

    @BeforeEach
    void beforeEach() {
        Integration integration = embeddedIntegrationFixtures.createIntegration(INTEGRATION_COMPONENT_NAME);

        IntegrationInstanceConfiguration decoyIntegrationInstanceConfiguration =
            embeddedIntegrationFixtures.createIntegrationInstanceConfiguration(integration, "decoy");

        embeddedIntegrationFixtures.createIntegrationInstanceConfigurationWorkflow(
            decoyIntegrationInstanceConfiguration, WORKFLOW_ID,
            List.of(new IntegrationInstanceConfigurationWorkflowConnection(DECOY_CONNECTION_ID, "slack", "slack_1")));

        integrationInstanceConfiguration = embeddedIntegrationFixtures.createIntegrationInstanceConfiguration(
            integration, "configuration");

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            embeddedIntegrationFixtures.createIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfiguration, WORKFLOW_ID,
                List.of(
                    new IntegrationInstanceConfigurationWorkflowConnection(
                        CONFIGURATION_CONNECTION_ID, "slack", "slack_1")));

        integrationInstance = embeddedIntegrationFixtures.createIntegrationInstance(
            integrationInstanceConfiguration, CONNECTED_USER_ID, CONNECTED_USER_CONNECTION_ID);

        embeddedIntegrationFixtures.createIntegrationInstanceWorkflow(
            integrationInstance, integrationInstanceConfigurationWorkflow);
    }

    @AfterEach
    void afterEach() {
        embeddedIntegrationFixtures.deleteAll();
    }

    @Test
    void processResolvesConfigurationLevelConnectionByConfigurationId() {
        assertThat(integrationInstance.getId()).isNotEqualTo(integrationInstanceConfiguration.getId());

        TaskExecution taskExecution = stubEmbeddedJobTask("slack_1", "slack/v1/sendMessage");

        TaskExecution result = processor.process(taskExecution);

        assertThat(result.getMetadata()
            .get(MetadataConstants.CONNECTION_IDS)).isEqualTo(Map.of("slack", CONFIGURATION_CONNECTION_ID));
    }

    @Test
    void processBindsConnectedUserConnectionToIntegrationComponentTask() {
        TaskExecution taskExecution = stubEmbeddedJobTask("hubspot_1", "hubspot/v1/createContact");

        TaskExecution result = processor.process(taskExecution);

        assertThat(result.getMetadata()
            .get(MetadataConstants.CONNECTION_IDS)).isEqualTo(Map.of("hubspot_1", CONNECTED_USER_CONNECTION_ID));
    }

    private TaskExecution stubEmbeddedJobTask(String taskName, String taskType) {
        WorkflowTask workflowTask = mock(WorkflowTask.class);

        when(workflowTask.getName()).thenReturn(taskName);
        when(workflowTask.getType()).thenReturn(taskType);

        Job job = mock(Job.class);

        when(job.getId()).thenReturn(JOB_ID);
        when(job.getWorkflowId()).thenReturn(WORKFLOW_ID);
        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(principalJobService.getJobPrincipalId(JOB_ID, PlatformType.EMBEDDED))
            .thenReturn(integrationInstance.getId());

        JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.EMBEDDED))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getEnvironmentId(integrationInstance.getId())).thenReturn(1L);

        return TaskExecution.builder()
            .jobId(JOB_ID)
            .workflowTask(workflowTask)
            .build();
    }
}
