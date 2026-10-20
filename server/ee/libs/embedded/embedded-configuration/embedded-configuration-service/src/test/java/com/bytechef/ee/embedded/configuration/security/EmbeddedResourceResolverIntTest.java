/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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

    @AfterEach
    void afterEach() {
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM connected_user_project");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
    }

    @Test
    void testAnIntegrationWorkflowIsEmbeddedAndAnUnknownWorkflowIsNot() {
        Integration integration = integrationRepository.save(
            Integration.builder()
                .componentName("componentName")
                .name("Name")
                .build());

        integrationWorkflowService.addWorkflow(
            Validate.notNull(integration.getId(), "id"),
            Validate.notNull(integration.getLastIntegrationVersion(), "lastVersion"), "integration-workflow");

        assertThat(embeddedResourceResolver.isEmbeddedWorkflow("integration-workflow")).isTrue();
        assertThat(embeddedResourceResolver.isEmbeddedWorkflow(UUID.randomUUID() + "")).isFalse();
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
