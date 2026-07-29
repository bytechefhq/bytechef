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

package com.bytechef.automation.ai.a2a.server.security.web.authentication;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.mcp.McpApiKeyCredentials;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * Converts an A2A request into an unauthenticated {@link ApiKeyAuthenticationToken}. The A2A server secret key is the
 * first path segment after {@code /api/automation/a2a/} (the card path adds a {@code /.well-known/agent-card.json}
 * suffix; the JSON-RPC path is the bare secret). The Bearer token is optional — a missing one yields a {@code null}
 * credential secret, and the per-server provider decides whether a token is actually required.
 *
 * <p>
 * Reuses {@link McpApiKeyCredentials} as the generic transport-security credential holder — its "server secret key"
 * slot carries the A2A server's secret; the A2A stack keeps its own registration domain but not its own copy of the
 * shared security plumbing.
 * </p>
 *
 * @author Ivica Cardic
 */
public class A2aApiKeyAuthenticationConverter implements AuthenticationConverter {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ENVIRONMENT_HEADER_NAME = "X-ENVIRONMENT";

    private final String pathPrefix;

    public A2aApiKeyAuthenticationConverter(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER_NAME);

        String secretKey = null;

        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            secretKey = authorization.substring(BEARER_PREFIX.length());
        }

        return ApiKeyAuthenticationToken.unauthenticated(
            new McpApiKeyCredentials(getEnvironment(request), extractA2aServerSecretKey(request), secretKey));
    }

    private String extractA2aServerSecretKey(HttpServletRequest request) {
        String servletPath = request.getServletPath();

        int prefixIndex = servletPath.indexOf(pathPrefix);

        if (prefixIndex < 0) {
            return "";
        }

        String remainder = servletPath.substring(prefixIndex + pathPrefix.length());

        int slashIndex = remainder.indexOf('/');

        return slashIndex >= 0 ? remainder.substring(0, slashIndex) : remainder;
    }

    private Environment getEnvironment(HttpServletRequest request) {
        String environment = request.getHeader(ENVIRONMENT_HEADER_NAME);

        if (StringUtils.isBlank(environment)) {
            return Environment.PRODUCTION;
        }

        try {
            return Environment.valueOf(environment.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new BadCredentialsException("Invalid X-ENVIRONMENT header", illegalArgumentException);
        }
    }
}
