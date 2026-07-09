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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the federation-neutral trust gate of the decoder factory: with no (or an unrelated) per-tenant trust context, a
 * non-statically-configured issuer is rejected before any network fetch - the behavior a CE-only build (where the trust
 * context is never populated) inherits. The admit path is not unit tested here because it fetches issuer metadata over
 * the network.
 *
 * @author Ivica Cardic
 */
class IssuerLocationMcpJwtDecoderFactoryTest {

    private static final String STATIC_ISSUER_URI = "https://as.bytechef.test";

    private final IssuerLocationMcpJwtDecoderFactory issuerLocationMcpJwtDecoderFactory =
        new IssuerLocationMcpJwtDecoderFactory(Set.of(STATIC_ISSUER_URI));

    @AfterEach
    void afterEach() {
        McpTenantTrustContext.clear();
    }

    @Test
    void testRejectsUntrustedIssuerWhenNoTenantTrustContext() {
        assertThat(issuerLocationMcpJwtDecoderFactory.createJwtDecoder("https://attacker.test")).isNull();
    }

    @Test
    void testRejectsUntrustedIssuerWhenTenantTrustContextListsAnotherIssuer() {
        McpTenantTrustContext.set(List.of(new McpTenantIssuer("https://idp.customer.test", false, null, Map.of())));

        assertThat(issuerLocationMcpJwtDecoderFactory.createJwtDecoder("https://attacker.test")).isNull();
    }
}
