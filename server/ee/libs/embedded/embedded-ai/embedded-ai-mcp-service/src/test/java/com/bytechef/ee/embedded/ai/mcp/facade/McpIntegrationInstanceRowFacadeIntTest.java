/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.config.McpRowsIntTestFixture;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowServiceImpl;
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
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
        EmbeddedMcpIntTestConfiguration.class, McpIntegrationInstanceRowFacadeIntTest.RowsIntTestConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true"
    })
public class McpIntegrationInstanceRowFacadeIntTest {

    private static final String ARCHIVE_EMAIL_WORKFLOW_ID = "6eed0000-0000-0000-0000-0000000000b1";
    private static final String AUTOMATION_WORKFLOW_UUID = "6eed0000-0000-0000-0000-00000000cccc";
    private static final String SEND_EMAIL_WORKFLOW_ID = "6eed0000-0000-0000-0000-0000000000a1";
    private static final String SEND_EMAIL_WORKFLOW_V7_ID = "6eed0000-0000-0000-0000-0000000000a7";
    private static final String SEND_EMAIL_WORKFLOW_UUID = "6eed0000-0000-0000-0000-00000000aaaa";

    @Autowired
    private Encryption encryption;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpIntegrationInstanceRowFacade mcpIntegrationInstanceRowFacade;

    @Autowired
    private McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @Autowired
    private McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpToolRepository mcpToolRepository;

    private long connectedUserId;
    private long connectionId;
    private long integrationId;
    private long integrationInstanceConfigurationId;
    private long integrationInstanceId;
    private McpServer mcpServer;
    private long mcpIntegrationInstanceConfigurationId;
    private long otherConnectedUserId;
    private long otherIntegrationInstanceId;
    private long searchEmailToolId;
    private long sendEmailLinkId;
    private long getEmailToolId;
    private McpRowsIntTestFixture fixture;

    @BeforeEach
    public void beforeEach() {
        fixture = new McpRowsIntTestFixture(encryption, jdbcTemplate);

        mcpServer = mcpServerRepository.save(new McpServer("Google", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        connectionId = fixture.insertConnection("gmail");

        connectedUserId = fixture.insertConnectedUser("user-1");
        otherConnectedUserId = fixture.insertConnectedUser("user-2");

        integrationId = fixture.insertIntegration("gmail");
        integrationInstanceConfigurationId = fixture.insertIntegrationInstanceConfiguration(integrationId, 6);

        integrationInstanceId =
            fixture.insertIntegrationInstance(integrationInstanceConfigurationId, connectedUserId, connectionId);
        otherIntegrationInstanceId = fixture.insertIntegrationInstance(
            integrationInstanceConfigurationId, otherConnectedUserId, connectionId);

        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent("gmail", 1, mcpServer.getId(), null));

        searchEmailToolId = Objects.requireNonNull(
            mcpToolRepository.save(new McpTool("searchEmail", Map.of(), mcpComponent.getId()))
                .getId());
        getEmailToolId = Objects.requireNonNull(
            mcpToolRepository.save(new McpTool("getEmail", Map.of(), mcpComponent.getId()))
                .getId());

        mcpIntegrationInstanceConfigurationId = fixture.insertMcpIntegrationInstanceConfiguration(mcpServer.getId(),
            integrationInstanceConfigurationId);

        sendEmailLinkId = fixture.insertLink(
            mcpIntegrationInstanceConfigurationId,
            fixture.insertConfigurationWorkflow(integrationInstanceConfigurationId, SEND_EMAIL_WORKFLOW_ID, true),
            true);

        fixture.insertIntegrationWorkflow(integrationId, 6, SEND_EMAIL_WORKFLOW_ID, SEND_EMAIL_WORKFLOW_UUID);
    }

    @AfterEach
    public void afterEach() {
        fixture.deleteAll();

        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    public void testAddMissingRowsCreatesToolAndWorkflowRows() {
        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, searchEmailToolId, true);

        mcpIntegrationInstanceRowFacade.addMissingRows(connectedUserId, mcpServer.getId());

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(McpIntegrationInstanceTool::getMcpToolId, McpIntegrationInstanceTool::isEnabled)
            .containsExactlyInAnyOrder(tuple(searchEmailToolId, true), tuple(getEmailToolId, true));
        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(
                McpIntegrationInstanceWorkflow::getMcpIntegrationInstanceConfigurationWorkflowId,
                McpIntegrationInstanceWorkflow::isEnabled)
            .containsExactly(tuple(sendEmailLinkId, true));
        assertThat(fixture.getInstanceWorkflowEnabled(integrationInstanceId, SEND_EMAIL_WORKFLOW_ID))
            .containsExactly(false);
    }

    @Test
    public void testAddMissingRowsKeepsSwitchedOffRow() {
        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, searchEmailToolId, true);
        mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
            integrationInstanceId, sendEmailLinkId, false);

