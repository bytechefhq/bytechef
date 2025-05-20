/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.security.web.authentication.AuthenticationProviderContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ConnectedUserAuthenticationProviderContributor implements AuthenticationProviderContributor {

    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public ConnectedUserAuthenticationProviderContributor(ConnectedUserService connectedUserService) {
        this.connectedUserService = connectedUserService;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return new ConnectedUserAuthenticationProvider(connectedUserService);
    }
}
