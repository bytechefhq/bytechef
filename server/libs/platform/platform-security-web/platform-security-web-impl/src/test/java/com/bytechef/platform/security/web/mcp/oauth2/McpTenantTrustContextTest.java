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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class McpTenantTrustContextTest {

    @AfterEach
    void afterEach() {
        McpTenantTrustContext.clear();
    }

    @Test
    void testGetIssuerReturnsNullWhenUnset() {
        assertThat(McpTenantTrustContext.getIssuer("https://idp.test")).isNull();
        assertThat(McpTenantTrustContext.getIssuerUris()).isEmpty();
    }

    @Test
    void testSetExposesIssuersByUri() {
        McpTenantIssuer tenantIssuer = new McpTenantIssuer(
            "https://idp.test", true, null, Map.of("sales", "ROLE_SALES"));

        McpTenantTrustContext.set(List.of(tenantIssuer));

        assertThat(McpTenantTrustContext.getIssuerUris()).containsExactly("https://idp.test");
        assertThat(McpTenantTrustContext.getIssuer("https://idp.test")).isSameAs(tenantIssuer);
        assertThat(McpTenantTrustContext.getIssuer("https://other.test")).isNull();
    }

    @Test
    void testClearRemovesIssuers() {
        McpTenantTrustContext.set(List.of(new McpTenantIssuer("https://idp.test", false, null, Map.of())));

        McpTenantTrustContext.clear();

        assertThat(McpTenantTrustContext.getIssuerUris()).isEmpty();
    }
}
