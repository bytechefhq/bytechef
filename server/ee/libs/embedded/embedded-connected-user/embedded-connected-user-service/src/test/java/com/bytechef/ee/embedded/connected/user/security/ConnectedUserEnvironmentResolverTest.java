/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserEnvironmentResolverTest {

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final ConnectedUserEnvironmentResolver resolver =
        new ConnectedUserEnvironmentResolver(connectedUserService);

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(resolver.resourceType()).isEqualTo("ConnectedUser");
    }

    @Test
    void testTheEnvironmentComesFromTheEnvironmentColumn() {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setEnvironment(Environment.PRODUCTION);

        when(connectedUserService.fetchConnectedUser(7L)).thenReturn(Optional.of(connectedUser));

        assertThat(resolver.fetchEnvironment(7L)).contains(Environment.PRODUCTION);
    }

    @Test
    void testAMissingRowHasNoEnvironment() {
        when(connectedUserService.fetchConnectedUser(7L)).thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(7L)).isEmpty();
    }

    @Test
    void testANonNumericIdHasNoEnvironment() {
        assertThat(resolver.fetchEnvironment("not-a-number")).isEmpty();
    }
}
