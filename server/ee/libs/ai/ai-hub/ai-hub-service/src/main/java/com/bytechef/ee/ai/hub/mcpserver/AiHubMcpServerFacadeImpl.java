/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpClientManager.McpToolInfo;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerRepository;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerToolRepository;
import com.bytechef.encryption.Encryption;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AiHubMcpServerFacadeImpl implements AiHubMcpServerFacade {

    private final AiHubMcpServerRepository mcpServerRepository;
    private final AiHubMcpServerToolRepository mcpServerToolRepository;
    private final AiHubMcpClientManager clientManager;
    private final Encryption encryption;
    private final AiHubMcpServerUrlGuard urlGuard;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubMcpServerFacadeImpl(
        AiHubMcpServerRepository mcpServerRepository, AiHubMcpServerToolRepository mcpServerToolRepository,
        AiHubMcpClientManager clientManager, Encryption encryption, AiHubMcpServerUrlGuard urlGuard) {

        this.mcpServerRepository = mcpServerRepository;
        this.mcpServerToolRepository = mcpServerToolRepository;
        this.clientManager = clientManager;
        this.encryption = encryption;
        this.urlGuard = urlGuard;
    }

    @Override
    public long addMcpServer(
        long userId, long workspaceId, int environment, String name, String url, @Nullable String authToken) {

        // SSRF guard: reject non-http(s) schemes and any host resolving to a loopback/private/link-local/
        // cloud-metadata address (unless operator-allowlisted) BEFORE persisting, so a user can't register a URL the
        // agent would later dial into the internal network. Re-checked at connect time in AiHubMcpClientManager
        // (DNS-rebind defense).
        urlGuard.validate(url);

        // Encrypt the bearer token at rest. A blank token means "no auth" — persist NULL so the agent's
        // outbound MCP client sends no Authorization header.
        String encryptedToken = StringUtils.isBlank(authToken) ? null : encryption.encrypt(authToken);

        AiHubMcpServer mcpServer = new AiHubMcpServer(userId, workspaceId, environment, name, url, encryptedToken);

        mcpServer = mcpServerRepository.save(mcpServer);

        return mcpServer.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubMcpServer> listMcpServers(long userId, long workspaceId) {
        return mcpServerRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId);
    }

    @Override
    public void removeMcpServer(long mcpServerId) {
        mcpServerRepository.deleteById(mcpServerId);
    }

    @Override
    public void setMcpServerEnabled(long mcpServerId, boolean enabled) {
        AiHubMcpServer mcpServer = mcpServerRepository.findById(mcpServerId)
            .orElseThrow(() -> new IllegalArgumentException("AiHubMcpServer " + mcpServerId + " not found"));

        mcpServer.setEnabled(enabled);

        mcpServerRepository.save(mcpServer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpServerToolInfo> listMcpServerTools(long mcpServerId) {
        AiHubMcpServer mcpServer = mcpServerRepository.findById(mcpServerId)
            .orElseThrow(() -> new IllegalArgumentException("AiHubMcpServer " + mcpServerId + " not found"));

        String bearerToken = mcpServer.getAuthToken() == null ? null : encryption.decrypt(mcpServer.getAuthToken());

        List<McpToolInfo> tools = clientManager.listTools(mcpServer.getId(), mcpServer.getUrl(), bearerToken);

        // A stored row with enabled=false is a deviation from the all-tools-enabled default.
        Set<String> disabledTools = mcpServerToolRepository.findAllByMcpServerId(mcpServerId)
            .stream()
            .filter(tool -> !tool.isEnabled())
            .map(AiHubMcpServerTool::getName)
            .collect(Collectors.toSet());

        return tools.stream()
            .map(tool -> new McpServerToolInfo(tool.name(), tool.description(), !disabledTools.contains(tool.name())))
            .toList();
    }

    @Override
    public void setMcpServerToolEnabled(long mcpServerId, String toolName, boolean enabled) {
        // Upsert the per-tool row carrying the enabled flag — a missing row defaults to enabled, so disabling
        // persists a row and re-enabling flips it back.
        AiHubMcpServerTool tool = mcpServerToolRepository.findByMcpServerIdAndName(mcpServerId, toolName)
            .orElseGet(() -> new AiHubMcpServerTool(mcpServerId, toolName, enabled));

        tool.setEnabled(enabled);

        mcpServerToolRepository.save(tool);
    }
}
