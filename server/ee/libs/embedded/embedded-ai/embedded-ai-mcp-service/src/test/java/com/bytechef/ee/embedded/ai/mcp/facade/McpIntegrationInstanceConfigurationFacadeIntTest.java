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
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowServiceImpl;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.WorkflowConfigurationDTO;
import com.bytechef.platform.configuration.dto.WorkflowConfigurationDTO.WorkflowConnectionDTO;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.connection.service.ConnectionServiceImpl;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import java.util.List;
import java.util.Map;
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
        "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true"
    })
class McpIntegrationInstanceConfigurationFacadeIntTest {

    private static final String WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000b1";

    @MockitoBean
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpIntegrationInstanceConfigurationFacade mcpIntegrationInstanceConfigurationFacade;

    @Autowired
    private McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;

    @Autowired
    private McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @MockitoBean
    private PermissionService permissionService;

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

        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("Google", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        mcpServerId = mcpServer.getId();

        Integration integration = new Integration();

        integration.setComponentName("gmail");
        integration.setName("Gmail");

        integration = integrationService.create(integration);

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

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenReturn(
                List.of(
                    new ComponentConnection("gmail", 1, "gmail_1", "gmail", true),
                    new ComponentConnection("slack", 1, "slack_1", "slack", true)));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration_workflow_connection");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration");
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
                List.of(slackWorkflowConfiguration(connectionId, Map.of("subject", "Hello"))));

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
                    WORKFLOW_ID, List.of(new WorkflowConnectionDTO(connectionId, "gmail", "gmail_1")), Map.of()))))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("cannot be configured");

        assertNothingWasStored();
    }

    @Test
    void testCreateRejectsAConnectionFromAnotherEnvironment() {
        long connectionId = createConnection("slack", Environment.PRODUCTION);

        assertThatThrownBy(() -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
            mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID),
            List.of(slackWorkflowConfiguration(connectionId, Map.of()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Connection " + connectionId + " cannot be used");

        assertNothingWasStored();
    }

    @Test
    void testCreateRejectsAConfigurationWithoutARequiredNonIntegrationConnection() {
        assertThatThrownBy(() -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
            mcpServerId, integrationInstanceConfigurationId, List.of(WORKFLOW_ID),
            List.of(new WorkflowConfigurationDTO(WORKFLOW_ID, List.of(), Map.of()))))
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
            List.of(slackWorkflowConfiguration(connectionId, Map.of("subject", "Hi"))));

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
    void testUpdateVersionOfAnUnknownMcpIntegrationInstanceConfigurationRecordsAFailure() {
        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationFacade.updateMcpIntegrationInstanceConfigurationVersion(
                Long.MAX_VALUE, 2, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);

        assertThat(
            findSingleEventData(
                McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_VERSION_UPDATED))
                    .containsEntry("result", "ERROR")
                    .containsEntry("mcpIntegrationInstanceConfigurationId", String.valueOf(Long.MAX_VALUE))
                    .containsEntry("integrationVersion", "2");
    }

    private void assertNothingWasStored() {
        assertThat(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfigurationId)).isEmpty();
        assertThat(mcpIntegrationInstanceConfigurationService.getMcpIntegrationInstanceConfigurations()).isEmpty();
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
        IntegrationServiceImpl.class, IntegrationWorkflowServiceImpl.class,
        McpIntegrationInstanceConfigurationFacadeImpl.class, McpIntegrationInstanceConfigurationServiceImpl.class,
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
