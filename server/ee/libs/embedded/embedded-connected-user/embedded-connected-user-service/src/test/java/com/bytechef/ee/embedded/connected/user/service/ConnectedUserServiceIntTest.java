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

import com.bytechef.ee.embedded.connected.user.config.ConnectedUserIntTestConfiguration;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.repository.ConnectedUserRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ConnectedUserIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "spring.liquibase.contexts=user"
    })
@Import(PostgreSQLContainerConfiguration.class)
class ConnectedUserServiceIntTest {

    @Autowired
    private ConnectedUserRepository connectedUserRepository;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        connectedUserRepository.deleteAll();
    }

    @Test
    void testCreatePersistsNameEmailAndMetadata() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "customer-a", Environment.PRODUCTION, "Acme", "ops@acme.io", Map.of("a", "1", "b", "2"));

        ConnectedUser storedConnectedUser = connectedUserService.getConnectedUser(getId(connectedUser));

        assertThat(storedConnectedUser.getExternalId()).isEqualTo("customer-a");
        assertThat(storedConnectedUser.getName()).isEqualTo("Acme");
        assertThat(storedConnectedUser.getEmail()).isEqualTo("ops@acme.io");
        assertThat(storedConnectedUser.isEnabled()).isTrue();
        assertThat(storedConnectedUser.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(storedConnectedUser.getMetadata()).isEqualTo(Map.of("a", "1", "b", "2"));
        assertThat(metadataRows(getId(connectedUser))).isEqualTo(Map.of("a", "1", "b", "2"));
    }

    @Test
    void testCreateWithNullMetadataPersistsNoMetadata() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "customer-a", Environment.PRODUCTION, null, null, null);

        assertThat(connectedUserService.getConnectedUser(getId(connectedUser))
            .getMetadata()).isEmpty();
        assertThat(metadataRows(getId(connectedUser))).isEmpty();
    }

    @Test
    void testUpdateReplacesMetadataInTheDatabase() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "customer-a", Environment.PRODUCTION, null, null, Map.of("a", "1", "b", "2"));

        connectedUserService.updateConnectedUser(
            getId(connectedUser), "Acme", null, Map.of("a", "1"), connectedUser.getVersion());

        ConnectedUser storedConnectedUser = connectedUserService.getConnectedUser(getId(connectedUser));

        assertThat(storedConnectedUser.getMetadata()).isEqualTo(Map.of("a", "1"));
        assertThat(storedConnectedUser.getName()).isEqualTo("Acme");
        assertThat(metadataRows(getId(connectedUser))).isEqualTo(Map.of("a", "1"));
    }

    @Test
    void testUpdateWithNullMetadataClearsMetadata() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "customer-a", Environment.PRODUCTION, null, null, Map.of("a", "1"));

        connectedUserService.updateConnectedUser(getId(connectedUser), null, null, null, connectedUser.getVersion());

        assertThat(metadataRows(getId(connectedUser))).isEmpty();
    }

    @Test
    void testUpdateWithTheOldVersionAfterAnUpdateIsRefused() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "customer-a", Environment.PRODUCTION, null, null, Map.of("a", "1"));

        int originalVersion = connectedUser.getVersion();

        ConnectedUser updatedConnectedUser = connectedUserService.updateConnectedUser(
            getId(connectedUser), "First", null, Map.of("a", "2"), originalVersion);

        assertThat(updatedConnectedUser.getVersion()).isNotEqualTo(originalVersion);

        assertThatThrownBy(
            () -> connectedUserService.updateConnectedUser(
                getId(connectedUser), "Second", null, Map.of("a", "3"), originalVersion))
                    .isInstanceOf(OptimisticLockingFailureException.class);

        ConnectedUser storedConnectedUser = connectedUserService.getConnectedUser(getId(connectedUser));

        assertThat(storedConnectedUser.getName()).isEqualTo("First");
        assertThat(metadataRows(getId(connectedUser))).isEqualTo(Map.of("a", "2"));
    }

    @Test
    void testAdminCreatedUserIsReusedBySdkAuthentication() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "customer-a", Environment.STAGING, "Acme", null, Map.of());

        long environmentId = Environment.STAGING.ordinal();

        ConnectedUser sdkConnectedUser = connectedUserService.fetchConnectedUser("customer-a", environmentId)
            .orElseGet(() -> connectedUserService.createConnectedUser("customer-a", environmentId));

        assertThat(sdkConnectedUser.getId()).isEqualTo(connectedUser.getId());
        assertThat(sdkConnectedUser.getName()).isEqualTo("Acme");
        assertThat(connectedUserRepository.count()).isEqualTo(1);
    }

    private Map<String, String> metadataRows(long connectedUserId) {
        Map<String, String> metadata = new HashMap<>();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT key, value FROM connected_user_metadata WHERE connected_user_id = ?", connectedUserId);

        for (Map<String, Object> row : rows) {
            metadata.put((String) row.get("key"), (String) row.get("value"));
        }

        return metadata;
    }

    private static long getId(ConnectedUser connectedUser) {
        return Objects.requireNonNull(connectedUser.getId());
    }

    @Nested
    class WithMockedRepository {

        private ConnectedUserRepository mockedConnectedUserRepository;
        private ConnectedUserServiceImpl mockedConnectedUserService;

        @BeforeEach
        void setUp() {
            mockedConnectedUserRepository = mock(ConnectedUserRepository.class);
            mockedConnectedUserService = new ConnectedUserServiceImpl(mockedConnectedUserRepository);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "app-events", "automation", "components", "external", "integration-instances", "integrations", "me",
            "unified", "workflows"
        })
        void testCreateConnectedUserWithReservedExternalIdThrowsIllegalArgumentException(String reservedExternalId) {
            assertThatThrownBy(
                () -> mockedConnectedUserService.createConnectedUser(reservedExternalId, Environment.PRODUCTION))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(reservedExternalId);

            verifyNoInteractions(mockedConnectedUserRepository);
        }

        @Test
        void testCreateConnectedUserWithNormalExternalIdSucceeds() {
            String externalId = "real-external-user-42";

            when(mockedConnectedUserRepository.save(any(ConnectedUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            ConnectedUser connectedUser = mockedConnectedUserService.createConnectedUser(
                externalId, Environment.PRODUCTION);

            assertThat(connectedUser.getExternalId()).isEqualTo(externalId);
            assertThat(connectedUser.isEnabled()).isTrue();
            assertThat(connectedUser.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        }

        @Test
        void testCreateStoresNameEmailAndMetadata() {
            when(mockedConnectedUserRepository.save(any(ConnectedUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            ConnectedUser connectedUser = mockedConnectedUserService.createConnectedUser(
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
            when(mockedConnectedUserRepository.findByExternalIdAndEnvironment(
                "customer-z", Environment.PRODUCTION.ordinal()))
                    .thenReturn(Optional.of(new ConnectedUser()));

            assertThatThrownBy(
                () -> mockedConnectedUserService.createConnectedUser(
                    "customer-z", Environment.PRODUCTION, null, null, Map.of()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("customer-z");

            verify(mockedConnectedUserRepository, never()).save(any(ConnectedUser.class));
        }

        @Test
        void testCreateRefusesAnExternalIdWithSurroundingWhitespace() {
            assertThatThrownBy(
                () -> mockedConnectedUserService.createConnectedUser(
                    " customer-z", Environment.PRODUCTION, null, null, Map.of()))
                        .isInstanceOf(IllegalArgumentException.class);

            verify(mockedConnectedUserRepository, never()).save(any(ConnectedUser.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "", "   ",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        })
        void testCreateRefusesABlankOrTooLongExternalId(String externalId) {
            assertThatThrownBy(
                () -> mockedConnectedUserService.createConnectedUser(
                    externalId, Environment.PRODUCTION, null, null, Map.of()))
                        .isInstanceOf(IllegalArgumentException.class);

            verify(mockedConnectedUserRepository, never()).save(any(ConnectedUser.class));
        }

        @Test
        void testCreateRefusesAnInvalidEmail() {
            assertThatThrownBy(
                () -> mockedConnectedUserService.createConnectedUser(
                    "customer-z", Environment.PRODUCTION, null, "not-an-email", Map.of()))
                        .isInstanceOf(IllegalArgumentException.class);

            verify(mockedConnectedUserRepository, never()).save(any(ConnectedUser.class));
        }

        @Test
        void testUpdateReplacesMetadata() {
            ConnectedUser existing = existingConnectedUser(3);

            existing.setMetadata(Map.of("plan", "pro", "region", "eu"));

            when(mockedConnectedUserRepository.findById(7L)).thenReturn(Optional.of(existing));
            when(mockedConnectedUserRepository.save(any(ConnectedUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            ConnectedUser connectedUser = mockedConnectedUserService.updateConnectedUser(
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

            when(mockedConnectedUserRepository.findById(7L)).thenReturn(Optional.of(existing));
            when(mockedConnectedUserRepository.save(any(ConnectedUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            ConnectedUser connectedUser = mockedConnectedUserService.updateConnectedUser(7L, "", null, Map.of(), 3);

            assertThat(connectedUser.getName()).isNull();
            assertThat(connectedUser.getEmail()).isNull();
        }

        @Test
        void testUpdateRefusesAStaleVersion() {
            when(mockedConnectedUserRepository.findById(7L)).thenReturn(Optional.of(existingConnectedUser(3)));

            assertThatThrownBy(() -> mockedConnectedUserService.updateConnectedUser(7L, "Acme", null, Map.of(), 2))
                .isInstanceOf(OptimisticLockingFailureException.class);

            verify(mockedConnectedUserRepository, never()).save(any(ConnectedUser.class));
        }

        @Test
        void testSdkCreateKeepsAcceptingSurroundingWhitespaceButRejectsReservedSegment() {
            when(mockedConnectedUserRepository.save(any(ConnectedUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            ConnectedUser connectedUser = mockedConnectedUserService.createConnectedUser(
                " customer-z ", Environment.PRODUCTION);

            assertThat(connectedUser.getExternalId()).isEqualTo(" customer-z ");

            assertThatThrownBy(
                () -> mockedConnectedUserService.createConnectedUser("workflows", Environment.PRODUCTION))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        private static ConnectedUser existingConnectedUser(int version) {
            ConnectedUser connectedUser = new ConnectedUser(Map.of(), null, true, "customer-z", 7L, null, version);

            connectedUser.setEnvironment(Environment.PRODUCTION);

            return connectedUser;
        }
    }
}
