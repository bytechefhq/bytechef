/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.security;

import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ConnectedUserAuthentications {

    private ConnectedUserAuthentications() {
    }

    public static Optional<ConnectedUserAuthentication> fetchCurrent() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        if (authentication instanceof ConnectedUserAuthentication connectedUserAuthentication &&
            authentication.isAuthenticated()) {

            return Optional.of(connectedUserAuthentication);
        }

        return Optional.empty();
    }

    public static String getCurrentExternalUserId() {
        return fetchCurrent()
            .map(ConnectedUserAuthentication::externalUserId)
            .orElseThrow(() -> new AccessDeniedException("Only a connected user may call this endpoint"));
    }

    public static String requireCurrentExternalUserId(String externalUserId) {
        String currentExternalUserId = getCurrentExternalUserId();

        if (!currentExternalUserId.equals(externalUserId)) {
            throw new AccessDeniedException("A connected user may only act on their own resources");
        }

        return currentExternalUserId;
    }

    public static boolean isCurrentConnectedUser(long connectedUserId) {
        Optional<ConnectedUserAuthentication> connectedUserAuthentication = fetchCurrent();

        return connectedUserAuthentication
            .map(currentConnectedUserAuthentication -> currentConnectedUserAuthentication
                .connectedUserId() == connectedUserId)
            .orElse(false);
    }

    public static boolean isConnectedUser() {
        Optional<ConnectedUserAuthentication> connectedUserAuthentication = fetchCurrent();

        return connectedUserAuthentication.isPresent();
    }
}
