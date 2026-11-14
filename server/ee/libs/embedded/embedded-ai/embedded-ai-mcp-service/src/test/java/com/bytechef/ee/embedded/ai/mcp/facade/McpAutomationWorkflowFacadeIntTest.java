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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.ee.embedded.ai.mcp.audit.ConnectedUserMcpServerAuditEvents;
import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpAuditIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.config.McpRowsIntTestFixture;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.ai.mcp.exception.McpAutomationWorkflowNotFoundException;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentication;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.service.McpComponentServiceImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EmbeddedMcpIntTestConfiguration.class, McpAutomationWorkflowFacadeIntTest.FacadeIntTestConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.audit.enabled=true", "bytechef.workflow.repository.jdbc.enabled=true"
    })
public class McpAutomationWorkflowFacadeIntTest {

    private static final String OTHER_WORKFLOW_UUID = "7eed0000-0000-0000-0000-00000000dddd";
    private static final String SEND_WORKFLOW_UUID = "7eed0000-0000-0000-0000-00000000aaaa";
    private static final String UNKNOWN_WORKFLOW_UUID = "7eed0000-0000-0000-0000-00000000ffff";

    @Autowired
    private Encryption encryption;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService;

    @Autowired
    private McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService;

    @Autowired
    private McpAutomationWorkflowFacade mcpAutomationWorkflowFacade;

    @Autowired
    private McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    private long automationProjectConfigurationId;
    private long connectedUserId;
    private McpRowsIntTestFixture fixture;
    private McpServer firstMcpServer;
    private long firstSendLinkWorkflowId;
    private long projectId;
    private long referenceId;
    private McpServer secondMcpServer;
    private long secondSendLinkWorkflowId;

    @BeforeEach
    public void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        fixture = new McpRowsIntTestFixture(encryption, jdbcTemplate);

        firstMcpServer = mcpServerRepository.save(
            new McpServer("First", PlatformType.EMBEDDED, Environment.DEVELOPMENT));
        secondMcpServer = mcpServerRepository.save(
            new McpServer("Second", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        connectedUserId = fixture.insertConnectedUser("user-1");

        projectId = fixture.insertAutomationProject();
        automationProjectConfigurationId = fixture.insertAutomationProjectConfiguration(projectId);

        firstSendLinkWorkflowId = createLinkWorkflow(firstMcpServer.getId(), SEND_WORKFLOW_UUID);
        secondSendLinkWorkflowId = createLinkWorkflow(secondMcpServer.getId(), SEND_WORKFLOW_UUID);

        fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);

        referenceId = fixture.insertReference(connectedUserId, projectId, SEND_WORKFLOW_UUID, false);
    }

    @AfterEach
    public void afterEach() {
        fixture.deleteAll();

        mcpServerRepository.deleteAll();
    }

    @Test
    public void testEnableSetsEveryServersRow() {
        mcpConnectedUserProjectWorkflowService.create(referenceId, firstSendLinkWorkflowId, false);
        mcpConnectedUserProjectWorkflowService.create(referenceId, secondSendLinkWorkflowId, false);

        mcpAutomationWorkflowFacade.enableMcpAutomationWorkflow(connectedUserId, SEND_WORKFLOW_UUID, true);

        assertThat(getRows(connectedUserId))
            .extracting(McpConnectedUserProjectWorkflow::isEnabled)
            .containsExactly(true, true);
        assertThat(
            EmbeddedMcpAuditIntTestConfiguration.findSingleEventData(
                persistenceAuditEventRepository,
                ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_AUTOMATION_WORKFLOW_ENABLED_UPDATED))
                    .containsEntry("result", "SUCCESS")
                    .containsEntry("connectedUserId", String.valueOf(connectedUserId))
                    .containsEntry("workflowUuid", SEND_WORKFLOW_UUID)
                    .containsEntry("enabled", "true");
    }

    @Test
    public void testDisableSetsEveryServersRow() {
        mcpConnectedUserProjectWorkflowService.create(referenceId, firstSendLinkWorkflowId, true);
        mcpConnectedUserProjectWorkflowService.create(referenceId, secondSendLinkWorkflowId, true);

        mcpAutomationWorkflowFacade.enableMcpAutomationWorkflow(connectedUserId, SEND_WORKFLOW_UUID, false);

        assertThat(getRows(connectedUserId))
            .extracting(McpConnectedUserProjectWorkflow::isEnabled)
            .containsExactly(false, false);
    }

