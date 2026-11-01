/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.audit.WorkspaceConnectionAuditEvents;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacadeImpl;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
    PostgreSQLContainerConfiguration.class, WorkspaceConnectionAuditIntTest.WorkspaceConnectionFacadeConfiguration.class
})
class WorkspaceConnectionAuditIntTest {

    private static final long CONNECTION_ID = 42L;

    @MockitoBean
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDisconnectRecordsTheConnectionOnTheAutomationPlatform() {
        authenticate(AuthorityConstants.ADMIN);

        workspaceConnectionFacade.disconnectConnection(CONNECTION_ID);

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(WorkspaceConnectionAuditEvents.CONNECTION_DISCONNECTED);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID))
            .doesNotContainKey("dataError");
    }

    @Test
    void testFailedDisconnectIsRecordedAsAnError() {
        authenticate(AuthorityConstants.ADMIN);

        doThrow(new IllegalStateException("boom")).when(projectDeploymentWorkflowService)
            .deleteProjectDeploymentWorkflowConnection(CONNECTION_ID);

        assertThatThrownBy(() -> workspaceConnectionFacade.disconnectConnection(CONNECTION_ID))
            .isInstanceOf(IllegalStateException.class);

        assertThat(findSingle().getData())
            .containsEntry("result", "ERROR")
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID));
    }

    @Test
    void testDeniedDisconnectIsRecorded() {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> workspaceConnectionFacade.disconnectConnection(CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(findSingle().getData())
            .containsEntry("result", "DENIED")
            .containsEntry("connectionId", String.valueOf(CONNECTION_ID));
    }

    private static void authenticate(String authority) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private PersistentAuditEvent findSingle() {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    @EnableMethodSecurity
    static class WorkspaceConnectionFacadeConfiguration {

        @Bean
        WorkspaceConnectionFacade workspaceConnectionFacade(
            ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

            return new WorkspaceConnectionFacadeImpl(
                mock(ConnectionFacade.class), mock(ConnectionLifecycleFacade.class), projectDeploymentWorkflowService,
                mock(WorkflowTestConfigurationService.class), mock(WorkspaceConnectionService.class));
        }
    }
}
