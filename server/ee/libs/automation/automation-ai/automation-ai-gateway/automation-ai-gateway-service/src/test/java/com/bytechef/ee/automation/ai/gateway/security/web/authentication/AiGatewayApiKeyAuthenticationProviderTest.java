/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.security.web.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class AiGatewayApiKeyAuthenticationProviderTest {

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private AuthorityService authorityService;

    @Mock
    private UserService userService;

    private AiGatewayApiKeyAuthenticationProvider authenticationProvider;

    @BeforeEach
    void setUp() {
        authenticationProvider = new AiGatewayApiKeyAuthenticationProvider(
            apiKeyService, authorityService, userService);
    }

    @Test
    void testAuthenticateWithValidApiKey() {
        String secretKey = "test-secret-key";
        long environmentId = 1L;

        AiGatewayApiKeyAuthenticationToken inputToken =
            new AiGatewayApiKeyAuthenticationToken(environmentId, secretKey, "tenant-1");

        ApiKey apiKey = new ApiKey();

        apiKey.setUserId(100L);

        when(apiKeyService.getApiKey(secretKey, environmentId)).thenReturn(apiKey);

        User user = new User();

        user.setLogin("admin@localhost.com");
        user.setPassword("encoded-password");
        user.setActivated(true);
        user.setAuthorityIds(List.of(1L));

        when(userService.fetchUser(100L)).thenReturn(Optional.of(user));

        Authority authority = new Authority();

        authority.setId(1L);
        authority.setName("ROLE_ADMIN");

        when(authorityService.fetchAuthority(1L)).thenReturn(Optional.of(authority));

        Authentication result = authenticationProvider.authenticate(inputToken);

        assertTrue(result.isAuthenticated());
        assertTrue(result instanceof AiGatewayApiKeyAuthenticationToken);
        assertEquals(1, result.getAuthorities()
            .size());
    }

    @Test
    void testAuthenticateWithInvalidApiKeyThrowsBadCredentials() {
        String secretKey = "invalid-key";
        long environmentId = 1L;

        AiGatewayApiKeyAuthenticationToken inputToken =
            new AiGatewayApiKeyAuthenticationToken(environmentId, secretKey, "tenant-1");

        when(apiKeyService.getApiKey(secretKey, environmentId)).thenThrow(new IllegalArgumentException("Not found"));

        assertThrows(BadCredentialsException.class, () -> authenticationProvider.authenticate(inputToken));
    }

    @Test
    void testAuthenticateWithInactiveUserThrowsUserNotActivated() {
        String secretKey = "test-secret-key";
        long environmentId = 1L;

        AiGatewayApiKeyAuthenticationToken inputToken =
            new AiGatewayApiKeyAuthenticationToken(environmentId, secretKey, "tenant-1");

        ApiKey apiKey = new ApiKey();

        apiKey.setUserId(100L);

        when(apiKeyService.getApiKey(secretKey, environmentId)).thenReturn(apiKey);

        User user = new User();

        user.setLogin("inactive@localhost.com");
        user.setPassword("encoded-password");
        user.setActivated(false);

        when(userService.fetchUser(100L)).thenReturn(Optional.of(user));

        assertThrows(UserNotActivatedException.class, () -> authenticationProvider.authenticate(inputToken));
    }

    @Test
    void testAuthenticateWithMissingUserThrowsUsernameNotFound() {
        String secretKey = "test-secret-key";
        long environmentId = 1L;

        AiGatewayApiKeyAuthenticationToken inputToken =
            new AiGatewayApiKeyAuthenticationToken(environmentId, secretKey, "tenant-1");

        ApiKey apiKey = new ApiKey();

        apiKey.setUserId(999L);

        when(apiKeyService.getApiKey(secretKey, environmentId)).thenReturn(apiKey);
        when(userService.fetchUser(999L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authenticationProvider.authenticate(inputToken));
    }

    @Test
    void testSupportsCorrectTokenType() {
        assertTrue(authenticationProvider.supports(AiGatewayApiKeyAuthenticationToken.class));
        assertFalse(authenticationProvider.supports(Authentication.class));
    }
}
