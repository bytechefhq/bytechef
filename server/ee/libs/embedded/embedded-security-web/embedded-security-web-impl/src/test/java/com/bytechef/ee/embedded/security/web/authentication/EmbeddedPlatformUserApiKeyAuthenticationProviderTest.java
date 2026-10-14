/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedPlatformUserApiKeyAuthenticationProviderTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final String SECRET_KEY = "secret-key";
    private static final long USER_ID = 7L;

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final UserService userService = mock(UserService.class);

    private final EmbeddedPlatformUserApiKeyAuthenticationProvider provider =
        new EmbeddedPlatformUserApiKeyAuthenticationProvider(apiKeyService, authorityService, userService);

    @BeforeEach
    void beforeEach() {
        Authority authority = mock(Authority.class);

        when(authority.getName()).thenReturn("ROLE_ADMIN");
        when(authorityService.fetchAuthority(1L)).thenReturn(Optional.of(authority));
    }

    @Test
    void testAuthenticatesTypedKeyAsItsOwnUser() {
        stubApiKey(PlatformType.EMBEDDED);
        stubUser(true);

        Authentication authentication = provider.authenticate(createToken());

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("admin@localhost.com");
        assertThat(authentication.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    void testRejectsKeyWithoutType() {
        stubApiKey(null);
        stubUser(true);

        assertThatThrownBy(() -> provider.authenticate(createToken()))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testRejectsUnknownKey() {
        when(apiKeyService.getApiKey(SECRET_KEY, ENVIRONMENT_ID)).thenThrow(new IllegalArgumentException("unknown"));

        assertThatThrownBy(() -> provider.authenticate(createToken()))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testRejectsNotActivatedUser() {
        stubApiKey(PlatformType.AUTOMATION);
        stubUser(false);

        assertThatThrownBy(() -> provider.authenticate(createToken()))
            .isInstanceOf(UserNotActivatedException.class);
    }

    @Test
    void testSupportsOnlyItsOwnToken() {
        assertThat(provider.supports(EmbeddedPlatformUserApiKeyAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(Authentication.class)).isFalse();
    }

    private static EmbeddedPlatformUserApiKeyAuthenticationToken createToken() {
        return new EmbeddedPlatformUserApiKeyAuthenticationToken(ENVIRONMENT_ID, SECRET_KEY, "tenant");
    }

    private void stubApiKey(PlatformType type) {
        ApiKey apiKey = new ApiKey();

        apiKey.setType(type);
        apiKey.setUserId(USER_ID);

        when(apiKeyService.getApiKey(SECRET_KEY, ENVIRONMENT_ID)).thenReturn(apiKey);
    }

    private void stubUser(boolean activated) {
        User user = mock(User.class);

        when(user.isActivated()).thenReturn(activated);
        when(user.getAuthorityIds()).thenReturn(List.of(1L));
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getPassword()).thenReturn("password");
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));
    }
}
