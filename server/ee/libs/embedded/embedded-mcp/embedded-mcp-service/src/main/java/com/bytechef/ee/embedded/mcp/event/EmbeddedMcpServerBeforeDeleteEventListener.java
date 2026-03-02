/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.event;

import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegration;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationWorkflowService;
import com.bytechef.platform.mcp.domain.McpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.stereotype.Component;

/**
 * Event listener that handles before-delete events for {@link McpServer} entities. This listener is responsible for
 * cleaning up related MCP integration data including auto-created IntegrationInstanceConfiguration records.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
public class EmbeddedMcpServerBeforeDeleteEventListener extends AbstractRelationalEventListener<McpServer> {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final McpIntegrationService mcpIntegrationService;
    private final McpIntegrationWorkflowService mcpIntegrationWorkflowService;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerBeforeDeleteEventListener(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        McpIntegrationService mcpIntegrationService,
        McpIntegrationWorkflowService mcpIntegrationWorkflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationService = mcpIntegrationService;
        this.mcpIntegrationWorkflowService = mcpIntegrationWorkflowService;
    }

    @Override
    protected void onBeforeDelete(BeforeDeleteEvent<McpServer> beforeDeleteEvent) {
        Identifier identifier = beforeDeleteEvent.getId();

        deleteMcpIntegrations((Long) identifier.getValue());
    }

    private void deleteMcpIntegrations(long mcpServerId) {
        List<McpIntegration> mcpIntegrations = mcpIntegrationService.getMcpServerMcpIntegrations(mcpServerId);

        for (McpIntegration mcpIntegration : mcpIntegrations) {
            List<McpIntegrationWorkflow> mcpIntegrationWorkflows =
                mcpIntegrationWorkflowService.getMcpIntegrationMcpIntegrationWorkflows(mcpIntegration.getId());

            for (McpIntegrationWorkflow mcpIntegrationWorkflow : mcpIntegrationWorkflows) {
                mcpIntegrationWorkflowService.delete(mcpIntegrationWorkflow.getId());

                integrationInstanceConfigurationWorkflowService.delete(
                    mcpIntegrationWorkflow.getIntegrationInstanceConfigurationWorkflowId());
            }

            mcpIntegrationService.delete(mcpIntegration.getId());

            integrationInstanceConfigurationService.delete(
                mcpIntegration.getIntegrationInstanceConfigurationId());
        }
    }
}
