/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.domain.IdentityProviderRegistrationId;
import com.bytechef.ee.security.sso.IdentityProviderLocator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.constant.UserConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.stereotype.Component;

/**
 * Builds a SAML relying party registration per enabled SAML identity provider on demand. Fetching the IdP metadata is
 * an outbound request that anonymous callers can trigger, so built registrations are cached until the identity provider
 * changes or {@link #CACHE_TTL} passes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.security.sso", name = "enabled", havingValue = "true")
public class DynamicRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    public static final String SAML_PREFIX = IdentityProviderRegistrationId.SAML_PREFIX;

    static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private static final Logger log = LoggerFactory.getLogger(DynamicRelyingPartyRegistrationRepository.class);

    private final Map<String, CachedRelyingPartyRegistration> cachedRelyingPartyRegistrations =
        new ConcurrentHashMap<>();
    private final IdentityProviderLocator identityProviderLocator;

    @SuppressFBWarnings("EI")
    public DynamicRelyingPartyRegistrationRepository(IdentityProviderLocator identityProviderLocator) {
        this.identityProviderLocator = identityProviderLocator;
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        if (registrationId == null || !registrationId.startsWith(SAML_PREFIX)) {
            return null;
        }

        return identityProviderLocator.findByRegistrationId(registrationId)
            .map(IdentityProviderLocator.LocatedIdentityProvider::identityProvider)
            .filter(identityProvider -> identityProvider.isEnabled() &&
                UserConstants.AUTH_PROVIDER_SAML.equals(identityProvider.getType()))
            .map(identityProvider -> getRelyingPartyRegistration(registrationId, identityProvider))
            .orElse(null);
    }

    private RelyingPartyRegistration getRelyingPartyRegistration(
        String registrationId, IdentityProvider identityProvider) {

        Instant version = identityProvider.getLastModifiedDate();
        CachedRelyingPartyRegistration cachedRelyingPartyRegistration =
            cachedRelyingPartyRegistrations.get(registrationId);

        if (cachedRelyingPartyRegistration != null && cachedRelyingPartyRegistration.isFresh(version)) {
            return cachedRelyingPartyRegistration.relyingPartyRegistration();
        }

        try {
            RelyingPartyRegistration relyingPartyRegistration = RelyingPartyRegistrations
                .fromMetadataLocation(identityProvider.getMetadataUri())
                .registrationId(registrationId)
                .entityId("{baseUrl}/saml2/metadata/{registrationId}")
                .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/{registrationId}")
                .build();

            cachedRelyingPartyRegistrations.put(
                registrationId, new CachedRelyingPartyRegistration(relyingPartyRegistration, version, Instant.now()));

            return relyingPartyRegistration;
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Unable to build SAML registration {} from metadata {}", registrationId,
                identityProvider.getMetadataUri(), runtimeException);

            return null;
        }
    }

    private record CachedRelyingPartyRegistration(
        RelyingPartyRegistration relyingPartyRegistration, Instant version, Instant cachedAt) {

        boolean isFresh(Instant currentVersion) {
            return Objects.equals(version, currentVersion) && cachedAt.plus(CACHE_TTL)
                .isAfter(Instant.now());
        }
    }
}
