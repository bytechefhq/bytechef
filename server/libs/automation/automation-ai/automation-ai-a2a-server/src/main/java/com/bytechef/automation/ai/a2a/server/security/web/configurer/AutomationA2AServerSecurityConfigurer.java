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

package com.bytechef.automation.ai.a2a.server.security.web.configurer;

import com.bytechef.automation.ai.a2a.server.security.web.authentication.A2aApiKeyAuthenticationConverter;
import com.bytechef.automation.ai.a2a.server.security.web.authentication.AutomationA2AServerApiKeyAuthenticationProvider;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.McpApiKeyHttpConfigurer;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;

/**
 * Registers secret-key API-key authentication for the automation A2A server endpoints, reusing the shared MCP API-key
 * transport plumbing with an A2A-specific path converter and per-server authentication provider.
 *
 * @author Ivica Cardic
 */
public class AutomationA2AServerSecurityConfigurer extends McpApiKeyHttpConfigurer {

    // Covers both the JSON-RPC endpoint (/api/automation/a2a/{secretKey}) and the agent-card endpoint
    // (/api/automation/a2a/{secretKey}/.well-known/agent-card.json), with an optional trailing query string.
    private static final String PATH_PATTERN = "^/api/automation/a2a/.+";

    public AutomationA2AServerSecurityConfigurer(
        ApiKeyService apiKeyService, AuthorityService authorityService, A2aServerService a2aServerService,
        UserService userService) {

        super(
            PATH_PATTERN, new A2aApiKeyAuthenticationConverter("/api/automation/a2a/"),
            new AutomationA2AServerApiKeyAuthenticationProvider(
                apiKeyService, authorityService, a2aServerService, userService));
    }
}
