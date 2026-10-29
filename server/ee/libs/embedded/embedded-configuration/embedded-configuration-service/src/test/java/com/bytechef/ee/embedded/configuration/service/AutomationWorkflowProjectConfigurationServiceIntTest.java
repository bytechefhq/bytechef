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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

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
    void testLockRolloutHoldsAnAdvisoryLockInTheCallersTransaction() {
        long projectId = insertProject(insertWorkspace());

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        int heldAdvisoryLockCount = Objects.requireNonNull(transactionTemplate.execute(status -> {
            automationWorkflowProjectConfigurationService.lockRollout(projectId, ENVIRONMENT_ID);

            return countAdvisoryLocksHeldByThisSession();
        }));

        assertThat(heldAdvisoryLockCount).isEqualTo(1);
    }

    @Test
    void testLockRolloutOutsideATransactionThrows() {
        assertThatThrownBy(() -> automationWorkflowProjectConfigurationService.lockRollout(1L, ENVIRONMENT_ID))
            .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void testRolloutLockKeyIsDeterministicPerProjectAndEnvironment() {
        long rolloutLockKey = AutomationWorkflowProjectConfigurationServiceImpl.getRolloutLockKey(7L, 2L);

        assertThat(AutomationWorkflowProjectConfigurationServiceImpl.getRolloutLockKey(7L, 2L))
            .isEqualTo(rolloutLockKey);
        assertThat(AutomationWorkflowProjectConfigurationServiceImpl.getRolloutLockKey(7L, 1L))
            .isNotEqualTo(rolloutLockKey);
        assertThat(AutomationWorkflowProjectConfigurationServiceImpl.getRolloutLockKey(72L, 2L))
            .isNotEqualTo(rolloutLockKey);
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
    void testUpdateWorkflowProvidedValuesPersistsBothColumnsAndCreatesAnEnabledRow() {
        long projectId = insertProject(insertWorkspace());
        UUID workflowUuid = UUID.randomUUID();

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.create(projectId, ENVIRONMENT_ID, 1, true, true);

        automationWorkflowProjectConfigurationService.updateWorkflowProvidedValues(
            configuration.getId(), workflowUuid, Map.of("openai", 42L),
            Map.of("channel", "general", "limit", 3));

        assertThat(countWorkflowRows()).isEqualTo(1);

        AutomationWorkflowProjectConfiguration reloadedConfiguration =
            automationWorkflowProjectConfigurationService.getConfiguration(configuration.getId());

        assertThat(reloadedConfiguration.isWorkflowEnabled(workflowUuid)).isTrue();
        assertThat(reloadedConfiguration.fetchWorkflow(workflowUuid))
            .hasValueSatisfying(workflow -> {
                assertThat(workflow.getProvidedConnections()).isEqualTo(Map.of("openai", 42L));
                assertThat(workflow.getProvidedInputs()).isEqualTo(Map.of("channel", "general", "limit", 3));
            });

        automationWorkflowProjectConfigurationService.updateWorkflowEnabled(configuration.getId(), workflowUuid, false);

        reloadedConfiguration = automationWorkflowProjectConfigurationService.getConfiguration(configuration.getId());

        assertThat(reloadedConfiguration.fetchWorkflow(workflowUuid))
            .hasValueSatisfying(workflow -> {
                assertThat(workflow.isEnabled()).isFalse();
                assertThat(workflow.getProvidedConnections()).isEqualTo(Map.of("openai", 42L));
            });
    }

    @Test
    void testUpdateWorkflowProvidedValuesWithEmptyMapsClearsThem() {
        long projectId = insertProject(insertWorkspace());
        UUID workflowUuid = UUID.randomUUID();

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.create(projectId, ENVIRONMENT_ID, 1, true, true);

        automationWorkflowProjectConfigurationService.updateWorkflowProvidedValues(
            configuration.getId(), workflowUuid, Map.of("openai", 42L), Map.of("channel", "general"));
        automationWorkflowProjectConfigurationService.updateWorkflowProvidedValues(
            configuration.getId(), workflowUuid, Map.of(), Map.of());

        assertThat(
            automationWorkflowProjectConfigurationService.getConfiguration(configuration.getId())
                .fetchWorkflow(workflowUuid))
                    .hasValueSatisfying(workflow -> {
                        assertThat(workflow.getProvidedConnections()).isEmpty();
                        assertThat(workflow.getProvidedInputs()).isEmpty();
                    });
    }

    @Test
    void testWorkflowRowWithoutProvidedValuesReadsEmptyMaps() {
        long projectId = insertProject(insertWorkspace());
        UUID workflowUuid = UUID.randomUUID();

        AutomationWorkflowProjectConfiguration configuration =
            automationWorkflowProjectConfigurationService.create(projectId, ENVIRONMENT_ID, 1, true, true);

        automationWorkflowProjectConfigurationService.updateWorkflowEnabled(configuration.getId(), workflowUuid, false);

        jdbcTemplate.update(
            "UPDATE automation_workflow_project_configuration_workflow SET provided_connections = NULL, " +
                "provided_inputs = NULL");

        assertThat(
            automationWorkflowProjectConfigurationService.getConfiguration(configuration.getId())
                .fetchWorkflow(workflowUuid))
                    .hasValueSatisfying(workflow -> {
                        assertThat(workflow.getProvidedConnections()).isEmpty();
                        assertThat(workflow.getProvidedInputs()).isEmpty();
                    });
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

    private int countAdvisoryLocksHeldByThisSession() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_locks WHERE locktype = 'advisory' AND granted AND pid = pg_backend_pid()",
                Integer.class));
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
