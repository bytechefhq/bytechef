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

import com.bytechef.platform.security.web.authentication.UnresolvedCredentialAuthenticationToken;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.security.server.apikey.web.ApiKeyAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * @author Ivica Cardic
 */
public class TenantAwareApiKeyAuthenticationFilter extends ApiKeyAuthenticationFilter {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CROSS_TENANT_API_KEY_ATTRIBUTE =
        TenantAwareApiKeyAuthenticationFilter.class.getName() + ".CROSS_TENANT_API_KEY";
    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private static final Logger log = LoggerFactory.getLogger(TenantAwareApiKeyAuthenticationFilter.class);

    private final @Nullable AuthenticationEventPublisher authenticationEventPublisher;
    private final Pattern mcpServerSecretPattern;
    private final @Nullable String tenantlessPath;

    public TenantAwareApiKeyAuthenticationFilter(
        RequestMatcher requestMatcher, Pattern mcpServerSecretPattern, @Nullable String tenantlessPath,
        AuthenticationManager authenticationManager, AuthenticationConverter authenticationConverter) {

        this(
            requestMatcher, mcpServerSecretPattern, tenantlessPath, authenticationManager, authenticationConverter,
            null);
    }

    public TenantAwareApiKeyAuthenticationFilter(
        RequestMatcher requestMatcher, Pattern mcpServerSecretPattern, @Nullable String tenantlessPath,
        AuthenticationManager authenticationManager, AuthenticationConverter authenticationConverter,
        @Nullable AuthenticationEventPublisher authenticationEventPublisher) {

        super(
            authenticationManager,
            new PathTenantAuthenticationConverter(authenticationConverter, authenticationEventPublisher));

        this.authenticationEventPublisher = authenticationEventPublisher;
        this.mcpServerSecretPattern = mcpServerSecretPattern;
        this.tenantlessPath = tenantlessPath;

        AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver =
            getAuthenticationManagerResolver();

        setAuthenticationManagerResolver(httpServletRequest -> new PublishingAuthenticationManager(
            authenticationManagerResolver.resolve(httpServletRequest), httpServletRequest,
            authenticationEventPublisher));
        setFailureHandler(
            new McpServerDisabledAuthenticationFailureHandler(
                createServiceUnavailableAwareFailureHandler(getFailureHandler())));
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
            BadCredentialsException badCredentialsException = new BadCredentialsException(
                "Invalid MCP server secret key");

            if (authorization != null || !isTenantlessPath(httpServletRequest)) {
                publishAuthenticationFailure(
                    authenticationEventPublisher, badCredentialsException, httpServletRequest);
            }

            AuthenticationFailureHandler authenticationFailureHandler = getFailureHandler();

            authenticationFailureHandler.onAuthenticationFailure(
                httpServletRequest, httpServletResponse, badCredentialsException);

            return;
        }

