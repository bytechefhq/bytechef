/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.audit.McpProjectAuditEvents;
import com.bytechef.automation.ai.mcp.audit.McpProjectAuditMapper;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacadeImpl;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.mcp.service.McpServerService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = McpProjectAuditIntTest.Config.class, properties = "bytechef.edition=ee")
@MockitoBean(types = {
    ComponentConnectionFacade.class, ConnectionService.class, McpProjectService.class,
    McpProjectWorkflowService.class, McpServerService.class, ProjectDeploymentFacade.class,
    ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class, ProjectService.class,
    ProjectWorkflowService.class, WorkflowService.class, WorkflowTestConfigurationService.class,
    WorkspaceMcpServerService.class
})
class McpProjectAuditIntTest {

    private static final long MCP_PROJECT_ID = 7L;
    private static final long MCP_SERVER_ID = 2L;
    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long PROJECT_ID = 3L;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private McpProjectFacade mcpProjectFacade;

    @Autowired
    private McpProjectService mcpProjectService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectService projectService;

    @BeforeEach
    void beforeEach() {
        reset(auditEventService);

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteRecordsTheMcpProjectId() {
        when(mcpProjectService.fetchMcpProject(MCP_PROJECT_ID))
            .thenReturn(Optional.of(new McpProject(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_ID, MCP_SERVER_ID)));

        mcpProjectFacade.deleteMcpProject(MCP_PROJECT_ID);

        verify(projectDeploymentFacade).deleteProjectDeployment(PROJECT_DEPLOYMENT_ID);

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(McpProjectAuditEvents.MCP_PROJECT_DELETED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpProjectId", "7");
    }

    @Test
    void testFailedCreateThroughTheShortOverloadRecordsExactlyOneError() {
        when(projectService.getProject(PROJECT_ID)).thenThrow(new IllegalArgumentException("Project not found"));

        assertThatThrownBy(() -> mcpProjectFacade.createMcpProject(MCP_SERVER_ID, PROJECT_ID, 1, List.of("workflow1")))
            .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(McpProjectAuditEvents.MCP_PROJECT_CREATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("mcpServerId", "2")
            .containsEntry("projectId", "3")
            .containsEntry("projectVersion", "1")
            .doesNotContainKey("mcpProjectId");
    }

    @SuppressWarnings("unchecked")
    private PersistentAuditEvent captureSavedEvent() {
        ArgumentCaptor<List<PersistentAuditEvent>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(auditEventService).saveAll(argumentCaptor.capture());

        List<PersistentAuditEvent> persistentAuditEvents = argumentCaptor.getValue();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditMapperResolver.class, McpProjectAuditMapper.class,
        McpProjectFacadeImpl.class
    })
    static class Config {

        @Bean
        AuditEventService auditEventService() {
            return mock(AuditEventService.class);
        }
    }
}
