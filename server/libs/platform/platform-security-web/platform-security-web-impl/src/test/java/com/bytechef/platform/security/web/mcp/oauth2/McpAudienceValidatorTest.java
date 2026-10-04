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

import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * @author Ivica Cardic
 */
class McpAudienceValidatorTest {

    private static final String ENDPOINT_URL = "https://mcp.example.com/api/automation/acme.tenant/mcp";

    private final McpAudienceValidator mcpAudienceValidator = new McpAudienceValidator();

    @Test
    void testSelfIssuerAcceptsWhenAudienceContainsEndpointUrl() {
        Jwt jwt = jwt(List.of(ENDPOINT_URL));

        assertThat(mcpAudienceValidator.isAudienceValid(jwt, selfIssuer(), ENDPOINT_URL)).isTrue();
    }

    @Test
    void testSelfIssuerRejectsWhenAudienceIsAnotherEndpoint() {
        Jwt jwt = jwt(List.of("https://mcp.example.com/api/automation/other.tenant/mcp"));

        assertThat(mcpAudienceValidator.isAudienceValid(jwt, selfIssuer(), ENDPOINT_URL)).isFalse();
    }

    @Test
    void testSelfIssuerRejectsWhenAudienceIsMissing() {
        Jwt jwt = jwt(null);

        assertThat(mcpAudienceValidator.isAudienceValid(jwt, selfIssuer(), ENDPOINT_URL)).isFalse();
    }

    @Test
    void testSelfIssuerToleratesTrailingSlashDifference() {
        Jwt jwt = jwt(List.of(ENDPOINT_URL + "/"));

        assertThat(mcpAudienceValidator.isAudienceValid(jwt, selfIssuer(), ENDPOINT_URL)).isTrue();
    }

    @Test
    void testExternalIssuerWithoutConfiguredAudienceRequiresEndpointUrl() {
        assertThat(
            mcpAudienceValidator.isAudienceValid(jwt(List.of(ENDPOINT_URL)), externalIssuer(null), ENDPOINT_URL))
                .isTrue();
        assertThat(
            mcpAudienceValidator.isAudienceValid(jwt(List.of("urn:something-else")), externalIssuer(null),
                ENDPOINT_URL))
                    .isFalse();
    }

    @Test
    void testExternalIssuerAcceptsWhenAudienceContainsConfiguredValue() {
        Jwt jwt = jwt(List.of("https://mcp.example.com"));

        assertThat(mcpAudienceValidator.isAudienceValid(jwt, externalIssuer("https://mcp.example.com"), ENDPOINT_URL))
            .isTrue();
    }

    @Test
    void testExternalIssuerRejectsWhenConfiguredAudienceAbsent() {
        Jwt jwt = jwt(List.of("urn:wrong-audience"));

        assertThat(mcpAudienceValidator.isAudienceValid(jwt, externalIssuer("https://mcp.example.com"), ENDPOINT_URL))
            .isFalse();
    }

    private static Issuer selfIssuer() {
        Issuer issuer = new Issuer();

        issuer.setUri("https://mcp.example.com");
        issuer.setSelf(true);

        return issuer;
    }

    private static Issuer externalIssuer(String audience) {
        Issuer issuer = new Issuer();

        issuer.setUri("https://idp.customer.test");
        issuer.setAudience(audience);

        return issuer;
    }

    private static Jwt jwt(List<String> audiences) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuer("https://mcp.example.com")
            .subject("user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now()
                .plusSeconds(300));

        if (audiences != null) {
            builder.audience(audiences);
        }

        return builder.build();
    }
}
