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
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springaicommunity.mcp.security.server.apikey.web.ApiKeyAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Wraps the mcp-security authentication filter so that authentication AND all downstream request processing run within
 * the tenant resolved from the presented API key ({@code ApiKey.secretKey} values are {@link TenantKey} strings).
 *
 * <p>
 * A Bearer token that is not a parseable {@link TenantKey} (for example an OAuth2 JWT) is not an API key: the filter
 * falls through so a downstream OAuth2 resource-server filter can authenticate it. If nothing authenticates, the
 * surrounding {@code /api/**} chain's {@code authenticated()} rule rejects the request — so falling through never
 * grants unauthenticated access.
 *
 * @author Ivica Cardic
 */
public class TenantAwareApiKeyAuthenticationFilter extends ApiKeyAuthenticationFilter {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public TenantAwareApiKeyAuthenticationFilter(
        RequestMatcher requestMatcher, AuthenticationManager authenticationManager,
        AuthenticationConverter authenticationConverter) {

        super(authenticationManager, authenticationConverter);

        setRequestMatcher(requestMatcher);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain)
        throws ServletException, IOException {

        String authorization = httpServletRequest.getHeader(AUTHORIZATION_HEADER_NAME);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            // no tenant to resolve; the converter yields a token with no api-key secret and the authentication
            // manager (the per-server provider) decides — rejecting with 401 when the server requires
            // authentication, or authenticating anonymously when it does not

            super.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

            return;
        }

        TenantKey tenantKey;

        try {
            tenantKey = TenantKey.parse(authorization.substring(BEARER_PREFIX.length()));
        } catch (Exception exception) {
            // Not a ByteChef API key (e.g. an OAuth2 JWT); let a downstream resource-server filter handle it.

            filterChain.doFilter(httpServletRequest, httpServletResponse);

            return;
        }

        TenantContext.runWithTenantId(
            tenantKey.getTenantId(),
            () -> super.doFilterInternal(httpServletRequest, httpServletResponse, filterChain));
    }
}
