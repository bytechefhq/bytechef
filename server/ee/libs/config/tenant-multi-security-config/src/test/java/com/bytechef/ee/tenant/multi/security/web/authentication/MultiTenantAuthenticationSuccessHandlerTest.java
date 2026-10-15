/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.tenant.multi.security.MultiTenantUserDetailsService;
import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.authentication.TenantUserDetails;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class MultiTenantAuthenticationSuccessHandlerTest {

    @Test
    @SuppressWarnings("unchecked")
    void testStoresTheTenantOfAUserWhoseLoginIsNotTheirEmail() {
        ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider =
            mock(ObjectProvider.class);

        MultiTenantAuthenticationSuccessHandler multiTenantAuthenticationSuccessHandler =
            new MultiTenantAuthenticationSuccessHandler(twoFactorAuthenticationCustomizerProvider);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TenantUserDetails principal = new TenantUserDetails("invited_member", "password", List.of(), "000001");

        multiTenantAuthenticationSuccessHandler.onAuthenticationSuccess(
            request, response, new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        assertThat(request.getSession()
            .getAttribute(TenantConstants.CURRENT_TENANT_ID)).isEqualTo("000001");
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEmailSignInLandsInTheTenantWhosePasswordWasCheckedWhenTheLoginExistsInTwoTenants() {
        TenantService tenantService = mock(TenantService.class);
        UserService tenantOneUserService = mock(UserService.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        User tenantOneUser = new User();

        tenantOneUser.setActivated(true);
        tenantOneUser.setAuthorityIds(List.of());
        tenantOneUser.setEmail("shared@tenant-one.com");
        tenantOneUser.setLogin("shared");
        tenantOneUser.setPassword("tenant-one-password");

        when(applicationContext.getBean(UserService.class)).thenReturn(tenantOneUserService);
        when(tenantService.getTenantIdsByUserEmail("shared@tenant-one.com")).thenReturn(List.of("000001"));
        when(tenantService.getTenantIdsByUserLogin("shared")).thenReturn(List.of("000002", "000001"));
        when(tenantOneUserService.fetchUserByEmail("shared@tenant-one.com"))
            .thenAnswer(invocation -> "000001".equals(TenantContext.getCurrentTenantId())
                ? Optional.of(tenantOneUser) : Optional.empty());

        MultiTenantUserDetailsService multiTenantUserDetailsService =
            new MultiTenantUserDetailsService(mock(AuthorityService.class), tenantService);

        multiTenantUserDetailsService.setApplicationContext(applicationContext);

        UserDetails principal = multiTenantUserDetailsService.loadUserByUsername("shared@tenant-one.com");

        MultiTenantAuthenticationSuccessHandler multiTenantAuthenticationSuccessHandler =
            new MultiTenantAuthenticationSuccessHandler(mock(ObjectProvider.class));

        MockHttpServletRequest request = new MockHttpServletRequest();

        multiTenantAuthenticationSuccessHandler.onAuthenticationSuccess(
            request, new MockHttpServletResponse(),
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertThat(principal.getUsername()).isEqualTo("shared");
        assertThat(request.getSession()
            .getAttribute(TenantConstants.CURRENT_TENANT_ID)).isEqualTo("000001");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRejectsAPrincipalWithoutATenant() {
        MultiTenantAuthenticationSuccessHandler multiTenantAuthenticationSuccessHandler =
            new MultiTenantAuthenticationSuccessHandler(mock(ObjectProvider.class));

        org.springframework.security.core.userdetails.User principal =
            new org.springframework.security.core.userdetails.User("invited_member", "password", List.of());

        assertThatThrownBy(() -> multiTenantAuthenticationSuccessHandler.onAuthenticationSuccess(
            new MockHttpServletRequest(), new MockHttpServletResponse(),
            new UsernamePasswordAuthenticationToken(principal, null, List.of())))
                .isInstanceOf(IllegalStateException.class);
    }
}
