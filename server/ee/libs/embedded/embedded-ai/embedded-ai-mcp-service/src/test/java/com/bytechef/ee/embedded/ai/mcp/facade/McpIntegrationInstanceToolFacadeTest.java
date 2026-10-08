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

import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationInstanceFacade;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class McpIntegrationInstanceToolFacadeTest {

    private final ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade =
        mock(ConnectedUserIntegrationInstanceFacade.class);
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService =
        mock(McpIntegrationInstanceToolService.class);
    private final McpIntegrationInstanceToolFacadeImpl mcpIntegrationInstanceToolFacade =
        new McpIntegrationInstanceToolFacadeImpl(
            connectedUserIntegrationInstanceFacade, mcpIntegrationInstanceToolService);

    @Test
    void testEnableAnotherConnectedUsersIntegrationInstanceToolIsRefused() {
        doThrow(new EmbeddedIntegrationNotVisibleException(7L))
            .when(connectedUserIntegrationInstanceFacade)
            .checkIntegrationInstanceOwner("external-user-2", 7L);

        assertThatThrownBy(
            () -> mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool("external-user-2", 7L, 5L, true))
                .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        verifyNoInteractions(mcpIntegrationInstanceToolService);
    }

    @Test
    void testEnableOwnIntegrationInstanceTool() {
        when(mcpIntegrationInstanceToolService.fetchMcpIntegrationInstanceTool(7L, 5L)).thenReturn(Optional.empty());

        mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool("external-user-1", 7L, 5L, true);

        verify(connectedUserIntegrationInstanceFacade).checkIntegrationInstanceOwner("external-user-1", 7L);
        verify(mcpIntegrationInstanceToolService).createMcpIntegrationInstanceTool(7L, 5L, true);
    }
}
