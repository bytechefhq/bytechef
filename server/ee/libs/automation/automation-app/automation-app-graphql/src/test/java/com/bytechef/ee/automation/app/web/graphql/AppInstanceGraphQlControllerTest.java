/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppInstanceFacade;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlMapper.AppInstanceResponse;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlMapper.AppInstanceSecretKeyResponse;
import com.bytechef.ee.automation.app.web.graphql.AppInstanceGraphQlController.AppBindableConnectionResponse;
import com.bytechef.ee.automation.app.web.graphql.AppInstanceGraphQlController.AppBindingInput;
import com.bytechef.ee.automation.app.web.graphql.AppInstanceGraphQlController.AppBindingResponse;
import com.bytechef.ee.automation.app.web.graphql.AppInstanceGraphQlController.AppInstanceDeploymentSettingsInput;
import com.bytechef.ee.automation.app.web.graphql.AppInstanceGraphQlController.AppInstanceInput;
import com.bytechef.ee.automation.app.web.graphql.AppInstanceGraphQlController.AppInstanceUpdateInput;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 */
class AppInstanceGraphQlControllerTest {

    private static final Instant CREATED_DATE = Instant.parse("2026-01-01T00:00:00Z");

    private static final long APP_ID = 5L;
    private static final long APP_INSTANCE_ID = 50L;

    private AppFacade appFacade;
    private AppInstanceFacade appInstanceFacade;
    private AppInstanceGraphQlController controller;

    @BeforeEach
    void beforeEach() {
        appFacade = mock(AppFacade.class);
        appInstanceFacade = mock(AppInstanceFacade.class);

        EnvironmentService environmentService = mock(EnvironmentService.class);

        when(environmentService.getEnvironment(2L)).thenReturn(Environment.PRODUCTION);

        controller = new AppInstanceGraphQlController(appFacade, appInstanceFacade, environmentService);
    }

    @Test
    void testControllerOwnsNoAuthorization() {
        for (Method method : AppInstanceGraphQlController.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(PreAuthorize.class)).as(method.getName())
                .isNull();
        }

