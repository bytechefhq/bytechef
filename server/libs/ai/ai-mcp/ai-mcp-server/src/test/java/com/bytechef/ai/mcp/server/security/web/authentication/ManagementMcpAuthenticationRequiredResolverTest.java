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
class ManagementMcpAuthenticationRequiredResolverTest {

    private static final String MCP_SERVER_SECRET_KEY = String.valueOf(TenantKey.of("public"));

    private final PropertyService propertyService = mock(PropertyService.class);
    private final ManagementMcpAuthenticationRequiredResolver managementMcpAuthenticationRequiredResolver =
        new ManagementMcpAuthenticationRequiredResolver(propertyService);

    @Test
    void testAbstainsWhenRequestIsNotAManagementMcpEndpoint() {
        Optional<Boolean> authenticationRequired = managementMcpAuthenticationRequiredResolver
            .resolveAuthenticationRequired(mockRequest("/api/automation/" + MCP_SERVER_SECRET_KEY + "/mcp"));

        assertThat(authenticationRequired).isEmpty();
    }

    @Test
    void testResolvesTenantOptingOutOfAuthentication() {
        mockProperty(false);

        Optional<Boolean> authenticationRequired = managementMcpAuthenticationRequiredResolver
            .resolveAuthenticationRequired(mockRequest("/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp"));

        assertThat(authenticationRequired).hasValue(false);
    }

    @Test
    void testResolvesTenantRequiringAuthentication() {
        mockProperty(true);

        Optional<Boolean> authenticationRequired = managementMcpAuthenticationRequiredResolver
            .resolveAuthenticationRequired(mockRequest("/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp"));

        assertThat(authenticationRequired).hasValue(true);
    }

    @Test
    void testRequiresAuthenticationWhenPropertyOmitsTheEntry() {
        mockProperty(null);

        Optional<Boolean> authenticationRequired = managementMcpAuthenticationRequiredResolver
            .resolveAuthenticationRequired(mockRequest("/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp"));

        assertThat(authenticationRequired).hasValue(true);
    }

    @Test
    void testRequiresAuthenticationWhenPathSecretIsNotATenantKey() {
        Optional<Boolean> authenticationRequired = managementMcpAuthenticationRequiredResolver
            .resolveAuthenticationRequired(mockRequest("/api/management/not-a-tenant-key/mcp"));

        assertThat(authenticationRequired).hasValue(true);
    }

    @Test
    void testRequiresAuthenticationWhenPropertyCannotBeResolved() {
        when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null))
            .thenThrow(new IllegalArgumentException());

        Optional<Boolean> authenticationRequired = managementMcpAuthenticationRequiredResolver
            .resolveAuthenticationRequired(mockRequest("/api/management/" + MCP_SERVER_SECRET_KEY + "/mcp"));

        assertThat(authenticationRequired).hasValue(true);
    }

    private void mockProperty(Boolean authenticationRequired) {
        Property property = mock(Property.class);

        when(property.get("authenticationRequired")).thenReturn(authenticationRequired);
        when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);
    }

    private static MockHttpServletRequest mockRequest(String servletPath) {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

        mockHttpServletRequest.setServletPath(servletPath);

        return mockHttpServletRequest;
    }
}
