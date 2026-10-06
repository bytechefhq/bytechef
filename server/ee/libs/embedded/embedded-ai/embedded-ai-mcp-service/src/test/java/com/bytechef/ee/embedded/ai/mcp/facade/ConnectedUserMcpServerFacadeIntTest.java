/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerToolDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerWorkflowDTO;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl;
import com.bytechef.encryption.Encryption;
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
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EmbeddedMcpIntTestConfiguration.class, ConnectedUserMcpServerFacadeIntTest.FacadeIntTestConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true"
    })
public class ConnectedUserMcpServerFacadeIntTest {

    private static final String ARCHIVE_EMAIL_WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000b1";
    private static final String HIDDEN_WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000c1";
    private static final Instant LAST_EXECUTION_DATE = Instant.parse("2026-10-05T10:00:00Z");
    private static final String SEND_EMAIL_WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000a1";

    @Autowired
    private ConnectedUserMcpServerFacade connectedUserMcpServerFacade;

    @Autowired
    private Encryption encryption;

    @MockitoBean
    private IntegrationInstanceFacade integrationInstanceFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JobService jobService;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpToolRepository mcpToolRepository;

    @MockitoBean
    private PrincipalJobService principalJobService;

    private long connectedUserId;
    private long integrationInstanceId;
    private McpServer toolsMcpServer;
    private McpServer workflowsMcpServer;

