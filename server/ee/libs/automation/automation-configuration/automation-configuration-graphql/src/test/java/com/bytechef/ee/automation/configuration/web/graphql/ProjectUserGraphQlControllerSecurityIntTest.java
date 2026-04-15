/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
import com.bytechef.ee.automation.configuration.service.ProjectUserService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
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
 * Verifies that {@link ProjectUserGraphQlController} mutations are gated by the controller-level {@code @PreAuthorize}.
 * The controller annotations are defense-in-depth on top of the service-layer guards (which are pinned in
 * {@code PreAuthorizeAnnotationTest}) \u2014 this test catches regressions where the controller mapping loses the proxy
 * chain or where someone removes the controller annotation believing the service-layer guard is sufficient.
 * Service-layer mutations would still fire an AccessDeniedException, but only AFTER the controller has resolved
 * arguments and started the call frame, so audit events would show the controller method as ALLOWED followed by the
 * service method as DENIED \u2014 confusing for investigators.
 *
 * <p>
 * The controller is invoked directly via Spring proxy rather than through the GraphQL HTTP pipeline so the test stays
 * focused on the security boundary without depending on a full GraphQL transport stack.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectUserGraphQlControllerSecurityIntTest.Config.class,
    properties = {
        "bytechef.edition=ee", "bytechef.coordinator.enabled=true"
    })
class ProjectUserGraphQlControllerSecurityIntTest {

    @Autowired
    private ProjectUserGraphQlController controller;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectUserService projectUserService;

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
    void testUpdateProjectUserRoleDeniedWhenCallerLacksManageUsersScope() {
        assertThatThrownBy(() -> controller.updateProjectUserRole(1L, 2L, ProjectRole.EDITOR))
            .isInstanceOf(AccessDeniedException.class);

        // The service must NOT be invoked when controller-level security denies. If it was invoked, the gate fired
        // too late \u2014 the controller frame had already started and audit events would be misleading.
        verify(projectUserService, never()).updateProjectUserRole(anyLong(), anyLong(), anyInt());
    }

    @Test
    void testAddProjectUserDeniedWhenCallerLacksManageUsersScope() {
        assertThatThrownBy(() -> controller.addProjectUser(1L, 2L, ProjectRole.VIEWER))
            .isInstanceOf(AccessDeniedException.class);

        verify(projectUserService, never()).addProjectUser(anyLong(), anyLong(), anyInt());
    }

    @Test
    void testRemoveProjectUserDeniedWhenCallerLacksManageUsersScope() {
        assertThatThrownBy(() -> controller.removeProjectUser(1L, 2L))
            .isInstanceOf(AccessDeniedException.class);

        verify(projectUserService, never()).deleteProjectUser(anyLong(), anyLong());
    }

    @Test
    void testUpdateProjectUserRoleAllowedWhenCallerHoldsManageUsersScope() {
        // Positive control: with the scope granted, the controller-level guard passes and the call reaches the
        // service. Without this assertion, the denied tests above could be green for the wrong reason (e.g., method
        // security disabled \u2014 every call would fall through silently).
        when(permissionService.hasProjectScope(1L, "PROJECT_MANAGE_USERS")).thenReturn(true);

        controller.updateProjectUserRole(1L, 2L, ProjectRole.EDITOR);

        verify(projectUserService).updateProjectUserRole(1L, 2L, ProjectRole.EDITOR.ordinal());
    }

    // @SpringBootConfiguration (not @TestConfiguration): Spring ignores @TestConfiguration when it is passed
    // directly via @SpringBootTest(classes = ...), so the context bootstrapper reported "Unable to find a
    // @SpringBootConfiguration". @SpringBootConfiguration is a @Configuration specialization that Spring will
    // accept as the primary source.
    @SpringBootConfiguration
    @EnableMethodSecurity
    @Import({
        ProjectUserGraphQlController.class
    })
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        ProjectUserService projectUserService() {
            return mock(ProjectUserService.class);
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }
    }
}
