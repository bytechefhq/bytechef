/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.platform.mcp.domain.McpTool;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.conversion.MutableAggregateChange;
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
    void testOnBeforeDeleteRemovesIntegrationInstanceTools() {
        McpTool mcpTool = new McpTool("sendEmail", Map.of(), 1L);

        mcpTool.setId(1052L);

        mcpToolBeforeDeleteEventListener.onApplicationEvent(
            new BeforeDeleteEvent<>(
                Identifier.of(1052L), mcpTool, MutableAggregateChange.forDelete(McpTool.class)));

        verify(mcpIntegrationInstanceToolService).deleteByMcpToolId(1052L);
    }
}
