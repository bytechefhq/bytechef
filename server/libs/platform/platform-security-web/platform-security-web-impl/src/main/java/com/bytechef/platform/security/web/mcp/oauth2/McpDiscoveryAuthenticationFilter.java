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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Predicate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Answers an unauthenticated MCP request (no {@code Authorization} header) with the OAuth2 protected-resource discovery
 * response - {@code 401} plus a {@code WWW-Authenticate: Bearer} header pointing at the RFC 9728 protected-resource
 * metadata - so a client can discover the authorization server. Runs early, ahead of the API-key filter, so it replaces
 * that filter's bare {@code 401} for the no-credentials case; it only acts when the resource server is enabled (the
 * contributor adds it only then). Requests that carry a credential pass through to the credential filters unchanged.
 *
 * <p>
 * The optional {@code authenticationRequiredPredicate} lets a per-server MCP endpoint opt a token-less request out of
 * the challenge: when the predicate returns {@code false} for the request (the resolved server does not require
 * authentication), the request falls through to the credential filters (which serve it anonymously) instead of
 * receiving the discovery {@code 401}. The default predicate always requires authentication, so existing callers keep
 * challenging every token-less request.
 *
 * @author Ivica Cardic
 */
public class McpDiscoveryAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final Predicate<HttpServletRequest> authenticationRequiredPredicate;
    private final RequestMatcher mcpRequestMatcher;

    public McpDiscoveryAuthenticationFilter(
        RequestMatcher mcpRequestMatcher, AuthenticationEntryPoint authenticationEntryPoint) {

        this(mcpRequestMatcher, authenticationEntryPoint, request -> true);
    }

    public McpDiscoveryAuthenticationFilter(
        RequestMatcher mcpRequestMatcher, AuthenticationEntryPoint authenticationEntryPoint,
        Predicate<HttpServletRequest> authenticationRequiredPredicate) {

        this.mcpRequestMatcher = mcpRequestMatcher;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authenticationRequiredPredicate = authenticationRequiredPredicate;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        if (mcpRequestMatcher.matches(request) && request.getHeader(AUTHORIZATION_HEADER_NAME) == null
            && authenticationRequiredPredicate.test(request)) {

            authenticationEntryPoint.commence(
                request, response, new InsufficientAuthenticationException("Authentication is required"));

            return;
        }

        filterChain.doFilter(request, response);
    }
}
