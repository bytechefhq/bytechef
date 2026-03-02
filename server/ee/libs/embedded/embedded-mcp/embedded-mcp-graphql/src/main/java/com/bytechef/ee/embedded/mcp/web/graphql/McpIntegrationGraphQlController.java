/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegration;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationWorkflow;
import com.bytechef.ee.embedded.mcp.facade.McpIntegrationFacade;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing {@link McpIntegration} entities.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnCoordinator
class McpIntegrationGraphQlController {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationService integrationService;
    private final McpIntegrationFacade mcpIntegrationFacade;
    private final McpIntegrationService mcpIntegrationService;
    private final McpIntegrationWorkflowService mcpIntegrationWorkflowService;

    @SuppressFBWarnings("EI")
    McpIntegrationGraphQlController(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationService integrationService, McpIntegrationFacade mcpIntegrationFacade,
        McpIntegrationService mcpIntegrationService,
        McpIntegrationWorkflowService mcpIntegrationWorkflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationService = integrationService;
        this.mcpIntegrationFacade = mcpIntegrationFacade;
        this.mcpIntegrationService = mcpIntegrationService;
        this.mcpIntegrationWorkflowService = mcpIntegrationWorkflowService;
    }

    @MutationMapping
    McpIntegration createMcpIntegration(@Argument CreateMcpIntegrationInput input) {
        return mcpIntegrationFacade.createMcpIntegration(
            input.mcpServerId(), input.integrationId(), input.integrationVersion(), input.selectedWorkflowIds());
    }

    @MutationMapping
    boolean deleteMcpIntegration(@Argument long id) {
        mcpIntegrationFacade.deleteMcpIntegration(id);

        return true;
    }

    @QueryMapping
    McpIntegration mcpIntegration(@Argument long id) {
        return mcpIntegrationService.fetchMcpIntegration(id)
            .orElse(null);
    }

    @QueryMapping
    List<McpIntegration> mcpIntegrations() {
        return mcpIntegrationService.getMcpIntegrations();
    }

    @QueryMapping
    List<McpIntegration> mcpIntegrationsByServerId(@Argument long mcpServerId) {
        return mcpIntegrationService.getMcpServerMcpIntegrations(mcpServerId);
    }

    @SchemaMapping
    Integration integration(McpIntegration mcpIntegration) {
        return integrationService.getIntegrationInstanceConfigurationIntegration(
            mcpIntegration.getIntegrationInstanceConfigurationId());
    }

    @SchemaMapping
    Integer integrationVersion(McpIntegration mcpIntegration) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                mcpIntegration.getIntegrationInstanceConfigurationId());

        return integrationInstanceConfiguration.getIntegrationVersion();
    }

    @SchemaMapping
    List<McpIntegrationWorkflow> mcpIntegrationWorkflows(McpIntegration mcpIntegration) {
        return mcpIntegrationWorkflowService.getMcpIntegrationMcpIntegrationWorkflows(mcpIntegration.getId());
    }

    @SuppressFBWarnings("EI")
    record CreateMcpIntegrationInput(
        long mcpServerId, long integrationId, int integrationVersion, List<String> selectedWorkflowIds) {
    }
}
