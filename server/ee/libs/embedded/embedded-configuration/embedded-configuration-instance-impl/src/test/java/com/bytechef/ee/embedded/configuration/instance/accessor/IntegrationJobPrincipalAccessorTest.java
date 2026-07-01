/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.instance.accessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class IntegrationJobPrincipalAccessorTest {

    private static final long INSTANCE_ID = 10L;
    private static final long CONFIGURATION_ID = 99L;
    private static final String WORKFLOW_UUID = "test-uuid";
    private static final String WORKFLOW_ID = "workflow-1";

    @Mock
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Mock
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Mock
    private IntegrationInstanceService integrationInstanceService;

    @Mock
    private IntegrationWorkflowService integrationWorkflowService;

    @Mock
    private IntegrationInstance integrationInstance;

    private IntegrationJobPrincipalAccessor accessor;

    @BeforeEach
    void setUp() {
        accessor = new IntegrationJobPrincipalAccessor(
            integrationInstanceConfigurationService,
            integrationInstanceConfigurationWorkflowService,
            integrationInstanceService,
            integrationWorkflowService);

        when(integrationInstanceService.getIntegrationInstance(INSTANCE_ID)).thenReturn(integrationInstance);
        when(integrationInstance.getIntegrationInstanceConfigurationId()).thenReturn(CONFIGURATION_ID);
    }

    @Test
    void testIsWorkflowEnabledUsesConfigurationIdNotInstanceId() {
        when(integrationWorkflowService.getWorkflowId(INSTANCE_ID, WORKFLOW_UUID)).thenReturn(WORKFLOW_ID);
        when(integrationInstanceConfigurationService
            .isIntegrationInstanceConfigurationEnabled(CONFIGURATION_ID)).thenReturn(true);
        when(integrationInstanceConfigurationWorkflowService
            .isIntegrationInstanceWorkflowEnabled(CONFIGURATION_ID, WORKFLOW_ID)).thenReturn(true);

        boolean result = accessor.isWorkflowEnabled(INSTANCE_ID, WORKFLOW_UUID);

        assertTrue(result);

        verify(integrationInstanceConfigurationService)
            .isIntegrationInstanceConfigurationEnabled(CONFIGURATION_ID);
        verify(integrationInstanceConfigurationWorkflowService)
            .isIntegrationInstanceWorkflowEnabled(CONFIGURATION_ID, WORKFLOW_ID);

        verify(integrationInstanceConfigurationService, never())
            .isIntegrationInstanceConfigurationEnabled(INSTANCE_ID);
        verify(integrationInstanceConfigurationWorkflowService, never())
            .isIntegrationInstanceWorkflowEnabled(INSTANCE_ID, WORKFLOW_ID);
    }

    @Test
    void testIsWorkflowEnabledReturnsFalseWhenConfigurationDisabled() {
        when(integrationInstanceConfigurationService
            .isIntegrationInstanceConfigurationEnabled(CONFIGURATION_ID)).thenReturn(false);

        boolean result = accessor.isWorkflowEnabled(INSTANCE_ID, WORKFLOW_UUID);

        assertFalse(result);
    }

    @Test
    void testGetInputMapUsesConfigurationIdNotInstanceId() {
        when(integrationWorkflowService.getWorkflowId(INSTANCE_ID, WORKFLOW_UUID)).thenReturn(WORKFLOW_ID);

        IntegrationInstanceConfigurationWorkflow configurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        when(integrationInstanceConfigurationWorkflowService
            .getIntegrationInstanceConfigurationWorkflow(CONFIGURATION_ID, WORKFLOW_ID))
                .thenReturn(configurationWorkflow);

        Map<String, ?> result = accessor.getInputMap(INSTANCE_ID, WORKFLOW_UUID);

        assertEquals(configurationWorkflow.getInputs(), result);

        verify(integrationInstanceConfigurationWorkflowService)
            .getIntegrationInstanceConfigurationWorkflow(CONFIGURATION_ID, WORKFLOW_ID);

        verify(integrationInstanceConfigurationWorkflowService, never())
            .getIntegrationInstanceConfigurationWorkflow(INSTANCE_ID, WORKFLOW_ID);
    }
}
