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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Ivica Cardic
 */
public class McpAuthenticationGuardFilter extends OncePerRequestFilter {

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final List<Class<? extends Authentication>> mcpAuthenticationTypes;
    private final RequestMatcher mcpRequestMatcher;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
        SecurityContextHolder.getContextHolderStrategy();

    public McpAuthenticationGuardFilter(
        RequestMatcher mcpRequestMatcher, List<Class<? extends Authentication>> mcpAuthenticationTypes,
        AuthenticationEntryPoint authenticationEntryPoint) {

        this.authenticationEntryPoint = authenticationEntryPoint;
        this.mcpAuthenticationTypes = List.copyOf(mcpAuthenticationTypes);
        this.mcpRequestMatcher = mcpRequestMatcher;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        SecurityContext securityContext = securityContextHolderStrategy.getContext();

        if (isMcpAuthentication(securityContext.getAuthentication())) {
            filterChain.doFilter(request, response);

            return;
        }

        securityContextHolderStrategy.clearContext();

        authenticationEntryPoint.commence(
            request, response, new InsufficientAuthenticationException("MCP credentials are required"));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !mcpRequestMatcher.matches(request);
    }

    private boolean isMcpAuthentication(@Nullable Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return mcpAuthenticationTypes.stream()
            .anyMatch(mcpAuthenticationType -> mcpAuthenticationType.isInstance(authentication));
    }
}
