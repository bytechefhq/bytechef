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
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
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

    private final McpComponentService mcpComponentService;
    private final McpProjectFacade mcpProjectFacade;
    private final McpProjectService mcpProjectService;
    private final McpServerFacade mcpServerFacade;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;
    private final TagService tagService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public WorkspaceMcpServerFacadeImpl(
        McpComponentService mcpComponentService, McpProjectFacade mcpProjectFacade,
        McpProjectService mcpProjectService, McpServerFacade mcpServerFacade, McpServerService mcpServerService,
        McpToolService mcpToolService, TagService tagService, WorkspaceMcpServerService workspaceMcpServerService) {

        this.mcpComponentService = mcpComponentService;
        this.mcpProjectFacade = mcpProjectFacade;
        this.mcpProjectService = mcpProjectService;
        this.mcpServerFacade = mcpServerFacade;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
        this.tagService = tagService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'MCP_VIEW')")
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
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'MCP_VIEW')")
    public List<McpServer> getWorkspaceMcpServers(Long workspaceId) {
        List<WorkspaceMcpServer> workspaceMcpServers = workspaceMcpServerService.getWorkspaceMcpServers(workspaceId);

        return workspaceMcpServers.stream()
            .map(workspaceMcpServer -> mcpServerService.getMcpServer(workspaceMcpServer.getMcpServerId()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'MCP_VIEW')")
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
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'MCP_CREATE')")
    public McpServer createWorkspaceMcpServer(
        String name, PlatformType type, Environment environment, Boolean enabled, Boolean authenticationRequired,
        Long workspaceId) {

        McpServer mcpServer = new McpServer(name, type, environment);

        if (enabled != null) {
            mcpServer.setEnabled(enabled);
        }

        if (authenticationRequired != null) {
            mcpServer.setAuthenticationRequired(authenticationRequired);
        }

        mcpServer = mcpServerService.create(mcpServer);

        workspaceMcpServerService.assignMcpServerToWorkspace(mcpServer.getId(), workspaceId);

        return mcpServer;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')")
    public McpServer updateWorkspaceMcpServer(Long mcpServerId, String name, Boolean enabled) {
        return mcpServerService.update(mcpServerId, name, enabled);
    }

    @Override
    @PreAuthorize("hasPermission(#mcpToolId, 'McpTool', 'MCP_EDIT')")
    public McpTool updateWorkspaceMcpToolEnabled(long mcpToolId, boolean enabled) {
        getAutomationMcpTool(mcpToolId);

        mcpToolService.updateEnabled(mcpToolId, enabled);

        return getAutomationMcpTool(mcpToolId);
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')")
    public void deleteWorkspaceMcpServer(Long mcpServerId) {
        for (McpProject mcpProject : mcpProjectService.getMcpServerMcpProjects(mcpServerId)) {
            mcpProjectFacade.deleteMcpProject(mcpProject.getId());
        }

        mcpServerFacade.deleteMcpServer(mcpServerId);
    }

    private McpTool getAutomationMcpTool(long mcpToolId) {
        McpTool mcpTool = mcpToolService.fetchMcpTool(mcpToolId)
            .orElseThrow(() -> new IllegalArgumentException("MCP tool not found: " + mcpToolId));

        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpTool.getMcpComponentId());

        McpServer mcpServer = mcpServerService.getMcpServer(mcpComponent.getMcpServerId());

        if (mcpServer.getType() != PlatformType.AUTOMATION) {
            throw new IllegalArgumentException(
                "MCP server %s is not an automation MCP server".formatted(mcpComponent.getMcpServerId()));
        }

        return mcpTool;
    }
}
