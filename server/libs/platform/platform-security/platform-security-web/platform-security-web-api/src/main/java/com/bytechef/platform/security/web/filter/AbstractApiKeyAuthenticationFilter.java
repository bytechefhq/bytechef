/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.platform.security.web.authentication.ApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.util.AuthTokenUtils;
import com.bytechef.platform.security.web.util.AuthTokenUtils.AuthToken;
import com.bytechef.tenant.TenantKey;
import com.bytechef.tenant.util.TenantUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final Pattern pathPattern;
    private final RequestMatcher requestMatcher;

    private final AuthenticationManager authenticationManager;

    @SuppressFBWarnings("EI")
    public AbstractApiKeyAuthenticationFilter(
        String pathRegex, RequestMatcher requestMatcher, AuthenticationManager authenticationManager) {

        this.authenticationManager = authenticationManager;
        this.pathPattern = Pattern.compile(pathRegex);
        this.requestMatcher = new NegatedRequestMatcher(requestMatcher);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) {

        Authentication authentication = getAuthentication(httpServletRequest);

        String tenantId = ((ApiKeyAuthenticationToken) authentication).getTenantId();

        TenantUtils.runWithTenantId(
            tenantId,
            () -> {
                Authentication authenticatedAuthentication = authenticationManager.authenticate(authentication);

                SecurityContext context = SecurityContextHolder.getContext();

                context.setAuthentication(authenticatedAuthentication);

                filterChain.doFilter(httpServletRequest, httpServletResponse);
            });
    }

    private Authentication getAuthentication(HttpServletRequest request) {
        AuthToken authToken = AuthTokenUtils.getAuthToken(pathPattern, request);

        TenantKey tenantKey = TenantKey.parse(authToken.token());

        return new ApiKeyAuthenticationToken(authToken.environment(), authToken.token(), tenantKey.getTenantId());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return requestMatcher.matches(request);
    }
}
