/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerRepository;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerToolRepository;
import com.bytechef.encryption.Encryption;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubMcpToolCallbackProvider {

    private static final Logger log = LoggerFactory.getLogger(AiHubMcpToolCallbackProvider.class);

    private final AiHubMcpServerRepository mcpServerRepository;
    private final AiHubMcpServerToolRepository mcpServerToolRepository;
    private final AiHubMcpClientManager clientManager;
    private final Encryption encryption;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubMcpToolCallbackProvider(
        AiHubMcpServerRepository mcpServerRepository, AiHubMcpServerToolRepository mcpServerToolRepository,
        AiHubMcpClientManager clientManager, Encryption encryption) {

        this.mcpServerRepository = mcpServerRepository;
        this.mcpServerToolRepository = mcpServerToolRepository;
        this.clientManager = clientManager;
        this.encryption = encryption;
    }

    public List<AiHubMcpToolCallback> resolve(long userId, long workspaceId) {
        List<AiHubMcpToolCallback> toolCallbacks = new ArrayList<>();
        Set<String> toolNames = new HashSet<>();

        for (AiHubMcpServer mcpServer : mcpServerRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId)) {
            if (!mcpServer.isEnabled()) {
                continue;
            }

            String bearerToken = decryptToken(mcpServer.getAuthToken());

            try {
                McpSyncClient client = clientManager.getOrCreateClient(
                    mcpServer.getId(), mcpServer.getUrl(), bearerToken);

                Set<String> disabledToolNames = mcpServerToolRepository.findAllByMcpServerId(mcpServer.getId())
                    .stream()
                    .filter(tool -> !tool.isEnabled())
                    .map(AiHubMcpServerTool::getName)
                    .collect(Collectors.toSet());

                McpSchema.ListToolsResult listToolsResult = client.listTools();

                for (McpSchema.Tool tool : listToolsResult.tools()) {
                    if (disabledToolNames.contains(tool.name())) {
                        continue;
                    }

                    String toolName = McpToolUtils.format(tool.name());

                    if (!toolNames.add(toolName)) {
                        log.warn(
                            "MCP tool name {} of server {} is already taken by another server, so it is left out",
                            toolName, mcpServer.getId());

                        continue;
                    }

                    SyncMcpToolCallback toolCallback = SyncMcpToolCallback.builder()
                        .mcpClient(client)
                        .tool(tool)
                        .prefixedToolName(toolName)
                        .build();

                    toolCallbacks.add(new AiHubMcpToolCallback(toolCallback, mcpServer.getId(), isReadOnly(tool)));
                }
            } catch (RuntimeException exception) {
                log.warn(
                    "Skipping MCP server {}: could not load its tools", mcpServer.getId(), exception);

                clientManager.invalidate(mcpServer.getId(), mcpServer.getUrl(), bearerToken);
            }
        }

        return toolCallbacks;
    }

    static boolean isReadOnly(McpSchema.Tool tool) {
        McpSchema.ToolAnnotations annotations = tool.annotations();

        return annotations != null && Boolean.TRUE.equals(annotations.readOnlyHint());
    }

    private @Nullable String decryptToken(@Nullable String encryptedToken) {
        return encryptedToken == null ? null : encryption.decrypt(encryptedToken);
    }
}
