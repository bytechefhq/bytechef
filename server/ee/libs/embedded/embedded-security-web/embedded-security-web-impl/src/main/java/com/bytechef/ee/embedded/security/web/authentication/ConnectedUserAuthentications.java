/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ConnectedUserAuthentications {

    private ConnectedUserAuthentications() {
    }

    static Optional<Authentication> fetchAuthentication(
        ConnectedUserService connectedUserService, long connectedUserId) {

        return connectedUserService.fetchConnectedUser(connectedUserId)
            .filter(ConnectedUser::isEnabled)
            .map(ConnectedUserAuthentications::createAuthentication);
    }

    private static Authentication createAuthentication(ConnectedUser connectedUser) {
        return new EmbeddedApiKeyAuthenticationToken(
            connectedUser.getEnvironmentId(), connectedUser.getId(),
            new User(connectedUser.getExternalId(), "", List.of()),
            false);
    }
}
