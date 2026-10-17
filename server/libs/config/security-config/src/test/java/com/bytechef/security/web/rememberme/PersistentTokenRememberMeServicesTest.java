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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.PersistentTokenService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.config.RememberMeKey;
import com.bytechef.security.web.authentication.TenantUser;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;

/**
 * @author Ivica Cardic
 */
class PersistentTokenRememberMeServicesTest {

    private static final String LOGIN = "jane";
    private static final String FIRST_TENANT_ID = "000002";
    private static final String SECOND_TENANT_ID = "000003";

    private final PersistentTokenService persistentTokenService = mock(PersistentTokenService.class);
    private final List<String> savedTokenTenantIds = new ArrayList<>();
    private final List<PersistentToken> savedTokens = new ArrayList<>();
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

        when(userService.fetchUserByLogin(LOGIN)).thenAnswer(
            invocation -> Optional.of(createUser(TenantContext.getCurrentTenantId())));

        doAnswer(invocation -> {
            savedTokens.add(invocation.getArgument(0));
            savedTokenTenantIds.add(TenantContext.getCurrentTenantId());

            return null;
        }).when(persistentTokenService)
            .save(any(PersistentToken.class));

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserLogin(LOGIN)).thenReturn(List.of(FIRST_TENANT_ID, SECOND_TENANT_ID));
    }

    @Test
    void testLoginSuccessIssuesTheTokenInTheTenantTheUserWasAuthenticatedAgainst() {
        MockHttpServletRequest request = createRememberMeRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        persistentTokenRememberMeServices.loginSuccess(
            request, response, createAuthentication(new TenantUser(LOGIN, "", List.of(), SECOND_TENANT_ID)));

        assertThat(savedTokenTenantIds).containsExactly(SECOND_TENANT_ID);
        assertThat(savedTokens.getFirst()
            .getUserId()).isEqualTo(Long.parseLong(SECOND_TENANT_ID));
        assertThat(getRememberMeCookieTenantId(response)).isEqualTo(SECOND_TENANT_ID);
    }

    @Test
    void testLoginSuccessUsesTheSessionTenantWhenThePrincipalCarriesNone() {
        MockHttpServletRequest request = createRememberMeRequest();

        request.getSession()
            .setAttribute(TenantConstants.CURRENT_TENANT_ID, SECOND_TENANT_ID);

        MockHttpServletResponse response = new MockHttpServletResponse();

        persistentTokenRememberMeServices.loginSuccess(request, response, createAuthentication(createPlainUser()));

        assertThat(savedTokenTenantIds).containsExactly(SECOND_TENANT_ID);
        assertThat(getRememberMeCookieTenantId(response)).isEqualTo(SECOND_TENANT_ID);
    }

    @Test
    void testLoginSuccessUsesTheOnlyTenantOfTheLogin() {
        when(tenantService.getTenantIdsByUserLogin(LOGIN)).thenReturn(List.of(SECOND_TENANT_ID));

        MockHttpServletResponse response = new MockHttpServletResponse();

        persistentTokenRememberMeServices.loginSuccess(
            createRememberMeRequest(), response, createAuthentication(createPlainUser()));

        assertThat(savedTokenTenantIds).containsExactly(SECOND_TENANT_ID);
    }

    @Test
    void testLoginSuccessIssuesNoTokenWhenTheTenantIsAmbiguous() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        persistentTokenRememberMeServices.loginSuccess(
            createRememberMeRequest(), response, createAuthentication(createPlainUser()));

        verify(persistentTokenService, never()).save(any(PersistentToken.class));

        assertThat(getRememberMeCookies(response)).isEmpty();
    }

    @Test
    void testLoginSuccessInSingleTenantModeIssuesTheToken() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        persistentTokenRememberMeServices.loginSuccess(
            createRememberMeRequest(), new MockHttpServletResponse(), createAuthentication(createPlainUser()));

        assertThat(savedTokens).hasSize(1);
    }

    private static Authentication createAuthentication(UserDetails userDetails) {
        return UsernamePasswordAuthenticationToken.authenticated(userDetails, null, List.of());
    }

    private static UserDetails createPlainUser() {
        return new org.springframework.security.core.userdetails.User(LOGIN, "", List.of());
    }

    private static MockHttpServletRequest createRememberMeRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/authentication");

        request.setParameter(AbstractRememberMeServices.DEFAULT_PARAMETER, "true");
        request.addHeader("User-Agent", "Test agent");

        return request;
    }

    private static User createUser(String tenantId) {
        User user = new User();

        user.setId(TenantContext.DEFAULT_TENANT_ID.equals(tenantId) ? 1L : Long.parseLong(tenantId));
        user.setLogin(LOGIN);

        return user;
    }

    private static List<Cookie> getRememberMeCookies(MockHttpServletResponse response) {
        return Arrays.stream(response.getCookies())
            .filter(cookie -> AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY.equals(
                cookie.getName()))
            .toList();
    }

    private static String getRememberMeCookieTenantId(MockHttpServletResponse response) {
        Cookie cookie = getRememberMeCookies(response).getLast();

        String decodedValue = EncodingUtils.base64DecodeToString(cookie.getValue());

        String[] cookieTokens = decodedValue.split(":");

        return cookieTokens[2];
    }
}
