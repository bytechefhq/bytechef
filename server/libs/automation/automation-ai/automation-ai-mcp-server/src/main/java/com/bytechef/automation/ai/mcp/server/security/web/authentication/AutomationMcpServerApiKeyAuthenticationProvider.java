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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Ivica Cardic
 */
public class AutomationMcpServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI")
    public AutomationMcpServerApiKeyAuthenticationProvider(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        AutomationMcpServerApiKeyAuthenticationToken automationMcpServerApiKeyAuthenticationToken =
            (AutomationMcpServerApiKeyAuthenticationToken) authentication;

        McpServer mcpServer = mcpServerService.getMcpServer(
            automationMcpServerApiKeyAuthenticationToken.getMcpServerSecretKey());

        if (!Objects.equals(
            mcpServer.getSecretKey(), automationMcpServerApiKeyAuthenticationToken.getMcpServerSecretKey())) {

            throw new BadCredentialsException("Invalid secret key");
        }

        return new AutomationMcpServerApiKeyAuthenticationToken(
            automationMcpServerApiKeyAuthenticationToken.getAuthSecretKey());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(AutomationMcpServerApiKeyAuthenticationToken.class);
    }
}
