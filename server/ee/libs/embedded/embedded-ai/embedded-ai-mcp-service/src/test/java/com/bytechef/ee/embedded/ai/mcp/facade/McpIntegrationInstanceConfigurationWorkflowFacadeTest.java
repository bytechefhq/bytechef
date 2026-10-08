/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class McpIntegrationInstanceConfigurationWorkflowFacadeTest {

    private static final long INTEGRATION_ID = 5L;
    private static final int INTEGRATION_VERSION = 2;

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade =
        new McpIntegrationInstanceConfigurationWorkflowFacadeImpl(
            integrationInstanceConfigurationService, mock(IntegrationInstanceConfigurationWorkflowService.class),
            mock(IntegrationInstanceWorkflowService.class), integrationWorkflowService,
            mock(McpIntegrationInstanceConfigurationWorkflowService.class), workflowService);

    @BeforeEach
    void beforeEach() {
        stubWorkflow("workflowCallTrigger", """
            {"triggers":[{"name":"trigger_1","type":"workflow/v1/newWorkflowCall"}],"tasks":[]}""");
        stubWorkflow("webhookTrigger", """
            {"triggers":[{"name":"trigger_1","type":"webhook/v1/newRequest"}],"tasks":[]}""");
        stubWorkflow("noTrigger", """
            {"tasks":[]}""");
        stubWorkflow("workflowCallAmongOtherTriggers", """
            {"triggers":[{"name":"trigger_1","type":"webhook/v1/newRequest"},\
            {"name":"trigger_2","type":"workflow/v1/newWorkflowCall"}],"tasks":[]}""");

        when(integrationWorkflowService.getIntegrationWorkflows(INTEGRATION_ID, INTEGRATION_VERSION))
            .thenReturn(
                List.of(
                    integrationWorkflow(1L, "workflowCallTrigger"), integrationWorkflow(2L, "webhookTrigger"),
                    integrationWorkflow(3L, "noTrigger"), integrationWorkflow(4L, "workflowCallAmongOtherTriggers")));
    }

    @Test
    void testGetToolEligibleIntegrationVersionWorkflowsKeepsOnlyWorkflowCallTriggeredWorkflows() {
        List<IntegrationWorkflowDTO> integrationWorkflowDTOs =
            mcpIntegrationInstanceConfigurationWorkflowFacade.getToolEligibleIntegrationVersionWorkflows(
                INTEGRATION_ID, INTEGRATION_VERSION);

        assertThat(integrationWorkflowDTOs)
            .extracting(IntegrationWorkflowDTO::getId)
            .containsExactly("workflowCallTrigger", "workflowCallAmongOtherTriggers");
    }

    @Test
    void testGetToolEligibleIntegrationInstanceConfigurationWorkflowsUsesTheConfiguredIntegrationVersion() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            mock(IntegrationInstanceConfiguration.class);

        when(integrationInstanceConfiguration.getIntegrationId()).thenReturn(INTEGRATION_ID);
        when(integrationInstanceConfiguration.getIntegrationVersion()).thenReturn(INTEGRATION_VERSION);
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(9L))
            .thenReturn(integrationInstanceConfiguration);

        List<IntegrationWorkflowDTO> integrationWorkflowDTOs =
            mcpIntegrationInstanceConfigurationWorkflowFacade
                .getToolEligibleIntegrationInstanceConfigurationWorkflows(9L);

        assertThat(integrationWorkflowDTOs)
            .extracting(IntegrationWorkflowDTO::getId)
            .containsExactly("workflowCallTrigger", "workflowCallAmongOtherTriggers");
    }

    private static IntegrationWorkflow integrationWorkflow(long integrationWorkflowId, String workflowId) {
        IntegrationWorkflow integrationWorkflow = new IntegrationWorkflow(integrationWorkflowId);

        integrationWorkflow.setIntegrationVersion(INTEGRATION_VERSION);
        integrationWorkflow.setWorkflowId(workflowId);

        return integrationWorkflow;
    }

    private void stubWorkflow(String workflowId, String definition) {
        when(workflowService.getWorkflow(workflowId))
            .thenReturn(new Workflow(workflowId, definition, Workflow.Format.JSON));
    }
}
