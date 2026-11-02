/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.exception.UserNotActivatedException;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @version ee
 * @author Ivica Cardic
 */
class PlatformApiKeyAuthenticationProviderTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final String SECRET_KEY = "secret";

    @Test
    void testAuthenticateAcceptsAdminApiKey() {
        Authentication authentication = newProvider(null).authenticate(newToken());

        assertThat(authentication).isInstanceOf(PlatformApiKeyAuthenticationToken.class);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("admin@localhost.com");
        assertThat(authentication.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    void testAuthenticateAcceptsAdminApiKeyInAnyEnvironmentAndKeepsTheRequestedOne() {
        Authentication authentication = newProvider(null).authenticate(
            new PlatformApiKeyAuthenticationToken(2, SECRET_KEY, "public"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(((PlatformApiKeyAuthenticationToken) authentication).getEnvironmentId()).isEqualTo(2);
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

    @Test
    void testAuthenticateRejectsAnApiKeyWithoutOwner() {
        assertThatThrownBy(() -> newProvider(null, null).authenticate(newToken()))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("API key has no owner");
    }

    @Test
    void testInactiveUserIsRejectedWithoutTheSecretKeyInTheMessage() {
        assertThatThrownBy(() -> newProvider(null, 1L, false, true).authenticate(newToken()))
            .isInstanceOf(UserNotActivatedException.class)
            .hasMessage("User admin@localhost.com was not activated");
    }

    @Test
    void testMissingUserIsRejectedWithoutTheSecretKeyInTheMessage() {
        assertThatThrownBy(() -> newProvider(null, 1L, true, false).authenticate(newToken()))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("User 1 was not found in the database");
    }

    @Test
    void testUnknownSecretKeyIsRejectedAsBadCredentials() {
        assertThatThrownBy(
            () -> newProvider(null).authenticate(new PlatformApiKeyAuthenticationToken(0, "unknown", "public")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Unknown API secret key");
    }

    @Test
    void testAuthenticatedTokenCarriesTheApiKeyId() {
        Authentication authentication = newProvider(null).authenticate(newToken());

        assertThat(((PlatformApiKeyAuthenticationToken) authentication).getApiKeyId()).isEqualTo(7L);
    }

    private static PlatformApiKeyAuthenticationToken newToken() {
        return new PlatformApiKeyAuthenticationToken((int) ENVIRONMENT_ID, SECRET_KEY, "public");
    }

    private static PlatformApiKeyAuthenticationProvider newProvider(PlatformType platformType) {
        return newProvider(platformType, 1L);
    }

    private static PlatformApiKeyAuthenticationProvider newProvider(PlatformType platformType, Long userId) {
        return newProvider(platformType, userId, true, true);
    }

    private static PlatformApiKeyAuthenticationProvider newProvider(
        PlatformType platformType, Long userId, boolean activated, boolean userExists) {

        ApiKey apiKey = mock(ApiKey.class);

        when(apiKey.getId()).thenReturn(7L);

        when(apiKey.getType()).thenReturn(platformType);
        when(apiKey.getUserId()).thenReturn(userId);

        ApiKeyService apiKeyService = mock(ApiKeyService.class);

        when(apiKeyService.fetchApiKey(SECRET_KEY)).thenReturn(Optional.of(apiKey));

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(activated);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getPassword()).thenReturn("password");
        when(user.getAuthorityIds()).thenReturn(List.of(1L));

        UserService userService = mock(UserService.class);

        when(userService.fetchUser(anyLong())).thenReturn(userExists ? Optional.of(user) : Optional.empty());

        Authority authority = mock(Authority.class);

        when(authority.getName()).thenReturn("ROLE_ADMIN");

        AuthorityService authorityService = mock(AuthorityService.class);

        when(authorityService.fetchAuthority(anyLong())).thenReturn(Optional.of(authority));

        return new PlatformApiKeyAuthenticationProvider(apiKeyService, authorityService, userService);
    }
}
