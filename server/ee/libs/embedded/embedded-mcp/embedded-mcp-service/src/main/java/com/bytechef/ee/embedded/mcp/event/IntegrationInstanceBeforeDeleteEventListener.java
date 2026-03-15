/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.event;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.stereotype.Component;

/**
 * Event listener that handles before-delete events for {@link IntegrationInstance} entities. This listener is
 * responsible for cleaning up related MCP integration instance tool records before the integration instance is deleted.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class IntegrationInstanceBeforeDeleteEventListener
    extends AbstractRelationalEventListener<IntegrationInstance> {

    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceBeforeDeleteEventListener(
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService) {

        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
    }

    @Override
    protected void onBeforeDelete(BeforeDeleteEvent<IntegrationInstance> beforeDeleteEvent) {
        Identifier identifier = beforeDeleteEvent.getId();

        long integrationInstanceId = (Long) identifier.getValue();

        mcpIntegrationInstanceToolService.deleteByIntegrationInstanceId(integrationInstanceId);
    }
}
