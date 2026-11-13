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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
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
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowServiceImpl;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.service.McpComponentServiceImpl;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringJUnitConfig(McpIntegrationInstanceConfigurationWorkflowFacadeIntTest.MethodSecurityConfiguration.class)
class McpIntegrationInstanceConfigurationWorkflowFacadeIntTest {

    @Autowired
    private McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade;

    @Autowired
    private PermissionService permissionService;

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        reset(permissionService);
    }

    @Test
    void testEveryMethodRequiresTenantAdmin() {
        List<Method> methods =
            Arrays.stream(McpIntegrationInstanceConfigurationWorkflowFacade.class.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .filter(method -> !method.isDefault())
                .toList();

        assertThat(methods).isNotEmpty();

        for (Method method : methods) {
            assertThatThrownBy(() -> invoke(method))
                .as("%s.%s", McpIntegrationInstanceConfigurationWorkflowFacade.class.getSimpleName(), method.getName())
                .isInstanceOf(AccessDeniedException.class);
        }

        verify(permissionService, times(methods.size())).isTenantAdmin();
    }

    private void invoke(Method method) throws Throwable {
        Object[] arguments = Arrays.stream(method.getParameterTypes())
            .map(McpIntegrationInstanceConfigurationWorkflowFacadeIntTest::defaultValue)
            .toArray();

        try {
            method.invoke(mcpIntegrationInstanceConfigurationWorkflowFacade, arguments);
        } catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getCause();
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }

        if (type == int.class) {
            return 0;
        }

        if (type == long.class) {
            return 0L;
        }

        return null;
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(
        classes = {
            EmbeddedMcpIntTestConfiguration.class, ToolEligibleWorkflowsIntTest.ToolEligibleWorkflowsConfiguration.class
        },
        properties = {
            "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true"
        })
    @MockitoBean(types = {
        McpIntegrationInstanceConfigurationService.class, McpIntegrationInstanceRows.class, PermissionService.class
    })
    class ToolEligibleWorkflowsIntTest {

        private static final int INTEGRATION_VERSION = 2;

        @Autowired
        private Encryption encryption;

        @MockitoBean
        private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

        @MockitoBean
        private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

        @Autowired
        private IntegrationWorkflowRepository integrationWorkflowRepository;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade;

        @MockitoBean
        private McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

        private long integrationId;
        private long integrationInstanceConfigurationId;

        @BeforeEach
        void beforeEach() {
            integrationId = insert(
                """
                    INSERT INTO integration (name, component_name, allow_multiple_instances, created_date, created_by,
                        last_modified_date, last_modified_by, version)
                    VALUES ('gmail', 'gmail', false, now(), 'system', now(), 'system', 0)
                    RETURNING id
                    """);

            integrationInstanceConfigurationId = insert(
                """
                    INSERT INTO integration_instance_configuration (integration_id, integration_version, name,
                        enabled, environment, connection_parameters, authorization_type, created_date, created_by,
                        last_modified_date, last_modified_by, version)
                    VALUES (?, ?, 'gmail', true, 0, ?, 0, now(), 'system', now(), 'system', 0)
                    RETURNING id
                    """,
                integrationId, INTEGRATION_VERSION, encryption.encrypt("{}"));

            insertIntegrationWorkflow(
                "5eed0000-0000-0000-0000-0000000000a1",
                """
                    {"triggers":[{"name":"trigger_1","type":"workflow/v1/newWorkflowCall"}],"tasks":[]}""");
            insertIntegrationWorkflow(
                "5eed0000-0000-0000-0000-0000000000a2",
                """
                    {"triggers":[{"name":"trigger_1","type":"webhook/v1/newRequest"}],"tasks":[]}""");
            insertIntegrationWorkflow(
                "5eed0000-0000-0000-0000-0000000000a3",
                """
                    {"tasks":[]}""");
            insertIntegrationWorkflow(
                "5eed0000-0000-0000-0000-0000000000a4",
                """
                    {"triggers":[{"name":"trigger_1","type":"webhook/v1/newRequest"},\
                    {"name":"trigger_2","type":"workflow/v1/newWorkflowCall"}],"tasks":[]}""");
        }

        @AfterEach
        void afterEach() {
            jdbcTemplate.update("DELETE FROM integration_instance_configuration");
            jdbcTemplate.update("DELETE FROM integration_workflow");
            jdbcTemplate.update("DELETE FROM integration");
            jdbcTemplate.update("DELETE FROM workflow");
        }

        @Test
        void testGetToolEligibleIntegrationVersionWorkflowsKeepsOnlyWorkflowCallTriggeredWorkflows() {
            List<IntegrationWorkflowDTO> integrationWorkflowDTOs =
                mcpIntegrationInstanceConfigurationWorkflowFacade.getToolEligibleIntegrationVersionWorkflows(
                    integrationId, INTEGRATION_VERSION);

            assertThat(integrationWorkflowDTOs)
                .extracting(IntegrationWorkflowDTO::getId)
                .containsExactlyInAnyOrder(
                    "5eed0000-0000-0000-0000-0000000000a1", "5eed0000-0000-0000-0000-0000000000a4");
        }

        @Test
        void testGetToolEligibleIntegrationInstanceConfigurationWorkflowsUsesTheConfiguredIntegrationVersion() {
            insertIntegrationWorkflow(
                INTEGRATION_VERSION + 1, "5eed0000-0000-0000-0000-0000000000a5",
                """
                    {"triggers":[{"name":"trigger_1","type":"workflow/v1/newWorkflowCall"}],"tasks":[]}""");

            List<IntegrationWorkflowDTO> integrationWorkflowDTOs =
                mcpIntegrationInstanceConfigurationWorkflowFacade
                    .getToolEligibleIntegrationInstanceConfigurationWorkflows(integrationInstanceConfigurationId);

            assertThat(integrationWorkflowDTOs)
                .extracting(IntegrationWorkflowDTO::getId)
                .containsExactlyInAnyOrder(
                    "5eed0000-0000-0000-0000-0000000000a1", "5eed0000-0000-0000-0000-0000000000a4");
        }

        private long insert(String sql, Object... arguments) {
            return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Long.class, arguments));
        }

        private void insertIntegrationWorkflow(String workflowId, String definition) {
            insertIntegrationWorkflow(INTEGRATION_VERSION, workflowId, definition);
        }

        private void insertIntegrationWorkflow(int integrationVersion, String workflowId, String definition) {
            jdbcTemplate.update(
                """
                    INSERT INTO workflow (id, definition, format, created_date, created_by, last_modified_date,
                        last_modified_by, version)
                    VALUES (?, ?, 0, now(), 'system', now(), 'system', 0)
                    """,
                workflowId, definition);

            integrationWorkflowRepository.save(
                new IntegrationWorkflow(integrationId, integrationVersion, workflowId, UUID.randomUUID()));
        }

        @Configuration
        @EnableCaching
        @Import({
            IntegrationInstanceConfigurationServiceImpl.class, IntegrationWorkflowServiceImpl.class,
            McpIntegrationInstanceConfigurationWorkflowFacadeImpl.class
        })
        static class ToolEligibleWorkflowsConfiguration {

            @Bean
            WorkflowService workflowService(
                CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
                List<WorkflowRepository> workflowRepositories) {

                return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
            }
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(
        classes = {
            EmbeddedMcpIntTestConfiguration.class, AuditIntTest.AuditConfiguration.class
        },
        properties = {
            "bytechef.edition=ee", "bytechef.audit.enabled=true"
        })
    @MockitoBean(types = {
        IntegrationInstanceConfigurationService.class, IntegrationInstanceConfigurationWorkflowService.class,
        IntegrationInstanceWorkflowService.class, IntegrationWorkflowService.class,
        McpIntegrationInstanceConfigurationService.class, McpIntegrationInstanceRows.class, PermissionService.class,
        WorkflowService.class
    })
    class AuditIntTest {

        private static final long INTEGRATION_INSTANCE_CONFIGURATION_ID = 1L;
        private static final long INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID = 5L;
        private static final long MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID = 3L;
        private static final long MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID = 9L;
        private static final long MCP_SERVER_ID = 2L;

        @Autowired
        private McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;

        @Autowired
        private McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade;

        @MockitoBean
        private McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

        @Autowired
        private PersistenceAuditEventRepository persistenceAuditEventRepository;

        @BeforeEach
        void beforeEach() {
            persistenceAuditEventRepository.deleteAll();
        }

        @AfterEach
        void afterEach() {
            persistenceAuditEventRepository.deleteAll();
        }

        @Test
        void testCreateRecordsTheWorkflowAndItsConfiguration() {
            when(
                mcpIntegrationInstanceConfigurationWorkflowService.create(
                    MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID, INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID))
                        .thenReturn(newMcpIntegrationInstanceConfigurationWorkflow());
            when(
                mcpIntegrationInstanceConfigurationService.fetchMcpIntegrationInstanceConfiguration(
                    MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID))
                        .thenReturn(
                            Optional.of(
                                new McpIntegrationInstanceConfiguration(
                                    MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID, INTEGRATION_INSTANCE_CONFIGURATION_ID,
                                    MCP_SERVER_ID)));

            mcpIntegrationInstanceConfigurationWorkflowFacade.createMcpIntegrationInstanceConfigurationWorkflow(
                MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID, INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID);

            assertThat(
                findSingleEventData(
                    McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_CREATED))
                        .containsEntry("result", "SUCCESS")
                        .containsEntry(
                            "mcpIntegrationInstanceConfigurationWorkflowId",
                            String.valueOf(MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID))
                        .containsEntry(
                            "mcpIntegrationInstanceConfigurationId",
                            String.valueOf(MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID))
                        .containsEntry(
                            "integrationInstanceConfigurationWorkflowId",
                            String.valueOf(INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID));
        }

        @Test
        void testDeleteRecordsTheWorkflow() {
            when(
                mcpIntegrationInstanceConfigurationWorkflowService.fetchMcpIntegrationInstanceConfigurationWorkflow(
                    MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID))
                        .thenReturn(Optional.of(newMcpIntegrationInstanceConfigurationWorkflow()));

            mcpIntegrationInstanceConfigurationWorkflowFacade.deleteMcpIntegrationInstanceConfigurationWorkflow(
                MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID);

            assertThat(
                findSingleEventData(
                    McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_DELETED))
                        .containsEntry("result", "SUCCESS")
                        .containsEntry(
                            "mcpIntegrationInstanceConfigurationWorkflowId",
                            String.valueOf(MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID));
        }

        @Test
        void testDeleteOfAnUnknownWorkflowRecordsAFailure() {
            when(
                mcpIntegrationInstanceConfigurationWorkflowService.fetchMcpIntegrationInstanceConfigurationWorkflow(
                    MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID))
                        .thenReturn(Optional.empty());

            assertThatThrownBy(
                () -> mcpIntegrationInstanceConfigurationWorkflowFacade
                    .deleteMcpIntegrationInstanceConfigurationWorkflow(
                        MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID))
                            .isInstanceOf(IllegalArgumentException.class);

            assertThat(
                findSingleEventData(
                    McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_DELETED))
                        .containsEntry("result", "ERROR")
                        .containsEntry("errorClass", IllegalArgumentException.class.getName())
                        .containsEntry(
                            "mcpIntegrationInstanceConfigurationWorkflowId",
                            String.valueOf(MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID));
        }

        @Test
        void testUpdateEnabledRecordsTheWorkflowItsConfigurationAndTheNewState() {
            when(
                mcpIntegrationInstanceConfigurationWorkflowService.updateEnabled(
                    MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID, false))
                        .thenReturn(newMcpIntegrationInstanceConfigurationWorkflow());

            mcpIntegrationInstanceConfigurationWorkflowFacade.updateMcpIntegrationInstanceConfigurationWorkflowEnabled(
                MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID, false);

            assertThat(
                findSingleEventData(
                    McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ENABLED_UPDATED))
                        .containsEntry("result", "SUCCESS")
                        .containsEntry(
                            "mcpIntegrationInstanceConfigurationWorkflowId",
                            String.valueOf(MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID))
                        .containsEntry(
                            "mcpIntegrationInstanceConfigurationId",
                            String.valueOf(MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID))
                        .containsEntry("enabled", "false");
        }

        private Map<String, String> findSingleEventData(String eventType) {
            return EmbeddedMcpAuditIntTestConfiguration.findSingleEventData(persistenceAuditEventRepository, eventType);
        }

        private static McpIntegrationInstanceConfigurationWorkflow newMcpIntegrationInstanceConfigurationWorkflow() {
            McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
                new McpIntegrationInstanceConfigurationWorkflow(
                    MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID, INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID);

            mcpIntegrationInstanceConfigurationWorkflow.setId(MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ID);

            return mcpIntegrationInstanceConfigurationWorkflow;
        }

        @Configuration
        @Import({
            EmbeddedMcpAuditIntTestConfiguration.class, McpIntegrationInstanceConfigurationWorkflowFacadeImpl.class
        })
        static class AuditConfiguration {
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(
        classes = {
            EmbeddedMcpIntTestConfiguration.class, LinkIntTest.LinkConfiguration.class
        },
        properties = {
            "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true"
        })
    @MockitoBean(types = PermissionService.class)
    class LinkIntTest {

        private static final String SECOND_WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000c2";
        private static final String WORKFLOW_ID = "5eed0000-0000-0000-0000-0000000000c1";

        @Autowired
        private Encryption encryption;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade;

        @Autowired
        private McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

        @Autowired
        private McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService;

        @Autowired
        private McpServerRepository mcpServerRepository;

        private McpRowsIntTestFixture fixture;
        private long integrationInstanceConfigurationId;
        private long integrationInstanceId;
        private long linkId;
        private long mcpIntegrationInstanceConfigurationId;
        private long secondIntegrationInstanceConfigurationWorkflowId;

        @BeforeEach
        void beforeEach() {
            fixture = new McpRowsIntTestFixture(encryption, jdbcTemplate);

            McpServer mcpServer = mcpServerRepository.save(
                new McpServer("Google", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

            integrationInstanceConfigurationId = fixture.insertIntegrationInstanceConfiguration(
                fixture.insertIntegration("gmail"), 1);
            mcpIntegrationInstanceConfigurationId = fixture.insertMcpIntegrationInstanceConfiguration(
                mcpServer.getId(), integrationInstanceConfigurationId);
            linkId = fixture.insertLink(
                mcpIntegrationInstanceConfigurationId,
                fixture.insertConfigurationWorkflow(integrationInstanceConfigurationId, WORKFLOW_ID, true), true);
            secondIntegrationInstanceConfigurationWorkflowId = fixture.insertConfigurationWorkflow(
                integrationInstanceConfigurationId, SECOND_WORKFLOW_ID, true);
            integrationInstanceId = fixture.insertIntegrationInstance(
                integrationInstanceConfigurationId, fixture.insertConnectedUser("user-1"),
                fixture.insertConnection("gmail"));

            fixture.insertInstanceWorkflow(
                integrationInstanceId, integrationInstanceConfigurationId, WORKFLOW_ID, true);

            mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
                integrationInstanceId, linkId, true);
        }

        @AfterEach
        void afterEach() {
            fixture.deleteAll();

            mcpServerRepository.deleteAll();
        }

        @Test
        void testDeleteMcpWorkflowKeepsTheIntegrationConfigurationWorkflow() {
            mcpIntegrationInstanceConfigurationWorkflowFacade.deleteMcpIntegrationInstanceConfigurationWorkflow(
                linkId);

            assertThat(
                mcpIntegrationInstanceConfigurationWorkflowService.fetchMcpIntegrationInstanceConfigurationWorkflow(
                    linkId)).isEmpty();
            assertThat(
                jdbcTemplate.queryForList(
                    "SELECT enabled FROM integration_instance_configuration_workflow WHERE workflow_id = ?",
                    Boolean.class, WORKFLOW_ID)).containsExactly(true);
            assertThat(fixture.getInstanceWorkflowEnabled(integrationInstanceId, WORKFLOW_ID)).containsExactly(true);
            assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
                .isEmpty();
        }

        @Test
        void testRepointingALinkIsRejected() {
            assertThatThrownBy(
                () -> mcpIntegrationInstanceConfigurationWorkflowFacade
                    .updateMcpIntegrationInstanceConfigurationWorkflow(
                        linkId, null, secondIntegrationInstanceConfigurationWorkflowId))
                            .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(
                () -> mcpIntegrationInstanceConfigurationWorkflowFacade
                    .updateMcpIntegrationInstanceConfigurationWorkflow(
                        linkId, mcpIntegrationInstanceConfigurationId + 1, null))
                            .isInstanceOf(IllegalArgumentException.class);

            McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
                mcpIntegrationInstanceConfigurationWorkflowService
                    .fetchMcpIntegrationInstanceConfigurationWorkflow(linkId)
                    .orElseThrow();

            assertThat(mcpIntegrationInstanceConfigurationWorkflow.getMcpIntegrationInstanceConfigurationId())
                .isEqualTo(mcpIntegrationInstanceConfigurationId);
            assertThat(mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId())
                .isNotEqualTo(secondIntegrationInstanceConfigurationWorkflowId);
        }

        @Test
        void testCreatedLinkReachesConnectedUserWithTheServer() {
            McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
                mcpIntegrationInstanceConfigurationWorkflowFacade.createMcpIntegrationInstanceConfigurationWorkflow(
                    mcpIntegrationInstanceConfigurationId, secondIntegrationInstanceConfigurationWorkflowId);

            assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
                .extracting(
                    McpIntegrationInstanceWorkflow::getMcpIntegrationInstanceConfigurationWorkflowId,
                    McpIntegrationInstanceWorkflow::isEnabled)
                .containsExactlyInAnyOrder(
                    tuple(linkId, true), tuple(mcpIntegrationInstanceConfigurationWorkflow.getId(), true));
        }

        @Test
        void testLinkSwitchedBackOnReachesConnectedUsers() {
            long secondLinkId = fixture.insertLink(
                mcpIntegrationInstanceConfigurationId, secondIntegrationInstanceConfigurationWorkflowId, false);

            mcpIntegrationInstanceConfigurationWorkflowFacade.updateMcpIntegrationInstanceConfigurationWorkflowEnabled(
                secondLinkId, true);

            assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
                .extracting(McpIntegrationInstanceWorkflow::getMcpIntegrationInstanceConfigurationWorkflowId)
                .containsExactlyInAnyOrder(linkId, secondLinkId);
        }

        @Configuration
        @EnableCaching
        @Import({
            IntegrationInstanceConfigurationServiceImpl.class,
            IntegrationInstanceConfigurationWorkflowServiceImpl.class, IntegrationInstanceServiceImpl.class,
            IntegrationInstanceWorkflowServiceImpl.class, IntegrationServiceImpl.class,
            IntegrationWorkflowServiceImpl.class, McpComponentServiceImpl.class,
            McpIntegrationInstanceConfigurationServiceImpl.class,
            McpIntegrationInstanceConfigurationWorkflowFacadeImpl.class, McpIntegrationInstanceRowsImpl.class
        })
        static class LinkConfiguration {

            @Bean
            WorkflowService workflowService(
                CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
                List<WorkflowRepository> workflowRepositories) {

                return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
            }
        }
    }

    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        static PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            PermissionService permissionService,
            ObjectProvider<MethodSecurityExpressionContributor> methodSecurityExpressionContributorProvider) {
            return new AutomationMethodSecurityExpressionHandler(
                permissionService, methodSecurityExpressionContributorProvider);
        }

        @Bean
        McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade()
            throws ReflectiveOperationException {
            Constructor<?> constructor =
                McpIntegrationInstanceConfigurationWorkflowFacadeImpl.class.getDeclaredConstructors()[0];

            Object[] arguments = Arrays.stream(constructor.getParameterTypes())
                .map(parameterType -> (Object) mock(parameterType))
                .toArray();

            return (McpIntegrationInstanceConfigurationWorkflowFacade) constructor.newInstance(arguments);
        }
    }
}
