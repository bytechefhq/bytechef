/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.config.McpRowsIntTestFixture;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowServiceImpl;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EmbeddedMcpIntTestConfiguration.class,
        EmbeddedMcpServerBeforeDeleteEventListenerIntTest.ListenerIntTestConfiguration.class
    },
    properties = "bytechef.edition=ee")
class EmbeddedMcpServerBeforeDeleteEventListenerIntTest {

    private static final String WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000d1";

    @Autowired
    private Encryption encryption;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @AfterEach
    void afterEach() {
        new McpRowsIntTestFixture(encryption, jdbcTemplate).deleteAll();

        mcpServerRepository.deleteAll();
    }

    @Test
    void testDeleteMcpServerKeepsIntegrationInstanceConfiguration() {
        McpRowsIntTestFixture fixture = new McpRowsIntTestFixture(encryption, jdbcTemplate);

        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("Google", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        long integrationInstanceConfigurationId = fixture.insertIntegrationInstanceConfiguration(
            fixture.insertIntegration("gmail"), 1);

        long linkId = fixture.insertLink(
            fixture.insertMcpIntegrationInstanceConfiguration(mcpServer.getId(), integrationInstanceConfigurationId),
            fixture.insertConfigurationWorkflow(integrationInstanceConfigurationId, WORKFLOW_ID, true), true);

        long integrationInstanceId = fixture.insertIntegrationInstance(
            integrationInstanceConfigurationId, fixture.insertConnectedUser("user-1"),
            fixture.insertConnection("gmail"));

        fixture.insertInstanceWorkflow(integrationInstanceId, integrationInstanceConfigurationId, WORKFLOW_ID, true);

        mcpServerRepository.deleteById(mcpServer.getId());

        assertThat(count("SELECT count(*) FROM integration_instance_configuration WHERE id = ?",
            integrationInstanceConfigurationId))
                .isEqualTo(1);
        assertThat(
            jdbcTemplate.queryForList(
                "SELECT enabled FROM integration_instance_configuration_workflow WHERE workflow_id = ?",
                Boolean.class, WORKFLOW_ID)).containsExactly(true);
        assertThat(fixture.getInstanceWorkflowEnabled(integrationInstanceId, WORKFLOW_ID)).containsExactly(true);
        assertThat(count("SELECT count(*) FROM mcp_integration_instance_configuration_workflow WHERE id = ?", linkId))
            .isZero();
        assertThat(count("SELECT count(*) FROM mcp_integration_instance_configuration")).isZero();
    }

    private int count(String sql, Object... arguments) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, arguments);

        return count == null ? 0 : count;
    }

    @Configuration
    @EnableCaching
    @Import({
        EmbeddedMcpServerBeforeDeleteEventListener.class, IntegrationInstanceConfigurationServiceImpl.class,
        IntegrationInstanceConfigurationWorkflowServiceImpl.class, IntegrationInstanceWorkflowServiceImpl.class,
        McpIntegrationInstanceConfigurationServiceImpl.class
    })
    static class ListenerIntTestConfiguration {
    }
}
