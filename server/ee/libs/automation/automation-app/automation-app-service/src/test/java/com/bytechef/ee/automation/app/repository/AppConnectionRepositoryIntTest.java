/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.app.config.AppIntTestConfiguration;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppOperationType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppConnectionRepositoryIntTest {

    @Autowired
    private AppConnectionRepository appConnectionRepository;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM connection");
    }

    @Test
    void roundTripsBindingWithOperationsAndFindsByAppBindingKeyAndConnection() {
        App app = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard", "a".repeat(64)));
        long connectionId = insertConnection("slack");

        AppConnection appConnection = newBinding(app, connectionId, "sales-slack");

        appConnection.setOperations(List.of("send_message", "list_channels"), List.of("SEND_MESSAGE"));
        appConnection.setProxyEnabled(true);
        appConnection.setProxyAllowedMethodList(List.of("GET", "POST"));

        AppConnection saved = appConnectionRepository.save(appConnection);

        AppConnection found = appConnectionRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(found.getAppId()).isEqualTo(app.getId());
        assertThat(found.getConnectionId()).isEqualTo(connectionId);
        assertThat(found.getBindingKey()).isEqualTo("sales-slack");
        assertThat(found.isProxyEnabled()).isTrue();
        assertThat(found.getProxyAllowedMethodList()).containsExactly("GET", "POST");
        assertThat(found.getOperationNames(AppOperationType.ACTION)).containsExactly("list_channels", "send_message");
        assertThat(found.getOperationNames(AppOperationType.TOOL)).containsExactly("SEND_MESSAGE");
        assertThat(found.allows(AppOperationType.ACTION, "send_message")).isTrue();
        assertThat(found.allows(AppOperationType.TOOL, "send_message")).isFalse();
        assertThat(found.allows(AppOperationType.ACTION, "delete_channel")).isFalse();

        AppConnection second = newBinding(app, connectionId, "alerts-slack");

        appConnectionRepository.save(second);

        assertThat(appConnectionRepository.findAllByAppIdOrderByBindingKey(app.getId()))
            .extracting(AppConnection::getBindingKey)
            .containsExactly("alerts-slack", "sales-slack");
        assertThat(appConnectionRepository.findByAppIdAndBindingKey(app.getId(), "sales-slack")).get()
            .extracting(AppConnection::getId)
            .isEqualTo(saved.getId());
        assertThat(appConnectionRepository.findByAppIdAndBindingKey(app.getId(), "missing")).isEmpty();
        assertThat(appConnectionRepository.findAllByConnectionId(connectionId)).hasSize(2);
        assertThat(appConnectionRepository.findAllByConnectionId(connectionId + 1000)).isEmpty();
    }

    @Test
    void replacingOperationsRewritesTheAllowList() {
        App app = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard", "a".repeat(64)));
        AppConnection appConnection = newBinding(app, insertConnection("slack"), "sales-slack");

        appConnection.setOperations(List.of("send_message"), List.of());

        AppConnection saved = appConnectionRepository.save(appConnection);

        saved.setOperations(List.of(), List.of("LIST_CHANNELS"));

        appConnectionRepository.save(saved);

        AppConnection found = appConnectionRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(found.getOperationNames(AppOperationType.ACTION)).isEmpty();
        assertThat(found.getOperationNames(AppOperationType.TOOL)).containsExactly("LIST_CHANNELS");
    }

    @Test
    void deletingTheAppCascadesBindingsAndOperations() {
        App app = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard", "a".repeat(64)));
        AppConnection appConnection = newBinding(app, insertConnection("slack"), "sales-slack");

        appConnection.setOperations(List.of("send_message"), List.of("SEND_MESSAGE"));

        AppConnection saved = appConnectionRepository.save(appConnection);

        appRepository.deleteById(app.getId());

        assertThat(appConnectionRepository.findById(saved.getId())).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_connection_operation", Long.class))
            .isZero();
    }

    @Test
    void deletingABoundConnectionIsRefused() {
        App app = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard", "a".repeat(64)));
        long connectionId = insertConnection("slack");

        appConnectionRepository.save(newBinding(app, connectionId, "sales-slack"));

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM connection WHERE id = ?", connectionId))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("fk_app_connection_connection");
    }

    private long insertConnection(String componentName) {
        Long connectionId = jdbcTemplate.queryForObject(
            """
                INSERT INTO connection (
                    name, component_name, environment, connection_version, parameters, credential_status, type,
                    created_date, created_by, last_modified_date, last_modified_by, version
                ) VALUES (?, ?, 2, 1, '{}', 0, 0, now(), 'test', now(), 'test', 0)
                RETURNING id
                """,
            Long.class, componentName + " connection", componentName);

        return Objects.requireNonNull(connectionId);
    }

    private static AppConnection newBinding(App app, long connectionId, String bindingKey) {
        AppConnection appConnection = new AppConnection();

        appConnection.setAppId(app.getId());
        appConnection.setConnectionId(connectionId);
        appConnection.setBindingKey(bindingKey);

        return appConnection;
    }
}
