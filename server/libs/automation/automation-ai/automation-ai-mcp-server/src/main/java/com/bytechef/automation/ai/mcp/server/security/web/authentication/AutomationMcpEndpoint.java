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

package com.bytechef.automation.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.web.mcp.McpEndpoint;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The automation MCP server endpoint, served at {@code /api/automation/{secretKey}/mcp}.
 *
 * @author Ivica Cardic
 */
public class AutomationMcpEndpoint implements McpEndpoint {

    private static final Pattern PATH_PATTERN = Pattern.compile("^/api/automation/(.+)/mcp$");
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("/api/automation/([^/]+)/mcp/?$");
    private static final String SCOPE = "mcp:automation";

    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI2")
    public AutomationMcpEndpoint(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @Override
    public boolean matches(String path) {
        Matcher matcher = PATH_PATTERN.matcher(path);

        return matcher.matches();
    }

    @Override
    public String getScope() {
        return SCOPE;
    }

    @Override
    public Optional<String> resolveTenantId(String path) {
        Matcher matcher = PATH_PATTERN.matcher(path);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Automation MCP server secret key not found");
        }

        TenantKey tenantKey = TenantKey.parse(matcher.group(1));

        return Optional.of(tenantKey.getTenantId());
    }

    @Override
    public Optional<String> resolveResourceName(String resource) {
        Matcher matcher = RESOURCE_PATTERN.matcher(resource);

        if (!matcher.find()) {
            return Optional.empty();
        }

        McpServer mcpServer = mcpServerService.getMcpServer(matcher.group(1));

        return Optional.of(mcpServer.getName());
    }
}
