/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.domain.IdentityProviderDomain;
import com.bytechef.ee.security.sso.IdentityProviderLocator.LocatedIdentityProvider;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SsoUserProvisionerTest {

    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final UserService userService = mock(UserService.class);
    private final SsoUserProvisioner ssoUserProvisioner = new SsoUserProvisioner(authorityService, userService);

    @Test
    void testProvisionsInsideTheIdentityProvidersTenantWithItsSettings() {
        AtomicReference<String> tenantIdDuringLookup = new AtomicReference<>();

        when(
            userService.findOrCreateSocialUser(
                anyString(), nullable(String.class), nullable(String.class), nullable(String.class), anyString(),
                anyString(), anyBoolean(), anyString()))
                    .thenAnswer(invocation -> {
                        tenantIdDuringLookup.set(TenantContext.getCurrentTenantId());

                        User user = new User();

                        user.setLogin("someone");
                        user.setAuthorityIds(List.of());

                        return user;
                    });

        ssoUserProvisioner.provision(
            new LocatedIdentityProvider("000003", identityProvider("acme.com")), "someone@ACME.com", "Some", "One",
            null, "subject-1");

        assertThat(tenantIdDuringLookup.get()).isEqualTo("000003");

        verify(userService).findOrCreateSocialUser(
            "someone@ACME.com", "Some", "One", null, "SSO-7", "subject-1", true, "ROLE_USER");
    }

    @Test
    void testRefusesAnEmailOutsideTheIdentityProvidersDomains() {
        assertThatThrownBy(
            () -> ssoUserProvisioner.provision(
                new LocatedIdentityProvider("000003", identityProvider("acme.com")), "admin@other.com", null, null,
                null, "subject-1"))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .extracting(exception -> ((OAuth2AuthenticationException) exception).getError()
                        .getErrorCode())
                    .isEqualTo(SsoUserProvisioner.EMAIL_DOMAIN_NOT_ALLOWED_ERROR_CODE);

        verify(userService, never()).findOrCreateSocialUser(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void testKeysUsersPerIdentityProvider() {
        IdentityProvider samlIdentityProvider = identityProvider("acme.com");

        samlIdentityProvider.setType("SAML");

        assertThat(SsoUserProvisioner.getAuthProvider(identityProvider("acme.com"))).isEqualTo("SSO-7");
        assertThat(SsoUserProvisioner.getAuthProvider(samlIdentityProvider)).isEqualTo("SAML-7");
    }

    @Test
    void testRefusedProvisioningBecomesAnAuthenticationFailure() {
        when(
            userService.findOrCreateSocialUser(
                anyString(), nullable(String.class), nullable(String.class), nullable(String.class), anyString(),
                anyString(), eq(true), anyString()))
                    .thenThrow(new IllegalStateException("Auto-provisioning is disabled"));

        assertThatThrownBy(
            () -> ssoUserProvisioner.provision(
                new LocatedIdentityProvider("000003", identityProvider("acme.com")), "someone@acme.com", null, null,
                null, "subject-1"))
                    .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private static IdentityProvider identityProvider(String domain) {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setAutoProvision(true);
        identityProvider.setDomains(Set.of(new IdentityProviderDomain(domain)));
        identityProvider.setId(7L);
        identityProvider.setName("Acme");
        identityProvider.setType("OIDC");

        return identityProvider;
    }
}
