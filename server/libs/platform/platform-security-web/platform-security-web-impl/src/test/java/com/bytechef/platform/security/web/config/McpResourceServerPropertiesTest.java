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

package com.bytechef.platform.security.web.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class McpResourceServerPropertiesTest {

    @Test
    void testAcceptsNoIssuers() {
        McpResourceServerProperties mcpResourceServerProperties = new McpResourceServerProperties();

        assertThatCode(mcpResourceServerProperties::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void testAcceptsValidIssuers() {
        Issuer selfIssuer = issuer("https://as.bytechef.test", true, null);
        Issuer externalIssuer = issuer("https://idp.customer.test", false, "bytechef-mcp");

        McpResourceServerProperties mcpResourceServerProperties = properties(selfIssuer, externalIssuer);

        assertThatCode(mcpResourceServerProperties::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void testRejectsIssuerWithoutUri() {
        McpResourceServerProperties mcpResourceServerProperties = properties(issuer(null, false, null));

        assertThatIllegalStateException()
            .isThrownBy(mcpResourceServerProperties::afterPropertiesSet)
            .withMessageContaining("issuers[0].uri");
    }

    @Test
    void testRejectsIssuerWithBlankUri() {
        McpResourceServerProperties mcpResourceServerProperties = properties(
            issuer("https://as.bytechef.test", true, null), issuer(" ", false, null));

        assertThatIllegalStateException()
            .isThrownBy(mcpResourceServerProperties::afterPropertiesSet)
            .withMessageContaining("issuers[1].uri");
    }

    @Test
    void testRejectsSelfIssuerWithAudience() {
        McpResourceServerProperties mcpResourceServerProperties = properties(
            issuer("https://as.bytechef.test", true, "bytechef-mcp"));

        assertThatIllegalStateException()
            .isThrownBy(mcpResourceServerProperties::afterPropertiesSet)
            .withMessageContaining("issuers[0]");
    }

    private static Issuer issuer(String uri, boolean self, String audience) {
        Issuer issuer = new Issuer();

        issuer.setUri(uri);
        issuer.setSelf(self);
        issuer.setAudience(audience);

        return issuer;
    }

    private static McpResourceServerProperties properties(Issuer... issuers) {
        McpResourceServerProperties mcpResourceServerProperties = new McpResourceServerProperties();

        mcpResourceServerProperties.setIssuers(List.of(issuers));

        return mcpResourceServerProperties;
    }
}
