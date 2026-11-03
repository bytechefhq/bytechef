/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.audit.McpProjectAuditEvents;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.security.McpProjectWorkspaceGuard;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectServiceImpl;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
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
    McpProjectServiceImpl.class, PostgreSQLContainerConfiguration.class
})
class McpProjectServiceAuditIntTest {

    private static final long MCP_PROJECT_ID = 7L;
    private static final long MCP_SERVER_ID = 2L;
    private static final long PROJECT_DEPLOYMENT_ID = 11L;

    @MockitoBean
    private McpProjectRepository mcpProjectRepository;

    @Autowired
    private McpProjectService mcpProjectService;

    @MockitoBean
    private McpProjectWorkspaceGuard mcpProjectWorkspaceGuard;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

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
    void testUpdateRecordsTheMcpProjectAndItsNewLinks() {
        McpProject mcpProject = new McpProject(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_ID, MCP_SERVER_ID);

        when(mcpProjectRepository.findById(MCP_PROJECT_ID)).thenReturn(Optional.of(mcpProject));
        when(mcpProjectRepository.save(any(McpProject.class))).thenReturn(mcpProject);

        mcpProjectService.update(new McpProject(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_ID, MCP_SERVER_ID));

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(McpProjectAuditEvents.MCP_PROJECT_UPDATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpProjectId", "7")
            .containsEntry("mcpServerId", "2")
            .containsEntry("projectDeploymentId", "11");
    }

    @Test
    void testUpdateAcrossWorkspacesRecordsAnError() {
        doThrow(new IllegalArgumentException("Deployment is not in the server workspace"))
            .when(mcpProjectWorkspaceGuard)
            .requireDeploymentInServerWorkspace(MCP_SERVER_ID, PROJECT_DEPLOYMENT_ID);

        assertThatThrownBy(
            () -> mcpProjectService.update(new McpProject(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_ID, MCP_SERVER_ID)))
                .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(McpProjectAuditEvents.MCP_PROJECT_UPDATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("mcpProjectId", "7");
    }

    private PersistentAuditEvent findSingleEvent() {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }
}
