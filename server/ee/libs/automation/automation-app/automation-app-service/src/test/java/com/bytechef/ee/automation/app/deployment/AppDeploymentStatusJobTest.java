/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.audit.AppAuditDataAssertions;
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.AppDeployment;
import com.bytechef.ee.automation.app.domain.AppDeploymentProviderType;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppVersion;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppDeploymentService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppVersionService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppDeploymentStatusJobTest {

    private static final long APP_ID = 1L;
    private static final long APP_INSTANCE_ID = 11L;
    private static final long APP_VERSION_ID = 21L;
    private static final long APP_DEPLOYMENT_ID = 31L;
    private static final long DEPLOYMENT_CONNECTION_ID = 41L;
    private static final Map<String, Object> DEPLOYMENT_AUDIT_DATA = Map.of(
        "appInstanceId", APP_INSTANCE_ID, "appDeploymentId", APP_DEPLOYMENT_ID, "appVersionId", APP_VERSION_ID,
        "provider", "VERCEL");

    private final AppAuditPublisher appAuditPublisher = mock(AppAuditPublisher.class);
    private final AppConnectionService appConnectionService = mock(AppConnectionService.class);
    private final AppDeploymentService appDeploymentService = mock(AppDeploymentService.class);
    private final AppInstanceService appInstanceService = mock(AppInstanceService.class);
    private final AppVersionService appVersionService = mock(AppVersionService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final RecordingAppExecutor recordingAppExecutor = new RecordingAppExecutor();
    private final FakeAppDeploymentProvider fakeAppDeploymentProvider = new FakeAppDeploymentProvider();
    private AppDeploymentSynchronizer appDeploymentSynchronizer;

    @BeforeEach
    void beforeEach() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        beanFactory.addBean("fakeAppDeploymentProvider", fakeAppDeploymentProvider);

        appDeploymentSynchronizer = new AppDeploymentSynchronizer(
            appAuditPublisher, beanFactory.getBeanProvider(AppDeploymentProvider.class), appDeploymentService,
            appConnectionService, appInstanceService, appVersionService, meterRegistryProvider(meterRegistry));

        lenient().when(appConnectionService.isConnectionOfComponent(DEPLOYMENT_CONNECTION_ID, "vercel"))
            .thenReturn(true);

        AppInstance appInstance = new AppInstance();

        appInstance.setId(APP_INSTANCE_ID);
        appInstance.setAppId(APP_ID);
        appInstance.setHostingTarget(AppHostingTarget.VERCEL);
        appInstance.setDeploymentConnectionId(DEPLOYMENT_CONNECTION_ID);
        appInstance.setDeploymentSettings(Map.of("projectId", "prj_1"));

        AppVersion appVersion = new AppVersion();

        appVersion.setId(APP_VERSION_ID);
        appVersion.setAppId(APP_ID);
        appVersion.setName("1.2");

        lenient().when(appInstanceService.getAppInstance(APP_INSTANCE_ID))
            .thenReturn(appInstance);
        lenient().when(appVersionService.getAppVersion(APP_VERSION_ID))
            .thenReturn(appVersion);
    }

    @AfterEach
    void afterEach() {
        AppAuditDataAssertions.assertNoSecrets(appAuditPublisher, "prj_1");
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testJobMovesBuildingToReadyAndSetsCurrent() {
        AppDeployment buildingAppDeployment = buildingDeployment(Instant.now());

        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(buildingAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(buildingAppDeployment);
        when(appDeploymentService.updateStatus(
            APP_DEPLOYMENT_ID, "dpl_1", AppDeploymentStatus.READY, "https://crm.vercel.app", null))
                .thenReturn(withStatus(buildingDeployment(Instant.now()), AppDeploymentStatus.READY));

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.READY, "https://crm.vercel.app");

        newJob(tenantServiceProvider(null)).pollDeployments();

        assertThat(fakeAppDeploymentProvider.polledRefs)
            .singleElement()
            .satisfies(ref -> {
                assertThat(ref.providerDeploymentId()).isEqualTo("dpl_1");
                assertThat(ref.connectionId()).isEqualTo(DEPLOYMENT_CONNECTION_ID);
                assertThat(ref.deploymentSettings()).isEqualTo(Map.of("projectId", "prj_1"));
            });
        assertThat(recordingAppExecutor.executedTasks).isEqualTo(1);

        verify(appDeploymentService).updateStatus(
            APP_DEPLOYMENT_ID, "dpl_1", AppDeploymentStatus.READY, "https://crm.vercel.app", null);
        verify(appInstanceService).updateCurrentAppDeployment(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_DEPLOYMENT_COMPLETED, APP_ID,
            Map.of(
                "appInstanceId", APP_INSTANCE_ID, "appDeploymentId", APP_DEPLOYMENT_ID, "appVersionId",
                APP_VERSION_ID, "versionName", "1.2", "provider", "VERCEL"));
    }

    @Test
    void testJobCountsAReadyDeploymentOnce() {
        AppDeployment buildingAppDeployment = buildingDeployment(Instant.now());

        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(buildingAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(buildingAppDeployment);
        when(appDeploymentService.updateStatus(
            APP_DEPLOYMENT_ID, "dpl_1", AppDeploymentStatus.READY, "https://crm.vercel.app", null))
                .thenReturn(withStatus(buildingDeployment(Instant.now()), AppDeploymentStatus.READY));

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.READY, "https://crm.vercel.app");

        newJob(tenantServiceProvider(null)).pollDeployments();

        assertThat(deploymentCount("VERCEL", "READY")).isEqualTo(1.0);
        assertThat(totalDeploymentCount()).isEqualTo(1.0);
    }

    @Test
    void testJobCountsAnUnchangedDeploymentNever() {
        AppDeployment buildingAppDeployment = buildingDeployment(Instant.now());

        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(buildingAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(buildingAppDeployment);

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.BUILDING, null);

        newJob(tenantServiceProvider(null)).pollDeployments();

        assertThat(totalDeploymentCount()).isZero();
    }

    @Test
    void testJobCountsTheDeadlineFailureOnce() {
        AppDeployment expiredAppDeployment = buildingDeployment(
            Instant.now()
                .minus(Duration.ofMinutes(31)));

        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(expiredAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(expiredAppDeployment);
        when(appDeploymentService.updateStatus(eq(APP_DEPLOYMENT_ID), eq(AppDeploymentStatus.FAILED), any(), any()))
            .thenReturn(withStatus(buildingDeployment(Instant.now()), AppDeploymentStatus.FAILED));

        newJob(tenantServiceProvider(null)).pollDeployments();

        assertThat(deploymentCount("VERCEL", "FAILED")).isEqualTo(1.0);
        assertThat(totalDeploymentCount()).isEqualTo(1.0);
    }

    @Test
    void testJobFailsTheRowAndDoesNotPollWithAConnectionOfAnotherComponent() {
        AppDeployment buildingAppDeployment = buildingDeployment(Instant.now());

        when(appConnectionService.isConnectionOfComponent(DEPLOYMENT_CONNECTION_ID, "vercel")).thenReturn(false);
        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(buildingAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(buildingAppDeployment);
        when(appDeploymentService.updateStatus(eq(APP_DEPLOYMENT_ID), eq(AppDeploymentStatus.FAILED), any(), any()))
            .thenReturn(withStatus(buildingDeployment(Instant.now()), AppDeploymentStatus.FAILED));

        newJob(tenantServiceProvider(null)).pollDeployments();

        assertThat(fakeAppDeploymentProvider.polledRefs).isEmpty();
        assertThat(fakeAppDeploymentProvider.canceledRefs).isEmpty();

        verify(appDeploymentService).updateStatus(
            APP_DEPLOYMENT_ID, AppDeploymentStatus.FAILED, null,
            "The deployment connection does not belong to the component the hosting provider uses");
        verify(appAuditPublisher).publish(AppAuditEvent.APP_DEPLOYMENT_FAILED, APP_ID, DEPLOYMENT_AUDIT_DATA);
    }

    @Test
    void testJobDeadlineDoesNotCancelWithAConnectionOfAnotherComponent() {
        AppDeployment expiredAppDeployment = buildingDeployment(
            Instant.now()
                .minus(Duration.ofMinutes(31)));

        when(appConnectionService.isConnectionOfComponent(DEPLOYMENT_CONNECTION_ID, "vercel")).thenReturn(false);
        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(expiredAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(expiredAppDeployment);
        when(appDeploymentService.updateStatus(eq(APP_DEPLOYMENT_ID), eq(AppDeploymentStatus.FAILED), any(), any()))
            .thenReturn(withStatus(buildingDeployment(Instant.now()), AppDeploymentStatus.FAILED));

        newJob(tenantServiceProvider(null)).pollDeployments();

        assertThat(fakeAppDeploymentProvider.cancelAttempts).isZero();
        assertThat(fakeAppDeploymentProvider.polledRefs).isEmpty();
    }

    @Test
    void testJobLeavesAnUnchangedDeploymentAlone() {
        AppDeployment buildingAppDeployment = buildingDeployment(Instant.now());

        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(buildingAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(buildingAppDeployment);

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.BUILDING, null);

        newJob(tenantServiceProvider(null)).pollDeployments();

        verify(appDeploymentService, never()).updateStatus(anyLong(), any(), any(), any(), any());
        verify(appDeploymentService, never()).updateStatus(anyLong(), any(), any(), any());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testJobMarksFailedAfterDeadline() {
        AppDeployment expiredAppDeployment = buildingDeployment(
            Instant.now()
                .minus(Duration.ofMinutes(31)));

        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(expiredAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(expiredAppDeployment);
        when(appDeploymentService.updateStatus(eq(APP_DEPLOYMENT_ID), eq(AppDeploymentStatus.FAILED), any(), any()))
            .thenReturn(withStatus(buildingDeployment(Instant.now()), AppDeploymentStatus.FAILED));

        newJob(tenantServiceProvider(null)).pollDeployments();

        verify(appDeploymentService).updateStatus(
            APP_DEPLOYMENT_ID, AppDeploymentStatus.FAILED, null, "The deployment did not finish within 30 minutes");
        verify(appAuditPublisher).publish(AppAuditEvent.APP_DEPLOYMENT_FAILED, APP_ID, DEPLOYMENT_AUDIT_DATA);
        verify(appInstanceService, never()).updateCurrentAppDeployment(anyLong(), anyLong());

        assertThat(fakeAppDeploymentProvider.polledRefs).isEmpty();
        assertThat(fakeAppDeploymentProvider.canceledRefs)
            .singleElement()
            .satisfies(ref -> {
                assertThat(ref.providerDeploymentId()).isEqualTo("dpl_1");
                assertThat(ref.connectionId()).isEqualTo(DEPLOYMENT_CONNECTION_ID);
            });
    }

    @Test
    void testJobDeadlineKeepsTheRowFailedWhenTheProviderCannotCancel() {
        AppDeployment expiredAppDeployment = buildingDeployment(
            Instant.now()
                .minus(Duration.ofMinutes(31)));

        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(expiredAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(expiredAppDeployment);
        when(appDeploymentService.updateStatus(eq(APP_DEPLOYMENT_ID), eq(AppDeploymentStatus.FAILED), any(), any()))
            .thenReturn(withStatus(buildingDeployment(Instant.now()), AppDeploymentStatus.FAILED));

        fakeAppDeploymentProvider.cancelFailure = new IllegalStateException("provider unavailable");

        newJob(tenantServiceProvider(null)).pollDeployments();

        assertThat(fakeAppDeploymentProvider.cancelAttempts).isEqualTo(1);

        verify(appAuditPublisher).publish(AppAuditEvent.APP_DEPLOYMENT_FAILED, APP_ID, DEPLOYMENT_AUDIT_DATA);
    }

    @Test
    void testJobDeadlineWithoutAProviderIdDoesNotCallTheProvider() {
        AppDeployment expiredAppDeployment = buildingDeployment(
            Instant.now()
                .minus(Duration.ofMinutes(31)));

        expiredAppDeployment.setProviderDeploymentId(null);
        expiredAppDeployment.setStatus(AppDeploymentStatus.QUEUED);

        AppDeployment failedAppDeployment = withStatus(buildingDeployment(Instant.now()), AppDeploymentStatus.FAILED);

        failedAppDeployment.setProviderDeploymentId(null);

        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(expiredAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(expiredAppDeployment);
        when(appDeploymentService.updateStatus(eq(APP_DEPLOYMENT_ID), eq(AppDeploymentStatus.FAILED), any(), any()))
            .thenReturn(failedAppDeployment);

        newJob(tenantServiceProvider(null)).pollDeployments();

        verify(appDeploymentService).updateStatus(
            APP_DEPLOYMENT_ID, AppDeploymentStatus.FAILED, null, "The deployment did not finish within 30 minutes");

        assertThat(fakeAppDeploymentProvider.cancelAttempts).isZero();
        assertThat(fakeAppDeploymentProvider.polledRefs).isEmpty();
    }

    @Test
    void testJobDeadlineOfAnAlreadyTerminalRowDoesNotCallTheProvider() {
        AppDeployment expiredAppDeployment = buildingDeployment(
            Instant.now()
                .minus(Duration.ofMinutes(31)));

        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(expiredAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID))
            .thenReturn(withStatus(buildingDeployment(Instant.now()), AppDeploymentStatus.READY));

        newJob(tenantServiceProvider(null)).pollDeployments();

        verify(appDeploymentService, never()).updateStatus(anyLong(), any(), any(), any());

        assertThat(fakeAppDeploymentProvider.cancelAttempts).isZero();
    }

    @Test
    void testJobSkipsFailingTenant() {
        TenantService tenantService = mock(TenantService.class);
        List<String> polledTenantIds = new ArrayList<>();
        AppDeployment buildingAppDeployment = buildingDeployment(Instant.now());

        when(tenantService.getTenantIds()).thenReturn(List.of("tenant1", "tenant2"));
        when(appDeploymentService.getNonTerminalAppDeployments()).thenAnswer(invocation -> {
            String tenantId = TenantContext.getCurrentTenantId();

            polledTenantIds.add(tenantId);

            if ("tenant1".equals(tenantId)) {
                throw new IllegalStateException("schema unavailable");
            }

            return List.of(buildingAppDeployment);
        });
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(buildingAppDeployment);

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.BUILDING, null);

        newJob(tenantServiceProvider(tenantService)).pollDeployments();

        assertThat(polledTenantIds).containsExactly("tenant1", "tenant2");
        assertThat(fakeAppDeploymentProvider.polledTenantIds).containsExactly("tenant2");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testJobKeepsPollingAfterOneDeploymentFails() {
        AppDeployment failingAppDeployment = buildingDeployment(Instant.now());
        AppDeployment otherAppDeployment = buildingDeployment(Instant.now());

        failingAppDeployment.setProviderDeploymentId("dpl_broken");
        otherAppDeployment.setId(APP_DEPLOYMENT_ID + 1);

        when(appDeploymentService.getNonTerminalAppDeployments())
            .thenReturn(List.of(failingAppDeployment, otherAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID + 1)).thenReturn(otherAppDeployment);

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.BUILDING, null);

        newJob(tenantServiceProvider(null)).pollDeployments();

        assertThat(fakeAppDeploymentProvider.polledRefs)
            .extracting(ProviderDeploymentRef::providerDeploymentId)
            .containsExactly("dpl_broken", "dpl_1");
    }

    @Test
    void testJobDoesNothingWithoutTheAppExecutor() {
        TenantService tenantService = mock(TenantService.class);

        @SuppressWarnings("unchecked")
        ObjectProvider<AsyncTaskExecutor> appExecutorProvider = mock(ObjectProvider.class);

        AppDeploymentStatusJob appDeploymentStatusJob = new AppDeploymentStatusJob(
            appExecutorProvider, applicationProperties(), appDeploymentService, appDeploymentSynchronizer,
            appInstanceService, tenantServiceProvider(tenantService));

        appDeploymentStatusJob.pollDeployments();

        verifyNoInteractions(appDeploymentService, tenantService);
    }

    @Test
    void testJobFallsBackToThirtyMinutesWhenTheTimeoutIsNotConfigured() {
        assertDeadlineFallsBackToThirtyMinutes(null);
    }

    @Test
    void testJobFallsBackToThirtyMinutesWhenTheTimeoutIsZero() {
        assertDeadlineFallsBackToThirtyMinutes(Duration.ZERO);
    }

    @Test
    void testJobFallsBackToThirtyMinutesWhenTheTimeoutIsNegative() {
        assertDeadlineFallsBackToThirtyMinutes(Duration.ofMinutes(-5));
    }

    @Test
    void testJobKeepsAnUnexpiredDeploymentWhenTheTimeoutIsNotConfigured() {
        AppDeployment recentAppDeployment = buildingDeployment(
            Instant.now()
                .minus(Duration.ofMinutes(10)));

        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(recentAppDeployment));

        newJob(tenantServiceProvider(null), null).pollDeployments();

        verify(appDeploymentService, never()).updateStatus(
            eq(APP_DEPLOYMENT_ID), eq(AppDeploymentStatus.FAILED), any(), any());
    }

    @Test
    void testJobUsesTheConfiguredTimeoutWhenItIsPositive() {
        AppDeployment appDeployment = buildingDeployment(
            Instant.now()
                .minus(Duration.ofMinutes(10)));

        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(appDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(appDeployment);
        when(appDeploymentService.updateStatus(eq(APP_DEPLOYMENT_ID), eq(AppDeploymentStatus.FAILED), any(), any()))
            .thenReturn(withStatus(buildingDeployment(Instant.now()), AppDeploymentStatus.FAILED));

        newJob(tenantServiceProvider(null), Duration.ofMinutes(5)).pollDeployments();

        verify(appDeploymentService).updateStatus(
            APP_DEPLOYMENT_ID, AppDeploymentStatus.FAILED, null, "The deployment did not finish within 5 minutes");
    }

    private void assertDeadlineFallsBackToThirtyMinutes(@Nullable Duration configuredTimeout) {
        AppDeployment expiredAppDeployment = buildingDeployment(
            Instant.now()
                .minus(Duration.ofMinutes(31)));

        when(appDeploymentService.getNonTerminalAppDeployments()).thenReturn(List.of(expiredAppDeployment));
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(expiredAppDeployment);
        when(appDeploymentService.updateStatus(eq(APP_DEPLOYMENT_ID), eq(AppDeploymentStatus.FAILED), any(), any()))
            .thenReturn(withStatus(buildingDeployment(Instant.now()), AppDeploymentStatus.FAILED));

        newJob(tenantServiceProvider(null), configuredTimeout).pollDeployments();

        verify(appDeploymentService).updateStatus(
            APP_DEPLOYMENT_ID, AppDeploymentStatus.FAILED, null, "The deployment did not finish within 30 minutes");
    }

    private AppDeploymentStatusJob newJob(ObjectProvider<TenantService> tenantServiceProvider) {
        return newJob(tenantServiceProvider, Duration.ofMinutes(30));
    }

    private AppDeploymentStatusJob newJob(
        ObjectProvider<TenantService> tenantServiceProvider, @Nullable Duration timeout) {

        @SuppressWarnings("unchecked")
        ObjectProvider<AsyncTaskExecutor> appExecutorProvider = mock(ObjectProvider.class);

        when(appExecutorProvider.getIfAvailable()).thenReturn(recordingAppExecutor);

        return new AppDeploymentStatusJob(
            appExecutorProvider, applicationProperties(timeout), appDeploymentService, appDeploymentSynchronizer,
            appInstanceService, tenantServiceProvider);
    }

    private static ApplicationProperties applicationProperties() {
        return applicationProperties(Duration.ofMinutes(30));
    }

    private static ApplicationProperties applicationProperties(@Nullable Duration timeout) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.App.Deployment deployment = applicationProperties.getApp()
            .getDeployment();

        deployment.setTimeout(timeout);

        return applicationProperties;
    }

    private double deploymentCount(String provider, String status) {
        Counter counter = meterRegistry.find("bytechef_app_deployment")
            .tag("provider", provider)
            .tag("status", status)
            .counter();

        return counter == null ? 0.0 : counter.count();
    }

    private double totalDeploymentCount() {
        return meterRegistry.find("bytechef_app_deployment")
            .counters()
            .stream()
            .mapToDouble(Counter::count)
            .sum();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MeterRegistry> meterRegistryProvider(MeterRegistry registry) {
        ObjectProvider<MeterRegistry> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(registry);

        return objectProvider;
    }

    private static AppDeployment buildingDeployment(Instant createdDate) {
        AppDeployment appDeployment = new AppDeployment();

        appDeployment.setId(APP_DEPLOYMENT_ID);
        appDeployment.setAppInstanceId(APP_INSTANCE_ID);
        appDeployment.setAppVersionId(APP_VERSION_ID);
        appDeployment.setProvider(AppDeploymentProviderType.VERCEL);
        appDeployment.setProviderDeploymentId("dpl_1");
        appDeployment.setStatus(AppDeploymentStatus.BUILDING);
        appDeployment.setCreatedBy("editor");
        appDeployment.setCreatedDate(createdDate);

        return appDeployment;
    }

    private static AppDeployment withStatus(AppDeployment appDeployment, AppDeploymentStatus status) {
        appDeployment.setStatus(status);

        return appDeployment;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<TenantService> tenantServiceProvider(@Nullable TenantService tenantService) {
        ObjectProvider<TenantService> tenantServiceProvider = mock(ObjectProvider.class);

        when(tenantServiceProvider.getIfAvailable()).thenReturn(tenantService);

        return tenantServiceProvider;
    }

    private static final class FakeAppDeploymentProvider implements AppDeploymentProvider {

        private ProviderDeployment nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.BUILDING, null);
        private final List<ProviderDeploymentRef> polledRefs = new ArrayList<>();
        private final List<String> polledTenantIds = new ArrayList<>();
        private final List<ProviderDeploymentRef> canceledRefs = new ArrayList<>();
        private int cancelAttempts;
        private @Nullable RuntimeException cancelFailure;

        @Override
        public AppHostingTarget getHostingTarget() {
            return AppHostingTarget.VERCEL;
        }

        @Override
        public String getDeploymentConnectionComponentName() {
            return "vercel";
        }

        @Override
        public ProviderDeployment deploy(ProviderDeploymentRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProviderDeployment getDeployment(ProviderDeploymentRef ref) {
            polledRefs.add(ref);
            polledTenantIds.add(TenantContext.getCurrentTenantId());

            if ("dpl_broken".equals(ref.providerDeploymentId())) {
                throw new IllegalStateException("provider unavailable");
            }

            return nextDeployment;
        }

        @Override
        public void cancel(ProviderDeploymentRef ref) {
            cancelAttempts++;

            if (cancelFailure != null) {
                throw cancelFailure;
            }

            canceledRefs.add(ref);
        }
    }

    private static final class RecordingAppExecutor implements AsyncTaskExecutor {

        private int executedTasks;

        @Override
        public void execute(Runnable task) {
            executedTasks++;

            task.run();
        }
    }
}
