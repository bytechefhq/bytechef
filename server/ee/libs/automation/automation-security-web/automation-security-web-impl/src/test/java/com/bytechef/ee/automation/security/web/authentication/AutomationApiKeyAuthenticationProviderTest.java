/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationApiKeyAuthenticationProviderTest {

    private static final String SECRET_KEY = "secret-key-value";

    @Test
    void testAuthenticatedTokenCarriesTheApiKeyId() {
        Authentication authentication = newProvider(true, true).authenticate(newToken());

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(((AutomationApiKeyAuthenticationToken) authentication).getApiKeyId()).isEqualTo(7L);
    }

    @Test
    void testInactiveUserIsRejectedWithoutTheSecretKeyInTheMessage() {
        assertThatThrownBy(() -> newProvider(false, true).authenticate(newToken()))
            .isInstanceOf(UserNotActivatedException.class)
            .hasMessage("User admin@localhost.com was not activated");
    }

    @Test
    void testMissingUserIsRejectedWithoutTheSecretKeyInTheMessage() {
        assertThatThrownBy(() -> newProvider(true, false).authenticate(newToken()))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("User 1 was not found in the database");
    }

    private static AutomationApiKeyAuthenticationToken newToken() {
        return new AutomationApiKeyAuthenticationToken(0, SECRET_KEY, "public");
    }

    private static AutomationApiKeyAuthenticationProvider newProvider(boolean activated, boolean userExists) {
        ApiKey apiKey = mock(ApiKey.class);

        when(apiKey.getId()).thenReturn(7L);
        when(apiKey.getUserId()).thenReturn(1L);

        ApiKeyService apiKeyService = mock(ApiKeyService.class);

        when(apiKeyService.getApiKey(SECRET_KEY, 0L)).thenReturn(apiKey);

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(activated);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getPassword()).thenReturn("password");
        when(user.getAuthorityIds()).thenReturn(List.of());

        UserService userService = mock(UserService.class);

        when(userService.fetchUser(anyLong())).thenReturn(userExists ? Optional.of(user) : Optional.empty());

        return new AutomationApiKeyAuthenticationProvider(apiKeyService, mock(AuthorityService.class), userService);
    }
}
