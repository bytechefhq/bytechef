/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class AutomationApiKeyAuthenticationProviderTest {

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private AuthorityService authorityService;

    @Mock
    private UserService userService;

    @Test
    void testAnEmbeddedKeyIsRejected() {
        ApiKey apiKey = new ApiKey();

        apiKey.setType(PlatformType.EMBEDDED);

        when(apiKeyService.getApiKey("btc_x", 0L)).thenReturn(apiKey);

        AutomationApiKeyAuthenticationProvider provider = new AutomationApiKeyAuthenticationProvider(
            apiKeyService, authorityService, userService);

        AutomationApiKeyAuthenticationToken automationApiKeyAuthenticationToken =
            new AutomationApiKeyAuthenticationToken(0L, "btc_x", "tenant");

        assertThrows(
            BadCredentialsException.class, () -> provider.authenticate(automationApiKeyAuthenticationToken));

        verifyNoInteractions(userService);
    }

    @Test
    void testAnUntypedAdminKeyIsRejected() {
        ApiKey apiKey = new ApiKey();

        when(apiKeyService.getApiKey("btc_admin", 0L)).thenReturn(apiKey);

        AutomationApiKeyAuthenticationProvider provider = new AutomationApiKeyAuthenticationProvider(
            apiKeyService, authorityService, userService);

        AutomationApiKeyAuthenticationToken automationApiKeyAuthenticationToken =
            new AutomationApiKeyAuthenticationToken(0L, "btc_admin", "tenant");

        assertThrows(
            BadCredentialsException.class, () -> provider.authenticate(automationApiKeyAuthenticationToken));

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

        when(apiKeyService.getApiKey("btc_ok", 0L)).thenReturn(apiKey);
        when(userService.fetchUser(1L)).thenReturn(Optional.of(user));
        when(authorityService.fetchAuthority(anyLong())).thenReturn(Optional.of(authority));

        AutomationApiKeyAuthenticationProvider provider = new AutomationApiKeyAuthenticationProvider(
            apiKeyService, authorityService, userService);

        AutomationApiKeyAuthenticationToken automationApiKeyAuthenticationToken =
            new AutomationApiKeyAuthenticationToken(0L, "btc_ok", "tenant");

        Authentication authentication = provider.authenticate(automationApiKeyAuthenticationToken);

        assertThat(authentication.isAuthenticated()).isTrue();
    }
}
