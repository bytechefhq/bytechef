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
import com.bytechef.platform.constant.PlatformType;
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
import com.bytechef.platform.mcp.web.McpServerTypeDefinition;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class McpServerFacadeImpl implements McpServerFacade {

    private final McpComponentService mcpComponentService;
    private final McpServerService mcpServerService;
    private final ObjectProvider<McpServerTypeDefinition> mcpServerTypeDefinitionsProvider;
    private final TagService tagService;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public McpServerFacadeImpl(
        McpComponentService mcpComponentService, McpServerService mcpServerService,
        ObjectProvider<McpServerTypeDefinition> mcpServerTypeDefinitionsProvider, McpToolService mcpToolService,
        TagService tagService) {

        this.mcpComponentService = mcpComponentService;
        this.mcpServerService = mcpServerService;
        this.mcpServerTypeDefinitionsProvider = mcpServerTypeDefinitionsProvider;
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
        return doCreate(mcpComponent, mcpTools);
    }

    @Override
    @Audited(event = McpComponentAuditEvents.MCP_COMPONENT_CREATED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpComponentId", value = "#result?.id"),
        @AuditData(key = "mcpServerId", value = "#mcpComponent.mcpServerId"),
        @AuditData(key = "componentName", value = "#mcpComponent.componentName"),
        @AuditData(key = "componentVersion", value = "#mcpComponent.componentVersion")
    })
    @PreAuthorize("hasPermission(#mcpComponent.mcpServerId, 'McpServer', 'MCP_EDIT')")
    public McpComponent createSharedMcpComponent(McpComponent mcpComponent, List<McpTool> mcpTools) {
        checkMcpServerSharedAdministration(mcpComponent.getMcpServerId());

        return doCreate(mcpComponent, mcpTools);
    }

    @Override
    @Audited(event = McpToolAuditEvents.MCP_TOOL_CREATED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpToolId", value = "#result?.id"),
        @AuditData(key = "mcpComponentId", value = "#mcpTool.mcpComponentId"),
        @AuditData(key = "name", value = "#mcpTool.name")
    })
    @PreAuthorize("hasPermission(#mcpTool.mcpComponentId, 'McpComponent', 'MCP_EDIT')")
    public McpTool createMcpTool(McpTool mcpTool) {
        checkMcpComponentSharedAdministration(mcpTool.getMcpComponentId());

        return mcpToolService.create(mcpTool);
    }

    @Override
    @Audited(event = McpComponentAuditEvents.MCP_COMPONENT_CREATED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpComponentId", value = "#result?.id"),
        @AuditData(key = "mcpServerId", value = "#mcpComponent.mcpServerId"),
        @AuditData(key = "componentName", value = "#mcpComponent.componentName"),
        @AuditData(key = "componentVersion", value = "#mcpComponent.componentVersion")
    })
    @PreAuthorize("hasPermission(#mcpComponent.mcpServerId, 'McpServer', 'MCP_EDIT')")
    public McpComponent createMcpComponent(McpComponent mcpComponent) {
        checkMcpServerSharedAdministration(mcpComponent.getMcpServerId());

        return mcpComponentService.create(mcpComponent);
    }

    @Override
    @Audited(event = McpComponentAuditEvents.MCP_COMPONENT_DELETED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpComponentId", value = "#mcpComponentId")
    })
    @PreAuthorize("hasPermission(#mcpComponentId, 'McpComponent', 'MCP_EDIT')")
    public void deleteMcpComponent(long mcpComponentId) {
        doDeleteMcpComponent(mcpComponentId);
    }

    @Override
    @Audited(event = McpComponentAuditEvents.MCP_COMPONENT_DELETED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpComponentId", value = "#mcpComponentId")
    })
    @PreAuthorize("hasPermission(#mcpComponentId, 'McpComponent', 'MCP_EDIT')")
    public void deleteSharedMcpComponent(long mcpComponentId) {
        checkMcpComponentSharedAdministration(mcpComponentId);

        doDeleteMcpComponent(mcpComponentId);
    }

    @Override
    @Audited(event = McpServerAuditEvents.MCP_SERVER_DELETED, mapper = McpServerAuditMapper.class, data = {
        @AuditData(key = "mcpServerId", value = "#mcpServerId")
    })
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')")
    public void deleteMcpServer(long mcpServerId) {
        doDeleteMcpServer(mcpServerId);
    }

    @Override
    @Audited(event = McpServerAuditEvents.MCP_SERVER_DELETED, mapper = McpServerAuditMapper.class, data = {
        @AuditData(key = "mcpServerId", value = "#mcpServerId")
    })
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')")
    public void deleteSharedMcpServer(long mcpServerId) {
        checkMcpServerSharedAdministration(mcpServerId);

        doDeleteMcpServer(mcpServerId);
    }

    @Override
    @PreAuthorize("hasPermission(#mcpComponentId, 'McpComponent', 'MCP_VIEW')")
    public McpComponent getMcpComponent(long mcpComponentId) {
        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpComponentId);

        checkMcpServerSharedAdministration(mcpComponent.getMcpServerId());

        return mcpComponent;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpComponentId, 'McpComponent', 'MCP_VIEW')")
    public List<McpTool> getMcpComponentMcpTools(long mcpComponentId) {
        checkMcpComponentSharedAdministration(mcpComponentId);

        return mcpToolService.getMcpComponentMcpTools(mcpComponentId);
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_VIEW')")
    public List<McpComponent> getMcpServerMcpComponents(long mcpServerId) {
        checkMcpServerSharedAdministration(mcpServerId);

        return mcpComponentService.getMcpServerMcpComponents(mcpServerId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<McpComponent> getMcpComponents() {
        Set<Long> sharedAdministrationMcpServerIds = getSharedAdministrationMcpServerIds();

        return mcpComponentService.getMcpComponents()
            .stream()
            .filter(mcpComponent -> sharedAdministrationMcpServerIds.contains(mcpComponent.getMcpServerId()))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_VIEW')")
    public McpServer getMcpServer(long mcpServerId) {
        McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

        checkSharedAdministration(mcpServer.getType());

        return mcpServer;
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
    @PreAuthorize("isTenantAdmin()")
    public List<McpTool> getMcpTools() {
        Set<Long> sharedAdministrationMcpComponentIds = getMcpComponents().stream()
            .map(McpComponent::getId)
            .collect(Collectors.toSet());

        return mcpToolService.getMcpTools()
            .stream()
            .filter(mcpTool -> sharedAdministrationMcpComponentIds.contains(mcpTool.getMcpComponentId()))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#mcpToolId, 'McpTool', 'MCP_VIEW')")
    public Optional<McpTool> fetchMcpTool(long mcpToolId) {
        Optional<McpTool> mcpToolOptional = mcpToolService.fetchMcpTool(mcpToolId);

        mcpToolOptional.ifPresent(mcpTool -> checkMcpComponentSharedAdministration(mcpTool.getMcpComponentId()));

        return mcpToolOptional;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')")
    public McpServer rotateMcpServerSecretKey(long mcpServerId) {
        checkMcpServerSharedAdministration(mcpServerId);

        return mcpServerService.rotateSecretKey(mcpServerId);
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
        return doUpdate(mcpComponent, requiredAuthorities, mcpTools);
    }

    @Override
    @Audited(event = McpComponentAuditEvents.MCP_COMPONENT_UPDATED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpComponentId", value = "#mcpComponent.id"),
        @AuditData(key = "mcpServerId", value = "#result?.mcpServerId"),
        @AuditData(key = "componentName", value = "#result?.componentName"),
        @AuditData(key = "componentVersion", value = "#result?.componentVersion")
    })
    @PreAuthorize("hasPermission(#mcpComponent.id, 'McpComponent', 'MCP_EDIT')")
    public McpComponent updateSharedMcpComponent(
        McpComponent mcpComponent, @Nullable Set<String> requiredAuthorities, List<McpTool> mcpTools) {
        checkMcpComponentSharedAdministration(Objects.requireNonNull(mcpComponent.getId()));

        return doUpdate(mcpComponent, requiredAuthorities, mcpTools);
    }

    @Override
    @Audited(event = McpToolAuditEvents.MCP_TOOL_DELETED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpToolId", value = "#mcpTool.id"),
        @AuditData(key = "mcpComponentId", value = "#mcpTool.mcpComponentId"),
        @AuditData(key = "name", value = "#mcpTool.name")
    })
    @PreAuthorize("hasPermission(#mcpTool.id, 'McpTool', 'MCP_EDIT')")
    public void deleteMcpTool(McpTool mcpTool) {
        doDeleteMcpTool(mcpTool);
    }

    @Override
    @Audited(event = McpToolAuditEvents.MCP_TOOL_DELETED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpToolId", value = "#mcpTool.id"),
        @AuditData(key = "mcpComponentId", value = "#mcpTool.mcpComponentId"),
        @AuditData(key = "name", value = "#mcpTool.name")
    })
    @PreAuthorize("hasPermission(#mcpTool.id, 'McpTool', 'MCP_EDIT')")
    public void deleteSharedMcpTool(McpTool mcpTool) {
        checkMcpComponentSharedAdministration(mcpTool.getMcpComponentId());

        doDeleteMcpTool(mcpTool);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'McpServer', 'MCP_EDIT')")
    public McpServer updateMcpServer(
        long id, @Nullable String name, @Nullable Boolean enabled, @Nullable Boolean enforceToolAuthorization,
        @Nullable Boolean authenticationRequired) {

        checkMcpServerSharedAdministration(id);

        return mcpServerService.update(id, name, enabled, enforceToolAuthorization, authenticationRequired);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'McpServer', 'MCP_EDIT')")
    public List<Tag> updateMcpServerTags(long id, List<Tag> tags) {
        return doUpdateMcpServerTags(id, tags);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'McpServer', 'MCP_EDIT')")
    public List<Tag> updateSharedMcpServerTags(long id, List<Tag> tags) {
        checkMcpServerSharedAdministration(id);

        return doUpdateMcpServerTags(id, tags);
    }

    @Override
    @Audited(event = McpToolAuditEvents.MCP_TOOL_UPDATED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpToolId", value = "#mcpTool.id"),
        @AuditData(key = "mcpComponentId", value = "#mcpTool.mcpComponentId"),
        @AuditData(key = "name", value = "#mcpTool.name")
    })
    @PreAuthorize("hasPermission(#mcpTool.id, 'McpTool', 'MCP_EDIT')")
    public McpTool updateMcpTool(McpTool mcpTool) {
        return doUpdateMcpTool(mcpTool);
    }

    @Override
    @Audited(event = McpToolAuditEvents.MCP_TOOL_UPDATED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpToolId", value = "#mcpTool.id"),
        @AuditData(key = "mcpComponentId", value = "#mcpTool.mcpComponentId"),
        @AuditData(key = "name", value = "#mcpTool.name")
    })
    @PreAuthorize("hasPermission(#mcpTool.id, 'McpTool', 'MCP_EDIT')")
    public McpTool updateSharedMcpTool(McpTool mcpTool) {
        long mcpToolId = Objects.requireNonNull(mcpTool.getId());

        Optional<McpTool> currentMcpToolOptional = mcpToolService.fetchMcpTool(mcpToolId);

        McpTool currentMcpTool = currentMcpToolOptional.orElseThrow(
            () -> new IllegalArgumentException("MCP tool not found: " + mcpToolId));

        checkMcpComponentSharedAdministration(currentMcpTool.getMcpComponentId());
        checkMcpComponentSharedAdministration(mcpTool.getMcpComponentId());

        return doUpdateMcpTool(mcpTool);
    }

    private McpComponent doCreate(McpComponent mcpComponent, List<McpTool> mcpTools) {
        McpComponent savedComponent = mcpComponentService.create(mcpComponent);

        if (mcpTools != null && !mcpTools.isEmpty()) {
            for (McpTool mcpTool : mcpTools) {
                McpTool toolToCreate = new McpTool(mcpTool.getName(), mcpTool.getParameters(), savedComponent.getId());

                mcpToolService.create(toolToCreate);
            }
        }

        return savedComponent;
    }

    private void doDeleteMcpComponent(long mcpComponentId) {
        mcpComponentService.getMcpComponent(mcpComponentId);

        for (McpTool mcpTool : mcpToolService.getMcpComponentMcpTools(mcpComponentId)) {
            mcpToolService.delete(mcpTool);
        }

        mcpComponentService.delete(mcpComponentId);
    }

    private void doDeleteMcpServer(long mcpServerId) {
        mcpServerService.getMcpServer(mcpServerId);

        for (McpComponent mcpComponent : mcpComponentService.getMcpServerMcpComponents(mcpServerId)) {
            mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())
                .forEach(mcpToolService::delete);

            mcpComponentService.delete(mcpComponent.getId());
        }

        mcpServerService.delete(mcpServerId);
    }

    private void doDeleteMcpTool(McpTool mcpTool) {
        mcpToolService.delete(mcpTool);
    }

    private McpComponent doUpdate(
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

    private List<Tag> doUpdateMcpServerTags(long id, List<Tag> tags) {
        List<Tag> validatedTags = checkTags(tags);

        mcpServerService.updateTags(id, CollectionUtils.map(validatedTags, Tag::getId));

        return validatedTags;
    }

    private McpTool doUpdateMcpTool(McpTool mcpTool) {
        return mcpToolService.update(mcpTool);
    }

    private void checkMcpComponentSharedAdministration(long mcpComponentId) {
        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpComponentId);

        checkMcpServerSharedAdministration(mcpComponent.getMcpServerId());
    }

    private void checkMcpServerSharedAdministration(long mcpServerId) {
        McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

        checkSharedAdministration(mcpServer.getType());
    }

    private void checkSharedAdministration(PlatformType type) {
        boolean sharedAdministration = mcpServerTypeDefinitionsProvider.stream()
            .anyMatch(mcpServerTypeDefinition -> mcpServerTypeDefinition.getType() == type &&
                mcpServerTypeDefinition.isSharedAdministration());

        if (!sharedAdministration) {
            throw new AccessDeniedException(
                "MCP servers of type " + type + " can only be administered through their own MCP server operations");
        }
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    private Set<Long> getSharedAdministrationMcpServerIds() {
        return mcpServerTypeDefinitionsProvider.stream()
            .filter(McpServerTypeDefinition::isSharedAdministration)
            .flatMap(mcpServerTypeDefinition -> CollectionUtils.stream(
                mcpServerService.getMcpServers(mcpServerTypeDefinition.getType())))
            .map(McpServer::getId)
            .collect(Collectors.toSet());
    }
}
