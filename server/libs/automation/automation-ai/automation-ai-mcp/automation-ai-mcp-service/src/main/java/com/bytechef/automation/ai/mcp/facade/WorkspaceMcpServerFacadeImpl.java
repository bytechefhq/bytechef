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

package com.bytechef.automation.ai.mcp.facade;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.WorkspaceMcpServer;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link WorkspaceMcpServerFacade} interface that handles workspace MCP server operations.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceMcpServerFacadeImpl implements WorkspaceMcpServerFacade {

    private final McpProjectService mcpProjectService;
    private final McpServerFacade mcpServerFacade;
    private final McpServerService mcpServerService;
    private final TagService tagService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public WorkspaceMcpServerFacadeImpl(
        McpProjectService mcpProjectService, McpServerFacade mcpServerFacade, McpServerService mcpServerService,
        TagService tagService, WorkspaceMcpServerService workspaceMcpServerService) {

        this.mcpProjectService = mcpProjectService;
        this.mcpServerFacade = mcpServerFacade;
        this.mcpServerService = mcpServerService;
        this.tagService = tagService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
    public List<McpProject> getWorkspaceMcpProjects(Long workspaceId) {
        Set<Long> mcpServerIds = workspaceMcpServerService.getWorkspaceMcpServers(workspaceId)
            .stream()
            .map(WorkspaceMcpServer::getMcpServerId)
            .collect(Collectors.toSet());

        return mcpServerIds.stream()
            .flatMap(mcpServerId -> mcpProjectService.getMcpServerMcpProjects(mcpServerId)
                .stream())
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
    public List<McpServer> getWorkspaceMcpServers(Long workspaceId) {
        List<WorkspaceMcpServer> workspaceMcpServers = workspaceMcpServerService.getWorkspaceMcpServers(workspaceId);

        return workspaceMcpServers.stream()
            .map(workspaceMcpServer -> mcpServerService.getMcpServer(workspaceMcpServer.getMcpServerId()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
    public List<Tag> getWorkspaceMcpServerTags(Long workspaceId) {
        List<Long> tagIds = workspaceMcpServerService.getWorkspaceMcpServers(workspaceId)
            .stream()
            .map(workspaceMcpServer -> mcpServerService.getMcpServer(workspaceMcpServer.getMcpServerId()))
            .flatMap(mcpServer -> CollectionUtils.stream(mcpServer.getTagIds()))
            .distinct()
            .toList();

        if (tagIds.isEmpty()) {
            return List.of();
        }

        return tagService.getTags(tagIds);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'EDITOR')")
    public McpServer createWorkspaceMcpServer(
        String name, PlatformType type, Environment environment, Boolean enabled, Long workspaceId) {

        McpServer mcpServer = mcpServerService.create(name, type, environment, enabled);

        workspaceMcpServerService.assignMcpServerToWorkspace(mcpServer.getId(), workspaceId);

        return mcpServer;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer:ResourceRole', 'EDITOR')")
    public void deleteWorkspaceMcpServer(Long mcpServerId) {
        workspaceMcpServerService.removeMcpServerFromWorkspace(mcpServerId);

        mcpServerFacade.deleteMcpServer(mcpServerId);
    }
}
