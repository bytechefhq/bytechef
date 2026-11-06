/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 * @author Ivica Cardic
 */
class ConnectedUserAiGatewayFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";

    @Test
    void testEveryFacadeMethodRequiresAdmin() throws NoSuchMethodException {
        Method[] interfaceMethods = ConnectedUserAiGatewayFacade.class.getMethods();

        assertThat(interfaceMethods)
            .as("Sanity check: the sweep is vacuous if the facade declares nothing")
            .hasSize(5);

        for (Method interfaceMethod : interfaceMethods) {
            Method implementationMethod = ConnectedUserAiGatewayFacadeImpl.class.getDeclaredMethod(
                interfaceMethod.getName(), interfaceMethod.getParameterTypes());

            PreAuthorize preAuthorize = implementationMethod.getAnnotation(PreAuthorize.class);

            assertThat(preAuthorize)
                .as("Method '%s' must have @PreAuthorize", interfaceMethod.getName())
                .isNotNull();
            assertThat(preAuthorize.value())
                .as("Method '%s' @PreAuthorize expression", interfaceMethod.getName())
                .isEqualTo(ADMIN_EXPRESSION);
        }
    }
}
