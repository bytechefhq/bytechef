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

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentications;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link McpComponentService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpComponentServiceImpl implements McpComponentService {

    private final McpComponentRepository mcpComponentRepository;
    private final List<McpComponentConnectionUsageChecker> mcpComponentConnectionUsageCheckers;

    @SuppressFBWarnings("EI")
    public McpComponentServiceImpl(
        McpComponentRepository mcpComponentRepository,
        List<McpComponentConnectionUsageChecker> mcpComponentConnectionUsageCheckers) {

        this.mcpComponentRepository = mcpComponentRepository;
        this.mcpComponentConnectionUsageCheckers = mcpComponentConnectionUsageCheckers;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpComponent.mcpServerId, 'McpServer', 'MCP_EDIT')")
    public McpComponent create(McpComponent mcpComponent) {
        checkConnectionUsage(mcpComponent.getMcpServerId(), mcpComponent.getConnectionId());

        return mcpComponentRepository.save(mcpComponent);
    }

    @Override
    public McpComponent update(McpComponent mcpComponent) {
        McpComponent currentMcpComponent = OptionalUtils.get(mcpComponentRepository.findById(mcpComponent.getId()));

        checkConnectionUsage(currentMcpComponent.getMcpServerId(), mcpComponent.getConnectionId());

        currentMcpComponent.setConnectionId(mcpComponent.getConnectionId());
        currentMcpComponent.setVersion(mcpComponent.getVersion());

        return mcpComponentRepository.save(currentMcpComponent);
    }

    @Override
    public void delete(long mcpComponentId) {
        mcpComponentRepository.deleteById(mcpComponentId);
    }

    @Override
    public McpComponent getMcpComponent(long mcpComponentId) {
        return OptionalUtils.get(mcpComponentRepository.findById(mcpComponentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpComponent> getMcpComponentsByComponentName(String componentName) {
        return mcpComponentRepository.findAllByComponentName(componentName);
    }

    @Override
    public List<McpComponent> getMcpComponents() {
        if (ConnectedUserAuthentications.isConnectedUser()) {
            throw new AccessDeniedException("A connected user may not list every MCP component");
        }

        return mcpComponentRepository.findAll();
    }

    @Override
    public List<McpComponent> getMcpServerMcpComponents(long mcpServerId) {
        return mcpComponentRepository.findAllByMcpServerId(mcpServerId);
    }

    private void checkConnectionUsage(Long mcpServerId, Long connectionId) {
        if (connectionId == null) {
            return;
        }

        for (McpComponentConnectionUsageChecker mcpComponentConnectionUsageChecker : mcpComponentConnectionUsageCheckers) {

            mcpComponentConnectionUsageChecker.checkConnectionUsage(
                Objects.requireNonNull(mcpServerId, "mcpServerId"), connectionId);
        }
    }
}
