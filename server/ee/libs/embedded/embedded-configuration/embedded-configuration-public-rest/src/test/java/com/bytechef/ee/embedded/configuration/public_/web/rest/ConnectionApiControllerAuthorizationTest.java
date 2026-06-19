/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} that prevents an embedded caller authenticated for one end user from reading another
 * end user's connections by passing a different {@code externalUserId} in the path (T23, connection sub-path). The
 * authenticated principal's name is the connected user's externalId, so the path value must equal it.
 *
 * @author Ivica Cardic
 * @version ee
 */
class ConnectionApiControllerAuthorizationTest {

    @Test
    void testGetConnectionsRequiresMatchingExternalUserId() {
        Method method = null;

        for (Method candidate : ConnectionApiController.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals("getConnections")) {
                method = candidate;

                break;
            }
        }

        assertThat(method)
            .as("method getConnections")
            .isNotNull();

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on getConnections")
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("#externalUserId == authentication.name");
    }
}
