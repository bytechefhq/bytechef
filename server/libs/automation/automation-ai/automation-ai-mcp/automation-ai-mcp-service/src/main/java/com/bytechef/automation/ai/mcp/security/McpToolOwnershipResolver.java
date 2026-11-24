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

package com.bytechef.automation.ai.mcp.security;

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpToolService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class McpToolOwnershipResolver implements ResourceOwnershipResolver {

    private final McpComponentService mcpComponentService;
    private final McpToolService mcpToolService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public McpToolOwnershipResolver(
        McpComponentService mcpComponentService, McpToolService mcpToolService,
        WorkspaceMcpServerService workspaceMcpServerService) {

        this.mcpComponentService = mcpComponentService;
        this.mcpToolService = mcpToolService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    public String resourceType() {
        return "McpTool";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return mcpToolService.fetchMcpToolMcpComponentId(id)
            .flatMap(mcpComponentService::fetchMcpComponentMcpServerId)
            .flatMap(workspaceMcpServerService::fetchMcpServerWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
