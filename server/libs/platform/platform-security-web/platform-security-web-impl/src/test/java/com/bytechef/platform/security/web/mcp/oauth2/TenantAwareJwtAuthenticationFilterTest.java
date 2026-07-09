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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import jakarta.servlet.FilterChain;
import java.time.Instant;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * @author Ivica Cardic
 */
class TenantAwareJwtAuthenticationFilterTest {

    private static final String ISSUER_URI = "https://as.bytechef.test";

    private final UserService userService = mock(UserService.class);

    private final TenantAwareJwtAuthenticationFilter tenantAwareJwtAuthenticationFilter =
        new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), mcpResourceServerProperties(), userService, null);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testManagementResolvesTenantFromUrlAndEnrichesPrincipal() throws Exception {
        String path = managementPath("acme");

        authenticate(jwtNoTenant("mcp:management", endpointUrl(path)), "SCOPE_mcp:management");

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
            .containsExactlyInAnyOrder("SCOPE_mcp:management", "ROLE_ADMIN");
    }

    @Test
    void testAutomationResolvesTenantFromUrl() throws Exception {
        String path = automationPath("acme");

        authenticate(jwtNoTenant("mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

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

        authenticate(jwtNoTenant("mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRejectsTokenMissingAudience() throws Exception {
        authenticate(jwtNoTenant("mcp:automation", null), "SCOPE_mcp:automation");

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
            jwtNoTenant("mcp:automation", "http://localhost/api/automation/other-secret/mcp"),
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
    void testRejectsSelfIssuerTokenWhenByteChefUserIsNotActive() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), selfIssuerProperties(), userService, null);

        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.empty());

        String path = automationPath("acme");

        authenticate(jwtNoTenant("mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(request(path), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testAllowsSelfIssuerTokenWhenByteChefUserIsActive() throws Exception {
        TenantAwareJwtAuthenticationFilter filter = new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), selfIssuerProperties(), userService, null);

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.of(user));

        String path = automationPath("acme");

        authenticate(jwtNoTenant("mcp:automation", endpointUrl(path)), "SCOPE_mcp:automation");

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
    void testDelegatesNonStaticIssuerToFederatedAuthenticator() throws Exception {
        McpFederatedIssuerAuthenticator federatedIssuerAuthenticator = mock(McpFederatedIssuerAuthenticator.class);

        when(federatedIssuerAuthenticator.authenticate(any(), any(), any()))
            .thenReturn(
                Optional.of(
                    new McpJwtIdentity(
                        "acme", "user@customer.test", List.of(new SimpleGrantedAuthority("ROLE_USER")))));

        TenantAwareJwtAuthenticationFilter filter = new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), mcpResourceServerProperties(), userService,
            federatedIssuerAuthenticator);

        String path = automationPath("acme");

        authenticate(jwtNonStaticIssuer(), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> tenantId = new AtomicReference<>();
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        FilterChain filterChain = (servletRequest, servletResponse) -> {
            tenantId.set(TenantContext.getCurrentTenantId());
            authentication.set(SecurityContextHolder.getContext()
                .getAuthentication());
        };

        filter.doFilter(request(path), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(tenantId.get()).isEqualTo("acme");
        assertThat(authentication.get()
            .getName()).isEqualTo("user@customer.test");
        assertThat(authorityNames(authentication.get())).containsExactly("ROLE_USER");
    }

    @Test
    void testRejectsNonStaticIssuerWhenFederatedAuthenticatorReturnsEmpty() throws Exception {
        McpFederatedIssuerAuthenticator federatedIssuerAuthenticator = mock(McpFederatedIssuerAuthenticator.class);

        when(federatedIssuerAuthenticator.authenticate(any(), any(), any())).thenReturn(Optional.empty());

        TenantAwareJwtAuthenticationFilter filter = new TenantAwareJwtAuthenticationFilter(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), mcpResourceServerProperties(), userService,
            federatedIssuerAuthenticator);

        authenticate(jwtNonStaticIssuer(), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(
            request(automationPath("acme")), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void testRejectsNonStaticIssuerWhenNoFederatedAuthenticator() throws Exception {
        authenticate(jwtNonStaticIssuer(), "SCOPE_mcp:automation");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean();

        tenantAwareJwtAuthenticationFilter.doFilter(
            request(automationPath("acme")), response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
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
        return jwtBuilder(scope)
            .claim("tenant_id", tenantId)
            .audience(List.of(audience))
            .build();
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
            .claim("authorities", List.of("ROLE_ADMIN"))
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
