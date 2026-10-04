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

package com.bytechef.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.tenant.domain.TenantKey;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Ivica Cardic
 */
class ManagementMcpServerSecretVerifierTest {

    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of("acme"));

    private final PropertyService propertyService = mock(PropertyService.class);

    private final ManagementMcpServerSecretVerifier managementMcpServerSecretVerifier =
        new ManagementMcpServerSecretVerifier(propertyService);

    @Test
    void testConfiguredSecretKeyIsVerified() {
        mockProperty(MCP_SERVER_SECRET_KEY);

        assertThat(managementMcpServerSecretVerifier.verifyMcpServerSecret(managementRequest(MCP_SERVER_SECRET_KEY)))
            .contains(true);
    }

    @Test
    void testForgedSecretKeyIsRejected() {
        mockProperty(MCP_SERVER_SECRET_KEY);

        String forgedSecretKey = String.valueOf(TenantKey.of("acme"));

        assertThat(managementMcpServerSecretVerifier.verifyMcpServerSecret(managementRequest(forgedSecretKey)))
            .contains(false);
    }

    @Test
    void testMissingPropertyIsRejected() {
        when(propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(Optional.empty());

        assertThat(managementMcpServerSecretVerifier.verifyMcpServerSecret(managementRequest(MCP_SERVER_SECRET_KEY)))
            .contains(false);
    }

    @Test
    void testPropertyWithoutSecretKeyIsRejected() {
        mockProperty(null);

        assertThat(managementMcpServerSecretVerifier.verifyMcpServerSecret(managementRequest(MCP_SERVER_SECRET_KEY)))
            .contains(false);
    }

    @Test
    void testUnreadablePropertyIsRejected() {
        when(propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null))
            .thenThrow(new IllegalStateException());

        assertThat(managementMcpServerSecretVerifier.verifyMcpServerSecret(managementRequest(MCP_SERVER_SECRET_KEY)))
            .contains(false);
    }

    @Test
    void testOtherSurfaceIsNotClaimed() {
        String path = "/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);

        request.setServletPath(path);

        assertThat(managementMcpServerSecretVerifier.verifyMcpServerSecret(request)).isEmpty();
        verifyNoInteractions(propertyService);
    }

    private void mockProperty(String secretKey) {
        Property property = mock(Property.class);

        when(property.get("secretKey")).thenReturn(secretKey);
        when(propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null))
            .thenReturn(Optional.of(property));
    }

    private static MockHttpServletRequest managementRequest(String mcpServerSecretKey) {
        String path = "/api/management/%s/mcp".formatted(mcpServerSecretKey);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);

        request.setServletPath(path);

        return request;
    }
}
