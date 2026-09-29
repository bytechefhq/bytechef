/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.config;

import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WithMockConnectedUserSecurityContextFactory implements WithSecurityContextFactory<WithMockConnectedUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockConnectedUser withMockConnectedUser) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(TestConnectedUserAuthentication.of(withMockConnectedUser.externalUserId()));

        return securityContext;
    }
}
