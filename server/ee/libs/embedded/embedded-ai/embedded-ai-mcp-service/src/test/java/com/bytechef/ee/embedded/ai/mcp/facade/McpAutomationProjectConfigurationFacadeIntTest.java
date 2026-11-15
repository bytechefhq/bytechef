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
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectServiceImpl;
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.audit.McpAutomationProjectConfigurationAuditEvents;
import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpAuditIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.config.McpRowsIntTestFixture;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationProjectConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationWorkflowConfigurationDTO;
import com.bytechef.ee.embedded.ai.mcp.event.EmbeddedMcpServerBeforeDeleteEventListener;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.event.AutomationProjectConfigurationUpdatedEvent;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.service.McpComponentServiceImpl;
import com.bytechef.platform.mcp.service.McpServerServiceImpl;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringJUnitConfig(McpAutomationProjectConfigurationFacadeIntTest.MethodSecurityConfiguration.class)
class McpAutomationProjectConfigurationFacadeIntTest {

    @Autowired
    private McpAutomationProjectConfigurationFacade mcpAutomationProjectConfigurationFacade;

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
    void testNonAdminDenied() {
        List<Method> methods = Arrays.stream(McpAutomationProjectConfigurationFacade.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> !method.isDefault())
            .toList();

        assertThat(methods).isNotEmpty();

        for (Method method : methods) {
            assertThatThrownBy(() -> invoke(method))
                .as("%s.%s", McpAutomationProjectConfigurationFacade.class.getSimpleName(), method.getName())
                .isInstanceOf(AccessDeniedException.class);
        }

        verify(permissionService, times(methods.size())).isTenantAdmin();
    }

