/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.domain.AppDeploymentProviderType;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppDeploymentDTO;
import com.bytechef.ee.automation.app.dto.AppVersionDTO;
import com.bytechef.ee.automation.app.facade.AppDeploymentFacade;
import com.bytechef.ee.automation.app.web.graphql.AppDeploymentGraphQlController.AppDeploymentRecordInput;
import com.bytechef.ee.automation.app.web.graphql.AppDeploymentGraphQlController.AppDeploymentResponse;
import com.bytechef.ee.automation.app.web.graphql.AppDeploymentGraphQlController.AppVersionInput;
import com.bytechef.ee.automation.app.web.graphql.AppDeploymentGraphQlController.AppVersionResponse;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlController.AppResponse;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlMapper.AppInstanceResponse;
import com.bytechef.ee.automation.app.web.graphql.AppGraphQlMapper.AppSummaryResponse;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 */
class AppDeploymentGraphQlControllerTest {

    private static final Instant CREATED_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FINISHED_DATE = Instant.parse("2026-01-01T00:05:00Z");

    private static final long APP_ID = 5L;
    private static final long APP_INSTANCE_ID = 50L;
    private static final long APP_VERSION_ID = 70L;

    private AppDeploymentFacade appDeploymentFacade;
    private AppDeploymentGraphQlController controller;

    @BeforeEach
    void beforeEach() {
        appDeploymentFacade = mock(AppDeploymentFacade.class);

        controller = new AppDeploymentGraphQlController(appDeploymentFacade);
    }

