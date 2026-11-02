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
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
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
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class ConnectedUserProjectConfigurationServiceIntTest {

    private static final long ENVIRONMENT_ID = 0L;

    @Autowired
    private AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;

    @Autowired
    private ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM connected_user_project_configuration");
        jdbcTemplate.update("DELETE FROM automation_workflow_project_configuration_workflow");
        jdbcTemplate.update("DELETE FROM automation_workflow_project_configuration");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
    }

    @Test
    void testAssignInsertsThenMovesTheSameRow() {
        long projectId = insertProject(insertWorkspace());
        long configurationAId = createConfiguration(projectId, "A");
        long configurationBId = createConfiguration(projectId, "B");

        ConnectedUserProjectConfiguration insertedAssignment =
            connectedUserProjectConfigurationService.assign(1L, projectId, configurationAId);

        ConnectedUserProjectConfiguration movedAssignment =
            connectedUserProjectConfigurationService.assign(1L, projectId, configurationBId);

        assertThat(movedAssignment.getId()).isEqualTo(insertedAssignment.getId());
        assertThat(connectedUserProjectConfigurationService.fetchAssignment(1L, projectId))
            .hasValueSatisfying(assignment -> {
                assertThat(assignment.getConfigurationId()).isEqualTo(configurationBId);
                assertThat(assignment.getConnectedUserId()).isEqualTo(1L);
                assertThat(assignment.getProjectId()).isEqualTo(projectId);
            });
        assertThat(countRows()).isEqualTo(1);
        assertThat(connectedUserProjectConfigurationService.fetchAssignment(2L, projectId)).isEmpty();
    }

    @Test
    void testCountByConfigurationIds() {
        long projectId = insertProject(insertWorkspace());
        long configurationAId = createConfiguration(projectId, "A");
        long configurationBId = createConfiguration(projectId, "B");

        connectedUserProjectConfigurationService.assign(1L, projectId, configurationAId);
        connectedUserProjectConfigurationService.assign(2L, projectId, configurationAId);
        connectedUserProjectConfigurationService.assign(3L, projectId, configurationBId);

        assertThat(
            connectedUserProjectConfigurationService.countByConfigurationIds(
                List.of(configurationAId, configurationBId)))
                    .isEqualTo(Map.of(configurationAId, 2, configurationBId, 1));
        assertThat(connectedUserProjectConfigurationService.countByConfigurationIds(List.of())).isEmpty();
    }

    @Test
    void testDeleteByConfigurationIdLeavesOtherConfigurations() {
        long projectId = insertProject(insertWorkspace());
        long configurationAId = createConfiguration(projectId, "A");
        long configurationBId = createConfiguration(projectId, "B");

        connectedUserProjectConfigurationService.assign(1L, projectId, configurationAId);
        connectedUserProjectConfigurationService.assign(2L, projectId, configurationBId);

        connectedUserProjectConfigurationService.deleteByConfigurationId(configurationAId);

        assertThat(connectedUserProjectConfigurationService.getAssignments(configurationAId)).isEmpty();
        assertThat(connectedUserProjectConfigurationService.getAssignments(configurationBId))
            .extracting(ConnectedUserProjectConfiguration::getConnectedUserId)
            .containsExactly(2L);
    }

    private long createConfiguration(long projectId, String name) {
        AutomationWorkflowProjectConfiguration configuration = automationWorkflowProjectConfigurationService.create(
            projectId, ENVIRONMENT_ID, name, null, 1, true, true);

        return Objects.requireNonNull(configuration.getId());
    }

    private int countRows() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM connected_user_project_configuration", Integer.class));
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
}
