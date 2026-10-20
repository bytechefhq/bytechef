/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserFacadeTest {

    @Test
    void testEveryFacadeMethodCarriesItsEmbeddedScopeGate() throws NoSuchMethodException {
        Method[] facadeMethods = ConnectedUserFacade.class.getMethods();

        assertThat(facadeMethods).isNotEmpty();

        Map<String, String> expectedExpressions = Map.of(
            "deleteConnectedUser", "hasPermission(#id, 'ConnectedUser', 'CONNECTED_USER_DELETE')",
            "enableConnectedUser", "hasPermission(#id, 'ConnectedUser', 'CONNECTED_USER_MANAGE')",
            "getConnectedUser", "hasPermission(#id, 'ConnectedUser', 'CONNECTED_USER_VIEW')",
            "getConnectedUserDTO", "hasPermission(#id, 'ConnectedUser', 'CONNECTED_USER_VIEW')",
            "getConnectedUserDTOs", "hasEmbeddedScopeInEnvironmentId('CONNECTED_USER_VIEW', #environmentId)",
            "getConnectedUsers", "hasEmbeddedScopeInEnvironmentId('CONNECTED_USER_VIEW', #environmentId)");

        List<String> wrongGates = new ArrayList<>();

        for (Method facadeMethod : facadeMethods) {
            Method implementationMethod = ConnectedUserFacadeImpl.class.getDeclaredMethod(
                facadeMethod.getName(), facadeMethod.getParameterTypes());

            PreAuthorize preAuthorize = implementationMethod.getAnnotation(PreAuthorize.class);

            String expectedExpression = expectedExpressions.get(facadeMethod.getName());

            if (preAuthorize == null || !preAuthorize.value()
                .equals(expectedExpression)) {

                wrongGates.add(implementationMethod.toGenericString());
            }
        }

        assertThat(wrongGates)
            .as("ConnectedUserFacadeImpl methods without their embedded scope gate")
            .isEmpty();
    }

    @Test
    void testFacadeExposesTheManagementOperations() {
        List<String> facadeMethodNames = Arrays.stream(ConnectedUserFacade.class.getMethods())
            .map(Method::getName)
            .toList();

        assertThat(facadeMethodNames).contains(
            "deleteConnectedUser", "enableConnectedUser", "getConnectedUser", "getConnectedUserDTO",
            "getConnectedUserDTOs", "getConnectedUsers");
    }
}
