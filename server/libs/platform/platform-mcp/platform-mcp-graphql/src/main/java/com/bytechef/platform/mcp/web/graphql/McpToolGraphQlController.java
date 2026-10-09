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

package com.bytechef.platform.mcp.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class McpToolGraphQlController {

    private final McpComponentService mcpComponentService;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public McpToolGraphQlController(
        McpComponentService mcpComponentService, McpServerService mcpServerService, McpToolService mcpToolService) {

        this.mcpComponentService = mcpComponentService;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
    }

    @QueryMapping
    public McpTool mcpTool(@Argument long id) {
        McpTool mcpTool = mcpToolService.fetchMcpTool(id)
            .orElse(null);

        if (mcpTool != null) {
            checkMcpComponentNotEmbedded(mcpTool.getMcpComponentId());
        }

        return mcpTool;
    }

    @QueryMapping
    public List<McpTool> mcpTools() {
        Set<Long> embeddedMcpServerIds = McpServerTypeUtils.getEmbeddedMcpServerIds(mcpServerService);

        Set<Long> nonEmbeddedMcpComponentIds = mcpComponentService.getMcpComponents()
            .stream()
            .filter(mcpComponent -> !embeddedMcpServerIds.contains(mcpComponent.getMcpServerId()))
            .map(McpComponent::getId)
            .collect(Collectors.toSet());

        return mcpToolService.getMcpTools()
            .stream()
            .filter(mcpTool -> nonEmbeddedMcpComponentIds.contains(mcpTool.getMcpComponentId()))
            .toList();
    }

    @QueryMapping
    @PreAuthorize("hasPermission(#mcpComponentId, 'McpComponent', 'MCP_VIEW')")
    public List<McpTool> mcpToolsByComponentId(@Argument long mcpComponentId) {
        checkMcpComponentNotEmbedded(mcpComponentId);

        return mcpToolService.getMcpComponentMcpTools(mcpComponentId);
    }

    @MutationMapping
    public McpTool createMcpTool(@Argument McpToolInput input) {
        checkMcpComponentNotEmbedded(input.mcpComponentId());

        Map<String, Object> parameters = input.parameters() != null ? input.parameters() : Map.of();

        return mcpToolService.create(new McpTool(input.name(), parameters, input.mcpComponentId()));
    }

    @MutationMapping
    public boolean deleteMcpTool(@Argument long id) {
        McpTool mcpTool = mcpToolService.fetchMcpTool(id)
            .orElseThrow(() -> new IllegalArgumentException("MCP tool not found: " + id));

        checkMcpComponentNotEmbedded(mcpTool.getMcpComponentId());

        mcpToolService.delete(mcpTool);

        return true;
    }

    @MutationMapping
    public McpTool updateMcpTool(@Argument long id, @Argument McpToolInput input) {
        McpTool currentMcpTool = mcpToolService.fetchMcpTool(id)
            .orElseThrow(() -> new IllegalArgumentException("MCP tool not found: " + id));

        checkMcpComponentNotEmbedded(currentMcpTool.getMcpComponentId());
        checkMcpComponentNotEmbedded(input.mcpComponentId());

        Map<String, Object> parameters = input.parameters() != null ? input.parameters() : Map.of();

        McpTool mcpTool = new McpTool(input.name(), parameters, input.mcpComponentId());

        mcpTool.setId(id);

        if (input.version() != null) {
            mcpTool.setVersion(input.version());
        }

        return mcpToolService.update(mcpTool);
    }

    private void checkMcpComponentNotEmbedded(long mcpComponentId) {
        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpComponentId);

        McpServer mcpServer = mcpServerService.getMcpServer(mcpComponent.getMcpServerId());

        McpServerTypeUtils.checkNotEmbedded(mcpServer.getType());
    }

    @SuppressFBWarnings("EI")
    public record McpToolInput(Long mcpComponentId, String name, Map<String, Object> parameters, Integer version) {
    }
}
