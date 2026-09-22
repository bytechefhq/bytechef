/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserServiceImpl;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.aspect.AuditedMethodValidator;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The production {@code WorkspaceUserServiceImpl}, its {@code WorkspaceUserAuditMapper} and both audit aspects wired
 * together, with persistence mocked: proves each {@code @Audited} call produces exactly one correctly shaped event.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkspaceUserAuditIntTest.Config.class, properties = "bytechef.edition=ee")
class WorkspaceUserAuditIntTest {

    private static final long USER_ID = 2L;
    private static final long WORKSPACE_ID = 1L;
    private static final String WORKSPACE_MEMBER_MANAGE = "WORKSPACE_MEMBER_MANAGE";

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    @Autowired
    private WorkspaceUserService workspaceUserService;

    @BeforeEach
    void beforeEach() {
        reset(auditEventService, permissionService, workspaceUserRepository);

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(workspaceUserRepository.save(any(WorkspaceUser.class))).then(AdditionalAnswers.returnsFirstArg());
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRoleUpdateRecordsPreviousAndNewRole() {
        when(permissionService.hasResourceScope(WORKSPACE_ID, "Workspace", WORKSPACE_MEMBER_MANAGE))
            .thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER)));

        workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("workspaceId", "1")
            .containsEntry("userId", "2")
            .containsEntry("previousRole", "VIEWER")
            .containsEntry("role", "EDITOR");
    }

    @Test
    void testDeniedUpdateRecordsDeniedAndNeverTouchesTheRepository() {
        when(permissionService.hasResourceScope(WORKSPACE_ID, "Workspace", WORKSPACE_MEMBER_MANAGE))
            .thenReturn(false);

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(workspaceUserRepository);

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "DENIED")
            .doesNotContainKey("previousRole");
    }

    @Test
    void testAddRecordsExactlyOneRow() {
        when(permissionService.hasResourceScope(WORKSPACE_ID, "Workspace", WORKSPACE_MEMBER_MANAGE))
            .thenReturn(true);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID)).thenReturn(Optional.empty());

        workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER);

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("workspaceId", "1")
            .containsEntry("userId", "2")
            .containsEntry("role", "VIEWER");
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
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditedMethodValidator.class, AuditMapperResolver.class,
        WorkspaceUserAuditMapper.class, WorkspaceUserServiceImpl.class
    })
    static class Config {

        @Bean
        AuditEventService auditEventService() {
            return mock(AuditEventService.class);
        }

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        WorkspaceUserRepository workspaceUserRepository() {
            return mock(WorkspaceUserRepository.class);
        }
    }
}
