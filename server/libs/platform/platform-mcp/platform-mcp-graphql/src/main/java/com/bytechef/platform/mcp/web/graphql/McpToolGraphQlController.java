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
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class McpToolGraphQlController {

    private final McpServerFacade mcpServerFacade;

    @SuppressFBWarnings("EI")
    public McpToolGraphQlController(McpServerFacade mcpServerFacade) {
        this.mcpServerFacade = mcpServerFacade;
    }

    @QueryMapping
    public McpTool mcpTool(@Argument long id) {
        Optional<McpTool> mcpToolOptional = mcpServerFacade.fetchMcpTool(id);

        return mcpToolOptional.orElse(null);
    }

    @QueryMapping
    public List<McpTool> mcpTools() {
        return mcpServerFacade.getMcpTools();
    }

    @QueryMapping
    public List<McpTool> mcpToolsByComponentId(@Argument long mcpComponentId) {
        return mcpServerFacade.getMcpComponentMcpTools(mcpComponentId);
    }

    @MutationMapping
    public McpTool createMcpTool(@Argument McpToolInput input) {
        Map<String, Object> parameters = input.parameters() != null ? input.parameters() : Map.of();

        return mcpServerFacade.createMcpTool(new McpTool(input.name(), parameters, input.mcpComponentId()));
    }

    @MutationMapping
    public boolean deleteMcpTool(@Argument long id) {
        Optional<McpTool> mcpToolOptional = mcpServerFacade.fetchMcpTool(id);

        McpTool mcpTool = mcpToolOptional.orElseThrow(() -> new IllegalArgumentException("MCP tool not found: " + id));

        mcpServerFacade.deleteMcpTool(mcpTool);

        return true;
    }

    @MutationMapping
    public McpTool updateMcpTool(@Argument long id, @Argument McpToolInput input) {
        Map<String, Object> parameters = input.parameters() != null ? input.parameters() : Map.of();

        McpTool mcpTool = new McpTool(input.name(), parameters, input.mcpComponentId());

        mcpTool.setId(id);

        if (input.version() != null) {
            mcpTool.setVersion(input.version());
        }

        return mcpServerFacade.updateMcpTool(mcpTool);
    }

    @SuppressFBWarnings("EI")
    public record McpToolInput(Long mcpComponentId, String name, Map<String, Object> parameters, Integer version) {
    }
}
