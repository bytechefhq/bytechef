/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
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
    classes = IntegrationIntTestConfiguration.class, properties = "bytechef.edition=EE")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class EmbeddedResourceResolverIntTest {

    @Autowired
    private EmbeddedResourceResolverImpl embeddedResourceResolver;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @AfterEach
    void afterEach() {
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM automation_workflow_project");
        jdbcTemplate.update("DELETE FROM connected_user_project");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
    }

    @Test
    void testAnIntegrationWorkflowIsAnIntegrationWorkflowAndAnUnknownWorkflowIsNot() {
        Integration integration = integrationRepository.save(
            Integration.builder()
                .componentName("componentName")
                .name("Name")
                .build());

        integrationWorkflowService.addWorkflow(
            Validate.notNull(integration.getId(), "id"),
            Validate.notNull(integration.getLastIntegrationVersion(), "lastVersion"), "integration-workflow");

        String unknownWorkflowId = UUID.randomUUID() + "";

        assertThat(embeddedResourceResolver.isIntegrationWorkflow("integration-workflow")).isTrue();
        assertThat(embeddedResourceResolver.isEmbeddedProjectWorkflow("integration-workflow")).isFalse();
        assertThat(embeddedResourceResolver.isIntegrationWorkflow(unknownWorkflowId)).isFalse();
        assertThat(embeddedResourceResolver.isEmbeddedProjectWorkflow(unknownWorkflowId)).isFalse();
    }

    @Test
    void testAWorkflowOfAHubProjectIsEmbeddedThroughItsProjectButIsNoIntegrationWorkflow() {
        long workspaceId = insertWorkspace();
        long hubProjectId = insertProject(workspaceId);
        long workspaceProjectId = insertProject(workspaceId);

        jdbcTemplate.update(
            "INSERT INTO connected_user_project (connected_user_id, project_id) VALUES (1, ?)", hubProjectId);

        when(projectWorkflowService.getWorkflowProjectWorkflows(List.of("hub-workflow")))
            .thenReturn(List.of(new ProjectWorkflow(hubProjectId, 1, "hub-workflow")));
        when(projectWorkflowService.getWorkflowProjectWorkflows(List.of("workspace-workflow")))
            .thenReturn(List.of(new ProjectWorkflow(workspaceProjectId, 1, "workspace-workflow")));

        assertThat(embeddedResourceResolver.isEmbeddedProjectWorkflow("hub-workflow")).isTrue();
        assertThat(embeddedResourceResolver.isIntegrationWorkflow("hub-workflow")).isFalse();
        assertThat(embeddedResourceResolver.isEmbeddedProjectWorkflow("workspace-workflow")).isFalse();
    }

    @Test
    void testAHubProjectIsEmbeddedAndAnotherProjectIsNot() {
        long workspaceId = insertWorkspace();
        long hubProjectId = insertProject(workspaceId);
        long workspaceProjectId = insertProject(workspaceId);

        jdbcTemplate.update(
            "INSERT INTO connected_user_project (connected_user_id, project_id) VALUES (1, ?)", hubProjectId);

        assertThat(embeddedResourceResolver.isEmbeddedProject(hubProjectId)).isTrue();
        assertThat(embeddedResourceResolver.isEmbeddedProject(workspaceProjectId)).isFalse();
        assertThat(embeddedResourceResolver.isEmbeddedProject(Long.MAX_VALUE)).isFalse();
    }

    @Test
    void testOnlyAnAutomationHubProjectAndItsWorkflowsAreHub() {
        long workspaceId = insertWorkspace();
        long automationHubProjectId = insertProject(workspaceId);
        long connectedUserProjectId = insertProject(workspaceId);

        jdbcTemplate.update("INSERT INTO automation_workflow_project (project_id) VALUES (?)", automationHubProjectId);
        jdbcTemplate.update(
            "INSERT INTO connected_user_project (connected_user_id, project_id) VALUES (1, ?)",
            connectedUserProjectId);

        when(projectWorkflowService.getWorkflowProjectWorkflows(List.of("automation-hub-workflow")))
            .thenReturn(List.of(new ProjectWorkflow(automationHubProjectId, 1, "automation-hub-workflow")));
        when(projectWorkflowService.getWorkflowProjectWorkflows(List.of("connected-user-workflow")))
            .thenReturn(List.of(new ProjectWorkflow(connectedUserProjectId, 1, "connected-user-workflow")));

        assertThat(embeddedResourceResolver.isHubProject(automationHubProjectId)).isTrue();
        assertThat(embeddedResourceResolver.isEmbeddedProject(automationHubProjectId)).isTrue();
        assertThat(embeddedResourceResolver.isHubProject(connectedUserProjectId)).isFalse();
        assertThat(embeddedResourceResolver.isHubProject(Long.MAX_VALUE)).isFalse();
        assertThat(embeddedResourceResolver.isHubProjectWorkflow("automation-hub-workflow")).isTrue();
        assertThat(embeddedResourceResolver.isHubProjectWorkflow("connected-user-workflow")).isFalse();
        assertThat(embeddedResourceResolver.isEmbeddedProjectWorkflow("connected-user-workflow")).isTrue();
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
}
