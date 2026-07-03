/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerToolRepository;
import com.bytechef.encryption.Encryption;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.client.McpSyncClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Resolves the {@link ToolCallback}s for a user's ENABLED external MCP servers at agent-turn time: for each server it
 * gets a cached client, lists its tools, and bridges them via Spring AI's {@link SyncMcpToolCallbackProvider}. The
 * {@link com.bytechef.ee.ai.hub.toolsearch.AiHubTaskBindingToolCallbackResolver} unions these with the task's own tools
 * and the user's added connectors.
 *
 * <p>
 * Each server is resolved independently and defensively: a server that is unreachable (or whose handshake fails) is
 * logged and skipped so the rest of the agent's tools still register and the turn proceeds.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class AiHubMcpToolCallbackProvider {

    private static final Logger log = LoggerFactory.getLogger(AiHubMcpToolCallbackProvider.class);

    private final AiHubMcpServerFacade mcpServerFacade;
    private final AiHubMcpServerToolRepository mcpServerToolRepository;
    private final AiHubMcpClientManager clientManager;
    private final Encryption encryption;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubMcpToolCallbackProvider(
        AiHubMcpServerFacade mcpServerFacade, AiHubMcpServerToolRepository mcpServerToolRepository,
        AiHubMcpClientManager clientManager, Encryption encryption) {

        this.mcpServerFacade = mcpServerFacade;
        this.mcpServerToolRepository = mcpServerToolRepository;
        this.clientManager = clientManager;
        this.encryption = encryption;
    }

    public List<ToolCallback> resolve(long userId, long workspaceId) {
        List<ToolCallback> callbacks = new ArrayList<>();

        for (AiHubMcpServer mcpServer : mcpServerFacade.listMcpServers(userId, workspaceId)) {
            if (!mcpServer.isEnabled()) {
                continue;
            }

            String bearerToken = decryptToken(mcpServer.getAuthToken());

            try {
                McpSyncClient client = clientManager.getOrCreateClient(
                    mcpServer.getId(), mcpServer.getUrl(), bearerToken);

                // Honor the per-tool toggles: a tool with a stored enabled=false row is filtered out of the agent's
                // tool list (a tool with no row is enabled by default).
                Set<String> disabledTools = mcpServerToolRepository.findAllByMcpServerId(mcpServer.getId())
                    .stream()
                    .filter(tool -> !tool.isEnabled())
                    .map(tool -> tool.getName())
                    .collect(Collectors.toSet());

                McpToolFilter toolFilter = (connectionInfo, tool) -> !disabledTools.contains(tool.name());

                SyncMcpToolCallbackProvider toolCallbackProvider = SyncMcpToolCallbackProvider.builder()
                    .mcpClients(client)
                    .toolFilter(toolFilter)
                    .build();

                callbacks.addAll(List.of(toolCallbackProvider.getToolCallbacks()));
            } catch (RuntimeException exception) {
                // Unreachable server / failed handshake / listTools error — skip this server for the turn and drop
                // its (possibly broken) cached client so the next turn reconnects.
                log.warn(
                    "Skipping MCP server {} ({}) — could not load its tools", mcpServer.getId(), mcpServer.getUrl(),
                    exception);

                clientManager.invalidate(mcpServer.getId(), mcpServer.getUrl(), bearerToken);
            }
        }

        return callbacks;
    }

    private @Nullable String decryptToken(@Nullable String encryptedToken) {
        return encryptedToken == null ? null : encryption.decrypt(encryptedToken);
    }
}
