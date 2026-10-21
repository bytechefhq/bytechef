/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 */
class AppGraphQlControllerTest {

    private static final Instant CREATED_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DEPLOYED_DATE = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant USED_DATE = Instant.parse("2026-01-03T00:00:00Z");

    private AppFacade appFacade;
    private AppGraphQlController controller;
    private EnvironmentService environmentService;

    @BeforeEach
    void beforeEach() {
        appFacade = mock(AppFacade.class);
        environmentService = mock(EnvironmentService.class);

        when(environmentService.getEnvironment(1L)).thenReturn(Environment.STAGING);

        controller = new AppGraphQlController(appFacade, environmentService);
    }

    @Test
    void testControllerOwnsNoAuthorization() {
        for (Method method : AppGraphQlController.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
        }
    }

    @Test
    void testCreateAppResolvesEnvironmentAndDefaultsVisibilityToWorkspace() {
        when(
            appFacade.createApp(
                7L, Environment.STAGING, "Sales", "desc", AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE))
                    .thenReturn(new AppSecretKeyDTO(appDto(), "secret"));

        AppGraphQlController.AppSecretKeyResponse response = controller.createApp(
            7L, 1L, new AppGraphQlController.AppInput("Sales", "desc", AppHostingTarget.VERCEL, null, null));

        assertThat(response.secretKey()).isEqualTo("secret");
        assertThat(response.app()
            .environmentId()).isEqualTo(1L);
    }

    @Test
    void testCreateAppUsesExplicitVisibility() {
        when(
            appFacade.createApp(
                7L, Environment.STAGING, "Sales", null, AppHostingTarget.SELF_HOSTED, ResourceVisibility.PRIVATE))
                    .thenReturn(new AppSecretKeyDTO(appDto(), "secret"));

        controller.createApp(
            7L, 1L, new AppGraphQlController.AppInput("Sales", null, AppHostingTarget.SELF_HOSTED, null, "PRIVATE"));

        verify(appFacade).createApp(
            7L, Environment.STAGING, "Sales", null, AppHostingTarget.SELF_HOSTED, ResourceVisibility.PRIVATE);
    }

    @Test
    void testAppMapsInstantsToStrings() {
        when(appFacade.getApp(5L)).thenReturn(appDto());

        AppGraphQlController.AppResponse response = controller.app(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.environmentId()).isEqualTo(1L);
        assertThat(response.visibility()).isEqualTo("WORKSPACE");
        assertThat(response.createdDate()).isEqualTo(CREATED_DATE.toString());
        assertThat(response.lastDeployedDate()).isEqualTo(DEPLOYED_DATE.toString());
        assertThat(response.secretKeyLastUsedDate()).isEqualTo(USED_DATE.toString());
        assertThat(response.lastModifiedDate()).isEqualTo(CREATED_DATE.toString());
    }

    @Test
    void testAppMapsNullInstantsToNull() {
        when(appFacade.getApp(5L)).thenReturn(appDto(null, null));

        AppGraphQlController.AppResponse response = controller.app(5L);

        assertThat(response.lastDeployedDate()).isNull();
        assertThat(response.secretKeyLastUsedDate()).isNull();
    }

    @Test
    void testAppsDelegates() {
        when(appFacade.getApps(7L, 1L)).thenReturn(List.of(appDto()));

        assertThat(controller.apps(7L, 1L)).extracting(AppGraphQlController.AppResponse::id)
            .containsExactly(5L);
    }

    @Test
    void testUpdateAppPassesInputThroughWithExplicitVisibility() {
        when(
            appFacade.updateApp(
                5L, "Sales", null, AppHostingTarget.VERCEL, "https://x.test", null, ResourceVisibility.PRIVATE))
                    .thenReturn(appDto());

        AppGraphQlController.AppResponse response = controller.updateApp(
            5L, new AppGraphQlController.AppInput(
                "Sales", null, AppHostingTarget.VERCEL, "https://x.test", "private"));

        assertThat(response.id()).isEqualTo(5L);

        verify(appFacade, never()).getApp(5L);
    }

    @Test
    void testUpdateAppWithNullVisibilityPassesNullSoTheFacadeKeepsIt() {
        when(appFacade.updateApp(5L, "Sales", null, AppHostingTarget.VERCEL, null, null, null)).thenReturn(appDto());

        controller.updateApp(5L, new AppGraphQlController.AppInput("Sales", null, AppHostingTarget.VERCEL, null, null));

        verify(appFacade).updateApp(5L, "Sales", null, AppHostingTarget.VERCEL, null, null, null);
        verify(appFacade, never()).getApp(5L);
    }

    @Test
    void testInvalidVisibilitySurfacesAsVisibilityUnsupported() {
        AppGraphQlController.AppInput input =
            new AppGraphQlController.AppInput("Sales", null, AppHostingTarget.VERCEL, null, "BOGUS");

        for (ThrowingCallable call : new ThrowingCallable[] {
            () -> controller.updateApp(5L, input), () -> controller.createApp(7L, 1L, input)
        }) {
            assertThatThrownBy(call).isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AppErrorType.APP_VISIBILITY_UNSUPPORTED.getErrorKey());
        }

