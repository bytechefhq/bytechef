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
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Objects;
import java.util.UUID;
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
class AutomationWorkflowProjectConfigurationServiceIntTest {

    private static final long ENVIRONMENT_ID = 0L;

    @Autowired
    private AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM automation_workflow_project_configuration_workflow");
        jdbcTemplate.update("DELETE FROM automation_workflow_project_configuration");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
    }

    @Test
    void testCreateAndFetchByProjectAndEnvironment() {
        long projectId = insertProject(insertWorkspace());

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.create(projectId, ENVIRONMENT_ID, 2, false, true);

        assertThat(configuration.getId()).isNotNull();
        assertThat(automationWorkflowProjectConfigurationService.fetchConfiguration(projectId, ENVIRONMENT_ID))
            .hasValueSatisfying(fetchedConfiguration -> {
                assertThat(fetchedConfiguration.getId()).isEqualTo(configuration.getId());
                assertThat(fetchedConfiguration.getProjectId()).isEqualTo(projectId);
                assertThat(fetchedConfiguration.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                assertThat(fetchedConfiguration.getProjectVersion()).isEqualTo(2);
                assertThat(fetchedConfiguration.isEnabled()).isFalse();
                assertThat(fetchedConfiguration.isAutomationHubVisible()).isTrue();
            });
        assertThat(automationWorkflowProjectConfigurationService.fetchConfiguration(projectId, 1L)).isEmpty();
        assertThat(automationWorkflowProjectConfigurationService.getConfigurations(projectId)).hasSize(1);
    }

    @Test
    void testCreateTwiceForTheSameEnvironmentThrows() {
        long projectId = insertProject(insertWorkspace());

        automationWorkflowProjectConfigurationService.create(projectId, ENVIRONMENT_ID, 1, true, true);

        assertThatThrownBy(
            () -> automationWorkflowProjectConfigurationService.create(projectId, ENVIRONMENT_ID, 1, true, true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testFetchConfigurationForUpdateReturnsTheConfigurationWithItsWorkflows() {
        long projectId = insertProject(insertWorkspace());
        UUID workflowUuid = UUID.randomUUID();

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.create(projectId, ENVIRONMENT_ID, 3, true, true);

        automationWorkflowProjectConfigurationService.updateWorkflowEnabled(configuration.getId(), workflowUuid, false);

        assertThat(automationWorkflowProjectConfigurationService.fetchConfigurationForUpdate(projectId, ENVIRONMENT_ID))
            .hasValueSatisfying(fetchedConfiguration -> {
                assertThat(fetchedConfiguration.getId()).isEqualTo(configuration.getId());
                assertThat(fetchedConfiguration.getProjectVersion()).isEqualTo(3);
                assertThat(fetchedConfiguration.isWorkflowEnabled(workflowUuid)).isFalse();
            });
        assertThat(automationWorkflowProjectConfigurationService.fetchConfigurationForUpdate(projectId, 1L)).isEmpty();
    }

    @Test
    void testIsWorkflowEnabledDefaultsToTrueWithoutARow() {
        long projectId = insertProject(insertWorkspace());

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.create(projectId, ENVIRONMENT_ID, 1, true, true);

        assertThat(automationWorkflowProjectConfigurationService.isWorkflowEnabled(configuration, UUID.randomUUID()))
            .isTrue();
    }

    @Test
    void testUpdateWorkflowEnabledUpsertsAndReads() {
        long projectId = insertProject(insertWorkspace());
        UUID workflowUuid = UUID.randomUUID();

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.create(projectId, ENVIRONMENT_ID, 1, true, true);

        configuration = automationWorkflowProjectConfigurationService.updateWorkflowEnabled(
            configuration.getId(), workflowUuid, false);

        assertThat(automationWorkflowProjectConfigurationService.isWorkflowEnabled(configuration, workflowUuid))
            .isFalse();
        assertThat(countWorkflowRows()).isEqualTo(1);

        configuration = automationWorkflowProjectConfigurationService.updateWorkflowEnabled(
            configuration.getId(), workflowUuid, true);

        assertThat(automationWorkflowProjectConfigurationService.isWorkflowEnabled(configuration, workflowUuid))
            .isTrue();
        assertThat(countWorkflowRows()).isEqualTo(1);

        AutomationWorkflowProjectConfiguration reloadedConfiguration =
            automationWorkflowProjectConfigurationService.getConfiguration(configuration.getId());

        assertThat(automationWorkflowProjectConfigurationService.isWorkflowEnabled(reloadedConfiguration, workflowUuid))
            .isTrue();
    }

    @Test
    void testUpdateAndUpdateEnabled() {
        long projectId = insertProject(insertWorkspace());

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.create(projectId, ENVIRONMENT_ID, 1, false, true);

        configuration = automationWorkflowProjectConfigurationService.update(configuration.getId(), 3, true, false);

        assertThat(configuration.getProjectVersion()).isEqualTo(3);
        assertThat(configuration.isEnabled()).isTrue();
        assertThat(configuration.isAutomationHubVisible()).isFalse();

        configuration = automationWorkflowProjectConfigurationService.updateEnabled(configuration.getId(), false);

        assertThat(configuration.isEnabled()).isFalse();
        assertThat(configuration.getProjectVersion()).isEqualTo(3);
    }

    @Test
    void testDeleteRemovesWorkflowRows() {
        long projectId = insertProject(insertWorkspace());

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.create(projectId, ENVIRONMENT_ID, 1, true, true);

        automationWorkflowProjectConfigurationService.updateWorkflowEnabled(
            configuration.getId(), UUID.randomUUID(), false);

        assertThat(countWorkflowRows()).isEqualTo(1);

        automationWorkflowProjectConfigurationService.delete(configuration.getId());

        assertThat(countWorkflowRows()).isZero();
        assertThat(automationWorkflowProjectConfigurationService.fetchConfiguration(projectId, ENVIRONMENT_ID))
            .isEmpty();
    }

    private int countWorkflowRows() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM automation_workflow_project_configuration_workflow", Integer.class));
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
