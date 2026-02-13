/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.saml2;

import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.service.IdentityProviderService;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.stereotype.Component;

/**
 * A dynamic {@link RelyingPartyRegistrationRepository} that resolves SAML2 relying party registrations from the
 * database. Dynamic SAML providers use registration IDs in the format {@code saml-{idpId}}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnMultiTenant
@ConditionalOnProperty(prefix = "bytechef.security.social-login", name = "enabled", havingValue = "true")
public class DynamicRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    public static final String SAML_PREFIX = "saml-";

    private final IdentityProviderService identityProviderService;

    @SuppressFBWarnings("EI")
    public DynamicRelyingPartyRegistrationRepository(IdentityProviderService identityProviderService) {
        this.identityProviderService = identityProviderService;
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        if (registrationId == null || !registrationId.startsWith(SAML_PREFIX)) {
            return null;
        }

        long identityProviderId = Long.parseLong(registrationId.substring(SAML_PREFIX.length()));

        IdentityProvider identityProvider = identityProviderService.getIdentityProvider(identityProviderId);

        if (!identityProvider.isEnabled() || !"SAML".equals(identityProvider.getType())) {
            return null;
        }

        return buildRelyingPartyRegistration(registrationId, identityProvider);
    }

    private RelyingPartyRegistration buildRelyingPartyRegistration(
        String registrationId, IdentityProvider identityProvider) {

        return RelyingPartyRegistrations
            .fromMetadataLocation(identityProvider.getMetadataUri())
            .registrationId(registrationId)
            .entityId("{baseUrl}/saml2/metadata/{registrationId}")
            .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/{registrationId}")
            .build();
    }
}
