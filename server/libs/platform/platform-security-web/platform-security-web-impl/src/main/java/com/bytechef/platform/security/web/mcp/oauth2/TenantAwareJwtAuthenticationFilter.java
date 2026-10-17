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
import com.bytechef.platform.security.web.mcp.SelfIssuedTokenRevocationChecker;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Ivica Cardic
 */
public class TenantAwareJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareJwtAuthenticationFilter.class);

    private static final String AUTOMATION_SCOPE = "mcp:automation";
    private static final Pattern CANONICAL_MANAGEMENT_PATTERN = Pattern.compile("^/api/management/mcp$");
    private static final String MANAGEMENT_SCOPE = "mcp:management";
    private static final Pattern MCP_SECRET_PATTERN = Pattern.compile("/api/(?:automation|management)/(.+)/mcp");
    private static final String SCOPE_AUTHORITY_PREFIX = "SCOPE_";

    private final RequestMatcher automationRequestMatcher =
        RegexRequestMatcher.regexMatcher("^/api/automation/.+/mcp");
    private final RequestMatcher managementRequestMatcher =
        RegexRequestMatcher.regexMatcher("^/api/management(/[^/]+)?/mcp$");
    private final McpAudienceValidator mcpAudienceValidator;
    private final McpTenantProtectedResourceMetadataAuthenticationEntryPoint mcpAuthenticationEntryPoint;
    private final McpJwtIdentityMapper mcpJwtIdentityMapper;
    private final McpResourceServerProperties mcpResourceServerProperties;
    private final List<McpServerSecretVerifier> mcpServerSecretVerifiers;
    private final @Nullable String publicUrl;
    private final @Nullable SelfIssuedTokenRevocationChecker selfIssuedTokenRevocationChecker;
    private final AuthorityService authorityService;
    private final UserService userService;

    @SuppressFBWarnings("EI2")
    public TenantAwareJwtAuthenticationFilter(
        McpAudienceValidator mcpAudienceValidator, McpJwtIdentityMapper mcpJwtIdentityMapper,
        McpResourceServerProperties mcpResourceServerProperties, UserService userService,
        AuthorityService authorityService, List<McpServerSecretVerifier> mcpServerSecretVerifiers,
        @Nullable SelfIssuedTokenRevocationChecker selfIssuedTokenRevocationChecker, @Nullable String publicUrl) {

        this.authorityService = authorityService;
        this.mcpAudienceValidator = mcpAudienceValidator;
        this.mcpAuthenticationEntryPoint = new McpTenantProtectedResourceMetadataAuthenticationEntryPoint(publicUrl);
        this.mcpJwtIdentityMapper = mcpJwtIdentityMapper;
        this.mcpResourceServerProperties = mcpResourceServerProperties;
        this.mcpServerSecretVerifiers = List.copyOf(mcpServerSecretVerifiers);
        this.publicUrl = publicUrl;
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

        Jwt jwt = jwtAuthenticationToken.getToken();

        String issuerUri = String.valueOf(jwt.getIssuer());

        Optional<Issuer> staticIssuer = mcpResourceServerProperties.findIssuer(issuerUri);

        String urlTenantId;

        try {
            urlTenantId = resolveUrlTenantId(request, jwt, staticIssuer.orElse(null));
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn(
                "Rejecting MCP JWT for {}: {}", request.getServletPath(), illegalArgumentException.getMessage(),
                illegalArgumentException);

            reject(request, response, HttpServletResponse.SC_UNAUTHORIZED);

            return;
        } catch (IllegalStateException illegalStateException) {
            log.error(
                "Rejecting MCP JWT for {}: {}", request.getServletPath(), illegalStateException.getMessage(),
                illegalStateException);

            reject(request, response, HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        boolean mcpServerSecretVerified;

        try {
            mcpServerSecretVerified = isMcpServerSecretVerified(request, urlTenantId);
        } catch (RuntimeException runtimeException) {
            log.error(
                "Rejecting MCP JWT for {}: MCP server secret verification failed in tenant {}",
                request.getServletPath(), urlTenantId, runtimeException);

            reject(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            return;
        }

        if (!mcpServerSecretVerified) {
            reject(request, response, HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        if (staticIssuer.isEmpty()) {
            reject(request, response, HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        McpJwtIdentity mcpJwtIdentity = authenticateStaticIssuer(
            request, response, jwtAuthenticationToken, jwt, staticIssuer.get(), urlTenantId);

        if (mcpJwtIdentity == null) {
            return;
        }

        List<GrantedAuthority> authorities = staticIssuer.get()
            .isSelf() ? getSelfIssuedAuthorities(mcpJwtIdentity) : mcpJwtIdentity.authorities();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
            jwt, new org.springframework.security.core.userdetails.User(mcpJwtIdentity.login(), "", authorities),
            authorities);

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(authentication);

        TenantContext.runWithTenantId(
            mcpJwtIdentity.tenantId(), () -> filterChain.doFilter(request, response));
    }

    @Nullable
    private McpJwtIdentity authenticateStaticIssuer(
        HttpServletRequest request, HttpServletResponse response, JwtAuthenticationToken jwtAuthenticationToken,
        Jwt jwt, Issuer issuer, String urlTenantId) {

        String requiredScope = getRequiredScope(request);

        if (requiredScope == null) {
            reject(request, response, HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }

        if (!hasScope(jwtAuthenticationToken, requiredScope)) {
            reject(request, response, HttpServletResponse.SC_FORBIDDEN);

            return null;
        }

        if (!mcpAudienceValidator.isAudienceValid(
            jwt, issuer, McpProtectedResourceUrls.resolveResourceUrl(publicUrl, request))) {

            reject(request, response, HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }

        if (issuer.isSelf() && !isActiveByteChefUser(jwt.getSubject(), urlTenantId)) {
            reject(request, response, HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }

        if (issuer.isSelf()) {
            Integer revocationErrorStatus = getRevocationErrorStatus(jwt, urlTenantId);

            if (revocationErrorStatus != null) {
                reject(request, response, revocationErrorStatus);

                return null;
            }
        }

        try {
            return mcpJwtIdentityMapper.map(jwt, issuer, jwtAuthenticationToken.getAuthorities(), urlTenantId);
        } catch (OAuth2AuthenticationException oAuth2AuthenticationException) {
            reject(request, response, HttpServletResponse.SC_UNAUTHORIZED);

            return null;
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, int status) {
        if (status == HttpServletResponse.SC_UNAUTHORIZED) {
            mcpAuthenticationEntryPoint.sendChallenge(
                request, response, status,
                McpTenantProtectedResourceMetadataAuthenticationEntryPoint.INVALID_TOKEN);
        } else if (status == HttpServletResponse.SC_FORBIDDEN) {
            mcpAuthenticationEntryPoint.sendChallenge(
                request, response, status,
                McpTenantProtectedResourceMetadataAuthenticationEntryPoint.INSUFFICIENT_SCOPE);
        } else {
            response.setStatus(status);
        }
    }

    private String resolveUrlTenantId(HttpServletRequest request, Jwt jwt, @Nullable Issuer issuer) {
        Matcher canonicalManagementMatcher = CANONICAL_MANAGEMENT_PATTERN.matcher(request.getServletPath());

        if (canonicalManagementMatcher.matches()) {
            return resolveClaimTenantId(jwt, issuer);
        }

        Matcher matcher = MCP_SECRET_PATTERN.matcher(request.getRequestURI());

        if (!matcher.find()) {
            throw new IllegalArgumentException("MCP endpoint secret not found");
        }

        return TenantKey.parse(matcher.group(1))
            .getTenantId();
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

    private boolean isMcpServerSecretVerified(HttpServletRequest request, String urlTenantId) {
        return TenantContext.callWithTenantId(
            urlTenantId,
            () -> mcpServerSecretVerifiers.stream()
                .map(mcpServerSecretVerifier -> mcpServerSecretVerifier.verifyMcpServerSecret(request))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(false));
    }

    private List<GrantedAuthority> getSelfIssuedAuthorities(McpJwtIdentity mcpJwtIdentity) {
        List<GrantedAuthority> authorities = new ArrayList<>(mcpJwtIdentity.authorities());

        TenantContext.runWithTenantId(
            mcpJwtIdentity.tenantId(),
            () -> userService.fetchUserByLogin(mcpJwtIdentity.login())
                .map(User::getAuthorityIds)
                .orElse(List.of())
                .stream()
                .map(authorityService::fetchAuthority)
                .flatMap(Optional::stream)
                .map(Authority::getName)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add));

        return authorities;
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
