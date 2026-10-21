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

import com.bytechef.ee.automation.app.dto.AppApiCollectionBindingDTO;
import com.bytechef.ee.automation.app.dto.AppBindableApiCollectionDTO;
import com.bytechef.ee.automation.app.facade.AppApiCollectionFacade;
import com.bytechef.ee.automation.app.web.graphql.AppApiCollectionGraphQlController.AppApiCollectionBindingResponse;
import com.bytechef.ee.automation.app.web.graphql.AppApiCollectionGraphQlController.AppBindableApiCollectionResponse;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 */
class AppApiCollectionGraphQlControllerTest {

    private static final long APP_INSTANCE_ID = 50L;

    private AppApiCollectionFacade appApiCollectionFacade;
    private AppApiCollectionGraphQlController controller;

    @BeforeEach
    void beforeEach() {
        appApiCollectionFacade = mock(AppApiCollectionFacade.class);

        controller = new AppApiCollectionGraphQlController(appApiCollectionFacade);
    }

    @Test
    void testControllerOwnsNoAuthorization() {
        for (Method method : AppApiCollectionGraphQlController.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
        }
    }

    @Test
    void testAppApiCollectionsDelegates() {
        when(appApiCollectionFacade.getApiCollectionBindings(APP_INSTANCE_ID)).thenReturn(List.of(bindingDto()));

        assertThat(controller.appApiCollections(APP_INSTANCE_ID)).containsExactly(
            new AppApiCollectionBindingResponse(1L, APP_INSTANCE_ID, 21L, "CRM", "crm", 2, 3L, "Sales", true, 11L, 4));
    }

    @Test
    void testAppApiCollectionsMapsAMissingCollectionWithoutAProject() {
        when(appApiCollectionFacade.getApiCollectionBindings(APP_INSTANCE_ID))
            .thenReturn(
                List.of(new AppApiCollectionBindingDTO(2L, APP_INSTANCE_ID, 22L, "#22", null, 0, 0L, null, false, null,
                    null)));

        assertThat(controller.appApiCollections(APP_INSTANCE_ID)).containsExactly(
            new AppApiCollectionBindingResponse(2L, APP_INSTANCE_ID, 22L, "#22", null, 0, null, null, false, null,
                null));
    }

    @Test
    void testAppBindableApiCollectionsDelegates() {
        when(appApiCollectionFacade.getBindableApiCollections(APP_INSTANCE_ID))
            .thenReturn(List.of(new AppBindableApiCollectionDTO(21L, "CRM", "crm", 2, 3L, "Sales", false)));

        assertThat(controller.appBindableApiCollections(APP_INSTANCE_ID)).containsExactly(
            new AppBindableApiCollectionResponse(21L, "CRM", "crm", 2, 3L, "Sales", false));
    }

    @Test
    void testBindAppApiCollectionDelegates() {
        when(appApiCollectionFacade.bindApiCollection(APP_INSTANCE_ID, 21L)).thenReturn(bindingDto());

        AppApiCollectionBindingResponse response = controller.bindAppApiCollection(APP_INSTANCE_ID, 21L);

        assertThat(response.apiCollectionId()).isEqualTo(21L);
        assertThat(response.available()).isTrue();

        verify(appApiCollectionFacade).bindApiCollection(APP_INSTANCE_ID, 21L);
    }

    @Test
    void testUnbindAppApiCollectionDelegatesAndReturnsTrue() {
        assertThat(controller.unbindAppApiCollection(APP_INSTANCE_ID, 21L)).isTrue();

        verify(appApiCollectionFacade).unbindApiCollection(APP_INSTANCE_ID, 21L);
    }

    private static AppApiCollectionBindingDTO bindingDto() {
        return new AppApiCollectionBindingDTO(1L, APP_INSTANCE_ID, 21L, "CRM", "crm", 2, 3L, "Sales", true, 11L, 4);
    }
}
