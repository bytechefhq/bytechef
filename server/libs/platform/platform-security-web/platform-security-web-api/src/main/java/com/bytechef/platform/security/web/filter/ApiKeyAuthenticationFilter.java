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

package com.bytechef.platform.security.web.filter;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.TenantWebAuthenticationDetails;
import com.bytechef.platform.security.web.authentication.UnresolvedCredentialAuthenticationToken;
import com.bytechef.platform.security.web.mcp.McpServerDisabledException;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A filter class for handling API authentication based on request headers and patterns. This class extends the
 * {@link OncePerRequestFilter} and provides methods to authenticate requests against a defined path pattern. The filter
 * supports tenant-based processing, allowing the execution of actions within a tenant's context. It integrates with an
 * {@link AuthenticationManager} to authenticate user credentials and set up the security context for authenticated
 * requests.
 *
 * @author Ivica Cardic
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
        .getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();
    private final AuthenticationSuccessHandler successHandler = new PassthroughSuccessHandler();
    private final AuthenticationFailureHandler failureHandler = new AuthenticationEntryPointFailureHandler(
        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
    private final AuthenticationFailureHandler serviceUnavailableFailureHandler =
        createServiceUnavailableFailureHandler();

    private final AuthenticationConverter authenticationConverter;
    private final @Nullable AuthenticationEventPublisher authenticationEventPublisher;
    private final AuthenticationManager authenticationManager;
    private final RequestMatcher requestMatcher;

    public ApiKeyAuthenticationFilter(
        RequestMatcher requestMatcher, AuthenticationConverter authenticationConverter,
        AuthenticationManager authenticationManager) {

        this(requestMatcher, authenticationConverter, authenticationManager, null);
    }

    public ApiKeyAuthenticationFilter(
        RequestMatcher requestMatcher, AuthenticationConverter authenticationConverter,
        AuthenticationManager authenticationManager,
        @Nullable AuthenticationEventPublisher authenticationEventPublisher) {

        this.authenticationConverter = authenticationConverter;
        this.authenticationEventPublisher = authenticationEventPublisher;
        this.authenticationManager = authenticationManager;
        this.requestMatcher = new NegatedRequestMatcher(requestMatcher);
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    protected void doFilterInternal(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain)
        throws ServletException, IOException {

        AbstractApiKeyAuthenticationToken authentication;

        try {
            authentication = (AbstractApiKeyAuthenticationToken) authenticationConverter.convert(httpServletRequest);
        } catch (AuthenticationException authenticationException) {
            if (authenticationEventPublisher != null) {
                authenticationEventPublisher.publishAuthenticationFailure(
                    authenticationException, new UnresolvedCredentialAuthenticationToken(httpServletRequest));
            }

            unsuccessfulAuthentication(httpServletRequest, httpServletResponse, authenticationException);

            return;
        } catch (RuntimeException runtimeException) {
            rejectServiceUnavailable(
                httpServletRequest, httpServletResponse,
                new UnresolvedCredentialAuthenticationToken(httpServletRequest),
                null, runtimeException);

            return;
        }

        if (authentication == null) {
            try {
                filterChain.doFilter(httpServletRequest, httpServletResponse);
            } catch (AuthenticationException authenticationException) {
                unsuccessfulAuthentication(httpServletRequest, httpServletResponse, authenticationException);
            }

            return;
        }

        HttpSession httpSession = httpServletRequest.getSession(false);

        if (httpSession != null) {
            httpServletRequest.changeSessionId();
        }

        authentication.setDetails(new TenantWebAuthenticationDetails(httpServletRequest, authentication.getTenantId()));

        TenantContext.runWithTenantId(
            authentication.getTenantId(),
            () -> authenticate(httpServletRequest, httpServletResponse, filterChain, authentication));
    }

    private void authenticate(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain,
        AbstractApiKeyAuthenticationToken authentication) throws IOException, ServletException {

        Authentication authenticatedAuthentication;

        try {
            authenticatedAuthentication = authenticationManager.authenticate(authentication);
        } catch (AuthenticationException authenticationException) {
            unsuccessfulAuthentication(httpServletRequest, httpServletResponse, authenticationException);

            return;
        } catch (RuntimeException runtimeException) {
            rejectServiceUnavailable(
                httpServletRequest, httpServletResponse, authentication, authentication.getTenantId(),
                runtimeException);

            return;
        }

        try {
            successfulAuthentication(
                httpServletRequest, httpServletResponse, filterChain, authenticatedAuthentication);
        } catch (AuthenticationException authenticationException) {
            unsuccessfulAuthentication(httpServletRequest, httpServletResponse, authenticationException);
        }
    }

    private void rejectServiceUnavailable(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication,
        @Nullable String tenantId, RuntimeException runtimeException) throws IOException, ServletException {

        log.error(
            "Rejecting API key for a {} request: authentication failed in tenant {}", httpServletRequest.getMethod(),
            tenantId, runtimeException);

        AuthenticationServiceException authenticationServiceException = new AuthenticationServiceException(
            "API key authentication failed", runtimeException);

        if (authenticationEventPublisher != null) {
            authenticationEventPublisher.publishAuthenticationFailure(authenticationServiceException, authentication);
        }

        securityContextHolderStrategy.clearContext();

        serviceUnavailableFailureHandler.onAuthenticationFailure(
            httpServletRequest, httpServletResponse, authenticationServiceException);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return requestMatcher.matches(request);
    }

    private void successfulAuthentication(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication)
        throws IOException, ServletException {

        SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();

        context.setAuthentication(authentication);

        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        successHandler.onAuthenticationSuccess(request, response, chain, authentication);
    }

    private void unsuccessfulAuthentication(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException failed)
        throws IOException, ServletException {

        securityContextHolderStrategy.clearContext();

        if (failed instanceof McpServerDisabledException mcpServerDisabledException) {
            mcpServerDisabledException.writeResponse(response);

            return;
        }

        failureHandler.onAuthenticationFailure(request, response, failed);
    }

    private static AuthenticationFailureHandler createServiceUnavailableFailureHandler() {
        AuthenticationEntryPointFailureHandler authenticationEntryPointFailureHandler =
            new AuthenticationEntryPointFailureHandler(new HttpStatusEntryPoint(HttpStatus.SERVICE_UNAVAILABLE));

        authenticationEntryPointFailureHandler.setRethrowAuthenticationServiceException(false);

        return authenticationEntryPointFailureHandler;
    }

    private static class PassthroughSuccessHandler implements AuthenticationSuccessHandler {

        @Override
        public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication)
            throws IOException, ServletException {

            chain.doFilter(request, response);
        }

        @Override
        public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

            throw new RuntimeException("Should never reach this");
        }
    }
}
