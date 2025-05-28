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

package com.bytechef.platform.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.configuration.domain.McpServer;
import com.bytechef.platform.configuration.service.McpServerService;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.ModeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing {@link McpServer} entities.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class McpServerGraphQlController {

    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI")
    McpServerGraphQlController(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @QueryMapping
    McpServer mcpServer(@Argument long id) {
        return mcpServerService.fetchMcpServer(id)
            .orElse(null);
    }

    @QueryMapping
    List<McpServer> mcpServers() {
        return mcpServerService.getMcpServers();
    }

    @MutationMapping
    McpServer createMcpServer(@Argument("input") Map<String, Object> input) {
        String name = (String) input.get("name");
        ModeType type = ModeType.valueOf((String) input.get("type"));
        Environment environment = Environment.valueOf((String) input.get("environment"));

        return mcpServerService.createFromInput(name, type, environment);
    }

    @MutationMapping
    McpServer updateMcpServer(@Argument("id") long id, @Argument("input") Map<String, Object> input) {
        String name = null;
        if (input.containsKey("name")) {
            name = (String) input.get("name");
        }

        ModeType type = null;
        if (input.containsKey("type")) {
            type = ModeType.valueOf((String) input.get("type"));
        }

        Environment environment = null;
        if (input.containsKey("environment")) {
            environment = Environment.valueOf((String) input.get("environment"));
        }

        return mcpServerService.updateFromInput(id, name, type, environment);
    }
}
