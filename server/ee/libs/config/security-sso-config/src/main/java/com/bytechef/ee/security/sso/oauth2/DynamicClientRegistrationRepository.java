/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.oauth2;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.domain.IdentityProviderRegistrationId;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.ee.security.sso.IdentityProviderLocator;
import com.bytechef.ee.security.sso.IdentityProviderLocator.LocatedIdentityProvider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.constant.UserConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;

/**
 * Serves the static social-login registrations (Google, GitHub) and builds a registration per enabled OIDC identity
 * provider on demand. Issuer discovery is an outbound request that anonymous callers can trigger, so built
 * registrations are cached until the identity provider changes or {@link #CACHE_TTL} passes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Primary
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.security.sso", name = "enabled", havingValue = "true")
public class DynamicClientRegistrationRepository implements ClientRegistrationRepository {

    public static final String SSO_PREFIX = IdentityProviderRegistrationId.OIDC_PREFIX;

    static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private static final Logger log = LoggerFactory.getLogger(DynamicClientRegistrationRepository.class);

    private final Map<String, CachedClientRegistration> cachedClientRegistrations = new ConcurrentHashMap<>();
    private final IdentityProviderLocator identityProviderLocator;
    private final IdentityProviderService identityProviderService;
    private final InMemoryClientRegistrationRepository staticRegistrations;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public DynamicClientRegistrationRepository(
        ObjectProvider<InMemoryClientRegistrationRepository> staticRegistrationsProvider,
        IdentityProviderLocator identityProviderLocator, IdentityProviderService identityProviderService) {

        this.identityProviderLocator = identityProviderLocator;
        this.identityProviderService = identityProviderService;
        this.staticRegistrations = staticRegistrationsProvider.getIfAvailable();
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (registrationId != null && registrationId.startsWith(SSO_PREFIX)) {
            return identityProviderLocator.findByRegistrationId(registrationId)
                .flatMap(locatedIdentityProvider -> buildDynamicRegistration(registrationId, locatedIdentityProvider))
                .orElse(null);
        }

        if (staticRegistrations == null) {
            return null;
        }

        return staticRegistrations.findByRegistrationId(registrationId);
    }

    private Optional<ClientRegistration> buildDynamicRegistration(
        String registrationId, LocatedIdentityProvider locatedIdentityProvider) {

        IdentityProvider identityProvider = locatedIdentityProvider.identityProvider();

        if (!identityProvider.isEnabled() || UserConstants.AUTH_PROVIDER_SAML.equals(identityProvider.getType())) {
            return Optional.empty();
        }

        Instant version = identityProvider.getLastModifiedDate();
        CachedClientRegistration cachedClientRegistration = cachedClientRegistrations.get(registrationId);

        if (cachedClientRegistration != null && cachedClientRegistration.isFresh(version)) {
            return Optional.of(cachedClientRegistration.clientRegistration());
        }

        try {
            ClientRegistration clientRegistration = buildClientRegistration(registrationId, identityProvider);

            cachedClientRegistrations.put(
                registrationId, new CachedClientRegistration(clientRegistration, version, Instant.now()));

            return Optional.of(clientRegistration);
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Unable to build OIDC registration {} from issuer {}", registrationId, identityProvider.getIssuerUri(),
                runtimeException);

            return Optional.empty();
        }
    }

    private ClientRegistration buildClientRegistration(String registrationId, IdentityProvider identityProvider) {
        String decryptedSecret = identityProviderService.getDecryptedClientSecret(identityProvider);

        String[] scopes = Arrays.stream(identityProvider.getScopes()
            .split(","))
            .map(String::trim)
            .filter(scope -> !scope.isEmpty())
            .toArray(String[]::new);

        return ClientRegistrations.fromIssuerLocation(identityProvider.getIssuerUri())
            .registrationId(registrationId)
            .clientId(identityProvider.getClientId())
            .clientSecret(decryptedSecret)
            .scope(scopes)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .clientName(identityProvider.getName())
            .build();
    }

    private record CachedClientRegistration(ClientRegistration clientRegistration, Instant version, Instant cachedAt) {

        boolean isFresh(Instant currentVersion) {
            return Objects.equals(version, currentVersion) && cachedAt.plus(CACHE_TTL)
                .isAfter(Instant.now());
        }
    }
}
