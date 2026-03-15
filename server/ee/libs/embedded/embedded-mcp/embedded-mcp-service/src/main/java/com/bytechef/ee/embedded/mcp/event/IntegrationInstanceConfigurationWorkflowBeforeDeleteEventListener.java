/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.event;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.stereotype.Component;

/**
 * Event listener that handles before-delete events for {@link IntegrationInstanceConfigurationWorkflow} entities. This
 * listener is responsible for cleaning up related MCP integration instance configuration workflow records before the
 * integration instance configuration workflow is deleted.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class IntegrationInstanceConfigurationWorkflowBeforeDeleteEventListener
    extends AbstractRelationalEventListener<IntegrationInstanceConfigurationWorkflow> {

    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationWorkflowBeforeDeleteEventListener(
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService) {

        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
    }

    @Override
    protected void onBeforeDelete(
        BeforeDeleteEvent<IntegrationInstanceConfigurationWorkflow> beforeDeleteEvent) {

        Identifier identifier = beforeDeleteEvent.getId();

        long integrationInstanceConfigurationWorkflowId = (Long) identifier.getValue();

        mcpIntegrationInstanceConfigurationWorkflowService.deleteByIntegrationInstanceConfigurationWorkflowId(
            integrationInstanceConfigurationWorkflowId);
    }
}
