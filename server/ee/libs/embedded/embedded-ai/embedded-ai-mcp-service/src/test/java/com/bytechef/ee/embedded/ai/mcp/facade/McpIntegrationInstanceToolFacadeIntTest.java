/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationInstanceFacadeImpl;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowServiceImpl;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserServiceImpl;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import com.bytechef.platform.mcp.service.McpComponentServiceImpl;
import com.bytechef.platform.mcp.service.McpServerServiceImpl;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EmbeddedMcpIntTestConfiguration.class,
        McpIntegrationInstanceToolFacadeIntTest.FacadeIntTestConfiguration.class
    },
    properties = "bytechef.edition=ee")
class McpIntegrationInstanceToolFacadeIntTest {

    private static final int INTEGRATION_VERSION = 6;
    private static final String OWN_EXTERNAL_USER_ID = "user-1";
    private static final String FOREIGN_EXTERNAL_USER_ID = "user-2";
    private static final String WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000d1";
    private static final String WORKFLOW_UUID = "5eed0000-0000-0000-0000-0000000000e1";

    @MockitoBean
    private ComponentDefinitionFacade componentDefinitionFacade;

    @MockitoSpyBean
    private ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade;

    @Autowired
    private Encryption encryption;

    @MockitoBean
    private IntegrationInstanceFacade integrationInstanceFacade;

    @MockitoSpyBean
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpIntegrationInstanceToolFacade mcpIntegrationInstanceToolFacade;

    @MockitoSpyBean
    private McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpToolRepository mcpToolRepository;

    private long foreignIntegrationInstanceId;
    private long integrationInstanceConfigurationWorkflowId;
    private long integrationInstanceId;
    private long mcpToolId;
    private long ownConnectedUserId;

    @BeforeEach
    void beforeEach() {
        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("Google", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent("gmail", 1, mcpServer.getId(), null));

        McpTool mcpTool = mcpToolRepository.save(new McpTool("searchEmail", Map.of(), mcpComponent.getId()));

        mcpToolId = Objects.requireNonNull(mcpTool.getId());

        long connectionId = insert(
            """
                INSERT INTO connection (name, component_name, environment, connection_version, parameters,
                    credential_status, type, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES ('gmail', 'gmail', 0, 1, '{}', 0, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """);

        ownConnectedUserId = insertConnectedUser(OWN_EXTERNAL_USER_ID);

        long foreignConnectedUserId = insertConnectedUser(FOREIGN_EXTERNAL_USER_ID);

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
                VALUES (?, ?, 'gmail', true, 0, ?, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationId, INTEGRATION_VERSION, encryption.encrypt("{}"));

        integrationInstanceId = insertIntegrationInstance(
            integrationInstanceConfigurationId, ownConnectedUserId, connectionId);
        foreignIntegrationInstanceId = insertIntegrationInstance(
            integrationInstanceConfigurationId, foreignConnectedUserId, connectionId);

        jdbcTemplate.update(
            """
                INSERT INTO workflow (id, definition, format, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, '{"label":"Send Email","triggers":[],"tasks":[]}', 0, now(), 'system', now(), 'system', 0)
                """,
            WORKFLOW_ID);

        integrationWorkflowRepository.save(
            new IntegrationWorkflow(integrationId, INTEGRATION_VERSION, WORKFLOW_ID, UUID.fromString(WORKFLOW_UUID)));

        integrationInstanceConfigurationWorkflowId = insert(
            """
                INSERT INTO integration_instance_configuration_workflow (integration_instance_configuration_id,
                    workflow_id, inputs, enabled, created_date, created_by, last_modified_date, last_modified_by,
                    version)
                VALUES (?, ?, '{}', true, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationInstanceConfigurationId, WORKFLOW_ID);

        insertIntegrationInstanceWorkflow(integrationInstanceId);
        insertIntegrationInstanceWorkflow(foreignIntegrationInstanceId);

        long mcpIntegrationInstanceConfigurationId = insert(
            """
                INSERT INTO mcp_integration_instance_configuration (mcp_server_id,
                    integration_instance_configuration_id, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            mcpServer.getId(), integrationInstanceConfigurationId);

        jdbcTemplate.update(
            """
                INSERT INTO mcp_integration_instance_configuration_workflow (mcp_integration_instance_configuration_id,
                    integration_instance_configuration_workflow_id, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, now(), 'system', now(), 'system', 0)
                """,
            mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);

        SecurityContextHolder.getContext()
            .setAuthentication(
                new EmbeddedApiKeyAuthenticationToken(
                    Environment.DEVELOPMENT.ordinal(), ownConnectedUserId,
                    new User(OWN_EXTERNAL_USER_ID, "", List.of()), false));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

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

        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    void testEnableMcpIntegrationInstanceToolDeniesForeignInstance() {
        assertThatThrownBy(() -> enableMcpIntegrationInstanceTool(foreignIntegrationInstanceId, mcpToolId, true))
            .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        verifyNoInteractions(mcpIntegrationInstanceToolService);

        assertThat(countMcpIntegrationInstanceTools()).isZero();
    }

    @Test
    void testEnableMcpIntegrationInstanceToolChecksOwnershipOfOwnInstance() throws Throwable {
        enableMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId, true);

        verify(connectedUserIntegrationInstanceFacade).validateCurrentPrincipalIntegrationInstanceOwnership(
            integrationInstanceId);
        verify(mcpIntegrationInstanceToolService).createMcpIntegrationInstanceTool(
            integrationInstanceId, mcpToolId, true);

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(McpIntegrationInstanceTool::getMcpToolId, McpIntegrationInstanceTool::isEnabled)
            .containsExactly(tuple(mcpToolId, true));
    }

    @Test
    void testEnableMcpIntegrationInstanceToolRejectsGloballyDisabledTool() {
        McpTool mcpTool = mcpToolRepository.findById(mcpToolId)
            .orElseThrow();

        mcpTool.setEnabled(false);

        mcpToolRepository.save(mcpTool);

        assertThatThrownBy(() -> enableMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId, true))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(countMcpIntegrationInstanceTools()).isZero();
    }

