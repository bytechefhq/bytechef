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
import com.bytechef.automation.ai.mcp.server.security.web.authentication.AutomationMcpServerApiKeyAuthenticationToken;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.tenant.domain.TenantKey;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @author Ivica Cardic
 */
public class AutomationMcpServerSecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    private static final String PATH_PATTERN = "^/api/automation/.+/mcp";

    public AutomationMcpServerSecurityConfigurer(McpServerService mcpServerService) {
        super(
            PATH_PATTERN, new AutomationMcpServerApiKeyAuthenticationConverter(),
            new AutomationMcpServerApiKeyAuthenticationProvider(mcpServerService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(RegexRequestMatcher.regexMatcher(PATH_PATTERN));
    }

    private static class AutomationMcpServerApiKeyAuthenticationConverter
        extends AbstractApiKeyAuthenticationConverter {

        @Override
        public Authentication convert(HttpServletRequest request) {
            String authToken = fetchAuthToken(request);
            String servletPath = request.getServletPath();

            String mcpServerSecretKey = servletPath.replace("/api/automation/", "")
                .replace("/mcp", "");

            TenantKey tenantKey = TenantKey.parse(mcpServerSecretKey);

            return new AutomationMcpServerApiKeyAuthenticationToken(
                mcpServerSecretKey, authToken, tenantKey.getTenantId());
        }
    }
}
