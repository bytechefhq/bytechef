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
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing {@link Tag} entities related to McpServer.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class McpServerTagGraphQlController {

    private final McpServerService mcpServerService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public McpServerTagGraphQlController(McpServerService mcpServerService, TagService tagService) {
        this.tagService = tagService;
        this.mcpServerService = mcpServerService;
    }

    @QueryMapping
    public List<Tag> mcpServerTags(@Argument(name = "type") PlatformType type) {
        if (type == null) {
            return List.of();
        }

        List<McpServer> mcpServers = mcpServerService.getMcpServers(type);

        List<Long> tagIds = mcpServers.stream()
            .filter(server -> server.getType() == type)
            .flatMap(server -> CollectionUtils.stream(server.getTagIds()))
            .distinct()
            .toList();

        if (tagIds.isEmpty()) {
            return List.of();
        }

        return tagService.getTags(tagIds);
    }
}
