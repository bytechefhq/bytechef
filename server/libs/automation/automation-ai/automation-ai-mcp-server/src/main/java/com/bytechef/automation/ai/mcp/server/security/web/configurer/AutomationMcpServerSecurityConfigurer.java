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

package com.bytechef.automation.ai.mcp.server.security.web.configurer;

import com.bytechef.automation.ai.mcp.server.security.web.authentication.AutomationMcpServerApiKeyAuthenticationProvider;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.McpApiKeyAuthenticationConverter;
import com.bytechef.platform.security.web.mcp.McpApiKeyHttpConfigurer;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;

/**
 * @author Ivica Cardic
 */
public class AutomationMcpServerSecurityConfigurer extends McpApiKeyHttpConfigurer {

    private static final String PATH_PATTERN = "^/api/automation/.+/mcp";

    public AutomationMcpServerSecurityConfigurer(
        ApiKeyService apiKeyService, AuthorityService authorityService, McpServerService mcpServerService,
        UserService userService) {

        super(
            PATH_PATTERN, new McpApiKeyAuthenticationConverter("/api/automation/"),
            new AutomationMcpServerApiKeyAuthenticationProvider(
                apiKeyService, authorityService, mcpServerService, userService));
    }
}
