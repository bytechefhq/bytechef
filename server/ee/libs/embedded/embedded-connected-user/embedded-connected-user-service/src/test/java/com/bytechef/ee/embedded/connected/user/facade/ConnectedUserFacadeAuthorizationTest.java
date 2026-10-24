/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserFacadeAuthorizationTest {

    @Test
    void testGetConnectedUserRequiresTenantAdmin() {
        assertAdmin("getConnectedUser", long.class);
    }

    @Test
    void testGetConnectedUserDTOsRequiresTenantAdmin() {
        assertAdmin(
            "getConnectedUserDTOs", Long.class, String.class,
            com.bytechef.platform.connection.domain.Connection.CredentialStatus.class,
            java.time.LocalDate.class, java.time.LocalDate.class, Long.class, int.class);
    }

    @Test
    void testEnableConnectedUserRequiresTenantAdmin() {
        assertAdmin("enableConnectedUser", long.class, boolean.class);
    }

    @Test
    void testCreateConnectedUserRequiresTenantAdmin() {
        assertAdmin(
            "createConnectedUser", String.class, com.bytechef.platform.configuration.domain.Environment.class,
            String.class, String.class, java.util.Map.class);
    }

    @Test
    void testImportConnectedUsersRequiresTenantAdmin() {
        assertAdmin(
            "importConnectedUsers", com.bytechef.platform.configuration.domain.Environment.class,
            java.io.InputStream.class);
    }

    @Test
    void testUpdateConnectedUserRequiresTenantAdmin() {
        assertAdmin(
            "updateConnectedUser", long.class, String.class, String.class, java.util.Map.class, int.class);
    }

    private static void assertAdmin(String methodName, Class<?>... parameterTypes) {
        Method method;

        try {
            method = ConnectedUserFacadeImpl.class.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("method " + methodName + " not found", exception);
        }

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("isTenantAdmin()");
    }
}
