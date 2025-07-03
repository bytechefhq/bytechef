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

package com.bytechef.platform.configuration.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.domain.McpComponent;
import com.bytechef.platform.configuration.domain.McpServer;
import com.bytechef.platform.configuration.domain.McpTool;
import com.bytechef.platform.configuration.service.McpComponentService;
import com.bytechef.platform.configuration.service.McpServerService;
import com.bytechef.platform.configuration.service.McpToolService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link McpServerFacade}.
 *
 * @author Ivica Cardic
 */
@Service
public class McpServerFacadeImpl implements McpServerFacade {

    private final McpComponentService mcpComponentService;
    private final McpServerService mcpServerService;
    private final TagService tagService;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public McpServerFacadeImpl(
        McpComponentService mcpComponentService, McpServerService mcpServerService, McpToolService mcpToolService,
        TagService tagService) {

        this.mcpComponentService = mcpComponentService;
        this.mcpServerService = mcpServerService;
        this.tagService = tagService;
        this.mcpToolService = mcpToolService;
    }

    @Override
    public McpComponent create(McpComponent mcpComponent, List<McpTool> mcpTools) {
        McpComponent savedComponent = mcpComponentService.create(mcpComponent);

        if (mcpTools != null && !mcpTools.isEmpty()) {
            for (McpTool mcpTool : mcpTools) {
                @SuppressWarnings("unchecked")
                Map<String, String> parameters = (Map<String, String>) mcpTool.getParameters();

                McpTool toolToCreate = new McpTool(mcpTool.getName(), parameters, savedComponent.getId());

                mcpToolService.create(toolToCreate);
            }
        }

        return savedComponent;
    }

    @Override
    public void deleteMcpComponent(long mcpComponentId) {
        mcpToolService.getMcpComponentMcpTools(mcpComponentId)
            .forEach(existingTool -> mcpToolService.delete(existingTool.getId()));

        mcpComponentService.delete(mcpComponentId);
    }

    @Override
    public void deleteMcpServer(long mcpServerId) {
        mcpComponentService.getMcpServerMcpComponents(mcpServerId)
            .forEach(mcpComponent -> {
                mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())
                    .forEach(mcpTool -> mcpToolService.delete(mcpTool.getId()));

                mcpComponentService.delete(mcpComponent.getId());
            });

        mcpServerService.delete(mcpServerId);
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
    public McpComponent update(McpComponent mcpComponent, List<McpTool> mcpTools) {
        McpComponent updatedComponent = mcpComponentService.update(mcpComponent);

        mcpToolService.getMcpComponentMcpTools(updatedComponent.getId())
            .forEach(existingTool -> mcpToolService.delete(existingTool.getId()));

        if (mcpTools != null && !mcpTools.isEmpty()) {
            for (McpTool mcpTool : mcpTools) {
                @SuppressWarnings("unchecked")
                Map<String, String> parameters = (Map<String, String>) mcpTool.getParameters();

                McpTool toolToCreate = new McpTool(mcpTool.getName(), parameters, updatedComponent.getId());

                mcpToolService.create(toolToCreate);
            }
        }

        return updatedComponent;
    }

    @Override
    public List<Tag> updateMcpServerTags(long id, List<Tag> tags) {
        List<Tag> validatedTags = checkTags(tags);

        mcpServerService.updateTags(id, CollectionUtils.map(validatedTags, Tag::getId));

        return validatedTags;
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }
}
