/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.repository.ConnectedUserRepository;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pins the fix that closes the reserved-word collision at the source: an externalId equal to one of
 * {@link com.bytechef.ee.embedded.connected.user.constant.ConnectedUserConstants#FRONTEND_RESERVED_PATH_SEGMENTS} must
 * never be persisted as a connected user, since {@code EmbeddedApiKeyAuthenticationConverter} would silently 401 every
 * non-JWT sub-resource request for that user afterwards.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserServiceCreateTest {

    private ConnectedUserRepository connectedUserRepository;
    private ConnectedUserServiceImpl connectedUserService;

    @BeforeEach
    void setUp() {
        connectedUserRepository = mock(ConnectedUserRepository.class);
        connectedUserService = new ConnectedUserServiceImpl(connectedUserRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "app-events", "automation", "components", "external", "integration-instances", "integrations", "me",
        "unified", "workflows"
    })
    void testCreateConnectedUserWithReservedExternalIdThrowsIllegalArgumentException(String reservedExternalId) {
        assertThatThrownBy(() -> connectedUserService.createConnectedUser(reservedExternalId, Environment.PRODUCTION))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(reservedExternalId);

        verifyNoInteractions(connectedUserRepository);
    }

    @Test
    void testCreateConnectedUserWithNormalExternalIdSucceeds() {
        String externalId = "real-external-user-42";

        when(connectedUserRepository.save(any(ConnectedUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ConnectedUser connectedUser = connectedUserService.createConnectedUser(externalId, Environment.PRODUCTION);

        assertThat(connectedUser.getExternalId()).isEqualTo(externalId);
        assertThat(connectedUser.isEnabled()).isTrue();
        assertThat(connectedUser.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }
}
