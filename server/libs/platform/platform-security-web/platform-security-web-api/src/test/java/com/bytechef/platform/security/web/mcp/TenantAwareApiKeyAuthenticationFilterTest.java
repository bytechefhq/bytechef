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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import jakarta.servlet.FilterChain;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @author Ivica Cardic
 */
class TenantAwareApiKeyAuthenticationFilterTest {

    private static final String PATH_TENANT_ID = "tenantb";
    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of(PATH_TENANT_ID));
    private static final String MCP_PATH = "/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY);

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final List<String> authenticationTenantIds = new ArrayList<>();
    private final List<String> chainTenantIds = new ArrayList<>();
    private final List<String> presentedApiKeys = new ArrayList<>();

    private final TenantAwareApiKeyAuthenticationFilter tenantAwareApiKeyAuthenticationFilter =
        new TenantAwareApiKeyAuthenticationFilter(
            RegexRequestMatcher.regexMatcher("^/api/automation/.+/mcp"),
            Pattern.compile("/api/automation/(.+)/mcp"), authenticationManager,
            new McpApiKeyAuthenticationConverter("/api/automation/"));

    private final FilterChain recordingFilterChain =
        (request, response) -> chainTenantIds.add(TenantContext.getCurrentTenantId());

    @Test
    void testRequestWithoutBearerTokenIsDelegatedToAuthenticationManager() throws Exception {
        MockHttpServletRequest mockHttpServletRequest = getMcpRequest();

        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Authentication required"));

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        tenantAwareApiKeyAuthenticationFilter.doFilter(
            mockHttpServletRequest, mockHttpServletResponse, new MockFilterChain());

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(401);
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void testRequestWithoutBearerTokenRunsInsidePathTenant() throws Exception {
        recordAuthenticationAndAuthenticateAnonymously();

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        tenantAwareApiKeyAuthenticationFilter.doFilter(getMcpRequest(), mockHttpServletResponse, recordingFilterChain);

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(200);
        assertThat(authenticationTenantIds).containsExactly(PATH_TENANT_ID);
        assertThat(chainTenantIds).containsExactly(PATH_TENANT_ID);
        assertThat(presentedApiKeys).containsExactly((String) null);
    }

    @Test
    void testSameTenantApiKeyIsPresentedInsidePathTenant() throws Exception {
        String apiKey = String.valueOf(TenantKey.of(PATH_TENANT_ID));

        recordAuthenticationAndAuthenticateAnonymously();

        MockHttpServletRequest mockHttpServletRequest = getMcpRequest();

        mockHttpServletRequest.addHeader("Authorization", "Bearer " + apiKey);

        tenantAwareApiKeyAuthenticationFilter.doFilter(
            mockHttpServletRequest, new MockHttpServletResponse(), recordingFilterChain);

        assertThat(authenticationTenantIds).containsExactly(PATH_TENANT_ID);
        assertThat(chainTenantIds).containsExactly(PATH_TENANT_ID);
        assertThat(presentedApiKeys).containsExactly(apiKey);
    }

    @Test
    void testCrossTenantApiKeyIsIgnoredAndPathTenantUsedWhenAuthenticationIsNotRequired() throws Exception {
        recordAuthenticationAndAuthenticateAnonymously();

        MockHttpServletRequest mockHttpServletRequest = getMcpRequest();

        mockHttpServletRequest.addHeader("Authorization", "Bearer " + TenantKey.of("tenanta"));

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        tenantAwareApiKeyAuthenticationFilter.doFilter(
            mockHttpServletRequest, mockHttpServletResponse, recordingFilterChain);

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(200);
        assertThat(authenticationTenantIds).containsExactly(PATH_TENANT_ID);
        assertThat(chainTenantIds).containsExactly(PATH_TENANT_ID);
        assertThat(presentedApiKeys).containsExactly((String) null);
    }

    @Test
    void testCrossTenantApiKeyIsRejectedWhenAuthenticationIsRequired() throws Exception {
        when(authenticationManager.authenticate(any())).thenAnswer(invocation -> {
            McpApiKeyCredentials mcpApiKeyCredentials = getCredentials(invocation.getArgument(0));

            if (mcpApiKeyCredentials.getSecret() == null) {
                throw new BadCredentialsException("Authorization token does not exist");
            }

            return McpAnonymousAuthenticationToken.ofAutomationMcpServer(1L);
        });

        MockHttpServletRequest mockHttpServletRequest = getMcpRequest();

        mockHttpServletRequest.addHeader("Authorization", "Bearer " + TenantKey.of("tenanta"));

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        tenantAwareApiKeyAuthenticationFilter.doFilter(
            mockHttpServletRequest, mockHttpServletResponse, recordingFilterChain);

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(401);
        assertThat(chainTenantIds).isEmpty();
    }

    @Test
    void testUnparseablePathSecretIsRejected() throws Exception {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(
            "POST", "/api/automation/server-secret/mcp");

        mockHttpServletRequest.setServletPath("/api/automation/server-secret/mcp");
        mockHttpServletRequest.addHeader("Authorization", "Bearer " + TenantKey.of(PATH_TENANT_ID));

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        tenantAwareApiKeyAuthenticationFilter.doFilter(
            mockHttpServletRequest, mockHttpServletResponse, recordingFilterChain);

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(401);
        assertThat(chainTenantIds).isEmpty();
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void testRequestWithNonApiKeyBearerFallsThrough() throws Exception {
        MockHttpServletRequest mockHttpServletRequest = getMcpRequest();

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

    private static McpApiKeyCredentials getCredentials(ApiKeyAuthenticationToken apiKeyAuthenticationToken) {
        return (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();
    }

    private static MockHttpServletRequest getMcpRequest() {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("POST", MCP_PATH);

        mockHttpServletRequest.setServletPath(MCP_PATH);

        return mockHttpServletRequest;
    }

    private void recordAuthenticationAndAuthenticateAnonymously() {
        when(authenticationManager.authenticate(any())).thenAnswer(invocation -> {
            McpApiKeyCredentials mcpApiKeyCredentials = getCredentials(invocation.getArgument(0));

            authenticationTenantIds.add(TenantContext.getCurrentTenantId());
            presentedApiKeys.add(mcpApiKeyCredentials.getSecret());

            return McpAnonymousAuthenticationToken.ofAutomationMcpServer(1L);
        });
    }
}
