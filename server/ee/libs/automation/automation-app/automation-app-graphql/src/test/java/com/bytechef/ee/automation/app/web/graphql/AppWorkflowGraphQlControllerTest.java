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
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.dto.AppBindableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowBindingDTO;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 */
class AppWorkflowGraphQlControllerTest {

    private static final long APP_INSTANCE_ID = 50L;

    private AppWorkflowFacade appWorkflowFacade;
    private AppWorkflowGraphQlController controller;

    @BeforeEach
    void beforeEach() {
        appWorkflowFacade = mock(AppWorkflowFacade.class);

        controller = new AppWorkflowGraphQlController(appWorkflowFacade);
    }

    @Test
    void testControllerOwnsNoAuthorization() {
        for (Method method : AppWorkflowGraphQlController.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
        }
    }

    @Test
    void testAppWorkflowsDelegates() {
        when(appWorkflowFacade.getWorkflowBindings(APP_INSTANCE_ID)).thenReturn(List.of(bindingDto()));

        assertThat(controller.appWorkflows(APP_INSTANCE_ID)).containsExactly(
            new AppWorkflowGraphQlController.AppWorkflowBindingResponse(
                1L, APP_INSTANCE_ID, "approve_refund", 11L, "Refunds", "uuid-1", "Approve refund", true));
    }

    @Test
    void testAppWorkflowsKeepsNullNamesOfAnUnavailableWorkflow() {
        when(appWorkflowFacade.getWorkflowBindings(APP_INSTANCE_ID))
            .thenReturn(
                List.of(new AppWorkflowBindingDTO(2L, APP_INSTANCE_ID, "legacy", 12L, "gone", null, null, false)));

        assertThat(controller.appWorkflows(APP_INSTANCE_ID)).containsExactly(
            new AppWorkflowGraphQlController.AppWorkflowBindingResponse(
                2L, APP_INSTANCE_ID, "legacy", 12L, null, "gone", null, false));
    }

    @Test
    void testAppBindableWorkflowsDelegates() {
        when(appWorkflowFacade.getBindableWorkflows(APP_INSTANCE_ID))
            .thenReturn(
                List.of(new AppBindableWorkflowDTO(11L, "Refunds", "Billing", "uuid-1", "Approve refund", null)));

        assertThat(controller.appBindableWorkflows(APP_INSTANCE_ID)).containsExactly(
            new AppWorkflowGraphQlController.AppBindableWorkflowResponse(
                11L, "Refunds", "Billing", "uuid-1", "Approve refund", null));
    }

    @Test
    void testBindAppWorkflowDelegates() {
        when(appWorkflowFacade.bindWorkflow(APP_INSTANCE_ID, "approve_refund", 11L, "uuid-1")).thenReturn(bindingDto());

        AppWorkflowGraphQlController.AppWorkflowBindingResponse response = controller.bindAppWorkflow(
            APP_INSTANCE_ID, new AppWorkflowGraphQlController.AppWorkflowBindingInput("approve_refund", 11L, "uuid-1"));

        assertThat(response.workflowKey()).isEqualTo("approve_refund");
        assertThat(response.available()).isTrue();

        verify(appWorkflowFacade).bindWorkflow(APP_INSTANCE_ID, "approve_refund", 11L, "uuid-1");
    }

    @Test
    void testUnbindAppWorkflowDelegatesAndReturnsTrue() {
        assertThat(controller.unbindAppWorkflow(APP_INSTANCE_ID, "approve_refund")).isTrue();

        verify(appWorkflowFacade).unbindWorkflow(APP_INSTANCE_ID, "approve_refund");
    }

    private static AppWorkflowBindingDTO bindingDto() {
        return new AppWorkflowBindingDTO(1L, APP_INSTANCE_ID, "approve_refund", 11L, "uuid-1", "Refunds",
            "Approve refund", true);
    }
}
