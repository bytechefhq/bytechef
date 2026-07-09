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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Answers an unauthenticated automation/management MCP request with the RFC 9728 discovery challenge: {@code 401} plus
 * a {@code WWW-Authenticate: Bearer resource_metadata="..."} header pointing at this endpoint's protected-resource
 * metadata. The pointer is built per request by inserting {@code /.well-known/oauth-protected-resource} ahead of the
 * request path, so each tenant's {@code /api/{automation|management}/{secret}/mcp} resource advertises its own metadata
 * document (which names that tenant's identity providers) - the "your IdP runs the show" external-direct discovery
 * path.
 *
 * <p>
 * <strong>Base / CE seam:</strong> this challenge is federation-neutral - the pointer is pure per-request path
 * insertion with no tenant or identity-provider logic. It is the base resource server's discovery entry point and is
 * designed to move to CE alongside the discovery filter and the static metadata chain. The federation-specific
 * discovery pieces - the per-endpoint, tenant-aware metadata <em>document</em> and the chain that serves it
 * ({@code McpTenantProtectedResourceMetadataController}, {@code McpTenantProtectedResourceMetadataResolver}, and the
 * dedicated tenant metadata {@code SecurityFilterChain}) - are already separate enterprise-only classes and stay EE. No
 * SPI is needed here: on a CE-only build this same challenge points at the static metadata document served by the base
 * chain.
 *
 * @author Ivica Cardic
 */
public class McpTenantProtectedResourceMetadataAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String WELL_KNOWN_PREFIX = "/.well-known/oauth-protected-resource";

    @Override
    public void commence(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException) {

        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        String pathWithinApplication = requestUri.substring(contextPath.length());

        String metadataUrl = UriComponentsBuilder.fromUriString(UrlUtils.buildFullRequestUrl(request))
            .replacePath(contextPath + WELL_KNOWN_PREFIX + pathWithinApplication)
            .replaceQuery(null)
            .fragment(null)
            .build()
            .toUriString();

        response.setHeader("WWW-Authenticate", "Bearer resource_metadata=\"" + metadataUrl + "\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
