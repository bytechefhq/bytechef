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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.repository.ConnectedUserRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserServiceTest {

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

    @Test
    void testCreateStoresNameEmailAndMetadata() {
        when(connectedUserRepository.save(any(ConnectedUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "customer-z", Environment.PRODUCTION, "Acme", "ops@acme.io", Map.of("plan", "pro"));

        assertThat(connectedUser.getExternalId()).isEqualTo("customer-z");
        assertThat(connectedUser.getName()).isEqualTo("Acme");
        assertThat(connectedUser.getEmail()).isEqualTo("ops@acme.io");
        assertThat(connectedUser.getMetadata()).isEqualTo(Map.of("plan", "pro"));
        assertThat(connectedUser.isEnabled()).isTrue();
        assertThat(connectedUser.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testCreateRefusesAnExistingExternalId() {
        when(connectedUserRepository.findByExternalIdAndEnvironment("customer-z", Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.of(new ConnectedUser()));

        assertThatThrownBy(
            () -> connectedUserService.createConnectedUser(
                "customer-z", Environment.PRODUCTION, null, null, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("customer-z");

        verify(connectedUserRepository, never()).save(any(ConnectedUser.class));
    }

    @Test
    void testCreateRefusesAnExternalIdWithSurroundingWhitespace() {
        assertThatThrownBy(
            () -> connectedUserService.createConnectedUser(
                " customer-z", Environment.PRODUCTION, null, null, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class);

        verify(connectedUserRepository, never()).save(any(ConnectedUser.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "   ",
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    })
    void testCreateRefusesABlankOrTooLongExternalId(String externalId) {
        assertThatThrownBy(
            () -> connectedUserService.createConnectedUser(
                externalId, Environment.PRODUCTION, null, null, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class);

        verify(connectedUserRepository, never()).save(any(ConnectedUser.class));
    }

    @Test
    void testCreateRefusesAnInvalidEmail() {
        assertThatThrownBy(
            () -> connectedUserService.createConnectedUser(
                "customer-z", Environment.PRODUCTION, null, "not-an-email", Map.of()))
                    .isInstanceOf(IllegalArgumentException.class);

        verify(connectedUserRepository, never()).save(any(ConnectedUser.class));
    }

    @Test
    void testUpdateReplacesMetadata() {
        ConnectedUser existing = existingConnectedUser(3);

        existing.setMetadata(Map.of("plan", "pro", "region", "eu"));

        when(connectedUserRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(connectedUserRepository.save(any(ConnectedUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ConnectedUser connectedUser = connectedUserService.updateConnectedUser(
            7L, "Acme", "ops@acme.io", Map.of("plan", "enterprise"), 3);

        assertThat(connectedUser.getMetadata()).isEqualTo(Map.of("plan", "enterprise"));
        assertThat(connectedUser.getExternalId()).isEqualTo("customer-z");
        assertThat(connectedUser.getName()).isEqualTo("Acme");
    }

    @Test
    void testUpdateClearsNameAndEmail() {
        ConnectedUser existing = existingConnectedUser(3);

        existing.setName("Acme");
        existing.setEmail("ops@acme.io");

        when(connectedUserRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(connectedUserRepository.save(any(ConnectedUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ConnectedUser connectedUser = connectedUserService.updateConnectedUser(7L, "", null, Map.of(), 3);

        assertThat(connectedUser.getName()).isNull();
        assertThat(connectedUser.getEmail()).isNull();
    }

    @Test
    void testUpdateRefusesAStaleVersion() {
        when(connectedUserRepository.findById(7L)).thenReturn(Optional.of(existingConnectedUser(3)));

        assertThatThrownBy(() -> connectedUserService.updateConnectedUser(7L, "Acme", null, Map.of(), 2))
            .isInstanceOf(OptimisticLockingFailureException.class);

        verify(connectedUserRepository, never()).save(any(ConnectedUser.class));
    }

    @Test
    void testSdkCreateKeepsAcceptingSurroundingWhitespaceButRejectsReservedSegment() {
        when(connectedUserRepository.save(any(ConnectedUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ConnectedUser connectedUser = connectedUserService.createConnectedUser(" customer-z ", Environment.PRODUCTION);

        assertThat(connectedUser.getExternalId()).isEqualTo(" customer-z ");

        assertThatThrownBy(() -> connectedUserService.createConnectedUser("workflows", Environment.PRODUCTION))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static ConnectedUser existingConnectedUser(int version) {
        ConnectedUser connectedUser = new ConnectedUser(Map.of(), null, true, "customer-z", 7L, null, version);

        connectedUser.setEnvironment(Environment.PRODUCTION);

        return connectedUser;
    }
}
