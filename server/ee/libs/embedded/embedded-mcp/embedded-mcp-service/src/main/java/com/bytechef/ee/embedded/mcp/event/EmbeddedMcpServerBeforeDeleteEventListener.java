/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.event;

import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@ConditionalOnEEVersion
public class EmbeddedMcpServerBeforeDeleteEventListener extends AbstractRelationalEventListener<McpServer> {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerBeforeDeleteEventListener(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
    }

    @Override
    protected void onBeforeDelete(BeforeDeleteEvent<McpServer> beforeDeleteEvent) {
        Identifier identifier = beforeDeleteEvent.getId();

        deleteMcpIntegrationInstanceConfigurations((Long) identifier.getValue());
    }

    private void deleteMcpIntegrationInstanceConfigurations(long mcpServerId) {
        List<McpIntegrationInstanceConfiguration> mcpIntegrationInstanceConfigurations =
            mcpIntegrationInstanceConfigurationService.getMcpServerMcpIntegrationInstanceConfigurations(mcpServerId);

        for (McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration : mcpIntegrationInstanceConfigurations) {
            List<McpIntegrationInstanceConfigurationWorkflow> mcpIntegrationInstanceConfigurationWorkflows =
                mcpIntegrationInstanceConfigurationWorkflowService
                    .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                        mcpIntegrationInstanceConfiguration.getId());

            for (McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow : mcpIntegrationInstanceConfigurationWorkflows) {
                integrationInstanceWorkflowService.deleteByIntegrationInstanceConfigurationWorkflowId(
                    mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

                mcpIntegrationInstanceConfigurationWorkflowService
                    .delete(mcpIntegrationInstanceConfigurationWorkflow.getId());

                integrationInstanceConfigurationWorkflowService.delete(
                    mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());
            }

            mcpIntegrationInstanceConfigurationService.delete(mcpIntegrationInstanceConfiguration.getId());

            integrationInstanceConfigurationService.delete(
                mcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId());
        }
    }
}
