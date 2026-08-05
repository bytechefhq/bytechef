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

package com.bytechef.automation.ai.a2a.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.web.mcp.McpApiKeyCredentials;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
class A2aApiKeyAuthenticationConverterTest {

    private final A2aApiKeyAuthenticationConverter converter =
        new A2aApiKeyAuthenticationConverter("/api/automation/a2a/");

    @Test
    void testExtractsSecretKeyFromAgentCardPath() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getServletPath()).thenReturn("/api/automation/a2a/SECRET123/.well-known/agent-card.json");

        assertThat(serverSecretKey(converter.convert(request))).isEqualTo("SECRET123");
    }

    @Test
    void testExtractsSecretKeyFromJsonRpcPath() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getServletPath()).thenReturn("/api/automation/a2a/SECRET123");

        assertThat(serverSecretKey(converter.convert(request))).isEqualTo("SECRET123");
    }

    @Test
    void testBearerTokenIsOptional() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getServletPath()).thenReturn("/api/automation/a2a/SECRET123");

        Authentication authentication = Objects.requireNonNull(converter.convert(request), "authentication");

        McpApiKeyCredentials credentials = (McpApiKeyCredentials) Objects.requireNonNull(
            authentication.getCredentials(), "credentials");

        assertThat(credentials.getSecret()).isNull();
    }

    @Test
    void testBearerTokenIsExtractedWhenPresent() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getServletPath()).thenReturn("/api/automation/a2a/SECRET123");
        when(request.getHeader("Authorization")).thenReturn("Bearer the-api-key");

        Authentication authentication = Objects.requireNonNull(converter.convert(request), "authentication");

        McpApiKeyCredentials credentials = (McpApiKeyCredentials) Objects.requireNonNull(
            authentication.getCredentials(), "credentials");

        assertThat(credentials.getSecret()).isEqualTo("the-api-key");
    }

    private static String serverSecretKey(Authentication authentication) {
        ApiKeyAuthenticationToken apiKeyAuthenticationToken = (ApiKeyAuthenticationToken) authentication;

        McpApiKeyCredentials credentials = (McpApiKeyCredentials) Objects.requireNonNull(
            apiKeyAuthenticationToken.getCredentials(), "credentials");

        return credentials.getMcpServerSecretKey();
    }
}
