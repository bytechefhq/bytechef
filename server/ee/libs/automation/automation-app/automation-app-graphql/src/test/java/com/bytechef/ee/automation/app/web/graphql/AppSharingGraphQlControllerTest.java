/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.facade.AppSharingFacade;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 */
class AppSharingGraphQlControllerTest {

    private static final long APP_ID = 10L;
    private static final long APP_INSTANCE_ID = 30L;
    private static final long USER_ID = 7L;
    private static final long WORKSPACE_ID = 1L;

    private AppSharingFacade appSharingFacade;
    private AppSharingGraphQlController controller;

    @BeforeEach
    void beforeEach() {
        appSharingFacade = mock(AppSharingFacade.class);

        controller = new AppSharingGraphQlController(appSharingFacade);
    }

    @Test
    void testControllerOwnsNoAuthorization() {
        for (Method method : AppSharingGraphQlController.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
        }
    }

    @Test
    void testAppGrantsDelegates() {
        when(appSharingFacade.getAppGrants(WORKSPACE_ID, APP_ID)).thenReturn(List.of(USER_ID));

        assertThat(controller.appGrants(WORKSPACE_ID, APP_ID)).containsExactly(USER_ID);

        verify(appSharingFacade).getAppGrants(WORKSPACE_ID, APP_ID);
        verifyNoMoreInteractions(appSharingFacade);
    }

    @Test
    void testAppInstanceGrantsDelegates() {
        when(appSharingFacade.getAppInstanceGrants(WORKSPACE_ID, APP_INSTANCE_ID)).thenReturn(List.of(USER_ID));

        assertThat(controller.appInstanceGrants(WORKSPACE_ID, APP_INSTANCE_ID)).containsExactly(USER_ID);

        verify(appSharingFacade).getAppInstanceGrants(WORKSPACE_ID, APP_INSTANCE_ID);
        verifyNoMoreInteractions(appSharingFacade);
    }

    @Test
    void testAppMutationsDelegate() {
        assertThat(controller.setAppVisibility(WORKSPACE_ID, APP_ID, ResourceVisibility.PRIVATE)).isTrue();
        assertThat(controller.grantAppAccess(WORKSPACE_ID, APP_ID, USER_ID)).isTrue();
        assertThat(controller.revokeAppAccess(WORKSPACE_ID, APP_ID, USER_ID)).isTrue();

        verify(appSharingFacade).setAppVisibility(WORKSPACE_ID, APP_ID, ResourceVisibility.PRIVATE);
        verify(appSharingFacade).grantAppAccess(WORKSPACE_ID, APP_ID, USER_ID);
        verify(appSharingFacade).revokeAppAccess(WORKSPACE_ID, APP_ID, USER_ID);
        verifyNoMoreInteractions(appSharingFacade);
    }

    @Test
    void testAppInstanceMutationsDelegate() {
        assertThat(controller.setAppInstanceVisibility(WORKSPACE_ID, APP_INSTANCE_ID, ResourceVisibility.WORKSPACE))
            .isTrue();
        assertThat(controller.grantAppInstanceAccess(WORKSPACE_ID, APP_INSTANCE_ID, USER_ID)).isTrue();
        assertThat(controller.revokeAppInstanceAccess(WORKSPACE_ID, APP_INSTANCE_ID, USER_ID)).isTrue();

        verify(appSharingFacade).setAppInstanceVisibility(WORKSPACE_ID, APP_INSTANCE_ID, ResourceVisibility.WORKSPACE);
        verify(appSharingFacade).grantAppInstanceAccess(WORKSPACE_ID, APP_INSTANCE_ID, USER_ID);
        verify(appSharingFacade).revokeAppInstanceAccess(WORKSPACE_ID, APP_INSTANCE_ID, USER_ID);
        verifyNoMoreInteractions(appSharingFacade);
    }
}
