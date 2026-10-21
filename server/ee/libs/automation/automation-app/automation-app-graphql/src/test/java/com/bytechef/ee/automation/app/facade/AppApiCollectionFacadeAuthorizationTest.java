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
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_A;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.authenticate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.ee.automation.app.service.AppApiCollectionService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.workflow.AppApiCollectionResolver;
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
 * Runs the {@code @PreAuthorize} guards on {@link AppApiCollectionFacadeImpl} through real Spring method security; see
 * {@link AppAuthorizationTestConfiguration} for what is real and {@link AppAuthorizationFixture} for the tenant. Every
 * denial is paired with an allowance on the same guard.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppAuthorizationTestConfiguration.class, properties = "bytechef.edition=ee")
class AppApiCollectionFacadeAuthorizationTest {

    @Autowired
    private AppApiCollectionFacade appApiCollectionFacade;

    @Autowired
    private AppApiCollectionResolver appApiCollectionResolver;

    @Autowired
    private AppApiCollectionService appApiCollectionService;

    @Autowired
    private AppService appService;

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
        void testViewerCanGetApiCollectionBindings() {
            authenticate(VIEWER);

            assertThat(appApiCollectionFacade.getApiCollectionBindings(APP)).isEmpty();

            verify(appApiCollectionService).getBindings(APP);
        }

        @Test
        void testMemberOfAnotherWorkspaceIsDeniedGetApiCollectionBindings() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appApiCollectionFacade.getApiCollectionBindings(APP))
                .isInstanceOf(AccessDeniedException.class);

            verify(appApiCollectionService, never()).getBindings(anyLong());
        }
    }

    @Nested
    class AppManage {

        @Test
        void testViewerIsDeniedGetBindableApiCollections() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appApiCollectionFacade.getBindableApiCollections(APP))
                .isInstanceOf(AccessDeniedException.class);

            verify(appApiCollectionResolver, never()).getBindableApiCollections(anyLong(), any());
        }

        @Test
        void testWorkspaceAdminCanGetBindableApiCollections() {
            authenticate(WORKSPACE_ADMIN);

            assertThat(appApiCollectionFacade.getBindableApiCollections(APP)).isEmpty();

            verify(appApiCollectionResolver).getBindableApiCollections(WORKSPACE_A, Environment.PRODUCTION);
        }

        @Test
        void testViewerIsDeniedBindApiCollection() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appApiCollectionFacade.bindApiCollection(APP, 21L))
                .isInstanceOf(AccessDeniedException.class);

            verify(appApiCollectionService, never()).bind(anyLong(), anyLong());
        }

        @Test
        void testEditorIsDeniedBindApiCollection() {
            authenticate(EDITOR);

            assertThatThrownBy(() -> appApiCollectionFacade.bindApiCollection(APP, 21L))
                .isInstanceOf(AccessDeniedException.class);

            verify(appApiCollectionService, never()).bind(anyLong(), anyLong());
        }

        @Test
        void testWorkspaceAdminPassesTheBindApiCollectionGuard() {
            authenticate(WORKSPACE_ADMIN);

            assertThatThrownBy(() -> appApiCollectionFacade.bindApiCollection(APP, 21L))
                .isNotInstanceOf(AccessDeniedException.class);

            verify(appApiCollectionResolver).getBindableApiCollections(WORKSPACE_A, Environment.PRODUCTION);
        }

        @Test
        void testViewerIsDeniedUnbindApiCollection() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appApiCollectionFacade.unbindApiCollection(APP, 21L))
                .isInstanceOf(AccessDeniedException.class);

            verify(appApiCollectionService, never()).unbind(anyLong(), anyLong());
        }

        @Test
        void testWorkspaceAdminCanUnbindApiCollection() {
            authenticate(WORKSPACE_ADMIN);

            appApiCollectionFacade.unbindApiCollection(APP, 21L);

            verify(appApiCollectionService).unbind(APP, 21L);
        }
    }
}
