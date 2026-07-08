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

    void delete(long id);

    Optional<IdentityProvider> fetchByDomain(String emailDomain);

    Optional<IdentityProvider> fetchByName(String name);

    Optional<IdentityProvider> fetchByScimApiKey(String scimApiKey);

    Optional<IdentityProvider> fetchMcpIdentityProvider();

    IdentityProvider getIdentityProvider(long id);

    List<IdentityProvider> getIdentityProviders();

    String getDecryptedClientSecret(IdentityProvider identityProvider);

    IdentityProvider update(IdentityProvider identityProvider);
}
