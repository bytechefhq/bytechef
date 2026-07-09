/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.security.web.mcp.config.McpProtectedResourceMetadataSecurityIntTestConfiguration;
import com.bytechef.tenant.domain.TenantKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

/**
 * Proves the dedicated metadata chain serves the RFC 9728 protected-resource metadata at the well-known path (which the
 * {@code /api/**} chain does not match), and that the metadata advertises the configured issuer - so the discovery
 * challenge's pointer resolves in production.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = McpProtectedResourceMetadataSecurityIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "bytechef.edition=ee",
    "bytechef.oauth2.resource-server.issuers[0].uri=https://as.bytechef.test",
    "bytechef.oauth2.resource-server.issuers[0].tenant-claim=tenant_id"
})
class McpProtectedResourceMetadataSecurityIntTest {

    private static final String ISSUER_URI = "https://as.bytechef.test";

    @LocalServerPort
    private int port;

    @Test
    void testProtectedResourceMetadataIsServed() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/.well-known/oauth-protected-resource/api"))
            .GET()
            .build();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(httpResponse.statusCode()).isEqualTo(200);
            assertThat(httpResponse.body()).contains(ISSUER_URI);
        }
    }

    @Test
    void testTenantProtectedResourceMetadataAdvertisesTheTenantIdentityProvider() throws Exception {
        assertTenantMetadataAdvertisesTheTenantIdentityProvider("automation");
    }

    @Test
    void testTenantProtectedResourceMetadataAdvertisesTheTenantIdentityProviderForManagement() throws Exception {
        assertTenantMetadataAdvertisesTheTenantIdentityProvider("management");
    }

    private void assertTenantMetadataAdvertisesTheTenantIdentityProvider(String surface) throws Exception {
        String secret = String.valueOf(TenantKey.of("public"));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "http://localhost:" + port + "/.well-known/oauth-protected-resource/api/" + surface + "/" + secret +
                        "/mcp"))
            .GET()
            .build();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(httpResponse.statusCode()).isEqualTo(200);
            assertThat(httpResponse.body())
                .contains(McpProtectedResourceMetadataSecurityIntTestConfiguration.TENANT_IDP_ISSUER_URI);
            assertThat(httpResponse.body()).contains(ISSUER_URI);
            assertThat(httpResponse.body()).contains("authorization_servers");
        }
    }
}
