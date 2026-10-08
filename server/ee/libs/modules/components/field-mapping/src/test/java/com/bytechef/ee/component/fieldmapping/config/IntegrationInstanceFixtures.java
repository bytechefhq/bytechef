/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.fieldmapping.config;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.encryption.Encryption;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationInstanceFixtures {

    private final Encryption encryption;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI2")
    public IntegrationInstanceFixtures(
        Encryption encryption, IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        JdbcTemplate jdbcTemplate) {

        this.encryption = encryption;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createIntegrationInstanceConfiguration() {
        long integrationId = insert(
            """
                INSERT INTO integration (name, component_name, allow_multiple_instances, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES ('crm', 'crm', false, now(), 'system', now(), 'system', 0)
                RETURNING id
                """);

        return insert(
            """
                INSERT INTO integration_instance_configuration (integration_id, integration_version, name, enabled,
                    environment, connection_parameters, authorization_type, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, 1, 'crm', true, 0, ?, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationId, encryption.encrypt("{}"));
    }

    public long createIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId) {

        return insert(
            """
                INSERT INTO integration_instance_configuration_workflow (integration_instance_configuration_id,
                    workflow_id, inputs, enabled, created_date, created_by, last_modified_date, last_modified_by,
                    version)
                VALUES (?, ?, '{}', true, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationInstanceConfigurationId, workflowId);
    }

    public long createIntegrationInstance(long integrationInstanceConfigurationId, String connectedUserExternalId) {
        long connectionId = insert(
            """
                INSERT INTO connection (name, component_name, environment, connection_version, parameters,
                    credential_status, type, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES ('crm', 'crm', 0, 1, '{}', 0, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """);

        long connectedUserId = insert(
            """
                INSERT INTO connected_user (external_id, enabled, environment, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, true, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            connectedUserExternalId);

        return insert(
            """
                INSERT INTO integration_instance (integration_instance_configuration_id, connected_user_id,
                    connection_id, enabled, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, true, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationInstanceConfigurationId, connectedUserId, connectionId);
    }

    public void saveIntegrationInstanceWorkflowInputs(
        long integrationInstanceId, long integrationInstanceConfigurationWorkflowId, Map<String, ?> inputs) {

        IntegrationInstanceWorkflow integrationInstanceWorkflow = integrationInstanceWorkflowService
            .createIntegrationInstanceWorkflow(integrationInstanceId, integrationInstanceConfigurationWorkflowId);

        integrationInstanceWorkflow.setInputs(inputs);

        integrationInstanceWorkflowService.update(integrationInstanceWorkflow);
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM integration_instance_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM integration");
        jdbcTemplate.update("DELETE FROM connected_user");
        jdbcTemplate.update("DELETE FROM connection");
        jdbcTemplate.update("DELETE FROM workflow_test_configuration");
    }

    private long insert(String sql, Object... arguments) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Long.class, arguments));
    }
}
