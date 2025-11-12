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
import com.bytechef.tenant.util.TenantUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
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
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
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

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
        .getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();
    private final AuthenticationSuccessHandler successHandler = new PassthroughSuccessHandler();
    private final AuthenticationFailureHandler failureHandler = new AuthenticationEntryPointFailureHandler(
        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

    private final AuthenticationConverter authenticationConverter;
    private final AuthenticationManager authenticationManager;
    private final RequestMatcher requestMatcher;

    public ApiKeyAuthenticationFilter(
        String pathPatternRegex, AuthenticationConverter authenticationConverter,
        AuthenticationManager authenticationManager) {

        this.authenticationConverter = authenticationConverter;
        this.authenticationManager = authenticationManager;
        this.requestMatcher = new NegatedRequestMatcher(RegexRequestMatcher.regexMatcher(pathPatternRegex));
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain)
        throws ServletException, IOException {

        try {
            AbstractApiKeyAuthenticationToken authentication =
                (AbstractApiKeyAuthenticationToken) authenticationConverter.convert(httpServletRequest);

            if (authentication == null) {
                filterChain.doFilter(httpServletRequest, httpServletResponse);
            } else {
                HttpSession httpSession = httpServletRequest.getSession(false);

                if (httpSession != null) {
                    httpServletRequest.changeSessionId();
                }

                TenantUtils.runWithTenantId(
                    authentication.getTenantId(),
                    () -> {
                        Authentication authenticatedAuthentication = authenticationManager.authenticate(authentication);

                        successfulAuthentication(httpServletRequest, httpServletResponse, filterChain,
                            authenticatedAuthentication);
                    });
            }
        } catch (AuthenticationException ex) {
            unsuccessfulAuthentication(httpServletRequest, httpServletResponse, ex);
        }
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
        failureHandler.onAuthenticationFailure(request, response, failed);
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
