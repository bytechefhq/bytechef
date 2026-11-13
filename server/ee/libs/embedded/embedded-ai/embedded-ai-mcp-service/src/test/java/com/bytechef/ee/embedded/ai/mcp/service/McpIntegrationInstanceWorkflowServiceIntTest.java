/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EmbeddedMcpIntTestConfiguration.class, properties = "bytechef.edition=ee")
public class McpIntegrationInstanceWorkflowServiceIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @Autowired
    private McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @Autowired
    private McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpToolRepository mcpToolRepository;

    private long connectedUserId;
    private long connectionId;
    private long integrationInstanceConfigurationId;
    private long integrationInstanceId;
    private long mcpIntegrationInstanceConfigurationWorkflowId;
    private McpServer mcpServer;
    private McpServer otherMcpServer;

    @BeforeEach
    public void beforeEach() {
        mcpServer = mcpServerRepository.save(
            new McpServer("workflow-server", PlatformType.EMBEDDED, Environment.DEVELOPMENT));
        otherMcpServer = mcpServerRepository.save(
            new McpServer("other-server", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        connectionId = insert(
            """
                INSERT INTO connection (name, component_name, environment, connection_version, parameters,
                    credential_status, type, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES ('gmail', 'gmail', 0, 1, '{}', 0, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """);
        connectedUserId = insert(
            """
                INSERT INTO connected_user (external_id, enabled, environment, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES ('user-1', true, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """);

        long integrationId = insert(
            """
                INSERT INTO integration (name, component_name, allow_multiple_instances, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES ('gmail', 'gmail', false, now(), 'system', now(), 'system', 0)
                RETURNING id
                """);

        integrationInstanceConfigurationId = insert(
            """
                INSERT INTO integration_instance_configuration (integration_id, integration_version, name, enabled,
                    environment, connection_parameters, authorization_type, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, 1, 'gmail', true, 0, '{}', 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationId);

        integrationInstanceId = createIntegrationInstance();

        mcpIntegrationInstanceConfigurationWorkflowId = createMcpIntegrationInstanceConfigurationWorkflow(
            mcpServer.getId());
    }

    @AfterEach
    public void afterEach() {
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_workflow");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_tool");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM integration_instance");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM integration");
        jdbcTemplate.update("DELETE FROM connected_user");
        jdbcTemplate.update("DELETE FROM connection");

        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    public void testCreateAndFetch() {
        McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow =
            mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
                integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId, true);

        assertThat(
            mcpIntegrationInstanceWorkflowService.fetchMcpIntegrationInstanceWorkflow(
                integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId))
                    .hasValueSatisfying(fetched -> {
                        assertThat(fetched.getId()).isEqualTo(mcpIntegrationInstanceWorkflow.getId());
                        assertThat(fetched.isEnabled()).isTrue();
                    });
        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(McpIntegrationInstanceWorkflow::getMcpIntegrationInstanceConfigurationWorkflowId)
            .containsExactly(mcpIntegrationInstanceConfigurationWorkflowId);
    }

    @Test
    public void testUpdateEnabled() {
        McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow =
            mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
                integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId, true);

        mcpIntegrationInstanceWorkflowService.updateEnabled(mcpIntegrationInstanceWorkflow.getId(), false);

        assertThat(
            mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflow(
                mcpIntegrationInstanceWorkflow.getId())
                .isEnabled())
                    .isFalse();
    }

    @Test
    public void testDeletingTheMcpConfigurationWorkflowDeletesTheRows() {
        mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
            integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId, true);

        mcpIntegrationInstanceConfigurationWorkflowService.delete(mcpIntegrationInstanceConfigurationWorkflowId);

        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .isEmpty();
    }

    @Test
    public void testDeletingTheIntegrationInstanceDeletesTheRows() {
        mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
            integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId, true);

        jdbcTemplate.update("DELETE FROM integration_instance WHERE id = ?", integrationInstanceId);

        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .isEmpty();
    }

    @Test
    public void testGetIntegrationInstanceIdsByMcpServerId() {
        long otherIntegrationInstanceId = createIntegrationInstance();
        long otherMcpIntegrationInstanceConfigurationWorkflowId = createMcpIntegrationInstanceConfigurationWorkflow(
            otherMcpServer.getId());

        mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
            integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId, true);
        mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
            otherIntegrationInstanceId, otherMcpIntegrationInstanceConfigurationWorkflowId, false);

        assertThat(mcpIntegrationInstanceWorkflowService.getIntegrationInstanceIdsByMcpServerId(mcpServer.getId()))
            .containsExactly(integrationInstanceId);
        assertThat(
            mcpIntegrationInstanceWorkflowService.getIntegrationInstanceIdsByMcpServerId(otherMcpServer.getId()))
                .containsExactly(otherIntegrationInstanceId);
    }

    @Test
    public void testGetIntegrationInstanceIdsByMcpServerIdForTools() {
        long otherIntegrationInstanceId = createIntegrationInstance();

        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent("gmail", 1, mcpServer.getId(), null));
        McpComponent otherMcpComponent = mcpComponentRepository.save(
            new McpComponent("gmail", 1, otherMcpServer.getId(), null));

        McpTool mcpTool = mcpToolRepository.save(new McpTool("sendEmail", Map.of(), mcpComponent.getId()));
        McpTool otherMcpTool = mcpToolRepository.save(new McpTool("sendEmail", Map.of(), otherMcpComponent.getId()));

        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, Objects.requireNonNull(mcpTool.getId()), true);
        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            otherIntegrationInstanceId, Objects.requireNonNull(otherMcpTool.getId()), true);

        assertThat(mcpIntegrationInstanceToolService.getIntegrationInstanceIdsByMcpServerId(mcpServer.getId()))
            .containsExactly(integrationInstanceId);
    }

    @Test
    public void testGetLinksByConfigurationAndWorkflowId() {
        long integrationInstanceConfigurationWorkflowId = getIntegrationInstanceConfigurationWorkflowId(
            mcpIntegrationInstanceConfigurationWorkflowId);
        String workflowId = getWorkflowId(integrationInstanceConfigurationWorkflowId);

        long otherServerLinkId = createLink(otherMcpServer.getId(), integrationInstanceConfigurationWorkflowId);

        assertThat(
            mcpIntegrationInstanceConfigurationWorkflowService.getMcpIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfigurationId, workflowId))
                    .extracting(McpIntegrationInstanceConfigurationWorkflow::getId)
                    .containsExactlyInAnyOrder(mcpIntegrationInstanceConfigurationWorkflowId, otherServerLinkId);
    }

    private long createIntegrationInstance() {
        return insert(
            """
                INSERT INTO integration_instance (integration_instance_configuration_id, connected_user_id,
                    connection_id, enabled, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, true, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationInstanceConfigurationId, connectedUserId, connectionId);
    }

    private long createMcpIntegrationInstanceConfigurationWorkflow(long mcpServerId) {
        String workflowId = UUID.randomUUID()
            .toString();

        jdbcTemplate.update(
            """
                INSERT INTO workflow (id, definition, format, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, '{"label":"Workflow","tasks":[]}', 0, now(), 'system', now(), 'system', 0)
                """,
            workflowId);

        long integrationInstanceConfigurationWorkflowId = insert(
            """
                INSERT INTO integration_instance_configuration_workflow (integration_instance_configuration_id,
                    workflow_id, inputs, enabled, created_date, created_by, last_modified_date, last_modified_by,
                    version)
                VALUES (?, ?, '{}', true, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationInstanceConfigurationId, workflowId);

        long mcpIntegrationInstanceConfigurationId = insert(
            """
                INSERT INTO mcp_integration_instance_configuration (mcp_server_id,
                    integration_instance_configuration_id, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            mcpServerId, integrationInstanceConfigurationId);

        return insert(
            """
                INSERT INTO mcp_integration_instance_configuration_workflow (mcp_integration_instance_configuration_id,
                    integration_instance_configuration_workflow_id, parameters, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, '{}', now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);
    }

    private long createLink(long mcpServerId, long integrationInstanceConfigurationWorkflowId) {
        long mcpIntegrationInstanceConfigurationId = insert(
            """
                INSERT INTO mcp_integration_instance_configuration (mcp_server_id,
                    integration_instance_configuration_id, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            mcpServerId, integrationInstanceConfigurationId);

        return insert(
            """
                INSERT INTO mcp_integration_instance_configuration_workflow (mcp_integration_instance_configuration_id,
                    integration_instance_configuration_workflow_id, parameters, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, '{}', now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);
    }

    private long getIntegrationInstanceConfigurationWorkflowId(long mcpIntegrationInstanceConfigurationWorkflowId) {
        return insert(
            "SELECT integration_instance_configuration_workflow_id FROM mcp_integration_instance_configuration_workflow "
                + "WHERE id = ?",
            mcpIntegrationInstanceConfigurationWorkflowId);
    }

    private String getWorkflowId(long integrationInstanceConfigurationWorkflowId) {
        return jdbcTemplate.queryForObject(
            "SELECT workflow_id FROM integration_instance_configuration_workflow WHERE id = ?", String.class,
            integrationInstanceConfigurationWorkflowId);
    }

    private long insert(String sql, Object... arguments) {
        Long id = jdbcTemplate.queryForObject(sql, Long.class, arguments);

        return Objects.requireNonNull(id);
    }
}
