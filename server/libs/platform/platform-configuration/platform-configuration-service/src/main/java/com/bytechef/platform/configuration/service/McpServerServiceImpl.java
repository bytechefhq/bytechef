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

package com.bytechef.platform.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.configuration.domain.McpServer;
import com.bytechef.platform.configuration.repository.McpServerRepository;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.ModeType;
import java.util.List;
import java.util.Optional;
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

    private final McpServerRepository mcpServerRepository;

    public McpServerServiceImpl(McpServerRepository mcpServerRepository) {
        this.mcpServerRepository = mcpServerRepository;
    }

    @Override
    public McpServer create(McpServer mcpServer) {
        return mcpServerRepository.save(mcpServer);
    }

    @Override
    public McpServer create(String name, Environment environment, Boolean enabled) {
        McpServer mcpServer;

        if (enabled != null) {
            mcpServer = new McpServer(name, ModeType.AUTOMATION, environment, enabled);
        } else {
            mcpServer = new McpServer(name, ModeType.AUTOMATION, environment);
        }

        return create(mcpServer);
    }

    @Override
    public void delete(long mcpServerId) {
        mcpServerRepository.deleteById(mcpServerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpServer> fetchMcpServer(long mcpServerId) {
        return mcpServerRepository.findById(mcpServerId);
    }

    @Override
    public McpServer create(String name, ModeType type, Environment environment, Boolean enabled) {
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
    public List<McpServer> getMcpServers(Long tagId) {
        return mcpServerRepository.findAllByTagId(tagId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpServer> getMcpServers(ModeType type) {
        return mcpServerRepository.findAll()
            .stream()
            .filter(server -> server.getType() == type)
            .toList();
    }

    @Override
    public McpServer update(McpServer mcpServer) {
        McpServer currentMcpServer = OptionalUtils.get(mcpServerRepository.findById(mcpServer.getId()));

        currentMcpServer.setName(mcpServer.getName());
        currentMcpServer.setType(mcpServer.getType());
        currentMcpServer.setEnvironment(mcpServer.getEnvironment());
        currentMcpServer.setEnabled(mcpServer.isEnabled());
        currentMcpServer.setTagIds(mcpServer.getTagIds());
        currentMcpServer.setVersion(mcpServer.getVersion());

        return mcpServerRepository.save(currentMcpServer);
    }

    @Override
    public McpServer update(long id, String name, Environment environment, Boolean enabled) {
        McpServer existingMcpServer = fetchMcpServer(id)
            .orElseThrow(() -> new IllegalArgumentException("McpServer not found with id: " + id));

        if (name != null) {
            existingMcpServer.setName(name);
        }

        if (environment != null) {
            existingMcpServer.setEnvironment(environment);
        }

        if (enabled != null) {
            existingMcpServer.setEnabled(enabled);
        }

        return update(existingMcpServer);
    }

    @Override
    public McpServer update(long id, String name, ModeType type, Environment environment, Boolean enabled) {
        McpServer existingMcpServer = fetchMcpServer(id)
            .orElseThrow(() -> new IllegalArgumentException("McpServer not found with id: " + id));

        if (name != null) {
            existingMcpServer.setName(name);
        }

        if (type != null) {
            existingMcpServer.setType(type);
        }

        if (environment != null) {
            existingMcpServer.setEnvironment(environment);
        }

        if (enabled != null) {
            existingMcpServer.setEnabled(enabled);
        }

        return update(existingMcpServer);
    }

    @Override
    public McpServer updateTags(long id, List<Long> tagIds) {
        McpServer mcpServer = fetchMcpServer(id)
            .orElseThrow(() -> new IllegalArgumentException("McpServer not found with id: " + id));

        mcpServer.setTagIds(tagIds);

        return mcpServerRepository.save(mcpServer);
    }
}
