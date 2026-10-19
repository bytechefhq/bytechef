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

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.security.server.apikey.web.ApiKeyAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * @author Ivica Cardic
 */
public class TenantAwareApiKeyAuthenticationFilter extends ApiKeyAuthenticationFilter {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private final Pattern mcpServerSecretPattern;
    private final @Nullable String tenantlessPath;

    public TenantAwareApiKeyAuthenticationFilter(
        RequestMatcher requestMatcher, Pattern mcpServerSecretPattern, @Nullable String tenantlessPath,
        AuthenticationManager authenticationManager, AuthenticationConverter authenticationConverter) {

        super(authenticationManager, new PathTenantAuthenticationConverter(authenticationConverter));

        this.mcpServerSecretPattern = mcpServerSecretPattern;
        this.tenantlessPath = tenantlessPath;

        setFailureHandler(new McpServerDisabledAuthenticationFailureHandler(getFailureHandler()));
        setRequestMatcher(requestMatcher);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain)
        throws ServletException, IOException {

        RequestMatcher requestMatcher = getRequestMatcher();

        if (!requestMatcher.matches(httpServletRequest)) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);

            return;
        }

        String authorization = httpServletRequest.getHeader(AUTHORIZATION_HEADER_NAME);

        if (authorization != null && authorization.startsWith(BEARER_PREFIX) &&
            parseTenantId(authorization.substring(BEARER_PREFIX.length())) == null) {

            filterChain.doFilter(httpServletRequest, httpServletResponse);

            return;
        }

        String tenantId = resolvePathTenantId(httpServletRequest);

        if (tenantId == null && isTenantlessPath(httpServletRequest) && authorization != null &&
            authorization.startsWith(BEARER_PREFIX)) {

            tenantId = parseTenantId(authorization.substring(BEARER_PREFIX.length()));
        }

        if (tenantId == null) {
            AuthenticationFailureHandler authenticationFailureHandler = getFailureHandler();

            authenticationFailureHandler.onAuthenticationFailure(
                httpServletRequest, httpServletResponse, new BadCredentialsException("Invalid MCP server secret key"));

            return;
        }

        TenantContext.runWithTenantId(
            tenantId, () -> super.doFilterInternal(httpServletRequest, httpServletResponse, filterChain));
    }

    private boolean isTenantlessPath(HttpServletRequest httpServletRequest) {
        return tenantlessPath != null && tenantlessPath.equals(httpServletRequest.getServletPath());
    }

    @Nullable
    private String resolvePathTenantId(HttpServletRequest httpServletRequest) {
        Matcher matcher = mcpServerSecretPattern.matcher(httpServletRequest.getServletPath());

        if (!matcher.matches()) {
            return null;
        }

        return parseTenantId(matcher.group(1));
    }

    @Nullable
    private static String parseTenantId(String tenantKeyString) {
        String tenantId;

        try {
            TenantKey tenantKey = TenantKey.parse(tenantKeyString);

            tenantId = tenantKey.getTenantId();
        } catch (RuntimeException runtimeException) {
            return null;
        }

        if (tenantId == null || !TENANT_ID_PATTERN.matcher(tenantId)
            .matches()) {

            return null;
        }

        return tenantId;
    }

    private static final class PathTenantAuthenticationConverter implements AuthenticationConverter {

        private final AuthenticationConverter authenticationConverter;

        private PathTenantAuthenticationConverter(AuthenticationConverter authenticationConverter) {
            this.authenticationConverter = authenticationConverter;
        }

        @Override
        @Nullable
        public Authentication convert(HttpServletRequest httpServletRequest) {
            String authorization = httpServletRequest.getHeader(AUTHORIZATION_HEADER_NAME);

            if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
                return authenticationConverter.convert(httpServletRequest);
            }

            String apiKeyTenantId = parseTenantId(authorization.substring(BEARER_PREFIX.length()));

            if (Objects.equals(apiKeyTenantId, TenantContext.getCurrentTenantId())) {
                return authenticationConverter.convert(httpServletRequest);
            }

            return authenticationConverter.convert(new WithoutAuthorizationHttpServletRequest(httpServletRequest));
        }
    }

    private static final class WithoutAuthorizationHttpServletRequest extends HttpServletRequestWrapper {

        private WithoutAuthorizationHttpServletRequest(HttpServletRequest httpServletRequest) {
            super(httpServletRequest);
        }

        @Override
        @Nullable
        public String getHeader(String name) {
            if (AUTHORIZATION_HEADER_NAME.equalsIgnoreCase(name)) {
                return null;
            }

            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (AUTHORIZATION_HEADER_NAME.equalsIgnoreCase(name)) {
                return Collections.emptyEnumeration();
            }

            return super.getHeaders(name);
        }
    }
}
