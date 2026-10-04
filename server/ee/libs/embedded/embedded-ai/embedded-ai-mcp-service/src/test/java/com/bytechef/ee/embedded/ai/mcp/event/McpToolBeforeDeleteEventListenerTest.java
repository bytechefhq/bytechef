/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.platform.mcp.domain.McpTool;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class McpToolBeforeDeleteEventListenerTest {

    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService =
        mock(McpIntegrationInstanceToolService.class);
    private final McpToolBeforeDeleteEventListener mcpToolBeforeDeleteEventListener =
        new McpToolBeforeDeleteEventListener(mcpIntegrationInstanceToolService);

    @Test
    @SuppressWarnings("unchecked")
    void testOnBeforeDeleteRemovesIntegrationInstanceTools() {
        BeforeDeleteEvent<McpTool> beforeDeleteEvent = mock(BeforeDeleteEvent.class);
        Identifier identifier = mock(Identifier.class);

        when(identifier.getValue()).thenReturn(1052L);
        when(beforeDeleteEvent.getId()).thenReturn(identifier);

        mcpToolBeforeDeleteEventListener.onBeforeDelete(beforeDeleteEvent);

        verify(mcpIntegrationInstanceToolService).deleteByMcpToolId(1052L);
    }
}
