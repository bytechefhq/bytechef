/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.security.web.authentication;

import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
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

    public static boolean isConnectedUser() {
        Optional<ConnectedUserAuthentication> connectedUserAuthentication = fetchCurrent();

        return connectedUserAuthentication.isPresent();
    }
}