        for (Method method : AppGraphQlMapper.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(PreAuthorize.class)).as(method.getName())
                .isNull();
        }
    }

    @Test
    void testAppInstanceDelegates() {
        when(appInstanceFacade.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstanceDto(APP_INSTANCE_ID));

        AppInstanceResponse response = controller.appInstance(APP_INSTANCE_ID);

        assertThat(response.id()).isEqualTo(APP_INSTANCE_ID);
        assertThat(response.app()
            .id()).isEqualTo(APP_ID);
        assertThat(response.environmentId()).isEqualTo(Environment.PRODUCTION.ordinal());
        assertThat(response.visibility()).isEqualTo("WORKSPACE");
        assertThat(response.createdDate()).isEqualTo(CREATED_DATE.toString());
        assertThat(response.configuration()).isEqualTo(Map.of("country", "HR"));
        assertThat(response.deploymentConnectionId()).isEqualTo(9L);
        assertThat(response.deploymentSettings()).isEqualTo(Map.of("projectId", "prj_1"));
    }

    @Test
    void testUpdateAppInstanceDeploymentSettingsIsOneFacadeCall() {
        Map<String, Object> deploymentSettings = Map.of("projectId", "prj_2");

        when(appInstanceFacade.updateAppInstanceDeploymentSettings(APP_INSTANCE_ID, 9L, deploymentSettings))
            .thenReturn(appInstanceDto(APP_INSTANCE_ID));

        AppInstanceResponse response = controller.updateAppInstanceDeploymentSettings(
            APP_INSTANCE_ID, new AppInstanceDeploymentSettingsInput(9L, deploymentSettings));

        assertThat(response.id()).isEqualTo(APP_INSTANCE_ID);

        verify(appInstanceFacade).updateAppInstanceDeploymentSettings(APP_INSTANCE_ID, 9L, deploymentSettings);
        verifyNoMoreInteractions(appInstanceFacade);
    }

    @Test
    void testUpdateAppInstanceDeploymentSettingsWithNoConnectionAndNoSettingsAsksTheFacadeToClear() {
        when(appInstanceFacade.updateAppInstanceDeploymentSettings(APP_INSTANCE_ID, null, null))
            .thenReturn(appInstanceDto(APP_INSTANCE_ID));

        controller.updateAppInstanceDeploymentSettings(
            APP_INSTANCE_ID, new AppInstanceDeploymentSettingsInput(null, null));

        verify(appInstanceFacade).updateAppInstanceDeploymentSettings(APP_INSTANCE_ID, null, null);
    }

    @Test
    void testUpdateAppInstanceConfigurationPassesTheValueThroughUnchecked() {
        Map<String, Object> configuration = Map.of("country", "DE");

        when(appInstanceFacade.updateAppInstanceConfiguration(eq(APP_INSTANCE_ID), any()))
            .thenReturn(appInstanceDto(APP_INSTANCE_ID));

        AppInstanceResponse response = controller.updateAppInstanceConfiguration(APP_INSTANCE_ID, configuration);

        assertThat(response.id()).isEqualTo(APP_INSTANCE_ID);

        List<String> notAnObject = List.of("a");

        controller.updateAppInstanceConfiguration(APP_INSTANCE_ID, notAnObject);

        verify(appInstanceFacade).updateAppInstanceConfiguration(APP_INSTANCE_ID, configuration);
        verify(appInstanceFacade).updateAppInstanceConfiguration(APP_INSTANCE_ID, notAnObject);
        verifyNoMoreInteractions(appInstanceFacade);
    }

    @Test
    void testAppInstancesListsTheInstancesTheAppReadReturns() {
        when(appFacade.getApp(APP_ID)).thenReturn(appDto(List.of(appInstanceDto(50L), appInstanceDto(51L))));

        assertThat(controller.appInstances(APP_ID)).extracting(AppInstanceResponse::id)
            .containsExactly(50L, 51L);

        verify(appFacade).getApp(APP_ID);
        verifyNoInteractions(appInstanceFacade);
    }

    @Test
    void testAppInstancesOfAnAppWhoseInstancesTheCallerCannotSeeIsEmpty() {
        when(appFacade.getApp(APP_ID)).thenReturn(appDto(List.of()));

        assertThat(controller.appInstances(APP_ID)).isEmpty();
    }

    @Test
    void testHostedAppInstancesResolvesTheEnvironment() {
        when(appInstanceFacade.getHostedAppInstances(7L, Environment.PRODUCTION))
            .thenReturn(List.of(appInstanceDto(APP_INSTANCE_ID)));

        assertThat(controller.hostedAppInstances(7L, 2L)).extracting(AppInstanceResponse::id)
            .containsExactly(APP_INSTANCE_ID);
    }

    @Test
    void testCreateAppInstancePassesTheTargetWorkspaceAndDefaultsVisibility() {
        when(
            appInstanceFacade.createAppInstance(
                APP_ID, 8L, Environment.PRODUCTION, "Germany", AppHostingTarget.SELF_HOSTED,
                ResourceVisibility.WORKSPACE, null))
                    .thenReturn(new AppSecretKeyDTO(appInstanceDto(APP_INSTANCE_ID), "secret"));

        AppInstanceSecretKeyResponse response = controller.createAppInstance(
            APP_ID, 8L, 2L, new AppInstanceInput("Germany", AppHostingTarget.SELF_HOSTED, null, null));

        assertThat(response.secretKey()).isEqualTo("secret");
        assertThat(response.appInstance()
            .id()).isEqualTo(APP_INSTANCE_ID);

        verifyNoInteractions(appFacade);
    }

    @Test
    void testCreateAppInstanceRejectsAnUnknownVisibilityBeforeTheFacade() {
        assertThatThrownBy(
            () -> controller.createAppInstance(
                APP_ID, 8L, 2L, new AppInstanceInput("Germany", AppHostingTarget.VERCEL, null, "BOGUS")))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AppErrorType.APP_VISIBILITY_UNSUPPORTED.getErrorKey());

        verify(appInstanceFacade, never()).createAppInstance(anyLong(), anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void testCreateAppInstancePassesTheSignInMode() {
        when(
            appInstanceFacade.createAppInstance(
                APP_ID, 8L, Environment.PRODUCTION, "Germany", AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
                AppUserAuthMode.OPTIONAL))
                    .thenReturn(new AppSecretKeyDTO(appInstanceDto(APP_INSTANCE_ID), "secret"));

        controller.createAppInstance(
            APP_ID, 8L, 2L,
            new AppInstanceInput("Germany", AppHostingTarget.VERCEL, AppUserAuthMode.OPTIONAL, null));

        verify(appInstanceFacade).createAppInstance(
            APP_ID, 8L, Environment.PRODUCTION, "Germany", AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
            AppUserAuthMode.OPTIONAL);
    }

    @Test
    void testUpdateAppInstanceUserAuthIsOneFacadeCall() {
        when(appInstanceFacade.updateAppInstanceUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED))
            .thenReturn(appInstanceDto(APP_INSTANCE_ID));

        assertThat(controller.updateAppInstanceUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED)
            .id()).isEqualTo(APP_INSTANCE_ID);

        verify(appInstanceFacade).updateAppInstanceUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED);
    }

    @Test
    void testUpdateAppInstanceIsOneFacadeCall() {
        when(appInstanceFacade.updateAppInstance(APP_INSTANCE_ID, "Croatia", AppHostingTarget.VERCEL, "https://x.test"))
            .thenReturn(appInstanceDto(APP_INSTANCE_ID));

        controller.updateAppInstance(
            APP_INSTANCE_ID, new AppInstanceUpdateInput("Croatia", AppHostingTarget.VERCEL, "https://x.test"));

        verify(appInstanceFacade).updateAppInstance(APP_INSTANCE_ID, "Croatia", AppHostingTarget.VERCEL,
            "https://x.test");
        verifyNoMoreInteractions(appInstanceFacade);
        verifyNoInteractions(appFacade);
    }

    @Test
    void testUpdateAppInstanceStatusDelegates() {
        when(appInstanceFacade.updateAppInstanceStatus(APP_INSTANCE_ID, AppInstanceStatus.TERMINATED))
            .thenReturn(appInstanceDto(APP_INSTANCE_ID));

        controller.updateAppInstanceStatus(APP_INSTANCE_ID, AppInstanceStatus.TERMINATED);

        verify(appInstanceFacade).updateAppInstanceStatus(APP_INSTANCE_ID, AppInstanceStatus.TERMINATED);
    }

    @Test
    void testDeleteAppInstanceDelegatesAndReturnsTrue() {
        assertThat(controller.deleteAppInstance(APP_INSTANCE_ID)).isTrue();

        verify(appInstanceFacade).deleteAppInstance(APP_INSTANCE_ID);
    }

    @Test
    void testRotateAppInstanceSecretKeyReturnsTheNewKeyInOneCall() {
        when(appInstanceFacade.rotateSecretKey(APP_INSTANCE_ID))
            .thenReturn(new AppSecretKeyDTO(appInstanceDto(APP_INSTANCE_ID), "rotated"));

        assertThat(controller.rotateAppInstanceSecretKey(APP_INSTANCE_ID)
            .secretKey()).isEqualTo("rotated");

        verifyNoInteractions(appFacade);
    }

    @Test
    void testAppBindingsDelegatesAndCarriesTheInstanceId() {
        when(appInstanceFacade.getBindings(APP_INSTANCE_ID)).thenReturn(List.of(bindingDto()));

        assertThat(controller.appBindings(APP_INSTANCE_ID)).containsExactly(
            new AppBindingResponse(
                1L, APP_INSTANCE_ID, "sales_slack", 9L, "Slack", "slack", 1, 2, List.of("a"), List.of("t"), true,
                List.of("GET")));
    }

    @Test
    void testAppBindingsKeepsANullComponentVersion() {
        when(appInstanceFacade.getBindings(APP_INSTANCE_ID)).thenReturn(
            List.of(
                new AppBindingDTO(
                    1L, APP_INSTANCE_ID, "legacy", 9L, "Legacy", "deleted-component", 1, null, List.of("a"),
                    List.of("t"), false, List.of())));

        assertThat(controller.appBindings(APP_INSTANCE_ID)).extracting(AppBindingResponse::componentVersion)
            .containsOnlyNulls();
    }

    @Test
    void testAppBindableConnectionsDelegatesToFacade() {
        when(appInstanceFacade.getBindableConnections(APP_INSTANCE_ID, null))
            .thenReturn(List.of(new AppBindableConnectionDTO(9L, "Slack", "slack", 1, 2, true)));

        assertThat(controller.appBindableConnections(APP_INSTANCE_ID)).containsExactly(
            new AppBindableConnectionResponse(9L, "Slack", "slack", 1, 2));
    }

    @Test
    void testBindAppConnectionDelegates() {
        when(
            appInstanceFacade.bindConnection(
                APP_INSTANCE_ID, "sales_slack", 9L, List.of("a"), List.of("t"), true, List.of("GET")))
                    .thenReturn(bindingDto());

        AppBindingResponse response = controller.bindAppConnection(
            APP_INSTANCE_ID, new AppBindingInput("sales_slack", 9L, List.of("a"), List.of("t"), true, List.of("GET")));

        assertThat(response.connectionName()).isEqualTo("Slack");
        assertThat(response.appInstanceId()).isEqualTo(APP_INSTANCE_ID);
    }

    @Test
    void testUnbindAppConnectionDelegatesAndReturnsTrue() {
        assertThat(controller.unbindAppConnection(APP_INSTANCE_ID, "sales_slack")).isTrue();

        verify(appInstanceFacade).unbindConnection(APP_INSTANCE_ID, "sales_slack");
    }

    private static AppDTO appDto(List<AppInstanceDTO> appInstances) {
        return new AppDTO(
            APP_ID, "uuid", "Sales", null, 7L, 3L, null, false, AppStatus.ACTIVE, ResourceVisibility.WORKSPACE, false,
            appInstances, List.of(), "alice", CREATED_DATE, "bob", CREATED_DATE, 0);
    }

    private static AppInstanceDTO appInstanceDto(long id) {
        return new AppInstanceDTO(
            id, "instance-uuid-" + id,
            new AppInstanceDTO.AppSummary(APP_ID, "Sales", null, null, false, AppStatus.ACTIVE), 7L,
            Environment.PRODUCTION, "Production", 3L, AppInstanceStatus.ACTIVE, ResourceVisibility.WORKSPACE,
            AppHostingTarget.VERCEL, "https://x.test", "hint", null, AppUserAuthMode.REQUIRED,
            Map.of("country", "HR"), null, 9L, Map.of("projectId", "prj_1"), "alice", CREATED_DATE, "bob",
            CREATED_DATE, 0);
    }

    private static AppBindingDTO bindingDto() {
        return new AppBindingDTO(
            1L, APP_INSTANCE_ID, "sales_slack", 9L, "Slack", "slack", 1, 2, List.of("a"), List.of("t"), true,
            List.of("GET"));
    }
}
