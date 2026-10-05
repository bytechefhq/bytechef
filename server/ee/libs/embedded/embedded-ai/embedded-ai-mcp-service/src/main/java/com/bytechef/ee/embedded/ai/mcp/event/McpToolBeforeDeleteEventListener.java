/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.event;

import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.mcp.domain.McpTool;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class McpToolBeforeDeleteEventListener extends AbstractRelationalEventListener<McpTool> {

    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @SuppressFBWarnings("EI")
    public McpToolBeforeDeleteEventListener(McpIntegrationInstanceToolService mcpIntegrationInstanceToolService) {
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
    }

    @Override
    protected void onBeforeDelete(BeforeDeleteEvent<McpTool> beforeDeleteEvent) {
        Identifier identifier = beforeDeleteEvent.getId();

        long mcpToolId = (Long) identifier.getValue();

        mcpIntegrationInstanceToolService.deleteByMcpToolId(mcpToolId);
    }
}
