/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class IntegrationInstanceConfigurationServiceIntTest {

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void afterEach() {
        jdbcClient.sql("DELETE FROM mcp_integration_instance_configuration")
            .update();
        jdbcClient.sql("DELETE FROM mcp_server")
            .update();
        jdbcClient.sql("DELETE FROM integration_instance_configuration")
            .update();
        jdbcClient.sql("DELETE FROM integration_version")
            .update();
        jdbcClient.sql("DELETE FROM integration")
            .update();
    }

    @Test
    void testGetIntegrationInstanceConfigurationsIncludesConfigurationsLinkedToAnMcpServer() {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName("integration");

        integration = integrationService.create(integration);

        long integrationId = Objects.requireNonNull(integration.getId());

        IntegrationInstanceConfiguration plainConfiguration = createIntegrationInstanceConfiguration(
            integrationId, "plain");
        IntegrationInstanceConfiguration linkedConfiguration = createIntegrationInstanceConfiguration(
            integrationId, "linked");

        linkToMcpServer(Objects.requireNonNull(linkedConfiguration.getId()));

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(null, null, null);

        assertThat(integrationInstanceConfigurations)
            .extracting(IntegrationInstanceConfiguration::getId)
            .containsExactlyInAnyOrder(plainConfiguration.getId(), linkedConfiguration.getId());
    }

    private IntegrationInstanceConfiguration createIntegrationInstanceConfiguration(long integrationId, String name) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName(name);

        return integrationInstanceConfigurationService.create(integrationInstanceConfiguration);
    }

    private void linkToMcpServer(long integrationInstanceConfigurationId) {
        long mcpServerId = Objects.requireNonNull(
            jdbcClient
                .sql(
                    """
                        INSERT INTO mcp_server
                            (name, type, environment, enabled, secret_key, created_date, created_by,
                             last_modified_date, last_modified_by, version)
                        VALUES ('mcp-server', 1, 2, true, 'secret', now(), 'test', now(), 'test', 0)
                        RETURNING id
                        """)
                .query(Long.class)
                .single(),
            "mcp server id");

        jdbcClient
            .sql(
                """
                    INSERT INTO mcp_integration_instance_configuration
                        (mcp_server_id, integration_instance_configuration_id, created_date, created_by,
                         last_modified_date, last_modified_by, version)
                    VALUES (?, ?, now(), 'test', now(), 'test', 0)
                    """)
            .params(mcpServerId, integrationInstanceConfigurationId)
            .update();
    }
}
