/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Objects;
import java.util.Optional;
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
class ConnectedUserProjectWorkflowRepositoryIntTest {

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        connectedUserProjectWorkflowRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM connected_user_project");
        jdbcTemplate.update("DELETE FROM project_workflow");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
    }

    @Test
    void testReferenceColumnsRoundTrip() {
        long connectedUserProjectId = insertConnectedUserProject(insertProject(insertWorkspace()));

        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setConnectedUserProjectId(connectedUserProjectId);
        connectedUserProjectWorkflow.setAutomationWorkflowUuid("11111111-1111-1111-1111-111111111111");
        connectedUserProjectWorkflow.setProjectDeploymentId(42L);
        connectedUserProjectWorkflow.setEnabled(true);
        connectedUserProjectWorkflow.setDangling(false);

        ConnectedUserProjectWorkflow saved = connectedUserProjectWorkflowRepository.save(connectedUserProjectWorkflow);

        ConnectedUserProjectWorkflow reloaded = connectedUserProjectWorkflowRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(reloaded.getAutomationWorkflowUuid()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(reloaded.getProjectDeploymentId()).isEqualTo(42L);
        assertThat(reloaded.isEnabled()).isTrue();
        assertThat(reloaded.isDangling()).isFalse();
        assertThat(reloaded.getProjectWorkflowId()).isNull();
    }

    @Test
    void testCopyModeRowsAreUnaffected() {
        long workspaceId = insertWorkspace();
        long projectId = insertProject(workspaceId);
        long connectedUserProjectId = insertConnectedUserProject(projectId);
        long projectWorkflowId = insertProjectWorkflow(projectId);

        ConnectedUserProjectWorkflow copyModeRow = new ConnectedUserProjectWorkflow();

        copyModeRow.setConnectedUserProjectId(connectedUserProjectId);
        copyModeRow.setProjectWorkflowId(projectWorkflowId);

        ConnectedUserProjectWorkflow saved = connectedUserProjectWorkflowRepository.save(copyModeRow);

        ConnectedUserProjectWorkflow reloaded = connectedUserProjectWorkflowRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(reloaded.getAutomationWorkflowUuid()).isNull();
        assertThat(reloaded.isEnabled()).isTrue();
        assertThat(reloaded.isDangling()).isFalse();
    }

    @Test
    void testFindByConnectedUserProjectIdAndAutomationWorkflowUuid() {
        long connectedUserProjectId = insertConnectedUserProject(insertProject(insertWorkspace()));

        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setConnectedUserProjectId(connectedUserProjectId);
        connectedUserProjectWorkflow.setAutomationWorkflowUuid("22222222-2222-2222-2222-222222222222");

        connectedUserProjectWorkflowRepository.save(connectedUserProjectWorkflow);

        Optional<ConnectedUserProjectWorkflow> found = connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndAutomationWorkflowUuid(
                connectedUserProjectId, "22222222-2222-2222-2222-222222222222");

        assertThat(found).isPresent();
    }

    @Test
    void testConstraintRejectsBothProjectWorkflowIdAndAutomationWorkflowUuid() {
        long workspaceId = insertWorkspace();
        long projectId = insertProject(workspaceId);
        long connectedUserProjectId = insertConnectedUserProject(projectId);
        long projectWorkflowId = insertProjectWorkflow(projectId);

        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                """
                    INSERT INTO connected_user_project_workflow
                        (connected_user_project_id, project_workflow_id, automation_workflow_uuid)
                    VALUES (?, ?, ?)
                    """,
                connectedUserProjectId, projectWorkflowId, "33333333-3333-3333-3333-333333333333");
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testConstraintRejectsNeitherProjectWorkflowIdNorAutomationWorkflowUuid() {
        long connectedUserProjectId = insertConnectedUserProject(insertProject(insertWorkspace()));

        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                """
                    INSERT INTO connected_user_project_workflow (connected_user_project_id)
                    VALUES (?)
                    """,
                connectedUserProjectId);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testPartialUniqueIndexRejectsDuplicateReferenceRow() {
        long connectedUserProjectId = insertConnectedUserProject(insertProject(insertWorkspace()));

        String automationWorkflowUuid = "44444444-4444-4444-4444-444444444444";

        jdbcTemplate.update(
            """
                INSERT INTO connected_user_project_workflow (connected_user_project_id, automation_workflow_uuid)
                VALUES (?, ?)
                """,
            connectedUserProjectId, automationWorkflowUuid);

        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                """
                    INSERT INTO connected_user_project_workflow (connected_user_project_id, automation_workflow_uuid)
                    VALUES (?, ?)
                    """,
                connectedUserProjectId, automationWorkflowUuid);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testCopiedFromWorkflowUuidRoundTrip() {
        long workspaceId = insertWorkspace();
        long projectId = insertProject(workspaceId);
        long connectedUserProjectId = insertConnectedUserProject(projectId);
        long projectWorkflowId = insertProjectWorkflow(projectId);

        ConnectedUserProjectWorkflow copyModeRow = new ConnectedUserProjectWorkflow();

        copyModeRow.setConnectedUserProjectId(connectedUserProjectId);
        copyModeRow.setProjectWorkflowId(projectWorkflowId);
        copyModeRow.setCopiedFromWorkflowUuid("55555555-5555-5555-5555-555555555555");

        ConnectedUserProjectWorkflow saved = connectedUserProjectWorkflowRepository.save(copyModeRow);

        ConnectedUserProjectWorkflow reloaded = connectedUserProjectWorkflowRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(reloaded.getCopiedFromWorkflowUuid()).isEqualTo("55555555-5555-5555-5555-555555555555");
    }

    @Test
    void testPartialUniqueIndexRejectsDuplicateCopyFromSameTemplate() {
        long workspaceId = insertWorkspace();
        long projectId = insertProject(workspaceId);
        long connectedUserProjectId = insertConnectedUserProject(projectId);
        long firstProjectWorkflowId = insertProjectWorkflow(projectId, "workflow1");
        long secondProjectWorkflowId = insertProjectWorkflow(projectId, "workflow2");

        String templateUuid = "66666666-6666-6666-6666-666666666666";

        jdbcTemplate.update(
            """
                INSERT INTO connected_user_project_workflow
                    (connected_user_project_id, project_workflow_id, copied_from_workflow_uuid)
                VALUES (?, ?, ?)
                """,
            connectedUserProjectId, firstProjectWorkflowId, templateUuid);

        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                """
                    INSERT INTO connected_user_project_workflow
                        (connected_user_project_id, project_workflow_id, copied_from_workflow_uuid)
                    VALUES (?, ?, ?)
                    """,
                connectedUserProjectId, secondProjectWorkflowId, templateUuid);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testPartialUniqueIndexExemptsRowsWithoutCopiedFromWorkflowUuid() {
        long workspaceId = insertWorkspace();
        long projectId = insertProject(workspaceId);
        long connectedUserProjectId = insertConnectedUserProject(projectId);
        long firstProjectWorkflowId = insertProjectWorkflow(projectId, "workflow1");
        long secondProjectWorkflowId = insertProjectWorkflow(projectId, "workflow2");

        ConnectedUserProjectWorkflow firstCopyModeRow = new ConnectedUserProjectWorkflow();

        firstCopyModeRow.setConnectedUserProjectId(connectedUserProjectId);
        firstCopyModeRow.setProjectWorkflowId(firstProjectWorkflowId);

        ConnectedUserProjectWorkflow secondCopyModeRow = new ConnectedUserProjectWorkflow();

        secondCopyModeRow.setConnectedUserProjectId(connectedUserProjectId);
        secondCopyModeRow.setProjectWorkflowId(secondProjectWorkflowId);

        ConnectedUserProjectWorkflow savedFirstCopyModeRow = connectedUserProjectWorkflowRepository.save(
            firstCopyModeRow);
        ConnectedUserProjectWorkflow savedSecondCopyModeRow = connectedUserProjectWorkflowRepository.save(
            secondCopyModeRow);

        assertThat(connectedUserProjectWorkflowRepository.findById(savedFirstCopyModeRow.getId())).isPresent();
        assertThat(connectedUserProjectWorkflowRepository.findById(savedSecondCopyModeRow.getId())).isPresent();
    }

    @Test
    void testPartialUniqueIndexExemptsCopyModeRows() {
        long workspaceId = insertWorkspace();
        long projectId = insertProject(workspaceId);
        long connectedUserProjectId = insertConnectedUserProject(projectId);
        long firstProjectWorkflowId = insertProjectWorkflow(projectId, "workflow1");
        long secondProjectWorkflowId = insertProjectWorkflow(projectId, "workflow2");

        ConnectedUserProjectWorkflow firstCopyModeRow = new ConnectedUserProjectWorkflow();

        firstCopyModeRow.setConnectedUserProjectId(connectedUserProjectId);
        firstCopyModeRow.setProjectWorkflowId(firstProjectWorkflowId);

        ConnectedUserProjectWorkflow secondCopyModeRow = new ConnectedUserProjectWorkflow();

        secondCopyModeRow.setConnectedUserProjectId(connectedUserProjectId);
        secondCopyModeRow.setProjectWorkflowId(secondProjectWorkflowId);

        ConnectedUserProjectWorkflow savedFirstCopyModeRow = connectedUserProjectWorkflowRepository.save(
            firstCopyModeRow);
        ConnectedUserProjectWorkflow savedSecondCopyModeRow = connectedUserProjectWorkflowRepository.save(
            secondCopyModeRow);

        assertThat(connectedUserProjectWorkflowRepository.findById(savedFirstCopyModeRow.getId())).isPresent();
        assertThat(connectedUserProjectWorkflowRepository.findById(savedSecondCopyModeRow.getId())).isPresent();
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
                    VALUES ('test', gen_random_uuid(), ?, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, workspaceId),
            "project id");
    }

    private long insertProjectWorkflow(long projectId) {
        return insertProjectWorkflow(projectId, "workflow1");
    }

    private long insertProjectWorkflow(long projectId, String workflowId) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO project_workflow
                        (project_id, workflow_id, uuid, project_version, created_date, created_by, last_modified_date,
                         last_modified_by, version)
                    VALUES (?, ?, gen_random_uuid(), 1, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, projectId, workflowId),
            "project workflow id");
    }

    private long insertConnectedUserProject(long projectId) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                "INSERT INTO connected_user_project (connected_user_id, project_id) VALUES (1, ?) RETURNING id",
                Long.class, projectId),
            "connected user project id");
    }
}
