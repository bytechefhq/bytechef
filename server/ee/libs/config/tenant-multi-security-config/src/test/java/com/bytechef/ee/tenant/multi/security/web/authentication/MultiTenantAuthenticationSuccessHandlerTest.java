/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.security.web.authentication.TenantUser;
import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class MultiTenantAuthenticationSuccessHandlerTest {

    private static final String AUTHENTICATED_TENANT_ID = "000003";

    private final TenantService tenantService = mock(TenantService.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider =
        mock(ObjectProvider.class);

    private final MultiTenantAuthenticationSuccessHandler multiTenantAuthenticationSuccessHandler =
        new MultiTenantAuthenticationSuccessHandler(tenantService, twoFactorAuthenticationCustomizerProvider);

    @Test
    void testSessionGetsTheTenantThePasswordWasCheckedAgainst() {
        when(tenantService.getTenantIdsByUserEmail(anyString())).thenReturn(List.of("000004"));

        TenantUser tenantUser = new TenantUser(
            "jane@example.com", String.valueOf(UUID.randomUUID()), List.of(), AUTHENTICATED_TENANT_ID);

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
            tenantUser, null, tenantUser.getAuthorities());

        MockHttpServletRequest request = new MockHttpServletRequest();

        multiTenantAuthenticationSuccessHandler.onAuthenticationSuccess(
            request, new MockHttpServletResponse(), authentication);

        assertThat(request.getSession()
            .getAttribute(TenantConstants.CURRENT_TENANT_ID)).isEqualTo(AUTHENTICATED_TENANT_ID);
    }
}
