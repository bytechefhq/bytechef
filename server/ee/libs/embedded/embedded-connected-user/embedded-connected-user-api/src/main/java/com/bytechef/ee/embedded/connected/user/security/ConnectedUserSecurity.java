/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.security;

import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ConnectedUserSecurity implements MethodSecurityExpressionContributor {

    public boolean isConnectedUser() {
        return ConnectedUserAuthentications.isConnectedUser();
    }

    public boolean isCurrentConnectedUser(long connectedUserId) {
        return ConnectedUserAuthentications.isCurrentConnectedUser(connectedUserId);
    }
}
