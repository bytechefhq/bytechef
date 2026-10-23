/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.PersistentTokenService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.config.RememberMeKey;
import com.bytechef.security.web.authentication.TenantUserDetails;
import com.bytechef.security.web.rememberme.PersistentTokenRememberMeServices;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.Cookie;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class MultiTenantUserDetailsServiceTest {

    private static final String SERIES = "series";
    private static final String TENANT_ONE_ID = "000001";
    private static final String TENANT_TWO_ID = "000002";
    private static final String TOKEN_VALUE = "token-value";

    @Test
    void testAutoLoginLoadsTheAuthoritiesOfTheCookieTenantUserWhenTheLoginExistsInTwoTenants() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        AuthorityService authorityService = mock(AuthorityService.class);
        PersistentTokenService persistentTokenService = mock(PersistentTokenService.class);
        RememberMeKey rememberMeKey = mock(RememberMeKey.class);
        TenantService tenantService = mock(TenantService.class);
        UserService userService = mock(UserService.class);

        User tenantOneUser = createUser(1L, 1L);
        User tenantTwoUser = createUser(2L, 2L);

        PersistentToken persistentToken = new PersistentToken();

        persistentToken.setSeries(SERIES);
        persistentToken.setTokenDate(LocalDate.now());
        persistentToken.setTokenValue(TOKEN_VALUE);
        persistentToken.setUser(tenantTwoUser);

        when(applicationContext.getBean(UserService.class)).thenReturn(userService);
        when(authorityService.fetchAuthority(1L)).thenReturn(Optional.of(createAuthority(1L, "ROLE_ADMIN")));
        when(authorityService.fetchAuthority(2L)).thenReturn(Optional.of(createAuthority(2L, "ROLE_USER")));
        when(persistentTokenService.fetchPersistentToken(SERIES))
            .thenAnswer(invocation -> TENANT_TWO_ID.equals(TenantContext.getCurrentTenantId())
                ? Optional.of(persistentToken) : Optional.empty());
        when(persistentTokenService.save(any(PersistentToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(rememberMeKey.getKey()).thenReturn("remember-me-key");
        when(tenantService.getTenantIdsByUserLogin("shared")).thenReturn(List.of(TENANT_ONE_ID, TENANT_TWO_ID));
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(userService.fetchUserByLogin("shared"))
            .thenAnswer(invocation -> TENANT_ONE_ID.equals(TenantContext.getCurrentTenantId())
                ? Optional.of(tenantOneUser) : Optional.of(tenantTwoUser));
        when(userService.getUser(2L))
            .thenAnswer(invocation -> TENANT_TWO_ID.equals(TenantContext.getCurrentTenantId())
                ? tenantTwoUser : tenantOneUser);

        MultiTenantUserDetailsService multiTenantUserDetailsService =
            new MultiTenantUserDetailsService(authorityService, tenantService);

        multiTenantUserDetailsService.setApplicationContext(applicationContext);

        PersistentTokenRememberMeServices persistentTokenRememberMeServices = new PersistentTokenRememberMeServices(
            rememberMeKey, multiTenantUserDetailsService, persistentTokenService, tenantService);

        persistentTokenRememberMeServices.setApplicationContext(applicationContext);

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader("User-Agent", "test-agent");

        request.setCookies(new Cookie("remember-me", encodeCookie(SERIES, TOKEN_VALUE, TENANT_TWO_ID)));

        Authentication authentication = persistentTokenRememberMeServices.autoLogin(
            request, new MockHttpServletResponse());

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");
        assertThat(authentication.getPrincipal())
            .isInstanceOfSatisfying(
                TenantUserDetails.class,
                tenantUserDetails -> assertThat(tenantUserDetails.getTenantId()).isEqualTo(TENANT_TWO_ID));
    }

    private static Authority createAuthority(long id, String name) {
        Authority authority = new Authority();

        authority.setId(id);
        authority.setName(name);

        return authority;
    }

    private static User createUser(long id, long authorityId) {
        User user = new User();

        user.setActivated(true);
        user.setAuthorityIds(List.of(authorityId));
        user.setId(id);
        user.setLogin("shared");
        user.setPassword("password");

        return user;
    }

    private static String encodeCookie(String... cookieTokens) {
        StringBuilder cookieValue = new StringBuilder();

        for (String cookieToken : cookieTokens) {
            if (!cookieValue.isEmpty()) {
                cookieValue.append(':');
            }

            cookieValue.append(URLEncoder.encode(cookieToken, StandardCharsets.UTF_8));
        }

        Base64.Encoder encoder = Base64.getEncoder();

        return encoder.encodeToString(
            cookieValue.toString()
                .getBytes(StandardCharsets.UTF_8));
    }
}
