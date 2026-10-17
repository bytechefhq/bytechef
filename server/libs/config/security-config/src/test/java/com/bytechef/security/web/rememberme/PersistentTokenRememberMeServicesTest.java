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

package com.bytechef.security.web.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.PersistentTokenService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.config.RememberMeKey;
import com.bytechef.tenant.service.TenantService;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;

/**
 * @author Ivica Cardic
 */
class PersistentTokenRememberMeServicesTest {

    private final PersistentTokenService persistentTokenService = mock(PersistentTokenService.class);
    private final TenantService tenantService = mock(TenantService.class);
    private final UserService userService = mock(UserService.class);
    private PersistentTokenRememberMeServices persistentTokenRememberMeServices;

    @BeforeEach
    void beforeEach() {
        RememberMeKey rememberMeKey = mock(RememberMeKey.class);

        when(rememberMeKey.getKey()).thenReturn("remember-me-test-key");

        persistentTokenRememberMeServices = new PersistentTokenRememberMeServices(
            rememberMeKey, mock(UserDetailsService.class), persistentTokenService, tenantService);

        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(applicationContext.getBean(UserService.class)).thenReturn(userService);

        persistentTokenRememberMeServices.setApplicationContext(applicationContext);

        User user = new User();

        user.setId(1L);
        user.setLogin("jane");

        when(userService.fetchUserByLogin("jane")).thenReturn(Optional.of(user));
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserLogin("jane")).thenReturn(List.of("000002"));
    }

    @Test
    void testLoginFailAfterLoginSuccessDeletesTheIssuedTokenAndCancelsTheCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/authentication");

        request.setParameter(AbstractRememberMeServices.DEFAULT_PARAMETER, "true");
        request.addHeader("User-Agent", "Test agent");

        MockHttpServletResponse response = new MockHttpServletResponse();

        persistentTokenRememberMeServices.loginSuccess(request, response, createAuthentication());

        ArgumentCaptor<PersistentToken> persistentTokenArgumentCaptor = ArgumentCaptor.forClass(PersistentToken.class);

        verify(persistentTokenService).save(persistentTokenArgumentCaptor.capture());

        PersistentToken persistentToken = persistentTokenArgumentCaptor.getValue();

        persistentTokenRememberMeServices.loginFail(request, response);

        verify(persistentTokenService).delete(persistentToken.getSeries());

        List<Cookie> rememberMeCookies = Arrays.stream(response.getCookies())
            .filter(cookie -> AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY.equals(
                cookie.getName()))
            .toList();

        assertThat(rememberMeCookies.getLast()
            .getMaxAge()).isZero();
    }

    @Test
    void testLoginFailWithoutIssuedTokenDeletesNothing() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/authentication");

        persistentTokenRememberMeServices.loginFail(request, new MockHttpServletResponse());

        verify(persistentTokenService, never()).delete(anyString());
    }

    private static Authentication createAuthentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
            new org.springframework.security.core.userdetails.User("jane", "", List.of()), null, List.of());
    }
}
