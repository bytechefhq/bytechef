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

package com.bytechef.platform.mcp.audit;

import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class McpComponentAuditMapper implements AuditMapper {

    private final McpComponentRepository mcpComponentRepository;
    private final McpServerRepository mcpServerRepository;

    @SuppressFBWarnings("EI")
    public McpComponentAuditMapper(
        McpComponentRepository mcpComponentRepository, McpServerRepository mcpServerRepository) {

        this.mcpComponentRepository = mcpComponentRepository;
        this.mcpServerRepository = mcpServerRepository;
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        Long mcpServerId = getMcpServerId(auditInvocation);

        if (mcpServerId == null) {
            return null;
        }

        return mcpServerRepository.findById(mcpServerId)
            .map(McpServer::getType)
            .orElse(null);
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        return Map.of();
    }

    @Override
    public @Nullable PlatformType platform(AuditInvocation auditInvocation) {
        if (auditInvocation.captured() instanceof PlatformType platformType) {
            return platformType;
        }

        return null;
    }

    private @Nullable Long getMcpServerId(AuditInvocation auditInvocation) {
        Long mcpComponentId = null;

        if (auditInvocation.argument("mcpComponent") instanceof McpComponent mcpComponent) {
            mcpComponentId = mcpComponent.getId();

            if (mcpComponentId == null) {
                return mcpComponent.getMcpServerId();
            }
        } else if (auditInvocation.argument("mcpComponentId") instanceof Long argumentMcpComponentId) {
            mcpComponentId = argumentMcpComponentId;
        }

        if (mcpComponentId == null) {
            return null;
        }

        Optional<McpComponent> mcpComponentOptional = mcpComponentRepository.findById(mcpComponentId);

        return mcpComponentOptional.map(McpComponent::getMcpServerId)
            .orElse(null);
    }
}
