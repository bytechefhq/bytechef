/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.EDITOR;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.OUTSIDER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.PRODUCTION_VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_A;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.authenticate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.service.AppWorkflowService;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the {@code @PreAuthorize} guards on {@link AppWorkflowFacadeImpl} through real Spring method security; see
 * {@link AppAuthorizationTestConfiguration} for what is real and {@link AppAuthorizationFixture} for the tenant. Every
 * denial is paired with an allowance on the same guard.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppAuthorizationTestConfiguration.class, properties = "bytechef.edition=ee")
class AppWorkflowFacadeAuthorizationTest {

    @Autowired
    private AppCallableWorkflowResolver appCallableWorkflowResolver;

    @Autowired
    private AppService appService;

    @Autowired
    private AppWorkflowFacade appWorkflowFacade;

    @Autowired
    private AppWorkflowService appWorkflowService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    @BeforeEach
    void beforeEach() {
        AppAuthorizationFixture.stubTenant(
            appService, connectionService, userService, workspaceConnectionRepository, workspaceUserRepository);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class AppView {

        @Test
        void testViewerCanGetWorkflowBindings() {
            authenticate(VIEWER);

            assertThat(appWorkflowFacade.getWorkflowBindings(APP)).isEmpty();

            verify(appWorkflowService).getBindings(APP);
        }

        @Test
        void testMemberOfAnotherWorkspaceIsDeniedGetWorkflowBindings() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appWorkflowFacade.getWorkflowBindings(APP))
                .isInstanceOf(AccessDeniedException.class);

            verify(appWorkflowService, never()).getBindings(anyLong());
        }
    }

    @Nested
    class AppManage {

        @Test
        void testViewerIsDeniedBindWorkflow() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appWorkflowFacade.bindWorkflow(APP, "sync", 30L, "workflow-uuid"))
                .isInstanceOf(AccessDeniedException.class);

            verify(appWorkflowService, never()).bind(anyLong(), anyString(), anyLong(), anyString());
        }

        @Test
        void testEditorIsDeniedBindWorkflow() {
            authenticate(EDITOR);

            assertThatThrownBy(() -> appWorkflowFacade.bindWorkflow(APP, "sync", 30L, "workflow-uuid"))
                .isInstanceOf(AccessDeniedException.class);

            verify(appWorkflowService, never()).bind(anyLong(), anyString(), anyLong(), anyString());
        }

        @Test
        void testViewerIsDeniedUnbindWorkflow() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appWorkflowFacade.unbindWorkflow(APP, "sync"))
                .isInstanceOf(AccessDeniedException.class);

            verify(appWorkflowService, never()).unbind(anyLong(), anyString());
        }

        @Test
        void testWorkspaceAdminCanUnbindWorkflow() {
            authenticate(WORKSPACE_ADMIN);

            appWorkflowFacade.unbindWorkflow(APP, "sync");

            verify(appWorkflowService).unbind(APP, "sync");
        }

        @Test
        void testViewerIsDeniedGetBindableWorkflows() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appWorkflowFacade.getBindableWorkflows(APP))
                .isInstanceOf(AccessDeniedException.class);

            verify(appCallableWorkflowResolver, never()).getBindableWorkflows(anyLong(), any());
        }

        @Test
        void testWorkspaceAdminCanGetBindableWorkflows() {
            authenticate(WORKSPACE_ADMIN);

            assertThat(appWorkflowFacade.getBindableWorkflows(APP)).isEmpty();

            verify(appCallableWorkflowResolver).getBindableWorkflows(WORKSPACE_A, Environment.PRODUCTION);
        }
    }

    @Nested
    class DeploymentView {

        @Test
        void testViewerCanGetCallableWorkflows() {
            authenticate(VIEWER);

            assertThat(appWorkflowFacade.getCallableWorkflows(WORKSPACE_A, null)).isEmpty();
        }

        /**
         * The guard pins DEVELOPMENT: a member who is only a viewer in PRODUCTION still lists callable workflows,
         * because the listing reads DEVELOPMENT deployments, where they are an admin.
         */
        @Test
        void testProductionViewerCanGetCallableWorkflowsBecauseTheGuardIsDevelopment() {
            authenticate(PRODUCTION_VIEWER);

            assertThat(appWorkflowFacade.getCallableWorkflows(WORKSPACE_A, null)).isEmpty();
        }

        @Test
        void testMemberOfAnotherWorkspaceIsDeniedGetCallableWorkflows() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appWorkflowFacade.getCallableWorkflows(WORKSPACE_A, null))
                .isInstanceOf(AccessDeniedException.class);
        }
    }
}
