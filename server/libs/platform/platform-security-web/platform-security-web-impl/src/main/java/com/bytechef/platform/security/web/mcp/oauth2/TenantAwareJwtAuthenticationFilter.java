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

import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import com.bytechef.platform.security.web.mcp.McpServerSecretVerifier;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Ivica Cardic
 */
public class TenantAwareJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTOMATION_SCOPE = "mcp:automation";
    private static final String MANAGEMENT_SCOPE = "mcp:management";
    private static final Pattern MCP_SECRET_PATTERN = Pattern.compile("/api/(?:automation|management)/(.+)/mcp");
    private static final String SCOPE_AUTHORITY_PREFIX = "SCOPE_";

    private final RequestMatcher automationRequestMatcher =
        RegexRequestMatcher.regexMatcher("^/api/automation/.+/mcp");
    private final RequestMatcher managementRequestMatcher =
        RegexRequestMatcher.regexMatcher("^/api/management/.+/mcp");
    private final McpAudienceValidator mcpAudienceValidator;
    private final McpJwtIdentityMapper mcpJwtIdentityMapper;
    private final McpResourceServerProperties mcpResourceServerProperties;
    private final List<McpServerSecretVerifier> mcpServerSecretVerifiers;
    private final UserService userService;

    @SuppressFBWarnings("EI2")
    public TenantAwareJwtAuthenticationFilter(
        McpAudienceValidator mcpAudienceValidator, McpJwtIdentityMapper mcpJwtIdentityMapper,
        McpResourceServerProperties mcpResourceServerProperties, UserService userService,
        List<McpServerSecretVerifier> mcpServerSecretVerifiers) {

        this.mcpAudienceValidator = mcpAudienceValidator;
        this.mcpJwtIdentityMapper = mcpJwtIdentityMapper;
        this.mcpResourceServerProperties = mcpResourceServerProperties;
        this.mcpServerSecretVerifiers = List.copyOf(mcpServerSecretVerifiers);
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        JwtAuthenticationToken jwtAuthenticationToken = getJwtAuthenticationToken();

        if (jwtAuthenticationToken == null) {
            filterChain.doFilter(request, response);

            return;
        }

        Jwt jwt = jwtAuthenticationToken.getToken();

        String issuerUri = String.valueOf(jwt.getIssuer());

        String urlTenantId;

        try {
            urlTenantId = resolveUrlTenantId(request);
        } catch (RuntimeException runtimeException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        if (!isMcpServerSecretVerified(request, urlTenantId)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        Optional<Issuer> staticIssuer = mcpResourceServerProperties.findIssuer(issuerUri);

        if (staticIssuer.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        McpJwtIdentity mcpJwtIdentity = authenticateStaticIssuer(
            request, response, jwtAuthenticationToken, jwt, staticIssuer.get(), urlTenantId);

        if (mcpJwtIdentity == null) {
            return;
        }

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
            jwt, mcpJwtIdentity.authorities(), mcpJwtIdentity.login());

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(authentication);

        TenantContext.runWithTenantId(
            mcpJwtIdentity.tenantId(), () -> filterChain.doFilter(request, response));
    }

    @Nullable
    private McpJwtIdentity authenticateStaticIssuer(
        HttpServletRequest request, HttpServletResponse response, JwtAuthenticationToken jwtAuthenticationToken,
        Jwt jwt, Issuer issuer, String urlTenantId) throws IOException {

        String requiredScope = getRequiredScope(request);

        if (requiredScope == null || !hasScope(jwtAuthenticationToken, requiredScope)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }

        if (!mcpAudienceValidator.isAudienceValid(jwt, issuer, buildRequestUrl(request))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }

        if (issuer.isSelf() && !isActiveByteChefUser(jwt.getSubject(), urlTenantId)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }

        try {
            return mcpJwtIdentityMapper.map(jwt, issuer, jwtAuthenticationToken.getAuthorities(), urlTenantId);
        } catch (OAuth2AuthenticationException oAuth2AuthenticationException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }
    }

    private static String buildRequestUrl(HttpServletRequest request) {
        String url = UrlUtils.buildFullRequestUrl(request);

        int queryIndex = url.indexOf('?');

        return queryIndex >= 0 ? url.substring(0, queryIndex) : url;
    }

    private String resolveUrlTenantId(HttpServletRequest request) {
        Matcher matcher = MCP_SECRET_PATTERN.matcher(request.getRequestURI());

        if (!matcher.find()) {
            throw new IllegalArgumentException("MCP endpoint secret not found");
        }

        return TenantKey.parse(matcher.group(1))
            .getTenantId();
    }

    private boolean isMcpServerSecretVerified(HttpServletRequest request, String urlTenantId) {
        try {
            return TenantContext.callWithTenantId(
                urlTenantId,
                () -> mcpServerSecretVerifiers.stream()
                    .map(mcpServerSecretVerifier -> mcpServerSecretVerifier.verifyMcpServerSecret(request))
                    .flatMap(Optional::stream)
                    .findFirst()
                    .orElse(false));
        } catch (RuntimeException runtimeException) {
            return false;
        }
    }

    private boolean isActiveByteChefUser(String login, String tenantId) {
        return TenantContext.callWithTenantId(
            tenantId,
            () -> userService.fetchUserByLogin(login)
                .map(User::isActivated)
                .orElse(false));
    }

    @Nullable
    private String getRequiredScope(HttpServletRequest request) {
        if (automationRequestMatcher.matches(request)) {
            return AUTOMATION_SCOPE;
        }

        if (managementRequestMatcher.matches(request)) {
            return MANAGEMENT_SCOPE;
        }

        return null;
    }

    @Nullable
    private static JwtAuthenticationToken getJwtAuthenticationToken() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken;
        }

        return null;
    }

    private static boolean hasScope(JwtAuthenticationToken jwtAuthenticationToken, String scope) {
        return jwtAuthenticationToken.getAuthorities()
            .contains(new SimpleGrantedAuthority(SCOPE_AUTHORITY_PREFIX + scope));
    }
}