    @Test
    void testEnableMcpIntegrationInstanceToolRejectsToolOfAnotherIntegration() {
        McpServer foreignMcpServer = mcpServerRepository.save(
            new McpServer("Slack", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        McpComponent foreignMcpComponent = mcpComponentRepository.save(
            new McpComponent("slack", 1, foreignMcpServer.getId(), null));

        McpTool foreignMcpTool = mcpToolRepository.save(
            new McpTool("sendMessage", Map.of(), foreignMcpComponent.getId()));

        long foreignMcpToolId = Objects.requireNonNull(foreignMcpTool.getId());

        assertThatThrownBy(() -> enableMcpIntegrationInstanceTool(integrationInstanceId, foreignMcpToolId, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to integration instance");

        verify(mcpIntegrationInstanceToolService, never()).createMcpIntegrationInstanceTool(
            integrationInstanceId, foreignMcpToolId, true);

        assertThat(countMcpIntegrationInstanceTools()).isZero();
    }

    @Test
    void testEnableMcpIntegrationInstanceToolRejectsToolOfANonEmbeddedMcpServer() {
        McpServer automationMcpServer = mcpServerRepository.save(
            new McpServer("Automation", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        McpComponent automationMcpComponent = mcpComponentRepository.save(
            new McpComponent("gmail", 1, automationMcpServer.getId(), null));

        McpTool automationMcpTool = mcpToolRepository.save(
            new McpTool("searchEmail", Map.of(), automationMcpComponent.getId()));

        long automationMcpToolId = Objects.requireNonNull(automationMcpTool.getId());

        assertThatThrownBy(() -> enableMcpIntegrationInstanceTool(integrationInstanceId, automationMcpToolId, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to integration instance");

        assertThat(countMcpIntegrationInstanceTools()).isZero();
    }

    private void enableMcpIntegrationInstanceTool(long integrationInstanceId, long mcpToolId, boolean enable)
        throws Throwable {

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId, enable);

            return null;
        });
    }

    private int countMcpIntegrationInstanceTools() {
        Integer count =
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mcp_integration_instance_tool", Integer.class);

        return Objects.requireNonNull(count);
    }

    private long insert(String sql, Object... arguments) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Long.class, arguments));
    }

    private long insertConnectedUser(String externalUserId) {
        return insert(
            """
                INSERT INTO connected_user (external_id, enabled, environment, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, true, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            externalUserId);
    }

    private long insertIntegrationInstance(
        long integrationInstanceConfigurationId, long connectedUserId, long connectionId) {

        return insert(
            """
                INSERT INTO integration_instance (integration_instance_configuration_id, connected_user_id,
                    connection_id, enabled, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, true, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationInstanceConfigurationId, connectedUserId, connectionId);
    }

    private void insertIntegrationInstanceWorkflow(long integrationInstanceId) {
        jdbcTemplate.update(
            """
                INSERT INTO integration_instance_workflow (integration_instance_id,
                    integration_instance_configuration_workflow_id, inputs, enabled, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, '{}', false, now(), 'system', now(), 'system', 1)
                """,
            integrationInstanceId, integrationInstanceConfigurationWorkflowId);
    }

    @Configuration
    @EnableCaching
    @EnableMethodSecurity
    @Import({
        ConnectedUserIntegrationInstanceFacadeImpl.class, ConnectedUserServiceImpl.class,
        IntegrationInstanceConfigurationServiceImpl.class, IntegrationInstanceServiceImpl.class,
        IntegrationInstanceWorkflowServiceImpl.class, IntegrationServiceImpl.class,
        IntegrationWorkflowServiceImpl.class, McpComponentServiceImpl.class,
        McpIntegrationInstanceConfigurationWorkflowServiceImpl.class, McpIntegrationInstanceToolFacadeImpl.class,
        McpIntegrationInstanceWorkflowFacadeImpl.class, McpServerServiceImpl.class
    })
    static class FacadeIntTestConfiguration {

        @Bean
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }
}