    @Test
    public void testOtherUsersRowsUntouched() {
        long otherConnectedUserId = fixture.insertConnectedUser("user-2");

        fixture.insertAssignment(otherConnectedUserId, projectId, automationProjectConfigurationId);

        long otherReferenceId = fixture.insertReference(
            otherConnectedUserId, projectId, SEND_WORKFLOW_UUID, false);

        mcpConnectedUserProjectWorkflowService.create(referenceId, firstSendLinkWorkflowId, true);
        mcpConnectedUserProjectWorkflowService.create(otherReferenceId, firstSendLinkWorkflowId, true);

        mcpAutomationWorkflowFacade.enableMcpAutomationWorkflow(connectedUserId, SEND_WORKFLOW_UUID, false);

        assertThat(getRows(connectedUserId))
            .extracting(McpConnectedUserProjectWorkflow::isEnabled)
            .containsExactly(false);
        assertThat(getRows(otherConnectedUserId))
            .extracting(McpConnectedUserProjectWorkflow::isEnabled)
            .containsExactly(true);
    }

    @Test
    public void testOtherWorkflowsRowsUntouched() {
        long otherLinkWorkflowId = fixture.insertAutomationLinkWorkflow(firstMcpServer.getId(), OTHER_WORKFLOW_UUID);
        long otherReferenceId = fixture.insertReference(connectedUserId, projectId, OTHER_WORKFLOW_UUID, false);

        mcpConnectedUserProjectWorkflowService.create(referenceId, firstSendLinkWorkflowId, true);
        mcpConnectedUserProjectWorkflowService.create(otherReferenceId, otherLinkWorkflowId, true);

        mcpAutomationWorkflowFacade.enableMcpAutomationWorkflow(connectedUserId, SEND_WORKFLOW_UUID, false);

        assertThat(getRows(connectedUserId))
            .extracting(
                McpConnectedUserProjectWorkflow::getConnectedUserProjectWorkflowId,
                McpConnectedUserProjectWorkflow::isEnabled)
            .containsExactlyInAnyOrder(
                tuple(referenceId, false),
                tuple(otherReferenceId, true));
    }

    @Test
    public void testNotFoundWithoutReference() {
        assertThatThrownBy(
            () -> mcpAutomationWorkflowFacade.enableMcpAutomationWorkflow(
                connectedUserId, UNKNOWN_WORKFLOW_UUID, true))
                    .isInstanceOf(McpAutomationWorkflowNotFoundException.class);

        assertThat(
            EmbeddedMcpAuditIntTestConfiguration.findSingleEventData(
                persistenceAuditEventRepository,
                ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_AUTOMATION_WORKFLOW_ENABLED_UPDATED))
                    .containsEntry("result", "ERROR");
    }

    @Test
    public void testNotFoundForAnotherUsersReference() {
        long otherConnectedUserId = fixture.insertConnectedUser("user-2");

        assertThatThrownBy(
            () -> mcpAutomationWorkflowFacade.enableMcpAutomationWorkflow(
                otherConnectedUserId, SEND_WORKFLOW_UUID, true))
                    .isInstanceOf(McpAutomationWorkflowNotFoundException.class);
    }

