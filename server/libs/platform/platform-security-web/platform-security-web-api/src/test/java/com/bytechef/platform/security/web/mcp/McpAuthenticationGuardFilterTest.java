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

import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntity;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @author Ivica Cardic
 */
class McpAuthenticationGuardFilterTest {

    private static final String MCP_PATH = "/api/automation/secret/mcp";

    private final McpAuthenticationGuardFilter mcpAuthenticationGuardFilter = new McpAuthenticationGuardFilter(
        RegexRequestMatcher.regexMatcher("^/api/automation/.+/mcp"),
        List.of(ApiKeyAuthenticationToken.class, McpAnonymousAuthenticationToken.class),
        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class McpRequest {

        @Test
        void testMcpAnonymousAuthenticationPasses() throws Exception {
            MockFilterChain mockFilterChain =
                filter(MCP_PATH, McpAnonymousAuthenticationToken.ofAutomationMcpServer(1L));

            assertThat(mockFilterChain.getRequest()).isNotNull();
        }

        @Test
        void testAuthenticatedApiKeyAuthenticationPasses() throws Exception {
            MockFilterChain mockFilterChain = filter(
                MCP_PATH, ApiKeyAuthenticationToken.authenticated(mock(ApiKeyEntity.class), List.of()));

            assertThat(mockFilterChain.getRequest()).isNotNull();
        }

        @Test
        void testUnauthenticatedApiKeyAuthenticationIsRejected() throws Exception {
            assertRejected(
                ApiKeyAuthenticationToken.unauthenticated(
                    new McpApiKeyCredentials(Environment.PRODUCTION, "secret", "key")));
        }

        @Test
        void testSessionAuthenticationIsRejected() throws Exception {
            assertRejected(
                UsernamePasswordAuthenticationToken.authenticated(
                    "admin@localhost.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        }

        @Test
        void testRememberMeAuthenticationIsRejected() throws Exception {
            assertRejected(
                new RememberMeAuthenticationToken(
                    "key", "admin@localhost.com", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        }

        @Test
        void testAnonymousAuthenticationIsRejected() throws Exception {
            assertRejected(
                new AnonymousAuthenticationToken(
                    "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        }

        @Test
        void testMissingAuthenticationIsRejected() throws Exception {
            assertRejected(null);
        }

        private void assertRejected(Authentication authentication) throws Exception {
            MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

            MockFilterChain mockFilterChain = filter(MCP_PATH, authentication, mockHttpServletResponse);

            assertThat(mockFilterChain.getRequest()).isNull();
            assertThat(mockHttpServletResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNull();
        }
    }

    @Nested
    class NonMcpRequest {

        @Test
        void testSessionAuthenticationPasses() throws Exception {
            MockFilterChain mockFilterChain = filter(
                "/api/automation/internal/projects",
                UsernamePasswordAuthenticationToken.authenticated("admin@localhost.com", null, List.of()));

            assertThat(mockFilterChain.getRequest()).isNotNull();
        }
    }

    private MockFilterChain filter(String servletPath, Authentication authentication) throws Exception {
        return filter(servletPath, authentication, new MockHttpServletResponse());
    }

    private MockFilterChain filter(
        String servletPath, Authentication authentication, MockHttpServletResponse mockHttpServletResponse)
        throws Exception {

        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("POST", servletPath);

        mockHttpServletRequest.setServletPath(servletPath);

        MockFilterChain mockFilterChain = new MockFilterChain();

        mcpAuthenticationGuardFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        return mockFilterChain;
    }
}
