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

package com.bytechef.platform.mcp.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.mcp.audit.McpComponentAuditEvents;
import com.bytechef.platform.mcp.audit.McpComponentAuditMapper;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link McpComponentService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpComponentServiceImpl implements McpComponentService {

    private final McpComponentRepository mcpComponentRepository;
    private final McpToolRepository mcpToolRepository;

    public McpComponentServiceImpl(
        McpComponentRepository mcpComponentRepository, McpToolRepository mcpToolRepository) {

        this.mcpComponentRepository = mcpComponentRepository;
        this.mcpToolRepository = mcpToolRepository;
    }

    @Override
    @Audited(event = McpComponentAuditEvents.MCP_COMPONENT_CREATED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpComponentId", value = "#result?.id"),
        @AuditData(key = "mcpServerId", value = "#mcpComponent.mcpServerId"),
        @AuditData(key = "componentName", value = "#mcpComponent.componentName"),
        @AuditData(key = "componentVersion", value = "#mcpComponent.componentVersion")
    })
    @PreAuthorize("hasPermission(#mcpComponent.mcpServerId, 'McpServer', 'MCP_EDIT')")
    public McpComponent create(McpComponent mcpComponent) {
        return mcpComponentRepository.save(mcpComponent);
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
        McpComponent savedMcpComponent = mcpComponentRepository.save(mcpComponent);

        for (McpTool mcpTool : mcpTools) {
            mcpToolRepository.save(new McpTool(mcpTool.getName(), mcpTool.getParameters(), savedMcpComponent.getId()));
        }

        return savedMcpComponent;
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

        McpComponent currentMcpComponent = OptionalUtils.get(mcpComponentRepository.findById(mcpComponent.getId()));

        currentMcpComponent.setConnectionId(mcpComponent.getConnectionId());
        currentMcpComponent.setVersion(mcpComponent.getVersion());

        if (requiredAuthorities != null) {
            currentMcpComponent.setRequiredAuthorities(requiredAuthorities);
        }

        McpComponent updatedMcpComponent = mcpComponentRepository.save(currentMcpComponent);

        Map<String, Boolean> enabledByToolName = new HashMap<>();

        for (McpTool currentMcpTool : mcpToolRepository.findAllByMcpComponentId(updatedMcpComponent.getId())) {
            enabledByToolName.put(currentMcpTool.getName(), currentMcpTool.isEnabled());

            mcpToolRepository.delete(currentMcpTool);
        }

        for (McpTool mcpTool : mcpTools) {
            McpTool newMcpTool = new McpTool(mcpTool.getName(), mcpTool.getParameters(), updatedMcpComponent.getId());

            newMcpTool.setEnabled(enabledByToolName.getOrDefault(mcpTool.getName(), true));

            mcpToolRepository.save(newMcpTool);
        }

        return updatedMcpComponent;
    }

    @Override
    @Audited(event = McpComponentAuditEvents.MCP_COMPONENT_DELETED, mapper = McpComponentAuditMapper.class, data = {
        @AuditData(key = "mcpComponentId", value = "#mcpComponentId")
    })
    public void delete(long mcpComponentId) {
        McpComponent mcpComponent = OptionalUtils.get(mcpComponentRepository.findById(mcpComponentId));

        mcpToolRepository.deleteAll(mcpToolRepository.findAllByMcpComponentId(mcpComponent.getId()));

        mcpComponentRepository.delete(mcpComponent);
    }

    @Override
    public McpComponent getMcpComponent(long mcpComponentId) {
        return OptionalUtils.get(mcpComponentRepository.findById(mcpComponentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpComponent> getMcpComponentsByComponentName(String componentName) {
        return mcpComponentRepository.findAllByComponentName(componentName);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<McpComponent> getMcpComponents() {
        return mcpComponentRepository.findAll();
    }

    @Override
    public List<McpComponent> getMcpServerMcpComponents(long mcpServerId) {
        return mcpComponentRepository.findAllByMcpServerId(mcpServerId);
    }
}
