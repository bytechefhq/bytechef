/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.server.web.config.EmbeddedMcpDiscoveryIntTestConfiguration;
import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.service.IdentityProviderService;
import com.bytechef.tenant.domain.TenantKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Proves the embedded MCP protected-resource metadata endpoint advertises the tenant's flagged identity provider as the
 * authorization server (the "your IdP runs the show" external-direct path), and returns 404 when the tenant has no
 * flagged identity provider.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = EmbeddedMcpDiscoveryIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmbeddedMcpDiscoveryIntTest {

    private static final String PATH_SECRET = String.valueOf(TenantKey.of("acme"));
    // A distinct tenant for the no-provider case: the shared issuer cache is keyed by tenant, so this test must not
    // reuse the tenant a data-returning test cached.
    private static final String NO_PROVIDER_PATH_SECRET = String.valueOf(TenantKey.of("no-provider"));

    @Autowired
    private IdentityProviderService identityProviderService;

    @LocalServerPort
    private int port;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(identityProviderService);
    }

    @Test
    void testMetadataAdvertisesTenantIdentityProviderIssuer() throws Exception {
        IdentityProvider identityProvider = mock(IdentityProvider.class);

        when(identityProvider.isEnabled()).thenReturn(true);
        when(identityProvider.isMcpEmbedded()).thenReturn(true);
        when(identityProvider.getIssuerUri()).thenReturn("https://idp.customer.test");
        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(identityProvider));

        HttpResponse<String> httpResponse = get(
            "/.well-known/oauth-protected-resource/api/embedded/" + PATH_SECRET + "/mcp");

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.body()).contains("\"authorization_servers\"");
        assertThat(httpResponse.body()).contains("https://idp.customer.test");
        assertThat(httpResponse.body()).contains("/api/embedded/" + PATH_SECRET + "/mcp");
    }

    @Test
    void testMetadataReturnsNotFoundWhenNoFlaggedIdentityProvider() throws Exception {
        when(identityProviderService.getIdentityProviders()).thenReturn(List.of());

        HttpResponse<String> httpResponse = get(
            "/.well-known/oauth-protected-resource/api/embedded/" + NO_PROVIDER_PATH_SECRET + "/mcp");

        assertThat(httpResponse.statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + path))
            .GET()
            .build();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        }
    }
}
