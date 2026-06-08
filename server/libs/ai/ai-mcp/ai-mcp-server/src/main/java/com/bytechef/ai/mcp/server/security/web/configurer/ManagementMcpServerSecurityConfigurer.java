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

package com.bytechef.ai.mcp.server.security.web.configurer;

import com.bytechef.ai.mcp.server.security.web.authentication.ManagementMcpServerApiKeyAuthenticationProvider;
import com.bytechef.ai.mcp.server.security.web.authentication.ManagementMcpServerApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.domain.TenantKey;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @author Ivica Cardic
 */
public class ManagementMcpServerSecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    private static final String PATH_PATTERN = "^/api/management/.+/mcp";

    public ManagementMcpServerSecurityConfigurer(
        ApiKeyService apiKeyService, AuthorityService authorityService, PropertyService propertyService,
        UserService userService) {

        super(
            PATH_PATTERN, new McpServerApiKeyAuthenticationConverter(),
            new ManagementMcpServerApiKeyAuthenticationProvider(apiKeyService, authorityService, propertyService,
                userService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(RegexRequestMatcher.regexMatcher(PATH_PATTERN));
    }

    private static class McpServerApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

        @Override
        public Authentication convert(HttpServletRequest request) {
            String authToken = fetchAuthToken(request);
            String servletPath = request.getServletPath();

            String mcpServerSecretKey = servletPath.replace("/api/management/", "")
                .replace("/mcp", "");

            TenantKey tenantKey = TenantKey.parse(mcpServerSecretKey);

            return new ManagementMcpServerApiKeyAuthenticationToken(mcpServerSecretKey, authToken,
                tenantKey.getTenantId());
        }
    }
}
