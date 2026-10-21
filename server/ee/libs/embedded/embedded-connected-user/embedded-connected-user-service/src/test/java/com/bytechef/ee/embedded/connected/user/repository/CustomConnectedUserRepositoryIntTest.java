/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomConnectedUserRepositoryIntTest {

    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20);

    private CustomConnectedUserRepositoryImpl customConnectedUserRepository;
    private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:" + System.nanoTime() + ";MODE=PostgreSQL;NON_KEYWORDS=KEY,VALUE;DB_CLOSE_DELAY=-1", "sa",
            "");

        jdbcClient = JdbcClient.create(dataSource);

        jdbcClient
            .sql(
                """
                    CREATE TABLE connected_user (
                        id BIGINT PRIMARY KEY, environment INT NOT NULL, external_id VARCHAR(256) NOT NULL,
                        name VARCHAR(256), email VARCHAR(256), enabled BOOLEAN NOT NULL,
                        created_by VARCHAR(256), created_date TIMESTAMP NOT NULL, last_modified_by VARCHAR(256),
                        last_modified_date TIMESTAMP, version INT NOT NULL)
                    """)
            .update();
        jdbcClient
            .sql(
                "CREATE TABLE connected_user_metadata (connected_user_id BIGINT, key VARCHAR(256), value VARCHAR(256))")
            .update();
        jdbcClient
            .sql(
                """
                    CREATE TABLE integration_instance_configuration (
                        id BIGINT PRIMARY KEY, integration_id BIGINT NOT NULL, environment INT NOT NULL,
                        name VARCHAR(256), created_date TIMESTAMP NOT NULL)
                    """)
            .update();
        jdbcClient
            .sql(
                """
                    CREATE TABLE integration_instance (
                        id BIGINT PRIMARY KEY, connected_user_id BIGINT NOT NULL,
                        integration_instance_configuration_id BIGINT NOT NULL, created_date TIMESTAMP NOT NULL)
                    """)
            .update();

        customConnectedUserRepository = new CustomConnectedUserRepositoryImpl(jdbcClient);
    }

    @Test
    void testFindAllByEnvironmentAndIntegration() {
        insertConnectedUser(1, 0, "acme-1", "Alice", "alice@acme.com");
        insertConnectedUser(2, 0, "acme-2", "Bob", "bob@acme.com");
        insertConnectedUser(3, 1, "acme-3", "Carol", "carol@acme.com");
        insertIntegrationInstanceConfiguration(10, 2, 0);
        insertIntegrationInstanceConfiguration(11, 2, 1);
        insertIntegrationInstance(100, 1, 10);
        insertIntegrationInstance(101, 3, 11);

        Page<ConnectedUser> page = customConnectedUserRepository.findAll(0, null, null, null, 2L, FIRST_PAGE);

        assertThat(getIds(page)).containsExactly(1L);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void testFindAllBySearchKeepsTheEnvironmentFilter() {
        insertConnectedUser(1, 0, "acme-1", "Alice", "alice@acme.com");
        insertConnectedUser(3, 1, "acme-3", "Carol", "carol@acme.com");

        Page<ConnectedUser> page = customConnectedUserRepository.findAll(0, "acme", null, null, null, FIRST_PAGE);

        assertThat(getIds(page)).containsExactly(1L);
    }

    @Test
    void testFindAllBySearchMatchesNameEmailAndExternalIdIgnoringCase() {
        insertConnectedUser(1, 0, "user-1", "Alice", "alice@acme.com");
        insertConnectedUser(2, 0, "user-2", "Bob", "bob@example.com");
        insertConnectedUser(3, 0, "user-3", "Carol", "carol@example.com");

        assertThat(getIds(customConnectedUserRepository.findAll(0, "ALICE", null, null, null, FIRST_PAGE)))
            .containsExactly(1L);
        assertThat(getIds(customConnectedUserRepository.findAll(0, "Example.COM", null, null, null, FIRST_PAGE)))
            .containsExactlyInAnyOrder(2L, 3L);
        assertThat(getIds(customConnectedUserRepository.findAll(0, "USER-3", null, null, null, FIRST_PAGE)))
            .containsExactly(3L);
    }

    @Test
    void testFindAllBySearchAndIntegration() {
        insertConnectedUser(1, 0, "acme-1", "Alice", "alice@acme.com");
        insertConnectedUser(2, 0, "acme-2", "Bob", "bob@acme.com");
        insertIntegrationInstanceConfiguration(10, 2, 0);
        insertIntegrationInstance(100, 1, 10);
        insertIntegrationInstance(101, 2, 10);

        Page<ConnectedUser> page = customConnectedUserRepository.findAll(0, "bob", null, null, 2L, FIRST_PAGE);

        assertThat(getIds(page)).containsExactly(2L);
    }

    @Test
    void testFindAllByIntegrationListsAConnectedUserOnce() {
        insertConnectedUser(1, 0, "acme-1", "Alice", "alice@acme.com");
        insertIntegrationInstanceConfiguration(10, 2, 0);
        insertIntegrationInstanceConfiguration(11, 2, 0);
        insertIntegrationInstance(100, 1, 10);
        insertIntegrationInstance(101, 1, 11);

        Page<ConnectedUser> page = customConnectedUserRepository.findAll(0, null, null, null, 2L, FIRST_PAGE);

        assertThat(getIds(page)).containsExactly(1L);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    private List<Long> getIds(Page<ConnectedUser> page) {
        return page.stream()
            .map(ConnectedUser::getId)
            .toList();
    }

    private void insertConnectedUser(long id, int environment, String externalId, String name, String email) {
        jdbcClient
            .sql(
                """
                    INSERT INTO connected_user (id, environment, external_id, name, email, enabled, created_date, version)
                    VALUES (?, ?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP, 0)
                    """)
            .params(id, environment, externalId, name, email)
            .update();
    }

    private void insertIntegrationInstance(long id, long connectedUserId, long integrationInstanceConfigurationId) {
        jdbcClient
            .sql(
                """
                    INSERT INTO integration_instance (id, connected_user_id, integration_instance_configuration_id,
                        created_date)
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                    """)
            .params(id, connectedUserId, integrationInstanceConfigurationId)
            .update();
    }

    private void insertIntegrationInstanceConfiguration(long id, long integrationId, int environment) {
        jdbcClient
            .sql(
                """
                    INSERT INTO integration_instance_configuration (id, integration_id, environment, name, created_date)
                    VALUES (?, ?, ?, 'Configuration', CURRENT_TIMESTAMP)
                    """)
            .params(id, integrationId, environment)
            .update();
    }
}
