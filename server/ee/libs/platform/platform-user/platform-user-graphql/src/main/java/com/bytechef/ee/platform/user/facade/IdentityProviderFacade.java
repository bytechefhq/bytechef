/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import java.util.List;

/**
 * Facade for managing identity providers (OIDC/SAML SSO configuration). Hosts the {@code ADMIN} authorization guard so
 * it applies to every caller of the facade rather than only the GraphQL entry point.
 *
 * <p>
 * A facade is used (rather than annotating {@code IdentityProviderService} directly) because
 * {@code IdentityProviderService.getIdentityProvider(Long)} is also called by the unauthenticated SSO login flows
 * (dynamic client/relying-party registration, OIDC user service); annotating the service with {@code ADMIN} would break
 * those login paths.
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
