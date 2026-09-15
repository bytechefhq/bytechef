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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.user.service.UserInvitationService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The same proxy enforcement as {@link PreAuthorizeProxyEnforcementIntTest}, but with no synthetic stand-ins: the
 * production {@code WorkspaceUserServiceImpl} and {@code CustomRoleServiceImpl} are the beans under test, so a guard
 * deleted from either one fails here.
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
    classes = RealImplProxyEnforcementIntTest.Config.class, properties = "bytechef.edition=ee")
class RealImplProxyEnforcementIntTest {

    private static final long CUSTOM_ROLE_ID = 3L;
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
    private WorkspaceUserService workspaceUserService;

    @Autowired
    private CustomRoleService customRoleService;

    @BeforeEach
    void authenticateAsNonAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        // Reset, not just re-stub: the mocks are singletons in the cached context, so a verify() would otherwise count
        // the invocations of every test that ran before it, and a positive control's stubbing would survive into the
        // next test's denial assertion.
        reset(permissionService, customRoleRepository, workspaceUserRepository);

        // Only the methods the production annotations route to: hasWorkspaceScopeInEveryEnvironment for the
        // workspace-wide member writes, hasResourceScope for the 'Workspace' token, isTenantAdmin for the tenant-global
        // custom-role mutations.
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.hasResourceScope(any(), anyString(), anyString())).thenReturn(false);
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(anyLong(), anyString())).thenReturn(false);
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
    void testRealWorkspaceUserServiceImplEnforcesUpdateWorkspaceUserRole() {
        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER))
                .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
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

    @Test
    void testRealCustomRoleServiceImplEnforcesCreateCustomRole() {
        assertThatThrownBy(
            () -> customRoleService.createCustomRole("r", "d", Set.of("WORKFLOW_VIEW")))
                .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testRealCustomRoleServiceImplAllowsCreateCustomRoleForATenantAdmin() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        // Reaching validateScopeNames — which rejects the name against an empty registry — is what proves the tenant
        // admin got past the gate.
        assertThatThrownBy(
            () -> customRoleService.createCustomRole("r", "d", Set.of("WORKFLOW_VIEW")))
                .isNotInstanceOf(AccessDeniedException.class);

        verify(permissionService).isTenantAdmin();
    }

    @Test
    void testRealCustomRoleServiceImplEnforcesDeleteCustomRole() {
        assertThatThrownBy(() -> customRoleService.deleteCustomRole(1L))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testRealCustomRoleServiceImplEnforcesGetCustomRoles() {
        assertThatThrownBy(() -> customRoleService.getCustomRoles(null))
            .isInstanceOf(AccessDeniedException.class);

        // The null-workspaceId tier is tenant-admin-only, and SpEL short-circuits the second disjunct because
        // '#workspaceId != null' is false — so isTenantAdmin is the only check that may run.
        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testRealCustomRoleServiceImplAllowsGetCustomRolesForATenantAdmin() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        assertThat(customRoleService.getCustomRoles(null)).isEmpty();

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    /**
     * The other tier of the same read: a workspace member manager populating the assignment picker. It must route
     * through the {@code 'Workspace'} token rather than through {@code isTenantAdmin()}, or the picker would be empty
     * for exactly the people who need it.
     */
    @Test
    void testRealCustomRoleServiceImplEnforcesGetCustomRolesForAWorkspace() {
        assertThatThrownBy(() -> customRoleService.getCustomRoles(WORKSPACE_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasResourceScope(WORKSPACE_ID, "Workspace", MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testRealCustomRoleServiceImplAllowsGetCustomRolesForAWorkspaceMemberManager() {
        when(permissionService.hasResourceScope(WORKSPACE_ID, "Workspace", MEMBER_MANAGE)).thenReturn(true);

        assertThat(customRoleService.getCustomRoles(WORKSPACE_ID)).isEmpty();

        verify(permissionService).hasResourceScope(WORKSPACE_ID, "Workspace", MEMBER_MANAGE);
        verifyNoMoreInteractions(permissionService);
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

        @Bean
        WorkspaceUserAuditPublisher workspaceUserAuditPublisher() {
            return mock(WorkspaceUserAuditPublisher.class);
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
}
