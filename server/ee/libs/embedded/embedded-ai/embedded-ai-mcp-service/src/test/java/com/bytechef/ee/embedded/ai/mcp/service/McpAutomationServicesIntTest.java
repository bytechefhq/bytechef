/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EmbeddedMcpIntTestConfiguration.class, properties = "bytechef.edition=ee")
public class McpAutomationServicesIntTest {

    private static final String WORKFLOW_UUID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService;

    @Autowired
    private McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService;

    @Autowired
    private McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    private long automationProjectConfigurationId;
    private long connectedUserId;
    private long connectedUserProjectId;
    private long connectedUserProjectWorkflowId;
    private McpServer mcpServer;
    private long projectId;

    @BeforeEach
    public void beforeEach() {
        mcpServer = mcpServerRepository.save(
            new McpServer("automation-server", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        long workspaceId = insert(
            """
                INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES ('test', now(), 'system', now(), 'system', 0)
                RETURNING id
                """);

        projectId = insert(
            """
                INSERT INTO project (name, uuid, workspace_id, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES ('template', gen_random_uuid(), ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            workspaceId);

        automationProjectConfigurationId = createAutomationProjectConfiguration(0);

        connectedUserId = createConnectedUser("user-1");
        connectedUserProjectId = createConnectedUserProject(connectedUserId);
        connectedUserProjectWorkflowId = createConnectedUserProjectWorkflow(connectedUserProjectId, WORKFLOW_UUID);
    }

    @AfterEach
    public void afterEach() {
        jdbcTemplate.update("DELETE FROM mcp_connected_user_project_workflow");
        jdbcTemplate.update("DELETE FROM mcp_automation_project_configuration_workflow");
        jdbcTemplate.update("DELETE FROM mcp_automation_project_configuration");
        jdbcTemplate.update("DELETE FROM connected_user_project_workflow");
        jdbcTemplate.update("DELETE FROM connected_user_project");
        jdbcTemplate.update("DELETE FROM automation_project_configuration");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
        jdbcTemplate.update("DELETE FROM connected_user");

        mcpServerRepository.deleteAll();
    }

    @Test
    public void testCreateAndReadLinkWorkflowRow() {
        McpAutomationProjectConfiguration link = mcpAutomationProjectConfigurationService.create(
            new McpAutomationProjectConfiguration(automationProjectConfigurationId, mcpServer.getId()));

        McpAutomationProjectConfigurationWorkflow linkWorkflow =
            mcpAutomationProjectConfigurationWorkflowService.create(link.getId(), WORKFLOW_UUID);

        assertThat(linkWorkflow.isEnabled()).isTrue();
        assertThat(linkWorkflow.getParameters()).isEmpty();

        mcpAutomationProjectConfigurationWorkflowService.updateParameters(
            linkWorkflow.getId(), Map.of("toolName", "sendReport"));
        mcpAutomationProjectConfigurationWorkflowService.updateEnabled(linkWorkflow.getId(), false);

        McpConnectedUserProjectWorkflow row = mcpConnectedUserProjectWorkflowService.create(
            connectedUserProjectWorkflowId, linkWorkflow.getId(), true);

        mcpConnectedUserProjectWorkflowService.updateEnabled(row.getId(), false);

        assertThat(mcpAutomationProjectConfigurationService.fetch(link.getId()))
            .hasValueSatisfying(fetched -> {
                assertThat(fetched.getMcpServerId()).isEqualTo(mcpServer.getId());
                assertThat(fetched.getAutomationProjectConfigurationId()).isEqualTo(automationProjectConfigurationId);
            });
        assertThat(mcpAutomationProjectConfigurationService.getMcpServerMcpAutomationProjectConfigurations(
            mcpServer.getId()))
                .extracting(McpAutomationProjectConfiguration::getId)
                .containsExactly(link.getId());
        assertThat(
            mcpAutomationProjectConfigurationService
                .getAutomationProjectConfigurationMcpAutomationProjectConfigurations(
                    automationProjectConfigurationId))
                        .extracting(McpAutomationProjectConfiguration::getId)
                        .containsExactly(link.getId());
        assertThat(mcpAutomationProjectConfigurationWorkflowService.fetch(linkWorkflow.getId()))
            .hasValueSatisfying(fetched -> {
                assertThat(fetched.getWorkflowUuid()).isEqualTo(WORKFLOW_UUID);
                assertThat(fetched.getParameters()
                    .get("toolName")).isEqualTo("sendReport");
                assertThat(fetched.isEnabled()).isFalse();
            });
        assertThat(mcpAutomationProjectConfigurationWorkflowService.getMcpAutomationProjectConfigurationWorkflows(
            link.getId()))
                .extracting(McpAutomationProjectConfigurationWorkflow::getId)
                .containsExactly(linkWorkflow.getId());
        assertThat(mcpConnectedUserProjectWorkflowService.fetch(row.getId()))
            .hasValueSatisfying(fetched -> assertThat(fetched.isEnabled()).isFalse());
        assertThat(
            mcpConnectedUserProjectWorkflowService.fetch(connectedUserProjectWorkflowId, linkWorkflow.getId()))
                .hasValueSatisfying(fetched -> assertThat(fetched.getId()).isEqualTo(row.getId()));
        assertThat(mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(
            connectedUserId))
                .extracting(McpConnectedUserProjectWorkflow::getId)
                .containsExactly(row.getId());
        assertThat(mcpConnectedUserProjectWorkflowService.getMcpServerMcpConnectedUserProjectWorkflows(
            mcpServer.getId()))
                .extracting(McpConnectedUserProjectWorkflow::getId)
                .containsExactly(row.getId());
    }

    @Test
    public void testDuplicateLinkRejected() {
        mcpAutomationProjectConfigurationService.create(
            new McpAutomationProjectConfiguration(automationProjectConfigurationId, mcpServer.getId()));

        assertThatThrownBy(
            () -> mcpAutomationProjectConfigurationService.create(
                new McpAutomationProjectConfiguration(automationProjectConfigurationId, mcpServer.getId())))
                    .isInstanceOfAny(DbActionExecutionException.class, DataIntegrityViolationException.class);
    }

    @Test
    public void testDeletingAutomationProjectConfigurationCascades() {
        McpAutomationProjectConfiguration link = mcpAutomationProjectConfigurationService.create(
            new McpAutomationProjectConfiguration(automationProjectConfigurationId, mcpServer.getId()));
        McpAutomationProjectConfigurationWorkflow linkWorkflow =
            mcpAutomationProjectConfigurationWorkflowService.create(link.getId(), WORKFLOW_UUID);
        McpConnectedUserProjectWorkflow row = mcpConnectedUserProjectWorkflowService.create(
            connectedUserProjectWorkflowId, linkWorkflow.getId(), true);

        jdbcTemplate.update(
            "DELETE FROM automation_project_configuration WHERE id = ?", automationProjectConfigurationId);

        assertThat(mcpAutomationProjectConfigurationService.fetch(link.getId())).isEmpty();
        assertThat(mcpAutomationProjectConfigurationWorkflowService.fetch(linkWorkflow.getId())).isEmpty();
        assertThat(mcpConnectedUserProjectWorkflowService.fetch(row.getId())).isEmpty();
    }

    @Test
    public void testDeletingReferenceCascades() {
        McpAutomationProjectConfiguration link = mcpAutomationProjectConfigurationService.create(
            new McpAutomationProjectConfiguration(automationProjectConfigurationId, mcpServer.getId()));
        McpAutomationProjectConfigurationWorkflow linkWorkflow =
            mcpAutomationProjectConfigurationWorkflowService.create(link.getId(), WORKFLOW_UUID);
        McpConnectedUserProjectWorkflow row = mcpConnectedUserProjectWorkflowService.create(
            connectedUserProjectWorkflowId, linkWorkflow.getId(), true);

        jdbcTemplate.update("DELETE FROM connected_user_project_workflow WHERE id = ?", connectedUserProjectWorkflowId);

        assertThat(mcpConnectedUserProjectWorkflowService.fetch(row.getId())).isEmpty();
        assertThat(mcpAutomationProjectConfigurationWorkflowService.fetch(linkWorkflow.getId())).isPresent();
    }

    @Test
    public void testGetConnectedUserIdsByMcpServerId() {
        McpServer otherMcpServer = mcpServerRepository.save(
            new McpServer("other-automation-server", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        long otherConfigurationId = createAutomationProjectConfiguration(1);
        long otherConnectedUserId = createConnectedUser("user-2");
        long otherReferenceId = createConnectedUserProjectWorkflow(
            createConnectedUserProject(otherConnectedUserId), WORKFLOW_UUID);

        McpAutomationProjectConfiguration link = mcpAutomationProjectConfigurationService.create(
            new McpAutomationProjectConfiguration(automationProjectConfigurationId, mcpServer.getId()));
        McpAutomationProjectConfiguration otherLink = mcpAutomationProjectConfigurationService.create(
            new McpAutomationProjectConfiguration(otherConfigurationId, otherMcpServer.getId()));

        McpAutomationProjectConfigurationWorkflow linkWorkflow =
            mcpAutomationProjectConfigurationWorkflowService.create(link.getId(), WORKFLOW_UUID);
        McpAutomationProjectConfigurationWorkflow otherLinkWorkflow =
            mcpAutomationProjectConfigurationWorkflowService.create(otherLink.getId(), WORKFLOW_UUID);

        mcpConnectedUserProjectWorkflowService.create(connectedUserProjectWorkflowId, linkWorkflow.getId(), true);
        mcpConnectedUserProjectWorkflowService.create(otherReferenceId, otherLinkWorkflow.getId(), false);

        assertThat(mcpConnectedUserProjectWorkflowService.getConnectedUserIdsByMcpServerId(mcpServer.getId()))
            .isEqualTo(Set.of(connectedUserId));
        assertThat(mcpConnectedUserProjectWorkflowService.getConnectedUserIdsByMcpServerId(otherMcpServer.getId()))
            .isEqualTo(Set.of(otherConnectedUserId));
        assertThat(
            mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(
                otherConnectedUserId))
                    .hasSize(1);
    }

    private long createAutomationProjectConfiguration(int environmentId) {
        return insert(
            """
                INSERT INTO automation_project_configuration (project_id, environment_id, project_version, enabled,
                    automation_hub_visible, name, created_date, created_by, last_modified_date, last_modified_by,
                    version)
                VALUES (?, ?, 1, true, true, ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            projectId, environmentId, "configuration-" + environmentId);
    }

    private long createConnectedUser(String externalId) {
        return insert(
            """
                INSERT INTO connected_user (external_id, enabled, environment, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, true, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            externalId);
    }

    private long createConnectedUserProject(long userId) {
        return insert(
            """
                INSERT INTO connected_user_project (connected_user_id, project_id, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            userId, projectId);
    }

    private long createConnectedUserProjectWorkflow(long userProjectId, String automationUuid) {
        return insert(
            """
                INSERT INTO connected_user_project_workflow (connected_user_project_id, automation_uuid, enabled,
                    dangling, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, true, false, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            userProjectId, automationUuid);
    }

    private long insert(String sql, Object... arguments) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Long.class, arguments));
    }
}
