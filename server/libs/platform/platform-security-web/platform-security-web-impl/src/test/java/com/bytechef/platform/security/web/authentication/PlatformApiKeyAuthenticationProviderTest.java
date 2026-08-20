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

package com.bytechef.platform.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * /api/platform/v1 is admin-key-only. An admin key is the one carrying no {@link PlatformType} --
 * {@code getAdminApiKeys(environmentId)} is {@code getApiKeys(environmentId, null)} -- so a non-null type means an
 * automation or embedded key was presented. Everything behind that path is ROLE_ADMIN-guarded and tenant-wide in
 * effect, so an environment-scoped key promised a containment the endpoints do not provide.
 *
 * @author Ivica Cardic
 */
class PlatformApiKeyAuthenticationProviderTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final String SECRET_KEY = "secret";

    @Test
    void testAuthenticateAcceptsAdminApiKey() {
        Authentication authentication = newProvider(null).authenticate(newToken());

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isNotNull();
    }

    @Test
    void testAuthenticateRejectsAutomationApiKey() {
        assertThatThrownBy(() -> newProvider(PlatformType.AUTOMATION).authenticate(newToken()))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Admin API key required");
    }

    @Test
    void testAuthenticateRejectsEmbeddedApiKey() {
        assertThatThrownBy(() -> newProvider(PlatformType.EMBEDDED).authenticate(newToken()))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Admin API key required");
    }

    private static PlatformApiKeyAuthenticationToken newToken() {
        return new PlatformApiKeyAuthenticationToken((int) ENVIRONMENT_ID, SECRET_KEY, "public");
    }

    private static PlatformApiKeyAuthenticationProvider newProvider(PlatformType platformType) {
        ApiKey apiKey = mock(ApiKey.class);

        when(apiKey.getType()).thenReturn(platformType);
        when(apiKey.getUserId()).thenReturn(1L);

        ApiKeyService apiKeyService = mock(ApiKeyService.class);

        when(apiKeyService.getApiKey(anyString(), anyLong())).thenReturn(apiKey);

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getPassword()).thenReturn("password");
        when(user.getAuthorityIds()).thenReturn(List.of(1L));

        UserService userService = mock(UserService.class);

        when(userService.fetchUser(anyLong())).thenReturn(Optional.of(user));

        Authority authority = mock(Authority.class);

        when(authority.getName()).thenReturn("ROLE_ADMIN");

        AuthorityService authorityService = mock(AuthorityService.class);

        when(authorityService.fetchAuthority(anyLong())).thenReturn(Optional.of(authority));

        return new PlatformApiKeyAuthenticationProvider(apiKeyService, authorityService, userService);
    }
}
