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

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/**
 * @author Ivica Cardic
 */
class McpTenantProtectedResourceMetadataAuthenticationEntryPointTest {

    private final McpTenantProtectedResourceMetadataAuthenticationEntryPoint entryPoint =
        new McpTenantProtectedResourceMetadataAuthenticationEntryPoint(null);

    @Test
    void testCommenceEmitsResourceMetadataPointerAndAutomationScopeForRequestPath() {
        MockHttpServletRequest request = request("/api/automation/secret/mcp");

        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("Authentication is required"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getHeader("WWW-Authenticate"))
            .isEqualTo(
                "Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource"
                    + "/api/automation/secret/mcp\", scope=\"mcp:automation\"");
    }

    @Test
    void testCommenceEmitsManagementScopeForSecretManagementPath() {
        MockHttpServletRequest request = request("/api/management/secret/mcp");

        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("Authentication is required"));

        assertThat(response.getHeader("WWW-Authenticate"))
            .isEqualTo(
                "Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource"
                    + "/api/management/secret/mcp\", scope=\"mcp:management\"");
    }

    @Test
    void testCommenceEmitsManagementScopeForTenantlessManagementPath() {
        MockHttpServletRequest request = request("/api/management/mcp");

        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("Authentication is required"));

        assertThat(response.getHeader("WWW-Authenticate"))
            .isEqualTo(
                "Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource"
                    + "/api/management/mcp\", scope=\"mcp:management\"");
    }

    @Test
    void testCommenceAnchorsMetadataUrlToPublicUrlInsteadOfRequestHost() {
        McpTenantProtectedResourceMetadataAuthenticationEntryPoint publicUrlEntryPoint =
            new McpTenantProtectedResourceMetadataAuthenticationEntryPoint("https://bytechef.example.com/");

        MockHttpServletRequest request = request("/api/management/mcp");

        request.setServerName("internal-host");
        request.setServerPort(9555);

        MockHttpServletResponse response = new MockHttpServletResponse();

        publicUrlEntryPoint.commence(
            request, response, new InsufficientAuthenticationException("Authentication is required"));

        assertThat(response.getHeader("WWW-Authenticate"))
            .isEqualTo(
                "Bearer resource_metadata=\"https://bytechef.example.com/.well-known/oauth-protected-resource"
                    + "/api/management/mcp\", scope=\"mcp:management\"");
    }

    @Test
    void testCommenceMarksARejectedBearerTokenAsInvalid() {
        MockHttpServletRequest request = request("/api/management/mcp");

        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InvalidBearerTokenException("Jwt expired"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getHeader("WWW-Authenticate"))
            .isEqualTo(
                "Bearer error=\"invalid_token\", resource_metadata=\"http://localhost/.well-known/" +
                    "oauth-protected-resource/api/management/mcp\", scope=\"mcp:management\"");
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);

        request.setServletPath(path);

        return request;
    }
}