    private void invoke(Method method) throws Throwable {
        Object[] arguments = Arrays.stream(method.getParameterTypes())
            .map(McpAutomationProjectConfigurationFacadeIntTest::defaultValue)
            .toArray();

        try {
            method.invoke(mcpAutomationProjectConfigurationFacade, arguments);
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
    @RecordApplicationEvents
    @SpringBootTest(
        classes = {
            EmbeddedMcpIntTestConfiguration.class, LinkIntTest.LinkConfiguration.class
        },
        properties = {
            "bytechef.edition=ee", "bytechef.audit.enabled=true", "bytechef.workflow.repository.jdbc.enabled=true"
        })
    @MockitoBean(types = PermissionService.class)
    class LinkIntTest {

        private static final String FIRST_WORKFLOW_UUID = "7eed0000-0000-0000-0000-0000000000a1";
        private static final String NO_TRIGGER_WORKFLOW_UUID = "7eed0000-0000-0000-0000-0000000000a3";
        private static final String SECOND_WORKFLOW_UUID = "7eed0000-0000-0000-0000-0000000000a2";

        @Autowired
        private ApplicationEvents applicationEvents;

        @Autowired
        private AutomationProjectConfigurationService automationProjectConfigurationService;

        @Autowired
        private Encryption encryption;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private McpAutomationProjectConfigurationFacade mcpAutomationProjectConfigurationFacade;

        @Autowired
        private McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService;

        @Autowired
        private McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService;

        @Autowired
        private McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService;

        @Autowired
        private McpServerRepository mcpServerRepository;

        @Autowired
        private PersistenceAuditEventRepository persistenceAuditEventRepository;

        private long automationProjectConfigurationId;
        private McpRowsIntTestFixture fixture;
        private long mcpServerId;
        private long projectId;

        @BeforeEach
        void beforeEach() {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

            persistenceAuditEventRepository.deleteAll();

            fixture = new McpRowsIntTestFixture(encryption, jdbcTemplate);

            McpServer mcpServer = mcpServerRepository.save(
                new McpServer("Google", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

            mcpServerId = mcpServer.getId();

            projectId = fixture.insertAutomationProject();
            automationProjectConfigurationId = fixture.insertAutomationProjectConfiguration(projectId);

            jdbcTemplate.update(
                "UPDATE automation_project_configuration SET version = 1 WHERE id = ?",
                automationProjectConfigurationId);

            insertProjectWorkflow(FIRST_WORKFLOW_UUID, true);
            insertProjectWorkflow(SECOND_WORKFLOW_UUID, true);
            insertProjectWorkflow(NO_TRIGGER_WORKFLOW_UUID, false);
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();

            jdbcTemplate.update("DELETE FROM project_workflow");

            fixture.deleteAll();

            mcpServerRepository.deleteAll();
            persistenceAuditEventRepository.deleteAll();
        }

        @Test
        void testCreateRejectsOtherEnvironment() {
            McpServer productionMcpServer = mcpServerRepository.save(
                new McpServer("Production", PlatformType.EMBEDDED, Environment.PRODUCTION));

            assertThatThrownBy(() -> mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                productionMcpServer.getId(), automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID),
                List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                        "Automation project configuration " + automationProjectConfigurationId +
                            " is not in the environment of MCP server " + productionMcpServer.getId());

            assertThat(mcpAutomationProjectConfigurationService.getMcpServerMcpAutomationProjectConfigurations(
                productionMcpServer.getId())).isEmpty();
        }

        @Test
        void testCreateRejectsWorkflowWithoutWorkflowCallTrigger() {
            assertThatThrownBy(() -> mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID, NO_TRIGGER_WORKFLOW_UUID),
                List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                        "Workflow " + NO_TRIGGER_WORKFLOW_UUID +
                            " has no workflow call trigger in automation project configuration " +
                            automationProjectConfigurationId);

            assertThat(mcpAutomationProjectConfigurationService.getMcpServerMcpAutomationProjectConfigurations(
                mcpServerId)).isEmpty();
        }

        @Test
        void testCreateRejectsWorkflowDisabledInConfiguration() {
            automationProjectConfigurationService.updateWorkflowEnabled(
                automationProjectConfigurationId, UUID.fromString(FIRST_WORKFLOW_UUID), false);

            assertThatThrownBy(() -> mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                        "Workflow " + FIRST_WORKFLOW_UUID + " is turned off in automation project configuration " +
                            automationProjectConfigurationId);

            assertThat(mcpAutomationProjectConfigurationService.getMcpServerMcpAutomationProjectConfigurations(
                mcpServerId)).isEmpty();
        }

        @Test
        void testCreateDoesNotChangeConfiguration() {
            automationProjectConfigurationService.updateWorkflowEnabled(
                automationProjectConfigurationId, UUID.fromString(SECOND_WORKFLOW_UUID), false);

            AutomationProjectConfiguration before = automationProjectConfigurationService.getConfiguration(
                automationProjectConfigurationId);

            int versionBefore = before.getVersion();

            applicationEvents.clear();

            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID),
                    List.of(
                        new McpAutomationWorkflowConfigurationDTO(
                            FIRST_WORKFLOW_UUID, Map.of("name", "send_message"))));

            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfiguration(
                mcpAutomationProjectConfiguration.getId(), List.of(FIRST_WORKFLOW_UUID), List.of());

            AutomationProjectConfiguration after = automationProjectConfigurationService.getConfiguration(
                automationProjectConfigurationId);

            assertThat(after.getVersion()).isEqualTo(versionBefore);
            assertThat(after.getProjectVersion()).isEqualTo(before.getProjectVersion());
            assertThat(after.isWorkflowEnabled(UUID.fromString(FIRST_WORKFLOW_UUID))).isTrue();
            assertThat(after.isWorkflowEnabled(UUID.fromString(SECOND_WORKFLOW_UUID))).isFalse();
            assertThat(applicationEvents.stream(AutomationProjectConfigurationUpdatedEvent.class)).isEmpty();

            assertThat(
                mcpAutomationProjectConfigurationWorkflowService.getMcpAutomationProjectConfigurationWorkflows(
                    mcpAutomationProjectConfiguration.getId()))
                        .extracting(
                            McpAutomationProjectConfigurationWorkflow::getWorkflowUuid,
                            McpAutomationProjectConfigurationWorkflow::getParameters)
                        .containsExactly(tuple(FIRST_WORKFLOW_UUID, Map.of("name", "send_message")));
        }

        @Test
        void testUpdateAddsAndRemovesWorkflows() {
            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfiguration(
                mcpAutomationProjectConfiguration.getId(), List.of(SECOND_WORKFLOW_UUID),
                List.of(new McpAutomationWorkflowConfigurationDTO(SECOND_WORKFLOW_UUID, Map.of("name", "second"))));

            assertThat(
                mcpAutomationProjectConfigurationWorkflowService.getMcpAutomationProjectConfigurationWorkflows(
                    mcpAutomationProjectConfiguration.getId()))
                        .extracting(
                            McpAutomationProjectConfigurationWorkflow::getWorkflowUuid,
                            McpAutomationProjectConfigurationWorkflow::getParameters)
                        .containsExactly(tuple(SECOND_WORKFLOW_UUID, Map.of("name", "second")));
        }

        @Test
        void testUpdateRejectsNewWorkflowDisabledInConfiguration() {
            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            automationProjectConfigurationService.updateWorkflowEnabled(
                automationProjectConfigurationId, UUID.fromString(SECOND_WORKFLOW_UUID), false);

            assertThatThrownBy(() -> mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfiguration(
                mcpAutomationProjectConfiguration.getId(), List.of(FIRST_WORKFLOW_UUID, SECOND_WORKFLOW_UUID),
                List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SECOND_WORKFLOW_UUID + " is turned off");

            assertThat(
                mcpAutomationProjectConfigurationWorkflowService.getMcpAutomationProjectConfigurationWorkflows(
                    mcpAutomationProjectConfiguration.getId())).hasSize(1);
        }

        @Test
        void testCreateAddsRowsForActivatedUsers() {
            long connectedUserId = fixture.insertConnectedUser("user-1");
            long notActivatedConnectedUserId = fixture.insertConnectedUser("user-2");

            fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);
            fixture.insertAssignment(notActivatedConnectedUserId, projectId, automationProjectConfigurationId);

            long referenceId = fixture.insertReference(connectedUserId, projectId, FIRST_WORKFLOW_UUID, false);

            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            List<McpAutomationProjectConfigurationWorkflow> linkWorkflows =
                mcpAutomationProjectConfigurationWorkflowService.getMcpAutomationProjectConfigurationWorkflows(
                    mcpAutomationProjectConfiguration.getId());

            assertThat(mcpConnectedUserProjectWorkflowService.getMcpServerMcpConnectedUserProjectWorkflows(mcpServerId))
                .extracting(
                    McpConnectedUserProjectWorkflow::getConnectedUserProjectWorkflowId,
                    McpConnectedUserProjectWorkflow::getMcpAutomationProjectConfigurationWorkflowId,
                    McpConnectedUserProjectWorkflow::isEnabled)
                .containsExactly(tuple(referenceId, linkWorkflows.getFirst()
                    .getId(), true));
        }

        @Test
        void testUpdateDoesNotReAddConnectedUserRemovedFromServer() {
            long connectedUserId = fixture.insertConnectedUser("user-1");

            fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);
            fixture.insertReference(connectedUserId, projectId, FIRST_WORKFLOW_UUID, false);
            fixture.insertReference(connectedUserId, projectId, SECOND_WORKFLOW_UUID, false);

            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            removeConnectedUsersFromMcpServer();

            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfiguration(
                mcpAutomationProjectConfiguration.getId(), List.of(FIRST_WORKFLOW_UUID, SECOND_WORKFLOW_UUID),
                List.of(new McpAutomationWorkflowConfigurationDTO(FIRST_WORKFLOW_UUID, Map.of("name", "renamed"))));

            assertThat(mcpConnectedUserProjectWorkflowService.getMcpServerMcpConnectedUserProjectWorkflows(mcpServerId))
                .isEmpty();
        }

