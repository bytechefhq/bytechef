/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.security;

import com.bytechef.platform.security.web.authentication.NonPlatformUserAuthentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserAuthentication extends NonPlatformUserAuthentication {

    long connectedUserId();

    String externalUserId();

    long environmentId();

    default boolean apiKeyAuthenticated() {
        return false;
    }
}
