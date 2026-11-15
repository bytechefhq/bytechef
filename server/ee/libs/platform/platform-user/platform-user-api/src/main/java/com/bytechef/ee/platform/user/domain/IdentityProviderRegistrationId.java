/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.domain;

import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.tenant.TenantContext;
import java.util.Objects;
import java.util.Optional;

/**
 * The OAuth2/SAML registration id of an identity provider, which is also the last path segment of its redirect URI and
 * SP metadata URL. Identity providers live in their tenant's schema, and the login callbacks that carry this id arrive
 * before anyone is authenticated, so in multi-tenant mode the id names the tenant: {@code sso-{tenantId}-{id}}. The
 * default tenant (single-tenant mode) keeps the short {@code sso-{id}} form so redirect URIs already registered at an
 * IdP stay valid.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record IdentityProviderRegistrationId(String prefix, String tenantId, long identityProviderId) {

    public static final String OIDC_PREFIX = "sso-";
    public static final String SAML_PREFIX = "saml-";

    public IdentityProviderRegistrationId {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(tenantId, "tenantId");
    }

    /**
     * The registration id of an identity provider stored in the current tenant.
     */
    public static IdentityProviderRegistrationId of(IdentityProvider identityProvider) {
        String prefix = UserConstants.AUTH_PROVIDER_SAML.equals(identityProvider.getType()) ? SAML_PREFIX : OIDC_PREFIX;

        return new IdentityProviderRegistrationId(
            prefix, TenantContext.getCurrentTenantId(), Objects.requireNonNull(identityProvider.getId(), "id"));
    }

    public static Optional<IdentityProviderRegistrationId> parse(String registrationId) {
        if (registrationId == null) {
            return Optional.empty();
        }

        String prefix;

        if (registrationId.startsWith(OIDC_PREFIX)) {
            prefix = OIDC_PREFIX;
        } else if (registrationId.startsWith(SAML_PREFIX)) {
            prefix = SAML_PREFIX;
        } else {
            return Optional.empty();
        }

        String rest = registrationId.substring(prefix.length());
        int separatorIndex = rest.indexOf('-');

        String tenantId = separatorIndex < 0 ? TenantContext.DEFAULT_TENANT_ID : rest.substring(0, separatorIndex);
        String identityProviderId = separatorIndex < 0 ? rest : rest.substring(separatorIndex + 1);

        if (tenantId.isEmpty() || !isDigits(identityProviderId) ||
            (separatorIndex >= 0 && !isDigits(tenantId))) {

            return Optional.empty();
        }

        try {
            return Optional.of(
                new IdentityProviderRegistrationId(prefix, tenantId, Long.parseLong(identityProviderId)));
        } catch (NumberFormatException numberFormatException) {
            return Optional.empty();
        }
    }

    public boolean isSaml() {
        return SAML_PREFIX.equals(prefix);
    }

    public String value() {
        if (TenantContext.DEFAULT_TENANT_ID.equals(tenantId)) {
            return prefix + identityProviderId;
        }

        return prefix + tenantId + "-" + identityProviderId;
    }

    @Override
    public String toString() {
        return value();
    }

    private static boolean isDigits(String value) {
        return !value.isEmpty() && value.chars()
            .allMatch(Character::isDigit);
    }
}