        TenantContext.runWithTenantId(
            tenantId, () -> super.doFilterInternal(httpServletRequest, httpServletResponse, filterChain));
    }

    private static AuthenticationFailureHandler createServiceUnavailableAwareFailureHandler(
        AuthenticationFailureHandler authenticationFailureHandler) {

        AuthenticationEntryPointFailureHandler serviceUnavailableFailureHandler =
            new AuthenticationEntryPointFailureHandler(new HttpStatusEntryPoint(HttpStatus.SERVICE_UNAVAILABLE));

        serviceUnavailableFailureHandler.setRethrowAuthenticationServiceException(false);

        return (httpServletRequest, httpServletResponse, authenticationException) -> {
            if (authenticationException instanceof AuthenticationServiceException) {
                serviceUnavailableFailureHandler.onAuthenticationFailure(
                    httpServletRequest, httpServletResponse, authenticationException);

                return;
            }

            authenticationFailureHandler.onAuthenticationFailure(
                httpServletRequest, httpServletResponse, authenticationException);
        };
    }

    private static AuthenticationServiceException toAuthenticationServiceException(
        RuntimeException runtimeException, HttpServletRequest httpServletRequest) {

        log.error(
            "Rejecting MCP API key for a {} request: authentication failed in tenant {}",
            httpServletRequest.getMethod(), TenantContext.getCurrentTenantId(), runtimeException);

        return new AuthenticationServiceException("MCP API key authentication failed", runtimeException);
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

    private static void publishAuthenticationFailure(
        @Nullable AuthenticationEventPublisher authenticationEventPublisher,
        AuthenticationException authenticationException, HttpServletRequest httpServletRequest) {

        if (authenticationEventPublisher == null) {
            return;
        }

        authenticationEventPublisher.publishAuthenticationFailure(
            authenticationException, new UnresolvedCredentialAuthenticationToken(httpServletRequest));
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
        private final @Nullable AuthenticationEventPublisher authenticationEventPublisher;

        private PathTenantAuthenticationConverter(
            AuthenticationConverter authenticationConverter,
            @Nullable AuthenticationEventPublisher authenticationEventPublisher) {

            this.authenticationConverter = authenticationConverter;
            this.authenticationEventPublisher = authenticationEventPublisher;
        }

        @Override
        @Nullable
        public Authentication convert(HttpServletRequest httpServletRequest) {
            try {
                return doConvert(httpServletRequest);
            } catch (AuthenticationException authenticationException) {
                publishAuthenticationFailure(authenticationEventPublisher, authenticationException, httpServletRequest);

                throw authenticationException;
            } catch (RuntimeException runtimeException) {
                AuthenticationServiceException authenticationServiceException = toAuthenticationServiceException(
                    runtimeException, httpServletRequest);

                publishAuthenticationFailure(
                    authenticationEventPublisher, authenticationServiceException, httpServletRequest);

                throw authenticationServiceException;
            }
        }

        @Nullable
        private Authentication doConvert(HttpServletRequest httpServletRequest) {
            String authorization = httpServletRequest.getHeader(AUTHORIZATION_HEADER_NAME);

            if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
                return authenticationConverter.convert(httpServletRequest);
            }

            String apiKeyTenantId = parseTenantId(authorization.substring(BEARER_PREFIX.length()));

            if (Objects.equals(apiKeyTenantId, TenantContext.getCurrentTenantId())) {
                return authenticationConverter.convert(httpServletRequest);
            }

            httpServletRequest.setAttribute(CROSS_TENANT_API_KEY_ATTRIBUTE, Boolean.TRUE);

            return authenticationConverter.convert(new WithoutAuthorizationHttpServletRequest(httpServletRequest));
        }
    }

    private static final class PublishingAuthenticationManager implements AuthenticationManager {

        private final AuthenticationManager authenticationManager;
        private final @Nullable AuthenticationEventPublisher authenticationEventPublisher;
        private final HttpServletRequest httpServletRequest;

        private PublishingAuthenticationManager(
            AuthenticationManager authenticationManager, HttpServletRequest httpServletRequest,
            @Nullable AuthenticationEventPublisher authenticationEventPublisher) {

            this.authenticationManager = authenticationManager;
            this.authenticationEventPublisher = authenticationEventPublisher;
            this.httpServletRequest = httpServletRequest;
        }

        @Override
        public Authentication authenticate(Authentication authentication) {
            Authentication authenticatedAuthentication;

            try {
                authenticatedAuthentication = authenticationManager.authenticate(authentication);
            } catch (AuthenticationException authenticationException) {
                throw authenticationException;
            } catch (RuntimeException runtimeException) {
                AuthenticationServiceException authenticationServiceException = toAuthenticationServiceException(
                    runtimeException, httpServletRequest);

                publishAuthenticationFailure(
                    authenticationEventPublisher, authenticationServiceException, httpServletRequest);

                throw authenticationServiceException;
            }

            if (httpServletRequest.getAttribute(CROSS_TENANT_API_KEY_ATTRIBUTE) != null) {
                publishAuthenticationFailure(
                    authenticationEventPublisher,
                    new BadCredentialsException("API key does not belong to the MCP server tenant"),
                    httpServletRequest);
            }

            return authenticatedAuthentication;
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
