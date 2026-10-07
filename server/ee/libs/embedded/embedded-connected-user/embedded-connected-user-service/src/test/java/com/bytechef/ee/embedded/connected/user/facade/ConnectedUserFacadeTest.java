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
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserFacadeTest {

    @Test
    void testEveryFacadeMethodRequiresTenantAdmin() throws NoSuchMethodException {
        Method[] facadeMethods = ConnectedUserFacade.class.getMethods();

        assertThat(facadeMethods).isNotEmpty();

        List<String> ungatedMethodNames = new ArrayList<>();

        for (Method facadeMethod : facadeMethods) {
            Method implementationMethod = ConnectedUserFacadeImpl.class.getDeclaredMethod(
                facadeMethod.getName(), facadeMethod.getParameterTypes());

            PreAuthorize preAuthorize = implementationMethod.getAnnotation(PreAuthorize.class);

            if (preAuthorize == null || !"isTenantAdmin()".equals(preAuthorize.value())) {
                ungatedMethodNames.add(implementationMethod.toGenericString());
            }
        }

        assertThat(ungatedMethodNames)
            .as("ConnectedUserFacadeImpl methods without @PreAuthorize(\"isTenantAdmin()\")")
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
