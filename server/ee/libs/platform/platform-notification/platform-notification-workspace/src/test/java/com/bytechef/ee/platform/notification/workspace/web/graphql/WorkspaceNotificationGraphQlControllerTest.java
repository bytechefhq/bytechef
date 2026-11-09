/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.workspace.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.notification.workspace.service.WorkspaceNotificationService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 * @author Ivica Cardic
 */
class WorkspaceNotificationGraphQlControllerTest {

    private AnnotationConfigApplicationContext applicationContext;
    private WorkspaceNotificationGraphQlController workspaceNotificationGraphQlController;
    private WorkspaceNotificationService workspaceNotificationService;

    @BeforeEach
    void beforeEach() {
        applicationContext = new AnnotationConfigApplicationContext(MethodSecurityTestConfiguration.class);

        workspaceNotificationGraphQlController = applicationContext.getBean(
            WorkspaceNotificationGraphQlController.class);
        workspaceNotificationService = applicationContext.getBean(WorkspaceNotificationService.class);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        applicationContext.close();
    }

    @Test
    void testWorkspaceNotificationsDeniesNonAdmin() {
        authenticate("ROLE_USER");

        assertThatThrownBy(() -> workspaceNotificationGraphQlController.workspaceNotifications(7L))
            .isInstanceOf(AccessDeniedException.class);

        verify(workspaceNotificationService, never()).getNotifications(7L);
    }

    @Test
    void testWorkspaceNotificationsAllowsAdmin() {
        when(workspaceNotificationService.getNotifications(7L)).thenReturn(List.of());

        authenticate("ROLE_ADMIN");

        assertThat(workspaceNotificationGraphQlController.workspaceNotifications(7L)).isEmpty();

        verify(workspaceNotificationService).getNotifications(7L);
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "password", AuthorityUtils.createAuthorityList(authority)));
    }

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityTestConfiguration {

        @Bean
        WorkspaceNotificationService workspaceNotificationService() {
            return mock(WorkspaceNotificationService.class);
        }

        @Bean
        WorkspaceNotificationGraphQlController workspaceNotificationGraphQlController(
            WorkspaceNotificationService workspaceNotificationService) {

            return new WorkspaceNotificationGraphQlController(workspaceNotificationService);
        }
    }
}
