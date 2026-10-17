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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.util.SecurityUtils;
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
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * @author Ivica Cardic
 */
class TenantAwareJwtAuthenticationFilterTest {

    private static final String CANONICAL_MANAGEMENT_PATH = "/api/management/mcp";
    private static final String ISSUER_URI = "https://as.bytechef.test";
    private static final SelfIssuedTokenRevocationChecker NOT_REVOKED_CHECKER = (tokenValue, tenantId) -> false;
    private static final String PUBLIC_URL = "https://bytechef.example.com";

    private final List<String> mcpServerSecretVerificationTenantIds = new ArrayList<>();
    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final UserService userService = mock(UserService.class);

    private final McpServerSecretVerifier mcpServerSecretVerifier = request -> {
        mcpServerSecretVerificationTenantIds.add(TenantContext.getCurrentTenantId());

        return Optional.of(true);
    };

    private final TenantAwareJwtAuthenticationFilter tenantAwareJwtAuthenticationFilter =
        new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), mcpResourceServerProperties(), userService,
            authorityService,
            List.of(mcpServerSecretVerifier), null, null);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testManagementResolvesTenantFromUrlAndEnrichesPrincipal() throws Exception {
        String path = managementPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:management", endpointUrl(path)), "SCOPE_mcp:management");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> tenantId = new AtomicReference<>();
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        FilterChain filterChain = (request, servletResponse) -> {
            tenantId.set(TenantContext.getCurrentTenantId());
            authentication.set(SecurityContextHolder.getContext()
                .getAuthentication());
        };

        tenantAwareJwtAuthenticationFilter.doFilter(request(path), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(tenantId.get()).isEqualTo("acme");
        assertThat(authentication.get()
            .getName()).isEqualTo("admin@localhost.com");
        assertThat(authorityNames(authentication.get()))
            .containsExactlyInAnyOrder("SCOPE_mcp:management", "engineering");
    }

    @Test
    void testAutomationResolvesTenantFromUrl() throws Exception {
        String path = automationPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> tenantId = new AtomicReference<>();

        FilterChain filterChain = (servletRequest, servletResponse) -> tenantId.set(TenantContext.getCurrentTenantId());

        tenantAwareJwtAuthenticationFilter.doFilter(request(path), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(tenantId.get()).isEqualTo("acme");
    }

    @Test
    void testRejectsTokenMissingRequiredScope() throws Exception {
        String path = managementPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("WWW-Authenticate"))
            .startsWith("Bearer error=\"insufficient_scope\"")
            .contains("scope=\"mcp:management\"");
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRejectionAnswersWithAnInvalidTokenChallenge() throws Exception {
        String path = automationPath("acme");

        authenticate(
            jwtWithTenant("acme", "mcp:automation", "http://localhost/api/automation/other-secret/mcp"),
            "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantAwareJwtAuthenticationFilter.doFilter(request(path), response, (servletRequest, servletResponse) -> {});

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).isNull();
        assertThat(response.getHeader("WWW-Authenticate"))
            .isEqualTo(
                "Bearer error=\"invalid_token\", resource_metadata=\"http://localhost/.well-known/" +
                    "oauth-protected-resource" + path + "\", scope=\"mcp:automation\"");
    }

    @Test
    void testAcceptsTokenWhoseAudienceIsThePublicResourceUrl() throws Exception {
        String path = automationPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:automation", PUBLIC_URL + path), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        publicUrlFilter().doFilter(
            request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chainInvoked).isTrue();
    }

    @Test
    void testRejectsTokenWhoseAudienceIsTheInternalUrlWhenAPublicUrlIsConfigured() throws Exception {
        String path = automationPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        publicUrlFilter().doFilter(
            request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRejectsRequestWhoseEndpointHasNoRequiredScope() throws Exception {
        String path = automationPath("acme") + "/unmatched";

        authenticate(jwtWithTenant("acme", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRejectsTokenMissingAudience() throws Exception {
        authenticate(jwtWithTenant("acme", "mcp:automation", null), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(automationPath("acme")), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRejectsTokenWhoseAudienceIsAnotherEndpoint() throws Exception {
        authenticate(
            jwtWithTenant("acme", "mcp:automation", "http://localhost/api/automation/other-secret/mcp"),
            "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(automationPath("acme")), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRejectsTokenWhoseTenantClaimDisagreesWithUrl() throws Exception {
        String path = automationPath("acme");

        authenticate(jwtWithTenant("victim", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRejectsTokenMissingTheConfiguredTenantClaim() throws Exception {
        String path = automationPath("acme");

        authenticate(jwtNoTenant("mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRejectsSelfIssuerTokenWhenByteChefUserIsNotActive() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), selfIssuerProperties(), userService,
            authorityService,
            List.of(mcpServerSecretVerifier), null, null);

        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.empty());

        String path = automationPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testSelfIssuedTokenCarriesTheByteChefUserAndItsRoles() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), selfIssuerProperties(), userService,
            authorityService, List.of(mcpServerSecretVerifier), NOT_REVOKED_CHECKER, null);

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getAuthorityIds()).thenReturn(List.of(5L));
        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.of(user));

        Authority adminAuthority = new Authority();

        adminAuthority.setName("ROLE_ADMIN");

        when(authorityService.fetchAuthority(5L)).thenReturn(Optional.of(adminAuthority));

        String path = automationPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        AtomicReference<Authentication> authentication = new AtomicReference<>();
        AtomicReference<Optional<String>> currentUserLogin = new AtomicReference<>();

        filter.doFilter(
            request(path), new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
                authentication.set(SecurityContextHolder.getContext()
                    .getAuthentication());
                currentUserLogin.set(SecurityUtils.fetchCurrentUserLogin());
            });

        assertThat(authentication.get()).isInstanceOf(JwtAuthenticationToken.class);
        assertThat(authentication.get()
            .getPrincipal()).isInstanceOf(UserDetails.class);
        assertThat(currentUserLogin.get()).contains("admin@localhost.com");
        assertThat(authorityNames(authentication.get())).contains("ROLE_ADMIN", "SCOPE_mcp:automation");
    }

    @Test
    void testExternalIssuerTokenNeverCarriesARoleClaim() throws Exception {
        String path = managementPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:management", endpointUrl(path)), "SCOPE_mcp:management");

        AtomicReference<Authentication> authentication = new AtomicReference<>();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(path), new MockHttpServletResponse(),
            (servletRequest, servletResponse) -> authentication.set(SecurityContextHolder.getContext()
                .getAuthentication()));

        assertThat(authorityNames(authentication.get())).doesNotContain("ROLE_ADMIN");
    }

    @Test
    void testAllowsSelfIssuerTokenWhenByteChefUserIsActive() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), selfIssuerProperties(), userService,
            authorityService,
            List.of(mcpServerSecretVerifier), NOT_REVOKED_CHECKER, null);

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.of(user));

        String path = automationPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chainInvoked).isTrue();
    }

    @Test
    void testPassesThroughNonJwtAuthenticationUntouched() throws Exception {
        Authentication apiKeyAuthentication = new UsernamePasswordAuthenticationToken(
            "api-key-user", null, List.of());

        SecurityContextHolder.getContext()
            .setAuthentication(apiKeyAuthentication);

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Authentication> authentication = new AtomicReference<>();

        FilterChain filterChain = (servletRequest, servletResponse) -> authentication.set(
            SecurityContextHolder.getContext()
                .getAuthentication());

        tenantAwareJwtAuthenticationFilter.doFilter(request(managementPath("acme")), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(authentication.get()).isSameAs(apiKeyAuthentication);
    }

    @Test
    void testRejectsStaticIssuerTokenWithoutSubject() throws Exception {
        String path = automationPath("acme");

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuer(ISSUER_URI)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now()
                .plusSeconds(300))
            .claim("scope", "mcp:automation")
            .audience(List.of(endpointUrl(path)))
            .build();

        authenticate(jwt, "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRejectsNonStaticIssuer() throws Exception {
        authenticate(jwtNonStaticIssuer(), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(automationPath("acme")), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testVerifiesMcpServerSecretInsideUrlTenant() throws Exception {
        String path = automationPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chainInvoked).isTrue();
        assertThat(mcpServerSecretVerificationTenantIds).containsExactly("acme");
    }

    @Test
    void testRejectsForgedMcpServerSecret() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), mcpResourceServerProperties(), userService,
            authorityService,
            List.of(request -> Optional.of(false)), null, null);

        String path = managementPath("victim");

        authenticate(jwtWithTenant("victim", "mcp:management", endpointUrl(path)), "SCOPE_mcp:management");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRejectsMcpServerSecretNoVerifierClaims() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), mcpResourceServerProperties(), userService,
            authorityService,
            List.of(request -> Optional.empty()), null, null);

        String path = automationPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRespondsServiceUnavailableWhenMcpServerSecretVerifierFails() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), mcpResourceServerProperties(), userService,
            authorityService,
            List.of(request -> {
                throw new IllegalStateException("lookup failed");
            }), null, null);

        String path = automationPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRejectsRevokedSelfIssuedToken() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = selfIssuerFilterWithChecker((tokenValue, tenantId) -> true);

        assertThat(doSelfIssuerRequest(filter)).isEqualTo(401);
    }

    @Test
    void testRejectsSelfIssuedTokenWhenNoRevocationCheckerIsConfigured() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = selfIssuerFilterWithChecker(null);

        assertThat(doSelfIssuerRequest(filter)).isEqualTo(401);
    }

    @Test
    void testRejectsSelfIssuedTokenWithServiceUnavailableWhenRevocationCheckFails() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = selfIssuerFilterWithChecker((tokenValue, tenantId) -> {
            throw new IllegalStateException("revocation store unavailable");
        });

        assertThat(doSelfIssuerRequest(filter)).isEqualTo(503);
    }

    @Test
    void testPassesTokenValueAndUrlTenantToChecker() throws Exception {
        List<String> checked = new ArrayList<>();

        TenantAwareJwtAuthenticationFilter filter = selfIssuerFilterWithChecker((tokenValue, tenantId) -> {
            checked.add(tokenValue + "@" + tenantId);

            return false;
        });

        assertThat(doSelfIssuerRequest(filter)).isEqualTo(200);
        assertThat(checked).containsExactly("token@acme");
    }

    @Test
    void testCanonicalManagementPathTakesTenantFromSelfIssuedClaim() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = selfIssuerFilterWithChecker(NOT_REVOKED_CHECKER);

        authenticate(
            jwtWithTenant("acme", "mcp:management", endpointUrl(CANONICAL_MANAGEMENT_PATH)), "SCOPE_mcp:management");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> tenantId = new AtomicReference<>();

        filter.doFilter(
            request(CANONICAL_MANAGEMENT_PATH), response,
            (servletRequest, servletResponse) -> tenantId.set(TenantContext.getCurrentTenantId()));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(tenantId.get()).isEqualTo("acme");
        assertThat(mcpServerSecretVerificationTenantIds).containsExactly("acme");
    }

    @Test
    void testCanonicalManagementPathChecksRevocationInClaimTenant() throws Exception {
        List<String> checked = new ArrayList<>();

        TenantAwareJwtAuthenticationFilter filter = selfIssuerFilterWithChecker((tokenValue, tenantId) -> {
            checked.add(tokenValue + "@" + tenantId);

            return true;
        });

        authenticate(
            jwtWithTenant("acme", "mcp:management", endpointUrl(CANONICAL_MANAGEMENT_PATH)), "SCOPE_mcp:management");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(CANONICAL_MANAGEMENT_PATH), response, (servletRequest, servletResponse) -> {});

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(checked).containsExactly("token@acme");
    }

    @Test
    void testCanonicalManagementPathRejectsExternalIssuer() throws Exception {
        authenticate(
            jwtWithTenant("acme", "mcp:management", endpointUrl(CANONICAL_MANAGEMENT_PATH)), "SCOPE_mcp:management");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(CANONICAL_MANAGEMENT_PATH), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
        assertThat(mcpServerSecretVerificationTenantIds).isEmpty();
    }

    @Test
    void testCanonicalManagementPathRejectsTokenWithoutTenantClaim() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = selfIssuerFilterWithChecker(NOT_REVOKED_CHECKER);

        authenticate(jwtNoTenant("mcp:management", endpointUrl(CANONICAL_MANAGEMENT_PATH)), "SCOPE_mcp:management");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(
            request(CANONICAL_MANAGEMENT_PATH), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testCanonicalManagementPathRejectsTenantWithoutManagementServer() throws Exception {
        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.of(user));

        TenantAwareJwtAuthenticationFilter filter = new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), selfIssuerProperties(), userService,
            authorityService,
            List.of(request -> Optional.of(false)), null, null);

        authenticate(
            jwtWithTenant("acme", "mcp:management", endpointUrl(CANONICAL_MANAGEMENT_PATH)), "SCOPE_mcp:management");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(
            request(CANONICAL_MANAGEMENT_PATH), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testTenantlessAutomationPathIsRejected() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = selfIssuerFilterWithChecker(NOT_REVOKED_CHECKER);

        String path = "/api/automation/mcp";

        authenticate(jwtWithTenant("acme", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(path), response, (servletRequest, servletResponse) -> {});

        assertThat(response.getStatus()).isEqualTo(401);
    }

    private TenantAwareJwtAuthenticationFilter selfIssuerFilterWithChecker(
        SelfIssuedTokenRevocationChecker selfIssuedTokenRevocationChecker) {

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.of(user));

        return new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), selfIssuerProperties(), userService,
            authorityService,
            List.of(mcpServerSecretVerifier), selfIssuedTokenRevocationChecker, null);
    }

    private TenantAwareJwtAuthenticationFilter publicUrlFilter() {
        return new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), mcpResourceServerProperties(), userService,
            authorityService,
            List.of(mcpServerSecretVerifier), null, PUBLIC_URL);
    }

    private int doSelfIssuerRequest(TenantAwareJwtAuthenticationFilter filter) throws Exception {
        String path = automationPath("acme");

        authenticate(jwtWithTenant("acme", "mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(path), response, (servletRequest, servletResponse) -> {});

        return response.getStatus();
    }

    private static void authenticate(Jwt jwt, String scopeAuthority) {
        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(
            jwt, Set.of(new SimpleGrantedAuthority(scopeAuthority)));

        SecurityContextHolder.getContext()
            .setAuthentication(jwtAuthenticationToken);
    }

    private static Jwt jwtNoTenant(String scope, String audience) {
        Jwt.Builder builder = jwtBuilder(scope);

        if (audience != null) {
            builder.audience(List.of(audience));
        }

        return builder.build();
    }

    private static Jwt jwtWithTenant(String tenantId, String scope, String audience) {
        Jwt.Builder builder = jwtBuilder(scope).claim("tenant_id", tenantId);

        if (audience != null) {
            builder.audience(List.of(audience));
        }

        return builder.build();
    }

    private static Jwt jwtNonStaticIssuer() {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuer("https://idp.customer.test")
            .subject("user@customer.test")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now()
                .plusSeconds(300))
            .build();
    }

    private static Jwt.Builder jwtBuilder(String scope) {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuer(ISSUER_URI)
            .subject("admin@localhost.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now()
                .plusSeconds(300))
            .claim("authorities", List.of("ROLE_ADMIN", "engineering"))
            .claim("scope", scope);
    }

    private static String automationPath(String tenantId) {
        return "/api/automation/" + TenantKey.of(tenantId) + "/mcp";
    }

    private static String managementPath(String tenantId) {
        return "/api/management/" + TenantKey.of(tenantId) + "/mcp";
    }

    private static String endpointUrl(String path) {
        return "http://localhost" + path;
    }

    private static MockHttpServletRequest request(String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", servletPath);

        request.setServletPath(servletPath);

        return request;
    }

    private static McpResourceServerProperties mcpResourceServerProperties() {
        Issuer issuer = new Issuer();

        issuer.setUri(ISSUER_URI);
        issuer.setTenantClaim("tenant_id");
        issuer.setAuthoritiesClaim("authorities");

        McpResourceServerProperties mcpResourceServerProperties = new McpResourceServerProperties();

        mcpResourceServerProperties.setIssuers(List.of(issuer));

        return mcpResourceServerProperties;
    }

    private static McpResourceServerProperties selfIssuerProperties() {
        Issuer issuer = new Issuer();

        issuer.setUri(ISSUER_URI);
        issuer.setTenantClaim("tenant_id");
        issuer.setAuthoritiesClaim("authorities");
        issuer.setSelf(true);

        McpResourceServerProperties mcpResourceServerProperties = new McpResourceServerProperties();

        mcpResourceServerProperties.setIssuers(List.of(issuer));

        return mcpResourceServerProperties;
    }

    private static List<String> authorityNames(Authentication authentication) {
        return authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
    }
}
