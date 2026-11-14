/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doReturn;

import com.bytechef.ee.embedded.ai.mcp.config.EmbeddedMcpIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.config.McpRowsIntTestFixture;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.ai.mcp.event.EmbeddedMcpServerBeforeDeleteEventListener;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.service.McpComponentServiceImpl;
import java.util.List;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EmbeddedMcpIntTestConfiguration.class, McpAutomationRowsIntTest.AutomationRowsIntTestConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true"
    })
public class McpAutomationRowsIntTest {

    private static final String ARCHIVE_WORKFLOW_UUID = "6eed0000-0000-0000-0000-00000000bbbb";
    private static final String OTHER_WORKFLOW_UUID = "6eed0000-0000-0000-0000-00000000dddd";
    private static final String SEND_WORKFLOW_UUID = "6eed0000-0000-0000-0000-00000000aaaa";

    @Autowired
    private Encryption encryption;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService;

    @Autowired
    private McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService;

    @Autowired
    private McpAutomationRows mcpAutomationRows;

    @MockitoSpyBean
    private McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    private long automationProjectConfigurationId;
    private long connectedUserId;
    private McpRowsIntTestFixture fixture;
    private long linkId;
    private McpServer mcpServer;
    private long projectId;
    private long sendLinkWorkflowId;

