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
 * Pins the {@code @PreAuthorize} expressions that close connected-user management IDOR (T25). The embedded
 * connected-user admin dashboard ({@code ConnectedUserApiController}, {@code /internal}) reads and toggles connected
 * users through this management facade, which is not runtime-shared, so the operations require tenant admin. The
 * runtime/dispatch path uses {@code ConnectedUserService.getConnectedUser(externalUserId, environment)} directly and is
 * unaffected.
 *
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
    void testGetConnectedUsersRequiresTenantAdmin() {
        assertAdmin(
            "getConnectedUsers", Long.class, String.class,
            com.bytechef.platform.connection.domain.Connection.CredentialStatus.class,
            java.time.LocalDate.class, java.time.LocalDate.class, Long.class, int.class);
    }

    @Test
    void testEnableConnectedUserRequiresTenantAdmin() {
        assertAdmin("enableConnectedUser", long.class, boolean.class);
    }

    @Test
    void testGetConnectedUserEntityRequiresTenantAdmin() {
        assertAdmin("getConnectedUserEntity", long.class);
    }

    @Test
    void testGetConnectedUserEntitiesRequiresTenantAdmin() {
        assertAdmin(
            "getConnectedUserEntities", Long.class, String.class, java.time.LocalDate.class, java.time.LocalDate.class,
            Long.class, int.class);
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
