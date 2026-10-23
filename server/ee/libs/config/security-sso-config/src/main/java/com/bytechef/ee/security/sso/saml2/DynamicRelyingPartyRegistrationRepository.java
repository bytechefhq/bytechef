/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.domain.IdentityProviderRegistrationId;
import com.bytechef.ee.platform.user.domain.IdentityProviderSigningCertificate;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.ee.security.sso.IdentityProviderLocator;
import com.bytechef.ee.security.sso.IdentityProviderLocator.LocatedIdentityProvider;
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
import org.springframework.security.saml2.core.Saml2X509Credential;
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
    private final IdentityProviderService identityProviderService;

    @SuppressFBWarnings("EI")
    public DynamicRelyingPartyRegistrationRepository(
        IdentityProviderLocator identityProviderLocator, IdentityProviderService identityProviderService) {

        this.identityProviderLocator = identityProviderLocator;
        this.identityProviderService = identityProviderService;
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        if (registrationId == null || !registrationId.startsWith(SAML_PREFIX)) {
            return null;
        }

        return identityProviderLocator.findByRegistrationId(registrationId)
            .filter(locatedIdentityProvider -> {
                IdentityProvider identityProvider = locatedIdentityProvider.identityProvider();

                return identityProvider.isEnabled() &&
                    UserConstants.AUTH_PROVIDER_SAML.equals(identityProvider.getType());
            })
            .map(locatedIdentityProvider -> getRelyingPartyRegistration(registrationId, locatedIdentityProvider))
            .orElse(null);
    }

    private RelyingPartyRegistration getRelyingPartyRegistration(
        String registrationId, LocatedIdentityProvider locatedIdentityProvider) {

        IdentityProvider identityProvider = locatedIdentityProvider.identityProvider();

        Instant version = identityProvider.getLastModifiedDate();
        CachedRelyingPartyRegistration cachedRelyingPartyRegistration =
            cachedRelyingPartyRegistrations.get(registrationId);

        if (cachedRelyingPartyRegistration != null && cachedRelyingPartyRegistration.isFresh(version)) {
            return cachedRelyingPartyRegistration.relyingPartyRegistration();
        }

        try {
            Saml2X509Credential signingCredential = getSigningCredential(locatedIdentityProvider);

            RelyingPartyRegistration.Builder relyingPartyRegistrationBuilder = RelyingPartyRegistrations
                .fromMetadataLocation(identityProvider.getMetadataUri())
                .registrationId(registrationId)
                .entityId("{baseUrl}/saml2/metadata/{registrationId}")
                .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/{registrationId}")
                .signingX509Credentials(signingCredentials -> signingCredentials.add(signingCredential));

            String pinnedCertificate = identityProvider.getSigningCertificate();

            if (pinnedCertificate != null && !pinnedCertificate.isBlank()) {
                Saml2X509Credential verificationCredential = Saml2X509Credential.verification(
                    IdentityProviderSigningCertificate.parse(pinnedCertificate));

                relyingPartyRegistrationBuilder.assertingPartyMetadata(
                    assertingPartyMetadata -> assertingPartyMetadata.verificationX509Credentials(
                        verificationCredentials -> {
                            verificationCredentials.clear();
                            verificationCredentials.add(verificationCredential);
                        }));
            }

            RelyingPartyRegistration relyingPartyRegistration = relyingPartyRegistrationBuilder.build();

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

    private Saml2X509Credential getSigningCredential(LocatedIdentityProvider locatedIdentityProvider) {
        String tenantId = locatedIdentityProvider.tenantId();
        IdentityProvider identityProvider = locatedIdentityProvider.identityProvider();

        if (identityProvider.getServiceProviderSigningCertificate() == null) {
            long identityProviderId = identityProvider.getId();

            identityProvider = IdentityProviderLocator.callWithTenantId(
                tenantId, () -> identityProviderService.createServiceProviderSigningCredentialIfAbsent(
                    identityProviderId));
        }

        IdentityProvider signingIdentityProvider = identityProvider;

        String privateKey = IdentityProviderLocator.callWithTenantId(
            tenantId, () -> identityProviderService.getDecryptedServiceProviderSigningKey(signingIdentityProvider));

        return ServiceProviderSigningCredentials.toSigningCredential(
            signingIdentityProvider.getServiceProviderSigningCertificate(), privateKey);
    }

    private record CachedRelyingPartyRegistration(
        RelyingPartyRegistration relyingPartyRegistration, Instant version, Instant cachedAt) {

        boolean isFresh(Instant currentVersion) {
            return Objects.equals(version, currentVersion) && cachedAt.plus(CACHE_TTL)
                .isAfter(Instant.now());
        }
    }
}
