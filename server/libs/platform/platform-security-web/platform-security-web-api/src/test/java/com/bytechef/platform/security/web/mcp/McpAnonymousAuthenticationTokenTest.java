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

    private static final String SECRET_KEY = "server-secret";

    @Test
    void testAutomationTokenIsAuthenticatedWithoutAuthorities() {
        McpAnonymousAuthenticationToken token = McpAnonymousAuthenticationToken.ofAutomationMcpServer(42);

        assertThat(token.isAuthenticated()).isTrue();
        assertThat(token.getAuthorities()).isEmpty();
        assertThat(token.getName()).isEqualTo("mcp-anonymous:automation:42");
        assertThat(token).isNotInstanceOf(AnonymousAuthenticationToken.class);
    }

    @Test
    void testManagementTokenIsAuthenticatedWithoutAuthorities() {
        McpAnonymousAuthenticationToken token = McpAnonymousAuthenticationToken.ofManagementMcpServer();

        assertThat(token.isAuthenticated()).isTrue();
        assertThat(token.getAuthorities()).isEmpty();
        assertThat(token.getName()).isEqualTo("mcp-anonymous:management");
        assertThat(token).isNotInstanceOf(AnonymousAuthenticationToken.class);
    }

    @Test
    void testPrincipalDoesNotDiscloseTheSecretKey() {
        McpAnonymousAuthenticationToken token = McpAnonymousAuthenticationToken.ofAutomationMcpServer(42);

        assertThat(token.getPrincipal()).asString()
            .doesNotContain(SECRET_KEY);
        assertThat(token.getName()).doesNotContain(SECRET_KEY);
        assertThat(token.toString()).doesNotContain(SECRET_KEY);
    }

    @Test
    void testOnlyManagementTokenIsManagementMcpServer() {
        assertThat(McpAnonymousAuthenticationToken.ofManagementMcpServer()
            .isManagementMcpServer()).isTrue();
        assertThat(McpAnonymousAuthenticationToken.ofAutomationMcpServer(42)
            .isManagementMcpServer()).isFalse();
    }

    @Test
    void testA2aTokenIdentifiesTheServerWithoutAuthorities() {
        McpAnonymousAuthenticationToken token = McpAnonymousAuthenticationToken.ofAutomationA2aServer(42);

        assertThat(token.isAuthenticated()).isTrue();
        assertThat(token.getAuthorities()).isEmpty();
        assertThat(token.getPrincipal()).isEqualTo("a2a-anonymous:automation:42");
        assertThat(token.isManagementMcpServer()).isFalse();
    }
}
