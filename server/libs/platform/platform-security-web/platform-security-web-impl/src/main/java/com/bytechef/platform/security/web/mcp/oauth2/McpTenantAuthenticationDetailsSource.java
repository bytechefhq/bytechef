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

import com.bytechef.platform.security.web.authentication.TenantWebAuthenticationDetails;
import com.bytechef.platform.security.web.mcp.McpEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationDetailsSource;

/**
 * @author Ivica Cardic
 */
public class McpTenantAuthenticationDetailsSource
    implements AuthenticationDetailsSource<HttpServletRequest, TenantWebAuthenticationDetails> {

    private static final Logger log = LoggerFactory.getLogger(McpTenantAuthenticationDetailsSource.class);

    private final McpEndpointRequestMatcher mcpEndpointRequestMatcher;

    @SuppressFBWarnings("EI2")
    public McpTenantAuthenticationDetailsSource(McpEndpointRequestMatcher mcpEndpointRequestMatcher) {
        this.mcpEndpointRequestMatcher = mcpEndpointRequestMatcher;
    }

    @Override
    public TenantWebAuthenticationDetails buildDetails(HttpServletRequest request) {
        return new TenantWebAuthenticationDetails(request, resolvePathTenantId(request));
    }

    private @Nullable String resolvePathTenantId(HttpServletRequest request) {
        try {
            Optional<McpEndpoint> mcpEndpoint = mcpEndpointRequestMatcher.findMcpEndpoint(request);

            return mcpEndpoint
                .flatMap(
                    endpoint -> endpoint.resolveTenantId(McpProtectedResourceUrls.getPathWithinApplication(request)))
                .orElse(null);
        } catch (RuntimeException runtimeException) {
            log.debug("Unable to resolve the MCP path tenant for {}", request.getRequestURI(), runtimeException);

            return null;
        }
    }
}
