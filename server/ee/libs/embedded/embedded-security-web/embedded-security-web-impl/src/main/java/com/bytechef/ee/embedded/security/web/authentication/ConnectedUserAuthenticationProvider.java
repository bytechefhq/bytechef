/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectedUserAuthenticationProvider implements AuthenticationProvider {

    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public ConnectedUserAuthenticationProvider(ConnectedUserService connectedUserService) {
        this.connectedUserService = connectedUserService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ConnectedUserAuthenticationToken connectedUserAuthenticationToken =
            (ConnectedUserAuthenticationToken) authentication;

        Environment environment = connectedUserAuthenticationToken.getEnvironment();
        String externalUserId = connectedUserAuthenticationToken.getExternalUserId();

        ConnectedUser connectedUser = connectedUserService.fetchConnectedUser(externalUserId, environment)
            .orElseGet(() -> connectedUserService.createConnectedUser(externalUserId, environment));

        return new ConnectedUserAuthenticationToken(createSpringSecurityUser(externalUserId, connectedUser));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(ConnectedUserAuthenticationToken.class);
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(
        String externalUserId, ConnectedUser connectedUser) {

        if (!connectedUser.isEnabled()) {
            throw new UserNotActivatedException("Connected User " + externalUserId + " was not enabled");
        }

        return new org.springframework.security.core.userdetails.User(connectedUser.getExternalId(), "", List.of());
    }
}
