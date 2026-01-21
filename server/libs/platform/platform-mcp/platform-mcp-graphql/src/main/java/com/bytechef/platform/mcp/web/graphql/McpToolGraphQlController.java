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
import com.bytechef.platform.mcp.service.McpToolService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing {@link McpTool} entities.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class McpToolGraphQlController {

    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public McpToolGraphQlController(McpToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    @QueryMapping
    public McpTool mcpTool(@Argument long id) {
        return mcpToolService.fetchMcpTool(id)
            .orElse(null);
    }

    @QueryMapping
    public List<McpTool> mcpTools() {
        return mcpToolService.getMcpTools();
    }

    @QueryMapping
    public List<McpTool> mcpToolsByComponentId(@Argument long mcpComponentId) {
        return mcpToolService.getMcpComponentMcpTools(mcpComponentId);
    }

    @MutationMapping
    public McpTool createMcpTool(@Argument McpToolInput input) {
        Map<String, String> parameters = input.parameters() != null ? input.parameters() : Map.of();

        return mcpToolService.create(new McpTool(input.name(), parameters, input.mcpComponentId()));
    }

    @SuppressFBWarnings("EI")
    public record McpToolInput(String name, Map<String, String> parameters, Long mcpComponentId) {
    }
}
