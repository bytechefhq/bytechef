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
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
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
class AutomationProjectConfigurationServiceIntTest {

    private static final long ENVIRONMENT_ID = 0L;

    @Autowired
    private AutomationProjectConfigurationService automationProjectConfigurationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM automation_project_configuration_workflow");
        jdbcTemplate.update("DELETE FROM automation_project_configuration");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
    }

    @Test
    void testCreateAndFetchByProjectAndEnvironment() {
        long projectId = insertProject(insertWorkspace());

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.create(projectId, ENVIRONMENT_ID, "Default", null, 2, false,
                true);

        assertThat(configuration.getId()).isNotNull();
        assertThat(automationProjectConfigurationService.getConfigurations(projectId, ENVIRONMENT_ID)
            .stream()
            .findFirst())
                .hasValueSatisfying(fetchedConfiguration -> {
                    assertThat(fetchedConfiguration.getId()).isEqualTo(configuration.getId());
                    assertThat(fetchedConfiguration.getProjectId()).isEqualTo(projectId);
                    assertThat(fetchedConfiguration.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                    assertThat(fetchedConfiguration.getProjectVersion()).isEqualTo(2);
                    assertThat(fetchedConfiguration.isEnabled()).isFalse();
                    assertThat(fetchedConfiguration.isAutomationHubVisible()).isTrue();
                });
        assertThat(automationProjectConfigurationService.getConfigurations(projectId, 1L)
            .stream()
            .findFirst()).isEmpty();
        assertThat(automationProjectConfigurationService.getConfigurations(projectId)).hasSize(1);
    }

    @Test
    void testCreateTwiceForTheSameEnvironmentThrows() {
        long projectId = insertProject(insertWorkspace());

        automationProjectConfigurationService.create(projectId, ENVIRONMENT_ID, "Default", null, 1, true, true);

        assertThatThrownBy(
            () -> automationProjectConfigurationService.create(projectId, ENVIRONMENT_ID, "Default", null, 1,
                true, true))
                    .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testTwoConfigurationsInOneEnvironment() {
        long projectId = insertProject(insertWorkspace());

        automationProjectConfigurationService.create(projectId, ENVIRONMENT_ID, "Default", null, 1, true, true);
        automationProjectConfigurationService.create(
            projectId, ENVIRONMENT_ID, "Beta", "hasRole('beta')", 1, true, true);

        List<AutomationProjectConfiguration> configurations =
            automationProjectConfigurationService.getConfigurations(projectId, ENVIRONMENT_ID);

        assertThat(configurations).extracting(AutomationProjectConfiguration::getName)
            .containsExactlyInAnyOrder("Default", "Beta");
        assertThat(configurations).extracting(AutomationProjectConfiguration::getPermissionExpression)
            .containsExactlyInAnyOrder(null, "hasRole('beta')");
    }

    @Test
    void testDuplicateNameIsRejectedIgnoringCase() {
        long projectId = insertProject(insertWorkspace());

        automationProjectConfigurationService.create(projectId, ENVIRONMENT_ID, "Beta", null, 1, true, true);

        assertThatThrownBy(
            () -> automationProjectConfigurationService.create(
                projectId, ENVIRONMENT_ID, " beta ", null, 1, true, true))
                    .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testLockRolloutHoldsAnAdvisoryLockInTheCallersTransaction() {
        long projectId = insertProject(insertWorkspace());

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        int heldAdvisoryLockCount = Objects.requireNonNull(transactionTemplate.execute(status -> {
            automationProjectConfigurationService.lockRollout(projectId, ENVIRONMENT_ID);

            return countAdvisoryLocksHeldByThisSession();
        }));

        assertThat(heldAdvisoryLockCount).isEqualTo(1);
    }

    @Test
    void testLockRolloutOutsideATransactionThrows() {
        assertThatThrownBy(() -> automationProjectConfigurationService.lockRollout(1L, ENVIRONMENT_ID))
            .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void testRolloutLockKeyIsDeterministicPerProjectAndEnvironment() {
        long rolloutLockKey = AutomationProjectConfigurationServiceImpl.getRolloutLockKey(7L, 2L);

        assertThat(AutomationProjectConfigurationServiceImpl.getRolloutLockKey(7L, 2L))
            .isEqualTo(rolloutLockKey);
        assertThat(AutomationProjectConfigurationServiceImpl.getRolloutLockKey(7L, 1L))
            .isNotEqualTo(rolloutLockKey);
        assertThat(AutomationProjectConfigurationServiceImpl.getRolloutLockKey(72L, 2L))
            .isNotEqualTo(rolloutLockKey);
    }

    @Test
    void testIsWorkflowEnabledDefaultsToTrueWithoutARow() {
        long projectId = insertProject(insertWorkspace());

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.create(projectId, ENVIRONMENT_ID, "Default", null, 1, true,
                true);

        assertThat(automationProjectConfigurationService.isWorkflowEnabled(configuration, UUID.randomUUID()))
            .isTrue();
    }

    @Test
    void testUpdateWorkflowEnabledUpsertsAndReads() {
        long projectId = insertProject(insertWorkspace());
        UUID workflowUuid = UUID.randomUUID();

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.create(projectId, ENVIRONMENT_ID, "Default", null, 1, true,
                true);

        configuration = automationProjectConfigurationService.updateWorkflowEnabled(
            configuration.getId(), workflowUuid, false);

        assertThat(automationProjectConfigurationService.isWorkflowEnabled(configuration, workflowUuid))
            .isFalse();
        assertThat(countWorkflowRows()).isEqualTo(1);

        configuration = automationProjectConfigurationService.updateWorkflowEnabled(
            configuration.getId(), workflowUuid, true);

        assertThat(automationProjectConfigurationService.isWorkflowEnabled(configuration, workflowUuid))
            .isTrue();
        assertThat(countWorkflowRows()).isEqualTo(1);

        AutomationProjectConfiguration reloadedConfiguration =
            automationProjectConfigurationService.getConfiguration(configuration.getId());

        assertThat(automationProjectConfigurationService.isWorkflowEnabled(reloadedConfiguration, workflowUuid))
            .isTrue();
    }

    @Test
    void testUpdateWorkflowProvidedValuesPersistsBothColumnsAndCreatesAnEnabledRow() {
        long projectId = insertProject(insertWorkspace());
        UUID workflowUuid = UUID.randomUUID();

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.create(projectId, ENVIRONMENT_ID, "Default", null, 1, true,
                true);

        automationProjectConfigurationService.updateWorkflowProvidedValues(
            configuration.getId(), workflowUuid, Map.of("openai", 42L),
            Map.of("channel", "general", "limit", 3));

        assertThat(countWorkflowRows()).isEqualTo(1);

        AutomationProjectConfiguration reloadedConfiguration =
            automationProjectConfigurationService.getConfiguration(configuration.getId());

        assertThat(reloadedConfiguration.isWorkflowEnabled(workflowUuid)).isTrue();
        assertThat(reloadedConfiguration.fetchWorkflow(workflowUuid))
            .hasValueSatisfying(workflow -> {
                assertThat(workflow.getProvidedConnections()).isEqualTo(Map.of("openai", 42L));
                assertThat(workflow.getProvidedInputs()).isEqualTo(Map.of("channel", "general", "limit", 3));
            });

        automationProjectConfigurationService.updateWorkflowEnabled(configuration.getId(), workflowUuid, false);

        reloadedConfiguration = automationProjectConfigurationService.getConfiguration(configuration.getId());

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

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.create(projectId, ENVIRONMENT_ID, "Default", null, 1, true,
                true);

        automationProjectConfigurationService.updateWorkflowProvidedValues(
            configuration.getId(), workflowUuid, Map.of("openai", 42L), Map.of("channel", "general"));
        automationProjectConfigurationService.updateWorkflowProvidedValues(
            configuration.getId(), workflowUuid, Map.of(), Map.of());

        assertThat(
            automationProjectConfigurationService.getConfiguration(configuration.getId())
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

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.create(projectId, ENVIRONMENT_ID, "Default", null, 1, true,
                true);

        automationProjectConfigurationService.updateWorkflowEnabled(configuration.getId(), workflowUuid, false);

        jdbcTemplate.update(
            "UPDATE automation_project_configuration_workflow SET provided_connections = NULL, " +
                "provided_inputs = NULL");

        assertThat(
            automationProjectConfigurationService.getConfiguration(configuration.getId())
                .fetchWorkflow(workflowUuid))
                    .hasValueSatisfying(workflow -> {
                        assertThat(workflow.getProvidedConnections()).isEmpty();
                        assertThat(workflow.getProvidedInputs()).isEmpty();
                    });
    }

    @Test
    void testUpdateAndUpdateEnabled() {
        long projectId = insertProject(insertWorkspace());

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.create(projectId, ENVIRONMENT_ID, "Default", null, 1, false,
                true);

        configuration = automationProjectConfigurationService.update(configuration.getId(),
            configuration.getName(), configuration.getPermissionExpression(), 3, true, false);

        assertThat(configuration.getProjectVersion()).isEqualTo(3);
        assertThat(configuration.isEnabled()).isTrue();
        assertThat(configuration.isAutomationHubVisible()).isFalse();

        configuration = automationProjectConfigurationService.updateEnabled(configuration.getId(), false);

        assertThat(configuration.isEnabled()).isFalse();
        assertThat(configuration.getProjectVersion()).isEqualTo(3);
    }

    @Test
    void testDeleteRemovesWorkflowRows() {
        long projectId = insertProject(insertWorkspace());

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.create(projectId, ENVIRONMENT_ID, "Default", null, 1, true,
                true);

        automationProjectConfigurationService.updateWorkflowEnabled(
            configuration.getId(), UUID.randomUUID(), false);

        assertThat(countWorkflowRows()).isEqualTo(1);

        automationProjectConfigurationService.delete(configuration.getId());

        assertThat(countWorkflowRows()).isZero();
        assertThat(automationProjectConfigurationService.getConfigurations(projectId, ENVIRONMENT_ID)
            .stream()
            .findFirst())
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
                "SELECT COUNT(*) FROM automation_project_configuration_workflow", Integer.class));
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
