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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class PlatformApiKeyAuthenticationProviderTest {

    private static final long ENVIRONMENT_ID = 1L;
    private static final String SECRET_KEY = "secret-key";
    private static final long USER_ID = 7L;

    private ApiKeyService apiKeyService;
    private PlatformApiKeyAuthenticationProvider platformApiKeyAuthenticationProvider;
    private UserService userService;

    @BeforeEach
    void setUp() {
        apiKeyService = mock(ApiKeyService.class);
        userService = mock(UserService.class);

        platformApiKeyAuthenticationProvider =
            new PlatformApiKeyAuthenticationProvider(apiKeyService, mock(AuthorityService.class), userService);
    }

    @Test
    void testRejectsAnAutomationKey() {
        when(apiKeyService.getApiKey(SECRET_KEY, ENVIRONMENT_ID)).thenReturn(createApiKey(PlatformType.AUTOMATION));

        assertThatThrownBy(() -> platformApiKeyAuthenticationProvider
            .authenticate(new PlatformApiKeyAuthenticationToken((int) ENVIRONMENT_ID, SECRET_KEY, "tenant")))
                .isInstanceOf(BadCredentialsException.class);

        verify(userService, never()).fetchUser(anyLong());
    }

    @Test
    void testRejectsAnEmbeddedKey() {
        when(apiKeyService.getApiKey(SECRET_KEY, ENVIRONMENT_ID)).thenReturn(createApiKey(PlatformType.EMBEDDED));

        assertThatThrownBy(() -> platformApiKeyAuthenticationProvider
            .authenticate(new PlatformApiKeyAuthenticationToken((int) ENVIRONMENT_ID, SECRET_KEY, "tenant")))
                .isInstanceOf(BadCredentialsException.class);

        verify(userService, never()).fetchUser(anyLong());
    }

    @Test
    void testAcceptsAPlatformKey() {
        User user = mock(User.class);

        when(user.getAuthorityIds()).thenReturn(List.of());
        when(user.getLogin()).thenReturn("login");
        when(user.getPassword()).thenReturn("password");
        when(user.isActivated()).thenReturn(true);
        when(apiKeyService.getApiKey(SECRET_KEY, ENVIRONMENT_ID)).thenReturn(createApiKey(null));
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        Authentication authentication = platformApiKeyAuthenticationProvider.authenticate(
            new PlatformApiKeyAuthenticationToken((int) ENVIRONMENT_ID, SECRET_KEY, "tenant"));

        assertThat(authentication.getName()).isEqualTo("login");
    }

    private static ApiKey createApiKey(PlatformType type) {
        ApiKey apiKey = new ApiKey();

        apiKey.setName("key");
        apiKey.setSecretKey(SECRET_KEY);
        apiKey.setType(type);
        apiKey.setUserId(USER_ID);

        return apiKey;
    }
}
