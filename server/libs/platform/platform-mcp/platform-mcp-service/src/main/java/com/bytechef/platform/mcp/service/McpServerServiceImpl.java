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

import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.audit.McpServerAuditEvents;
import com.bytechef.platform.mcp.audit.McpServerAuditMapper;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import com.bytechef.tenant.domain.TenantKey;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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

    private final McpComponentRepository mcpComponentRepository;
    private final List<McpServerEnablementValidator> mcpServerEnablementValidators;
    private final McpServerRepository mcpServerRepository;
    private final McpToolRepository mcpToolRepository;

    public McpServerServiceImpl(
        McpComponentRepository mcpComponentRepository,
        ObjectProvider<McpServerEnablementValidator> mcpServerEnablementValidatorProvider,
        McpServerRepository mcpServerRepository, McpToolRepository mcpToolRepository) {

        this.mcpComponentRepository = mcpComponentRepository;
        this.mcpServerEnablementValidators = mcpServerEnablementValidatorProvider.orderedStream()
            .toList();
        this.mcpServerRepository = mcpServerRepository;
        this.mcpToolRepository = mcpToolRepository;
    }

    @Override
    @Audited(event = McpServerAuditEvents.MCP_SERVER_CREATED, mapper = McpServerAuditMapper.class, data = {
        @AuditData(key = "mcpServerId", value = "#result?.id"),
        @AuditData(key = "name", value = "#result?.name"),
        @AuditData(key = "type", value = "#result?.type"),
        @AuditData(key = "environment", value = "#result?.environment")
    })
    public McpServer create(McpServer mcpServer) {
        checkAuthenticationRequiredForEnforcement(mcpServer);

        return mcpServerRepository.save(mcpServer);
    }

    @Override
    @Audited(event = McpServerAuditEvents.MCP_SERVER_DELETED, mapper = McpServerAuditMapper.class, data = {
        @AuditData(key = "mcpServerId", value = "#mcpServerId")
    })
    public void delete(long mcpServerId) {
        McpServer mcpServer = mcpServerRepository.findById(mcpServerId)
            .orElseThrow(() -> new NoSuchElementException("MCP server with id " + mcpServerId + " not found"));

        for (McpComponent mcpComponent : mcpComponentRepository.findAllByMcpServerId(mcpServerId)) {
            mcpToolRepository.deleteAll(mcpToolRepository.findAllByMcpComponentId(mcpComponent.getId()));

            mcpComponentRepository.delete(mcpComponent);
        }

        mcpServerRepository.delete(mcpServer);
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_VIEW')")
    public McpServer getMcpServer(long mcpServerId) {
        return mcpServerRepository.findById(mcpServerId)
            .orElseThrow(() -> new NoSuchElementException("MCP server with id " + mcpServerId + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')")
    public String getMcpServerSecretKey(long mcpServerId) {
        return mcpServerRepository.findById(mcpServerId)
            .map(McpServer::getSecretKey)
            .orElseThrow(() -> new NoSuchElementException("MCP server with id " + mcpServerId + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')")
    public String getEmbeddedMcpServerSecretKey(long mcpServerId) {
        McpServer mcpServer = mcpServerRepository.findById(mcpServerId)
            .orElseThrow(() -> new IllegalArgumentException("MCP server with id " + mcpServerId + " not found"));

        if (mcpServer.getType() != PlatformType.EMBEDDED) {
            throw new IllegalArgumentException("MCP server with id " + mcpServerId + " is not an embedded server");
        }

        return mcpServer.getSecretKey();
    }

    @Override
    public McpServer getMcpServer(String secretKey) {
        return mcpServerRepository.findBySecretKey(secretKey)
            .orElseThrow(() -> new IllegalArgumentException("MCP server for the given secret key not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpServer> fetchMcpServer(String secretKey) {
        return mcpServerRepository.findBySecretKey(secretKey);
    }

    @Override
    @Audited(event = McpServerAuditEvents.MCP_SERVER_CREATED, mapper = McpServerAuditMapper.class, data = {
        @AuditData(key = "mcpServerId", value = "#result?.id"),
        @AuditData(key = "name", value = "#result?.name"),
        @AuditData(key = "type", value = "#result?.type"),
        @AuditData(key = "environment", value = "#result?.environment")
    })
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
    public List<McpServer> getEnabledMcpServers(PlatformType type) {
        return mcpServerRepository.findAll()
            .stream()
            .filter(server -> server.getType() == type && server.isEnabled())
            .toList();
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
    @Audited(event = McpServerAuditEvents.MCP_SERVER_UPDATED, mapper = McpServerAuditMapper.class, data = {
        @AuditData(key = "mcpServerId", value = "#mcpServer.id"),
        @AuditData(key = "name", value = "#result?.name"),
        @AuditData(key = "enabled", value = "#result?.enabled")
    })
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
    @Audited(event = McpServerAuditEvents.MCP_SERVER_SECRET_KEY_ROTATED, mapper = McpServerAuditMapper.class, data = {
        @AuditData(key = "mcpServerId", value = "#mcpServerId"),
        @AuditData(key = "name", value = "#result?.name"),
        @AuditData(key = "type", value = "#result?.type")
    })
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')")
    public McpServer rotateSecretKey(long mcpServerId) {
        McpServer mcpServer = mcpServerRepository.findById(mcpServerId)
            .orElseThrow(() -> new NoSuchElementException("MCP server with id " + mcpServerId + " not found"));

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
    @Audited(event = McpServerAuditEvents.MCP_SERVER_UPDATED, mapper = McpServerAuditMapper.class, data = {
        @AuditData(key = "mcpServerId", value = "#id"),
        @AuditData(key = "name", value = "#result?.name"),
        @AuditData(key = "enabled", value = "#result?.enabled")
    })
    @PreAuthorize("hasPermission(#id, 'McpServer', 'MCP_EDIT')")
    public McpServer update(long id, String name, Boolean enabled) {
        return update(id, name, enabled, null, null);
    }

    @Override
    @Audited(event = McpServerAuditEvents.MCP_SERVER_UPDATED, mapper = McpServerAuditMapper.class, data = {
        @AuditData(key = "mcpServerId", value = "#id"),
        @AuditData(key = "name", value = "#result?.name"),
        @AuditData(key = "enabled", value = "#result?.enabled"),
        @AuditData(key = "authenticationRequired", value = "#authenticationRequired"),
        @AuditData(key = "enforceToolAuthorization", value = "#enforceToolAuthorization")
    })
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
