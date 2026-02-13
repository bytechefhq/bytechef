/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.oauth2;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.service.IdentityProviderService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
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
 * A dynamic {@link ClientRegistrationRepository} that resolves OIDC SSO providers from the database. Static social
 * login providers (Google, GitHub) are delegated to the auto-configured {@link InMemoryClientRegistrationRepository}.
 * Dynamic SSO providers use registration IDs in the format {@code sso-{idpId}}.
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

    public static final String SSO_PREFIX = "sso-";

    private final IdentityProviderService identityProviderService;
    private final InMemoryClientRegistrationRepository staticRegistrations;

    @SuppressFBWarnings({"CT_CONSTRUCTOR_THROW", "EI"})
    public DynamicClientRegistrationRepository(
        ObjectProvider<InMemoryClientRegistrationRepository> staticRegistrationsProvider,
        IdentityProviderService identityProviderService) {

        this.identityProviderService = identityProviderService;
        this.staticRegistrations = staticRegistrationsProvider.getIfAvailable();
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (registrationId != null && registrationId.startsWith(SSO_PREFIX)) {
            return buildDynamicRegistration(registrationId);
        }

        if (staticRegistrations == null) {
            return null;
        }

        return staticRegistrations.findByRegistrationId(registrationId);
    }

    private ClientRegistration buildDynamicRegistration(String registrationId) {
        long identityProviderId = Long.parseLong(registrationId.substring(SSO_PREFIX.length()));

        IdentityProvider identityProvider = identityProviderService.getIdentityProvider(identityProviderId);

        if (!identityProvider.isEnabled()) {
            return null;
        }

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
}
