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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppCategoryDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.dto.ForeignAppInstanceDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlMapper.AppInstanceResponse;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlMapper.AppInstanceSecretKeyResponse;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 */
class AppGraphQlControllerTest {

    private static final Instant CREATED_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant USED_DATE = Instant.parse("2026-01-03T00:00:00Z");

    private static final long APP_INSTANCE_ID = 50L;

    private AppFacade appFacade;
    private AppGraphQlController controller;

    @BeforeEach
    void beforeEach() {
        appFacade = mock(AppFacade.class);

        EnvironmentService environmentService = mock(EnvironmentService.class);

        when(environmentService.getEnvironment(1L)).thenReturn(Environment.STAGING);
        when(appFacade.getApp(5L)).thenReturn(appDto(AppStatus.ACTIVE, List.of(appInstanceDto(USED_DATE))));

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
                7L, Environment.STAGING, "Sales", "desc", AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
                null, null, false))
                    .thenReturn(new AppSecretKeyDTO(appInstanceDto(USED_DATE), "secret"));

        AppInstanceSecretKeyResponse response = controller.createApp(
            7L, 1L,
            new AppGraphQlController.AppInput("Sales", "desc", AppHostingTarget.VERCEL, null, null, null, null));

        assertThat(response.secretKey()).isEqualTo("secret");
        assertThat(response.appInstance()
            .id()).isEqualTo(APP_INSTANCE_ID);
        assertThat(response.appInstance()
            .app()
            .id()).isEqualTo(5L);
        assertThat(response.appInstance()
            .environmentId()).isEqualTo(1L);

        verify(appFacade).createApp(
            7L, Environment.STAGING, "Sales", "desc", AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE, null,
            null, false);
        verifyNoMoreInteractions(appFacade);
    }

    @Test
    void testCreateAppUsesExplicitVisibility() {
        when(
            appFacade.createApp(
                7L, Environment.STAGING, "Sales", null, AppHostingTarget.SELF_HOSTED, ResourceVisibility.PRIVATE,
                null, null, false))
                    .thenReturn(new AppSecretKeyDTO(appInstanceDto(USED_DATE), "secret"));

        controller.createApp(
            7L, 1L, new AppGraphQlController.AppInput("Sales", null, AppHostingTarget.SELF_HOSTED, null, "PRIVATE",
                null, null));

        verify(appFacade).createApp(
            7L, Environment.STAGING, "Sales", null, AppHostingTarget.SELF_HOSTED, ResourceVisibility.PRIVATE, null,
            null, false);
    }

    @Test
    void testCreateAppPassesTheSignInMode() {
        when(
            appFacade.createApp(
                7L, Environment.STAGING, "Sales", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
                AppUserAuthMode.NONE, null, false))
                    .thenReturn(new AppSecretKeyDTO(appInstanceDto(USED_DATE), "secret"));

        controller.createApp(
            7L, 1L,
            new AppGraphQlController.AppInput("Sales", null, AppHostingTarget.VERCEL, AppUserAuthMode.NONE, null, null,
                null));

        verify(appFacade).createApp(
            7L, Environment.STAGING, "Sales", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
            AppUserAuthMode.NONE, null, false);
    }

    @Test
    void testCreateAppPassesTheCategoryAndPortalVisibility() {
        when(
            appFacade.createApp(
                7L, Environment.STAGING, "Sales", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE, null,
                new AppCategoryDTO(null, "Finance"), true))
                    .thenReturn(new AppSecretKeyDTO(appInstanceDto(USED_DATE), "secret"));

        controller.createApp(
            7L, 1L,
            new AppGraphQlController.AppInput(
                "Sales", null, AppHostingTarget.VERCEL, null, null,
                new AppGraphQlController.AppCategoryInput(null, "Finance"), true));

        verify(appFacade).createApp(
            7L, Environment.STAGING, "Sales", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE, null,
            new AppCategoryDTO(null, "Finance"), true);
    }

    @Test
    void testUpdateAppPassesTheCategoryAndPortalVisibility() {
        AppDTO app = new AppDTO(
            5L, "uuid", "Sales", null, 7L, 3L, 42L, false, AppStatus.DRAFT, ResourceVisibility.WORKSPACE, true,
            List.of(), List.of(), "alice", CREATED_DATE, "bob", CREATED_DATE, 0);

        when(appFacade.updateApp(5L, null, null, new AppCategoryDTO(42L, null), true)).thenReturn(app);

        AppGraphQlController.AppResponse response = controller.updateApp(
            5L,
            new AppGraphQlController.AppUpdateInput(
                null, null, new AppGraphQlController.AppCategoryInput(42L, null), true));

        assertThat(response.categoryId()).isEqualTo(42L);
        assertThat(response.portalDiscoverable()).isTrue();

        verify(appFacade).updateApp(5L, null, null, new AppCategoryDTO(42L, null), true);
        verifyNoMoreInteractions(appFacade);
    }

    @Test
    void testAppCategoriesMapsTheFacadeCategories() {
        when(appFacade.getAppCategories(7L))
            .thenReturn(List.of(new AppCategoryDTO(42L, "Finance"), new AppCategoryDTO(43L, "Sales")));

        assertThat(controller.appCategories(7L)).containsExactly(
            new AppGraphQlController.AppCategoryResponse(42L, "Finance"),
            new AppGraphQlController.AppCategoryResponse(43L, "Sales"));
    }

    @Test
    void testAppMapsInstancesAndInstantsToStrings() {
        AppGraphQlController.AppResponse response = controller.app(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.status()).isEqualTo(AppStatus.ACTIVE);
        assertThat(response.visibility()).isEqualTo("WORKSPACE");
        assertThat(response.createdDate()).isEqualTo(CREATED_DATE.toString());
        assertThat(response.lastModifiedDate()).isEqualTo(CREATED_DATE.toString());
        assertThat(response.instances()).hasSize(1);

        AppInstanceResponse appInstance = response.instances()
            .getFirst();

        assertThat(appInstance.id()).isEqualTo(APP_INSTANCE_ID);
        assertThat(appInstance.environmentId()).isEqualTo(1L);
        assertThat(appInstance.status()).isEqualTo(AppInstanceStatus.ACTIVE);
        assertThat(appInstance.hostingTarget()).isEqualTo(AppHostingTarget.VERCEL);
        assertThat(appInstance.url()).isEqualTo("https://x.test");
        assertThat(appInstance.secretKeyHint()).isEqualTo("hint");
        assertThat(appInstance.secretKeyLastUsedDate()).isEqualTo(USED_DATE.toString());
        assertThat(appInstance.userAuthMode()).isEqualTo(AppUserAuthMode.NONE);
    }

    @Test
    void testAppAndTheSummaryOfItsInstancesTellWhetherTheAppHasAnIcon() {
        AppInstanceDTO appInstance = appInstanceDto(USED_DATE);
        AppInstanceDTO appInstanceWithIcon = new AppInstanceDTO(
            appInstance.id(), appInstance.uuid(), new AppInstanceDTO.AppSummary(
                5L, "Sales", null, null, true, AppStatus.ACTIVE),
            appInstance.workspaceId(), appInstance.environment(), appInstance.name(), appInstance.ownerUserId(),
            appInstance.status(), appInstance.visibility(), appInstance.hostingTarget(), appInstance.url(),
            appInstance.secretKeyHint(), appInstance.secretKeyLastUsedDate(), appInstance.userAuthMode(),
            appInstance.configuration(), appInstance.currentAppDeploymentId(), appInstance.deploymentConnectionId(),
            appInstance.deploymentSettings(), appInstance.createdBy(),
            appInstance.createdDate(), appInstance.lastModifiedBy(), appInstance.lastModifiedDate(),
            appInstance.version());

        when(appFacade.getApp(5L)).thenReturn(
            new AppDTO(
                5L, "uuid", "Sales", null, 7L, 3L, null, true, AppStatus.ACTIVE, ResourceVisibility.WORKSPACE, false,
                List.of(appInstanceWithIcon), List.of(), "alice", CREATED_DATE, "bob", CREATED_DATE, 0));

        AppGraphQlController.AppResponse response = controller.app(5L);

        assertThat(response.hasIcon()).isTrue();
        assertThat(response.instances()
            .getFirst()
            .app()
            .hasIcon()).isTrue();
    }

    @Test
    void testAppMapsNullInstantsToNull() {
        when(appFacade.getApp(5L)).thenReturn(appDto(AppStatus.ACTIVE, List.of(appInstanceDto(null))));

        assertThat(controller.app(5L)
            .instances()
            .getFirst()
            .secretKeyLastUsedDate()).isNull();
    }

    @Test
    void testAppMapsForeignInstancesToTheReducedRow() {
        ForeignAppInstanceDTO foreignAppInstance = new ForeignAppInstanceDTO(
            60L, "Germany", 8L, Environment.PRODUCTION, AppInstanceStatus.SUSPENDED, 4L);

        when(appFacade.getApp(5L)).thenReturn(
            new AppDTO(
                5L, "uuid", "Sales", null, 7L, 3L, null, false, AppStatus.ACTIVE, ResourceVisibility.WORKSPACE, false,
                List.of(), List.of(foreignAppInstance), "alice", CREATED_DATE, "bob", CREATED_DATE, 0));

        AppGraphQlController.AppResponse response = controller.app(5L);

        assertThat(response.instances()).isEmpty();
        assertThat(response.foreignInstances()).containsExactly(
            new AppGraphQlController.ForeignAppInstanceResponse(
                60L, "Germany", 8L, Environment.PRODUCTION.ordinal(), AppInstanceStatus.SUSPENDED, 4L));
        assertThat(AppGraphQlController.ForeignAppInstanceResponse.class.getRecordComponents()).hasSize(6);
    }

    @Test
    void testAppWhoseInstancesTheCallerCannotSeeIsReturnedWithoutInstances() {
        when(appFacade.getApp(5L)).thenReturn(appDto(AppStatus.ACTIVE, List.of()));

        AppGraphQlController.AppResponse response = controller.app(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.instances()).isEmpty();
    }

    @Test
    void testAppsListsEveryAppOfTheWorkspaceWhateverItsInstances() {
        AppDTO appWithoutInstances = new AppDTO(
            6L, "uuid-6", "Support", null, 7L, 3L, null, false, AppStatus.DRAFT, ResourceVisibility.WORKSPACE, false,
            List.of(), List.of(), "alice", CREATED_DATE, "bob", CREATED_DATE, 0);

        when(appFacade.getApps(7L)).thenReturn(
            List.of(appDto(AppStatus.ACTIVE, List.of(appInstanceDto(USED_DATE))), appWithoutInstances));

        assertThat(controller.apps(7L)).extracting(AppGraphQlController.AppResponse::id)
            .containsExactly(5L, 6L);
    }

    @Test
    void testAppUserAuthAvailableAsksTheFacade() {
        when(appFacade.isUserAuthAvailable(7L)).thenReturn(true);

        assertThat(controller.appUserAuthAvailable(7L)).isTrue();

        when(appFacade.isUserAuthAvailable(7L)).thenReturn(false);

        assertThat(controller.appUserAuthAvailable(7L)).isFalse();
    }

    @Test
    void testUpdateAppPassesOnlyAppFieldsInOneCall() {
        when(appFacade.updateApp(5L, "Sales", null, null, null)).thenReturn(appDto(AppStatus.ACTIVE, List.of()));

        AppGraphQlController.AppResponse response = controller.updateApp(
            5L, new AppGraphQlController.AppUpdateInput("Sales", null, null, null));

        assertThat(response.id()).isEqualTo(5L);

        verify(appFacade).updateApp(5L, "Sales", null, null, null);
        verifyNoMoreInteractions(appFacade);
    }

    @Test
    void testInvalidVisibilitySurfacesAsVisibilityUnsupported() {
        for (ThrowingCallable call : new ThrowingCallable[] {
            () -> controller.createApp(
                7L, 1L,
                new AppGraphQlController.AppInput("Sales", null, AppHostingTarget.VERCEL, null, "BOGUS", null, null))
        }) {
            assertThatThrownBy(call).isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AppErrorType.APP_VISIBILITY_UNSUPPORTED.getErrorKey());
        }

        verify(appFacade, never()).createApp(
            anyLong(), any(), any(), any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void testUpdateAppStatusPassesTheLifecycleStatus() {
        when(appFacade.updateAppStatus(5L, AppStatus.ARCHIVED)).thenReturn(appDto(AppStatus.ARCHIVED, List.of()));

        assertThat(controller.updateAppStatus(5L, AppStatus.ARCHIVED)
            .status()).isEqualTo(AppStatus.ARCHIVED);
    }

    @Test
    void testDeleteAppDelegatesAndReturnsTrue() {
        assertThat(controller.deleteApp(5L)).isTrue();

        verify(appFacade).deleteApp(5L);
    }

    @Test
    void testSecretKeyResponseToStringRedactsTheKey() {
        AppInstanceSecretKeyResponse response = new AppInstanceSecretKeyResponse(null, "bc_live_plaintext-key");

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

    private static AppDTO appDto(AppStatus status, List<AppInstanceDTO> appInstances) {
        return new AppDTO(
            5L, "uuid", "Sales", null, 7L, 3L, null, false, status, ResourceVisibility.WORKSPACE, false, appInstances,
            List.of(), "alice", CREATED_DATE, "bob", CREATED_DATE, 0);
    }

    private static AppInstanceDTO appInstanceDto(Instant secretKeyLastUsedDate) {
        return new AppInstanceDTO(
            APP_INSTANCE_ID, "instance-uuid",
            new AppInstanceDTO.AppSummary(5L, "Sales", null, null, false, AppStatus.ACTIVE), 7L, Environment.STAGING,
            "Staging", 3L, AppInstanceStatus.ACTIVE, ResourceVisibility.WORKSPACE, AppHostingTarget.VERCEL,
            "https://x.test", "hint", secretKeyLastUsedDate, AppUserAuthMode.NONE, Map.of(), null, null, Map.of(),
            "alice",
            CREATED_DATE, "bob", CREATED_DATE, 0);
    }
}
