/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class MultiTenantAuthenticationSuccessHandlerTest {

    @Test
    @SuppressWarnings("unchecked")
    void testStoresTheTenantOfAUserWhoseLoginIsNotTheirEmail() {
        TenantService tenantService = mock(TenantService.class);
        ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider =
            mock(ObjectProvider.class);

        when(tenantService.getTenantIdsByUserLogin("invited_member")).thenReturn(List.of("000001"));

        MultiTenantAuthenticationSuccessHandler multiTenantAuthenticationSuccessHandler =
            new MultiTenantAuthenticationSuccessHandler(tenantService, twoFactorAuthenticationCustomizerProvider);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        User principal = new User("invited_member", "password", List.of());

        multiTenantAuthenticationSuccessHandler.onAuthenticationSuccess(
            request, response, new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        assertThat(request.getSession()
            .getAttribute(TenantConstants.CURRENT_TENANT_ID)).isEqualTo("000001");
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }
}
