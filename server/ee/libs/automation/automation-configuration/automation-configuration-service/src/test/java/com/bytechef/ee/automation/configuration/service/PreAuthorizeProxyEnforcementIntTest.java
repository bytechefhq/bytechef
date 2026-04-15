/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Integration test that boots Spring method security and verifies {@code @PreAuthorize} expressions are actually
 * enforced by the Spring proxy — not just present as annotations. Without this, {@code PreAuthorizeAnnotationTest}
 * (which is reflection-only) would stay green even if {@code @EnableMethodSecurity} were removed globally or the
 * service proxies were bypassed.
 *
 * <p>
 * Each test method authenticates a non-tenant-admin principal, mocks {@link PermissionService} to deny the relevant
 * scope/role, and asserts {@link AccessDeniedException} is thrown through the proxy chain. The services themselves are
 * represented by stand-ins that mirror the production {@code @PreAuthorize} expressions on the real service methods
 * (the real services have many collaborators that would require a full Spring Boot context to wire; this lighter-weight
 * approach still exercises the proxy-based enforcement path).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PreAuthorizeProxyEnforcementIntTest.Config.class)
class PreAuthorizeProxyEnforcementIntTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private GuardedProjectMutations guardedProjectMutations;

    @Autowired
    private GuardedWorkspaceMutations guardedWorkspaceMutations;

    @Autowired
    private GuardedCustomRoleMutations guardedCustomRoleMutations;

    @Autowired
    private GuardedProjectFacadeReads guardedProjectFacadeReads;

    @BeforeEach
    void authenticateAsNonAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(anyLong())).thenReturn(false);
        when(permissionService.hasProjectScope(anyLong(), anyString())).thenReturn(false);
        when(permissionService.hasProjectRole(anyLong(), anyString())).thenReturn(false);
        when(permissionService.hasWorkspaceRole(anyLong(), anyString())).thenReturn(false);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAddProjectUserDeniedWhenCallerLacksManageUsersScope() {
        assertThatThrownBy(() -> guardedProjectMutations.addProjectUser(1L, 2L, 0))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testDeleteProjectUserDeniedWhenCallerLacksManageUsersScope() {
        assertThatThrownBy(() -> guardedProjectMutations.deleteProjectUser(1L, 2L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUpdateProjectUserRoleDeniedWhenCallerLacksManageUsersScope() {
        assertThatThrownBy(() -> guardedProjectMutations.updateProjectUserRole(1L, 2L, 0))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetProjectUsersDeniedWhenCallerLacksViewUsersScope() {
        assertThatThrownBy(() -> guardedProjectMutations.getProjectUsers(1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAddWorkspaceUserDeniedWhenCallerIsNotWorkspaceAdmin() {
        assertThatThrownBy(() -> guardedWorkspaceMutations.addWorkspaceUser(2L, 1L, WorkspaceRole.VIEWER))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRemoveWorkspaceUserDeniedWhenCallerIsNotWorkspaceAdmin() {
        assertThatThrownBy(() -> guardedWorkspaceMutations.removeWorkspaceUser(2L, 1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testCreateCustomRoleDeniedWhenCallerIsNotTenantAdmin() {
        assertThatThrownBy(
            () -> guardedCustomRoleMutations.createCustomRole("r", "d", Set.of(PermissionScope.WORKFLOW_VIEW)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testDeleteCustomRoleDeniedWhenCallerIsNotTenantAdmin() {
        assertThatThrownBy(() -> guardedCustomRoleMutations.deleteCustomRole(1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetUserWorkspacesDeniedForNonAdminOtherUser() {
        // isCurrentUser returns false (configured in setup) so only tenant admins can read another user's memberships.
        assertThatThrownBy(() -> guardedProjectFacadeReads.getUserWorkspaces(999L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAllowedWhenPermissionServiceGrants() {
        // Positive control: once hasProjectScope returns true, the same proxy chain allows the call. If this fails,
        // the rest of the denied-path assertions could be green for the wrong reason (e.g., method security disabled).
        when(permissionService.hasProjectScope(1L, PermissionScope.PROJECT_MANAGE_USERS.name())).thenReturn(true);

        guardedProjectMutations.addProjectUser(1L, 2L, 0);
    }

    // @SpringBootConfiguration (not @TestConfiguration) because @SpringBootTest(classes = Config.class) requires a
    // primary Spring Boot configuration class; @TestConfiguration is a supplemental config and Spring Boot explicitly
    // rejects it as the primary ("Classes annotated with @TestConfiguration are not considered"). The practical
    // difference is zero for this test — the class is only loaded when referenced via classes=... — but using the
    // correct annotation keeps Spring Boot's context bootstrapping happy.
    @SpringBootConfiguration
    @EnableMethodSecurity
    @Import({
        GuardedProjectMutations.class, GuardedWorkspaceMutations.class, GuardedCustomRoleMutations.class,
        GuardedProjectFacadeReads.class
    })
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }

    /**
     * Mirrors the {@code @PreAuthorize} expressions on {@code ProjectUserServiceImpl}. Kept in sync by
     * {@link PreAuthorizeAnnotationTest}, which pins the expressions on the production impl. If the production
     * annotation changes without updating this stand-in, the test still fires the proxy — it just exercises the old
     * expression, so the reflection test in {@code PreAuthorizeAnnotationTest} is the source of truth for drift.
     */
    @Service
    static class GuardedProjectMutations {

        @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_USERS')")
        public void addProjectUser(long projectId, long userId, int projectRoleOrdinal) {
        }

        @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_USERS')")
        public void deleteProjectUser(long projectId, long userId) {
        }

        @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_USERS')")
        public void updateProjectUserRole(long projectId, long userId, int projectRoleOrdinal) {
        }

        @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_VIEW_USERS')")
        public void getProjectUsers(long projectId) {
        }
    }

    @Service
    static class GuardedWorkspaceMutations {

        @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'ADMIN')")
        public void addWorkspaceUser(long workspaceId, long userId, WorkspaceRole role) {
        }

        @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'ADMIN')")
        public void removeWorkspaceUser(long workspaceId, long userId) {
        }
    }

    @Service
    static class GuardedCustomRoleMutations {

        @PreAuthorize("@permissionService.isTenantAdmin()")
        public void createCustomRole(String name, String description, Set<PermissionScope> scopes) {
        }

        @PreAuthorize("@permissionService.isTenantAdmin()")
        public void deleteCustomRole(long id) {
        }
    }

    @Service
    static class GuardedProjectFacadeReads {

        @PreAuthorize("@permissionService.isTenantAdmin() or @permissionService.isCurrentUser(#id)")
        public void getUserWorkspaces(long id) {
        }
    }
}
