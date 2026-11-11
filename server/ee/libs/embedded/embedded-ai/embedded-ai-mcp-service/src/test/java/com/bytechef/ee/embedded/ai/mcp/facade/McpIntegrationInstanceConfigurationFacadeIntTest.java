/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.ai.mcp.audit.McpIntegrationInstanceConfigurationAuditEvents;
import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpAuditIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.config.McpRowsIntTestFixture;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowServiceImpl;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.WorkflowConfigurationDTO;
import com.bytechef.platform.configuration.dto.WorkflowConfigurationDTO.WorkflowConnectionDTO;
import com.bytechef.platform.configuration.dto.WorkflowToolParametersDTO;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.connection.service.ConnectionServiceImpl;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.service.McpComponentServiceImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EmbeddedMcpIntTestConfiguration.class,
        McpIntegrationInstanceConfigurationFacadeIntTest.McpIntegrationInstanceConfigurationFacadeIntTestConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.audit.enabled=true", "bytechef.workflow.repository.jdbc.enabled=true"
    })
class McpIntegrationInstanceConfigurationFacadeIntTest {

    private static final String SECOND_WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000b2";
    private static final String WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000b1";

    @MockitoBean
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private Encryption encryption;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpIntegrationInstanceConfigurationFacade mcpIntegrationInstanceConfigurationFacade;

    @Autowired
    private McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;

    @Autowired
    private McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @Autowired
    private McpIntegrationInstanceRows mcpIntegrationInstanceRows;

    @Autowired
    private McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @MockitoBean
    private PermissionService permissionService;

    private McpRowsIntTestFixture fixture;
    private long integrationId;
    private long integrationInstanceConfigurationId;
    private long mcpServerId;

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        when(permissionService.isTenantAdmin()).thenReturn(true);

        persistenceAuditEventRepository.deleteAll();

        fixture = new McpRowsIntTestFixture(encryption, jdbcTemplate);

        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("Google", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        mcpServerId = mcpServer.getId();

        Integration integration = new Integration();

        integration.setComponentName("gmail");
        integration.setName("Gmail");

        integration = integrationService.create(integration);

        integrationId = integration.getId();

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.DEVELOPMENT);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("Gmail configuration");

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        integrationInstanceConfigurationId = integrationInstanceConfiguration.getId();

