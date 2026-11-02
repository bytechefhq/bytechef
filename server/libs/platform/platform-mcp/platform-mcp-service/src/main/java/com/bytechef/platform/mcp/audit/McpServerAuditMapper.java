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
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class McpServerAuditMapper implements AuditMapper {

    private final McpServerRepository mcpServerRepository;

    @SuppressFBWarnings("EI")
    public McpServerAuditMapper(McpServerRepository mcpServerRepository) {
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
        if (auditInvocation.result() instanceof McpServer mcpServer) {
            return mcpServer.getType();
        }

        if (auditInvocation.captured() instanceof PlatformType platformType) {
            return platformType;
        }

        return null;
    }

    private static @Nullable Long getMcpServerId(AuditInvocation auditInvocation) {
        if (auditInvocation.argument("mcpServerId") instanceof Long mcpServerId) {
            return mcpServerId;
        }

        if (auditInvocation.argument("id") instanceof Long id) {
            return id;
        }

        if (auditInvocation.argument("mcpServer") instanceof McpServer mcpServer) {
            return mcpServer.getId();
        }

        return null;
    }
}
