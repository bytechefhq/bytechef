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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectServiceImpl;
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.audit.ConnectedUserMcpServerAuditEvents;
import com.bytechef.ee.embedded.ai.mcp.audit.ConnectedUserMcpServerAuditMapper;
import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpAuditIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.config.McpRowsIntTestFixture;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerAutomationDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerToolDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerWorkflowDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ProvisionableConnectedUserMcpServerDTO;
import com.bytechef.ee.embedded.ai.mcp.exception.ConnectedUserMcpServerErrorType;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.dto.CallableAutomationDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserMcpAutomationResolver;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowServiceImpl;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserServiceImpl;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.encryption.Encryption;
import com.bytechef.exception.ConfigurationException;
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
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
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
        "bytechef.edition=ee", "bytechef.audit.enabled=true", "bytechef.workflow.repository.jdbc.enabled=true"
    })
public class ConnectedUserMcpServerFacadeIntTest {

    private static final String ARCHIVE_AUTOMATION_UUID = "5eed0000-0000-0000-0000-0000000000d2";
    private static final String ARCHIVE_EMAIL_WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000b1";
    private static final String HIDDEN_WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000c1";
    private static final Instant LAST_EXECUTION_DATE = Instant.parse("2026-10-05T10:00:00Z");
    private static final long PROJECT_DEPLOYMENT_ID = 4242L;
    private static final String SEND_AUTOMATION_UUID = "5eed0000-0000-0000-0000-0000000000d1";
    private static final String SEND_EMAIL_WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000a1";
    private static final String UNASSIGNED_AUTOMATION_UUID = "5eed0000-0000-0000-0000-0000000000d3";

    @Autowired
    private ConnectedUserMcpServerFacade connectedUserMcpServerFacade;

    @Autowired
    private Encryption encryption;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ConnectedUserMcpAutomationResolver connectedUserMcpAutomationResolver;

    @MockitoBean
    private JobService jobService;

    @Autowired
    private McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService;

    @Autowired
    private McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService;

    @Autowired
    private McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @Autowired
    private McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpToolRepository mcpToolRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private ProjectService projectService;

    @MockitoBean
    private PrincipalJobService principalJobService;

    private long archiveEmailMcpWorkflowId;
    private McpRowsIntTestFixture fixture;
    private long connectedUserId;
    private long connectionId;
    private long integrationInstanceConfigurationId;
    private long integrationInstanceId;
    private long sendEmailMcpWorkflowId;
    private McpServer toolsMcpServer;
    private McpServer workflowsMcpServer;

    @BeforeEach
    public void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        fixture = new McpRowsIntTestFixture(encryption, jdbcTemplate);

        toolsMcpServer = mcpServerRepository.save(
            new McpServer("Google", PlatformType.EMBEDDED, Environment.DEVELOPMENT));
        workflowsMcpServer = mcpServerRepository.save(
            new McpServer("Workflows only", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

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

        sendEmailMcpWorkflowId = insertMcpWorkflow(
            SEND_EMAIL_WORKFLOW_ID, "Send Email", "Sends an email", null, true, true,
            integrationInstanceConfigurationId, mcpIntegrationInstanceConfigurationId);
        archiveEmailMcpWorkflowId = insertMcpWorkflow(
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
        jdbcTemplate.update("DELETE FROM project_workflow");

        fixture.deleteAll();

        jdbcTemplate.update("DELETE FROM mcp_integration_instance_workflow");
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
                    archiveEmailMcpWorkflowId, integrationInstanceId, "gmail", 6, ARCHIVE_EMAIL_WORKFLOW_ID,
                    "archiveEmail", "Archives the email", false, null),
                new ConnectedUserMcpServerWorkflowDTO(
                    sendEmailMcpWorkflowId, integrationInstanceId, "gmail", 6, SEND_EMAIL_WORKFLOW_ID, "SEND_EMAIL",
                    "Sends an email", true, LAST_EXECUTION_DATE));
    }

    @Test
    public void testGetConnectedUserMcpServersOmitsGloballyDisabledTools() {
        McpTool getEmailTool = mcpToolRepository.findAll()
            .stream()
            .filter(mcpTool -> "getEmail".equals(mcpTool.getName()))
            .findFirst()
            .orElseThrow();

        getEmailTool.setEnabled(false);

        mcpToolRepository.save(getEmailTool);

        List<ConnectedUserMcpServerDTO> connectedUserMcpServers =
            connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId);

        ConnectedUserMcpServerDTO toolsConnectedUserMcpServer = connectedUserMcpServers.getFirst();

