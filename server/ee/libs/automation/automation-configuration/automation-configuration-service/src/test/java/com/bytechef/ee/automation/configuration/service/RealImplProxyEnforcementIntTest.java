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

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Verifies that the <em>real</em> production service implementations ({@link ProjectUserServiceImpl},
 * {@link WorkspaceUserServiceImpl}, {@link CustomRoleServiceImpl}) enforce {@code @PreAuthorize} through the Spring
 * proxy. Complements {@link PreAuthorizeProxyEnforcementIntTest} (which uses stand-in services that duplicate the SpEL
 * strings) by catching regressions that would leave the stand-in test green: e.g., someone removes the annotation from
 * the real impl, or introduces a {@code @Transactional(propagation = REQUIRES_NEW)} that breaks the proxy chain for the
 * real bean only.
 *
 * <p>
 * Collaborators are mocked to keep the test lightweight — only the proxy-enforcement path needs to fire. The assertions
 * here do not exercise business logic; they only prove the security interceptor runs before any service code executes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = RealImplProxyEnforcementIntTest.Config.class, properties = "bytechef.edition=ee")
class RealImplProxyEnforcementIntTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectUserService projectUserService;

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
    void testRealProjectUserServiceImplEnforcesAddProjectUser() {
        // If this test starts passing without an AccessDeniedException, the real ProjectUserServiceImpl lost its
        // @PreAuthorize on addProjectUser — the stand-in test in PreAuthorizeProxyEnforcementIntTest would stay green
        // because it exercises a different Spring bean.
        assertThatThrownBy(() -> projectUserService.addProjectUser(1L, 2L, ProjectRole.VIEWER.ordinal()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealProjectUserServiceImplEnforcesDeleteProjectUser() {
        assertThatThrownBy(() -> projectUserService.deleteProjectUser(1L, 2L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealProjectUserServiceImplEnforcesUpdateProjectUserRole() {
        assertThatThrownBy(
            () -> projectUserService.updateProjectUserRole(1L, 2L, ProjectRole.VIEWER.ordinal()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealProjectUserServiceImplEnforcesGetProjectUsers() {
        assertThatThrownBy(() -> projectUserService.getProjectUsers(1L))
            .isInstanceOf(AccessDeniedException.class);
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

    @Test
    void testRealCustomRoleServiceImplEnforcesCreateCustomRole() {
        assertThatThrownBy(
            () -> customRoleService.createCustomRole("r", "d", Set.of(PermissionScope.WORKFLOW_VIEW)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealCustomRoleServiceImplEnforcesDeleteCustomRole() {
        assertThatThrownBy(() -> customRoleService.deleteCustomRole(1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRealCustomRoleServiceImplEnforcesGetCustomRoles() {
        assertThatThrownBy(() -> customRoleService.getCustomRoles())
            .isInstanceOf(AccessDeniedException.class);
    }

    // @SpringBootConfiguration (not @TestConfiguration) because @SpringBootTest(classes = Config.class) requires a
    // primary Spring Boot configuration class; @TestConfiguration is a supplemental config and Spring Boot explicitly
    // rejects it as the primary ("Classes annotated with @TestConfiguration are not considered"). See the sibling
    // PreAuthorizeProxyEnforcementIntTest for the same reasoning.
    @SpringBootConfiguration
    @EnableMethodSecurity
    @Import({
        ProjectUserServiceImpl.class, WorkspaceUserServiceImpl.class, CustomRoleServiceImpl.class
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
        ProjectUserRepository projectUserRepository() {
            return mock(ProjectUserRepository.class);
        }

        @Bean
        ProjectRepository projectRepository() {
            return mock(ProjectRepository.class);
        }

        @Bean
        WorkspaceUserRepository workspaceUserRepository() {
            return mock(WorkspaceUserRepository.class);
        }

        @Bean
        CustomRoleRepository customRoleRepository() {
            return mock(CustomRoleRepository.class);
        }
    }
}
