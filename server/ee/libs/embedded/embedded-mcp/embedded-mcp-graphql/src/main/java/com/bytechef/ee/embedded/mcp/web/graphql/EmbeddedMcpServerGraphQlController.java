/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.mcp.facade.EmbeddedMcpServerFacade;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing embedded MCP servers.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnCoordinator
class EmbeddedMcpServerGraphQlController {

    private final ComponentDefinitionService componentDefinitionService;
    private final EmbeddedMcpServerFacade embeddedMcpServerFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationService integrationService;
    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI")
    EmbeddedMcpServerGraphQlController(
        ComponentDefinitionService componentDefinitionService, EmbeddedMcpServerFacade embeddedMcpServerFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationService integrationService, McpServerService mcpServerService) {

        this.componentDefinitionService = componentDefinitionService;
        this.embeddedMcpServerFacade = embeddedMcpServerFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationService = integrationService;
        this.mcpServerService = mcpServerService;
    }

    @QueryMapping
    List<McpServer> embeddedMcpServers() {
        return mcpServerService.getMcpServers(PlatformType.EMBEDDED);
    }

    @QueryMapping
    List<ComponentDefinition> mcpComponentDefinitions() {
        Set<Long> configuredIntegrationIds =
            Set.copyOf(integrationInstanceConfigurationService.getIntegrationIds());

        List<String> componentNames = integrationService.getIntegrations(
            null, List.of(), null, Status.PUBLISHED)
            .stream()
            .filter(integration -> configuredIntegrationIds.contains(integration.getId()))
            .map(Integration::getComponentName)
            .distinct()
            .toList();

        if (componentNames.isEmpty()) {
            return List.of();
        }

        return componentDefinitionService.getComponentDefinitions(
            true, null, null, null, componentNames, PlatformType.EMBEDDED);
    }

    @MutationMapping
    McpServer createEmbeddedMcpServer(@Argument CreateEmbeddedMcpServerInput input) {
        Environment[] environments = Environment.values();

        int environmentIndex = (int) input.environmentId();

        if (environmentIndex < 0 || environmentIndex >= environments.length) {
            throw new IllegalArgumentException("Invalid environmentId: " + input.environmentId());
        }

        return mcpServerService.create(
            input.name(), PlatformType.EMBEDDED, environments[environmentIndex], input.enabled());
    }

    @MutationMapping
    boolean deleteEmbeddedMcpServer(@Argument Long mcpServerId) {
        embeddedMcpServerFacade.deleteEmbeddedMcpServer(mcpServerId);

        return true;
    }

    record CreateEmbeddedMcpServerInput(String name, long environmentId, Boolean enabled) {
    }
}