    @BeforeEach
    public void beforeEach() {
        fixture = new McpRowsIntTestFixture(encryption, jdbcTemplate);

        mcpServer = mcpServerRepository.save(new McpServer("Google", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        connectedUserId = fixture.insertConnectedUser("user-1");

        projectId = fixture.insertAutomationProject();
        automationProjectConfigurationId = fixture.insertAutomationProjectConfiguration(projectId);

        linkId = createLink(mcpServer.getId(), automationProjectConfigurationId);
        sendLinkWorkflowId = createLinkWorkflow(linkId, SEND_WORKFLOW_UUID);
    }

    @AfterEach
    public void afterEach() {
        fixture.deleteAll();

        mcpServerRepository.deleteAll();
    }

    @Test
    public void testLinkCreatedAddsRowsForActivatedUsersOnly() {
        long notActivatedConnectedUserId = fixture.insertConnectedUser("user-2");
        long danglingConnectedUserId = fixture.insertConnectedUser("user-3");
        long notAssignedConnectedUserId = fixture.insertConnectedUser("user-4");

        fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);
        fixture.insertAssignment(notActivatedConnectedUserId, projectId, automationProjectConfigurationId);
        fixture.insertAssignment(danglingConnectedUserId, projectId, automationProjectConfigurationId);

        long referenceId = fixture.insertReference(connectedUserId, projectId, SEND_WORKFLOW_UUID, false);

        fixture.insertReference(notActivatedConnectedUserId, projectId, OTHER_WORKFLOW_UUID, false);
        fixture.insertReference(danglingConnectedUserId, projectId, SEND_WORKFLOW_UUID, true);
        fixture.insertReference(notAssignedConnectedUserId, projectId, SEND_WORKFLOW_UUID, false);

        mcpAutomationRows.addMissingRowsForLink(linkId);
        mcpAutomationRows.addMissingRowsForLink(linkId);

        assertThat(mcpConnectedUserProjectWorkflowService.getMcpServerMcpConnectedUserProjectWorkflows(
            mcpServer.getId()))
                .extracting(
                    McpConnectedUserProjectWorkflow::getConnectedUserProjectWorkflowId,
                    McpConnectedUserProjectWorkflow::getMcpAutomationProjectConfigurationWorkflowId,
                    McpConnectedUserProjectWorkflow::isEnabled)
                .containsExactly(tuple(referenceId, sendLinkWorkflowId, true));
    }

    @Test
    public void testReferenceCreatedAddsEnabledRowOnServerUserWasNotOn() {
        McpServer otherMcpServer = mcpServerRepository.save(
            new McpServer("Other", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        long otherSendLinkWorkflowId = createLinkWorkflow(
            createLink(otherMcpServer.getId(), automationProjectConfigurationId), SEND_WORKFLOW_UUID);

        createLinkWorkflow(linkId, ARCHIVE_WORKFLOW_UUID);

        fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);

        long referenceId = fixture.insertReference(connectedUserId, projectId, SEND_WORKFLOW_UUID, false);

        mcpAutomationRows.addMissingRowsForReference(referenceId);

        assertThat(mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(
            connectedUserId))
                .extracting(
                    McpConnectedUserProjectWorkflow::getConnectedUserProjectWorkflowId,
                    McpConnectedUserProjectWorkflow::getMcpAutomationProjectConfigurationWorkflowId,
                    McpConnectedUserProjectWorkflow::isEnabled)
                .containsExactlyInAnyOrder(
                    tuple(referenceId, sendLinkWorkflowId, true), tuple(referenceId, otherSendLinkWorkflowId, true));
    }

    @Test
    public void testReferenceCreatedToleratesRowInsertedConcurrently() {
        McpServer otherMcpServer = mcpServerRepository.save(
            new McpServer("Other", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        long otherSendLinkWorkflowId = createLinkWorkflow(
            createLink(otherMcpServer.getId(), automationProjectConfigurationId), SEND_WORKFLOW_UUID);

        fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);

        long referenceId = fixture.insertReference(connectedUserId, projectId, SEND_WORKFLOW_UUID, false);

        mcpConnectedUserProjectWorkflowService.create(referenceId, sendLinkWorkflowId, true);

        doReturn(Optional.empty()).when(mcpConnectedUserProjectWorkflowService)
            .fetch(referenceId, sendLinkWorkflowId);

        mcpAutomationRows.addMissingRowsForReference(referenceId);

        assertThat(mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(
            connectedUserId))
                .extracting(
                    McpConnectedUserProjectWorkflow::getConnectedUserProjectWorkflowId,
                    McpConnectedUserProjectWorkflow::getMcpAutomationProjectConfigurationWorkflowId,
                    McpConnectedUserProjectWorkflow::isEnabled)
                .containsExactlyInAnyOrder(
                    tuple(referenceId, sendLinkWorkflowId, true), tuple(referenceId, otherSendLinkWorkflowId, true));
    }

    @Test
    public void testReferenceCreatedFollowsUserStateOnServer() {
        long otherLinkWorkflowId = fixture.insertAutomationLinkWorkflow(mcpServer.getId(), OTHER_WORKFLOW_UUID);

        fixture.insertAutomationRow(connectedUserId, otherLinkWorkflowId, OTHER_WORKFLOW_UUID, false);
        fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);

        long referenceId = fixture.insertReference(connectedUserId, projectId, SEND_WORKFLOW_UUID, false);

        mcpAutomationRows.addMissingRowsForReference(referenceId);

        assertThat(getRows(sendLinkWorkflowId))
            .extracting(McpConnectedUserProjectWorkflow::isEnabled)
            .containsExactly(false);
    }

    @Test
    public void testReferenceCreatedWithoutAssignmentAddsNoRow() {
        long referenceId = fixture.insertReference(connectedUserId, projectId, SEND_WORKFLOW_UUID, false);

        mcpAutomationRows.addMissingRowsForReference(referenceId);

        assertThat(mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(
            connectedUserId)).isEmpty();
    }

    @Test
    public void testProvisionCreatesAndEnablesRows() {
        long archiveLinkWorkflowId = createLinkWorkflow(linkId, ARCHIVE_WORKFLOW_UUID);

        fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);

        long sendReferenceId = fixture.insertReference(connectedUserId, projectId, SEND_WORKFLOW_UUID, false);
        long archiveReferenceId = fixture.insertReference(connectedUserId, projectId, ARCHIVE_WORKFLOW_UUID, false);

        mcpConnectedUserProjectWorkflowService.create(sendReferenceId, sendLinkWorkflowId, false);

        mcpAutomationRows.provisionMcpServer(connectedUserId, mcpServer.getId());

        assertThat(mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(
            connectedUserId))
                .extracting(
                    McpConnectedUserProjectWorkflow::getConnectedUserProjectWorkflowId,
                    McpConnectedUserProjectWorkflow::getMcpAutomationProjectConfigurationWorkflowId,
                    McpConnectedUserProjectWorkflow::isEnabled)
                .containsExactlyInAnyOrder(
                    tuple(sendReferenceId, sendLinkWorkflowId, true),
                    tuple(archiveReferenceId, archiveLinkWorkflowId, true));
    }

    @Test
    public void testRemapOnMoveKeepsEnabled() {
        long targetAutomationProjectConfigurationId = fixture.insertAutomationProjectConfiguration(projectId);

        long targetSendLinkWorkflowId = createLinkWorkflow(
            createLink(mcpServer.getId(), targetAutomationProjectConfigurationId), SEND_WORKFLOW_UUID);

        fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);

        long referenceId = fixture.insertReference(connectedUserId, projectId, SEND_WORKFLOW_UUID, false);

        mcpConnectedUserProjectWorkflowService.create(referenceId, sendLinkWorkflowId, false);

        moveAssignment(targetAutomationProjectConfigurationId);

        mcpAutomationRows.remapConnectedUser(
            connectedUserId, automationProjectConfigurationId, targetAutomationProjectConfigurationId);

        assertThat(mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(
            connectedUserId))
                .extracting(
                    McpConnectedUserProjectWorkflow::getConnectedUserProjectWorkflowId,
                    McpConnectedUserProjectWorkflow::getMcpAutomationProjectConfigurationWorkflowId,
                    McpConnectedUserProjectWorkflow::isEnabled)
                .containsExactly(tuple(referenceId, targetSendLinkWorkflowId, false));
    }

