/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.web.mcp.oauth2.McpAudienceValidator;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentity;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentityMapper;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantIssuer;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantTrustContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * @version ee
 * @author Ivica Cardic
 */
class TenantIdpFederatedIssuerAuthenticatorTest {

    private static final String ISSUER_URI = "https://idp.customer.test";
    private static final String PATH = "/api/automation/secret/mcp";

    private final TenantIdpFederatedIssuerAuthenticator tenantIdpFederatedIssuerAuthenticator =
        new TenantIdpFederatedIssuerAuthenticator(new McpAudienceValidator(), new McpJwtIdentityMapper(), null);

    @AfterEach
    void afterEach() {
        McpTenantTrustContext.clear();
    }

    @Test
    void testAuthenticatesTrustedTenantIssuerAndMapsGroups() {
        McpTenantTrustContext.set(
            List.of(new McpTenantIssuer(ISSUER_URI, false, null, Map.of("customer-admins", "ROLE_ADMIN"))));

        Jwt jwt = jwtBuilder()
            .claim("groups", List.of("customer-admins"))
            .build();

        Optional<McpJwtIdentity> mcpJwtIdentity =
            tenantIdpFederatedIssuerAuthenticator.authenticate(jwt, request(), "acme");

        assertThat(mcpJwtIdentity).isPresent();
        assertThat(mcpJwtIdentity.get()
            .tenantId()).isEqualTo("acme");
        assertThat(mcpJwtIdentity.get()
            .login()).isEqualTo("user@customer.test");
        assertThat(authorityNames(mcpJwtIdentity.get())).containsExactly("ROLE_ADMIN");
    }

    @Test
    void testReturnsEmptyWhenIssuerNotTrustedForTenant() {
        Optional<McpJwtIdentity> mcpJwtIdentity =
            tenantIdpFederatedIssuerAuthenticator.authenticate(jwtBuilder().build(), request(), "acme");

        assertThat(mcpJwtIdentity).isEmpty();
    }

    @Test
    void testReturnsEmptyWhenAudienceRequiredButMissing() {
        McpTenantTrustContext.set(List.of(new McpTenantIssuer(ISSUER_URI, true, null, Map.of())));

        Optional<McpJwtIdentity> mcpJwtIdentity =
            tenantIdpFederatedIssuerAuthenticator.authenticate(jwtBuilder().build(), request(), "acme");

        assertThat(mcpJwtIdentity).isEmpty();
    }

    @Test
    void testAcceptsAudienceBoundToTheEndpointUrl() {
        McpTenantTrustContext.set(List.of(new McpTenantIssuer(ISSUER_URI, true, null, Map.of())));

        Jwt jwt = jwtBuilder().audience(List.of("http://localhost" + PATH))
            .build();

        assertThat(tenantIdpFederatedIssuerAuthenticator.authenticate(jwt, request(), "acme")).isPresent();
    }

    @Test
    void testAnchorsTheRequiredAudienceToThePublicUrl() {
        McpTenantTrustContext.set(List.of(new McpTenantIssuer(ISSUER_URI, true, null, Map.of())));

        TenantIdpFederatedIssuerAuthenticator publicUrlAuthenticator = new TenantIdpFederatedIssuerAuthenticator(
            new McpAudienceValidator(), new McpJwtIdentityMapper(), "https://bytechef.example.com");

        Jwt publicAudienceJwt = jwtBuilder().audience(List.of("https://bytechef.example.com" + PATH))
            .build();
        Jwt internalAudienceJwt = jwtBuilder().audience(List.of("http://localhost" + PATH))
            .build();

        assertThat(publicUrlAuthenticator.authenticate(publicAudienceJwt, request(), "acme")).isPresent();
        assertThat(publicUrlAuthenticator.authenticate(internalAudienceJwt, request(), "acme")).isEmpty();
    }

    private static Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuer(ISSUER_URI)
            .subject("user@customer.test")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now()
                .plusSeconds(300));
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", PATH);

        request.setServletPath(PATH);

        return request;
    }

    private static List<String> authorityNames(McpJwtIdentity mcpJwtIdentity) {
        return mcpJwtIdentity.authorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
    }
}
