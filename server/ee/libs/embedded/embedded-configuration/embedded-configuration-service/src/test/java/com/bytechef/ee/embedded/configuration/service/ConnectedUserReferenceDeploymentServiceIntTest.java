/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class ConnectedUserReferenceDeploymentServiceIntTest {

    private static final long CONNECTED_USER_ID = 1L;
    private static final long OTHER_CONNECTED_USER_ID = 2L;

    @Autowired
    private ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM connected_user_reference_deployment");
        jdbcTemplate.update("DELETE FROM project_deployment");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
    }

    @Test
    void testCreateAndFetchConnectedUserReferenceDeployment() {
        long projectId = insertProject(insertWorkspace());
        long projectDeploymentId = insertProjectDeployment(projectId);

        ConnectedUserReferenceDeployment connectedUserReferenceDeployment = connectedUserReferenceDeploymentService
            .create(CONNECTED_USER_ID, projectId, projectDeploymentId);

        assertThat(connectedUserReferenceDeployment.getId()).isNotNull();
        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            CONNECTED_USER_ID, projectId))
                .hasValueSatisfying(fetchedConnectedUserReferenceDeployment -> {
                    assertThat(fetchedConnectedUserReferenceDeployment.getConnectedUserId())
                        .isEqualTo(CONNECTED_USER_ID);
                    assertThat(fetchedConnectedUserReferenceDeployment.getProjectId()).isEqualTo(projectId);
                    assertThat(fetchedConnectedUserReferenceDeployment.getProjectDeploymentId())
                        .isEqualTo(projectDeploymentId);
                });
        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            OTHER_CONNECTED_USER_ID, projectId)).isEmpty();
    }

    @Test
    void testIsReferenceDeployment() {
        long projectId = insertProject(insertWorkspace());
        long projectDeploymentId = insertProjectDeployment(projectId);
        long otherProjectDeploymentId = insertProjectDeployment(projectId);

        connectedUserReferenceDeploymentService.create(CONNECTED_USER_ID, projectId, projectDeploymentId);

        assertThat(connectedUserReferenceDeploymentService.isReferenceDeployment(projectDeploymentId)).isTrue();
        assertThat(connectedUserReferenceDeploymentService.isReferenceDeployment(otherProjectDeploymentId)).isFalse();
    }

    @Test
    void testCreateRejectsASecondDeploymentForTheSameConnectedUserAndProject() {
        long projectId = insertProject(insertWorkspace());
        long projectDeploymentId = insertProjectDeployment(projectId);
        long otherProjectDeploymentId = insertProjectDeployment(projectId);

        connectedUserReferenceDeploymentService.create(CONNECTED_USER_ID, projectId, projectDeploymentId);

        assertThatThrownBy(() -> connectedUserReferenceDeploymentService.create(
            CONNECTED_USER_ID, projectId, otherProjectDeploymentId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testCreateRejectsTheSameDeploymentForTwoConnectedUsers() {
        long projectId = insertProject(insertWorkspace());
        long projectDeploymentId = insertProjectDeployment(projectId);

        connectedUserReferenceDeploymentService.create(CONNECTED_USER_ID, projectId, projectDeploymentId);

        assertThatThrownBy(() -> connectedUserReferenceDeploymentService.create(
            OTHER_CONNECTED_USER_ID, projectId, projectDeploymentId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testDeletingTheProjectDeploymentDeletesTheRelation() {
        long projectId = insertProject(insertWorkspace());
        long projectDeploymentId = insertProjectDeployment(projectId);

        connectedUserReferenceDeploymentService.create(CONNECTED_USER_ID, projectId, projectDeploymentId);

        jdbcTemplate.update("DELETE FROM project_deployment WHERE id = ?", projectDeploymentId);

        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            CONNECTED_USER_ID, projectId)).isEmpty();
    }

    private long insertWorkspace() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version)
                    VALUES ('test', now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class),
            "workspace id");
    }

    private long insertProject(long workspaceId) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO project
                        (name, uuid, workspace_id, created_date, created_by, last_modified_date, last_modified_by,
                         version)
                    VALUES ('__EMBEDDED_AUTOMATION__test', gen_random_uuid(), ?, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, workspaceId),
            "project id");
    }

    private long insertProjectDeployment(long projectId) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO project_deployment
                        (project_id, project_version, name, enabled, environment, created_date, created_by,
                         last_modified_date, last_modified_by, version)
                    VALUES (?, 1, 'test', true, 2, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, projectId),
            "project deployment id");
    }
}
