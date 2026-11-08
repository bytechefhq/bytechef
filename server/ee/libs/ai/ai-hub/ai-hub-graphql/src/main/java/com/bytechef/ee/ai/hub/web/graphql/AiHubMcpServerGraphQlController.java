/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpServer;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpServerFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnAiHub
public class AiHubMcpServerGraphQlController {

    private final AiHubMcpServerFacade mcpServerFacade;
    private final PermissionService permissionService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public AiHubMcpServerGraphQlController(
        AiHubMcpServerFacade mcpServerFacade, PermissionService permissionService, UserService userService) {

        this.mcpServerFacade = mcpServerFacade;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @QueryMapping
    public List<AiHubMcpServerResult> aiHubMcpServers(@Argument long workspaceId) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        return mcpServerFacade.listMcpServers(currentUserId(), workspaceId)
            .stream()
            .map(AiHubMcpServerResult::of)
            .toList();
    }

    @MutationMapping
    public String addAiHubMcpServer(
        @Argument long workspaceId, @Argument String name, @Argument String url, @Argument @Nullable String authToken,
        @Argument int environment) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long mcpServerId = mcpServerFacade.addMcpServer(
            currentUserId(), workspaceId, environment, name, url, authToken);

        return String.valueOf(mcpServerId);
    }

    @MutationMapping
    public boolean removeAiHubMcpServer(@Argument long workspaceId, @Argument long mcpServerId) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        return mcpServerFacade.removeMcpServer(currentUserId(), workspaceId, mcpServerId);
    }

    @MutationMapping
    public boolean setAiHubMcpServerEnabled(
        @Argument long workspaceId, @Argument long mcpServerId, @Argument boolean enabled) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        return mcpServerFacade.setMcpServerEnabled(currentUserId(), workspaceId, mcpServerId, enabled);
    }

    @QueryMapping
    public List<AiHubMcpServerToolResult> aiHubMcpServerTools(
        @Argument long workspaceId, @Argument long mcpServerId) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        return mcpServerFacade.listMcpServerTools(currentUserId(), workspaceId, mcpServerId)
            .stream()
            .map(tool -> new AiHubMcpServerToolResult(tool.name(), tool.description(), tool.enabled()))
            .toList();
    }

    @MutationMapping
    public boolean setAiHubMcpServerToolEnabled(
        @Argument long workspaceId, @Argument long mcpServerId, @Argument String toolName, @Argument boolean enabled) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        return mcpServerFacade.setMcpServerToolEnabled(currentUserId(), workspaceId, mcpServerId, toolName, enabled);
    }

    private long currentUserId() {
        return userService.getCurrentUser()
            .getId();
    }

    public record AiHubMcpServerResult(long id, String name, String url, boolean enabled, boolean hasAuthToken) {

        static AiHubMcpServerResult of(AiHubMcpServer mcpServer) {
            return new AiHubMcpServerResult(
                mcpServer.getId(), mcpServer.getName(), mcpServer.getUrl(), mcpServer.isEnabled(),
                mcpServer.getAuthToken() != null);
        }
    }

    public record AiHubMcpServerToolResult(String name, @Nullable String description, boolean enabled) {
    }
}
