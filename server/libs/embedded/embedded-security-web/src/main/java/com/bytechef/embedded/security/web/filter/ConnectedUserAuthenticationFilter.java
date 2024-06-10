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

import com.bytechef.embedded.configuration.service.ConnectedUserService;
import com.bytechef.embedded.user.service.SigningKeyService;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
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

    private static final String AUTH_TOKEN_HEADER_NAME = "Authorization";
    private static final Pattern PATH_PATTERN = Pattern.compile("^/api/embedded/by-user-token/([^/]+)");
    private static final RequestMatcher REQUEST_MATCHER = new NegatedRequestMatcher(
        new AntPathRequestMatcher("/api/embedded/by-user-token/**"));

    private final ConnectedUserService connectedUserService;
    private final SigningKeyService signingKeyService;

    @SuppressFBWarnings("EI")
    public ConnectedUserAuthenticationFilter(
        ConnectedUserService connectedUserService, SigningKeyService signingKeyService) {

        this.connectedUserService = connectedUserService;
        this.signingKeyService = signingKeyService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain)
        throws IOException, ServletException {

        Authentication authentication = getAuthentication(httpServletRequest);

        SecurityContext context = SecurityContextHolder.getContext();

        context.setAuthentication(authentication);

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    private Authentication getAuthentication(HttpServletRequest request) {
        String token = request.getHeader(AUTH_TOKEN_HEADER_NAME);

        if (token == null) {
            throw new BadCredentialsException("Authorization token does not exist");
        }

        Environment environment;

        Matcher matcher = PATH_PATTERN.matcher(request.getRequestURI());

        if (matcher.find()) {
            String group = matcher.group(1);

            environment = Environment.valueOf(group.toUpperCase());
        } else {
            throw new BadCredentialsException("Unknown environment");
        }

        String externalId = Jwts.parser()
            .verifyWith(signingKeyService.getPublicKey(environment))
            .build()
            .parseSignedClaims(token.replace("Bearer ", ""))
            .getPayload()
            .getSubject();

        if (!connectedUserService.hasConnectedUser(externalId, environment)) {
            connectedUserService.createConnectedUser(externalId, environment);
        }

        return new ConnectedUserAuthenticationToken(externalId, AuthorityUtils.NO_AUTHORITIES);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return REQUEST_MATCHER.matches(request);
    }
}
