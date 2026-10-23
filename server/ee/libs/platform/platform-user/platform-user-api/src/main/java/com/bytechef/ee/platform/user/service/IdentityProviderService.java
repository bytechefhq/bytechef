/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.service;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IdentityProviderService {

    IdentityProvider create(IdentityProvider identityProvider);

    /**
     * Stores the service provider signing credential unless the identity provider already has one, and returns the
     * identity provider with whichever credential won. The private key is encrypted at rest.
     */
    IdentityProvider createServiceProviderSigningCredentialIfAbsent(long id, String certificate, String privateKey);

    void delete(long id);

    Optional<IdentityProvider> fetchByDomain(String emailDomain);

    Optional<IdentityProvider> fetchByName(String name);

    Optional<IdentityProvider> fetchByScimApiKey(String scimApiKey);

    IdentityProvider getIdentityProvider(long id);

    List<IdentityProvider> getIdentityProviders();

    String generateScimApiKey(long id);

    String getDecryptedClientSecret(IdentityProvider identityProvider);

    String getDecryptedServiceProviderSigningKey(IdentityProvider identityProvider);

    IdentityProvider update(IdentityProvider identityProvider);
}
