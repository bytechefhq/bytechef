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

package com.bytechef.platform.security.web.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

class McpAnonymousAuthenticationTokenTest {

    private static final String PRINCIPAL = "mcp-anonymous:test:42";

    @Test
    void testTokenIsAuthenticatedWithoutAuthorities() {
        McpAnonymousAuthenticationToken token = McpAnonymousAuthenticationToken.of(PRINCIPAL);

        assertThat(token.isAuthenticated()).isTrue();
        assertThat(token.getAuthorities()).isEmpty();
        assertThat(token.getCredentials()).isEqualTo("");
        assertThat(token).isNotInstanceOf(AnonymousAuthenticationToken.class);
    }

    @Test
    void testTokenExposesThePrincipalAsItsName() {
        McpAnonymousAuthenticationToken token = McpAnonymousAuthenticationToken.of(PRINCIPAL);

        assertThat(token.getPrincipal()).isEqualTo(PRINCIPAL);
        assertThat(token.getName()).isEqualTo(PRINCIPAL);
    }
}
