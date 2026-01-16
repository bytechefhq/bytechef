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
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
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

    private final McpServerFacade mcpServerFacade;
    private final McpServerService mcpServerService;
    private final String publicUrl;

    @SuppressFBWarnings("EI")
    public McpServerGraphQlController(
        ApplicationProperties applicationProperties, McpServerFacade mcpServerFacade,
        McpServerService mcpServerService) {

        this.mcpServerFacade = mcpServerFacade;
        this.mcpServerService = mcpServerService;
        this.publicUrl = applicationProperties.getPublicUrl();
    }

    @QueryMapping
    public McpServer mcpServer(@Argument long id) {
        return mcpServerService.getMcpServer(id);
    }

    @QueryMapping
    public List<McpServer>
        mcpServers(@Argument PlatformType type, @Argument McpServerService.McpServerOrderBy orderBy) {
        return mcpServerService.getMcpServers(type, orderBy);
    }

    @MutationMapping
    public McpServer createMcpServer(@Argument McpServerInput input) {
        return mcpServerService.create(
            input.name(), input.type(), Environment.values()[(int) input.environmentId], input.enabled());
    }

    @MutationMapping
    public McpServer updateMcpServer(@Argument long id, @Argument McpServerUpdateInput input) {
        return mcpServerService.update(id, input.name(), input.enabled());
    }

    @MutationMapping
    public List<Tag> updateMcpServerTags(@Argument long id, @Argument List<TagInput> tags) {
        List<Tag> tagList = tags.stream()
            .map(tagInput -> {
                Tag tag = new Tag();

                tag.setId(tagInput.id());
                tag.setName(tagInput.name());

                return tag;
            })
            .toList();

        return mcpServerFacade.updateMcpServerTags(id, tagList);
    }

    @MutationMapping
    public boolean deleteMcpServer(@Argument long id) {
        mcpServerFacade.deleteMcpServer(id);

        return true;
    }

    @BatchMapping
    public Map<McpServer, List<McpComponent>> mcpComponents(List<McpServer> mcpServers) {
        return mcpServerFacade.getMcpServerMcpComponents(mcpServers);
    }

    @BatchMapping
    public Map<McpServer, String> url(List<McpServer> mcpServers) {
        return mcpServers.stream()
            .collect(Collectors.toMap(mcpServer -> mcpServer, this::getMcpServerUrl));
    }

    @BatchMapping
    public Map<McpServer, List<Tag>> tags(List<McpServer> mcpServers) {
        return mcpServerFacade.getMcpServerTags(mcpServers);
    }

    @MutationMapping
    public String updateMcpServerUrl(@Argument long id) {
        McpServer mcpServer = mcpServerService.getMcpServer(id);

        mcpServer.setSecretKey(String.valueOf(TenantKey.of()));

        mcpServer = mcpServerService.update(mcpServer);

        return getMcpServerUrl(mcpServer);
    }

    private String getMcpServerUrl(McpServer mcpServer) {
        return publicUrl + "/api/automation/" + mcpServer.getSecretKey() + "/mcp";
    }

    public record TagInput(Long id, String name) {
    }

    public record McpServerInput(String name, PlatformType type, long environmentId, Boolean enabled) {
    }

    public record McpServerUpdateInput(String name, Boolean enabled) {
    }
}
