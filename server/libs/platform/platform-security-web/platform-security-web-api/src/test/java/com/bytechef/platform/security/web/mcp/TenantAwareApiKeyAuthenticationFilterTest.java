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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @author Ivica Cardic
 */
class TenantAwareApiKeyAuthenticationFilterTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);

    private final TenantAwareApiKeyAuthenticationFilter tenantAwareApiKeyAuthenticationFilter =
        new TenantAwareApiKeyAuthenticationFilter(
            RegexRequestMatcher.regexMatcher("^/api/automation/.+/mcp"), authenticationManager,
            new McpApiKeyAuthenticationConverter("/api/automation/"));

    @Test
    void testRequestWithoutBearerTokenIsRejectedWith401() throws Exception {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(
            "POST", "/api/automation/server-secret/mcp");

        mockHttpServletRequest.setServletPath("/api/automation/server-secret/mcp");

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        tenantAwareApiKeyAuthenticationFilter.doFilter(
            mockHttpServletRequest, mockHttpServletResponse, new MockFilterChain());

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(401);
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void testRequestWithNonApiKeyBearerFallsThrough() throws Exception {
        // A Bearer token that is not a ByteChef API key (e.g. an OAuth2 JWT) does not parse as a TenantKey. The
        // API-key filter must fall through so a downstream OAuth2 resource-server filter can handle it, rather than
        // short-circuiting with 401. The /api/** chain's authenticated() rule is the backstop if nothing authenticates.
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(
            "POST", "/api/automation/server-secret/mcp");

        mockHttpServletRequest.setServletPath("/api/automation/server-secret/mcp");
        mockHttpServletRequest.addHeader("Authorization", "Bearer not-a-tenant-key");

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        MockFilterChain mockFilterChain = new MockFilterChain();

        tenantAwareApiKeyAuthenticationFilter.doFilter(
            mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(200);
        assertThat(mockFilterChain.getRequest()).isNotNull();
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void testNonMatchingRequestPassesThrough() throws Exception {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("GET", "/api/other");

        mockHttpServletRequest.setServletPath("/api/other");

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        tenantAwareApiKeyAuthenticationFilter.doFilter(
            mockHttpServletRequest, mockHttpServletResponse, new MockFilterChain());

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(200);
        verifyNoInteractions(authenticationManager);
    }
}
