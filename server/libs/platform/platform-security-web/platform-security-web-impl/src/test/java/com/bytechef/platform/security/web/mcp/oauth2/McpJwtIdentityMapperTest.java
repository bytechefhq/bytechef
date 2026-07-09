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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * @author Ivica Cardic
 */
class McpJwtIdentityMapperTest {

    private final McpJwtIdentityMapper mcpJwtIdentityMapper = new McpJwtIdentityMapper();

    @Test
    void testUsesUrlTenantAndMapsAuthorities() {
        Issuer issuer = issuer("tenant_id", "authorities", List.of());

        Jwt jwt = jwtBuilder()
            .subject("admin@localhost.com")
            .claim("authorities", List.of("ROLE_ADMIN"))
            .build();

        McpJwtIdentity mcpJwtIdentity = mcpJwtIdentityMapper.map(
            jwt, issuer, Set.of(new SimpleGrantedAuthority("SCOPE_mcp:management")), "acme");

        assertThat(mcpJwtIdentity.tenantId()).isEqualTo("acme");
        assertThat(mcpJwtIdentity.login()).isEqualTo("admin@localhost.com");
        assertThat(authorityNames(mcpJwtIdentity)).containsExactlyInAnyOrder("ROLE_ADMIN", "SCOPE_mcp:management");
    }

    @Test
    void testMapsAuthoritiesFromClaimPlusStatic() {
        Issuer issuer = issuer("org", "groups", List.of("ROLE_USER"));

        Jwt jwt = jwtBuilder()
            .subject("jane@customer.com")
            .claim("groups", List.of("editors", "viewers"))
            .build();

        McpJwtIdentity mcpJwtIdentity = mcpJwtIdentityMapper.map(
            jwt, issuer, Set.of(new SimpleGrantedAuthority("SCOPE_mcp:management")), "customer-tenant");

        assertThat(mcpJwtIdentity.tenantId()).isEqualTo("customer-tenant");
        assertThat(authorityNames(mcpJwtIdentity))
            .containsExactlyInAnyOrder("editors", "viewers", "ROLE_USER", "SCOPE_mcp:management");
    }

    @Test
    void testNoAuthoritiesClaimYieldsOnlyStaticAndScopeAuthorities() {
        Issuer issuer = issuer("tenant_id", null, List.of());

        Jwt jwt = jwtBuilder()
            .subject("admin@localhost.com")
            .claim("authorities", List.of("ROLE_ADMIN"))
            .build();

        McpJwtIdentity mcpJwtIdentity = mcpJwtIdentityMapper.map(
            jwt, issuer, Set.of(new SimpleGrantedAuthority("SCOPE_mcp:automation")), "acme");

        assertThat(authorityNames(mcpJwtIdentity)).containsExactly("SCOPE_mcp:automation");
    }

    @Test
    void testAcceptsAgreeingTenantClaim() {
        Issuer issuer = issuer("tenant_id", null, List.of());

        Jwt jwt = jwtBuilder()
            .subject("admin@localhost.com")
            .claim("tenant_id", "acme")
            .build();

        McpJwtIdentity mcpJwtIdentity = mcpJwtIdentityMapper.map(
            jwt, issuer, Set.of(new SimpleGrantedAuthority("SCOPE_mcp:automation")), "acme");

        assertThat(mcpJwtIdentity.tenantId()).isEqualTo("acme");
    }

    @Test
    void testRejectsDisagreeingTenantClaim() {
        Issuer issuer = issuer("tenant_id", null, List.of());

        Jwt jwt = jwtBuilder()
            .subject("attacker@customer.com")
            .claim("tenant_id", "victim")
            .build();

        assertThatExceptionOfType(OAuth2AuthenticationException.class)
            .isThrownBy(
                () -> mcpJwtIdentityMapper.map(
                    jwt, issuer, Set.of(new SimpleGrantedAuthority("SCOPE_mcp:automation")), "acme"));
    }

    @Test
    void testTenantIssuerMapsGroupsThroughAuthorityMap() {
        McpTenantIssuer tenantIssuer = new McpTenantIssuer(
            "https://idp.test", false, null, Map.of("sales", "ROLE_SALES", "admins", "ROLE_ADMIN"));

        Jwt jwt = jwtBuilder()
            .subject("jane@customer.com")
            .claim("groups", List.of("sales", "unmapped"))
            .build();

        McpJwtIdentity mcpJwtIdentity = mcpJwtIdentityMapper.mapTenantIssuer(jwt, tenantIssuer, "customer-tenant");

        assertThat(mcpJwtIdentity.tenantId()).isEqualTo("customer-tenant");
        assertThat(mcpJwtIdentity.login()).isEqualTo("jane@customer.com");
        assertThat(authorityNames(mcpJwtIdentity)).containsExactly("ROLE_SALES");
    }

    @Test
    void testTenantIssuerReadsConfiguredAuthoritiesClaim() {
        McpTenantIssuer tenantIssuer = new McpTenantIssuer(
            "https://idp.test", false, "roles", Map.of("sales", "ROLE_SALES"));

        Jwt jwt = jwtBuilder()
            .subject("jane@customer.com")
            .claim("roles", List.of("sales"))
            .claim("groups", List.of("admins"))
            .build();

        McpJwtIdentity mcpJwtIdentity = mcpJwtIdentityMapper.mapTenantIssuer(jwt, tenantIssuer, "customer-tenant");

        assertThat(authorityNames(mcpJwtIdentity)).containsExactly("ROLE_SALES");
    }

    @Test
    void testTenantIssuerFallsBackToRawGroupsWhenNoMapping() {
        McpTenantIssuer tenantIssuer = new McpTenantIssuer("https://idp.test", false, null, Map.of());

        Jwt jwt = jwtBuilder()
            .subject("jane@customer.com")
            .claim("groups", List.of("editors", "viewers"))
            .build();

        McpJwtIdentity mcpJwtIdentity = mcpJwtIdentityMapper.mapTenantIssuer(jwt, tenantIssuer, "customer-tenant");

        assertThat(authorityNames(mcpJwtIdentity)).containsExactlyInAnyOrder("editors", "viewers");
    }

    @Test
    void testTenantIssuerWithoutGroupsClaimYieldsNoAuthorities() {
        McpTenantIssuer tenantIssuer = new McpTenantIssuer(
            "https://idp.test", false, null, Map.of("sales", "ROLE_SALES"));

        Jwt jwt = jwtBuilder()
            .subject("jane@customer.com")
            .build();

        McpJwtIdentity mcpJwtIdentity = mcpJwtIdentityMapper.mapTenantIssuer(jwt, tenantIssuer, "customer-tenant");

        assertThat(mcpJwtIdentity.authorities()).isEmpty();
    }

    private static Issuer issuer(String tenantClaim, String authoritiesClaim, List<String> authorities) {
        Issuer issuer = new Issuer();

        issuer.setUri("https://issuer.test");
        issuer.setTenantClaim(tenantClaim);
        issuer.setAuthoritiesClaim(authoritiesClaim);
        issuer.setAuthorities(authorities);

        return issuer;
    }

    private static Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuer("https://issuer.test")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now()
                .plusSeconds(300));
    }

    private static List<String> authorityNames(McpJwtIdentity mcpJwtIdentity) {
        return mcpJwtIdentity.authorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
    }
}
