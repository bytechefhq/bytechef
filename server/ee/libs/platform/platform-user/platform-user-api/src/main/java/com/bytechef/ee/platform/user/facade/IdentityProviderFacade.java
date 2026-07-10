/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import com.bytechef.platform.user.domain.IdentityProvider;
import java.util.List;

/**
 * Facade for managing identity providers (OIDC/SAML SSO configuration). Hosts the {@code ADMIN} authorization guard so
 * it applies to every caller of the facade rather than only the GraphQL entry point.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IdentityProviderFacade {

    IdentityProvider create(IdentityProvider identityProvider);

    void delete(Long id);

    IdentityProvider getIdentityProvider(Long id);

    List<IdentityProvider> getIdentityProviders();

    IdentityProvider update(IdentityProvider identityProvider);
}