    @BeforeEach
    public void beforeEach() {
        toolsMcpServer = mcpServerRepository.save(
            new McpServer("Google", PlatformType.EMBEDDED, Environment.DEVELOPMENT));
        workflowsMcpServer = mcpServerRepository.save(
            new McpServer("Workflows only", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        long connectionId = insert(
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
        long integrationInstanceConfigurationId = insert(
            """
                INSERT INTO integration_instance_configuration (integration_id, integration_version, name, enabled,
                    environment, connection_parameters, authorization_type, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, 6, 'gmail', true, 0, ?, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationId, encryption.encrypt("{}"));

        integrationInstanceId = insert(
            """
                INSERT INTO integration_instance (integration_instance_configuration_id, connected_user_id,
                    connection_id, enabled, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, true, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationInstanceConfigurationId, connectedUserId, connectionId);

        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent("gmail", 1, toolsMcpServer.getId(), null));

        McpTool searchEmailTool = mcpToolRepository.save(new McpTool("searchEmail", Map.of(), mcpComponent.getId()));
        McpTool getEmailTool = mcpToolRepository.save(new McpTool("getEmail", Map.of(), mcpComponent.getId()));

        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, Objects.requireNonNull(searchEmailTool.getId()), true);
        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, Objects.requireNonNull(getEmailTool.getId()), false);

        long mcpIntegrationInstanceConfigurationId = insert(
            """
                INSERT INTO mcp_integration_instance_configuration (mcp_server_id,
                    integration_instance_configuration_id, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            workflowsMcpServer.getId(), integrationInstanceConfigurationId);

        insertMcpWorkflow(
            SEND_EMAIL_WORKFLOW_ID, "Send Email", "Sends an email", null, true, true,
            integrationInstanceConfigurationId, mcpIntegrationInstanceConfigurationId);
        insertMcpWorkflow(
            ARCHIVE_EMAIL_WORKFLOW_ID, "Archive Email", null,
            "{\"toolName\":\"archiveEmail\",\"toolDescription\":\"Archives the email\"}", true, false,
            integrationInstanceConfigurationId, mcpIntegrationInstanceConfigurationId);
        insertMcpWorkflow(
            HIDDEN_WORKFLOW_ID, "Hidden", null, null, false, true, integrationInstanceConfigurationId,
            mcpIntegrationInstanceConfigurationId);

        Job job = new Job();

        job.setEndDate(LAST_EXECUTION_DATE);

        when(principalJobService.fetchLastWorkflowJobId(
            integrationInstanceId, List.of(SEND_EMAIL_WORKFLOW_ID), PlatformType.EMBEDDED))
                .thenReturn(Optional.of(77L));
        when(jobService.getJob(77L)).thenReturn(job);
    }

    @AfterEach
    public void afterEach() {
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_tool");
        jdbcTemplate.update("DELETE FROM integration_instance_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM integration");
        jdbcTemplate.update("DELETE FROM connected_user");
        jdbcTemplate.update("DELETE FROM connection");
        jdbcTemplate.update("DELETE FROM workflow");

        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    public void testGetConnectedUserMcpServers() {
        List<ConnectedUserMcpServerDTO> connectedUserMcpServers =
            connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId);

        assertThat(connectedUserMcpServers)
            .extracting(ConnectedUserMcpServerDTO::name)
            .containsExactly("Google", "Workflows only");

        ConnectedUserMcpServerDTO toolsConnectedUserMcpServer = connectedUserMcpServers.getFirst();

        assertThat(toolsConnectedUserMcpServer.enabled()).isTrue();
        assertThat(toolsConnectedUserMcpServer.workflows()).isEmpty();
        assertThat(toolsConnectedUserMcpServer.tools())
            .extracting(ConnectedUserMcpServerToolDTO::name, ConnectedUserMcpServerToolDTO::enabled)
            .containsExactly(tuple("getEmail", false), tuple("searchEmail", true));

        ConnectedUserMcpServerDTO workflowsConnectedUserMcpServer = connectedUserMcpServers.get(1);

        assertThat(workflowsConnectedUserMcpServer.enabled()).isTrue();
        assertThat(workflowsConnectedUserMcpServer.tools()).isEmpty();
        assertThat(workflowsConnectedUserMcpServer.workflows())
            .containsExactly(
                new ConnectedUserMcpServerWorkflowDTO(
                    integrationInstanceId, "gmail", 6, ARCHIVE_EMAIL_WORKFLOW_ID, "archiveEmail",
                    "Archives the email", false, null),
                new ConnectedUserMcpServerWorkflowDTO(
                    integrationInstanceId, "gmail", 6, SEND_EMAIL_WORKFLOW_ID, "Send Email", "Sends an email", true,
                    LAST_EXECUTION_DATE));
    }

    @Test
    public void testEnableConnectedUserMcpServerFlipsOnlyWorkflowsInOtherState() {
        connectedUserMcpServerFacade.enableConnectedUserMcpServer(connectedUserId, workflowsMcpServer.getId(), true);

        verify(integrationInstanceFacade).enableIntegrationInstanceWorkflow(
            integrationInstanceId, ARCHIVE_EMAIL_WORKFLOW_ID, true);
        verify(integrationInstanceFacade, never()).enableIntegrationInstanceWorkflow(
            anyLong(), eq(SEND_EMAIL_WORKFLOW_ID), eq(true));
        verify(integrationInstanceFacade, never()).enableIntegrationInstanceWorkflow(
            anyLong(), eq(HIDDEN_WORKFLOW_ID), eq(true));
    }

    @Test
    public void testEnableConnectedUserMcpServerFlipsOnlyItsOwnTools() {
        connectedUserMcpServerFacade.enableConnectedUserMcpServer(connectedUserId, toolsMcpServer.getId(), true);

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .allSatisfy(mcpIntegrationInstanceTool -> assertThat(mcpIntegrationInstanceTool.isEnabled()).isTrue());

        verify(integrationInstanceFacade, never()).enableIntegrationInstanceWorkflow(
            anyLong(), anyString(), eq(true));
    }

    private void insertMcpWorkflow(
        String workflowId, String label, String description, String toolParameters, boolean configurationEnabled,
        boolean userEnabled, long integrationInstanceConfigurationId, long mcpIntegrationInstanceConfigurationId) {

        String definition = description == null
            ? "{\"label\":\"%s\",\"triggers\":[],\"tasks\":[]}".formatted(label)
            : "{\"label\":\"%s\",\"description\":\"%s\",\"triggers\":[],\"tasks\":[]}".formatted(label, description);

        jdbcTemplate.update(
            """
                INSERT INTO workflow (id, definition, format, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, 0, now(), 'system', now(), 'system', 0)
                """,
            workflowId, definition);

        long integrationInstanceConfigurationWorkflowId = insert(
            """
                INSERT INTO integration_instance_configuration_workflow (integration_instance_configuration_id,
                    workflow_id, inputs, enabled, created_date, created_by, last_modified_date, last_modified_by,
                    version)
                VALUES (?, ?, '{}', ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationInstanceConfigurationId, workflowId, configurationEnabled);

        jdbcTemplate.update(
            """
                INSERT INTO integration_instance_workflow (integration_instance_id,
                    integration_instance_configuration_workflow_id, inputs, enabled, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, '{}', ?, now(), 'system', now(), 'system', 0)
                """,
            integrationInstanceId, integrationInstanceConfigurationWorkflowId, userEnabled);

        jdbcTemplate.update(
            """
                INSERT INTO mcp_integration_instance_configuration_workflow (mcp_integration_instance_configuration_id,
                    integration_instance_configuration_workflow_id, parameters, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, now(), 'system', now(), 'system', 0)
                """,
            mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId, toolParameters);
    }

    private long insert(String sql, Object... arguments) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Long.class, arguments));
    }

    @Configuration
    @EnableCaching
    @Import({
        ConnectedUserMcpServerFacadeImpl.class, IntegrationInstanceConfigurationServiceImpl.class,
        IntegrationInstanceConfigurationWorkflowServiceImpl.class, IntegrationInstanceServiceImpl.class,
        IntegrationInstanceWorkflowServiceImpl.class, IntegrationServiceImpl.class, McpComponentServiceImpl.class,
        McpIntegrationInstanceConfigurationServiceImpl.class,
        McpIntegrationInstanceConfigurationWorkflowServiceImpl.class, McpServerServiceImpl.class,
        WorkflowServiceImpl.class
    })
    static class FacadeIntTestConfiguration {
    }
}
