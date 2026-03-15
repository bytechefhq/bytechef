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
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.mcp.facade.McpIntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing {@link McpIntegrationInstanceConfiguration} entities.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnCoordinator
class McpIntegrationInstanceConfigurationGraphQlController {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationService integrationService;
    private final McpIntegrationInstanceConfigurationFacade mcpIntegrationInstanceConfigurationFacade;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @SuppressFBWarnings("EI")
    McpIntegrationInstanceConfigurationGraphQlController(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationService integrationService,
        McpIntegrationInstanceConfigurationFacade mcpIntegrationInstanceConfigurationFacade,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationService = integrationService;
        this.mcpIntegrationInstanceConfigurationFacade = mcpIntegrationInstanceConfigurationFacade;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
    }

    @MutationMapping
    McpIntegrationInstanceConfiguration
        createMcpIntegrationInstanceConfiguration(@Argument CreateMcpIntegrationInstanceConfigurationInput input) {
        return mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
            input.mcpServerId(), input.integrationInstanceConfigurationId(), input.selectedWorkflowIds());
    }

    @MutationMapping
    boolean deleteMcpIntegrationInstanceConfiguration(@Argument long id) {
        mcpIntegrationInstanceConfigurationFacade.deleteMcpIntegrationInstanceConfiguration(id);

        return true;
    }

    @MutationMapping
    McpIntegrationInstanceConfiguration updateMcpIntegrationInstanceConfiguration(
        @Argument long id, @Argument UpdateMcpIntegrationInstanceConfigurationInput input) {
        return mcpIntegrationInstanceConfigurationFacade.updateMcpIntegrationInstanceConfiguration(id,
            input.selectedWorkflowIds());
    }

    @MutationMapping
    boolean updateMcpIntegrationInstanceConfigurationVersion(
        @Argument long id, @Argument UpdateMcpIntegrationInstanceConfigurationVersionInput input) {
        mcpIntegrationInstanceConfigurationFacade.updateMcpIntegrationInstanceConfigurationVersion(id,
            input.integrationVersion(), input.workflowUuids());

        return true;
    }

    @QueryMapping
    McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration(@Argument long id) {
        return mcpIntegrationInstanceConfigurationService.fetchMcpIntegrationInstanceConfiguration(id)
            .orElse(null);
    }

    @QueryMapping
    List<McpIntegrationInstanceConfiguration> mcpIntegrationInstanceConfigurations() {
        return mcpIntegrationInstanceConfigurationService.getMcpIntegrationInstanceConfigurations();
    }

    @QueryMapping
    List<McpIntegrationInstanceConfiguration>
        mcpIntegrationInstanceConfigurationsByServerId(@Argument long mcpServerId) {
        return mcpIntegrationInstanceConfigurationService.getMcpServerMcpIntegrationInstanceConfigurations(mcpServerId);
    }

    @SchemaMapping
    String integrationInstanceConfigurationName(
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                mcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId());

        return integrationInstanceConfiguration.getName();
    }

    @SchemaMapping
    Integration integration(McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration) {
        return integrationService.getIntegrationInstanceConfigurationIntegration(
            mcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId());
    }

    @SchemaMapping
    Integer integrationVersion(McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                mcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId());

        return integrationInstanceConfiguration.getIntegrationVersion();
    }

    @SchemaMapping
    List<McpIntegrationInstanceConfigurationWorkflow> mcpIntegrationInstanceConfigurationWorkflows(
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration) {
        return mcpIntegrationInstanceConfigurationWorkflowService
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                mcpIntegrationInstanceConfiguration.getId());
    }

    @SuppressFBWarnings("EI")
    record CreateMcpIntegrationInstanceConfigurationInput(
        long mcpServerId, long integrationInstanceConfigurationId, List<String> selectedWorkflowIds) {
    }

    @SuppressFBWarnings("EI")
    record UpdateMcpIntegrationInstanceConfigurationInput(List<String> selectedWorkflowIds) {
    }

    @SuppressFBWarnings("EI")
    record UpdateMcpIntegrationInstanceConfigurationVersionInput(int integrationVersion, List<String> workflowUuids) {
    }
}
