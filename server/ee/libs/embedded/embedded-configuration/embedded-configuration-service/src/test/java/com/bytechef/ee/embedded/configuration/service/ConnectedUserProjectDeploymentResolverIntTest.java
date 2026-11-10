/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.service.ConnectedUserProjectDeploymentResolver;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class,
    properties = "spring.liquibase.contexts=configuration,user,execution")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class ConnectedUserProjectDeploymentResolverIntTest {

    @Autowired
    private ConnectedUserProjectDeploymentResolver connectedUserProjectDeploymentResolver;

    @Autowired
    private ConnectedUserProjectService connectedUserProjectService;

    @Autowired
    private ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM connected_user_reference_deployment");
        jdbcTemplate.update("DELETE FROM connected_user_project");
        jdbcTemplate.update("DELETE FROM project_deployment");
        jdbcTemplate.update("DELETE FROM connected_user");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
    }

    @Test
    void testGetConnectedUserIdsResolvesReferenceAndCopyDeployments() {
        long workspaceId = insertWorkspace();

        long referencingConnectedUserId = insertConnectedUser("referencing-user");
        long copyingConnectedUserId = insertConnectedUser("copying-user");

        long catalogProjectId = insertProject(workspaceId);
        long copyProjectId = insertProject(workspaceId);
        long plainProjectId = insertProject(workspaceId);

        long referenceProjectDeploymentId = insertProjectDeployment(catalogProjectId);
        long copyProjectDeploymentId = insertProjectDeployment(copyProjectId);
        long plainProjectDeploymentId = insertProjectDeployment(plainProjectId);

        connectedUserReferenceDeploymentService.create(
            referencingConnectedUserId, catalogProjectId, referenceProjectDeploymentId);
        connectedUserProjectService.create(copyingConnectedUserId, copyProjectId);

        Map<Long, Long> connectedUserIds = connectedUserProjectDeploymentResolver.getConnectedUserIds(
            List.of(referenceProjectDeploymentId, copyProjectDeploymentId, plainProjectDeploymentId));

        assertThat(connectedUserIds).containsExactlyInAnyOrderEntriesOf(
            Map.of(
                referenceProjectDeploymentId, referencingConnectedUserId,
                copyProjectDeploymentId, copyingConnectedUserId));
    }

    @Test
    void testGetConnectedUserIdsOfNoDeploymentsIsEmpty() {
        assertThat(connectedUserProjectDeploymentResolver.getConnectedUserIds(List.of())).isEmpty();
    }

    private long insertConnectedUser(String externalId) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO connected_user
                        (external_id, enabled, environment, created_date, created_by, last_modified_date,
                         last_modified_by, version)
                    VALUES (?, true, ?, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, externalId, Environment.PRODUCTION.ordinal()),
            "connected user id");
    }

    private long insertProject(long workspaceId) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO project
                        (name, uuid, workspace_id, created_date, created_by, last_modified_date, last_modified_by,
                         version)
                    VALUES ('test', gen_random_uuid(), ?, now(), 'test', now(), 'test', 0)
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
}
