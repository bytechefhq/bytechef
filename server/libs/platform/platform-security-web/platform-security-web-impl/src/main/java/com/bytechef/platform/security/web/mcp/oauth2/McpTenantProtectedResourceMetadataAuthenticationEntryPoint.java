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

import com.bytechef.platform.security.web.mcp.McpAuthenticationEntryPoint;
import com.bytechef.platform.security.web.mcp.McpEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

/**
 * @author Ivica Cardic
 */
public class McpTenantProtectedResourceMetadataAuthenticationEntryPoint implements McpAuthenticationEntryPoint {

    public static final String INSUFFICIENT_SCOPE = "insufficient_scope";
    public static final String INVALID_TOKEN = "invalid_token";

    private static final String WELL_KNOWN_PREFIX = "/.well-known/oauth-protected-resource";

    private final McpEndpointRequestMatcher mcpEndpointRequestMatcher;
    private final @Nullable String publicUrl;

    @SuppressFBWarnings("EI2")
    public McpTenantProtectedResourceMetadataAuthenticationEntryPoint(
        McpEndpointRequestMatcher mcpEndpointRequestMatcher, @Nullable String publicUrl) {

        this.mcpEndpointRequestMatcher = mcpEndpointRequestMatcher;
        this.publicUrl = publicUrl;
    }

    @Override
    public void commence(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException) {

        String error = authenticationException instanceof OAuth2AuthenticationException ? INVALID_TOKEN : null;

        sendChallenge(request, response, HttpServletResponse.SC_UNAUTHORIZED, error);
    }

    public void sendChallenge(
        HttpServletRequest request, HttpServletResponse response, int status, @Nullable String error) {

        String pathWithinApplication = McpProtectedResourceUrls.getPathWithinApplication(request);

        StringBuilder challenge = new StringBuilder("Bearer ");

        if (error != null) {
            challenge.append("error=\"")
                .append(error)
                .append("\", ");
        }

        challenge.append("resource_metadata=\"")
            .append(
                McpProtectedResourceUrls.resolveResourceUrl(
                    publicUrl, McpProtectedResourceUrls.getRequestBaseUrl(request),
                    WELL_KNOWN_PREFIX + pathWithinApplication))
            .append('"');

        Optional<McpEndpoint> mcpEndpoint = mcpEndpointRequestMatcher.findMcpEndpoint(pathWithinApplication);

        if (mcpEndpoint.isPresent()) {
            McpEndpoint endpoint = mcpEndpoint.get();

            challenge.append(", scope=\"")
                .append(endpoint.getScope())
                .append('"');
        }

        response.setHeader("WWW-Authenticate", challenge.toString());
        response.setStatus(status);
    }
}
