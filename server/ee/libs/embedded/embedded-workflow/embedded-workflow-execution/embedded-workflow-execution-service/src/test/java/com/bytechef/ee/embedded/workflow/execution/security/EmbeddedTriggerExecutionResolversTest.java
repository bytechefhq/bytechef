/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedTriggerExecutionResolversTest {

    private static final long TRIGGER_EXECUTION_ID = 9L;

    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private IntegrationInstanceService integrationInstanceService;
    private IntegrationWorkflowService integrationWorkflowService;
    private TriggerExecutionService triggerExecutionService;
    private EmbeddedTriggerExecutionEnvironmentResolver environmentResolver;
    private EmbeddedTriggerExecutionOwnershipResolver ownershipResolver;

    @BeforeEach
    void setUp() {
        integrationInstanceConfigurationService = mock(IntegrationInstanceConfigurationService.class);
        integrationInstanceService = mock(IntegrationInstanceService.class);
        integrationWorkflowService = mock(IntegrationWorkflowService.class);
        triggerExecutionService = mock(TriggerExecutionService.class);

        environmentResolver = new EmbeddedTriggerExecutionEnvironmentResolver(
            new EmbeddedPrincipalEnvironmentResolver(
                integrationInstanceConfigurationService, integrationInstanceService, integrationWorkflowService),
            triggerExecutionService);
        ownershipResolver = new EmbeddedTriggerExecutionOwnershipResolver(triggerExecutionService);
    }

    @Test
    void testAnEmbeddedTriggerExecutionTakesItsInstanceEnvironment() {
        UUID workflowUuid = UUID.randomUUID();

        IntegrationInstanceConfiguration configuration = new IntegrationInstanceConfiguration();

        configuration.setEnvironment(Environment.STAGING);
        configuration.setId(20L);
        configuration.setIntegrationId(1L);

        IntegrationInstance instance = new IntegrationInstance();

        instance.setId(10L);
        instance.setIntegrationInstanceConfigurationId(20L);

        when(integrationInstanceService.getIntegrationInstances(List.of(10L))).thenReturn(List.of(instance));
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of(20L)))
            .thenReturn(List.of(configuration));
        when(integrationWorkflowService.getIntegrationWorkflows(1L))
            .thenReturn(List.of(new IntegrationWorkflow(1L, 3, "wf-1", workflowUuid)));

        givenTriggerExecution(PlatformType.EMBEDDED, 10L, workflowUuid);

        assertThat(environmentResolver.fetchEnvironment(TRIGGER_EXECUTION_ID)).contains(Environment.STAGING);
        assertThat(ownershipResolver.resolveOwner(TRIGGER_EXECUTION_ID)).isEqualTo(ResourceOwner.ofEmbedded());
    }

    @Test
    void testATriggerExecutionNeverUsesAConfigurationId() {
        UUID workflowUuid = UUID.randomUUID();

        IntegrationInstanceConfiguration configuration = new IntegrationInstanceConfiguration();

        configuration.setEnvironment(Environment.STAGING);
        configuration.setId(10L);
        configuration.setIntegrationId(1L);

        when(integrationInstanceService.getIntegrationInstances(List.of(10L))).thenReturn(List.of());
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of(10L)))
            .thenReturn(List.of(configuration));
        when(integrationWorkflowService.getIntegrationWorkflows(1L))
            .thenReturn(List.of(new IntegrationWorkflow(1L, 3, "wf-1", workflowUuid)));

        givenTriggerExecution(PlatformType.EMBEDDED, 10L, workflowUuid);

        assertThat(environmentResolver.fetchEnvironment(TRIGGER_EXECUTION_ID)).isEmpty();
    }

    @Test
    void testATriggerExecutionWithoutAWorkflowUuidHasNoEnvironment() {
        TriggerExecution triggerExecution = mock(TriggerExecution.class);
        WorkflowExecutionId workflowExecutionId = mock(WorkflowExecutionId.class);

        when(workflowExecutionId.getType()).thenReturn(PlatformType.EMBEDDED);
        when(triggerExecution.getWorkflowExecutionId()).thenReturn(workflowExecutionId);
        when(triggerExecutionService.getTriggerExecutions(List.of(TRIGGER_EXECUTION_ID)))
            .thenReturn(List.of(triggerExecution));

        assertThat(environmentResolver.fetchEnvironment(TRIGGER_EXECUTION_ID)).isEmpty();
    }

    @Test
    void testAnAutomationTriggerExecutionIsUnknown() {
        givenTriggerExecution(PlatformType.AUTOMATION, 10L, UUID.randomUUID());

        assertThat(environmentResolver.fetchEnvironment(TRIGGER_EXECUTION_ID)).isEmpty();
        assertThat(ownershipResolver.resolveOwner(TRIGGER_EXECUTION_ID)).isEqualTo(ResourceOwner.unknown());
    }

    private void givenTriggerExecution(PlatformType type, long principalId, UUID workflowUuid) {
        TriggerExecution triggerExecution = mock(TriggerExecution.class);

        when(triggerExecution.getWorkflowExecutionId())
            .thenReturn(WorkflowExecutionId.of(type, principalId, workflowUuid.toString(), "trigger_1"));
        when(triggerExecutionService.getTriggerExecutions(List.of(TRIGGER_EXECUTION_ID)))
            .thenReturn(List.of(triggerExecution));
    }
}
