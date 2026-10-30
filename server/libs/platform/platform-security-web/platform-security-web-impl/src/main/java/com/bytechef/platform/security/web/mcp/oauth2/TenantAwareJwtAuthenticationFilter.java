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
import com.bytechef.platform.security.web.mcp.McpEndpoint;
import com.bytechef.platform.security.web.mcp.McpServerSecretVerifier;
import com.bytechef.platform.security.web.mcp.SelfIssuedTokenRevocationChecker;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Ivica Cardic
 */
public class TenantAwareJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareJwtAuthenticationFilter.class);

    private static final String SCOPE_AUTHORITY_PREFIX = "SCOPE_";

    private final McpAudienceValidator mcpAudienceValidator;
    private final McpEndpointRequestMatcher mcpEndpointRequestMatcher;
    private final McpJwtIdentityMapper mcpJwtIdentityMapper;
    private final McpResourceServerProperties mcpResourceServerProperties;
    private final List<McpServerSecretVerifier> mcpServerSecretVerifiers;
    private final @Nullable SelfIssuedTokenRevocationChecker selfIssuedTokenRevocationChecker;
    private final UserService userService;

    @SuppressFBWarnings("EI2")
    public TenantAwareJwtAuthenticationFilter(
        McpAudienceValidator mcpAudienceValidator, McpJwtIdentityMapper mcpJwtIdentityMapper,
        McpResourceServerProperties mcpResourceServerProperties, UserService userService,
        List<McpEndpoint> mcpEndpoints, List<McpServerSecretVerifier> mcpServerSecretVerifiers,
        @Nullable SelfIssuedTokenRevocationChecker selfIssuedTokenRevocationChecker) {

        this.mcpAudienceValidator = mcpAudienceValidator;
        this.mcpEndpointRequestMatcher = new McpEndpointRequestMatcher(mcpEndpoints);
        this.mcpJwtIdentityMapper = mcpJwtIdentityMapper;
        this.mcpResourceServerProperties = mcpResourceServerProperties;
        this.mcpServerSecretVerifiers = List.copyOf(mcpServerSecretVerifiers);
        this.selfIssuedTokenRevocationChecker = selfIssuedTokenRevocationChecker;
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

        Optional<McpEndpoint> mcpEndpoint = mcpEndpointRequestMatcher.findMcpEndpoint(request);

        if (mcpEndpoint.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        Jwt jwt = jwtAuthenticationToken.getToken();

        String issuerUri = String.valueOf(jwt.getIssuer());

        Optional<Issuer> staticIssuer = mcpResourceServerProperties.findIssuer(issuerUri);

        String tenantId;

        try {
            tenantId = resolveTenantId(request, mcpEndpoint.get(), jwt, staticIssuer.orElse(null));
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn(
                "Rejecting MCP JWT for {}: {}", request.getServletPath(), illegalArgumentException.getMessage(),
                illegalArgumentException);

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        } catch (IllegalStateException illegalStateException) {
            log.error(
                "Rejecting MCP JWT for {}: {}", request.getServletPath(), illegalStateException.getMessage(),
                illegalStateException);

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        boolean mcpServerSecretVerified;

        try {
            mcpServerSecretVerified = isMcpServerSecretVerified(request, tenantId);
        } catch (RuntimeException runtimeException) {
            log.error(
                "Rejecting MCP JWT for {}: MCP server secret verification failed in tenant {}",
                request.getServletPath(), tenantId, runtimeException);

            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            return;
        }

        if (!mcpServerSecretVerified) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        if (staticIssuer.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        McpJwtIdentity mcpJwtIdentity = authenticateStaticIssuer(
            request, response, jwtAuthenticationToken, jwt, staticIssuer.get(), mcpEndpoint.get(), tenantId);

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
        Jwt jwt, Issuer issuer, McpEndpoint mcpEndpoint, String tenantId) throws IOException {

        if (!hasScope(jwtAuthenticationToken, mcpEndpoint.getScope())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }

        if (!mcpAudienceValidator.isAudienceValid(jwt, issuer, buildRequestUrl(request))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }

        if (issuer.isSelf() && !isActiveByteChefUser(jwt.getSubject(), tenantId)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }

        if (issuer.isSelf()) {
            Integer revocationErrorStatus = getRevocationErrorStatus(jwt, tenantId);

            if (revocationErrorStatus != null) {
                response.sendError(revocationErrorStatus);

                return null;
            }
        }

        try {
            return mcpJwtIdentityMapper.map(jwt, issuer, jwtAuthenticationToken.getAuthorities(), tenantId);
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

    private static String resolveTenantId(
        HttpServletRequest request, McpEndpoint mcpEndpoint, Jwt jwt, @Nullable Issuer issuer) {

        Optional<String> pathTenantId = mcpEndpoint.resolveTenantId(McpEndpointRequestMatcher.getPath(request));

        return pathTenantId.orElseGet(() -> resolveClaimTenantId(jwt, issuer));
    }

    private static String resolveClaimTenantId(Jwt jwt, @Nullable Issuer issuer) {
        if (issuer == null || !issuer.isSelf()) {
            throw new IllegalArgumentException("Tenant-less MCP endpoint accepts only self-issued tokens");
        }

        String tenantClaim = issuer.getTenantClaim();

        if (StringUtils.isBlank(tenantClaim)) {
            throw new IllegalStateException("Self issuer has no tenant claim configured");
        }

        String claimTenantId = jwt.getClaimAsString(tenantClaim);

        if (StringUtils.isBlank(claimTenantId)) {
            throw new IllegalArgumentException("Token carries no tenant claim");
        }

        return claimTenantId;
    }

    private boolean isMcpServerSecretVerified(HttpServletRequest request, String tenantId) {
        return TenantContext.callWithTenantId(
            tenantId,
            () -> mcpServerSecretVerifiers.stream()
                .map(mcpServerSecretVerifier -> mcpServerSecretVerifier.verifyMcpServerSecret(request))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(false));
    }

    private boolean isActiveByteChefUser(String login, String tenantId) {
        return TenantContext.callWithTenantId(
            tenantId,
            () -> userService.fetchUserByLogin(login)
                .map(User::isActivated)
                .orElse(false));
    }

    @Nullable
    private Integer getRevocationErrorStatus(Jwt jwt, String tenantId) {
        if (selfIssuedTokenRevocationChecker == null) {
            log.error("Rejecting self-issued MCP token: no SelfIssuedTokenRevocationChecker is configured");

            return HttpServletResponse.SC_UNAUTHORIZED;
        }

        boolean revoked;

        try {
            revoked = selfIssuedTokenRevocationChecker.isRevoked(jwt.getTokenValue(), tenantId);
        } catch (RuntimeException runtimeException) {
            log.error(
                "Rejecting self-issued MCP token: revocation check failed for tenant {}", tenantId, runtimeException);

            return HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        }

        return revoked ? Integer.valueOf(HttpServletResponse.SC_UNAUTHORIZED) : null;
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
