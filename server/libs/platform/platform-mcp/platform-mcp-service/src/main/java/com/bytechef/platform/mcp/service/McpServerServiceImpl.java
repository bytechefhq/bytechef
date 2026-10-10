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

package com.bytechef.platform.mcp.service;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.tenant.domain.TenantKey;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link McpServerService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpServerServiceImpl implements McpServerService {

    private final List<McpServerEnablementValidator> mcpServerEnablementValidators;
    private final McpServerRepository mcpServerRepository;

    public McpServerServiceImpl(
        ObjectProvider<McpServerEnablementValidator> mcpServerEnablementValidatorProvider,
        McpServerRepository mcpServerRepository) {

        this.mcpServerEnablementValidators = mcpServerEnablementValidatorProvider.orderedStream()
            .toList();
        this.mcpServerRepository = mcpServerRepository;
    }

    @Override
    public McpServer create(McpServer mcpServer) {
        checkAuthenticationRequiredForEnforcement(mcpServer);

        return mcpServerRepository.save(mcpServer);
    }

    @Override
    public void delete(long mcpServerId) {
        mcpServerRepository.deleteById(mcpServerId);
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_VIEW')")
    public McpServer getMcpServer(long mcpServerId) {
        return mcpServerRepository.findById(mcpServerId)
            .orElseThrow(() -> new IllegalArgumentException("MCP server with id " + mcpServerId + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isTenantAdmin()")
    public String getMcpServerSecretKey(long mcpServerId) {
        return mcpServerRepository.findById(mcpServerId)
            .map(McpServer::getSecretKey)
            .orElseThrow(() -> new IllegalArgumentException("MCP server with id " + mcpServerId + " not found"));
    }

    @Override
    public McpServer getMcpServer(String secretKey) {
        return mcpServerRepository.findBySecretKey(secretKey)
            .orElseThrow(() -> new IllegalArgumentException("MCP server for the given secret key not found"));
    }

    @Override
    public McpServer create(String name, PlatformType type, Environment environment, Boolean enabled) {
        McpServer mcpServer;

        if (enabled != null) {
            mcpServer = new McpServer(name, type, environment, enabled);
        } else {
            mcpServer = new McpServer(name, type, environment);
        }

        return create(mcpServer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpServer> getMcpServers(PlatformType type) {
        return mcpServerRepository.findAll()
            .stream()
            .filter(mcpServer -> mcpServer.getType() == type)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServer.id, 'McpServer', 'MCP_EDIT')")
    public McpServer update(McpServer mcpServer) {
        checkAuthenticationRequiredForEnforcement(mcpServer);

        McpServer currentMcpServer = mcpServerRepository.findById(mcpServer.getId())
            .orElseThrow(
                () -> new IllegalArgumentException("MCP server with id " + mcpServer.getId() + " not found"));

        if (mcpServer.isEnabled() && !currentMcpServer.isEnabled()) {
            checkEnablement(mcpServer.getId());
        }

        currentMcpServer.setName(mcpServer.getName());
        currentMcpServer.setEnabled(mcpServer.isEnabled());
        currentMcpServer.setAuthenticationRequired(mcpServer.isAuthenticationRequired());
        currentMcpServer.setEnforceToolAuthorization(mcpServer.isEnforceToolAuthorization());
        currentMcpServer.setTagIds(mcpServer.getTagIds());
        currentMcpServer.setVersion(mcpServer.getVersion());

        return mcpServerRepository.save(currentMcpServer);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpServer rotateSecretKey(long mcpServerId) {
        McpServer mcpServer = mcpServerRepository.findById(mcpServerId)
            .orElseThrow(() -> new IllegalArgumentException("MCP server with id " + mcpServerId + " not found"));

        mcpServer.setSecretKey(String.valueOf(TenantKey.of()));

        return mcpServerRepository.save(mcpServer);
    }

    private void checkAuthenticationRequiredForEnforcement(McpServer mcpServer) {
        if (!mcpServer.isAuthenticationRequired() && mcpServer.isEnforceToolAuthorization()) {
            throw new IllegalArgumentException(
                "enforceToolAuthorization requires authenticationRequired to be enabled");
        }
    }

    private void checkEnablement(long mcpServerId) {
        for (McpServerEnablementValidator mcpServerEnablementValidator : mcpServerEnablementValidators) {
            mcpServerEnablementValidator.validateEnablement(mcpServerId);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'McpServer', 'MCP_EDIT')")
    public McpServer update(long id, String name, Boolean enabled) {
        return update(id, name, enabled, null, null);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'McpServer', 'MCP_EDIT')")
    public McpServer update(
        long id, String name, Boolean enabled, Boolean enforceToolAuthorization, Boolean authenticationRequired) {

        McpServer existingMcpServer = getMcpServer(id);

        if (name != null) {
            existingMcpServer.setName(name);
        }

        if (Boolean.TRUE.equals(enabled)) {
            checkEnablement(id);
        }

        if (enabled != null) {
            existingMcpServer.setEnabled(enabled);
        }

        if (enforceToolAuthorization != null) {
            existingMcpServer.setEnforceToolAuthorization(enforceToolAuthorization);
        }

        if (authenticationRequired != null) {
            existingMcpServer.setAuthenticationRequired(authenticationRequired);
        }

        checkAuthenticationRequiredForEnforcement(existingMcpServer);

        return mcpServerRepository.save(existingMcpServer);
    }

    @Override
    public McpServer updateTags(long id, List<Long> tagIds) {
        McpServer mcpServer = getMcpServer(id);

        mcpServer.setTagIds(tagIds);

        return mcpServerRepository.save(mcpServer);
    }
}
