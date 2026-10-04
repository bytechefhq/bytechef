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
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
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
    public McpComponent createEmbeddedMcpComponent(McpComponent mcpComponent, List<McpTool> mcpTools) {
        getEmbeddedMcpServer(mcpComponent.getMcpServerId());

        return mcpServerFacade.create(mcpComponent, mcpTools);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpServer createEmbeddedMcpServer(String name, Environment environment, Boolean enabled) {
        return mcpServerService.create(name, PlatformType.EMBEDDED, environment, enabled);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void deleteEmbeddedMcpComponent(long mcpComponentId) {
        checkEmbeddedMcpComponent(mcpComponentId);

        mcpServerFacade.deleteMcpComponent(mcpComponentId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void deleteEmbeddedMcpServer(long mcpServerId) {
        getEmbeddedMcpServer(mcpServerId);

        for (var mcpComponent : mcpComponentService.getMcpServerMcpComponents(mcpServerId)) {
            for (var mcpTool : mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())) {
                mcpIntegrationInstanceToolService.deleteByMcpToolId(mcpTool.getId());
            }
        }

        mcpServerFacade.deleteMcpServer(mcpServerId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void deleteEmbeddedMcpTool(long mcpToolId) {
        McpTool mcpTool = getEmbeddedMcpTool(mcpToolId);

        mcpToolService.delete(mcpTool);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<McpTool> getEmbeddedMcpComponentMcpTools(long mcpComponentId) {
        checkEmbeddedMcpComponent(mcpComponentId);

        return mcpToolService.getMcpComponentMcpTools(mcpComponentId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<McpComponent> getEmbeddedMcpServerMcpComponents(long mcpServerId) {
        getEmbeddedMcpServer(mcpServerId);

        return mcpComponentService.getMcpServerMcpComponents(mcpServerId);
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

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpComponent updateEmbeddedMcpComponent(McpComponent mcpComponent, List<McpTool> mcpTools) {
        checkEmbeddedMcpComponent(mcpComponent.getId());
        getEmbeddedMcpServer(mcpComponent.getMcpServerId());

        return mcpServerFacade.update(mcpComponent, mcpTools);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpServer updateEmbeddedMcpServer(
        long mcpServerId, String name, Boolean enabled, Boolean enforceToolAuthorization,
        Boolean authenticationRequired) {

        getEmbeddedMcpServer(mcpServerId);

        McpServer mcpServer = mcpServerService.update(mcpServerId, name, enabled);

        if (enforceToolAuthorization != null || authenticationRequired != null) {
            if (enforceToolAuthorization != null) {
                mcpServer.setEnforceToolAuthorization(enforceToolAuthorization);
            }

            if (authenticationRequired != null) {
                mcpServer.setAuthenticationRequired(authenticationRequired);
            }

            mcpServer = mcpServerService.update(mcpServer);
        }

        return mcpServer;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpServer updateEmbeddedMcpServerSecretKey(long mcpServerId) {
        getEmbeddedMcpServer(mcpServerId);

        return mcpServerService.rotateSecretKey(mcpServerId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<Tag> updateEmbeddedMcpServerTags(long mcpServerId, List<Tag> tags) {
        getEmbeddedMcpServer(mcpServerId);

        return mcpServerFacade.updateMcpServerTags(mcpServerId, tags);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpTool updateEmbeddedMcpTool(McpTool mcpTool) {
        getEmbeddedMcpTool(mcpTool.getId());
        checkEmbeddedMcpComponent(mcpTool.getMcpComponentId());

        return mcpToolService.update(mcpTool);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpTool updateEmbeddedMcpToolEnabled(long mcpToolId, boolean enabled) {
        getEmbeddedMcpTool(mcpToolId);

        mcpToolService.updateEnabled(mcpToolId, enabled);

        return getEmbeddedMcpTool(mcpToolId);
    }

    private void checkEmbeddedMcpComponent(long mcpComponentId) {
        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpComponentId);

        getEmbeddedMcpServer(mcpComponent.getMcpServerId());
    }

    private McpServer getEmbeddedMcpServer(long mcpServerId) {
        McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

        if (mcpServer.getType() != PlatformType.EMBEDDED) {
            throw new IllegalArgumentException("MCP server %s is not an embedded MCP server".formatted(mcpServerId));
        }

        return mcpServer;
    }

    private McpTool getEmbeddedMcpTool(long mcpToolId) {
        McpTool mcpTool = mcpToolService.fetchMcpTool(mcpToolId)
            .orElseThrow(() -> new IllegalArgumentException("MCP tool not found: " + mcpToolId));

        checkEmbeddedMcpComponent(mcpTool.getMcpComponentId());

        return mcpTool;
    }
}
