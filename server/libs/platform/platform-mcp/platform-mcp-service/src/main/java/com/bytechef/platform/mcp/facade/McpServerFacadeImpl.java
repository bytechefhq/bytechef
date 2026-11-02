/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.mcp.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.mcp.audit.McpComponentAuditEvents;
import com.bytechef.platform.mcp.audit.McpComponentAuditMapper;
import com.bytechef.platform.mcp.audit.McpServerAuditEvents;
import com.bytechef.platform.mcp.audit.McpServerAuditMapper;
import com.bytechef.platform.mcp.audit.McpToolAuditEvents;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class McpServerFacadeImpl implements McpServerFacade {

    private final McpComponentService mcpComponentService;
    private final McpServerService mcpServerService;
    private final TagService tagService;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public McpServerFacadeImpl(
        McpComponentService mcpComponentService, McpServerService mcpServerService, McpToolService mcpToolService,
        TagService tagService) {

        this.mcpComponentService = mcpComponentService;
        this.mcpServerService = mcpServerService;
        this.tagService = tagService;
        this.mcpToolService = mcpToolService;
    }

    @Override
    @Audited(event = McpComponentAuditEvents.MCP_COMPONENT_CREATED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpComponentId", value = "#result?.id"),
        @AuditData(key = "mcpServerId", value = "#mcpComponent.mcpServerId"),
        @AuditData(key = "componentName", value = "#mcpComponent.componentName"),
        @AuditData(key = "componentVersion", value = "#mcpComponent.componentVersion")
    })
    @PreAuthorize("hasPermission(#mcpComponent.mcpServerId, 'McpServer', 'MCP_EDIT')")
    public McpComponent create(McpComponent mcpComponent, List<McpTool> mcpTools) {
        McpComponent savedComponent = mcpComponentService.create(mcpComponent);

        if (mcpTools != null && !mcpTools.isEmpty()) {
            for (McpTool mcpTool : mcpTools) {
                McpTool toolToCreate = new McpTool(mcpTool.getName(), mcpTool.getParameters(), savedComponent.getId());

                mcpToolService.create(toolToCreate);
            }
        }

        return savedComponent;
    }

    @Override
    @Audited(event = McpToolAuditEvents.MCP_TOOL_CREATED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpToolId", value = "#result?.id"),
        @AuditData(key = "mcpComponentId", value = "#mcpTool.mcpComponentId"),
        @AuditData(key = "name", value = "#mcpTool.name")
    })
    @PreAuthorize("hasPermission(#mcpTool.mcpComponentId, 'McpComponent', 'MCP_EDIT')")
    public McpTool createMcpTool(McpTool mcpTool) {
        return mcpToolService.create(mcpTool);
    }

    @Override
    @Audited(event = McpComponentAuditEvents.MCP_COMPONENT_DELETED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpComponentId", value = "#mcpComponentId")
    })
    @PreAuthorize("hasPermission(#mcpComponentId, 'McpComponent', 'MCP_EDIT')")
    public void deleteMcpComponent(long mcpComponentId) {
        mcpComponentService.getMcpComponent(mcpComponentId);

        for (McpTool mcpTool : mcpToolService.getMcpComponentMcpTools(mcpComponentId)) {
            mcpToolService.delete(mcpTool);
        }

        mcpComponentService.delete(mcpComponentId);
    }

    @Override
    @Audited(event = McpServerAuditEvents.MCP_SERVER_DELETED, mapper = McpServerAuditMapper.class, data = {
        @AuditData(key = "mcpServerId", value = "#mcpServerId")
    })
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')")
    public void deleteMcpServer(long mcpServerId) {
        mcpServerService.getMcpServer(mcpServerId);

        for (McpComponent mcpComponent : mcpComponentService.getMcpServerMcpComponents(mcpServerId)) {
            mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())
                .forEach(mcpToolService::delete);

            mcpComponentService.delete(mcpComponent.getId());
        }

        mcpServerService.delete(mcpServerId);
    }

    @Override
    public Map<McpServer, List<McpComponent>> getMcpServerMcpComponents(List<McpServer> mcpServers) {
        List<Long> mcpServerIds = mcpServers.stream()
            .map(McpServer::getId)
            .toList();

        List<McpComponent> allMcpComponents = mcpServerIds.stream()
            .flatMap(serverId -> CollectionUtils.stream(mcpComponentService.getMcpServerMcpComponents(serverId)))
            .toList();

        return mcpServers.stream()
            .collect(
                Collectors.toMap(
                    mcpServer -> mcpServer,
                    mcpServer -> allMcpComponents.stream()
                        .filter(component -> Objects.equals(component.getMcpServerId(), mcpServer.getId()))
                        .toList()));
    }

    @Override
    public Map<McpServer, List<Tag>> getMcpServerTags(List<McpServer> mcpServers) {
        var tagIds = mcpServers.stream()
            .flatMap(mcpServer -> CollectionUtils.stream(mcpServer.getTagIds()))
            .toList();

        List<Tag> tags = tagService.getTags(tagIds);

        return mcpServers.stream()
            .collect(
                Collectors.toMap(
                    mcpServer -> mcpServer,
                    mcpServer -> tags.stream()
                        .filter(tag -> {
                            List<Long> curTagIds = mcpServer.getTagIds();

                            return curTagIds.contains(tag.getId());
                        })
                        .toList()));
    }

    @Override
    @Audited(event = McpComponentAuditEvents.MCP_COMPONENT_UPDATED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpComponentId", value = "#mcpComponent.id"),
        @AuditData(key = "mcpServerId", value = "#result?.mcpServerId"),
        @AuditData(key = "componentName", value = "#result?.componentName"),
        @AuditData(key = "componentVersion", value = "#result?.componentVersion")
    })
    @PreAuthorize("hasPermission(#mcpComponent.id, 'McpComponent', 'MCP_EDIT')")
    public McpComponent update(
        McpComponent mcpComponent, @Nullable Set<String> requiredAuthorities, List<McpTool> mcpTools) {

        McpComponent updatedComponent = mcpComponentService.update(mcpComponent, requiredAuthorities);

        List<McpTool> currentMcpTools = mcpToolService.getMcpComponentMcpTools(updatedComponent.getId());

        Map<String, Boolean> enabledByToolName = new HashMap<>();

        for (McpTool currentMcpTool : currentMcpTools) {
            enabledByToolName.put(currentMcpTool.getName(), currentMcpTool.isEnabled());

            mcpToolService.delete(currentMcpTool);
        }

        if (mcpTools != null && !mcpTools.isEmpty()) {
            for (McpTool mcpTool : mcpTools) {
                McpTool toolToCreate =
                    new McpTool(mcpTool.getName(), mcpTool.getParameters(), updatedComponent.getId());

                toolToCreate.setEnabled(enabledByToolName.getOrDefault(mcpTool.getName(), true));

                mcpToolService.create(toolToCreate);
            }
        }

        return updatedComponent;
    }

    @Override
    @Audited(event = McpToolAuditEvents.MCP_TOOL_DELETED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpToolId", value = "#mcpTool.id"),
        @AuditData(key = "mcpComponentId", value = "#mcpTool.mcpComponentId"),
        @AuditData(key = "name", value = "#mcpTool.name")
    })
    @PreAuthorize("hasPermission(#mcpTool.id, 'McpTool', 'MCP_EDIT')")
    public void deleteMcpTool(McpTool mcpTool) {
        mcpToolService.delete(mcpTool);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'McpServer', 'MCP_EDIT')")
    public List<Tag> updateMcpServerTags(long id, List<Tag> tags) {
        List<Tag> validatedTags = checkTags(tags);

        mcpServerService.updateTags(id, CollectionUtils.map(validatedTags, Tag::getId));

        return validatedTags;
    }

    @Override
    @Audited(event = McpToolAuditEvents.MCP_TOOL_UPDATED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpToolId", value = "#mcpTool.id"),
        @AuditData(key = "mcpComponentId", value = "#mcpTool.mcpComponentId"),
        @AuditData(key = "name", value = "#mcpTool.name")
    })
    @PreAuthorize("hasPermission(#mcpTool.id, 'McpTool', 'MCP_EDIT')")
    public McpTool updateMcpTool(McpTool mcpTool) {
        return mcpToolService.update(mcpTool);
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }
}
