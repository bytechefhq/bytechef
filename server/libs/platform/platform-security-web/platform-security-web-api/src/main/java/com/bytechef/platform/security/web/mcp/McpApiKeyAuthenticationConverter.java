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

package com.bytechef.platform.security.web.mcp;

import com.bytechef.platform.configuration.domain.Environment;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * Converts an MCP request into an unauthenticated {@link ApiKeyAuthenticationToken}. The Bearer token is no longer
 * mandatory at this layer — a missing or malformed Authorization header yields a token whose credential
 * {@code secretKey} is {@code null}, a marker for "no token presented". The per-server authentication provider resolves
 * the target MCP server from the URL path secret and decides whether a token is actually required.
 *
 * @author Ivica Cardic
 */
public class McpApiKeyAuthenticationConverter implements AuthenticationConverter {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ENVIRONMENT_HEADER_NAME = "X-ENVIRONMENT";

    private final String pathPrefix;

    public McpApiKeyAuthenticationConverter(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER_NAME);

        String secretKey = null;

        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            secretKey = authorization.substring(BEARER_PREFIX.length());
        }

        String servletPath = request.getServletPath();

        String mcpServerSecretKey = servletPath.replace(pathPrefix, "")
            .replace("/mcp", "");

        return ApiKeyAuthenticationToken.unauthenticated(
            new McpApiKeyCredentials(getEnvironment(request), mcpServerSecretKey, secretKey));
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
