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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Ivica Cardic
 */
class ManagementMcpServerApiKeyAuthenticationProviderTest {

    private final PropertyService propertyService = mock(PropertyService.class);
    private final ManagementMcpServerApiKeyAuthenticationProvider provider =
        new ManagementMcpServerApiKeyAuthenticationProvider(propertyService);

    @Test
    void testValidSecretAuthenticatesAsAdmin() {
        givenConfiguredSecret("topsecret");

        Authentication result = provider.authenticate(
            new ManagementMcpServerApiKeyAuthenticationToken("topsecret", "public"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)).contains(AuthorityConstants.ADMIN);
    }

    @Test
    void testWrongSecretRejected() {
        givenConfiguredSecret("topsecret");

        assertThatThrownBy(() -> provider.authenticate(
            new ManagementMcpServerApiKeyAuthenticationToken("wrong", "public")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testBlankConfiguredSecretRejected() {
        givenConfiguredSecret("");

        assertThatThrownBy(() -> provider.authenticate(
            new ManagementMcpServerApiKeyAuthenticationToken("", "public")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testMissingPropertyRejected() {
        when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(null);

        assertThatThrownBy(() -> provider.authenticate(
            new ManagementMcpServerApiKeyAuthenticationToken("topsecret", "public")))
                .isInstanceOf(BadCredentialsException.class);
    }

    private void givenConfiguredSecret(String secret) {
        Property property = mock(Property.class);

        when(property.get("secretKey")).thenReturn(secret);
        when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);
    }
}
