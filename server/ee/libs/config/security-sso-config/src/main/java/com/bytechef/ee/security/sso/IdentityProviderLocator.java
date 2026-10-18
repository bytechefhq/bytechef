/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.domain.IdentityProviderRegistrationId;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Finds identity providers for requests that arrive before anyone is authenticated (SSO discovery, the enforcement
 * check, the authorization redirect, the OAuth2/SAML callbacks). Identity providers are stored per tenant, and those
 * requests run in the default tenant, so in multi-tenant mode the lookup has to name the tenant: by the registration
 * id, which carries it, or by scanning tenants for an email domain.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IdentityProviderLocator {

    private final IdentityProviderService identityProviderService;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public IdentityProviderLocator(IdentityProviderService identityProviderService, TenantService tenantService) {
        this.identityProviderService = identityProviderService;
        this.tenantService = tenantService;
    }

    public Optional<LocatedIdentityProvider> findByDomain(String emailDomain) {
        return findInTenants(tenantId -> identityProviderService.fetchByDomain(emailDomain));
    }

    public Optional<LocatedIdentityProvider> findByName(String name) {
        return findInTenants(tenantId -> identityProviderService.fetchByName(name));
    }

    public Optional<LocatedIdentityProvider> findByRegistrationId(String registrationId) {
        Optional<IdentityProviderRegistrationId> parsedRegistrationId =
            IdentityProviderRegistrationId.parse(registrationId);

        if (parsedRegistrationId.isEmpty()) {
            return Optional.empty();
        }

        IdentityProviderRegistrationId identityProviderRegistrationId = parsedRegistrationId.get();
        String tenantId = identityProviderRegistrationId.tenantId();

        boolean tenantMatchesMode =
            tenantService.isMultiTenantEnabled() != TenantContext.DEFAULT_TENANT_ID.equals(tenantId);

        if (!tenantMatchesMode) {
            return Optional.empty();
        }

        try {
            IdentityProvider identityProvider = callWithTenantId(
                tenantId,
                () -> identityProviderService.getIdentityProvider(
                    identityProviderRegistrationId.identityProviderId()));

            return Optional.of(new LocatedIdentityProvider(tenantId, identityProvider));
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }
    }

    /**
     * Runs a call in the given tenant without wrapping its exceptions, unlike {@link TenantContext#callWithTenantId}:
     * an {@code AuthenticationException} thrown inside has to reach Spring Security's failure handler as itself.
     */
    public static <T> T callWithTenantId(String tenantId, Supplier<T> supplier) {
        String previousTenantId = TenantContext.getCurrentTenantId();

        TenantContext.setCurrentTenantId(tenantId);

        try {
            return supplier.get();
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }

    private Optional<LocatedIdentityProvider> findInTenants(
        Function<String, Optional<IdentityProvider>> identityProviderFinder) {

        List<String> tenantIds = tenantService.isMultiTenantEnabled()
            ? tenantService.getTenantIds() : List.of(TenantContext.getCurrentTenantId());

        for (String tenantId : tenantIds) {
            Optional<IdentityProvider> identityProvider = callWithTenantId(
                tenantId, () -> identityProviderFinder.apply(tenantId));

            if (identityProvider.isPresent()) {
                return Optional.of(new LocatedIdentityProvider(tenantId, identityProvider.get()));
            }
        }

        return Optional.empty();
    }

    /**
     * An identity provider together with the tenant whose schema holds it.
     */
    @SuppressFBWarnings("EI")
    public record LocatedIdentityProvider(String tenantId, IdentityProvider identityProvider) {

        public IdentityProviderRegistrationId registrationId() {
            return callWithTenantId(tenantId, () -> IdentityProviderRegistrationId.of(identityProvider));
        }
    }
}
