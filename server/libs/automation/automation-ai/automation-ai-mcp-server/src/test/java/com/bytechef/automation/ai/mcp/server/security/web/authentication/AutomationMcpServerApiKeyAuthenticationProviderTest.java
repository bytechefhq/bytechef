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

package com.bytechef.automation.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Ivica Cardic
 */
class AutomationMcpServerApiKeyAuthenticationProviderTest {

    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final AutomationMcpServerApiKeyAuthenticationProvider provider =
        new AutomationMcpServerApiKeyAuthenticationProvider(mcpServerService);

    @Test
    void testValidEnabledServerAuthenticatesAsUser() {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.isEnabled()).thenReturn(true);
        when(mcpServerService.getMcpServer("secret123")).thenReturn(mcpServer);

        Authentication result = provider.authenticate(
            new AutomationMcpServerApiKeyAuthenticationToken("secret123", "public"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)).contains(AuthorityConstants.USER);
    }

    @Test
    void testUnknownSecretRejected() {
        when(mcpServerService.getMcpServer("nope")).thenThrow(new IllegalArgumentException("not found"));

        assertThatThrownBy(() -> provider.authenticate(
            new AutomationMcpServerApiKeyAuthenticationToken("nope", "public")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testDisabledServerRejected() {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.isEnabled()).thenReturn(false);
        when(mcpServerService.getMcpServer("secret123")).thenReturn(mcpServer);

        assertThatThrownBy(() -> provider.authenticate(
            new AutomationMcpServerApiKeyAuthenticationToken("secret123", "public")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testBlankSecretRejectedBeforeLookup() {
        assertThatThrownBy(() -> provider.authenticate(
            new AutomationMcpServerApiKeyAuthenticationToken("", "public")))
                .isInstanceOf(BadCredentialsException.class);

        verify(mcpServerService, never()).getMcpServer("");
    }
}
