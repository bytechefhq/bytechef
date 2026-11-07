/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpClientManager.McpToolInfo;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerRepository;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerToolRepository;
import com.bytechef.encryption.Encryption;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnAiHub
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

        urlGuard.validate(url);

        String encryptedToken = authToken == null || authToken.isBlank() ? null : encryption.encrypt(authToken);

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
    public boolean removeMcpServer(long userId, long workspaceId, long mcpServerId) {
        Optional<AiHubMcpServer> mcpServerOptional = findOwned(userId, workspaceId, mcpServerId);

        if (mcpServerOptional.isEmpty()) {
            return false;
        }

        AiHubMcpServer mcpServer = mcpServerOptional.get();

        mcpServerRepository.deleteById(mcpServerId);

        clientManager.invalidate(mcpServerId, mcpServer.getUrl(), decryptToken(mcpServer.getAuthToken()));

        return true;
    }

    @Override
    public boolean setMcpServerEnabled(long userId, long workspaceId, long mcpServerId, boolean enabled) {
        Optional<AiHubMcpServer> mcpServerOptional = findOwned(userId, workspaceId, mcpServerId);

        if (mcpServerOptional.isEmpty()) {
            return false;
        }

        AiHubMcpServer mcpServer = mcpServerOptional.get();

        mcpServer.setEnabled(enabled);

        mcpServerRepository.save(mcpServer);

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpServerToolInfo> listMcpServerTools(long userId, long workspaceId, long mcpServerId) {
        Optional<AiHubMcpServer> mcpServerOptional = findOwned(userId, workspaceId, mcpServerId);

        if (mcpServerOptional.isEmpty()) {
            return List.of();
        }

        AiHubMcpServer mcpServer = mcpServerOptional.get();

        List<McpToolInfo> tools = clientManager.listTools(
            mcpServer.getId(), mcpServer.getUrl(), decryptToken(mcpServer.getAuthToken()));

        Set<String> disabledToolNames = mcpServerToolRepository.findAllByMcpServerId(mcpServerId)
            .stream()
            .filter(tool -> !tool.isEnabled())
            .map(AiHubMcpServerTool::getName)
            .collect(Collectors.toSet());

        return tools.stream()
            .map(tool -> new McpServerToolInfo(
                tool.name(), tool.description(), !disabledToolNames.contains(tool.name())))
            .toList();
    }

    @Override
    public boolean setMcpServerToolEnabled(
        long userId, long workspaceId, long mcpServerId, String toolName, boolean enabled) {

        if (findOwned(userId, workspaceId, mcpServerId).isEmpty()) {
            return false;
        }

        AiHubMcpServerTool tool = mcpServerToolRepository.findByMcpServerIdAndName(mcpServerId, toolName)
            .orElseGet(() -> new AiHubMcpServerTool(mcpServerId, toolName, enabled));

        tool.setEnabled(enabled);

        mcpServerToolRepository.save(tool);

        return true;
    }

    private Optional<AiHubMcpServer> findOwned(long userId, long workspaceId, long mcpServerId) {
        return mcpServerRepository.findById(mcpServerId)
            .filter(mcpServer -> mcpServer.getUserId() == userId &&
                Objects.equals(mcpServer.getWorkspaceId(), workspaceId));
    }

    private @Nullable String decryptToken(@Nullable String encryptedToken) {
        return encryptedToken == null ? null : encryption.decrypt(encryptedToken);
    }
}
