/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import com.bytechef.platform.user.domain.Authority;
import java.util.List;

/**
 * Facade for fetching authorities (roles). Hosts the {@code ADMIN} authorization guard so it applies to every caller of
 * the facade rather than only the GraphQL entry point, and keeps it off the shared {@code AuthorityService} which
 * non-admin flows (account self-service, SCIM, login) rely on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AuthorityFacade {

    List<Authority> getAuthorities();
}
