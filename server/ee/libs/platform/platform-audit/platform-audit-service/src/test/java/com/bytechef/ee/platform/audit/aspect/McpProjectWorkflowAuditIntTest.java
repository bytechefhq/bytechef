/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.audit.McpProjectAuditEvents;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.McpProjectWorkflowFacade;
import com.bytechef.automation.ai.mcp.facade.McpProjectWorkflowFacadeImpl;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacadeImpl;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.audit.McpToolAuditEvents;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    McpProjectWorkflowFacadeImpl.class, PostgreSQLContainerConfiguration.class, WorkspaceMcpServerFacadeImpl.class
})
@MockitoBean(types = {
    McpComponentService.class, McpProjectFacade.class, McpProjectService.class, McpProjectWorkflowService.class,
    McpServerFacade.class, McpServerService.class, McpToolService.class, ProjectDeploymentWorkflowService.class,
    TagService.class, WorkspaceMcpServerService.class
})
class McpProjectWorkflowAuditIntTest {

    private static final long MCP_COMPONENT_ID = 4L;
    private static final long MCP_PROJECT_ID = 7L;
    private static final long MCP_PROJECT_WORKFLOW_ID = 13L;
    private static final long MCP_SERVER_ID = 2L;
    private static final long MCP_TOOL_ID = 21L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 17L;

    @Autowired
    private McpComponentService mcpComponentService;

    @Autowired
    private McpProjectWorkflowFacade mcpProjectWorkflowFacade;

    @Autowired
    private McpProjectWorkflowService mcpProjectWorkflowService;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private McpToolService mcpToolService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private WorkspaceMcpServerFacade workspaceMcpServerFacade;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateRecordsTheWorkflowItsProjectAndItsDeploymentWorkflow() {
        when(mcpProjectWorkflowService.create(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(newMcpProjectWorkflow());

        mcpProjectWorkflowFacade.createMcpProjectWorkflow(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(McpProjectAuditEvents.MCP_PROJECT_WORKFLOW_CREATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpProjectWorkflowId", String.valueOf(MCP_PROJECT_WORKFLOW_ID))
            .containsEntry("mcpProjectId", String.valueOf(MCP_PROJECT_ID))
            .containsEntry("projectDeploymentWorkflowId", String.valueOf(PROJECT_DEPLOYMENT_WORKFLOW_ID));
    }

    @Test
    void testDeleteRecordsTheWorkflow() {
        when(mcpProjectWorkflowService.fetchMcpProjectWorkflow(MCP_PROJECT_WORKFLOW_ID))
            .thenReturn(Optional.of(newMcpProjectWorkflow()));

        mcpProjectWorkflowFacade.deleteMcpProjectWorkflow(MCP_PROJECT_WORKFLOW_ID);

        verify(projectDeploymentWorkflowService).delete(PROJECT_DEPLOYMENT_WORKFLOW_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(McpProjectAuditEvents.MCP_PROJECT_WORKFLOW_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpProjectWorkflowId", String.valueOf(MCP_PROJECT_WORKFLOW_ID));
    }

    @Test
    void testDeleteOfAnUnknownWorkflowRecordsAnError() {
        when(mcpProjectWorkflowService.fetchMcpProjectWorkflow(MCP_PROJECT_WORKFLOW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcpProjectWorkflowFacade.deleteMcpProjectWorkflow(MCP_PROJECT_WORKFLOW_ID))
            .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(McpProjectAuditEvents.MCP_PROJECT_WORKFLOW_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("mcpProjectWorkflowId", String.valueOf(MCP_PROJECT_WORKFLOW_ID));
    }

    @Test
    void testUpdateEnabledRecordsTheWorkflowItsProjectAndTheNewState() {
        when(mcpProjectWorkflowService.updateEnabled(MCP_PROJECT_WORKFLOW_ID, false))
            .thenReturn(newMcpProjectWorkflow());

        mcpProjectWorkflowFacade.updateMcpProjectWorkflowEnabled(MCP_PROJECT_WORKFLOW_ID, false);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(McpProjectAuditEvents.MCP_PROJECT_WORKFLOW_ENABLED_UPDATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpProjectWorkflowId", String.valueOf(MCP_PROJECT_WORKFLOW_ID))
            .containsEntry("mcpProjectId", String.valueOf(MCP_PROJECT_ID))
            .containsEntry("enabled", "false");
    }

    @Test
    void testUpdateWorkspaceMcpToolEnabledRecordsTheToolItsComponentAndTheNewState() {
        McpTool mcpTool = new McpTool(MCP_TOOL_ID, "slack_send_message", Map.of(), MCP_COMPONENT_ID);

        McpComponent mcpComponent = new McpComponent("slack", 1, MCP_SERVER_ID, null);

        mcpComponent.setId(MCP_COMPONENT_ID);

        when(mcpToolService.fetchMcpTool(MCP_TOOL_ID)).thenReturn(Optional.of(mcpTool));
        when(mcpComponentService.getMcpComponent(MCP_COMPONENT_ID)).thenReturn(mcpComponent);
        when(mcpServerService.getMcpServer(MCP_SERVER_ID))
            .thenReturn(new McpServer("automation", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        workspaceMcpServerFacade.updateWorkspaceMcpToolEnabled(MCP_TOOL_ID, false);

        verify(mcpToolService).updateEnabled(MCP_TOOL_ID, false);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(McpToolAuditEvents.MCP_TOOL_ENABLED_UPDATED);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpToolId", String.valueOf(MCP_TOOL_ID))
            .containsEntry("mcpComponentId", String.valueOf(MCP_COMPONENT_ID))
            .containsEntry("name", "slack_send_message")
            .containsEntry("enabled", "false");
    }

    @Test
    void testUpdateWorkspaceMcpToolEnabledOfAnUnknownToolRecordsAnError() {
        when(mcpToolService.fetchMcpTool(MCP_TOOL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceMcpServerFacade.updateWorkspaceMcpToolEnabled(MCP_TOOL_ID, true))
            .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(McpToolAuditEvents.MCP_TOOL_ENABLED_UPDATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("mcpToolId", String.valueOf(MCP_TOOL_ID))
            .containsEntry("enabled", "true")
            .doesNotContainKeys("mcpComponentId", "name");
    }

    private static McpProjectWorkflow newMcpProjectWorkflow() {
        McpProjectWorkflow mcpProjectWorkflow = new McpProjectWorkflow(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID);

        mcpProjectWorkflow.setId(MCP_PROJECT_WORKFLOW_ID);

        return mcpProjectWorkflow;
    }

    private PersistentAuditEvent findSingleEvent() {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }
}
