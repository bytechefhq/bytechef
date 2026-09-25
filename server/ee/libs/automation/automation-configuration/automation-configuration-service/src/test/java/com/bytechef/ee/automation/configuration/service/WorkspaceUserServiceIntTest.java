/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.CustomRoleAuditMapper;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvents;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditMapper;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.aspect.AuditedMethodValidator;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserInvitationService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import org.springframework.test.context.NestedTestConfiguration;

/**
 * The same proxy enforcement as {@link PreAuthorizeProxyEnforcementIntTest}, but with no synthetic stand-ins: the
 * production {@code WorkspaceUserServiceImpl} is the bean under test, so a guard deleted from it fails here.
 * <p>
 * Each guard is asserted in both directions and the exact {@link PermissionService} call is verified. A deny-only
 * assertion against an unstubbed mock cannot tell "the named check refused" from "no gate named that check": Mockito
 * answers {@code false} for every boolean method it was never told about, so such an assertion stays green when the
 * annotation names a method nothing stubs — the defect this class exists to catch.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = WorkspaceUserServiceIntTest.Config.class, properties = "bytechef.edition=ee")
class WorkspaceUserServiceIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long CUSTOM_ROLE_ID = 3L;
    private static final String EMAIL = "someone@example.com";
    private static final String MEMBER_MANAGE = "WORKSPACE_MEMBER_MANAGE";
    private static final long USER_ID = 2L;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private CustomRoleRepository customRoleRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceUserService workspaceUserService;

    @BeforeEach
    void authenticateAsNonAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        // Reset, not just re-stub: the mocks are singletons in the cached context, so a verify() would otherwise count
        // the invocations of every test that ran before it, and a positive control's stubbing would survive into the
        // next test's denial assertion.
        reset(permissionService, customRoleRepository, workspaceService, workspaceUserRepository);

        // Only the methods the production annotations route to: hasWorkspaceScopeInEveryEnvironment for the
        // workspace-wide member writes, hasResourceScope for the 'Workspace' token, isTenantAdmin for the tenant-global
        // custom-role mutations.
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.hasResourceScope(any(), anyString(), anyString())).thenReturn(false);
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(anyLong(), anyString())).thenReturn(false);
        when(workspaceService.workspaceExists(anyLong())).thenReturn(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesAddWorkspaceUser() {
        assertThatThrownBy(() -> workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testRealWorkspaceUserServiceImplAllowsAddWorkspaceUserWhenTheScopeIsGranted() {
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE)).thenReturn(true);

        workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER);

        // Past the proxy: the eviction is the body's, so its presence is what separates "granted" from "refused
        // everybody".
        verify(permissionService).evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesRemoveWorkspaceUser() {
        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testRealWorkspaceUserServiceImplAllowsRemoveWorkspaceUserWhenTheScopeIsGranted() {
        grantMemberManagementInEveryEnvironment();

        when(workspaceUserRepository.lockWorkspace(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));

        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(USER_ID, WORKSPACE_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesUpdateWorkspaceUserRole() {
        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER))
                .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testRealWorkspaceUserServiceImplAllowsUpdateWorkspaceUserRoleWhenTheScopeIsGranted() {
        grantMemberManagementInEveryEnvironment();

        when(workspaceUserRepository.lockWorkspace(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));

        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesAddWorkspaceUserWithACustomRole() {
        assertThatThrownBy(
            () -> workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, null, CUSTOM_ROLE_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
        verifyNoInteractions(workspaceService);
    }

    @Test
    void testRealWorkspaceUserServiceImplAllowsAddWorkspaceUserWithACustomRoleWhenTheScopeIsGranted() {
        grantMemberManagementInEveryEnvironment();

        when(workspaceService.workspaceExists(WORKSPACE_ID)).thenThrow(new IllegalStateException(BODY_REACHED));

        assertThatThrownBy(
            () -> workspaceUserService.addWorkspaceUser(USER_ID, WORKSPACE_ID, null, CUSTOM_ROLE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesInviteWorkspaceUser() {
        assertThatThrownBy(() -> workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, EMAIL, WorkspaceRole.VIEWER))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
        verifyNoInteractions(workspaceService);
    }

    @Test
    void testRealWorkspaceUserServiceImplAllowsInviteWorkspaceUserWhenTheScopeIsGranted() {
        grantMemberManagementInEveryEnvironment();

        when(workspaceService.workspaceExists(WORKSPACE_ID)).thenThrow(new IllegalStateException(BODY_REACHED));

        assertThatThrownBy(() -> workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, EMAIL, WorkspaceRole.VIEWER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesInviteWorkspaceUserWithACustomRole() {
        assertThatThrownBy(() -> workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, EMAIL, null, CUSTOM_ROLE_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
        verifyNoInteractions(workspaceService);
    }

    @Test
    void testRealWorkspaceUserServiceImplAllowsInviteWorkspaceUserWithACustomRoleWhenTheScopeIsGranted() {
        grantMemberManagementInEveryEnvironment();

        when(workspaceService.workspaceExists(WORKSPACE_ID)).thenThrow(new IllegalStateException(BODY_REACHED));

        assertThatThrownBy(() -> workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, EMAIL, null, CUSTOM_ROLE_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesSetEnvironmentRole() {
        assertThatThrownBy(
            () -> workspaceUserService.setEnvironmentRole(
                USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER, null))
                    .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, MEMBER_MANAGE, Environment.PRODUCTION);
        verifyNoMoreInteractions(permissionService);
        verifyNoInteractions(workspaceService);
    }

    @Test
    void testRealWorkspaceUserServiceImplAllowsSetEnvironmentRoleWhenTheScopeIsGrantedInThatEnvironment() {
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, MEMBER_MANAGE, Environment.PRODUCTION))
            .thenReturn(true);
        when(workspaceService.workspaceExists(WORKSPACE_ID)).thenThrow(new IllegalStateException(BODY_REACHED));

        assertThatThrownBy(
            () -> workspaceUserService.setEnvironmentRole(
                USER_ID, WORKSPACE_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(BODY_REACHED);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, MEMBER_MANAGE, Environment.PRODUCTION);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesRemoveEnvironmentRole() {
        assertThatThrownBy(
            () -> workspaceUserService.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
                .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, MEMBER_MANAGE, Environment.PRODUCTION);
        verifyNoMoreInteractions(permissionService);
        verifyNoInteractions(workspaceUserRepository);
    }

    @Test
    void testRealWorkspaceUserServiceImplAllowsRemoveEnvironmentRoleWhenTheScopeIsGrantedInThatEnvironment() {
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, MEMBER_MANAGE, Environment.PRODUCTION))
            .thenReturn(true);
        when(workspaceUserRepository.lockWorkspace(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));

        assertThatThrownBy(
            () -> workspaceUserService.removeEnvironmentRole(USER_ID, WORKSPACE_ID, Environment.PRODUCTION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, MEMBER_MANAGE, Environment.PRODUCTION);
    }

    /**
     * Assigning a custom role changes a member's effective scopes as thoroughly as a built-in role change does, and the
     * role may carry every management scope. Its guard was pinned by nothing before this: deleting the annotation left
     * every test in the module green.
     */
    @Test
    void testRealWorkspaceUserServiceImplEnforcesAssignCustomRole() {
        assertThatThrownBy(() -> workspaceUserService.assignCustomRole(USER_ID, WORKSPACE_ID, CUSTOM_ROLE_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testRealWorkspaceUserServiceImplAllowsAssignCustomRoleWhenTheScopeIsGranted() {
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE)).thenReturn(true);

        // The caller also holds the role's scopes; granting a role that carries more than the caller holds is refused.
        when(permissionService.hasWorkspaceScope(anyLong(), anyString(), any(Environment.class))).thenReturn(true);

        stubAnAssignableCustomRoleAndMembership();

        WorkspaceUser workspaceUser = workspaceUserService.assignCustomRole(USER_ID, WORKSPACE_ID, CUSTOM_ROLE_ID);

        // The write itself, past the proxy. Asserting merely that the body threw something other than
        // AccessDeniedException would make this red the day assignCustomRole legitimately stops throwing — a failure
        // that says nothing about the gate.
        assertThat(workspaceUser.getCustomRoleId()).isEqualTo(CUSTOM_ROLE_ID);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verify(workspaceUserRepository).save(workspaceUser);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesGetWorkspaceWorkspaceUsers() {
        assertThatThrownBy(() -> workspaceUserService.getWorkspaceWorkspaceUsers(WORKSPACE_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW");
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testRealWorkspaceUserServiceImplAllowsGetWorkspaceWorkspaceUsersWhenTheScopeIsGranted() {
        when(permissionService.hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW")).thenReturn(true);

        workspaceUserService.getWorkspaceWorkspaceUsers(WORKSPACE_ID);

        verify(permissionService).hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW");
        verifyNoMoreInteractions(permissionService);
    }

    private void grantMemberManagementInEveryEnvironment() {
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE)).thenReturn(true);
    }

    /**
     * A present custom role and an existing workspace-wide membership on a non-admin built-in role, so the body of
     * {@code assignCustomRole} runs to its write instead of failing on a lookup. The role is deliberately not an
     * {@code ADMIN} one: that would pull in the last-admin check, which is a different guard.
     */
    private void stubAnAssignableCustomRoleAndMembership() {
        when(customRoleRepository.findById(CUSTOM_ROLE_ID))
            .thenReturn(Optional.of(new CustomRole("Auditor", Set.of("WORKFLOW_VIEW"))));
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER.ordinal())));
        when(workspaceUserRepository.save(any(WorkspaceUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // @SpringBootConfiguration (not @TestConfiguration) because @SpringBootTest(classes = Config.class) requires a
    // primary Spring Boot configuration class; @TestConfiguration is a supplemental config and Spring Boot explicitly
    // rejects it as the primary ("Classes annotated with @TestConfiguration are not considered"). See the sibling
    // PreAuthorizeProxyEnforcementIntTest for the same reasoning.
    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        WorkspaceUserServiceImpl.class, CustomRoleServiceImpl.class
    })
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        CustomRoleScopeResolver customRoleScopeResolver() {
            return mock(CustomRoleScopeResolver.class);
        }

        @Bean
        PermissionScopeRegistry permissionScopeRegistry() {
            return mock(PermissionScopeRegistry.class);
        }

        @Bean
        WorkspaceUserRepository workspaceUserRepository() {
            return mock(WorkspaceUserRepository.class);
        }

        @Bean
        CustomRoleRepository customRoleRepository() {
            return mock(CustomRoleRepository.class);
        }

        // The remaining WorkspaceUserServiceImpl collaborators, mocked like the ones above — a denial fires the
        // @PreAuthorize check before any of them is touched, and the positive controls only need Mockito's defaults.
        @Bean
        UserInvitationService userInvitationService() {
            return mock(UserInvitationService.class);
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        WorkspaceService workspaceService() {
            WorkspaceService workspaceService = mock(WorkspaceService.class);

            // Every workspace the enforcement tests name exists. The membership writes check existence before they
            // insert, so an unstubbed mock would fail the allow-direction tests with WORKSPACE_NOT_FOUND and hide
            // whether the proxy admitted the call at all.
            when(workspaceService.workspaceExists(anyLong())).thenReturn(true);

            return workspaceService;
        }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(classes = Audit.Config.class, properties = "bytechef.edition=ee")
    class Audit {

        private static final long USER_ID = 2L;
        private static final long WORKSPACE_ID = 1L;

        @Autowired
        private AuditEventService auditEventService;

        @Autowired
        private CustomRoleRepository customRoleRepository;

        @Autowired
        private CustomRoleService customRoleService;

        @Autowired
        private PermissionScopeRegistry permissionScopeRegistry;

        @Autowired
        private PermissionService permissionService;

        @Autowired
        private UserInvitationService userInvitationService;

        @Autowired
        private UserService userService;

        @Autowired
        private WorkspaceUserRepository workspaceUserRepository;

        @Autowired
        private WorkspaceUserService workspaceUserService;

        @BeforeEach
        void beforeEach() {
            reset(
                auditEventService, customRoleRepository, permissionScopeRegistry, permissionService,
                userInvitationService, userService, workspaceUserRepository);

            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            when(workspaceUserRepository.save(any(WorkspaceUser.class))).then(AdditionalAnswers.returnsFirstArg());
            when(permissionScopeRegistry.getScopeNames(any(WorkspaceRole.class))).thenReturn(Set.of());
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testRoleUpdateRecordsPreviousAndNewRole() {
            grantMemberManagement();

            when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
                .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER)));

            workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

            PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

            assertThat(persistentAuditEvent.getEventType())
                .isEqualTo(WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED);
            assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
            assertThat(persistentAuditEvent.getData())
                .containsEntry("result", "SUCCESS")
                .containsEntry("workspaceId", "1")
                .containsEntry("userId", "2")
                .containsEntry("previousRole", "VIEWER")
                .containsEntry("role", "EDITOR");
        }

        @Test
        void testDeniedCallRecordsDeniedAndNeverTouchesTheRepository() {
            when(permissionService.hasWorkspaceScopeInEveryEnvironment(anyLong(), anyString())).thenReturn(false);

            assertThatThrownBy(
                () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(workspaceUserRepository);

            PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

            assertThat(persistentAuditEvent.getEventType())
                .isEqualTo(WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED);
            assertThat(persistentAuditEvent.getData())
                .containsEntry("result", "DENIED")
                .containsEntry("role", "ADMIN")
                .doesNotContainKey("previousRole");
        }

        @Test
        void testInviteWritesExactlyOneRowWithoutTheEmail() {
            grantMemberManagement();

            User invitedUser = mock(User.class);

            when(invitedUser.getId()).thenReturn(9L);
            when(userService.fetchUserByEmail("someone@example.com")).thenReturn(Optional.empty());
            when(userInvitationService.inviteUser(anyString(), anyString())).thenReturn(invitedUser);

            workspaceUserService.inviteWorkspaceUser(WORKSPACE_ID, "someone@example.com", WorkspaceRole.VIEWER);

            PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

            assertThat(persistentAuditEvent.getEventType()).isEqualTo(WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED);
            assertThat(persistentAuditEvent.getData())
                .containsEntry("userId", "9")
                .containsEntry("invited", "true")
                .containsEntry("role", "VIEWER");
            assertThat(persistentAuditEvent.getData()
                .values()).noneMatch(value -> value.contains("@"));
        }

        private void grantMemberManagement() {
            when(permissionService.hasWorkspaceScopeInEveryEnvironment(anyLong(), anyString())).thenReturn(true);
            when(permissionService.hasWorkspaceScope(anyLong(), anyString(), any(Environment.class))).thenReturn(true);
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
            CustomRoleAuditMapper.class, CustomRoleServiceImpl.class, WorkspaceUserAuditMapper.class,
            WorkspaceUserServiceImpl.class
        })
        static class Config {

            @Bean
            AuditEventService auditEventService() {
                return mock(AuditEventService.class);
            }

            @Bean
            CustomRoleRepository customRoleRepository() {
                return mock(CustomRoleRepository.class);
            }

            @Bean("permissionService")
            PermissionService permissionService() {
                return mock(PermissionService.class);
            }

            @Bean
            PermissionScopeRegistry permissionScopeRegistry() {
                return mock(PermissionScopeRegistry.class);
            }

            @Bean
            UserInvitationService userInvitationService() {
                return mock(UserInvitationService.class);
            }

            @Bean
            UserService userService() {
                return mock(UserService.class);
            }

            @Bean
            WorkspaceService workspaceService() {
                WorkspaceService workspaceService = mock(WorkspaceService.class);

                when(workspaceService.workspaceExists(anyLong())).thenReturn(true);

                return workspaceService;
            }

            @Bean
            WorkspaceUserRepository workspaceUserRepository() {
                return mock(WorkspaceUserRepository.class);
            }
        }
    }
}
