/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.web.authentication.ConnectedUserPathBindings;
import com.bytechef.platform.security.web.authentication.ConnectedUserPathBindings.ExternalUserIdEndpoint;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolApiControllerTest {

    private static final String BASE_PACKAGE = "com.bytechef.ee.embedded.execution.public_.web.rest";

    @Test
    void testDiscoversEveryExternalUserIdEndpoint() {
        assertThat(externalUserIdEndpoints()).hasSize(2);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("externalUserIdEndpoints")
    void testAConnectedUserIsRefusedAnotherConnectedUsersPath(ExternalUserIdEndpoint externalUserIdEndpoint)
        throws ReflectiveOperationException {

        ConnectedUserPathBindings.assertRefusesAnotherConnectedUser(externalUserIdEndpoint);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("externalUserIdEndpoints")
    void testAPlatformSessionIsRefusedAConnectedUsersPath(ExternalUserIdEndpoint externalUserIdEndpoint)
        throws ReflectiveOperationException {

        ConnectedUserPathBindings.assertRefusesAPlatformSession(externalUserIdEndpoint);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("externalUserIdEndpoints")
    void testAConnectedUserPassesTheGuardOnTheirOwnPath(ExternalUserIdEndpoint externalUserIdEndpoint)
        throws ReflectiveOperationException {

        ConnectedUserPathBindings.assertLetsTheConnectedUserActOnTheirOwnPath(externalUserIdEndpoint);
    }

    static List<ExternalUserIdEndpoint> externalUserIdEndpoints() {
        return ConnectedUserPathBindings.findExternalUserIdEndpoints(BASE_PACKAGE)
            .stream()
            .filter(externalUserIdEndpoint -> externalUserIdEndpoint.controllerClass() == ToolApiController.class)
            .toList();
    }
}
