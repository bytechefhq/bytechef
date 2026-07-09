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
 * Runs after the OAuth2 resource-server filter has validated a Bearer JWT. It enforces the per-endpoint scope
 * ({@code mcp:automation} on the automation endpoint, {@code mcp:management} on the management endpoint), enforces the
 * token's audience is bound to the endpoint being called (RFC 8707, via {@link McpAudienceValidator}), derives the
 * ByteChef identity from the token's claims, installs it as the request's authentication, and runs the remainder of the
 * chain (including the MCP request handling that reads tenant-scoped repositories) within the token's tenant - the JWT
 * analogue of {@code TenantAwareApiKeyAuthenticationFilter}.
 *
 * <p>
 * Requests that were not authenticated with a JWT (API-key requests, or unauthenticated requests) are passed through
 * untouched, so this filter never affects the Phase 1 API-key path.
 *
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
    private final @Nullable McpFederatedIssuerAuthenticator federatedIssuerAuthenticator;
    private final McpAudienceValidator mcpAudienceValidator;
    private final McpJwtIdentityMapper mcpJwtIdentityMapper;
    private final McpResourceServerProperties mcpResourceServerProperties;
    private final UserService userService;

    @SuppressFBWarnings("EI2")
    public TenantAwareJwtAuthenticationFilter(
        McpAudienceValidator mcpAudienceValidator, McpJwtIdentityMapper mcpJwtIdentityMapper,
        McpResourceServerProperties mcpResourceServerProperties, UserService userService,
        @Nullable McpFederatedIssuerAuthenticator federatedIssuerAuthenticator) {

        this.federatedIssuerAuthenticator = federatedIssuerAuthenticator;
        this.mcpAudienceValidator = mcpAudienceValidator;
        this.mcpJwtIdentityMapper = mcpJwtIdentityMapper;
        this.mcpResourceServerProperties = mcpResourceServerProperties;
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

        Optional<Issuer> staticIssuer = mcpResourceServerProperties.findIssuer(issuerUri);

        McpJwtIdentity mcpJwtIdentity;

        if (staticIssuer.isPresent()) {
            mcpJwtIdentity = authenticateStaticIssuer(
                request, response, jwtAuthenticationToken, jwt, staticIssuer.get(), urlTenantId);
        } else {
            Optional<McpJwtIdentity> federatedIdentity = federatedIssuerAuthenticator == null
                ? Optional.empty()
                : federatedIssuerAuthenticator.authenticate(jwt, request, urlTenantId);

            if (federatedIdentity.isEmpty()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

                return;
            }

            mcpJwtIdentity = federatedIdentity.get();
        }

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

    /**
     * Strict path for a statically configured issuer (the embedded authorization server or a static external IdP):
     * enforces the per-endpoint scope, mandatory audience binding, and - for the embedded server - per-request user
     * revocation. Returns {@code null} after sending a 401 when any check fails.
     */
    @Nullable
    private McpJwtIdentity authenticateStaticIssuer(
        HttpServletRequest request, HttpServletResponse response, JwtAuthenticationToken jwtAuthenticationToken,
        Jwt jwt, Issuer issuer, String urlTenantId) throws IOException {

        String requiredScope = getRequiredScope(request);

        if (requiredScope != null && !hasScope(jwtAuthenticationToken, requiredScope)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }

        if (!mcpAudienceValidator.isAudienceValid(jwt, issuer, buildRequestUrl(request))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }

        // For tokens minted by the ByteChef embedded authorization server (self), the subject is a ByteChef user;
        // re-check per request that the user still exists and is active, so disabling a user revokes access before the
        // token expires. External IdP tokens are not ByteChef users (revocation is the IdP's concern) and are skipped.
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

    /**
     * The authoritative tenant, parsed from the MCP endpoint's URL path secret (a per-tenant {@link TenantKey} on both
     * the automation and management endpoints). The external IdP token is thus treated as identity-only - it never
     * asserts the tenant. Throws if the secret is not a parseable {@link TenantKey}, so the request is rejected.
     */
    private String resolveUrlTenantId(HttpServletRequest request) {
        Matcher matcher = MCP_SECRET_PATTERN.matcher(request.getRequestURI());

        if (!matcher.find()) {
            throw new IllegalArgumentException("MCP endpoint secret not found");
        }

        return TenantKey.parse(matcher.group(1))
            .getTenantId();
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
