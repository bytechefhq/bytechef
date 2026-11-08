/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import static com.bytechef.ee.platform.security.web.mcp.config.McpOAuth2ResourceServerSecurityIntTestConfiguration.AUDIENCE_BOUND_ISSUER_URI;
import static com.bytechef.ee.platform.security.web.mcp.config.McpOAuth2ResourceServerSecurityIntTestConfiguration.ISSUER_URI;
import static com.bytechef.ee.platform.security.web.mcp.config.McpOAuth2ResourceServerSecurityIntTestConfiguration.PER_TENANT_ISSUER_URI;
import static com.bytechef.ee.platform.security.web.mcp.config.McpOAuth2ResourceServerSecurityIntTestConfiguration.TENANT_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.security.web.mcp.config.McpOAuth2ResourceServerSecurityIntTestConfiguration;
import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = McpOAuth2ResourceServerSecurityIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "bytechef.edition=ee",
        "bytechef.oauth2.resource-server.issuers[0].uri=" + ISSUER_URI,
        "bytechef.oauth2.resource-server.issuers[0].tenant-claim=" + TENANT_CLAIM,
        "bytechef.oauth2.resource-server.issuers[0].self=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
class McpOAuth2ResourceServerSecurityIntTest {

    private static final String FEDERATED_USER_EMAIL = "alice@customer.test";
    private static final String OTHER_TENANT_ID = "other";
    private static final String PUBLIC_TENANT_ID = "public";

    private static final String OTHER_TENANT_SECRET_KEY = String.valueOf(TenantKey.of(OTHER_TENANT_ID));
    private static final String PUBLIC_TENANT_SECRET_KEY = String.valueOf(TenantKey.of(PUBLIC_TENANT_ID));

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    @Qualifier("mcpTestSigningKeyPair")
    private KeyPair mcpTestSigningKeyPair;

    @Autowired
    @Qualifier("mcpPerTenantIdpTestSigningKeyPair")
    private KeyPair mcpPerTenantIdpTestSigningKeyPair;

    @Autowired
    private McpTenantIssuerResolver mcpTenantIssuerResolver;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private UserService userService;

    @LocalServerPort
    private int port;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(identityProviderService, propertyService, userService);

        mcpTenantIssuerResolver.evictAll();

