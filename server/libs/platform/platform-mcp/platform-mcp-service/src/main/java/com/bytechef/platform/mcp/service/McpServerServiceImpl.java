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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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
        if (mcpServer.getUuid() == null) {
            mcpServer.setUuid(UUID.randomUUID());
        }

        return mcpServerRepository.save(mcpServer);
    }

    @Override
    public void delete(long mcpServerId) {
        mcpServerRepository.deleteById(mcpServerId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameAndEnvironment(String name, Environment environment, @Nullable UUID excludeUuid) {
        Assert.notNull(name, "'name' must not be null");

        if (excludeUuid == null) {
            return mcpServerRepository.existsByNameAndEnvironment(name, environment.ordinal());
        }

        return mcpServerRepository.existsByNameAndEnvironmentAndUuidNot(name, environment.ordinal(), excludeUuid);
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
            .orElseThrow(() -> new IllegalArgumentException("MCP server with secret key " + secretKey + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpServer> fetchMcpServer(UUID uuid, Environment environment) {
        return mcpServerRepository.findByUuidAndEnvironment(uuid, environment.ordinal());
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
        return getMcpServers(type, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpServer> getMcpServers(PlatformType type, McpServerOrderBy orderBy) {
        List<McpServer> servers = mcpServerRepository.findAll()
            .stream()
            .filter(server -> server.getType() == type)
            .toList();

        if (orderBy == null) {
            return servers;
        }

        Comparator<McpServer> comparator = switch (orderBy) {
            case NAME_ASC -> Comparator.comparing(McpServer::getName, String.CASE_INSENSITIVE_ORDER);
            case NAME_DESC -> Comparator.comparing(McpServer::getName, String.CASE_INSENSITIVE_ORDER)
                .reversed();
            case CREATED_DATE_ASC ->
                Comparator.comparing(McpServer::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case CREATED_DATE_DESC ->
                Comparator.comparing(McpServer::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed();
            case LAST_MODIFIED_DATE_ASC ->
                Comparator.comparing(McpServer::getLastModifiedDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case LAST_MODIFIED_DATE_DESC ->
                Comparator.comparing(McpServer::getLastModifiedDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed();
        };

        return servers.stream()
            .sorted(comparator)
            .toList();
    }

    @Override
    public McpServer update(McpServer mcpServer) {
        if (!mcpServer.isAuthenticationRequired() && mcpServer.isEnforceToolAuthorization()) {
            throw new IllegalArgumentException(
                "enforceToolAuthorization requires authenticationRequired to be enabled");
        }

        McpServer currentMcpServer = getMcpServer(mcpServer.getId());

        currentMcpServer.setName(mcpServer.getName());
        currentMcpServer.setEnabled(mcpServer.isEnabled());
        currentMcpServer.setAuthenticationRequired(mcpServer.isAuthenticationRequired());
        currentMcpServer.setEnforceToolAuthorization(mcpServer.isEnforceToolAuthorization());
        currentMcpServer.setSecretKey(mcpServer.getSecretKey());
        currentMcpServer.setTagIds(mcpServer.getTagIds());
        currentMcpServer.setVersion(mcpServer.getVersion());

        return mcpServerRepository.save(currentMcpServer);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'McpServer', 'MCP_EDIT')")
    public McpServer update(long id, String name, Boolean enabled) {
        McpServer existingMcpServer = getMcpServer(id);

        if (name != null) {
            existingMcpServer.setName(name);
        }

        if (Boolean.TRUE.equals(enabled)) {
            for (McpServerEnablementValidator mcpServerEnablementValidator : mcpServerEnablementValidators) {
                mcpServerEnablementValidator.validateEnablement(id);
            }
        }

        if (enabled != null) {
            existingMcpServer.setEnabled(enabled);
        }

        return mcpServerRepository.save(existingMcpServer);
    }

    @Override
    public McpServer updateTags(long id, List<Long> tagIds) {
        McpServer mcpServer = getMcpServer(id);

        mcpServer.setTagIds(tagIds);

        return mcpServerRepository.save(mcpServer);
    }
}