        @Test
        void testWorkflowEnabledToggleDoesNotReAddConnectedUserRemovedFromServer() {
            long connectedUserId = fixture.insertConnectedUser("user-1");

            fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);
            fixture.insertReference(connectedUserId, projectId, FIRST_WORKFLOW_UUID, false);

            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            removeConnectedUsersFromMcpServer();

            long linkWorkflowId = mcpAutomationProjectConfigurationWorkflowService
                .getMcpAutomationProjectConfigurationWorkflows(mcpAutomationProjectConfiguration.getId())
                .getFirst()
                .getId();

            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfigurationWorkflowEnabled(
                linkWorkflowId, false);
            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfigurationWorkflowEnabled(
                linkWorkflowId, true);

            assertThat(mcpConnectedUserProjectWorkflowService.getMcpServerMcpConnectedUserProjectWorkflows(mcpServerId))
                .isEmpty();
        }

        @Test
        void testUpdateAddsRowsForNewWorkflowWithEnabledServerState() {
            assertUpdateAddsRowsForNewWorkflowWithServerState(true);
        }

        @Test
        void testUpdateAddsRowsForNewWorkflowWithDisabledServerState() {
            assertUpdateAddsRowsForNewWorkflowWithServerState(false);
        }

        private void assertUpdateAddsRowsForNewWorkflowWithServerState(boolean serverState) {
            long connectedUserId = fixture.insertConnectedUser("user-1");

            fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);
            fixture.insertReference(connectedUserId, projectId, FIRST_WORKFLOW_UUID, false);

            long secondReferenceId = fixture.insertReference(connectedUserId, projectId, SECOND_WORKFLOW_UUID, false);

            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            for (McpConnectedUserProjectWorkflow row : mcpConnectedUserProjectWorkflowService
                .getMcpServerMcpConnectedUserProjectWorkflows(mcpServerId)) {

                mcpConnectedUserProjectWorkflowService.updateEnabled(row.getId(), serverState);
            }

            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfiguration(
                mcpAutomationProjectConfiguration.getId(), List.of(FIRST_WORKFLOW_UUID, SECOND_WORKFLOW_UUID),
                List.of());

            long secondLinkWorkflowId = mcpAutomationProjectConfigurationWorkflowService
                .getMcpAutomationProjectConfigurationWorkflows(mcpAutomationProjectConfiguration.getId())
                .stream()
                .filter(linkWorkflow -> SECOND_WORKFLOW_UUID.equals(linkWorkflow.getWorkflowUuid()))
                .findFirst()
                .orElseThrow()
                .getId();

            assertThat(mcpConnectedUserProjectWorkflowService.fetch(secondReferenceId, secondLinkWorkflowId))
                .map(McpConnectedUserProjectWorkflow::isEnabled)
                .contains(serverState);
        }

        private void removeConnectedUsersFromMcpServer() {
            for (McpConnectedUserProjectWorkflow row : mcpConnectedUserProjectWorkflowService
                .getMcpServerMcpConnectedUserProjectWorkflows(mcpServerId)) {

                mcpConnectedUserProjectWorkflowService.delete(row.getId());
            }

            assertThat(mcpConnectedUserProjectWorkflowService.getMcpServerMcpConnectedUserProjectWorkflows(mcpServerId))
                .isEmpty();
        }

        @Test
        void testListMarksTurnedOffWorkflowUnavailable() {
            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID, SECOND_WORKFLOW_UUID),
                    List.of());

            automationProjectConfigurationService.updateWorkflowEnabled(
                automationProjectConfigurationId, UUID.fromString(SECOND_WORKFLOW_UUID), false);

            List<McpAutomationProjectConfigurationDTO> mcpAutomationProjectConfigurationDTOs =
                mcpAutomationProjectConfigurationFacade.getMcpServerMcpAutomationProjectConfigurations(mcpServerId);

            assertThat(mcpAutomationProjectConfigurationDTOs).hasSize(1);

            McpAutomationProjectConfigurationDTO mcpAutomationProjectConfigurationDTO =
                mcpAutomationProjectConfigurationDTOs.getFirst();

            assertThat(mcpAutomationProjectConfigurationDTO.id()).isEqualTo(mcpAutomationProjectConfiguration.getId());
            assertThat(mcpAutomationProjectConfigurationDTO.automationProjectConfigurationId())
                .isEqualTo(automationProjectConfigurationId);
            assertThat(mcpAutomationProjectConfigurationDTO.workflows())
                .extracting(
                    McpAutomationProjectConfigurationWorkflowDTO::workflowUuid,
                    McpAutomationProjectConfigurationWorkflowDTO::available)
                .containsExactlyInAnyOrder(tuple(FIRST_WORKFLOW_UUID, true), tuple(SECOND_WORKFLOW_UUID, false));
        }

        @Test
        void testGetToolWorkflowUuidsSkipsWorkflowsWithoutWorkflowCallTrigger() {
            automationProjectConfigurationService.updateWorkflowEnabled(
                automationProjectConfigurationId, UUID.fromString(SECOND_WORKFLOW_UUID), false);

            assertThat(mcpAutomationProjectConfigurationFacade.getToolWorkflowUuids(automationProjectConfigurationId))
                .containsExactlyInAnyOrder(FIRST_WORKFLOW_UUID, SECOND_WORKFLOW_UUID);
        }

        @Test
        void testListReturnsProjectName() {
            mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            assertThat(
                mcpAutomationProjectConfigurationFacade.getMcpServerMcpAutomationProjectConfigurations(mcpServerId))
                    .extracting(McpAutomationProjectConfigurationDTO::projectName)
                    .containsExactly("template");
        }

        @Test
        void testListMarksWorkflowWithoutTriggerAtPinnedVersionUnavailable() {
            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            jdbcTemplate.update("UPDATE project_workflow SET uuid = gen_random_uuid() WHERE uuid = ?",
                UUID.fromString(FIRST_WORKFLOW_UUID));

            assertThat(
                mcpAutomationProjectConfigurationFacade
                    .getMcpAutomationProjectConfiguration(mcpAutomationProjectConfiguration.getId())
                    .workflows())
                        .extracting(McpAutomationProjectConfigurationWorkflowDTO::available)
                        .containsExactly(false);
        }

        @Test
        void testUpdateWorkflowParametersAndEnabled() {
            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            long linkWorkflowId = mcpAutomationProjectConfigurationWorkflowService
                .getMcpAutomationProjectConfigurationWorkflows(mcpAutomationProjectConfiguration.getId())
                .getFirst()
                .getId();

            McpAutomationProjectConfigurationWorkflow parametersUpdated = mcpAutomationProjectConfigurationFacade
                .updateMcpAutomationProjectConfigurationWorkflowParameters(linkWorkflowId, Map.of("name", "tool"));

            assertThat(parametersUpdated.getParameters()).isEqualTo(Map.of("name", "tool"));

            McpAutomationProjectConfigurationWorkflow enabledUpdated = mcpAutomationProjectConfigurationFacade
                .updateMcpAutomationProjectConfigurationWorkflowEnabled(linkWorkflowId, false);

            assertThat(enabledUpdated.isEnabled()).isFalse();
        }

        @Test
        void testDeleteRemovesLinkAndWorkflows() {
            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            mcpAutomationProjectConfigurationFacade.deleteMcpAutomationProjectConfiguration(
                mcpAutomationProjectConfiguration.getId());

            assertThat(mcpAutomationProjectConfigurationService.fetch(mcpAutomationProjectConfiguration.getId()))
                .isEmpty();
            assertThat(
                mcpAutomationProjectConfigurationWorkflowService.getMcpAutomationProjectConfigurationWorkflows(
                    mcpAutomationProjectConfiguration.getId())).isEmpty();
            assertThat(automationProjectConfigurationService.getConfiguration(automationProjectConfigurationId))
                .isNotNull();
        }

        @Test
        void testDeleteWorkflowRemovesOnlyThatWorkflow() {
            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID, SECOND_WORKFLOW_UUID),
                    List.of());

            McpAutomationProjectConfigurationWorkflow firstWorkflow = getLinkWorkflow(
                mcpAutomationProjectConfiguration.getId(), FIRST_WORKFLOW_UUID);

            mcpAutomationProjectConfigurationFacade.deleteMcpAutomationProjectConfigurationWorkflow(
                firstWorkflow.getId());

            assertThat(
                mcpAutomationProjectConfigurationWorkflowService.getMcpAutomationProjectConfigurationWorkflows(
                    mcpAutomationProjectConfiguration.getId()))
                        .extracting(McpAutomationProjectConfigurationWorkflow::getWorkflowUuid)
                        .containsExactly(SECOND_WORKFLOW_UUID);
            assertThat(
                findSingleEventData(
                    McpAutomationProjectConfigurationAuditEvents.MCP_AUTOMATION_PROJECT_CONFIGURATION_WORKFLOW_DELETED))
                        .containsEntry("result", "SUCCESS")
                        .containsEntry(
                            "mcpAutomationProjectConfigurationWorkflowId", String.valueOf(firstWorkflow.getId()));
        }

        @Test
        void testDeleteOfAnUnknownWorkflowRecordsAFailure() {
            long unknownWorkflowId = Long.MAX_VALUE;

            assertThatThrownBy(
                () -> mcpAutomationProjectConfigurationFacade.deleteMcpAutomationProjectConfigurationWorkflow(
                    unknownWorkflowId))
                        .isInstanceOf(IllegalArgumentException.class);

            assertThat(
                findSingleEventData(
                    McpAutomationProjectConfigurationAuditEvents.MCP_AUTOMATION_PROJECT_CONFIGURATION_WORKFLOW_DELETED))
                        .containsEntry("result", "ERROR")
                        .containsEntry("errorClass", IllegalArgumentException.class.getName())
                        .containsEntry("mcpAutomationProjectConfigurationWorkflowId",
                            String.valueOf(unknownWorkflowId));
        }

        @Test
        void testFetchToolWorkflowReturnsOnlyWorkflowsWithWorkflowCallTrigger() {
            assertThat(
                mcpAutomationProjectConfigurationFacade.fetchAutomationProjectConfigurationToolWorkflow(
                    automationProjectConfigurationId, FIRST_WORKFLOW_UUID))
                        .isPresent();
            assertThat(
                mcpAutomationProjectConfigurationFacade.fetchAutomationProjectConfigurationToolWorkflow(
                    automationProjectConfigurationId, NO_TRIGGER_WORKFLOW_UUID))
                        .isEmpty();
        }

        private McpAutomationProjectConfigurationWorkflow getLinkWorkflow(
            long mcpAutomationProjectConfigurationId, String workflowUuid) {

            return mcpAutomationProjectConfigurationWorkflowService
                .getMcpAutomationProjectConfigurationWorkflows(mcpAutomationProjectConfigurationId)
                .stream()
                .filter(linkWorkflow -> workflowUuid.equals(linkWorkflow.getWorkflowUuid()))
                .findFirst()
                .orElseThrow();
        }

        @Test
        void testCreateRecordsTheMcpAutomationProjectConfiguration() {
            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            assertThat(
                findSingleEventData(
                    McpAutomationProjectConfigurationAuditEvents.MCP_AUTOMATION_PROJECT_CONFIGURATION_CREATED))
                        .containsEntry("result", "SUCCESS")
                        .containsEntry(
                            "mcpAutomationProjectConfigurationId",
                            String.valueOf(mcpAutomationProjectConfiguration.getId()))
                        .containsEntry("mcpServerId", String.valueOf(mcpServerId))
                        .containsEntry("automationProjectConfigurationId",
                            String.valueOf(automationProjectConfigurationId))
                        .containsEntry("workflowUuids", "[" + FIRST_WORKFLOW_UUID + "]");
        }

        @Test
        void testUpdateRecordsTheSelectedWorkflows() {
            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfiguration(
                mcpAutomationProjectConfiguration.getId(), List.of(), List.of());

            assertThat(
                findSingleEventData(
                    McpAutomationProjectConfigurationAuditEvents.MCP_AUTOMATION_PROJECT_CONFIGURATION_UPDATED))
                        .containsEntry("result", "SUCCESS")
                        .containsEntry(
                            "mcpAutomationProjectConfigurationId",
                            String.valueOf(mcpAutomationProjectConfiguration.getId()))
                        .containsEntry("mcpServerId", String.valueOf(mcpServerId))
                        .containsEntry("workflowUuids", "[]");
        }

        @Test
        void testDeleteRecordsTheMcpAutomationProjectConfiguration() {
            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            mcpAutomationProjectConfigurationFacade.deleteMcpAutomationProjectConfiguration(
                mcpAutomationProjectConfiguration.getId());

            assertThat(
                findSingleEventData(
                    McpAutomationProjectConfigurationAuditEvents.MCP_AUTOMATION_PROJECT_CONFIGURATION_DELETED))
                        .containsEntry("result", "SUCCESS")
                        .containsEntry(
                            "mcpAutomationProjectConfigurationId",
                            String.valueOf(mcpAutomationProjectConfiguration.getId()));
        }

        @Test
        void testEnabledUpdateRecordsTheWorkflow() {
            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            long linkWorkflowId = mcpAutomationProjectConfigurationWorkflowService
                .getMcpAutomationProjectConfigurationWorkflows(mcpAutomationProjectConfiguration.getId())
                .getFirst()
                .getId();

            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfigurationWorkflowEnabled(
                linkWorkflowId, false);

            assertThat(
                findSingleEventData(
                    McpAutomationProjectConfigurationAuditEvents.MCP_AUTOMATION_PROJECT_CONFIGURATION_WORKFLOW_ENABLED_UPDATED))
                        .containsEntry("result", "SUCCESS")
                        .containsEntry("mcpAutomationProjectConfigurationWorkflowId", String.valueOf(linkWorkflowId))
                        .containsEntry(
                            "mcpAutomationProjectConfigurationId",
                            String.valueOf(mcpAutomationProjectConfiguration.getId()))
                        .containsEntry("enabled", "false");
        }

        @Test
        void testParametersUpdateRecordsTheWorkflowWithoutTheParameters() {
            McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
                mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                    mcpServerId, automationProjectConfigurationId, List.of(FIRST_WORKFLOW_UUID), List.of());

            long linkWorkflowId = mcpAutomationProjectConfigurationWorkflowService
                .getMcpAutomationProjectConfigurationWorkflows(mcpAutomationProjectConfiguration.getId())
                .getFirst()
                .getId();

            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfigurationWorkflowParameters(
                linkWorkflowId, Map.of("toolName", "secret_tool_name"));

            Map<String, String> eventData = findSingleEventData(
                McpAutomationProjectConfigurationAuditEvents.MCP_AUTOMATION_PROJECT_CONFIGURATION_WORKFLOW_PARAMETERS_UPDATED);

            assertThat(eventData)
                .containsEntry("result", "SUCCESS")
                .containsEntry("mcpAutomationProjectConfigurationWorkflowId", String.valueOf(linkWorkflowId))
                .containsEntry(
                    "mcpAutomationProjectConfigurationId", String.valueOf(mcpAutomationProjectConfiguration.getId()));
            assertThat(eventData.values()).noneMatch(value -> value.contains("secret_tool_name"));
        }

        private Map<String, String> findSingleEventData(String eventType) {
            return EmbeddedMcpAuditIntTestConfiguration.findSingleEventData(
                persistenceAuditEventRepository, eventType);
        }

        private void insertProjectWorkflow(String workflowUuid, boolean withWorkflowCallTrigger) {
            String workflowId = "wf-" + workflowUuid;

            String definition = withWorkflowCallTrigger
                ? """
                    {"label":"Workflow","triggers":[{"name":"newWorkflowCall_1","type":"workflow/v1/newWorkflowCall","parameters":{}}],"tasks":[]}"""
                : """
                    {"label":"Workflow","tasks":[]}""";

            jdbcTemplate.update(
                """
                    INSERT INTO workflow (id, definition, format, created_date, created_by, last_modified_date,
                        last_modified_by, version)
                    VALUES (?, ?, 0, now(), 'system', now(), 'system', 0)
                    """,
                workflowId, definition);
            jdbcTemplate.update(
                """
                    INSERT INTO project_workflow (project_id, project_version, workflow_id, uuid, created_date,
                        created_by, last_modified_date, last_modified_by, version)
                    VALUES (?, 1, ?, ?, now(), 'system', now(), 'system', 0)
                    """,
                projectId, workflowId, UUID.fromString(workflowUuid));
        }

        @Configuration
        @EnableCaching
        @Import({
            AutomationProjectConfigurationServiceImpl.class, ConnectedUserProjectConfigurationServiceImpl.class,
            ConnectedUserProjectServiceImpl.class, ConnectedUserProjectWorkflowServiceImpl.class,
            EmbeddedMcpAuditIntTestConfiguration.class, EmbeddedMcpServerBeforeDeleteEventListener.class,
            IntegrationInstanceServiceImpl.class, McpAutomationProjectConfigurationFacadeImpl.class,
            McpAutomationRowFacadeImpl.class, McpComponentServiceImpl.class,
            McpIntegrationInstanceConfigurationServiceImpl.class, McpServerMembership.class,
            McpServerServiceImpl.class, ProjectServiceImpl.class, ProjectWorkflowServiceImpl.class
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
        McpAutomationProjectConfigurationFacade mcpAutomationProjectConfigurationFacade()
            throws ReflectiveOperationException {
            Constructor<?> constructor = McpAutomationProjectConfigurationFacadeImpl.class.getDeclaredConstructors()[0];

            Object[] arguments = Arrays.stream(constructor.getParameterTypes())
                .map(parameterType -> (Object) mock(parameterType))
                .toArray();

            return (McpAutomationProjectConfigurationFacade) constructor.newInstance(arguments);
        }
    }
}
