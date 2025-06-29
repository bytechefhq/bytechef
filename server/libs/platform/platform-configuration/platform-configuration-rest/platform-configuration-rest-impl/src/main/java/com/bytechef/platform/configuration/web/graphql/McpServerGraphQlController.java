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
    List<McpServer> mcpServers(@Argument ModeType type) {
        return mcpServerService.getMcpServers(type);
    }

    @MutationMapping
    McpServer createMcpServer(
        @Argument String name, @Argument ModeType type, @Argument Environment environment, @Argument Boolean enabled) {
        return mcpServerService.create(name, type, environment, enabled);
    }

    @MutationMapping
    McpServer updateMcpServer(
        @Argument long id, @Argument String name, @Argument ModeType type, @Argument Environment environment,
        @Argument Boolean enabled) {

        return mcpServerService.update(id, name, type, environment, enabled);
    }

    @MutationMapping
    boolean deleteMcpServer(@Argument long id) {
        mcpServerService.delete(id);

        return true;
    }
}
