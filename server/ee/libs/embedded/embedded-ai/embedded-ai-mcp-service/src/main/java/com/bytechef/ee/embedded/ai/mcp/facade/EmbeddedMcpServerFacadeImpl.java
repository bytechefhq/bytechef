/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
class EmbeddedMcpServerFacadeImpl implements EmbeddedMcpServerFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationService integrationService;
    private final McpComponentService mcpComponentService;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpServerFacade mcpServerFacade;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerFacadeImpl(
        ComponentDefinitionService componentDefinitionService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationService integrationService, McpComponentService mcpComponentService,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService, McpServerFacade mcpServerFacade,
        McpServerService mcpServerService, McpToolService mcpToolService, TagService tagService) {

        this.componentDefinitionService = componentDefinitionService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationService = integrationService;
        this.mcpComponentService = mcpComponentService;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpServerFacade = mcpServerFacade;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
        this.tagService = tagService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpServer createEmbeddedMcpServer(String name, Environment environment, boolean enabled) {
        return mcpServerService.create(name, PlatformType.EMBEDDED, environment, enabled);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void deleteEmbeddedMcpServer(long mcpServerId) {
        for (var mcpComponent : mcpComponentService.getMcpServerMcpComponents(mcpServerId)) {
            for (var mcpTool : mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())) {
                mcpIntegrationInstanceToolService.deleteByMcpToolId(mcpTool.getId());
            }
        }

        mcpServerFacade.deleteMcpServer(mcpServerId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<McpServer> getEmbeddedMcpServers() {
        return mcpServerService.getMcpServers(PlatformType.EMBEDDED);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<Tag> getEmbeddedMcpServerTags() {
        List<Long> tagIds = mcpServerService.getMcpServers(PlatformType.EMBEDDED)
            .stream()
            .flatMap(mcpServer -> CollectionUtils.stream(mcpServer.getTagIds()))
            .distinct()
            .toList();

        if (tagIds.isEmpty()) {
            return List.of();
        }

        return tagService.getTags(tagIds);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<ComponentDefinition> getMcpComponentDefinitions() {
        Set<Long> configuredIntegrationIds = Set.copyOf(integrationInstanceConfigurationService.getIntegrationIds());

        List<String> componentNames = integrationService.getIntegrations(null, List.of(), null, Status.PUBLISHED)
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
}