        assertThat(toolsConnectedUserMcpServer.tools())
            .extracting(ConnectedUserMcpServerToolDTO::name)
            .containsExactly("searchEmail");
    }

    @Test
    public void testSwitchingOffAServerKeepsTheInstanceWorkflowsEnabled() {
        connectedUserMcpServerFacade.enableConnectedUserMcpServer(connectedUserId, workflowsMcpServer.getId(), false);

        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .allSatisfy(mcpIntegrationInstanceWorkflow -> assertThat(mcpIntegrationInstanceWorkflow.isEnabled())
                .isFalse());
        assertThat(getInstanceWorkflowEnabled(SEND_EMAIL_WORKFLOW_ID)).containsExactly(true);

        connectedUserMcpServerFacade.enableConnectedUserMcpServer(connectedUserId, workflowsMcpServer.getId(), true);

        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .allSatisfy(mcpIntegrationInstanceWorkflow -> assertThat(mcpIntegrationInstanceWorkflow.isEnabled())
                .isTrue());
        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(McpIntegrationInstanceTool::isEnabled)
            .containsExactlyInAnyOrder(true, false);
    }

    @Test
    public void testLinkedConfigurationWithoutRowsListsNothing() {
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_workflow");

        assertThat(connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId))
            .extracting(ConnectedUserMcpServerDTO::name)
            .containsExactly("Google");
    }

    @Test
    public void testDisabledLinkIsNotListed() {
        jdbcTemplate.update(
            "UPDATE mcp_integration_instance_configuration_workflow SET enabled = false WHERE id = ?",
            getLinkId(sendEmailMcpWorkflowId));

        ConnectedUserMcpServerDTO workflowsConnectedUserMcpServer =
            connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId)
                .get(1);

        assertThat(workflowsConnectedUserMcpServer.workflows())
            .extracting(ConnectedUserMcpServerWorkflowDTO::workflowId)
            .containsExactly(ARCHIVE_EMAIL_WORKFLOW_ID);
    }

    @Test
    public void testDeleteRemovesAServerThatExposesWorkflows() {
        long otherConnectedUserId = insert(
            """
                INSERT INTO connected_user (external_id, enabled, environment, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES ('user-2', true, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """);
        long otherIntegrationInstanceId = insert(
            """
                INSERT INTO integration_instance (integration_instance_configuration_id, connected_user_id,
                    connection_id, enabled, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, true, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationInstanceConfigurationId, otherConnectedUserId, connectionId);

        mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
            otherIntegrationInstanceId, getLinkId(sendEmailMcpWorkflowId), true);

        assertThat(
            connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUserId, workflowsMcpServer.getId()))
                .isTrue();

        assertThat(connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId))
            .extracting(ConnectedUserMcpServerDTO::name)
            .containsExactly("Google");
        assertThat(getInstanceWorkflowEnabled(SEND_EMAIL_WORKFLOW_ID)).containsExactly(true);
        assertThat(
            mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(otherIntegrationInstanceId))
                .hasSize(1);
        assertThat(connectedUserMcpServerFacade.getProvisionableMcpServers(connectedUserId))
            .extracting(
                ProvisionableConnectedUserMcpServerDTO::name, ProvisionableConnectedUserMcpServerDTO::provisioned)
            .containsExactly(tuple("Google", true), tuple("Workflows only", false));
    }

    @Test
    public void testEnableMcpWorkflowFlipsOnlyTheRow() {
        connectedUserMcpServerFacade.enableMcpWorkflow(sendEmailMcpWorkflowId, false);

        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflow(sendEmailMcpWorkflowId)
            .isEnabled()).isFalse();
        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflow(archiveEmailMcpWorkflowId)
            .isEnabled()).isFalse();
        assertThat(getInstanceWorkflowEnabled(SEND_EMAIL_WORKFLOW_ID)).containsExactly(true);
        assertThat(findSingleEventData(ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_WORKFLOW_ENABLED_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpIntegrationInstanceWorkflowId", String.valueOf(sendEmailMcpWorkflowId))
            .containsEntry("enabled", "false");
    }

    @Test
    public void testGetProvisionableMcpServersSkipsDisabledLinks() {
        jdbcTemplate.update("UPDATE mcp_integration_instance_configuration_workflow SET enabled = false");

        assertThat(connectedUserMcpServerFacade.getProvisionableMcpServers(connectedUserId))
            .extracting(
                ProvisionableConnectedUserMcpServerDTO::name,
                ProvisionableConnectedUserMcpServerDTO::coveredComponentNames)
            .containsExactly(tuple("Google", List.of("gmail")), tuple("Workflows only", List.of()));
    }

    @Test
    public void testEnableConnectedUserMcpServerFlipsOnlyItsOwnTools() {
        connectedUserMcpServerFacade.enableConnectedUserMcpServer(connectedUserId, toolsMcpServer.getId(), true);

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .allSatisfy(mcpIntegrationInstanceTool -> assertThat(mcpIntegrationInstanceTool.isEnabled()).isTrue());
        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflow(archiveEmailMcpWorkflowId)
            .isEnabled()).isFalse();
    }

    @Test
    public void testGetProvisionableMcpServersReportsCoverageFromComponentsAndConfigurations() {
        List<ProvisionableConnectedUserMcpServerDTO> provisionableMcpServers =
            connectedUserMcpServerFacade.getProvisionableMcpServers(connectedUserId);

        assertThat(provisionableMcpServers)
            .extracting(
                ProvisionableConnectedUserMcpServerDTO::name,
                ProvisionableConnectedUserMcpServerDTO::coveredComponentNames,
                ProvisionableConnectedUserMcpServerDTO::missingComponentNames)
            .containsExactly(
                tuple("Google", List.of("gmail"), List.of()), tuple("Workflows only", List.of("gmail"), List.of()));
    }

    @Test
    public void testGetProvisionableMcpServersListsMissingComponentsForUncoveredServer() {
        McpServer uncoveredMcpServer = mcpServerRepository.save(
            new McpServer("Chat", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        saveMcpComponentWithTool("slack", uncoveredMcpServer);
        saveMcpComponentWithTool("gmail", uncoveredMcpServer);

        ProvisionableConnectedUserMcpServerDTO provisionableMcpServer =
            connectedUserMcpServerFacade.getProvisionableMcpServers(connectedUserId)
                .stream()
                .filter(candidate -> candidate.id() == uncoveredMcpServer.getId())
                .findFirst()
                .orElseThrow();

        assertThat(provisionableMcpServer.provisioned()).isFalse();
        assertThat(provisionableMcpServer.coveredComponentNames()).containsExactly("gmail");
        assertThat(provisionableMcpServer.missingComponentNames()).containsExactly("slack");
    }

    @Test
    public void testGetProvisionableMcpServersSkipsDisabledServersAndOtherEnvironments() {
        mcpServerRepository.save(new McpServer("Disabled", PlatformType.EMBEDDED, Environment.DEVELOPMENT, false));
        mcpServerRepository.save(new McpServer("Production", PlatformType.EMBEDDED, Environment.PRODUCTION));

        assertThat(connectedUserMcpServerFacade.getProvisionableMcpServers(connectedUserId))
            .extracting(ProvisionableConnectedUserMcpServerDTO::name)
            .containsExactly("Google", "Workflows only");
    }

    @Test
    public void testGetProvisionableMcpServersSkipsComponentsWithoutEnabledTools() {
        for (McpTool mcpTool : mcpToolRepository.findAll()) {
            mcpTool.setEnabled(false);

            mcpToolRepository.save(mcpTool);
        }

        assertThat(connectedUserMcpServerFacade.getProvisionableMcpServers(connectedUserId))
            .extracting(
                ProvisionableConnectedUserMcpServerDTO::name,
                ProvisionableConnectedUserMcpServerDTO::coveredComponentNames,
                ProvisionableConnectedUserMcpServerDTO::missingComponentNames)
            .containsExactly(
                tuple("Google", List.of(), List.of()), tuple("Workflows only", List.of("gmail"), List.of()));

        assertThatThrownBy(
            () -> connectedUserMcpServerFacade.provisionMcpServer(connectedUserId, toolsMcpServer.getId()))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    configurationException -> assertThat(configurationException.getErrorKey())
                        .isEqualTo(ConnectedUserMcpServerErrorType.NO_COVERED_INTEGRATION.getErrorKey()));
    }

    @Test
    public void testGetProvisionableMcpServersSkipsConfigurationsWithoutEnabledWorkflows() {
        jdbcTemplate.update("UPDATE integration_instance_configuration_workflow SET enabled = false");

        assertThat(connectedUserMcpServerFacade.getProvisionableMcpServers(connectedUserId))
            .extracting(
                ProvisionableConnectedUserMcpServerDTO::name,
                ProvisionableConnectedUserMcpServerDTO::coveredComponentNames,
                ProvisionableConnectedUserMcpServerDTO::missingComponentNames)
            .containsExactly(
                tuple("Google", List.of("gmail"), List.of()), tuple("Workflows only", List.of(), List.of()));

        assertThatThrownBy(
            () -> connectedUserMcpServerFacade.provisionMcpServer(connectedUserId, workflowsMcpServer.getId()))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    configurationException -> assertThat(configurationException.getErrorKey())
                        .isEqualTo(ConnectedUserMcpServerErrorType.NO_COVERED_INTEGRATION.getErrorKey()));
    }

    @Test
    public void testGetProvisionableMcpServersMarksProvisionedServers() {
        assertThat(connectedUserMcpServerFacade.getProvisionableMcpServers(connectedUserId))
            .extracting(ProvisionableConnectedUserMcpServerDTO::provisioned)
            .containsExactly(true, true);

        connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUserId, toolsMcpServer.getId());

        assertThat(connectedUserMcpServerFacade.getProvisionableMcpServers(connectedUserId))
            .extracting(ProvisionableConnectedUserMcpServerDTO::name,
                ProvisionableConnectedUserMcpServerDTO::provisioned)
            .containsExactly(tuple("Google", false), tuple("Workflows only", true));
    }

    @Test
    public void testProvisionMcpServerCreatesToolRowsOnlyForMatchingInstances() {
        connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUserId, toolsMcpServer.getId());

        long otherIntegrationId = insert(
            """
                INSERT INTO integration (name, component_name, allow_multiple_instances, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES ('slack', 'slack', false, now(), 'system', now(), 'system', 0)
                RETURNING id
                """);
        long otherConfigurationId = insert(
            """
                INSERT INTO integration_instance_configuration (integration_id, integration_version, name, enabled,
                    environment, connection_parameters, authorization_type, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, 1, 'slack', true, 0, ?, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            otherIntegrationId, encryption.encrypt("{}"));
        long otherConnectionId = insert(
            """
                INSERT INTO connection (name, component_name, environment, connection_version, parameters,
                    credential_status, type, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES ('slack', 'slack', 0, 1, '{}', 0, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """);
        long otherInstanceId = insert(
            """
                INSERT INTO integration_instance (integration_instance_configuration_id, connected_user_id,
                    connection_id, enabled, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, true, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            otherConfigurationId, connectedUserId, otherConnectionId);

        connectedUserMcpServerFacade.provisionMcpServer(connectedUserId, toolsMcpServer.getId());

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .hasSize(2)
            .allSatisfy(mcpIntegrationInstanceTool -> assertThat(mcpIntegrationInstanceTool.isEnabled()).isTrue());
        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(otherInstanceId)).isEmpty();
    }

    @Test
    public void testProvisionMcpServerReEnablesExistingRows() {
        connectedUserMcpServerFacade.enableConnectedUserMcpServer(connectedUserId, toolsMcpServer.getId(), false);

        connectedUserMcpServerFacade.provisionMcpServer(connectedUserId, toolsMcpServer.getId());

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .hasSize(2)
            .allSatisfy(mcpIntegrationInstanceTool -> assertThat(mcpIntegrationInstanceTool.isEnabled()).isTrue());
    }

    @Test
    public void testProvisionMcpServerSkipsGloballyDisabledTools() {
        connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUserId, toolsMcpServer.getId());

        McpTool getEmailTool = mcpToolRepository.findAll()
            .stream()
            .filter(mcpTool -> "getEmail".equals(mcpTool.getName()))
            .findFirst()
            .orElseThrow();

        getEmailTool.setEnabled(false);

        mcpToolRepository.save(getEmailTool);

        connectedUserMcpServerFacade.provisionMcpServer(connectedUserId, toolsMcpServer.getId());

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(mcpIntegrationInstanceTool -> mcpIntegrationInstanceTool.getMcpToolId())
            .containsExactly(Objects.requireNonNull(
                mcpToolRepository.findAll()
                    .stream()
                    .filter(mcpTool -> "searchEmail".equals(mcpTool.getName()))
                    .findFirst()
                    .orElseThrow()
                    .getId()));
    }

    @Test
    public void testProvisionMcpServerEnablesRowsAndCreatesMissingInstanceWorkflowsDisabled() {
        connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUserId, workflowsMcpServer.getId());

        jdbcTemplate.update(
            """
                DELETE FROM integration_instance_workflow
                WHERE integration_instance_configuration_workflow_id IN (
                    SELECT id FROM integration_instance_configuration_workflow WHERE workflow_id = ?)
                """,
            ARCHIVE_EMAIL_WORKFLOW_ID);

        connectedUserMcpServerFacade.provisionMcpServer(connectedUserId, workflowsMcpServer.getId());

        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .hasSize(2)
            .allSatisfy(mcpIntegrationInstanceWorkflow -> assertThat(mcpIntegrationInstanceWorkflow.isEnabled())
                .isTrue());
        assertThat(getInstanceWorkflowEnabled(ARCHIVE_EMAIL_WORKFLOW_ID)).containsExactly(false);
        assertThat(getInstanceWorkflowEnabled(SEND_EMAIL_WORKFLOW_ID)).containsExactly(true);
        assertThat(getInstanceWorkflowEnabled(HIDDEN_WORKFLOW_ID)).containsExactly(true);
    }

    @Test
    public void testProvisionMcpServerRejectsServerWithoutCoveredIntegration() {
        McpServer uncoveredMcpServer = mcpServerRepository.save(
            new McpServer("Chat", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        saveMcpComponentWithTool("slack", uncoveredMcpServer);

        assertThatThrownBy(
            () -> connectedUserMcpServerFacade.provisionMcpServer(connectedUserId, uncoveredMcpServer.getId()))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    configurationException -> assertThat(configurationException.getErrorKey())
                        .isEqualTo(ConnectedUserMcpServerErrorType.NO_COVERED_INTEGRATION.getErrorKey()));

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .hasSize(2);
    }

    @Test
    public void testProvisionMcpServerRejectsDisabledServersAndOtherEnvironments() {
        McpServer disabledMcpServer = mcpServerRepository.save(
            new McpServer("Disabled", PlatformType.EMBEDDED, Environment.DEVELOPMENT, false));
        McpServer productionMcpServer = mcpServerRepository.save(
            new McpServer("Production", PlatformType.EMBEDDED, Environment.PRODUCTION));

        saveMcpComponentWithTool("gmail", disabledMcpServer);
        saveMcpComponentWithTool("gmail", productionMcpServer);

        for (McpServer unprovisionableMcpServer : List.of(disabledMcpServer, productionMcpServer)) {
            assertThatThrownBy(
                () -> connectedUserMcpServerFacade.provisionMcpServer(
                    connectedUserId, unprovisionableMcpServer.getId()))
                        .isInstanceOfSatisfying(
                            ConfigurationException.class,
                            configurationException -> assertThat(configurationException.getErrorKey())
                                .isEqualTo(
                                    ConnectedUserMcpServerErrorType.MCP_SERVER_NOT_PROVISIONABLE.getErrorKey()));
        }

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .hasSize(2);
    }

    @Test
    public void testProvisionMcpServerMakesTheServerAppearForTheUser() {
        connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUserId, toolsMcpServer.getId());

        assertThat(connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId))
            .extracting(ConnectedUserMcpServerDTO::name)
            .containsExactly("Workflows only");

        connectedUserMcpServerFacade.provisionMcpServer(connectedUserId, toolsMcpServer.getId());

        assertThat(connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId))
            .extracting(ConnectedUserMcpServerDTO::name)
            .containsExactly("Google", "Workflows only");
    }

    @Test
    public void testProvisionMcpServerRecordsTheConnectedUserAndTheServer() {
        connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUserId, toolsMcpServer.getId());

        connectedUserMcpServerFacade.provisionMcpServer(connectedUserId, toolsMcpServer.getId());

        assertThat(findSingleEventData(ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_SERVER_DELETED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectedUserId", String.valueOf(connectedUserId))
            .containsEntry("mcpServerId", String.valueOf(toolsMcpServer.getId()));
        assertThat(findSingleEventData(ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_SERVER_PROVISIONED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectedUserId", String.valueOf(connectedUserId))
            .containsEntry("mcpServerId", String.valueOf(toolsMcpServer.getId()));
    }

    @Test
    public void testDeleteThatRemovesNothingIsNotRecorded() {
        assertThat(connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUserId, toolsMcpServer.getId()))
            .isTrue();
        assertThat(connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUserId, toolsMcpServer.getId()))
            .isFalse();
        assertThat(connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUserId, Long.MAX_VALUE))
            .isFalse();

        assertThat(findSingleEventData(ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_SERVER_DELETED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpServerId", String.valueOf(toolsMcpServer.getId()));
    }

    @Test
    public void testRejectedProvisionRecordsAFailure() {
        McpServer uncoveredMcpServer = mcpServerRepository.save(
            new McpServer("Chat", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        saveMcpComponentWithTool("slack", uncoveredMcpServer);

        assertThatThrownBy(
            () -> connectedUserMcpServerFacade.provisionMcpServer(connectedUserId, uncoveredMcpServer.getId()))
                .isInstanceOf(ConfigurationException.class);

        assertThat(findSingleEventData(ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_SERVER_PROVISIONED))
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", ConfigurationException.class.getName())
            .containsEntry("mcpServerId", String.valueOf(uncoveredMcpServer.getId()));
    }

    @Test
    public void testEnableConnectedUserMcpServerRecordsTheNewState() {
        connectedUserMcpServerFacade.enableConnectedUserMcpServer(connectedUserId, toolsMcpServer.getId(), false);

        assertThat(findSingleEventData(ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_SERVER_ENABLED_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectedUserId", String.valueOf(connectedUserId))
            .containsEntry("mcpServerId", String.valueOf(toolsMcpServer.getId()))
            .containsEntry("enabled", "false");
    }

    @Test
    public void testEnableMcpToolRecordsTheToolAndTheNewState() {
        List<McpIntegrationInstanceTool> mcpIntegrationInstanceTools =
            mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId);

        McpIntegrationInstanceTool mcpIntegrationInstanceTool = mcpIntegrationInstanceTools.getFirst();

        connectedUserMcpServerFacade.enableMcpTool(mcpIntegrationInstanceTool.getId(), false);

        assertThat(findSingleEventData(ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_TOOL_ENABLED_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpIntegrationInstanceToolId", String.valueOf(mcpIntegrationInstanceTool.getId()))
            .containsEntry("enabled", "false");
    }

    @Test
    public void testEnableOfAnUnknownMcpToolRecordsAFailure() {
        assertThatThrownBy(() -> connectedUserMcpServerFacade.enableMcpTool(Long.MAX_VALUE, true))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(findSingleEventData(ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_TOOL_ENABLED_UPDATED))
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("mcpIntegrationInstanceToolId", String.valueOf(Long.MAX_VALUE))
            .containsEntry("enabled", "true");
    }

    @Test
    public void testAutomationOnlyServerListed() {
        McpServer automationsMcpServer = saveAutomationsMcpServer();

        Automation automation = createAutomation(automationsMcpServer, SEND_AUTOMATION_UUID, true);

        List<ConnectedUserMcpServerDTO> connectedUserMcpServers =
            connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId);

        assertThat(connectedUserMcpServers)
            .extracting(ConnectedUserMcpServerDTO::name)
            .containsExactly("Automations only", "Google", "Workflows only");

        ConnectedUserMcpServerDTO automationsConnectedUserMcpServer = connectedUserMcpServers.getFirst();

        assertThat(automationsConnectedUserMcpServer.enabled()).isTrue();
        assertThat(automationsConnectedUserMcpServer.tools()).isEmpty();
        assertThat(automationsConnectedUserMcpServer.workflows()).isEmpty();
        assertThat(automationsConnectedUserMcpServer.automations())
            .containsExactly(
                new ConnectedUserMcpServerAutomationDTO(
                    automation.rowId(), automation.referenceId(), "template", SEND_AUTOMATION_UUID, "SEND_MAIL",
                    "Sends mail", true, true, null));
    }

    @Test
    public void testAutomationRowAvailableFalseWhenConfigurationWorkflowOff() {
        McpServer automationsMcpServer = saveAutomationsMcpServer();

        Automation automation = createAutomation(automationsMcpServer, SEND_AUTOMATION_UUID, false);

        List<ConnectedUserMcpServerAutomationDTO> automations =
            connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId)
                .getFirst()
                .automations();

        assertThat(automations)
            .extracting(
                ConnectedUserMcpServerAutomationDTO::id,
                ConnectedUserMcpServerAutomationDTO::connectedUserProjectWorkflowId,
                ConnectedUserMcpServerAutomationDTO::enabled, ConnectedUserMcpServerAutomationDTO::available)
            .containsExactly(tuple(automation.rowId(), automation.referenceId(), true, false));
    }

    @Test
    public void testAutomationRowAvailableFalseWhenNotToolCallable() {
        McpServer automationsMcpServer = saveAutomationsMcpServer();

        Automation automation = createAutomation(automationsMcpServer, SEND_AUTOMATION_UUID, true);

        when(connectedUserMcpAutomationResolver.fetchCallableAutomation(
            connectedUserId, automation.configurationId(), SEND_AUTOMATION_UUID))
                .thenReturn(
                    Optional.of(
                        new CallableAutomationDTO(
                            automation.referenceId(), PROJECT_DEPLOYMENT_ID, automation.workflowId(), Map.of(), null,
                            null)));

        assertThat(connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId)
            .getFirst()
            .automations())
                .extracting(ConnectedUserMcpServerAutomationDTO::enabled,
                    ConnectedUserMcpServerAutomationDTO::available)
                .containsExactly(tuple(true, false));
    }

    @Test
    public void testAutomationRowUnavailableWhenResolverThrows() {
        McpServer automationsMcpServer = saveAutomationsMcpServer();

        Automation failingAutomation = createAutomation(automationsMcpServer, SEND_AUTOMATION_UUID, true);

        createAutomation(automationsMcpServer, ARCHIVE_AUTOMATION_UUID, true);

        when(connectedUserMcpAutomationResolver.fetchCallableAutomation(
            connectedUserId, failingAutomation.configurationId(), SEND_AUTOMATION_UUID))
                .thenAnswer(invocation -> projectService.getProject(Long.MAX_VALUE));

        List<ConnectedUserMcpServerAutomationDTO> automations =
            connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId)
                .getFirst()
                .automations();

        assertThat(automations)
            .extracting(
                ConnectedUserMcpServerAutomationDTO::workflowUuid, ConnectedUserMcpServerAutomationDTO::enabled,
                ConnectedUserMcpServerAutomationDTO::available)
            .containsExactlyInAnyOrder(
                tuple(SEND_AUTOMATION_UUID, true, false), tuple(ARCHIVE_AUTOMATION_UUID, true, true));
    }

    @Test
    public void testAutomationRowWithoutReferenceIsNotListed() {
        McpServer automationsMcpServer = saveAutomationsMcpServer();

        Automation automation = createAutomation(automationsMcpServer, SEND_AUTOMATION_UUID, true);

        when(connectedUserMcpAutomationResolver.fetchConnectedUserProjectWorkflowId(
            connectedUserId, automation.configurationId(), SEND_AUTOMATION_UUID)).thenReturn(Optional.empty());

        assertThat(connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId)
            .getFirst()
            .automations())
                .isEmpty();
    }

    @Test
    public void testEnableMcpAutomationRejectsOtherUsersRow() {
        McpServer automationsMcpServer = saveAutomationsMcpServer();

        Automation automation = createAutomation(automationsMcpServer, SEND_AUTOMATION_UUID, true);

        long otherConnectedUserId = fixture.insertConnectedUser("user-2");

        assertThatThrownBy(
            () -> connectedUserMcpServerFacade.enableMcpAutomation(otherConnectedUserId, automation.rowId(), false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(mcpConnectedUserProjectWorkflowService.fetch(automation.rowId())
            .orElseThrow()
            .isEnabled()).isTrue();
        assertThat(findSingleEventData(ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_AUTOMATION_ENABLED_UPDATED))
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName());
    }

    @Test
    public void testEnableMcpAutomationLeavesAutomationSwitch() {
        McpServer automationsMcpServer = saveAutomationsMcpServer();

        Automation automation = createAutomation(automationsMcpServer, SEND_AUTOMATION_UUID, true);

        ConnectedUserMcpServerAutomationDTO connectedUserMcpServerAutomation =
            connectedUserMcpServerFacade.enableMcpAutomation(connectedUserId, automation.rowId(), false);

        assertThat(connectedUserMcpServerAutomation.id()).isEqualTo(automation.rowId());
        assertThat(connectedUserMcpServerAutomation.enabled()).isFalse();
        assertThat(mcpConnectedUserProjectWorkflowService.fetch(automation.rowId())
            .orElseThrow()
            .isEnabled()).isFalse();
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT enabled FROM connected_user_project_workflow WHERE id = ?", Boolean.class,
                automation.referenceId())).isTrue();
        assertThat(findSingleEventData(ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_AUTOMATION_ENABLED_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectedUserId", String.valueOf(connectedUserId))
            .containsEntry("mcpConnectedUserProjectWorkflowId", String.valueOf(automation.rowId()))
            .containsEntry("enabled", "false");
    }

    @Test
    public void testEnableServerTogglesAutomationRows() {
        McpServer automationsMcpServer = saveAutomationsMcpServer();

        Automation automation = createAutomation(automationsMcpServer, SEND_AUTOMATION_UUID, true);

        connectedUserMcpServerFacade.enableConnectedUserMcpServer(connectedUserId, automationsMcpServer.getId(), false);

        assertThat(mcpConnectedUserProjectWorkflowService.fetch(automation.rowId())
            .orElseThrow()
            .isEnabled()).isFalse();
        assertThat(connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId)
            .getFirst()
            .enabled()).isFalse();

        connectedUserMcpServerFacade.enableConnectedUserMcpServer(connectedUserId, automationsMcpServer.getId(), true);

        assertThat(mcpConnectedUserProjectWorkflowService.fetch(automation.rowId())
            .orElseThrow()
            .isEnabled()).isTrue();
    }

    @Test
    public void testDeleteServerRemovesAutomationRows() {
        McpServer automationsMcpServer = saveAutomationsMcpServer();

        createAutomation(automationsMcpServer, SEND_AUTOMATION_UUID, true);

        assertThat(
            connectedUserMcpServerFacade.deleteConnectedUserMcpServer(connectedUserId, automationsMcpServer.getId()))
                .isTrue();

        assertThat(mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(
            connectedUserId)).isEmpty();
        assertThat(connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId))
            .extracting(ConnectedUserMcpServerDTO::name)
            .containsExactly("Google", "Workflows only");
    }

    @Test
    public void testProvisionableCoverage() {
        McpServer automationsMcpServer = saveAutomationsMcpServer();

        createAutomation(automationsMcpServer, SEND_AUTOMATION_UUID, true);

        AutomationTemplate assignedTemplate =
            createTemplate(automationsMcpServer, ARCHIVE_AUTOMATION_UUID, "Archive mail");

        fixture.insertAssignment(connectedUserId, assignedTemplate.projectId(), assignedTemplate.configurationId());

        createTemplate(automationsMcpServer, UNASSIGNED_AUTOMATION_UUID, "Unassigned");

        ProvisionableConnectedUserMcpServerDTO provisionableMcpServer =
            connectedUserMcpServerFacade.getProvisionableMcpServers(connectedUserId)
                .stream()
                .filter(candidate -> candidate.id() == automationsMcpServer.getId())
                .findFirst()
                .orElseThrow();

        assertThat(provisionableMcpServer.coveredAutomationNames()).containsExactly("SEND_MAIL");
        assertThat(provisionableMcpServer.missingAutomationNames()).containsExactly("ARCHIVE_MAIL");
        assertThat(provisionableMcpServer.coveredComponentNames()).isEmpty();
    }

    @Test
    public void testProvisionAllowedWithOnlyAutomationsCovered() {
        McpServer automationsMcpServer = saveAutomationsMcpServer();

        long automationOnlyConnectedUserId = fixture.insertConnectedUser("user-automation-only");

        AutomationTemplate template = createTemplate(automationsMcpServer, SEND_AUTOMATION_UUID, "Send mail");

        fixture.insertAssignment(automationOnlyConnectedUserId, template.projectId(), template.configurationId());

        long referenceId = fixture.insertReference(
            automationOnlyConnectedUserId, template.projectId(), SEND_AUTOMATION_UUID, false);

        connectedUserMcpServerFacade.provisionMcpServer(automationOnlyConnectedUserId, automationsMcpServer.getId());

        assertThat(
            mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(
                automationOnlyConnectedUserId))
                    .extracting(
                        McpConnectedUserProjectWorkflow::getConnectedUserProjectWorkflowId,
                        McpConnectedUserProjectWorkflow::getMcpAutomationProjectConfigurationWorkflowId,
                        McpConnectedUserProjectWorkflow::isEnabled)
                    .containsExactly(tuple(referenceId, template.linkWorkflowId(), true));
    }

    @Test
    public void testLastExecutionDateAcrossVersions() {
        McpServer automationsMcpServer = saveAutomationsMcpServer();

        Automation automation = createAutomation(automationsMcpServer, SEND_AUTOMATION_UUID, true);

        jdbcTemplate.update(
            "UPDATE connected_user_project_workflow SET project_deployment_id = ? WHERE id = ?", PROJECT_DEPLOYMENT_ID,
            automation.referenceId());

        Instant olderExecutionDate = Instant.parse("2026-09-01T10:00:00Z");

        Job job = new Job();

        job.setEndDate(olderExecutionDate);

        when(principalJobService.fetchLastWorkflowJobId(eq(PROJECT_DEPLOYMENT_ID), anyList(),
            eq(PlatformType.AUTOMATION)))
                .thenAnswer(invocation -> {
                    List<String> workflowIds = invocation.getArgument(1);

                    return workflowIds.containsAll(List.of(automation.workflowId(), automation.workflowId() + "-v2"))
                        ? Optional.of(88L)
                        : Optional.empty();
                });
        when(jobService.getJob(88L)).thenReturn(job);

        List<ConnectedUserMcpServerAutomationDTO> automations =
            connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId)
                .getFirst()
                .automations();

        assertThat(automations)
            .extracting(ConnectedUserMcpServerAutomationDTO::lastExecutionDate)
            .containsExactly(olderExecutionDate);
    }

    private Automation createAutomation(McpServer mcpServer, String workflowUuid, boolean callable) {

        AutomationTemplate template = createTemplate(mcpServer, workflowUuid, "Send mail");

        fixture.insertAssignment(connectedUserId, template.projectId(), template.configurationId());

        long referenceId = fixture.insertReference(connectedUserId, template.projectId(), workflowUuid, false);

        McpConnectedUserProjectWorkflow row = mcpConnectedUserProjectWorkflowService.create(
            referenceId, template.linkWorkflowId(), true);

        long rowId = row.getId();

        when(connectedUserMcpAutomationResolver.fetchConnectedUserProjectWorkflowId(
            connectedUserId, template.configurationId(), workflowUuid)).thenReturn(Optional.of(referenceId));

        if (callable) {
            when(connectedUserMcpAutomationResolver.fetchCallableAutomation(
                connectedUserId, template.configurationId(), workflowUuid))
                    .thenReturn(
                        Optional.of(
                            new CallableAutomationDTO(
                                referenceId, PROJECT_DEPLOYMENT_ID, template.workflowId(), Map.of(), null,
                                "newWorkflowCall_1")));
        }

        return new Automation(template.configurationId(), referenceId, rowId, template.workflowId());
    }

    private AutomationTemplate createTemplate(McpServer mcpServer, String workflowUuid, String label) {
        long projectId = fixture.insertAutomationProject();
        long configurationId = fixture.insertAutomationProjectConfiguration(projectId);

        String workflowId = "wf-" + workflowUuid;

        insertProjectWorkflow(projectId, 1, workflowId, workflowUuid, label);
        insertProjectWorkflow(projectId, 2, workflowId + "-v2", workflowUuid, label);

        McpAutomationProjectConfiguration link = mcpAutomationProjectConfigurationService.create(
            new McpAutomationProjectConfiguration(configurationId, mcpServer.getId()));

        McpAutomationProjectConfigurationWorkflow linkWorkflow =
            mcpAutomationProjectConfigurationWorkflowService.create(link.getId(), workflowUuid);

        mcpAutomationProjectConfigurationWorkflowService.updateParameters(
            linkWorkflow.getId(), Map.of("toolDescription", "Sends mail"));

        return new AutomationTemplate(configurationId, linkWorkflow.getId(), projectId, workflowId);
    }

    private void insertProjectWorkflow(
        long projectId, int projectVersion, String workflowId, String workflowUuid, String label) {

        jdbcTemplate.update(
            """
                INSERT INTO workflow (id, definition, format, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, 0, now(), 'system', now(), 'system', 0)
                """,
            workflowId,
            "{\"label\":\"%s\",\"triggers\":[{\"name\":\"newWorkflowCall_1\",\"type\":\"workflow/v1/newWorkflowCall\",\"parameters\":{}}],\"tasks\":[]}"
                .formatted(label));
        jdbcTemplate.update(
            """
                INSERT INTO project_workflow (project_id, project_version, workflow_id, uuid, created_date,
                    created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, ?, now(), 'system', now(), 'system', 0)
                """,
            projectId, projectVersion, workflowId, UUID.fromString(workflowUuid));
    }

    private McpServer saveAutomationsMcpServer() {
        return mcpServerRepository
            .save(new McpServer("Automations only", PlatformType.EMBEDDED, Environment.DEVELOPMENT));
    }

    private record Automation(long configurationId, long referenceId, long rowId, String workflowId) {
    }

    private record AutomationTemplate(long configurationId, long linkWorkflowId, long projectId, String workflowId) {
    }

    private Map<String, String> findSingleEventData(String eventType) {
        return EmbeddedMcpAuditIntTestConfiguration.findSingleEventData(persistenceAuditEventRepository, eventType);
    }

    private List<Boolean> getInstanceWorkflowEnabled(String workflowId) {
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

    private long getLinkId(long mcpIntegrationInstanceWorkflowId) {
        McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow =
            mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflow(mcpIntegrationInstanceWorkflowId);

        return mcpIntegrationInstanceWorkflow.getMcpIntegrationInstanceConfigurationWorkflowId();
    }

    private long insertMcpWorkflow(
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
                VALUES (?, ?, '{}', true, now(), 'system', now(), 'system', 0)
                """,
            integrationInstanceId, integrationInstanceConfigurationWorkflowId);

        long mcpIntegrationInstanceConfigurationWorkflowId = insert(
            """
                INSERT INTO mcp_integration_instance_configuration_workflow (mcp_integration_instance_configuration_id,
                    integration_instance_configuration_workflow_id, parameters, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId, toolParameters);

        McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow =
            mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
                integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId, userEnabled);

        return mcpIntegrationInstanceWorkflow.getId();
    }

    private void saveMcpComponentWithTool(String componentName, McpServer mcpServer) {
        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent(componentName, 1, mcpServer.getId(), null));

        mcpToolRepository.save(new McpTool(componentName + "Tool", Map.of(), mcpComponent.getId()));
    }

    private long insert(String sql, Object... arguments) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Long.class, arguments));
    }

    @Configuration
    @EnableCaching
    @Import({
        ConnectedUserMcpAutomations.class, ConnectedUserMcpServerAuditMapper.class,
        ConnectedUserMcpServerFacadeImpl.class, ConnectedUserServiceImpl.class,
        IntegrationInstanceConfigurationServiceImpl.class,
        IntegrationInstanceConfigurationWorkflowServiceImpl.class, IntegrationInstanceServiceImpl.class,
        IntegrationInstanceWorkflowServiceImpl.class, IntegrationServiceImpl.class, McpComponentServiceImpl.class,
        McpIntegrationInstanceConfigurationServiceImpl.class,
        McpIntegrationInstanceConfigurationWorkflowServiceImpl.class, McpIntegrationInstanceRowFacadeImpl.class,
        McpServerMembership.class,
        McpServerServiceImpl.class, EmbeddedMcpAuditIntTestConfiguration.class,
        IntegrationWorkflowServiceImpl.class, AutomationProjectConfigurationServiceImpl.class,
        ConnectedUserProjectConfigurationServiceImpl.class, ConnectedUserProjectServiceImpl.class,
        ConnectedUserProjectWorkflowServiceImpl.class, McpAutomationRowFacadeImpl.class, ProjectServiceImpl.class,
        ProjectWorkflowServiceImpl.class
    })
    static class FacadeIntTestConfiguration {

        @Bean
        WorkflowService workflowService(
            CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
            List<WorkflowRepository> workflowRepositories) {

            return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
        }
    }
}
