/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import java.util.List;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PreAuthorizeProxyEnforcementIntTest.Config.class, properties = "bytechef.edition=ee")
class PreAuthorizeProxyEnforcementIntTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private GuardedProjectMutations guardedProjectMutations;

    @Autowired
    private GuardedProjectFacadeReads guardedProjectFacadeReads;

    @Autowired
    private GuardedResourceOwnerReads guardedResourceOwnerReads;

    @Autowired
    private WorkspaceUserService workspaceUserService;

    @BeforeEach
    void authenticateAsNonAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        // Re-established each test so the positive control's specific true-stub does not leak across methods (the
        // PermissionService mock is shared via the cached Spring context). Every gate defaults to denied here.
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.isCurrentUser(anyLong())).thenReturn(false);
        when(permissionService.isResourceOwner(anyString(), anyLong())).thenReturn(false);
        when(permissionService.hasResourceScope(any(), anyString(), anyString())).thenReturn(false);
        when(permissionService.hasWorkspaceScope(anyLong(), anyString())).thenReturn(false);
        when(permissionService.hasWorkspaceScopeForProject(anyLong(), anyString())).thenReturn(false);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteProjectDeniedWhenCallerLacksProjectScope() {
        // 'Project' annotations now route through permissionService.hasWorkspaceScopeForProject(projectId, scope).
        // The proxy must deny when that check returns false (configured in setup).
        assertThatThrownBy(() -> guardedProjectMutations.deleteProject(1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetProjectDeniedWhenCallerLacksProjectScope() {
        assertThatThrownBy(() -> guardedProjectMutations.getProject(1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetUserWorkspacesDeniedForNonAdminOtherUser() {
        // isCurrentUser returns false (configured in setup) so only tenant admins can read another user's memberships.
        assertThatThrownBy(() -> guardedProjectFacadeReads.getUserWorkspaces(999L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetResourceDeniedWhenCallerIsNotResourceOwner() {
        // isResourceOwner returns false (configured in setup) so the isResourceOwner(#id, 'Type') built-in denies.
        assertThatThrownBy(() -> guardedResourceOwnerReads.getApiKey(9L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAllowedWhenPermissionServiceGrants() {
        // Positive control: once hasResourceScope grants, the same proxy chain allows the call. If this fails, the
        // rest of the denied-path assertions could be green for the wrong reason (e.g., method security disabled).
        when(permissionService.hasResourceScope(1L, "Project", "WORKFLOW_VIEW")).thenReturn(true);

        guardedProjectMutations.getProject(1L);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesAddWorkspaceUser() {
        assertThatThrownBy(() -> workspaceUserService.addWorkspaceUser(2L, 1L, WorkspaceRole.VIEWER))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesRemoveWorkspaceUser() {
        assertThatThrownBy(() -> workspaceUserService.removeWorkspaceUser(2L, 1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesUpdateWorkspaceUserRole() {
        assertThatThrownBy(
            () -> workspaceUserService.updateWorkspaceUserRole(2L, 1L, WorkspaceRole.VIEWER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealWorkspaceUserServiceImplEnforcesGetWorkspaceWorkspaceUsers() {
        assertThatThrownBy(() -> workspaceUserService.getWorkspaceWorkspaceUsers(1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    // @SpringBootConfiguration (not @TestConfiguration) because @SpringBootTest(classes = Config.class) requires a
    // primary Spring Boot configuration class; @TestConfiguration is a supplemental config and Spring Boot explicitly
    // rejects it as the primary ("Classes annotated with @TestConfiguration are not considered"). The synthetic
    // Guarded* stand-ins and the real WorkspaceUserServiceImpl share one context; the mocked PermissionService backs
    // both.
    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        GuardedProjectMutations.class, GuardedProjectFacadeReads.class, GuardedResourceOwnerReads.class,
        WorkspaceUserServiceImpl.class
    })
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        WorkspaceUserRepository workspaceUserRepository() {
            return mock(WorkspaceUserRepository.class);
        }

        @Bean
        WorkspaceUserAuditPublisher workspaceUserAuditPublisher() {
            return mock(WorkspaceUserAuditPublisher.class);
        }
    }

    /**
     * Mirrors the {@code 'Project'} {@code @PreAuthorize} expressions on the project facade/service impls. The
     * evaluator routes the {@code 'Project'} targetType to
     * {@code permissionService.hasWorkspaceScopeForProject(projectId, scope)}, which the mocked
     * {@link PermissionService} stubs. Kept in sync by {@link PreAuthorizeAnnotationTest}, which pins the expressions
     * on the production impls. If the production annotation changes without updating this stand-in, the test still
     * fires the proxy — it just exercises the old expression, so the reflection test in
     * {@code PreAuthorizeAnnotationTest} is the source of truth for drift.
     */
    @Service
    static class GuardedProjectMutations {

        @PreAuthorize("hasPermission(#projectId, 'Project', 'PROJECT_DELETE')")
        public void deleteProject(long projectId) {
        }

        @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')")
        public void getProject(long projectId) {
        }
    }

    @Service
    static class GuardedProjectFacadeReads {

        @PreAuthorize("isTenantAdmin() or isCurrentUser(#id)")
        public void getUserWorkspaces(long id) {
        }
    }

    @Service
    static class GuardedResourceOwnerReads {

        @PreAuthorize("isResourceOwner(#id, 'ApiKey')")
        public void getApiKey(long id) {
        }
    }
}
