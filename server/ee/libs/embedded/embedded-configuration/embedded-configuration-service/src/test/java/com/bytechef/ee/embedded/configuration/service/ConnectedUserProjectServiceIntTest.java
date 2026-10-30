/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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
    classes = IntegrationIntTestConfiguration.class, properties = "spring.liquibase.contexts=configuration,user")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class ConnectedUserProjectServiceIntTest {

    @Autowired
    private ConnectedUserProjectService connectedUserProjectService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM connected_user_project");
        jdbcTemplate.update("DELETE FROM connected_user");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
    }

    @Test
    void testGetConnectedUserProjectsFiltersByConnectedUserAndEnvironment() {
        long workspaceId = insertWorkspace();

        long developmentConnectedUserId = insertConnectedUser("user-1", Environment.DEVELOPMENT);
        long productionConnectedUserId = insertConnectedUser("user-1", Environment.PRODUCTION);
        long otherProductionConnectedUserId = insertConnectedUser("user-2", Environment.PRODUCTION);

        ConnectedUserProject developmentConnectedUserProject = connectedUserProjectService.create(
            developmentConnectedUserId, insertProject(workspaceId));
        ConnectedUserProject productionConnectedUserProject = connectedUserProjectService.create(
            productionConnectedUserId, insertProject(workspaceId));
        ConnectedUserProject secondProductionConnectedUserProject = connectedUserProjectService.create(
            productionConnectedUserId, insertProject(workspaceId));
        ConnectedUserProject otherProductionConnectedUserProject = connectedUserProjectService.create(
            otherProductionConnectedUserId, insertProject(workspaceId));

        assertThat(
            connectedUserProjectService.getConnectedUserProjects(productionConnectedUserId, Environment.PRODUCTION))
                .containsExactlyInAnyOrder(productionConnectedUserProject, secondProductionConnectedUserProject);
        assertThat(
            connectedUserProjectService.getConnectedUserProjects(productionConnectedUserId, Environment.DEVELOPMENT))
                .isEmpty();
        assertThat(connectedUserProjectService.getConnectedUserProjects(productionConnectedUserId, null))
            .containsExactlyInAnyOrder(productionConnectedUserProject, secondProductionConnectedUserProject);
        assertThat(connectedUserProjectService.getConnectedUserProjects(null, Environment.PRODUCTION))
            .containsExactlyInAnyOrder(
                productionConnectedUserProject, secondProductionConnectedUserProject,
                otherProductionConnectedUserProject);
        assertThat(connectedUserProjectService.getConnectedUserProjects(null, null))
            .containsExactlyInAnyOrder(
                developmentConnectedUserProject, productionConnectedUserProject, secondProductionConnectedUserProject,
                otherProductionConnectedUserProject);
    }

    private long insertConnectedUser(String externalId, Environment environment) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO connected_user
                        (external_id, enabled, environment, created_date, created_by, last_modified_date,
                         last_modified_by, version)
                    VALUES (?, true, ?, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, externalId, environment.ordinal()),
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
