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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import com.bytechef.platform.security.web.mcp.McpApiKeyCredentials;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class ManagementMcpServerApiKeyAuthenticationProviderTest {

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final PropertyService propertyService = mock(PropertyService.class);
    private final UserService userService = mock(UserService.class);

    private final ManagementMcpServerApiKeyAuthenticationProvider managementMcpServerApiKeyAuthenticationProvider =
        new ManagementMcpServerApiKeyAuthenticationProvider(
            apiKeyService, authorityService, propertyService, userService);

    @BeforeEach
    void beforeEach() {
        Property property = mock(Property.class);

        when(property.get("secretKey")).thenReturn("server-secret");
        when(property.get("authenticationRequired")).thenReturn(true);
        when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);
    }

    @Test
    void testAuthenticateWithValidAdminApiKeySucceeds() {
        mockApiKey(null, Environment.PRODUCTION);

        Authentication authentication = managementMcpServerApiKeyAuthenticationProvider.authenticate(
            getUnauthenticatedToken(Environment.PRODUCTION, "server-secret"));

        assertThat(authentication.isAuthenticated()).isTrue();
        verify(apiKeyService).updateLastUsedDate(7L);
    }

    @Test
    void testAuthenticateWithTypedApiKeyFails() {
        mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> managementMcpServerApiKeyAuthenticationProvider.authenticate(
                getUnauthenticatedToken(Environment.PRODUCTION, "server-secret")));
    }

    @Test
    void testAuthenticateWithWrongMcpServerSecretKeyFails() {
        mockApiKey(null, Environment.PRODUCTION);

        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> managementMcpServerApiKeyAuthenticationProvider.authenticate(
                getUnauthenticatedToken(Environment.PRODUCTION, "wrong-server-secret")));
    }

    @Test
    void testAuthenticateWithEnvironmentMismatchFails() {
        mockApiKey(null, Environment.PRODUCTION);

        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> managementMcpServerApiKeyAuthenticationProvider.authenticate(
                getUnauthenticatedToken(Environment.STAGING, "server-secret")));
    }

    @Test
    void testAuthenticateWithoutAuthenticationRequiredReturnsAnonymous() {
        Property property = mock(Property.class);

        when(property.get("secretKey")).thenReturn("server-secret");
        when(property.get("authenticationRequired")).thenReturn(false);
        when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);

        Authentication authentication = managementMcpServerApiKeyAuthenticationProvider.authenticate(
            getUnauthenticatedToken(Environment.PRODUCTION, "server-secret"));

        assertThat(authentication).isInstanceOf(McpAnonymousAuthenticationToken.class);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities()).isEmpty();
        verify(apiKeyService, never()).updateLastUsedDate(anyLong());
    }

    @Test
    void testAuthenticateWithMissingAuthenticationRequiredKeyReturnsAnonymous() {
        Property property = mock(Property.class);

        when(property.get("secretKey")).thenReturn("server-secret");
        when(property.get("authenticationRequired")).thenReturn(null);
        when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);

        Authentication authentication = managementMcpServerApiKeyAuthenticationProvider.authenticate(
            getUnauthenticatedToken(Environment.PRODUCTION, "server-secret"));

        assertThat(authentication).isInstanceOf(McpAnonymousAuthenticationToken.class);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities()).isEmpty();
        verify(apiKeyService, never()).updateLastUsedDate(anyLong());
    }

    private ApiKeyAuthenticationToken getUnauthenticatedToken(Environment environment, String mcpServerSecretKey) {
        return ApiKeyAuthenticationToken.unauthenticated(
            new McpApiKeyCredentials(environment, mcpServerSecretKey, "api-secret"));
    }

    private void mockApiKey(PlatformType type, Environment environment) {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(7L);
        apiKey.setName("test");
        apiKey.setSecretKey("api-secret");

        if (type != null) {
            apiKey.setType(type);
        }

        apiKey.setEnvironment(environment);
        apiKey.setUserId(100L);

        when(apiKeyService.fetchApiKey("api-secret")).thenReturn(Optional.of(apiKey));

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of());
        when(userService.fetchUser(100L)).thenReturn(Optional.of(user));
    }
}
