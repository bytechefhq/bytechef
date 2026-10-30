/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.security;

import org.springframework.stereotype.Component;

/**
 * Connected-user checks for {@code @PreAuthorize} expressions, referenced as {@code @connectedUserSecurity}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("connectedUserSecurity")
public class ConnectedUserSecurity {

    public boolean isConnectedUser() {
        return ConnectedUserAuthentications.isConnectedUser();
    }

    public boolean isCurrentConnectedUser(long connectedUserId) {
        return ConnectedUserAuthentications.isCurrentConnectedUser(connectedUserId);
    }
}