        verifyNoInteractions(appFacade);
    }

    @Test
    void testUpdateAppStatusDelegates() {
        when(appFacade.updateAppStatus(5L, AppStatus.DISABLED)).thenReturn(appDto());

        assertThat(controller.updateAppStatus(5L, AppStatus.DISABLED)
            .id()).isEqualTo(5L);
    }

    @Test
    void testRotateAppSecretKeyReturnsFacadeKey() {
        when(appFacade.rotateSecretKey(5L)).thenReturn(new AppSecretKeyDTO(appDto(), "rotated"));

        assertThat(controller.rotateAppSecretKey(5L)
            .secretKey()).isEqualTo("rotated");
    }

    @Test
    void testDeleteAppDelegatesAndReturnsTrue() {
        assertThat(controller.deleteApp(5L)).isTrue();

        verify(appFacade).deleteApp(5L);
    }

    @Test
    void testAppBindingsDelegates() {
        AppBindingDTO binding = bindingDto();

        when(appFacade.getBindings(5L)).thenReturn(List.of(binding));

        assertThat(controller.appBindings(5L)).containsExactly(
            new AppGraphQlController.AppBindingResponse(
                1L, 5L, "sales_slack", 9L, "Slack", "slack", 1, 2, List.of("a"), List.of("t"), true, List.of("GET")));
    }

    @Test
    void testAppBindingsKeepsANullComponentVersion() {
        AppBindingDTO binding = new AppBindingDTO(
            1L, 5L, "legacy", 9L, "Legacy", "deleted-component", 1, null, List.of("a"), List.of("t"), false,
            List.of());

        when(appFacade.getBindings(5L)).thenReturn(List.of(binding));

        assertThat(controller.appBindings(5L)).extracting(AppGraphQlController.AppBindingResponse::componentVersion)
            .containsOnlyNulls();
    }

    @Test
    void testBindAppConnectionDelegates() {
        when(appFacade.bindConnection(5L, "sales_slack", 9L, List.of("a"), List.of("t"), true, List.of("GET")))
            .thenReturn(bindingDto());

        AppGraphQlController.AppBindingResponse response = controller.bindAppConnection(
            5L, new AppGraphQlController.AppBindingInput(
                "sales_slack", 9L, List.of("a"), List.of("t"), true, List.of("GET")));

        assertThat(response.connectionName()).isEqualTo("Slack");
    }

    @Test
    void testUnbindAppConnectionDelegatesAndReturnsTrue() {
        assertThat(controller.unbindAppConnection(5L, "sales_slack")).isTrue();

        verify(appFacade).unbindConnection(5L, "sales_slack");
    }

    @Test
    void testAppBindableConnectionsDelegatesToFacade() {
        when(appFacade.getBindableConnections(5L, null))
            .thenReturn(List.of(new AppBindableConnectionDTO(9L, "Slack", "slack", 1, 2, true)));

        assertThat(controller.appBindableConnections(5L)).containsExactly(
            new AppGraphQlController.AppBindableConnectionResponse(9L, "Slack", "slack", 1, 2));
    }

    @Test
    void testSecretKeyResponseToStringRedactsTheKey() {
        AppGraphQlController.AppSecretKeyResponse response = new AppGraphQlController.AppSecretKeyResponse(
            null, "bc_live_plaintext-key");

        assertThat(response.toString()).doesNotContain("bc_live_plaintext-key")
            .contains("[REDACTED]");
    }

    @Test
    void testAppComponentOperationsDelegatesToFacade() {
        when(appFacade.getComponentOperations("slack", 1))
            .thenReturn(new AppComponentOperationsDTO(List.of("a"), List.of("t")));

        assertThat(controller.appComponentOperations("slack", 1)).isEqualTo(
            new AppGraphQlController.AppComponentOperationsResponse(List.of("a"), List.of("t")));
    }

    private static AppDTO appDto() {
        return appDto(DEPLOYED_DATE, USED_DATE);
    }

    private static AppDTO appDto(Instant lastDeployedDate, Instant secretKeyLastUsedDate) {
        return new AppDTO(
            5L, "uuid", "Sales", null, 7L, Environment.STAGING, 3L, AppStatus.ENABLED, ResourceVisibility.WORKSPACE,
            AppHostingTarget.VERCEL, "https://x.test", lastDeployedDate, "hint", secretKeyLastUsedDate, "alice",
            CREATED_DATE, "bob", CREATED_DATE, 0);
    }

    private static AppBindingDTO bindingDto() {
        return new AppBindingDTO(
            1L, 5L, "sales_slack", 9L, "Slack", "slack", 1, 2, List.of("a"), List.of("t"), true, List.of("GET"));
    }
}