        mcpIntegrationInstanceRowFacade.addMissingRows(connectedUserId, mcpServer.getId());

        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(McpIntegrationInstanceWorkflow::isEnabled)
            .containsExactly(false);
        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(McpIntegrationInstanceTool::getMcpToolId, McpIntegrationInstanceTool::isEnabled)
            .containsExactlyInAnyOrder(tuple(searchEmailToolId, true), tuple(getEmailToolId, true));
    }

    @Test
    public void testNewRowsDisabledWhenServerSwitchedOffForUser() {
        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, searchEmailToolId, false);

        mcpIntegrationInstanceRowFacade.addMissingRows(connectedUserId, mcpServer.getId());

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(McpIntegrationInstanceTool::getMcpToolId, McpIntegrationInstanceTool::isEnabled)
            .containsExactlyInAnyOrder(tuple(searchEmailToolId, false), tuple(getEmailToolId, false));
        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(McpIntegrationInstanceWorkflow::isEnabled)
            .containsExactly(false);
    }

    @Test
    public void testAddMissingRowsSkipsDisabledLink() {
        fixture.insertLink(
            mcpIntegrationInstanceConfigurationId,
            fixture.insertConfigurationWorkflow(integrationInstanceConfigurationId, ARCHIVE_EMAIL_WORKFLOW_ID, true),
            false);

        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, searchEmailToolId, true);

        mcpIntegrationInstanceRowFacade.addMissingRows(connectedUserId, mcpServer.getId());

        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(McpIntegrationInstanceWorkflow::getMcpIntegrationInstanceConfigurationWorkflowId)
            .containsExactly(sendEmailLinkId);
        assertThat(fixture.getInstanceWorkflowEnabled(integrationInstanceId, ARCHIVE_EMAIL_WORKFLOW_ID)).isEmpty();
    }

    @Test
    public void testAddMissingRowsForIntegrationInstanceConfigurationReachesUsersWithItsServers() {
        long archiveEmailConfigurationWorkflowId = fixture.insertConfigurationWorkflow(
            integrationInstanceConfigurationId, ARCHIVE_EMAIL_WORKFLOW_ID, false);
        long archiveEmailLinkId = fixture.insertLink(
            mcpIntegrationInstanceConfigurationId, archiveEmailConfigurationWorkflowId, true);

        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, searchEmailToolId, true);

        mcpIntegrationInstanceRowFacade.addMissingRows(connectedUserId, mcpServer.getId());

        jdbcTemplate.update(
            "UPDATE integration_instance_configuration_workflow SET enabled = true WHERE id = ?",
            archiveEmailConfigurationWorkflowId);

        mcpIntegrationInstanceRowFacade.addMissingRowsForIntegrationInstanceConfiguration(
            integrationInstanceConfigurationId);

        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(
                McpIntegrationInstanceWorkflow::getMcpIntegrationInstanceConfigurationWorkflowId,
                McpIntegrationInstanceWorkflow::isEnabled)
            .containsExactlyInAnyOrder(tuple(sendEmailLinkId, true), tuple(archiveEmailLinkId, true));
        assertThat(
            mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(otherIntegrationInstanceId))
                .isEmpty();
    }

    @Test
    public void testAddMissingRowsForConnectedUsersSkipsUsersWithoutTheServer() {
        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, searchEmailToolId, true);

        mcpIntegrationInstanceRowFacade.addMissingRowsForConnectedUsers(mcpServer.getId());

        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(McpIntegrationInstanceWorkflow::getMcpIntegrationInstanceConfigurationWorkflowId)
            .containsExactly(sendEmailLinkId);
        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(otherIntegrationInstanceId))
            .isEmpty();
        assertThat(
            mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(otherIntegrationInstanceId))
                .isEmpty();
        assertThat(mcpIntegrationInstanceRowFacade.getConnectedUserIds(mcpServer.getId()))
            .containsExactly(connectedUserId);
        assertThat(mcpIntegrationInstanceRowFacade.getMcpServerIds(otherConnectedUserId)).isEmpty();
    }

    @Test
    public void testAddMissingRowsForIntegrationInstance() {
        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, searchEmailToolId, true);

        long slackIntegrationInstanceConfigurationId = fixture.insertIntegrationInstanceConfiguration(
            fixture.insertIntegration("slack"), 1);

        long slackLinkId = fixture.insertLink(
            fixture.insertMcpIntegrationInstanceConfiguration(mcpServer.getId(),
                slackIntegrationInstanceConfigurationId),
            fixture.insertConfigurationWorkflow(slackIntegrationInstanceConfigurationId, ARCHIVE_EMAIL_WORKFLOW_ID,
                true),
            true);

        long slackIntegrationInstanceId = fixture.insertIntegrationInstance(
            slackIntegrationInstanceConfigurationId, connectedUserId, connectionId);

        mcpIntegrationInstanceRowFacade.addMissingRowsForIntegrationInstance(slackIntegrationInstanceId);

        assertThat(
            mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(slackIntegrationInstanceId))
                .extracting(
                    McpIntegrationInstanceWorkflow::getMcpIntegrationInstanceConfigurationWorkflowId,
                    McpIntegrationInstanceWorkflow::isEnabled)
                .containsExactly(tuple(slackLinkId, true));
        assertThat(fixture.getInstanceWorkflowEnabled(slackIntegrationInstanceId, ARCHIVE_EMAIL_WORKFLOW_ID))
            .containsExactly(false);
    }

    @Test
    public void testRemapKeepsEnabledStateOnTargetLink() {
        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, searchEmailToolId, true);
        mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
            integrationInstanceId, sendEmailLinkId, false);

        long targetIntegrationInstanceConfigurationId =
            fixture.insertIntegrationInstanceConfiguration(integrationId, 7);

        fixture.insertIntegrationWorkflow(integrationId, 7, SEND_EMAIL_WORKFLOW_V7_ID, SEND_EMAIL_WORKFLOW_UUID);

        long targetLinkId = fixture.insertLink(
            fixture.insertMcpIntegrationInstanceConfiguration(mcpServer.getId(),
                targetIntegrationInstanceConfigurationId),
            fixture.insertConfigurationWorkflow(targetIntegrationInstanceConfigurationId, SEND_EMAIL_WORKFLOW_V7_ID,
                true),
            true);

        jdbcTemplate.update(
            "UPDATE integration_instance SET integration_instance_configuration_id = ? WHERE id = ?",
            targetIntegrationInstanceConfigurationId, integrationInstanceId);

        mcpIntegrationInstanceRowFacade.remapIntegrationInstance(integrationInstanceId);

        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(
                McpIntegrationInstanceWorkflow::getMcpIntegrationInstanceConfigurationWorkflowId,
                McpIntegrationInstanceWorkflow::isEnabled)
            .containsExactly(tuple(targetLinkId, false));
    }

    @Test
    public void testProvisionEnablesExistingRowsAndNeverEnablesInstanceWorkflows() {
        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
            integrationInstanceId, searchEmailToolId, false);
        mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
            integrationInstanceId, sendEmailLinkId, false);

        fixture.insertInstanceWorkflow(integrationInstanceId, integrationInstanceConfigurationId,
            SEND_EMAIL_WORKFLOW_ID, false);

        mcpIntegrationInstanceRowFacade.provisionMcpServer(connectedUserId, mcpServer.getId());

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(McpIntegrationInstanceTool::getMcpToolId, McpIntegrationInstanceTool::isEnabled)
            .containsExactlyInAnyOrder(tuple(searchEmailToolId, true), tuple(getEmailToolId, true));
        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(McpIntegrationInstanceWorkflow::isEnabled)
            .containsExactly(true);
        assertThat(fixture.getInstanceWorkflowEnabled(integrationInstanceId, SEND_EMAIL_WORKFLOW_ID))
            .containsExactly(false);
    }

    @Test
    public void testAutomationOnlyUserGetsNewIntegrationRowsEnabled() {
        long linkWorkflowId = fixture.insertAutomationLinkWorkflow(mcpServer.getId(), AUTOMATION_WORKFLOW_UUID);

        fixture.insertAutomationRow(connectedUserId, linkWorkflowId, AUTOMATION_WORKFLOW_UUID, true);

        mcpIntegrationInstanceRowFacade.addMissingRows(connectedUserId, mcpServer.getId());

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(McpIntegrationInstanceTool::getMcpToolId, McpIntegrationInstanceTool::isEnabled)
            .containsExactlyInAnyOrder(tuple(searchEmailToolId, true), tuple(getEmailToolId, true));
        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(McpIntegrationInstanceWorkflow::isEnabled)
            .containsExactly(true);
    }

    @Test
    public void testAutomationOnlyUserWithDisabledRowGetsNewIntegrationRowsDisabled() {
        long linkWorkflowId = fixture.insertAutomationLinkWorkflow(mcpServer.getId(), AUTOMATION_WORKFLOW_UUID);

        fixture.insertAutomationRow(connectedUserId, linkWorkflowId, AUTOMATION_WORKFLOW_UUID, false);

        mcpIntegrationInstanceRowFacade.addMissingRows(connectedUserId, mcpServer.getId());

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(McpIntegrationInstanceTool::getMcpToolId, McpIntegrationInstanceTool::isEnabled)
            .containsExactlyInAnyOrder(tuple(searchEmailToolId, false), tuple(getEmailToolId, false));
        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(McpIntegrationInstanceWorkflow::isEnabled)
            .containsExactly(false);
    }

    @Test
    public void testEnabledRowsOnAnotherServerDoNotEnableNewRowsOnThisServer() {
        McpServer otherMcpServer = mcpServerRepository.save(
            new McpServer("Other", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        McpComponent otherMcpComponent = mcpComponentRepository.save(
            new McpComponent("gmail", 1, otherMcpServer.getId(), null));

        long otherToolId = Objects.requireNonNull(
            mcpToolRepository.save(new McpTool("listLabels", Map.of(), otherMcpComponent.getId()))
                .getId());

        mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(integrationInstanceId, otherToolId, true);

        long otherLinkWorkflowId =
            fixture.insertAutomationLinkWorkflow(otherMcpServer.getId(), AUTOMATION_WORKFLOW_UUID);

        fixture.insertAutomationRow(connectedUserId, otherLinkWorkflowId, AUTOMATION_WORKFLOW_UUID, true);

        mcpIntegrationInstanceRowFacade.addMissingRows(connectedUserId, mcpServer.getId());

        assertThat(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .extracting(McpIntegrationInstanceTool::getMcpToolId, McpIntegrationInstanceTool::isEnabled)
            .containsExactlyInAnyOrder(
                tuple(otherToolId, true), tuple(searchEmailToolId, false), tuple(getEmailToolId, false));
        assertThat(mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(McpIntegrationInstanceWorkflow::isEnabled)
            .containsExactly(false);
    }

    @Test
    public void testGetConnectedUserIdsIncludesAutomationOnlyUser() {
        long linkWorkflowId = fixture.insertAutomationLinkWorkflow(mcpServer.getId(), AUTOMATION_WORKFLOW_UUID);

        fixture.insertAutomationRow(otherConnectedUserId, linkWorkflowId, AUTOMATION_WORKFLOW_UUID, false);

        assertThat(mcpIntegrationInstanceRowFacade.getConnectedUserIds(mcpServer.getId()))
            .containsExactly(otherConnectedUserId);
        assertThat(mcpIntegrationInstanceRowFacade.getMcpServerIds(otherConnectedUserId))
            .containsExactly(mcpServer.getId());
    }

    @Configuration
    @EnableCaching
    @Import({
        IntegrationInstanceConfigurationServiceImpl.class, IntegrationInstanceConfigurationWorkflowServiceImpl.class,
        IntegrationInstanceServiceImpl.class, IntegrationInstanceWorkflowServiceImpl.class,
        IntegrationServiceImpl.class, IntegrationWorkflowServiceImpl.class, McpComponentServiceImpl.class,
        McpIntegrationInstanceConfigurationServiceImpl.class, McpIntegrationInstanceRowFacadeImpl.class,
        McpServerMembership.class
    })
    static class RowsIntTestConfiguration {
    }
}