    @Test
    public void testNotFoundWhenConnectedUserActsForAnotherUser() {
        long otherConnectedUserId = fixture.insertConnectedUser("user-2");

        fixture.insertAssignment(otherConnectedUserId, projectId, automationProjectConfigurationId);

        long otherReferenceId = fixture.insertReference(
            otherConnectedUserId, projectId, SEND_WORKFLOW_UUID, false);

        mcpConnectedUserProjectWorkflowService.create(otherReferenceId, firstSendLinkWorkflowId, true);

        ConnectedUserAuthentication connectedUserAuthentication = mock(
            ConnectedUserAuthentication.class, withSettings().extraInterfaces(Authentication.class));

        Authentication authentication = (Authentication) connectedUserAuthentication;

        when(authentication.isAuthenticated()).thenReturn(true);
        when(connectedUserAuthentication.connectedUserId()).thenReturn(connectedUserId);

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        try {
            assertThatThrownBy(
                () -> mcpAutomationWorkflowFacade.enableMcpAutomationWorkflow(
                    otherConnectedUserId, SEND_WORKFLOW_UUID, false))
                        .isInstanceOf(McpAutomationWorkflowNotFoundException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertThat(getRows(otherConnectedUserId))
            .extracting(McpConnectedUserProjectWorkflow::isEnabled)
            .containsExactly(true);
    }

    @Test
    public void testAutomationSwitchUnchanged() {
        mcpConnectedUserProjectWorkflowService.create(referenceId, firstSendLinkWorkflowId, true);

        mcpAutomationWorkflowFacade.enableMcpAutomationWorkflow(connectedUserId, SEND_WORKFLOW_UUID, false);

        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT enabled FROM connected_user_project_workflow WHERE id = ?", Boolean.class, referenceId))
                    .isTrue();
    }

    @Test
    public void testMcpEnabledNullWhenNotOffered() {
        assertThat(mcpAutomationWorkflowFacade.getMcpEnabledByAutomationUuid(connectedUserId)).isEmpty();
    }

    @Test
    public void testMcpEnabledTrueWhenAnyRowIsOn() {
        mcpConnectedUserProjectWorkflowService.create(referenceId, firstSendLinkWorkflowId, false);
        mcpConnectedUserProjectWorkflowService.create(referenceId, secondSendLinkWorkflowId, true);

        assertThat(mcpAutomationWorkflowFacade.getMcpEnabledByAutomationUuid(connectedUserId))
            .isEqualTo(Map.of(SEND_WORKFLOW_UUID, true));
    }

    @Test
    public void testMcpEnabledFalseWhenEveryRowIsOff() {
        mcpConnectedUserProjectWorkflowService.create(referenceId, firstSendLinkWorkflowId, false);
        mcpConnectedUserProjectWorkflowService.create(referenceId, secondSendLinkWorkflowId, false);

        assertThat(mcpAutomationWorkflowFacade.getMcpEnabledByAutomationUuid(connectedUserId))
            .isEqualTo(Map.of(SEND_WORKFLOW_UUID, false));
    }

    @Test
    public void testMcpEnabledIgnoresRowsOfLinkWorkflowsSwitchedOff() {
        mcpConnectedUserProjectWorkflowService.create(referenceId, firstSendLinkWorkflowId, true);
        mcpConnectedUserProjectWorkflowService.create(referenceId, secondSendLinkWorkflowId, false);

        mcpAutomationProjectConfigurationWorkflowService.updateEnabled(firstSendLinkWorkflowId, false);

        assertThat(mcpAutomationWorkflowFacade.getMcpEnabledByAutomationUuid(connectedUserId))
            .isEqualTo(Map.of(SEND_WORKFLOW_UUID, false));
    }

    @Test
    public void testMcpEnabledNullWhenOnlyLinkWorkflowsSwitchedOffOfferIt() {
        mcpConnectedUserProjectWorkflowService.create(referenceId, firstSendLinkWorkflowId, true);
        mcpConnectedUserProjectWorkflowService.create(referenceId, secondSendLinkWorkflowId, true);

        mcpAutomationProjectConfigurationWorkflowService.updateEnabled(firstSendLinkWorkflowId, false);
        mcpAutomationProjectConfigurationWorkflowService.updateEnabled(secondSendLinkWorkflowId, false);

        assertThat(mcpAutomationWorkflowFacade.getMcpEnabledByAutomationUuid(connectedUserId)).isEmpty();
    }

    @Test
    public void testMcpEnabledNullWhenConfigurationSwitchedOff() {
        mcpConnectedUserProjectWorkflowService.create(referenceId, firstSendLinkWorkflowId, true);

        jdbcTemplate.update(
            "UPDATE automation_project_configuration SET enabled = false WHERE id = ?",
            automationProjectConfigurationId);

        assertThat(mcpAutomationWorkflowFacade.getMcpEnabledByAutomationUuid(connectedUserId)).isEmpty();
    }

    private long createLinkWorkflow(long mcpServerId, String workflowUuid) {
        McpAutomationProjectConfiguration link = mcpAutomationProjectConfigurationService.create(
            new McpAutomationProjectConfiguration(automationProjectConfigurationId, mcpServerId));

        McpAutomationProjectConfigurationWorkflow linkWorkflow =
            mcpAutomationProjectConfigurationWorkflowService.create(link.getId(), workflowUuid);

        return linkWorkflow.getId();
    }

    private List<McpConnectedUserProjectWorkflow> getRows(long rowsConnectedUserId) {
        return mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(
            rowsConnectedUserId);
    }

    @Configuration
    @EnableCaching
    @Import({
        AutomationProjectConfigurationServiceImpl.class, ConnectedUserProjectConfigurationServiceImpl.class,
        ConnectedUserProjectServiceImpl.class,
        ConnectedUserProjectWorkflowServiceImpl.class, EmbeddedMcpAuditIntTestConfiguration.class,
        IntegrationInstanceServiceImpl.class, McpAutomationWorkflowFacadeImpl.class, McpComponentServiceImpl.class,
        McpIntegrationInstanceConfigurationServiceImpl.class
    })
    static class FacadeIntTestConfiguration {
    }
}
