/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import com.bytechef.platform.mcp.service.McpToolService;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Deletes an MCP tool that a connected user's integration instance has enabled, which used to fail with
 * {@code fk_mcp_integration_instance_tool_mcp_tool}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EmbeddedMcpIntTestConfiguration.class, properties = "bytechef.edition=ee")
public class McpToolBeforeDeleteEventListenerIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpToolRepository mcpToolRepository;

    @Autowired
    private McpToolService mcpToolService;

    private long integrationInstanceId;
    private McpComponent mcpComponent;

    @BeforeEach
    public void beforeEach() {
        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("test-server", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        mcpComponent = mcpComponentRepository.save(new McpComponent("gmail", 1, mcpServer.getId(), null));

        long connectionId = insert(
            """
                INSERT INTO connection (name, component_name, environment, connection_version, parameters,
                    credential_status, type, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES ('gmail', 'gmail', 0, 1, '{}', 0, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """);
        long connectedUserId = insert(
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
        long integrationInstanceConfigurationId = insert(
            """
                INSERT INTO integration_instance_configuration (integration_id, integration_version, name, enabled,
                    environment, connection_parameters, authorization_type, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, 1, 'gmail', true, 0, '{}', 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationId);

        integrationInstanceId = insert(
            """
                INSERT INTO integration_instance (integration_instance_configuration_id, connected_user_id,
                    connection_id, enabled, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, true, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationInstanceConfigurationId, connectedUserId, connectionId);
    }

    @AfterEach
    public void afterEach() {
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_tool");
        jdbcTemplate.update("DELETE FROM integration_instance");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM integration");
        jdbcTemplate.update("DELETE FROM connected_user");
        jdbcTemplate.update("DELETE FROM connection");

        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    public void testDeleteMcpToolEnabledForIntegrationInstance() {
        McpTool mcpTool = mcpToolRepository.save(
            new McpTool("sendEmail", Map.of("to", "user@example.com"), mcpComponent.getId()));

        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, Objects.requireNonNull(mcpTool.getId()), true);

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .hasSize(1);

        mcpToolService.delete(mcpTool);

        assertThat(mcpToolRepository.findById(mcpTool.getId())).isEmpty();
        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .isEmpty();
    }

    @Test
    public void testDeleteMcpToolKeepsOtherToolsEnabledForIntegrationInstance() {
        McpTool deletedMcpTool = mcpToolRepository.save(new McpTool("sendEmail", Map.of(), mcpComponent.getId()));
        McpTool keptMcpTool = mcpToolRepository.save(new McpTool("searchEmail", Map.of(), mcpComponent.getId()));

        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, Objects.requireNonNull(deletedMcpTool.getId()), true);
        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, Objects.requireNonNull(keptMcpTool.getId()), false);

        mcpToolService.delete(deletedMcpTool);

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .singleElement()
            .satisfies(mcpIntegrationInstanceTool -> assertThat(mcpIntegrationInstanceTool.getMcpToolId())
                .isEqualTo(keptMcpTool.getId()));
    }

    @Test
    public void testDeleteMcpToolWithoutIntegrationInstanceTools() {
        McpTool mcpTool = mcpToolRepository.save(new McpTool("getEmail", Map.of(), mcpComponent.getId()));

        mcpToolService.delete(mcpTool);

        assertThat(mcpToolRepository.findById(mcpTool.getId())).isEmpty();
    }

    private long insert(String sql, Object... arguments) {
        Long id = jdbcTemplate.queryForObject(sql, Long.class, arguments);

        return Objects.requireNonNull(id);
    }
}
