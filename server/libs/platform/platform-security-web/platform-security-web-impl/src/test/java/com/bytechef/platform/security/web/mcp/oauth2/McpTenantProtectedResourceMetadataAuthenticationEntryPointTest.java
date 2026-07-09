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

/**
 * Pins the federation-neutral base contract of the discovery challenge: it emits an RFC 9728 pointer built purely by
 * inserting the well-known prefix ahead of the request path, with no tenant or identity-provider input.
 *
 * @author Ivica Cardic
 */
class McpTenantProtectedResourceMetadataAuthenticationEntryPointTest {

    private final McpTenantProtectedResourceMetadataAuthenticationEntryPoint entryPoint =
        new McpTenantProtectedResourceMetadataAuthenticationEntryPoint();

    @Test
    void testCommenceEmitsResourceMetadataPointerForRequestPath() {
        MockHttpServletRequest request = request("/api/automation/secret/mcp");

        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("Authentication is required"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getHeader("WWW-Authenticate"))
            .isEqualTo(
                "Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource"
                    + "/api/automation/secret/mcp\"");
    }

    @Test
    void testCommenceHandlesManagementEndpointPathIdentically() {
        MockHttpServletRequest request = request("/api/management/secret/mcp");

        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("Authentication is required"));

        assertThat(response.getHeader("WWW-Authenticate"))
            .isEqualTo(
                "Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource"
                    + "/api/management/secret/mcp\"");
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);

        request.setServletPath(path);

        return request;
    }
}
