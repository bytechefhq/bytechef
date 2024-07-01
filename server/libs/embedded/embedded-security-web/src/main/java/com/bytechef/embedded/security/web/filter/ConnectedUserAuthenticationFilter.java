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

package com.bytechef.embedded.security.web.filter;

import com.bytechef.embedded.security.web.util.AuthTokenUtils;
import com.bytechef.embedded.security.web.util.AuthTokenUtils.AuthToken;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.user.domain.SigningKey.TenantKeyId;
import com.bytechef.platform.user.service.SigningKeyService;
import com.bytechef.tenant.util.TenantUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Key;
import java.util.regex.Pattern;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Ivica Cardic
 */
public class ConnectedUserAuthenticationFilter extends OncePerRequestFilter {

    private static final Pattern PATH_PATTERN = Pattern.compile("^/api/embedded/by-connected-user-token/v1/([^/]+)");
    private static final RequestMatcher REQUEST_MATCHER = new NegatedRequestMatcher(
        new AntPathRequestMatcher("/api/embedded/by-connected-user-token/v1/**"));

    private final AuthenticationManager authenticationManager;
    private final SigningKeyService signingKeyService;

    @SuppressFBWarnings("EI")
    public ConnectedUserAuthenticationFilter(
        AuthenticationManager authenticationManager, SigningKeyService signingKeyService) {

        this.authenticationManager = authenticationManager;
        this.signingKeyService = signingKeyService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) {

        Authentication authentication = getAuthentication(httpServletRequest);

        String tenantId = ((ConnectedUserAuthenticationToken) authentication).getTenantId();

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
        AuthToken authToken = AuthTokenUtils.getAuthToken(PATH_PATTERN, request);

        Jws<Claims> jws = getJws(authToken);

        Claims payload = jws.getPayload();

        String externalUserId = payload.getSubject();

        JwsHeader header = jws.getHeader();

        TenantKeyId tenantKeyId = TenantKeyId.parse(header.getKeyId());

        return new ConnectedUserAuthenticationToken(authToken.environment(), externalUserId, tenantKeyId.getTenantId());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return REQUEST_MATCHER.matches(request);
    }

    private Jws<Claims> getJws(AuthToken authToken) {
        return Jwts.parser()
            .keyLocator(new SigningKeyLocator(authToken.environment()))
            .build()
            .parseSignedClaims(authToken.token());
    }

    private class SigningKeyLocator implements Locator<Key> {

        private final Environment environment;

        private SigningKeyLocator(Environment environment) {
            this.environment = environment;
        }

        @Override
        public Key locate(Header header) {
            String keyId = (String) header.get("kid");

            TenantKeyId tenantKeyId = TenantKeyId.parse(keyId);

            return TenantUtils.callWithTenantId(
                tenantKeyId.getTenantId(), () -> signingKeyService.getPublicKey(keyId, environment));
        }
    }
}