    @Test
    public void testRemapOnMoveKeepsDisabledServerStateForNewWorkflows() {
        long targetAutomationProjectConfigurationId = fixture.insertAutomationProjectConfiguration(projectId);

        long targetArchiveLinkWorkflowId = createLinkWorkflow(
            createLink(mcpServer.getId(), targetAutomationProjectConfigurationId), ARCHIVE_WORKFLOW_UUID);

        fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);

        long sendReferenceId = fixture.insertReference(connectedUserId, projectId, SEND_WORKFLOW_UUID, false);
        long archiveReferenceId = fixture.insertReference(connectedUserId, projectId, ARCHIVE_WORKFLOW_UUID, false);

        mcpConnectedUserProjectWorkflowService.create(sendReferenceId, sendLinkWorkflowId, false);

        moveAssignment(targetAutomationProjectConfigurationId);

        mcpAutomationRows.remapConnectedUser(
            connectedUserId, automationProjectConfigurationId, targetAutomationProjectConfigurationId);

        assertThat(mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(
            connectedUserId))
                .extracting(
                    McpConnectedUserProjectWorkflow::getConnectedUserProjectWorkflowId,
                    McpConnectedUserProjectWorkflow::getMcpAutomationProjectConfigurationWorkflowId,
                    McpConnectedUserProjectWorkflow::isEnabled)
                .containsExactly(tuple(archiveReferenceId, targetArchiveLinkWorkflowId, false));
    }

    @Test
    public void testRemapOnMoveWithoutTargetLinkRemovesRows() {
        long targetAutomationProjectConfigurationId = fixture.insertAutomationProjectConfiguration(projectId);

        McpServer otherMcpServer = mcpServerRepository.save(
            new McpServer("Other", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        createLinkWorkflow(createLink(otherMcpServer.getId(), targetAutomationProjectConfigurationId),
            SEND_WORKFLOW_UUID);

        long unrelatedLinkWorkflowId = fixture.insertAutomationLinkWorkflow(mcpServer.getId(), OTHER_WORKFLOW_UUID);

        fixture.insertAutomationRow(connectedUserId, unrelatedLinkWorkflowId, OTHER_WORKFLOW_UUID, true);
        fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);

        long referenceId = fixture.insertReference(connectedUserId, projectId, SEND_WORKFLOW_UUID, false);

        mcpConnectedUserProjectWorkflowService.create(referenceId, sendLinkWorkflowId, false);

        moveAssignment(targetAutomationProjectConfigurationId);

        mcpAutomationRows.remapConnectedUser(
            connectedUserId, automationProjectConfigurationId, targetAutomationProjectConfigurationId);

        assertThat(getRows(sendLinkWorkflowId)).isEmpty();
        assertThat(getRows(unrelatedLinkWorkflowId))
            .extracting(McpConnectedUserProjectWorkflow::isEnabled)
            .containsExactly(true);
        assertThat(mcpConnectedUserProjectWorkflowService.getMcpServerMcpConnectedUserProjectWorkflows(
            otherMcpServer.getId()))
                .extracting(
                    McpConnectedUserProjectWorkflow::getConnectedUserProjectWorkflowId,
                    McpConnectedUserProjectWorkflow::isEnabled)
                .containsExactly(tuple(referenceId, true));
    }

    @Test
    public void testServerDeleteRemovesLinksAndRows() {
        fixture.insertAssignment(connectedUserId, projectId, automationProjectConfigurationId);

        long referenceId = fixture.insertReference(connectedUserId, projectId, SEND_WORKFLOW_UUID, false);

        mcpConnectedUserProjectWorkflowService.create(referenceId, sendLinkWorkflowId, true);

        mcpServerRepository.deleteById(mcpServer.getId());

        assertThat(mcpAutomationProjectConfigurationService.fetch(linkId)).isEmpty();
        assertThat(mcpAutomationProjectConfigurationWorkflowService.fetch(sendLinkWorkflowId)).isEmpty();
        assertThat(mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(
            connectedUserId)).isEmpty();
    }

    private long createLink(long mcpServerId, long automationProjectConfigurationId) {
        McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
            mcpAutomationProjectConfigurationService.create(
                new McpAutomationProjectConfiguration(automationProjectConfigurationId, mcpServerId));

        return mcpAutomationProjectConfiguration.getId();
    }

    private long createLinkWorkflow(long linkId, String workflowUuid) {
        McpAutomationProjectConfigurationWorkflow linkWorkflow =
            mcpAutomationProjectConfigurationWorkflowService.create(linkId, workflowUuid);

        return linkWorkflow.getId();
    }

    private List<McpConnectedUserProjectWorkflow> getRows(long linkWorkflowId) {
        return mcpConnectedUserProjectWorkflowService.getConnectedUserMcpConnectedUserProjectWorkflows(connectedUserId)
            .stream()
            .filter(row -> row.getMcpAutomationProjectConfigurationWorkflowId() == linkWorkflowId)
            .toList();
    }

    private void moveAssignment(long targetAutomationProjectConfigurationId) {
        jdbcTemplate.update(
            """
                UPDATE connected_user_project_configuration SET automation_project_configuration_id = ?
                WHERE connected_user_id = ? AND project_id = ?
                """,
            targetAutomationProjectConfigurationId, connectedUserId, projectId);
    }

    @Configuration
    @EnableCaching
    @Import({
        ConnectedUserProjectConfigurationServiceImpl.class, ConnectedUserProjectServiceImpl.class,
        ConnectedUserProjectWorkflowServiceImpl.class, EmbeddedMcpServerBeforeDeleteEventListener.class,
        IntegrationInstanceServiceImpl.class, McpAutomationRowsImpl.class, McpComponentServiceImpl.class,
        McpIntegrationInstanceConfigurationServiceImpl.class, McpServerMembership.class
    })
    static class AutomationRowsIntTestConfiguration {
    }
}
