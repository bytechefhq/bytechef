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

package com.bytechef.automation.ai.a2a.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.platform.configuration.domain.Environment;
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
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class AutomationA2AServerApiKeyAuthenticationProviderTest {

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final A2aServerService a2aServerService = mock(A2aServerService.class);
    private final UserService userService = mock(UserService.class);

    private final AutomationA2AServerApiKeyAuthenticationProvider provider =
        new AutomationA2AServerApiKeyAuthenticationProvider(
            apiKeyService, authorityService, a2aServerService, userService);

    @Test
    void testAuthenticateWithValidAutomationApiKeySucceeds() {
        mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);
        mockA2aServer(Environment.PRODUCTION);

        Authentication authentication = provider.authenticate(getUnauthenticatedToken());

        assertThat(authentication.isAuthenticated()).isTrue();
        verify(apiKeyService).updateLastUsedDate(7L);
    }

    @Test
    void testAuthenticateWithWrongTypeApiKeyFails() {
        mockApiKey(PlatformType.EMBEDDED, Environment.PRODUCTION);
        mockA2aServer(Environment.PRODUCTION);

        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> provider.authenticate(getUnauthenticatedToken()));
    }

    @Test
    void testAuthenticateWithoutAuthenticationRequiredReturnsAnonymous() {
        A2aServer a2aServer = mock(A2aServer.class);

        when(a2aServer.isAuthenticationRequired()).thenReturn(false);
        when(a2aServerService.getA2aServer("server-secret")).thenReturn(a2aServer);

        Authentication authentication = provider.authenticate(getUnauthenticatedToken());

        assertThat(authentication).isInstanceOf(McpAnonymousAuthenticationToken.class);
        assertThat(authentication.isAuthenticated()).isTrue();
        verify(apiKeyService, never()).updateLastUsedDate(anyLong());
    }

    @Test
    void testAuthenticateWithEnvironmentMismatchFails() {
        mockApiKey(PlatformType.AUTOMATION, Environment.STAGING);
        mockA2aServer(Environment.PRODUCTION);

        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> provider.authenticate(getUnauthenticatedToken()));
    }

    @Test
    void testAuthenticateWithUnknownA2aServerSecretKeyFails() {
        mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

        when(a2aServerService.getA2aServer("server-secret")).thenThrow(new IllegalArgumentException());

        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> provider.authenticate(getUnauthenticatedToken()));
    }

    private ApiKeyAuthenticationToken getUnauthenticatedToken() {
        return ApiKeyAuthenticationToken.unauthenticated(
            new McpApiKeyCredentials(Environment.PRODUCTION, "server-secret", "api-secret"));
    }

    private void mockApiKey(PlatformType type, Environment environment) {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(7L);
        apiKey.setName("test");
        apiKey.setSecretKey("api-secret");
        apiKey.setType(type);
        apiKey.setEnvironment(environment);
        apiKey.setUserId(100L);

        when(apiKeyService.fetchApiKey("api-secret")).thenReturn(Optional.of(apiKey));

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of());
        when(userService.fetchUser(100L)).thenReturn(Optional.of(user));
    }

    private void mockA2aServer(Environment environment) {
        A2aServer a2aServer = mock(A2aServer.class);

        when(a2aServer.getEnvironment()).thenReturn(environment);
        when(a2aServer.isAuthenticationRequired()).thenReturn(true);
        when(a2aServerService.getA2aServer("server-secret")).thenReturn(a2aServer);
    }
}
