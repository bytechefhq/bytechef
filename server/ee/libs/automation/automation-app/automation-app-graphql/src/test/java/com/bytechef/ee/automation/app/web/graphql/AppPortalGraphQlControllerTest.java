/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.dto.PortalAppDTO;
import com.bytechef.ee.automation.app.dto.PortalAppInstanceDTO;
import com.bytechef.ee.automation.app.facade.AppPortalFacade;
import com.bytechef.ee.automation.app.web.graphql.AppPortalGraphQlController.PortalAppInstanceResponse;
import com.bytechef.ee.automation.app.web.graphql.AppPortalGraphQlController.PortalAppResponse;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;

/**
 * @version ee
 */
class AppPortalGraphQlControllerTest {

    private static final Instant APP_LAST_USED_DATE = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant INSTANCE_LAST_USED_DATE = Instant.parse("2026-03-01T09:00:00Z");

    private static final long ENVIRONMENT_ID = Environment.PRODUCTION.ordinal();

    private AppPortalFacade appPortalFacade;
    private AppPortalGraphQlController controller;
    private EnvironmentService environmentService;

    @BeforeEach
    void beforeEach() {
        appPortalFacade = mock(AppPortalFacade.class);
        environmentService = mock(EnvironmentService.class);

        controller = new AppPortalGraphQlController(appPortalFacade, environmentService);
    }

    @Test
    void testControllerOwnsNoAuthorization() {
        for (Method method : AppPortalGraphQlController.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(PreAuthorize.class)).as(method.getName())
                .isNull();
        }
    }

    @Test
    void testPortalAppsMapsEveryField() {
        when(environmentService.getEnvironment(ENVIRONMENT_ID)).thenReturn(Environment.PRODUCTION);
        when(appPortalFacade.getPortalApps(Environment.PRODUCTION))
            .thenReturn(
                List.of(
                    new PortalAppDTO(
                        5L, "Sales", "Sales cockpit", 9L, "Finance", true, true, APP_LAST_USED_DATE,
                        List.of(
                            new PortalAppInstanceDTO(
                                50L, "Main", 7L, "Acme", "https://sales.example.com", INSTANCE_LAST_USED_DATE, 3L),
                            new PortalAppInstanceDTO(
                                51L, "Backup", 8L, "Beta", "https://backup.example.com", null, 0L))),
                    new PortalAppDTO(6L, "Support", null, null, null, false, false, null, List.of())));

        List<PortalAppResponse> responses = controller.portalApps(ENVIRONMENT_ID);

        assertThat(responses).containsExactly(
            new PortalAppResponse(
                5L, "Sales", "Sales cockpit", 9L, "Finance", true, true, APP_LAST_USED_DATE.toString(),
                List.of(
                    new PortalAppInstanceResponse(
                        50L, "Main", 7L, "Acme", "https://sales.example.com", INSTANCE_LAST_USED_DATE.toString(), 3L),
                    new PortalAppInstanceResponse(
                        51L, "Backup", 8L, "Beta", "https://backup.example.com", null, 0L))),
            new PortalAppResponse(6L, "Support", null, null, null, false, false, null, List.of()));

        verify(appPortalFacade).getPortalApps(Environment.PRODUCTION);
        verifyNoMoreInteractions(appPortalFacade);
    }

    @Test
    void testSetAppFavoriteDelegates() {
        assertThat(controller.setAppFavorite(5L, true)).isTrue();
        assertThat(controller.setAppFavorite(5L, false)).isTrue();

        verify(appPortalFacade).setAppFavorite(5L, true);
        verify(appPortalFacade).setAppFavorite(5L, false);
        verifyNoMoreInteractions(appPortalFacade);
    }

    @Test
    void testRecordAppLaunchDelegates() {
        assertThat(controller.recordAppLaunch(50L)).isTrue();

        verify(appPortalFacade).recordAppLaunch(50L);
        verifyNoMoreInteractions(appPortalFacade);
    }

    @Test
    void testFacadeRefusalsPropagate() {
        AuthorizationDeniedException refusal = new AuthorizationDeniedException("Access Denied");

        doThrow(refusal).when(appPortalFacade)
            .recordAppLaunch(50L);
        doThrow(refusal).when(appPortalFacade)
            .setAppFavorite(5L, true);

        assertThatThrownBy(() -> controller.recordAppLaunch(50L)).isSameAs(refusal);
        assertThatThrownBy(() -> controller.setAppFavorite(5L, true)).isSameAs(refusal);
    }
}