    @Test
    void testControllerOwnsNoAuthorization() {
        for (Method method : AppDeploymentGraphQlController.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(PreAuthorize.class)).as(method.getName())
                .isNull();
        }
    }

    @Test
    void testAppVersionsIsOneFacadeCall() {
        when(appDeploymentFacade.getVersions(APP_ID, 50)).thenReturn(List.of(appVersionDto()));

        assertThat(controller.appVersions(APP_ID, null)).containsExactly(
            new AppVersionResponse(
                APP_VERSION_ID, APP_ID, "1.1", "main", "abc123", "First release", "alice", CREATED_DATE.toString()));

        verify(appDeploymentFacade).getVersions(APP_ID, 50);
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testAppInstanceVersionsIsOneInstanceKeyedFacadeCall() {
        when(appDeploymentFacade.getVersionsForInstance(APP_INSTANCE_ID, 50)).thenReturn(List.of(appVersionDto()));

        assertThat(controller.appInstanceVersions(APP_INSTANCE_ID, null)).extracting(AppVersionResponse::name)
            .containsExactly("1.1");

        verify(appDeploymentFacade).getVersionsForInstance(APP_INSTANCE_ID, 50);
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testAppInstanceVersionsPassesAGivenLimit() {
        when(appDeploymentFacade.getVersionsForInstance(APP_INSTANCE_ID, 7)).thenReturn(List.of());

        assertThat(controller.appInstanceVersions(APP_INSTANCE_ID, 7)).isEmpty();

        verify(appDeploymentFacade).getVersionsForInstance(APP_INSTANCE_ID, 7);
    }

    @Test
    void testAppVersionsFieldOfAnAppIsOneFacadeCall() {
        when(appDeploymentFacade.getVersions(APP_ID, 50)).thenReturn(List.of(appVersionDto()));

        assertThat(controller.versions(appResponse(), null)).extracting(AppVersionResponse::name)
            .containsExactly("1.1");

        verify(appDeploymentFacade).getVersions(APP_ID, 50);
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testAppDeploymentsCarriesTheVersionNameAndTheDates() {
        when(appDeploymentFacade.getDeployments(APP_INSTANCE_ID, 50)).thenReturn(List.of(appDeploymentDto(90L, true)));

        AppDeploymentResponse response = controller.appDeployments(APP_INSTANCE_ID, null)
            .getFirst();

        assertThat(response.id()).isEqualTo(90L);
        assertThat(response.appInstanceId()).isEqualTo(APP_INSTANCE_ID);
        assertThat(response.appVersion()
            .name()).isEqualTo("1.1");
        assertThat(response.provider()).isEqualTo(AppDeploymentProviderType.EXTERNAL);
        assertThat(response.status()).isEqualTo(AppDeploymentStatus.READY);
        assertThat(response.url()).isEqualTo("https://crm.example.com");
        assertThat(response.current()).isTrue();
        assertThat(response.createdDate()).isEqualTo(CREATED_DATE.toString());
        assertThat(response.finishedDate()).isEqualTo(FINISHED_DATE.toString());

        verify(appDeploymentFacade).getDeployments(APP_INSTANCE_ID, 50);
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testDeploymentResponseCarriesNoMetadata() {
        assertThat(Arrays.stream(AppDeploymentResponse.class.getRecordComponents())
            .map(RecordComponent::getName)).doesNotContain("metadata");
    }

    @Test
    void testDeploymentsFieldOfAnInstanceIsOneFacadeCall() {
        when(appDeploymentFacade.getDeployments(APP_INSTANCE_ID, 50))
            .thenReturn(List.of(appDeploymentDto(91L, true), appDeploymentDto(90L, false)));

        assertThat(controller.deployments(appInstanceResponse(91L), null)).extracting(AppDeploymentResponse::id)
            .containsExactly(91L, 90L);

        verify(appDeploymentFacade).getDeployments(APP_INSTANCE_ID, 50);
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testListsPassAGivenLimitToTheFacade() {
        when(appDeploymentFacade.getVersions(APP_ID, 7)).thenReturn(List.of());
        when(appDeploymentFacade.getDeployments(APP_INSTANCE_ID, 200)).thenReturn(List.of());

        assertThat(controller.appVersions(APP_ID, 7)).isEmpty();
        assertThat(controller.versions(appResponse(), 7)).isEmpty();
        assertThat(controller.appDeployments(APP_INSTANCE_ID, 200)).isEmpty();
        assertThat(controller.deployments(appInstanceResponse(91L), 200)).isEmpty();

        verify(appDeploymentFacade, times(2)).getVersions(APP_ID, 7);
        verify(appDeploymentFacade, times(2)).getDeployments(APP_INSTANCE_ID, 200);
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testListsWithoutALimitUseTheDefaultOfFifty() {
        when(appDeploymentFacade.getVersions(APP_ID, AppDeploymentFacade.DEFAULT_LIST_LIMIT)).thenReturn(List.of());
        when(appDeploymentFacade.getDeployments(APP_INSTANCE_ID, AppDeploymentFacade.DEFAULT_LIST_LIMIT))
            .thenReturn(List.of());

        controller.appVersions(APP_ID, null);
        controller.appDeployments(APP_INSTANCE_ID, null);

        verify(appDeploymentFacade).getVersions(APP_ID, 50);
        verify(appDeploymentFacade).getDeployments(APP_INSTANCE_ID, 50);
    }

    @Test
    void testCurrentDeploymentIsOneSingleRowFacadeCall() {
        when(appDeploymentFacade.getCurrentDeployment(APP_INSTANCE_ID))
            .thenReturn(Optional.of(appDeploymentDto(91L, true)));

        AppDeploymentResponse currentDeployment = controller.currentDeployment(appInstanceResponse(91L));

        assertThat(currentDeployment).isNotNull();
        assertThat(currentDeployment.id()).isEqualTo(91L);
        assertThat(currentDeployment.current()).isTrue();

        verify(appDeploymentFacade).getCurrentDeployment(APP_INSTANCE_ID);
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testCurrentDeploymentTheFacadeDoesNotFindIsNull() {
        when(appDeploymentFacade.getCurrentDeployment(APP_INSTANCE_ID)).thenReturn(Optional.empty());

        assertThat(controller.currentDeployment(appInstanceResponse(91L))).isNull();
    }

    @Test
    void testCurrentDeploymentOfAnInstanceNeverDeployedIsNullWithoutAFacadeCall() {
        assertThat(controller.currentDeployment(appInstanceResponse(null))).isNull();

        verifyNoInteractions(appDeploymentFacade);
    }

    @Test
    void testCreateAppVersionIsOneFacadeCall() {
        when(appDeploymentFacade.createVersion(APP_ID, "1.1", "main", "abc123", "First release"))
            .thenReturn(appVersionDto());

        AppVersionResponse response = controller.createAppVersion(
            APP_ID, new AppVersionInput("1.1", "main", "abc123", "First release"));

        assertThat(response.id()).isEqualTo(APP_VERSION_ID);

        verify(appDeploymentFacade).createVersion(APP_ID, "1.1", "main", "abc123", "First release");
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testRecordAppDeploymentIsOneFacadeCall() {
        when(appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, "1.1", "https://crm.example.com", "abc123"))
            .thenReturn(appDeploymentDto(90L, true));

        AppDeploymentResponse response = controller.recordAppDeployment(
            APP_INSTANCE_ID, new AppDeploymentRecordInput("1.1", "https://crm.example.com", "abc123"));

        assertThat(response.id()).isEqualTo(90L);
        assertThat(response.current()).isTrue();

        verify(appDeploymentFacade).recordDeployment(APP_INSTANCE_ID, "1.1", "https://crm.example.com", "abc123");
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testAppDeploymentIsOneFacadeCall() {
        when(appDeploymentFacade.getDeployment(APP_INSTANCE_ID, 90L)).thenReturn(appDeploymentDto(90L, false));

        AppDeploymentResponse response = controller.appDeployment(APP_INSTANCE_ID, 90L);

        assertThat(response.id()).isEqualTo(90L);

        verify(appDeploymentFacade).getDeployment(APP_INSTANCE_ID, 90L);
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testDeployAppInstanceIsOneFacadeCall() {
        when(appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID)).thenReturn(appDeploymentDto(91L, false));

        AppDeploymentResponse response = controller.deployAppInstance(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(response.id()).isEqualTo(91L);

        verify(appDeploymentFacade).deploy(APP_INSTANCE_ID, APP_VERSION_ID);
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testRollbackAppInstanceIsOneFacadeCall() {
        when(appDeploymentFacade.rollback(APP_INSTANCE_ID, APP_VERSION_ID)).thenReturn(appDeploymentDto(92L, false));

        AppDeploymentResponse response = controller.rollbackAppInstance(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(response.id()).isEqualTo(92L);

        verify(appDeploymentFacade).rollback(APP_INSTANCE_ID, APP_VERSION_ID);
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testCancelAppDeploymentIsOneFacadeCall() {
        when(appDeploymentFacade.cancel(APP_INSTANCE_ID, 93L)).thenReturn(appDeploymentDto(93L, false));

        AppDeploymentResponse response = controller.cancelAppDeployment(APP_INSTANCE_ID, 93L);

        assertThat(response.id()).isEqualTo(93L);

        verify(appDeploymentFacade).cancel(APP_INSTANCE_ID, 93L);
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    @Test
    void testDeploymentProviderAvailableAsksTheFacadeForTheInstancesHostingTarget() {
        when(appDeploymentFacade.isDeploymentProviderAvailable(AppHostingTarget.VERCEL)).thenReturn(true);

        assertThat(controller.deploymentProviderAvailable(appInstanceResponse(null))).isTrue();

        verify(appDeploymentFacade).isDeploymentProviderAvailable(AppHostingTarget.VERCEL);
        verifyNoMoreInteractions(appDeploymentFacade);
    }

    private static AppVersionDTO appVersionDto() {
        return new AppVersionDTO(
            APP_VERSION_ID, APP_ID, "1.1", "main", "abc123", "First release", "alice", CREATED_DATE);
    }

    private static AppDeploymentDTO appDeploymentDto(long id, boolean current) {
        return new AppDeploymentDTO(
            id, APP_INSTANCE_ID, appVersionDto(), AppDeploymentProviderType.EXTERNAL, null, "https://crm.example.com",
            AppDeploymentStatus.READY, null, Map.of("internal", "value"), current, "alice", CREATED_DATE,
            FINISHED_DATE);
    }

    private static AppResponse appResponse() {
        return new AppResponse(
            APP_ID, "uuid", "Sales", null, 7L, 3L, null, false, AppStatus.ACTIVE, "WORKSPACE", false, List.of(),
            List.of(), "alice", null, "bob", null, 0);
    }

    private static AppInstanceResponse appInstanceResponse(@Nullable Long currentAppDeploymentId) {
        return new AppInstanceResponse(
            APP_INSTANCE_ID, "instance-uuid",
            new AppSummaryResponse(APP_ID, "Sales", null, null, false, AppStatus.ACTIVE), 7L, 2L, "Production", 3L,
            AppInstanceStatus.ACTIVE, "WORKSPACE", AppHostingTarget.VERCEL, null, null, null, AppUserAuthMode.NONE,
            Map.of(), currentAppDeploymentId, null, Map.of(), "alice", null, "bob", null, 0);
    }
}
