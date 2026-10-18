/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.domain.IdentityProviderRegistrationId;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.ee.security.sso.IdentityProviderLocator.LocatedIdentityProvider;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IdentityProviderLocatorTest {

    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
    private final TenantService tenantService = mock(TenantService.class);
    private final IdentityProviderLocator identityProviderLocator =
        new IdentityProviderLocator(identityProviderService, tenantService);

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testRegistrationIdKeepsTheShortFormInTheDefaultTenant() {
        IdentityProvider identityProvider = identityProvider(7L, "OIDC");

        assertThat(IdentityProviderRegistrationId.of(identityProvider)
            .value()).isEqualTo("sso-7");
    }

    @Test
    void testRegistrationIdNamesTheTenantOutsideTheDefaultTenant() {
        IdentityProvider identityProvider = identityProvider(7L, "SAML");

        String registrationId = IdentityProviderLocator.callWithTenantId(
            "000003", () -> IdentityProviderRegistrationId.of(identityProvider)
                .value());

        assertThat(registrationId).isEqualTo("saml-000003-7");
        assertThat(IdentityProviderRegistrationId.parse(registrationId))
            .contains(new IdentityProviderRegistrationId("saml-", "000003", 7L));
    }

    @Test
    void testMalformedRegistrationIdsAreRejected() {
        assertThat(IdentityProviderRegistrationId.parse("sso-abc")).isEmpty();
        assertThat(IdentityProviderRegistrationId.parse("sso-")).isEmpty();
        assertThat(IdentityProviderRegistrationId.parse("sso-x-7")).isEmpty();
        assertThat(IdentityProviderRegistrationId.parse("sso-000001-")).isEmpty();
        assertThat(IdentityProviderRegistrationId.parse("google")).isEmpty();
        assertThat(IdentityProviderRegistrationId.parse(null)).isEmpty();
    }

    @Test
    void testFindByRegistrationIdLooksUpInsideTheNamedTenant() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(identityProviderService.getIdentityProvider(7L)).thenAnswer(invocation -> {
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo("000003");

            return identityProvider(7L, "OIDC");
        });

        Optional<LocatedIdentityProvider> locatedIdentityProvider =
            identityProviderLocator.findByRegistrationId("sso-000003-7");

        assertThat(locatedIdentityProvider).map(LocatedIdentityProvider::tenantId)
            .contains("000003");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testFindByRegistrationIdRejectsTheShortFormInMultiTenantMode() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);

        assertThat(identityProviderLocator.findByRegistrationId("sso-7")).isEmpty();

        verify(identityProviderService, never()).getIdentityProvider(anyLong());
    }

    @Test
    void testFindByRegistrationIdRejectsATenantInSingleTenantMode() {
        assertThat(identityProviderLocator.findByRegistrationId("sso-000003-7")).isEmpty();

        verify(identityProviderService, never()).getIdentityProvider(anyLong());
    }

    @Test
    void testFindByRegistrationIdOfUnknownProviderIsEmpty() {
        when(identityProviderService.getIdentityProvider(9L)).thenThrow(new IllegalArgumentException("not found"));

        assertThat(identityProviderLocator.findByRegistrationId("sso-9")).isEmpty();
    }

    @Test
    void testFindByDomainScansTenantsInMultiTenantMode() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIds()).thenReturn(List.of("000001", "000002"));
        when(identityProviderService.fetchByDomain(anyString())).thenAnswer(invocation -> "000002".equals(
            TenantContext.getCurrentTenantId()) ? Optional.of(identityProvider(4L, "OIDC")) : Optional.empty());

        Optional<LocatedIdentityProvider> locatedIdentityProvider = identityProviderLocator.findByDomain("acme.com");

        assertThat(locatedIdentityProvider).map(LocatedIdentityProvider::tenantId)
            .contains("000002");
        assertThat(locatedIdentityProvider.map(LocatedIdentityProvider::registrationId)
            .map(IdentityProviderRegistrationId::value)).contains("sso-000002-4");
    }

    private static IdentityProvider identityProvider(long id, String type) {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setId(id);
        identityProvider.setType(type);

        return identityProvider;
    }
}
