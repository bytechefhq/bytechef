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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bytechef.platform.configuration.domain.Environment;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * @author Ivica Cardic
 */
class McpApiKeyAuthenticationConverterTest {

    private final McpApiKeyAuthenticationConverter mcpApiKeyAuthenticationConverter =
        new McpApiKeyAuthenticationConverter("/api/automation/");

    @Test
    void testConvertWithBearerTokenReturnsUnauthenticatedToken() {
        MockHttpServletRequest mockHttpServletRequest = getMockHttpServletRequest();

        mockHttpServletRequest.addHeader("Authorization", "Bearer api-secret");

        ApiKeyAuthenticationToken apiKeyAuthenticationToken = convertToApiKeyToken(mockHttpServletRequest);

        McpApiKeyCredentials mcpApiKeyCredentials =
            (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();

        assertThat(apiKeyAuthenticationToken.isAuthenticated()).isFalse();
        assertThat(mcpApiKeyCredentials.getId()).isEqualTo("api-secret");
        assertThat(mcpApiKeyCredentials.getSecret()).isEqualTo("api-secret");
        assertThat(mcpApiKeyCredentials.getMcpServerSecretKey()).isEqualTo("server-secret");
        assertThat(mcpApiKeyCredentials.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testConvertWithEnvironmentHeaderUsesRequestedEnvironment() {
        MockHttpServletRequest mockHttpServletRequest = getMockHttpServletRequest();

        mockHttpServletRequest.addHeader("Authorization", "Bearer api-secret");
        mockHttpServletRequest.addHeader("X-ENVIRONMENT", "staging");

        ApiKeyAuthenticationToken apiKeyAuthenticationToken = convertToApiKeyToken(mockHttpServletRequest);

        McpApiKeyCredentials mcpApiKeyCredentials =
            (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();

        assertThat(mcpApiKeyCredentials.getEnvironment()).isEqualTo(Environment.STAGING);
    }

    @Test
    void testConvertWithoutAuthorizationHeaderProducesNullSecretToken() {
        MockHttpServletRequest mockHttpServletRequest = getMockHttpServletRequest();

        ApiKeyAuthenticationToken apiKeyAuthenticationToken = convertToApiKeyToken(mockHttpServletRequest);

        McpApiKeyCredentials mcpApiKeyCredentials =
            (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();

        assertThat(mcpApiKeyCredentials.getMcpServerSecretKey()).isEqualTo("server-secret");
        assertThat(mcpApiKeyCredentials.getSecret()).isNull();
    }

    @Test
    void testConvertWithMalformedAuthorizationHeaderProducesNullSecretToken() {
        MockHttpServletRequest mockHttpServletRequest = getMockHttpServletRequest();

        mockHttpServletRequest.addHeader("Authorization", "Basic api-secret");

        ApiKeyAuthenticationToken apiKeyAuthenticationToken = convertToApiKeyToken(mockHttpServletRequest);

        McpApiKeyCredentials mcpApiKeyCredentials =
            (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();

        assertThat(mcpApiKeyCredentials.getMcpServerSecretKey()).isEqualTo("server-secret");
        assertThat(mcpApiKeyCredentials.getSecret()).isNull();
    }

    @Test
    void testConvertWithInvalidEnvironmentHeaderThrows() {
        MockHttpServletRequest mockHttpServletRequest = getMockHttpServletRequest();

        mockHttpServletRequest.addHeader("Authorization", "Bearer api-secret");
        mockHttpServletRequest.addHeader("X-ENVIRONMENT", "bogus");

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> mcpApiKeyAuthenticationConverter.convert(mockHttpServletRequest));
    }

    private ApiKeyAuthenticationToken convertToApiKeyToken(MockHttpServletRequest mockHttpServletRequest) {
        return (ApiKeyAuthenticationToken) Objects.requireNonNull(
            mcpApiKeyAuthenticationConverter.convert(mockHttpServletRequest), "authentication is required");
    }

    private MockHttpServletRequest getMockHttpServletRequest() {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(
            "POST", "/api/automation/server-secret/mcp");

        mockHttpServletRequest.setServletPath("/api/automation/server-secret/mcp");

        return mockHttpServletRequest;
    }
}
