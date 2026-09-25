/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class McpIntegrationInstanceWorkflowFacadeTest {

    private final ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade =
        mock(ConnectedUserIntegrationInstanceFacade.class);
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService =
        mock(IntegrationInstanceWorkflowService.class);
    private final IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService =
        mock(McpIntegrationInstanceConfigurationWorkflowService.class);
    private final McpIntegrationInstanceWorkflowFacadeImpl mcpIntegrationInstanceWorkflowFacade =
        new McpIntegrationInstanceWorkflowFacadeImpl(
            connectedUserIntegrationInstanceFacade, integrationInstanceWorkflowService, integrationWorkflowService,
            mcpIntegrationInstanceConfigurationWorkflowService);

    @Test
    void testEnableAnotherConnectedUsersIntegrationInstanceWorkflowIsRefused() {
        doThrow(new EmbeddedIntegrationNotVisibleException(7L))
            .when(connectedUserIntegrationInstanceFacade)
            .checkIntegrationInstanceOwner("external-user-2", 7L);

        assertThatThrownBy(
            () -> mcpIntegrationInstanceWorkflowFacade.enableMcpIntegrationInstanceWorkflow(
                "external-user-2", 7L, "workflow-uuid", true))
                    .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        verifyNoInteractions(integrationInstanceWorkflowService, integrationWorkflowService);
    }

    @Test
    void testUpdateAnotherConnectedUsersIntegrationInstanceWorkflowIsRefused() {
        doThrow(new EmbeddedIntegrationNotVisibleException(7L))
            .when(connectedUserIntegrationInstanceFacade)
            .checkIntegrationInstanceOwner("external-user-2", 7L);

        assertThatThrownBy(
            () -> mcpIntegrationInstanceWorkflowFacade.updateMcpIntegrationInstanceWorkflow(
                "external-user-2", 7L, "workflow-uuid", Map.of("key", "value")))
                    .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        verifyNoInteractions(integrationInstanceWorkflowService, integrationWorkflowService);
    }

    @Test
    void testEnableOwnIntegrationInstanceWorkflow() {
        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            mock(McpIntegrationInstanceConfigurationWorkflow.class);
        IntegrationInstanceWorkflow integrationInstanceWorkflow = mock(IntegrationInstanceWorkflow.class);

        when(integrationWorkflowService.getWorkflowId(7L, "workflow-uuid")).thenReturn("workflow-id");
        when(mcpIntegrationInstanceConfigurationWorkflowService
            .fetchMcpIntegrationInstanceConfigurationWorkflowByWorkflowId("workflow-id"))
                .thenReturn(Optional.of(mcpIntegrationInstanceConfigurationWorkflow));
        when(mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId())
            .thenReturn(11L);
        when(integrationInstanceWorkflow.getId()).thenReturn(13L);
        when(integrationInstanceWorkflowService.fetchIntegrationInstanceWorkflow(7L, 11L))
            .thenReturn(Optional.of(integrationInstanceWorkflow));

        mcpIntegrationInstanceWorkflowFacade.enableMcpIntegrationInstanceWorkflow(
            "external-user-1", 7L, "workflow-uuid", true);

        verify(connectedUserIntegrationInstanceFacade).checkIntegrationInstanceOwner("external-user-1", 7L);
        verify(integrationInstanceWorkflowService).updateEnabled(13L, true);
    }
}
