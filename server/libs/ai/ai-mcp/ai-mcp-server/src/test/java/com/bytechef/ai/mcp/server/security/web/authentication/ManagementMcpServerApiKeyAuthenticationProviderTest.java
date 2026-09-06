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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
class ManagementMcpServerApiKeyAuthenticationProviderTest {

    private static final String MCP_SERVER_SECRET_KEY = "mcp-server-secret";

    private ApiKeyService apiKeyService;
    private PropertyService propertyService;
    private ManagementMcpServerApiKeyAuthenticationProvider provider;

    @BeforeEach
    void beforeEach() {
        apiKeyService = mock(ApiKeyService.class);
        propertyService = mock(PropertyService.class);

        provider = new ManagementMcpServerApiKeyAuthenticationProvider(
            apiKeyService, mock(AuthorityService.class), propertyService, mock(UserService.class));
    }

    @Test
    void testAuthenticateRejectsWhenMcpServerPropertyIsMissing() {
        when(propertyService.fetchProperty(eq("mcp.server"), eq(Property.Scope.PLATFORM), isNull()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.authenticate(token(MCP_SERVER_SECRET_KEY, null)))
            .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(apiKeyService);
    }

    @Test
    void testAuthenticateRejectsWhenMcpServerSecretKeyDoesNotMatch() {
        stubMcpServerProperty(MCP_SERVER_SECRET_KEY);

        assertThatThrownBy(() -> provider.authenticate(token("wrong-secret", null)))
            .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(apiKeyService);
    }

    @Test
    void testAuthenticateReturnsAuthenticatedTokenWhenOnlyMcpServerSecretKeyMatches() {
        stubMcpServerProperty(MCP_SERVER_SECRET_KEY);

        Authentication authentication = provider.authenticate(token(MCP_SERVER_SECRET_KEY, null));

        assertThat(authentication.isAuthenticated()).isTrue();

        verifyNoInteractions(apiKeyService);
    }

    private void stubMcpServerProperty(String secretKey) {
        Property property = new Property();

        property.setValue(Map.of("secretKey", secretKey));

        when(propertyService.fetchProperty(eq("mcp.server"), eq(Property.Scope.PLATFORM), isNull()))
            .thenReturn(Optional.of(property));
    }

    private static ManagementMcpServerApiKeyAuthenticationToken token(String mcpServerSecretKey, String authSecretKey) {
        return new ManagementMcpServerApiKeyAuthenticationToken(mcpServerSecretKey, authSecretKey, "public");
    }
}
