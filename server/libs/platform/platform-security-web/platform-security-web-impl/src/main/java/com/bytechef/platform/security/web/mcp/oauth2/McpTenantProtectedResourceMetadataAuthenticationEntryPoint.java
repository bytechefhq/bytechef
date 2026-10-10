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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Ivica Cardic
 */
public class McpTenantProtectedResourceMetadataAuthenticationEntryPoint implements McpAuthenticationEntryPoint {

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
