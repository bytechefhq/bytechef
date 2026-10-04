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

import com.bytechef.tenant.domain.TenantKey;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @author Ivica Cardic
 */
class McpBearerTokenResolverTest {

    private static final String JWT = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2FzIn0.c2ln";

    private final McpBearerTokenResolver mcpBearerTokenResolver =
        new McpBearerTokenResolver(RegexRequestMatcher.regexMatcher("^/api/(automation|management)/.+/mcp"));

    @Test
    void testResolvesJwtOnMcpPath() {
        MockHttpServletRequest request = mcpRequest("/api/automation/server-secret/mcp");

        request.addHeader("Authorization", "Bearer " + JWT);

        assertThat(mcpBearerTokenResolver.resolve(request)).isEqualTo(JWT);
    }

    @Test
    void testIgnoresApiKeyOnMcpPath() {
        MockHttpServletRequest request = mcpRequest("/api/automation/server-secret/mcp");

        request.addHeader("Authorization", "Bearer " + TenantKey.of("public"));

        assertThat(mcpBearerTokenResolver.resolve(request)).isNull();
    }

    @Test
    void testIgnoresJwtOnNonMcpPath() {
        MockHttpServletRequest request = mcpRequest("/api/workspaces");

        request.addHeader("Authorization", "Bearer " + JWT);

        assertThat(mcpBearerTokenResolver.resolve(request)).isNull();
    }

    @Test
    void testReturnsNullWhenNoAuthorizationHeader() {
        MockHttpServletRequest request = mcpRequest("/api/automation/server-secret/mcp");

        assertThat(mcpBearerTokenResolver.resolve(request)).isNull();
    }

    private static MockHttpServletRequest mcpRequest(String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", servletPath);

        request.setServletPath(servletPath);

        return request;
    }
}
