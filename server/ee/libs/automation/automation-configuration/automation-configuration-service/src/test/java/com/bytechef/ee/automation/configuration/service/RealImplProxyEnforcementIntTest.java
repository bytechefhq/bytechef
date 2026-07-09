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

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.CustomRoleAuditPublisher;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import java.util.List;
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
        when(permissionService.hasWorkspaceScope(anyLong(), anyString())).thenReturn(false);
        when(permissionService.hasWorkspaceScopeForProject(anyLong(), anyString())).thenReturn(false);
        when(permissionService.hasWorkspaceRole(anyLong(), anyString())).thenReturn(false);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
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
            () -> customRoleService.createCustomRole("r", "d", Set.of("WORKFLOW_VIEW")))
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

        @Bean
        CustomRoleAuditPublisher customRoleAuditPublisher() {
            return mock(CustomRoleAuditPublisher.class);
        }
    }
}