        jdbcTemplate.update(
            """
                INSERT INTO workflow (id, definition, format, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, 0, now(), 'system', now(), 'system', 0)
                """,
            WORKFLOW_ID,
            """
                {"tasks":[{"name":"gmail_1","type":"gmail/v1/sendEmail"}]}""");
        jdbcTemplate.update(
            """
                INSERT INTO workflow (id, definition, format, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, 0, now(), 'system', now(), 'system', 0)
                """,
            SECOND_WORKFLOW_ID,
            """
                {"tasks":[]}""");

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenReturn(
                List.of(
                    new ComponentConnection("gmail", 1, "gmail_1", "gmail", true),
                    new ComponentConnection("slack", 1, "slack_1", "slack", true)));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        jdbcTemplate.update("DELETE FROM mcp_integration_instance_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance");
        jdbcTemplate.update("DELETE FROM connected_user");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration_workflow_connection");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM integration_workflow");
        jdbcTemplate.update("DELETE FROM integration_version");
        jdbcTemplate.update("DELETE FROM integration");
        jdbcTemplate.update("DELETE FROM connection");
        jdbcTemplate.update("DELETE FROM workflow");

        mcpServerRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateStoresTheConfiguredInputsAndNonIntegrationConnections() {
        long connectionId = createConnection("slack", Environment.DEVELOPMENT);

        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID),
                List.of(slackWorkflowConfiguration(connectionId, Map.of("subject", "Hello"))), List.of());

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            getIntegrationInstanceConfigurationWorkflow();

        assertThat(integrationInstanceConfigurationWorkflow.getConnections())
            .containsExactly(new IntegrationInstanceConfigurationWorkflowConnection(connectionId, "slack", "slack_1"));
        assertThat(integrationInstanceConfigurationWorkflow.getInputs()).isEqualTo(Map.of("subject", "Hello"));

        assertThat(getMcpIntegrationInstanceConfigurationWorkflows(mcpIntegrationInstanceConfiguration.getId()))
            .extracting(McpIntegrationInstanceConfigurationWorkflow::getIntegrationInstanceConfigurationWorkflowId)
            .containsExactly(integrationInstanceConfigurationWorkflow.getId());
    }

    @Test
    void testCreateRejectsAConnectionForTheIntegrationComponent() {
        long connectionId = createConnection("gmail", Environment.DEVELOPMENT);

        assertThatThrownBy(() -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
            mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID),
            List.of(
                new WorkflowConfigurationDTO(
                    WORKFLOW_ID, List.of(new WorkflowConnectionDTO(connectionId, "gmail", "gmail_1")), Map.of())),
            List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be configured");

        assertNothingWasStored();
    }

    @Test
    void testCreateRejectsAConnectionFromAnotherEnvironment() {
        long connectionId = createConnection("slack", Environment.PRODUCTION);

        assertThatThrownBy(() -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
            mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID),
            List.of(slackWorkflowConfiguration(connectionId, Map.of())), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Connection " + connectionId + " cannot be used");

        assertNothingWasStored();
    }

    @Test
    void testCreateRejectsAConfigurationWithoutARequiredNonIntegrationConnection() {
        assertThatThrownBy(() -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
            mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID),
            List.of(new WorkflowConfigurationDTO(WORKFLOW_ID, List.of(), Map.of())), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a slack connection");

        assertNothingWasStored();
    }

    @Test
    void testCreateWithoutAConfigurationKeepsEmptyInputs() {
        mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
            mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID));

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            getIntegrationInstanceConfigurationWorkflow();

        assertThat(integrationInstanceConfigurationWorkflow.getConnections()).isEmpty();
        assertThat(integrationInstanceConfigurationWorkflow.getInputs()).isEmpty();
        assertThat(integrationInstanceConfigurationWorkflow.isEnabled()).isTrue();
    }

    @Test
    void testUpdateAppliesTheConfigurationOfAKeptWorkflow() {
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID));

        long integrationInstanceConfigurationWorkflowId = getIntegrationInstanceConfigurationWorkflow().getId();

        long connectionId = createConnection("slack", Environment.DEVELOPMENT);

        mcpIntegrationInstanceConfigurationFacade.updateMcpIntegrationInstanceConfiguration(
            mcpIntegrationInstanceConfiguration.getId(), List.of(WORKFLOW_ID),
            List.of(slackWorkflowConfiguration(connectionId, Map.of("subject", "Hi"))), List.of());

        IntegrationInstanceConfigurationWorkflow updatedIntegrationInstanceConfigurationWorkflow =
            getIntegrationInstanceConfigurationWorkflow();

        assertThat(updatedIntegrationInstanceConfigurationWorkflow.getId())
            .isEqualTo(integrationInstanceConfigurationWorkflowId);
        assertThat(updatedIntegrationInstanceConfigurationWorkflow.getConnections())
            .containsExactly(new IntegrationInstanceConfigurationWorkflowConnection(connectionId, "slack", "slack_1"));
        assertThat(updatedIntegrationInstanceConfigurationWorkflow.getInputs()).isEqualTo(Map.of("subject", "Hi"));

        assertThat(getMcpIntegrationInstanceConfigurationWorkflows(mcpIntegrationInstanceConfiguration.getId()))
            .extracting(McpIntegrationInstanceConfigurationWorkflow::getIntegrationInstanceConfigurationWorkflowId)
            .containsExactly(integrationInstanceConfigurationWorkflowId);
    }

    @Test
    void testCreateRecordsTheMcpIntegrationInstanceConfiguration() {
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID));

        assertThat(
            findSingleEventData(
                McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_CREATED))
                    .containsEntry("result", "SUCCESS")
                    .containsEntry(
                        "mcpIntegrationInstanceConfigurationId",
                        String.valueOf(mcpIntegrationInstanceConfiguration.getId()))
                    .containsEntry("mcpServerId", String.valueOf(mcpServerId))
                    .containsEntry("integrationInstanceConfigurationId",
                        String.valueOf(integrationInstanceConfigurationId))
                    .containsEntry("workflowIds", "[" + WORKFLOW_ID + "]");
    }

    @Test
    void testUpdateRecordsTheSelectedWorkflows() {
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID));

        mcpIntegrationInstanceConfigurationFacade.updateMcpIntegrationInstanceConfiguration(
            mcpIntegrationInstanceConfiguration.getId(), List.of());

        assertThat(
            findSingleEventData(
                McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_UPDATED))
                    .containsEntry("result", "SUCCESS")
                    .containsEntry(
                        "mcpIntegrationInstanceConfigurationId",
                        String.valueOf(mcpIntegrationInstanceConfiguration.getId()))
                    .containsEntry("mcpServerId", String.valueOf(mcpServerId))
                    .containsEntry("workflowIds", "[]");
    }

    @Test
    void testDeleteRecordsTheMcpIntegrationInstanceConfiguration() {
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID));

        mcpIntegrationInstanceConfigurationFacade.deleteMcpIntegrationInstanceConfiguration(
            mcpIntegrationInstanceConfiguration.getId());

        assertThat(
            findSingleEventData(
                McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_DELETED))
                    .containsEntry("result", "SUCCESS")
                    .containsEntry(
                        "mcpIntegrationInstanceConfigurationId",
                        String.valueOf(mcpIntegrationInstanceConfiguration.getId()));
    }

    @Test
    void testDeleteOfAnUnknownMcpIntegrationInstanceConfigurationRecordsAFailure() {
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationFacade.deleteMcpIntegrationInstanceConfiguration(Long.MAX_VALUE))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(
            findSingleEventData(
                McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_DELETED))
                    .containsEntry("result", "ERROR")
                    .containsEntry("errorClass", IllegalArgumentException.class.getName())
                    .containsEntry("mcpIntegrationInstanceConfigurationId", String.valueOf(Long.MAX_VALUE));
    }

    @Test
    void testUntickWorkflowKeepsTheIntegrationConfigurationWorkflow() {
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID));

        long integrationInstanceId = createIntegrationInstanceWithEnabledWorkflow("user-1");

        mcpIntegrationInstanceConfigurationFacade.updateMcpIntegrationInstanceConfiguration(
            mcpIntegrationInstanceConfiguration.getId(), List.of());

        assertThat(getMcpIntegrationInstanceConfigurationWorkflows(mcpIntegrationInstanceConfiguration.getId()))
            .isEmpty();
        assertThat(getIntegrationInstanceConfigurationWorkflow().isEnabled()).isTrue();
        assertThat(fixture.getInstanceWorkflowEnabled(integrationInstanceId, WORKFLOW_ID)).containsExactly(true);
    }

    @Test
    void testDeleteMcpConfigurationKeepsTheIntegrationConfigurationWorkflows() {
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID));

        long integrationInstanceId = createIntegrationInstanceWithEnabledWorkflow("user-1");

        mcpIntegrationInstanceConfigurationFacade.deleteMcpIntegrationInstanceConfiguration(
            mcpIntegrationInstanceConfiguration.getId());

        assertThat(mcpIntegrationInstanceConfigurationService.getMcpIntegrationInstanceConfigurations()).isEmpty();
        assertThat(getIntegrationInstanceConfigurationWorkflow().isEnabled()).isTrue();
        assertThat(fixture.getInstanceWorkflowEnabled(integrationInstanceId, WORKFLOW_ID)).containsExactly(true);
    }

    @Test
    void testAddedWorkflowReachesConnectedUserWithTheServer() {
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID));

        long connectionId = fixture.insertConnection("gmail");

        long integrationInstanceId = fixture.insertIntegrationInstance(
            integrationInstanceConfigurationId, fixture.insertConnectedUser("user-1"), connectionId);
        long otherIntegrationInstanceId = fixture.insertIntegrationInstance(
            integrationInstanceConfigurationId, fixture.insertConnectedUser("user-2"), connectionId);

        mcpIntegrationInstanceRows.provisionMcpServer(
            getConnectedUserId(integrationInstanceId), mcpServerId);

        mcpIntegrationInstanceConfigurationFacade.updateMcpIntegrationInstanceConfiguration(
            mcpIntegrationInstanceConfiguration.getId(), List.of(WORKFLOW_ID, SECOND_WORKFLOW_ID));

        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(McpIntegrationInstanceWorkflow::isEnabled)
            .containsExactly(true, true);
        assertThat(
            mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(otherIntegrationInstanceId))
                .isEmpty();
    }

    @Test
    void testCreateStoresToolParameters() {
        String firstWorkflowId = "5eed0000-0000-0000-0000-0000000000d1";
        String secondWorkflowId = "5eed0000-0000-0000-0000-0000000000d2";

        insertCallableWorkflow(firstWorkflowId, 1, UUID.randomUUID(), "message");
        insertCallableWorkflow(secondWorkflowId, 1, UUID.randomUUID(), "message");

        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                mcpServerId, integrationInstanceConfigurationId, List.of(firstWorkflowId, secondWorkflowId), List.of(),
                List.of(new WorkflowToolParametersDTO(firstWorkflowId, Map.of("toolName", "first_tool"))));

        Map<String, McpIntegrationInstanceConfigurationWorkflow> mcpWorkflowMap =
            getMcpWorkflowsByWorkflowId(mcpIntegrationInstanceConfiguration.getId());

        assertThat(mcpWorkflowMap.get(firstWorkflowId)
            .getParameters()).isEqualTo(Map.of("toolName", "first_tool"));
        assertThat(mcpWorkflowMap.get(secondWorkflowId)
            .getParameters()).isNullOrEmpty();
    }

    @Test
    void testUpdateStoresToolParametersForNewAndKeptWorkflows() {
        String firstWorkflowId = "5eed0000-0000-0000-0000-0000000000d1";
        String secondWorkflowId = "5eed0000-0000-0000-0000-0000000000d2";

        insertCallableWorkflow(firstWorkflowId, 1, UUID.randomUUID(), "message");
        insertCallableWorkflow(secondWorkflowId, 1, UUID.randomUUID(), "message");

        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                mcpServerId, integrationInstanceConfigurationId, List.of(firstWorkflowId));

        mcpIntegrationInstanceConfigurationFacade.updateMcpIntegrationInstanceConfiguration(
            mcpIntegrationInstanceConfiguration.getId(), List.of(firstWorkflowId, secondWorkflowId), List.of(),
            List.of(
                new WorkflowToolParametersDTO(firstWorkflowId, Map.of("toolName", "kept_tool")),
                new WorkflowToolParametersDTO(secondWorkflowId, Map.of("toolName", "added_tool"))));

        Map<String, McpIntegrationInstanceConfigurationWorkflow> mcpWorkflowMap =
            getMcpWorkflowsByWorkflowId(mcpIntegrationInstanceConfiguration.getId());

        assertThat(mcpWorkflowMap.get(firstWorkflowId)
            .getParameters()).isEqualTo(Map.of("toolName", "kept_tool"));
        assertThat(mcpWorkflowMap.get(secondWorkflowId)
            .getParameters()).isEqualTo(Map.of("toolName", "added_tool"));
    }

    @Test
    void testUpdateLeavesParametersOfWorkflowsWithoutToolParameters() {
        String firstWorkflowId = "5eed0000-0000-0000-0000-0000000000d1";
        String secondWorkflowId = "5eed0000-0000-0000-0000-0000000000d2";

        insertCallableWorkflow(firstWorkflowId, 1, UUID.randomUUID(), "message");
        insertCallableWorkflow(secondWorkflowId, 1, UUID.randomUUID(), "message");

        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                mcpServerId, integrationInstanceConfigurationId, List.of(firstWorkflowId, secondWorkflowId), List.of(),
                List.of(
                    new WorkflowToolParametersDTO(firstWorkflowId, Map.of("toolName", "first_tool")),
                    new WorkflowToolParametersDTO(secondWorkflowId, Map.of("toolName", "second_tool"))));

        mcpIntegrationInstanceConfigurationFacade.updateMcpIntegrationInstanceConfiguration(
            mcpIntegrationInstanceConfiguration.getId(), List.of(firstWorkflowId, secondWorkflowId), List.of(),
            List.of(new WorkflowToolParametersDTO(secondWorkflowId, Map.of("toolName", "renamed_tool"))));

        Map<String, McpIntegrationInstanceConfigurationWorkflow> mcpWorkflowMap =
            getMcpWorkflowsByWorkflowId(mcpIntegrationInstanceConfiguration.getId());

        assertThat(mcpWorkflowMap.get(firstWorkflowId)
            .getParameters()).isEqualTo(Map.of("toolName", "first_tool"));
        assertThat(mcpWorkflowMap.get(secondWorkflowId)
            .getParameters()).isEqualTo(Map.of("toolName", "renamed_tool"));
    }

    private Map<String, McpIntegrationInstanceConfigurationWorkflow> getMcpWorkflowsByWorkflowId(
        long mcpIntegrationInstanceConfigurationId) {

        Map<Long, String> workflowIdMap = new HashMap<>();

        for (IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow : integrationInstanceConfigurationWorkflowService
            .getIntegrationInstanceConfigurationWorkflows(integrationInstanceConfigurationId)) {

            workflowIdMap.put(
                integrationInstanceConfigurationWorkflow.getId(),
                integrationInstanceConfigurationWorkflow.getWorkflowId());
        }

        Map<String, McpIntegrationInstanceConfigurationWorkflow> mcpWorkflowMap = new HashMap<>();

        for (McpIntegrationInstanceConfigurationWorkflow mcpWorkflow : getMcpIntegrationInstanceConfigurationWorkflows(
            mcpIntegrationInstanceConfigurationId)) {

            mcpWorkflowMap.put(
                workflowIdMap.get(mcpWorkflow.getIntegrationInstanceConfigurationWorkflowId()), mcpWorkflow);
        }

        return mcpWorkflowMap;
    }

    private void insertCallableWorkflow(
        String workflowId, int integrationVersion, UUID workflowUuid, String... inputNames) {

        StringBuilder properties = new StringBuilder();

        for (String inputName : inputNames) {
            if (properties.length() > 0) {
                properties.append(',');
            }

            properties.append("\"")
                .append(inputName)
                .append("\":{\"type\":\"string\"}");
        }

        insertCallableWorkflowWithProperties(workflowId, integrationVersion, workflowUuid, properties.toString());
    }

    private void insertCallableWorkflowWithProperties(
        String workflowId, int integrationVersion, UUID workflowUuid, String propertiesJson) {

        String inputSchema = "{\"type\":\"object\",\"properties\":{" + propertiesJson + "}}";

        String definition =
            "{\"triggers\":[{\"name\":\"trigger_1\",\"type\":\"workflow/v1/newWorkflowCall\",\"parameters\":" +
                "{\"inputSchema\":\"" + inputSchema.replace("\"", "\\\"") + "\"}}],\"tasks\":[]}";

        jdbcTemplate.update(
            """
                INSERT INTO workflow (id, definition, format, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, 0, now(), 'system', now(), 'system', 0)
                """,
            workflowId, definition);

        integrationWorkflowRepository.save(
            new IntegrationWorkflow(integrationId, integrationVersion, workflowId, workflowUuid));
    }

    private void assertNothingWasStored() {
        assertThat(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfigurationId)).isEmpty();
        assertThat(mcpIntegrationInstanceConfigurationService.getMcpIntegrationInstanceConfigurations()).isEmpty();
    }

    private long createIntegrationInstanceWithEnabledWorkflow(String externalId) {
        long integrationInstanceId = fixture.insertIntegrationInstance(
            integrationInstanceConfigurationId, fixture.insertConnectedUser(externalId),
            fixture.insertConnection("gmail"));

        fixture.insertInstanceWorkflow(integrationInstanceId, integrationInstanceConfigurationId, WORKFLOW_ID, true);

        return integrationInstanceId;
    }

    private long getConnectedUserId(long integrationInstanceId) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                "SELECT connected_user_id FROM integration_instance WHERE id = ?", Long.class,
                integrationInstanceId));
    }

    private Map<String, String> findSingleEventData(String eventType) {
        return EmbeddedMcpAuditIntTestConfiguration.findSingleEventData(persistenceAuditEventRepository, eventType);
    }

    private long createConnection(String componentName, Environment environment) {
        Connection connection = connectionService.create(
            null, componentName, 1, environment.ordinal(), componentName + " connection", Map.of(),
            PlatformType.EMBEDDED);

        return connection.getId();
    }

    private IntegrationInstanceConfigurationWorkflow getIntegrationInstanceConfigurationWorkflow() {
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfigurationId);

        assertThat(integrationInstanceConfigurationWorkflows).hasSize(1);

        return integrationInstanceConfigurationWorkflows.getFirst();
    }

    private List<McpIntegrationInstanceConfigurationWorkflow> getMcpIntegrationInstanceConfigurationWorkflows(
        long mcpIntegrationInstanceConfigurationId) {

        return mcpIntegrationInstanceConfigurationWorkflowService
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                mcpIntegrationInstanceConfigurationId);
    }

    private static WorkflowConfigurationDTO slackWorkflowConfiguration(long connectionId, Map<String, Object> inputs) {
        return new WorkflowConfigurationDTO(
            WORKFLOW_ID, List.of(new WorkflowConnectionDTO(connectionId, "slack", "slack_1")), inputs);
    }

    @Configuration
    @EnableCaching
    @Import({
        ConnectionServiceImpl.class, IntegrationInstanceConfigurationServiceImpl.class,
        IntegrationInstanceConfigurationWorkflowServiceImpl.class, IntegrationInstanceWorkflowServiceImpl.class,
        IntegrationInstanceServiceImpl.class, IntegrationServiceImpl.class, IntegrationWorkflowServiceImpl.class,
        McpComponentServiceImpl.class, McpIntegrationInstanceConfigurationFacadeImpl.class,
        McpIntegrationInstanceConfigurationServiceImpl.class, McpIntegrationInstanceRowsImpl.class,
        McpIntegrationInstanceConfigurationWorkflowServiceImpl.class, EmbeddedMcpAuditIntTestConfiguration.class
    })
    static class McpIntegrationInstanceConfigurationFacadeIntTestConfiguration {

        @Bean
        WorkflowService workflowService(
            CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
            List<WorkflowRepository> workflowRepositories) {

            return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
        }
    }
}
