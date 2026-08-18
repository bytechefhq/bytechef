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
import com.bytechef.platform.security.web.mcp.McpAuthenticationRequiredResolver;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reports whether the automation MCP server addressed by a request requires authentication, reading
 * {@code McpServer.authenticationRequired} from the {@code mcp_server} row selected by the URL path secret. Runs before
 * the request's tenant context exists, so the tenant is derived from the path secret (a {@code TenantKey}).
 *
 * @author Ivica Cardic
 */
public class AutomationMcpAuthenticationRequiredResolver implements McpAuthenticationRequiredResolver {

    private static final Pattern SECRET_KEY_PATH_PATTERN = Pattern.compile("/api/automation/(.+)/mcp");

    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI2")
    public AutomationMcpAuthenticationRequiredResolver(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @Override
    public Optional<Boolean> resolveAuthenticationRequired(HttpServletRequest request) {
        Matcher matcher = SECRET_KEY_PATH_PATTERN.matcher(request.getServletPath());

        if (!matcher.matches()) {
            return Optional.empty();
        }

        String mcpServerSecretKey = matcher.group(1);

        try {
            TenantKey tenantKey = TenantKey.parse(mcpServerSecretKey);

            McpServer mcpServer = TenantContext.callWithTenantId(
                tenantKey.getTenantId(), () -> mcpServerService.getMcpServer(mcpServerSecretKey));

            return Optional.of(mcpServer.isAuthenticationRequired());
        } catch (Exception exception) {
            // An unparseable secret or unknown server is challenged rather than served anonymously.

            return Optional.of(true);
        }
    }
}
