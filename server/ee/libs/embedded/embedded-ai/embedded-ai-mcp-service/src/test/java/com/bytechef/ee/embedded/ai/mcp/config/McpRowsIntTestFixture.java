/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.config;

import com.bytechef.encryption.Encryption;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class McpRowsIntTestFixture {

    private final Encryption encryption;
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI2")
    public McpRowsIntTestFixture(Encryption encryption, JdbcTemplate jdbcTemplate) {
        this.encryption = encryption;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_workflow");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_tool");
        jdbcTemplate.update("DELETE FROM integration_instance_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM integration_workflow");
        jdbcTemplate.update("DELETE FROM integration");
        jdbcTemplate.update("DELETE FROM connected_user");
        jdbcTemplate.update("DELETE FROM connection");
        jdbcTemplate.update("DELETE FROM workflow");
    }

    public List<Boolean> getInstanceWorkflowEnabled(long integrationInstanceId, String workflowId) {
        return jdbcTemplate.queryForList(
            """
                SELECT integration_instance_workflow.enabled
                FROM integration_instance_workflow
                JOIN integration_instance_configuration_workflow
                    ON integration_instance_configuration_workflow.id =
                        integration_instance_workflow.integration_instance_configuration_workflow_id
                WHERE integration_instance_workflow.integration_instance_id = ?
                AND integration_instance_configuration_workflow.workflow_id = ?
                """,
            Boolean.class, integrationInstanceId, workflowId);
    }

    public long insertConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId, boolean enabled) {

        jdbcTemplate.update(
            """
                INSERT INTO workflow (id, definition, format, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, '{"label":"Workflow","triggers":[],"tasks":[]}', 0, now(), 'system', now(), 'system', 1)
                ON CONFLICT DO NOTHING
                """,
            workflowId);

        return insert(
            """
                INSERT INTO integration_instance_configuration_workflow (integration_instance_configuration_id,
                    workflow_id, inputs, enabled, created_date, created_by, last_modified_date, last_modified_by,
                    version)
                VALUES (?, ?, '{}', ?, now(), 'system', now(), 'system', 1)
                RETURNING id
                """,
            integrationInstanceConfigurationId, workflowId, enabled);
    }

    public long insertConnectedUser(String externalId) {
        return insert(
            """
                INSERT INTO connected_user (external_id, enabled, environment, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, true, 0, now(), 'system', now(), 'system', 1)
                RETURNING id
                """,
            externalId);
    }

    public long insertConnection(String componentName) {
        return insert(
            """
                INSERT INTO connection (name, component_name, environment, connection_version, parameters,
                    credential_status, type, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, 0, 1, '{}', 0, 0, now(), 'system', now(), 'system', 1)
                RETURNING id
                """,
            componentName, componentName);
    }

    public void insertInstanceWorkflow(
        long integrationInstanceId, long integrationInstanceConfigurationId, String workflowId, boolean enabled) {

        jdbcTemplate.update(
            """
                INSERT INTO integration_instance_workflow (integration_instance_id,
                    integration_instance_configuration_workflow_id, inputs, enabled, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                SELECT ?, id, '{}', ?, now(), 'system', now(), 'system', 1
                FROM integration_instance_configuration_workflow
                WHERE integration_instance_configuration_id = ? AND workflow_id = ?
                """,
            integrationInstanceId, enabled, integrationInstanceConfigurationId, workflowId);
    }

    public long insertIntegration(String componentName) {
        return insert(
            """
                INSERT INTO integration (name, component_name, allow_multiple_instances, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, false, now(), 'system', now(), 'system', 1)
                RETURNING id
                """,
            componentName, componentName);
    }

    public long insertIntegrationInstance(
        long integrationInstanceConfigurationId, long connectedUserId, long connectionId) {

        return insert(
            """
                INSERT INTO integration_instance (integration_instance_configuration_id, connected_user_id,
                    connection_id, enabled, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, true, now(), 'system', now(), 'system', 1)
                RETURNING id
                """,
            integrationInstanceConfigurationId, connectedUserId, connectionId);
    }

    public long insertIntegrationInstanceConfiguration(long integrationId, int integrationVersion) {
        return insert(
            """
                INSERT INTO integration_instance_configuration (integration_id, integration_version, name, enabled,
                    environment, connection_parameters, authorization_type, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, 'configuration', true, 0, ?, 0, now(), 'system', now(), 'system', 1)
                RETURNING id
                """,
            integrationId, integrationVersion, encryption.encrypt("{}"));
    }

    public void insertIntegrationWorkflow(
        long integrationId, int integrationVersion, String workflowId, String workflowUuid) {

        jdbcTemplate.update(
            """
                INSERT INTO integration_workflow (integration_id, integration_version, workflow_id, uuid,
                    created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, ?, now(), 'system', now(), 'system', 1)
                """,
            integrationId, integrationVersion, workflowId, UUID.fromString(workflowUuid));
    }

    public long insertLink(
        long mcpIntegrationInstanceConfigurationId, long integrationInstanceConfigurationWorkflowId,
        boolean enabled) {

        return insert(
            """
                INSERT INTO mcp_integration_instance_configuration_workflow (mcp_integration_instance_configuration_id,
                    integration_instance_configuration_workflow_id, enabled, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, now(), 'system', now(), 'system', 1)
                RETURNING id
                """,
            mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId, enabled);
    }

    public long insertMcpIntegrationInstanceConfiguration(long mcpServerId, long integrationInstanceConfigurationId) {
        return insert(
            """
                INSERT INTO mcp_integration_instance_configuration (mcp_server_id,
                    integration_instance_configuration_id, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, now(), 'system', now(), 'system', 1)
                RETURNING id
                """,
            mcpServerId, integrationInstanceConfigurationId);
    }

    private long insert(String sql, Object... arguments) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Long.class, arguments));
    }
}
