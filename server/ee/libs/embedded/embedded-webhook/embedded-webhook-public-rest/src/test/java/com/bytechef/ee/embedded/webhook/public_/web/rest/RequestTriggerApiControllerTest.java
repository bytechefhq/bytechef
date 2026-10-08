/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.webhook.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.webhook.public_.web.rest.model.EnvironmentModel;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RequestTriggerApiControllerTest {

    private static final String EXTERNAL_USER_ID = "alice";

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRequestTriggerLooksUpTheCallingConnectedUser() throws ReflectiveOperationException {
        authenticateConnectedUser();

        RequestTriggerApiController requestTriggerApiController = instantiate(RequestTriggerApiController.class);

        runIgnoringDownstreamFailures(
            () -> requestTriggerApiController.executeWorkflow("workflow-uuid", EnvironmentModel.PRODUCTION));

        verify(connectedUserService).getConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION);
    }

    @Test
    void testAPlatformSessionWhoseLoginIsAnExternalIdIsRefused() throws ReflectiveOperationException {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(EXTERNAL_USER_ID, "", List.of()));

        when(environmentService.getEnvironment(any(String.class))).thenReturn(Environment.PRODUCTION);

        RequestTriggerApiController requestTriggerApiController = instantiate(RequestTriggerApiController.class);

        assertThatThrownBy(
            () -> requestTriggerApiController.executeWorkflow("workflow-uuid", EnvironmentModel.PRODUCTION))
                .isInstanceOf(AccessDeniedException.class);

        verify(connectedUserService, never()).getConnectedUser(any(String.class), any(Environment.class));
    }

    private void authenticateConnectedUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of(EXTERNAL_USER_ID));

        when(environmentService.getEnvironment(any(String.class))).thenReturn(Environment.PRODUCTION);
    }

    private <T> T instantiate(Class<T> controllerClass) throws ReflectiveOperationException {
        Map<Class<?>, Object> collaborators = Map.of(
            ConnectedUserService.class, connectedUserService, EnvironmentService.class, environmentService);

        Constructor<?> constructor = controllerClass.getConstructors()[0];

        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];

        for (int index = 0; index < parameterTypes.length; index++) {
            arguments[index] = collaborators.containsKey(parameterTypes[index])
                ? collaborators.get(parameterTypes[index]) : mock(parameterTypes[index]);
        }

        return controllerClass.cast(constructor.newInstance(arguments));
    }

    private static void runIgnoringDownstreamFailures(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException exception) {
            assertThat(exception).isNotInstanceOf(AccessDeniedException.class);
        }
    }
}