        when(propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null))
            .thenAnswer(invocation -> Optional.of(mcpServerProperty(currentTenantSecretKey())));

        when(identityProviderService.getIdentityProviders())
            .thenAnswer(invocation -> PUBLIC_TENANT_ID.equals(TenantContext.getCurrentTenantId())
                ? List.of(
                    identityProvider(PER_TENANT_ISSUER_URI, false),
                    identityProvider(AUDIENCE_BOUND_ISSUER_URI, true))
                : List.of());

        User federatedUser = activeUser(FEDERATED_USER_EMAIL);

        when(userService.fetchUserByEmail(anyString()))
            .thenAnswer(invocation -> PUBLIC_TENANT_ID.equals(TenantContext.getCurrentTenantId()) &&
                FEDERATED_USER_EMAIL.equals(invocation.getArgument(0)) ? Optional.of(federatedUser) : Optional.empty());

        User adminUser = activeUser("admin@localhost.com");

        when(userService.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.of(adminUser));
    }

    @Test
    void testPerTenantIdentityProviderTokenAuthenticatesOnItsTenantsEndpoint() throws Exception {
        String token = signWithPerTenantIdp(federatedClaims(PER_TENANT_ISSUER_URI, FEDERATED_USER_EMAIL).build());

        HttpResponse<String> httpResponse = postInitialize(managementPath(PUBLIC_TENANT_SECRET_KEY), token);

        assertThat(httpResponse.statusCode()).isEqualTo(200);
    }

    @Test
    void testPerTenantIdentityProviderTokenIsRejectedOnAnotherTenantsEndpoint() throws Exception {
        String token = signWithPerTenantIdp(federatedClaims(PER_TENANT_ISSUER_URI, FEDERATED_USER_EMAIL).build());

        HttpResponse<String> httpResponse = postInitialize(managementPath(OTHER_TENANT_SECRET_KEY), token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testPerTenantIdentityProviderTokenIsRejectedWithAForgedTenantSecret() throws Exception {
        String token = signWithPerTenantIdp(federatedClaims(PER_TENANT_ISSUER_URI, FEDERATED_USER_EMAIL).build());

        HttpResponse<String> httpResponse = postInitialize(
            managementPath(String.valueOf(TenantKey.of(PUBLIC_TENANT_ID))), token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testPerTenantIdentityProviderTokenWithoutAMatchingUserIsRejected() throws Exception {
        String token = signWithPerTenantIdp(federatedClaims(PER_TENANT_ISSUER_URI, "mallory@customer.test").build());

        HttpResponse<String> httpResponse = postInitialize(managementPath(PUBLIC_TENANT_SECRET_KEY), token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testPerTenantIdentityProviderTokenWithUnverifiedEmailIsRejected() throws Exception {
        String token = signWithPerTenantIdp(
            federatedClaims(PER_TENANT_ISSUER_URI, FEDERATED_USER_EMAIL)
                .claim("email_verified", false)
                .build());

        HttpResponse<String> httpResponse = postInitialize(managementPath(PUBLIC_TENANT_SECRET_KEY), token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testUntrustedIssuerIsRejected() throws Exception {
        String token = signWithPerTenantIdp(
            federatedClaims("https://untrusted.idp.test", FEDERATED_USER_EMAIL).build());

        HttpResponse<String> httpResponse = postInitialize(managementPath(PUBLIC_TENANT_SECRET_KEY), token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testTokenSignedWithAnotherKeyUnderATrustedIssuerIsRejected() throws Exception {
        String token = sign(
            federatedClaims(PER_TENANT_ISSUER_URI, FEDERATED_USER_EMAIL).build(), mcpTestSigningKeyPair);

        HttpResponse<String> httpResponse = postInitialize(managementPath(PUBLIC_TENANT_SECRET_KEY), token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testAudienceBoundIdentityProviderRejectsATokenForAnotherAudience() throws Exception {
        String token = signWithPerTenantIdp(
            federatedClaims(AUDIENCE_BOUND_ISSUER_URI, FEDERATED_USER_EMAIL)
                .audience(List.of("https://elsewhere.test/api/management/" + PUBLIC_TENANT_SECRET_KEY + "/mcp"))
                .build());

        HttpResponse<String> httpResponse = postInitialize(managementPath(PUBLIC_TENANT_SECRET_KEY), token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testAudienceBoundIdentityProviderAcceptsATokenForTheEndpoint() throws Exception {
        String path = managementPath(PUBLIC_TENANT_SECRET_KEY);

        String token = signWithPerTenantIdp(
            federatedClaims(AUDIENCE_BOUND_ISSUER_URI, FEDERATED_USER_EMAIL)
                .audience(List.of(baseUrl() + path))
                .build());

        HttpResponse<String> httpResponse = postInitialize(path, token);

        assertThat(httpResponse.statusCode()).isEqualTo(200);
    }

    @Test
    void testPerTenantIdentityProviderTokenIsRejectedOnTheCanonicalManagementEndpoint() throws Exception {
        String token = signWithPerTenantIdp(federatedClaims(PER_TENANT_ISSUER_URI, FEDERATED_USER_EMAIL).build());

        HttpResponse<String> httpResponse = postInitialize("/api/management/mcp", token);

        assertThat(httpResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void testSelfIssuedTokenStillAuthenticates() throws Exception {
        String path = managementPath(PUBLIC_TENANT_SECRET_KEY);

        String token = sign(
            baseClaims(ISSUER_URI, "admin@localhost.com")
                .claim(TENANT_CLAIM, PUBLIC_TENANT_ID)
                .claim("scope", "mcp:management")
                .audience(List.of(baseUrl() + path))
                .build(),
            mcpTestSigningKeyPair);

        HttpResponse<String> httpResponse = postInitialize(path, token);

        assertThat(httpResponse.statusCode()).isEqualTo(200);
    }

    @Test
    void testProtectedResourceMetadataAdvertisesTheTenantIdentityProvidersAndTheStaticIssuer() throws Exception {
        HttpResponse<String> httpResponse = getMetadata(PUBLIC_TENANT_SECRET_KEY);

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.body())
            .contains(
                "\"authorization_servers\":[\"" + PER_TENANT_ISSUER_URI + "\",\"" + AUDIENCE_BOUND_ISSUER_URI +
                    "\",\"" + ISSUER_URI + "\"]")
            .contains("\"scopes_supported\":[\"mcp:management\"]")
            .contains(
                "\"resource\":\"" + baseUrl() + managementPath(PUBLIC_TENANT_SECRET_KEY) + "\"");
    }

    @Test
    void testProtectedResourceMetadataOfAnotherTenantOmitsTheIdentityProviders() throws Exception {
        HttpResponse<String> httpResponse = getMetadata(OTHER_TENANT_SECRET_KEY);

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.body())
            .contains("\"authorization_servers\":[\"" + ISSUER_URI + "\"]")
            .doesNotContain(PER_TENANT_ISSUER_URI);
    }

    private static User activeUser(String login) {
        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn(login);
        when(user.getAuthorityIds()).thenReturn(List.of());

        return user;
    }

    private static JWTClaimsSet.Builder baseClaims(String issuer, String subject) {
        return new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(subject)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now()
                .plus(5, ChronoUnit.MINUTES)));
    }

    private static String currentTenantSecretKey() {
        return PUBLIC_TENANT_ID.equals(TenantContext.getCurrentTenantId())
            ? PUBLIC_TENANT_SECRET_KEY : OTHER_TENANT_SECRET_KEY;
    }

    private static JWTClaimsSet.Builder federatedClaims(String issuer, String email) {
        return baseClaims(issuer, "external-subject")
            .claim("email", email)
            .claim("email_verified", true)
            .claim("groups", List.of("ROLE_ADMIN"));
    }

    private static IdentityProvider identityProvider(String issuerUri, boolean validateMcpAudience) {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setEnabled(true);
        identityProvider.setIssuerUri(issuerUri);
        identityProvider.setMcpManagement(true);
        identityProvider.setName(issuerUri);
        identityProvider.setType("OIDC");
        identityProvider.setValidateMcpAudience(validateMcpAudience);
        identityProvider.setAuthorityMappings(Map.of());

        return identityProvider;
    }

    private static String managementPath(String secretKey) {
        return "/api/management/" + secretKey + "/mcp";
    }

    private static Property mcpServerProperty(String secretKey) {
        Property property = mock(Property.class);

        when(property.get("secretKey")).thenReturn(secretKey);

        return property;
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private HttpResponse<String> getMetadata(String secretKey) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl() + "/.well-known/oauth-protected-resource" + managementPath(secretKey)))
            .GET()
            .build();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private HttpResponse<String> postInitialize(String path, String bearerToken)
        throws IOException, InterruptedException {

        String initializeRequest = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05",\
            "capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}""";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl() + path))
            .header("Authorization", "Bearer " + bearerToken)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(initializeRequest))
            .build();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private String sign(JWTClaimsSet claimsSet, KeyPair keyPair) {
        try {
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);

            signedJWT.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));

            return signedJWT.serialize();
        } catch (JOSEException joseException) {
            throw new IllegalStateException(joseException);
        }
    }

    private String signWithPerTenantIdp(JWTClaimsSet claimsSet) {
        return sign(claimsSet, mcpPerTenantIdpTestSigningKeyPair);
    }
}
