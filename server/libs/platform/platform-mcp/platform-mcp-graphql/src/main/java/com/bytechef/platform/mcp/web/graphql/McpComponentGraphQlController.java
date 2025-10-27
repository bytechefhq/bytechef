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
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpToolService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing {@link McpComponent} entities.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class McpComponentGraphQlController {

    private final McpComponentService mcpComponentService;
    private final McpServerFacade mcpServerFacade;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public McpComponentGraphQlController(McpComponentService mcpComponentService, McpServerFacade mcpServerFacade,
        McpToolService mcpToolService) {
        this.mcpComponentService = mcpComponentService;
        this.mcpServerFacade = mcpServerFacade;
        this.mcpToolService = mcpToolService;
    }

    @QueryMapping
    public McpComponent mcpComponent(@Argument long id) {
        return mcpComponentService.getMcpComponent(id);
    }

    @QueryMapping
    public List<McpComponent> mcpComponents() {
        return mcpComponentService.getMcpComponents();
    }

    @QueryMapping
    public List<McpComponent> mcpComponentsByServerId(@Argument long mcpServerId) {
        return mcpComponentService.getMcpServerMcpComponents(mcpServerId);
    }

    @MutationMapping
    public McpComponent createMcpComponent(@Argument McpComponentInput input) {
        return mcpComponentService.create(
            new McpComponent(
                input.componentName(), input.componentVersion(), input.mcpServerId(), input.connectionId()));
    }

    @MutationMapping
    public McpComponent createMcpComponentWithTools(@Argument McpComponentWithToolsInput input) {
        McpComponent mcpComponent = new McpComponent(
            input.componentName(), input.componentVersion(), input.mcpServerId(), input.connectionId());

        List<McpTool> mcpTools = input.tools()
            .stream()
            .map(toolInput -> new McpTool(toolInput.name(), toolInput.parameters()))
            .toList();

        return mcpServerFacade.create(mcpComponent, mcpTools);
    }

    @MutationMapping
    public McpComponent updateMcpComponentWithTools(@Argument long id, @Argument McpComponentWithToolsInput input) {
        McpComponent mcpComponent = new McpComponent(
            input.componentName(), input.componentVersion(), input.mcpServerId(), input.connectionId(),
            input.version());
        mcpComponent.setId(id);

        List<McpTool> mcpTools = input.tools()
            .stream()
            .map(toolInput -> new McpTool(toolInput.name(), toolInput.parameters(), id))
            .toList();

        return mcpServerFacade.update(mcpComponent, mcpTools);
    }

    @MutationMapping
    public boolean deleteMcpComponent(@Argument long id) {
        mcpServerFacade.deleteMcpComponent(id);

        return true;
    }

    @SchemaMapping
    public Long connectionId(McpComponent mcpComponent) {
        return mcpComponent.getConnectionId();
    }

    @BatchMapping
    public Map<McpComponent, List<McpTool>> mcpTools(List<McpComponent> mcpComponents) {
        return mcpComponents.stream()
            .collect(Collectors.toMap(
                mcpComponent -> mcpComponent,
                mcpComponent -> mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())));
    }

    public record McpComponentInput(String componentName, int componentVersion, Long mcpServerId, Long connectionId) {
    }

    @SuppressFBWarnings("EI")
    public record McpComponentWithToolsInput(
        String componentName, int componentVersion, Long mcpServerId, Long connectionId,
        List<McpToolInputForComponent> tools, int version) {
    }

    @SuppressFBWarnings("EI")
    public record McpToolInputForComponent(String name, Map<String, String> parameters) {
    }
}
