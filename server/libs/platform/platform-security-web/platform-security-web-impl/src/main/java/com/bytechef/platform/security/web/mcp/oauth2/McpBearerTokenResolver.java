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

package com.bytechef.platform.security.web.mcp.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Resolves the Bearer token for the MCP resource server, scoping it so the OAuth2 resource-server filter only claims
 * tokens it owns. A token is resolved only when the request targets an MCP endpoint AND the token is a JWT. ByteChef
 * API keys (opaque base64 secrets, never containing a {@code '.'}) are left to the API-key filter, and non-MCP requests
 * are left to the surrounding application chain. This mirrors {@code TenantAwareApiKeyAuthenticationFilter}'s
 * fall-through so the two credential filters partition the Bearer space without overlap.
 *
 * @author Ivica Cardic
 */
public class McpBearerTokenResolver implements BearerTokenResolver {

    private final BearerTokenResolver delegate = new DefaultBearerTokenResolver();
    private final RequestMatcher mcpRequestMatcher;

    public McpBearerTokenResolver(RequestMatcher mcpRequestMatcher) {
        this.mcpRequestMatcher = mcpRequestMatcher;
    }

    @Override
    @Nullable
    public String resolve(HttpServletRequest request) {
        if (!mcpRequestMatcher.matches(request)) {
            return null;
        }

        String token = delegate.resolve(request);

        if (token == null || !isJwt(token)) {
            return null;
        }

        return token;
    }

    private static boolean isJwt(String token) {
        return token.indexOf('.') >= 0;
    }
}
