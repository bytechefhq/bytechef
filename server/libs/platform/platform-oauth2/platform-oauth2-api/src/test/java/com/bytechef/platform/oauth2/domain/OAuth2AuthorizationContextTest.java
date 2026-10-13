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

package com.bytechef.platform.oauth2.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Authorization;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class OAuth2AuthorizationContextTest {

    private static final DiscoveredAuthorizationServer AUTHORIZATION_SERVER = new DiscoveredAuthorizationServer(
        "https://auth.example.com", "https://auth.example.com/authorize", "https://auth.example.com/token",
        "https://auth.example.com/register", "https://mcp.example.com/mcp", List.of("read"), List.of("S256"),
        List.of("none"));

    @Test
    void testOfPkceIsNotDiscoveryAndExposesNoConnectionParameters() {
        OAuth2AuthorizationContext authorizationContext = OAuth2AuthorizationContext.ofPkce("canva", 1, "verifier");

        assertThat(authorizationContext.isDiscovery()).isFalse();
        assertThat(authorizationContext.toConnectionParameters()).isEmpty();
    }

    @Test
    void testOfDiscoveryExposesRegisteredClientAndEndpointsAsConnectionParameters() {
        OAuth2AuthorizationContext authorizationContext = OAuth2AuthorizationContext.ofDiscovery(
            "mcpClient", 1, "verifier", new RegisteredClient("client-1", null, "none"), AUTHORIZATION_SERVER);

        assertThat(authorizationContext.isDiscovery()).isTrue();
        assertThat(authorizationContext.toConnectionParameters())
            .containsEntry(Authorization.CLIENT_ID, "client-1")
            .containsEntry(Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "none")
            .containsEntry(Authorization.AUTHORIZATION_URL, "https://auth.example.com/authorize")
            .containsEntry(Authorization.TOKEN_URL, "https://auth.example.com/token")
            .containsEntry(Authorization.RESOURCE, "https://mcp.example.com/mcp")
            .doesNotContainKey(Authorization.CLIENT_SECRET);
    }

    @Test
    void testToStringRedactsClientSecretAndCodeVerifier() {
        OAuth2AuthorizationContext authorizationContext = OAuth2AuthorizationContext.ofDiscovery(
            "mcpClient", 1, "verifier-value", new RegisteredClient("client-1", "secret-value", "client_secret_post"),
            AUTHORIZATION_SERVER);

        assertThat(authorizationContext.toString())
            .doesNotContain("secret-value")
            .doesNotContain("verifier-value")
            .contains("client-1");
    }
}
