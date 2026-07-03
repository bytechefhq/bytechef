/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpServer;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpServerFacade;
import com.bytechef.ee.ai.hub.security.WorkspaceAccessGuard;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for a user's external MCP server registrations (the Connectors page "Custom MCP" section). All
 * operations are workspace-scoped and verify the caller can access the supplied workspace; mutations additionally
 * verify the server belongs to the caller. The stored bearer token is never returned — the {@code AiHubMcpServer} type
 * exposes only {@code hasAuthToken}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@PreAuthorize("isAuthenticated()")
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class AiHubMcpServerGraphQlController {

    private final AiHubMcpServerFacade mcpServerFacade;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public AiHubMcpServerGraphQlController(
        AiHubMcpServerFacade mcpServerFacade, UserService userService, WorkspaceFacade workspaceFacade) {

        this.mcpServerFacade = mcpServerFacade;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @QueryMapping
    public List<AiHubMcpServerResult> aiHubMcpServers(@Argument long workspaceId) {
        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        return mcpServerFacade.listMcpServers(userId, workspaceId)
            .stream()
            .map(AiHubMcpServerResult::of)
            .toList();
    }

    @MutationMapping
    public String addAiHubMcpServer(
        @Argument long workspaceId, @Argument String name, @Argument String url, @Argument @Nullable String authToken,
        @Argument int environment) {

        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        long mcpServerId = mcpServerFacade.addMcpServer(userId, workspaceId, environment, name, url, authToken);

        return String.valueOf(mcpServerId);
    }

    @MutationMapping
    public boolean removeAiHubMcpServer(@Argument long workspaceId, @Argument long mcpServerId) {
        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        if (!userOwnsMcpServer(userId, workspaceId, mcpServerId)) {
            return false;
        }

        mcpServerFacade.removeMcpServer(mcpServerId);

        return true;
    }

    @MutationMapping
    public boolean setAiHubMcpServerEnabled(
        @Argument long workspaceId, @Argument long mcpServerId, @Argument boolean enabled) {

        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        if (!userOwnsMcpServer(userId, workspaceId, mcpServerId)) {
            return false;
        }

        mcpServerFacade.setMcpServerEnabled(mcpServerId, enabled);

        return true;
    }

    @QueryMapping
    public List<AiHubMcpServerToolResult> aiHubMcpServerTools(
        @Argument long workspaceId, @Argument long mcpServerId) {

        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        if (!userOwnsMcpServer(userId, workspaceId, mcpServerId)) {
            return List.of();
        }

        return mcpServerFacade.listMcpServerTools(mcpServerId)
            .stream()
            .map(tool -> new AiHubMcpServerToolResult(tool.name(), tool.description(), tool.enabled()))
            .toList();
    }

    @MutationMapping
    public boolean setAiHubMcpServerToolEnabled(
        @Argument long workspaceId, @Argument long mcpServerId, @Argument String toolName, @Argument boolean enabled) {

        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        if (!userOwnsMcpServer(userId, workspaceId, mcpServerId)) {
            return false;
        }

        mcpServerFacade.setMcpServerToolEnabled(mcpServerId, toolName, enabled);

        return true;
    }

    // Ownership check before a mutation: the server must be one of the caller's registrations. Returning false
    // (rather than throwing) on a foreign/unknown id keeps the mutation idempotent and prevents id probing.
    private boolean userOwnsMcpServer(long userId, long workspaceId, long mcpServerId) {
        return mcpServerFacade.listMcpServers(userId, workspaceId)
            .stream()
            .anyMatch(mcpServer -> mcpServer.getId() == mcpServerId);
    }

    private long currentUserId() {
        return userService.getCurrentUser()
            .getId();
    }

    /**
     * GraphQL projection of {@link AiHubMcpServer} — deliberately omits the (encrypted) auth token, exposing only
     * whether one is set.
     */
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
