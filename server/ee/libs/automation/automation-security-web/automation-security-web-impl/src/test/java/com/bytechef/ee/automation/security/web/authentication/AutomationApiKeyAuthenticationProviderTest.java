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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class AutomationApiKeyAuthenticationProviderTest {

    private static final String SECRET_KEY = "secret-key-value";

    @Test
    void testUnknownSecretKeyIsRejectedAsBadCredentials() {
        assertThatThrownBy(
            () -> newProvider(true, true).authenticate(new AutomationApiKeyAuthenticationToken(0, "unknown", "public")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Unknown API secret key");
    }

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

    @Test
    void testAnEmbeddedKeyIsRejected() {
        ApiKey apiKey = new ApiKey();

        apiKey.setType(PlatformType.EMBEDDED);

        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        UserService userService = mock(UserService.class);

        when(apiKeyService.fetchApiKey("btc_x", 0L)).thenReturn(Optional.of(apiKey));

        AutomationApiKeyAuthenticationProvider provider = new AutomationApiKeyAuthenticationProvider(
            apiKeyService, mock(AuthorityService.class), userService);

        AutomationApiKeyAuthenticationToken automationApiKeyAuthenticationToken =
            new AutomationApiKeyAuthenticationToken(0L, "btc_x", "tenant");

        assertThatThrownBy(() -> provider.authenticate(automationApiKeyAuthenticationToken))
            .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(userService);
    }

    @Test
    void testAnUntypedAdminKeyIsRejected() {
        ApiKey apiKey = new ApiKey();

        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        UserService userService = mock(UserService.class);

        when(apiKeyService.fetchApiKey("btc_admin", 0L)).thenReturn(Optional.of(apiKey));

        AutomationApiKeyAuthenticationProvider provider = new AutomationApiKeyAuthenticationProvider(
            apiKeyService, mock(AuthorityService.class), userService);

        AutomationApiKeyAuthenticationToken automationApiKeyAuthenticationToken =
            new AutomationApiKeyAuthenticationToken(0L, "btc_admin", "tenant");

        assertThatThrownBy(() -> provider.authenticate(automationApiKeyAuthenticationToken))
            .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(userService);
    }

    @Test
    void testAnAutomationKeyIsAuthenticated() {
        ApiKey apiKey = new ApiKey();

        apiKey.setType(PlatformType.AUTOMATION);
        apiKey.setUserId(1L);

        User user = new User();

        user.setActivated(true);
        user.setLogin("user@bytechef.com");
        user.setPassword("encoded-password");
        user.setAuthorityIds(List.of(2L));

        Authority authority = new Authority();

        authority.setId(2L);
        authority.setName("ROLE_USER");

        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        AuthorityService authorityService = mock(AuthorityService.class);
        UserService userService = mock(UserService.class);

        when(apiKeyService.fetchApiKey("btc_ok", 0L)).thenReturn(Optional.of(apiKey));
        when(userService.fetchUser(1L)).thenReturn(Optional.of(user));
        when(authorityService.fetchAuthority(anyLong())).thenReturn(Optional.of(authority));

        AutomationApiKeyAuthenticationProvider provider = new AutomationApiKeyAuthenticationProvider(
            apiKeyService, authorityService, userService);

        AutomationApiKeyAuthenticationToken automationApiKeyAuthenticationToken =
            new AutomationApiKeyAuthenticationToken(0L, "btc_ok", "tenant");

        Authentication authentication = provider.authenticate(automationApiKeyAuthenticationToken);

        assertThat(authentication.isAuthenticated()).isTrue();
    }

    private static AutomationApiKeyAuthenticationToken newToken() {
        return new AutomationApiKeyAuthenticationToken(0, SECRET_KEY, "public");
    }

    private static AutomationApiKeyAuthenticationProvider newProvider(boolean activated, boolean userExists) {
        ApiKey apiKey = mock(ApiKey.class);

        when(apiKey.getId()).thenReturn(7L);
        when(apiKey.getType()).thenReturn(PlatformType.AUTOMATION);
        when(apiKey.getUserId()).thenReturn(1L);

        ApiKeyService apiKeyService = mock(ApiKeyService.class);

        when(apiKeyService.fetchApiKey(SECRET_KEY, 0L)).thenReturn(Optional.of(apiKey));

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
