/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.event;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.stereotype.Component;

/**
 * Event listener that handles before-delete events for {@link IntegrationInstanceConfiguration} entities. This listener
 * is responsible for cleaning up related MCP integration instance configuration records before the integration instance
 * configuration is deleted.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class IntegrationInstanceConfigurationBeforeDeleteEventListener
    extends AbstractRelationalEventListener<IntegrationInstanceConfiguration> {

    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationBeforeDeleteEventListener(
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService) {

        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
    }

    @Override
    protected void onBeforeDelete(
        BeforeDeleteEvent<IntegrationInstanceConfiguration> beforeDeleteEvent) {

        Identifier identifier = beforeDeleteEvent.getId();

        long integrationInstanceConfigurationId = (Long) identifier.getValue();

        mcpIntegrationInstanceConfigurationService.deleteByIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
    }
}
