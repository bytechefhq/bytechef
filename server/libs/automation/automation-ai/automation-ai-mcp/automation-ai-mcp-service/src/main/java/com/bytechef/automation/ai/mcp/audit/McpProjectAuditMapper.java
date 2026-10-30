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

package com.bytechef.automation.ai.mcp.audit;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class McpProjectAuditMapper implements AuditMapper {

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();
        String event = auditInvocation.event();

        if (event.equals(McpProjectAuditEvents.MCP_PROJECT_DELETED)) {
            data.put("mcpProjectId", String.valueOf(auditInvocation.requireArgument("mcpProjectId")));

            return data;
        }

        if (auditInvocation.result() instanceof McpProject mcpProject) {
            data.put("mcpProjectId", String.valueOf(mcpProject.getId()));
        }

        data.put("mcpServerId", String.valueOf(auditInvocation.requireArgument("mcpServerId")));
        data.put("projectId", String.valueOf(auditInvocation.requireArgument("projectId")));
        data.put("projectVersion", String.valueOf(auditInvocation.requireArgument("projectVersion")));

        return data;
    }
}
