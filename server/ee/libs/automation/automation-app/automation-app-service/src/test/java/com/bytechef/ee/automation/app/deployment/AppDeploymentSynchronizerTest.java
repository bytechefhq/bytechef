/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.AppDeployment;
import com.bytechef.ee.automation.app.domain.AppDeploymentProviderType;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppVersion;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppDeploymentService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppVersionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppDeploymentSynchronizerTest {

    private static final long APP_ID = 1L;
    private static final long APP_INSTANCE_ID = 11L;
    private static final long APP_VERSION_ID = 21L;
    private static final long APP_DEPLOYMENT_ID = 31L;
    private static final String COUNTER_NAME = "bytechef_app_deployment";

    private final AppAuditPublisher appAuditPublisher = mock(AppAuditPublisher.class);
    private final AppConnectionService appConnectionService = mock(AppConnectionService.class);
    private final AppDeploymentService appDeploymentService = mock(AppDeploymentService.class);
    private final AppInstanceService appInstanceService = mock(AppInstanceService.class);
    private final AppVersionService appVersionService = mock(AppVersionService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private AppDeploymentSynchronizer appDeploymentSynchronizer;

    @BeforeEach
    void beforeEach() {
        appDeploymentSynchronizer = createSynchronizer(meterRegistry);

        AppInstance appInstance = new AppInstance();

        appInstance.setId(APP_INSTANCE_ID);
        appInstance.setAppId(APP_ID);

        AppVersion appVersion = new AppVersion();

        appVersion.setId(APP_VERSION_ID);
        appVersion.setAppId(APP_ID);
        appVersion.setName("1.2");

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);
        when(appVersionService.getAppVersion(APP_VERSION_ID)).thenReturn(appVersion);
    }

    @AfterEach
    void afterEach() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void testProviderReadyCountsReadyOnce() {
        stubStored(AppDeploymentStatus.BUILDING);

        when(appDeploymentService.updateStatus(
            APP_DEPLOYMENT_ID, "dpl_1", AppDeploymentStatus.READY, null, null))
                .thenReturn(deployment(AppDeploymentStatus.READY));

        appDeploymentSynchronizer.applyProviderDeployment(
            APP_DEPLOYMENT_ID, new ProviderDeployment("dpl_1", AppDeploymentStatus.READY, null));

        assertThat(count("VERCEL", "READY")).isEqualTo(1.0);
        assertThat(totalCount()).isEqualTo(1.0);
    }

    @Test
    void testProviderFailedCountsFailedOnce() {
        stubStored(AppDeploymentStatus.BUILDING);

        when(appDeploymentService.updateStatus(
            eq(APP_DEPLOYMENT_ID), eq("dpl_1"), eq(AppDeploymentStatus.FAILED), any(), any()))
                .thenReturn(deployment(AppDeploymentStatus.FAILED));

        appDeploymentSynchronizer.applyProviderDeployment(
            APP_DEPLOYMENT_ID, new ProviderDeployment("dpl_1", AppDeploymentStatus.FAILED, null));

        assertThat(count("VERCEL", "FAILED")).isEqualTo(1.0);
        assertThat(totalCount()).isEqualTo(1.0);
    }

    @Test
    void testProviderCanceledCountsCanceledOnce() {
        stubStored(AppDeploymentStatus.BUILDING);

        when(appDeploymentService.updateStatus(
            APP_DEPLOYMENT_ID, "dpl_1", AppDeploymentStatus.CANCELED, null, null))
                .thenReturn(deployment(AppDeploymentStatus.CANCELED));

        appDeploymentSynchronizer.applyProviderDeployment(
            APP_DEPLOYMENT_ID, new ProviderDeployment("dpl_1", AppDeploymentStatus.CANCELED, null));

        assertThat(count("VERCEL", "CANCELED")).isEqualTo(1.0);
        assertThat(totalCount()).isEqualTo(1.0);
    }

    @Test
    void testProviderProgressWithoutATerminalStatusCountsNothing() {
        stubStored(AppDeploymentStatus.QUEUED);

        when(appDeploymentService.updateStatus(
            APP_DEPLOYMENT_ID, "dpl_1", AppDeploymentStatus.BUILDING, null, null))
                .thenReturn(deployment(AppDeploymentStatus.BUILDING));

        appDeploymentSynchronizer.applyProviderDeployment(
            APP_DEPLOYMENT_ID, new ProviderDeployment("dpl_1", AppDeploymentStatus.BUILDING, null));

        assertThat(totalCount()).isZero();
    }

    @Test
    void testAnAlreadyTerminalRowIsNeverCountedAgain() {
        stubStored(AppDeploymentStatus.READY);

        appDeploymentSynchronizer.applyProviderDeployment(
            APP_DEPLOYMENT_ID, new ProviderDeployment("dpl_1", AppDeploymentStatus.READY, null));
        appDeploymentSynchronizer.applyProviderDeployment(
            APP_DEPLOYMENT_ID, new ProviderDeployment("dpl_1", AppDeploymentStatus.FAILED, null));

        assertThat(appDeploymentSynchronizer.fail(APP_DEPLOYMENT_ID, "late")).isEmpty();
        assertThat(totalCount()).isZero();
    }

    @Test
    void testCancelCountsCanceledOnceAndAnUnhandledCancelCountsNothing() {
        when(appDeploymentService.cancel(APP_DEPLOYMENT_ID, "dpl_1"))
            .thenReturn(Optional.of(deployment(AppDeploymentStatus.CANCELED)));
        when(appDeploymentService.cancel(APP_DEPLOYMENT_ID, "other")).thenReturn(Optional.empty());

        assertThat(appDeploymentSynchronizer.cancel(APP_DEPLOYMENT_ID, "other")).isEmpty();
        assertThat(totalCount()).isZero();

        assertThat(appDeploymentSynchronizer.cancel(APP_DEPLOYMENT_ID, "dpl_1")).isPresent();
        assertThat(count("VERCEL", "CANCELED")).isEqualTo(1.0);
        assertThat(totalCount()).isEqualTo(1.0);
    }

    @Test
    void testFailCountsFailedOnce() {
        stubStored(AppDeploymentStatus.QUEUED);

        when(appDeploymentService.updateStatus(
            APP_DEPLOYMENT_ID, AppDeploymentStatus.FAILED, null, "boom"))
                .thenReturn(deployment(AppDeploymentStatus.FAILED));

        assertThat(appDeploymentSynchronizer.fail(APP_DEPLOYMENT_ID, "boom")).isPresent();

        assertThat(count("VERCEL", "FAILED")).isEqualTo(1.0);
        assertThat(totalCount()).isEqualTo(1.0);
    }

    @Test
    void testMarkCurrentCountsReadyOnceForARecordedDeployment() {
        AppVersion appVersion = appVersionService.getAppVersion(APP_VERSION_ID);

        appDeploymentSynchronizer.markCurrent(APP_ID, deployment(AppDeploymentStatus.READY), appVersion);

        assertThat(count("VERCEL", "READY")).isEqualTo(1.0);
        assertThat(totalCount()).isEqualTo(1.0);
    }

    @Test
    void testAFailedWriteIsNotCountedAndTheRetryIsCountedOnce() {
        stubStored(AppDeploymentStatus.QUEUED);

        when(appDeploymentService.updateStatus(
            APP_DEPLOYMENT_ID, AppDeploymentStatus.FAILED, null, "boom"))
                .thenThrow(new OptimisticLockingFailureException("stale"))
                .thenReturn(deployment(AppDeploymentStatus.FAILED));

        assertThatThrownBy(() -> appDeploymentSynchronizer.fail(APP_DEPLOYMENT_ID, "boom"))
            .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(totalCount()).isZero();

        appDeploymentSynchronizer.fail(APP_DEPLOYMENT_ID, "boom");

        assertThat(count("VERCEL", "FAILED")).isEqualTo(1.0);
        assertThat(totalCount()).isEqualTo(1.0);
    }

    @Test
    void testInsideATransactionTheCountWaitsForTheCommit() {
        stubStored(AppDeploymentStatus.QUEUED);

        when(appDeploymentService.updateStatus(
            APP_DEPLOYMENT_ID, AppDeploymentStatus.FAILED, null, "boom"))
                .thenReturn(deployment(AppDeploymentStatus.FAILED));

        TransactionSynchronizationManager.initSynchronization();

        appDeploymentSynchronizer.fail(APP_DEPLOYMENT_ID, "boom");

        assertThat(totalCount()).isZero();

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();

        for (TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCommit();
        }

        assertThat(count("VERCEL", "FAILED")).isEqualTo(1.0);
        assertThat(totalCount()).isEqualTo(1.0);
    }

    @Test
    void testARolledBackTransactionCountsNothing() {
        stubStored(AppDeploymentStatus.QUEUED);

        when(appDeploymentService.updateStatus(
            APP_DEPLOYMENT_ID, AppDeploymentStatus.FAILED, null, "boom"))
                .thenReturn(deployment(AppDeploymentStatus.FAILED));

        TransactionSynchronizationManager.initSynchronization();

        appDeploymentSynchronizer.fail(APP_DEPLOYMENT_ID, "boom");

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        assertThat(totalCount()).isZero();
    }

    @Test
    void testTheCounterCarriesOnlyTheProviderAndStatusTags() {
        stubStored(AppDeploymentStatus.QUEUED);

        when(appDeploymentService.updateStatus(
            APP_DEPLOYMENT_ID, AppDeploymentStatus.FAILED, null, "boom"))
                .thenReturn(deployment(AppDeploymentStatus.FAILED));

        appDeploymentSynchronizer.fail(APP_DEPLOYMENT_ID, "boom");

        Counter counter = meterRegistry.get(COUNTER_NAME)
            .counter();

        assertThat(counter.getId()
            .getTags()).extracting(tag -> tag.getKey())
                .containsExactlyInAnyOrder("provider", "status");
    }

    @Test
    void testWithoutAMeterRegistryTerminalWritesStillWork() {
        AppDeploymentSynchronizer synchronizerWithoutRegistry = createSynchronizer(null);

        stubStored(AppDeploymentStatus.QUEUED);

        when(appDeploymentService.updateStatus(
            APP_DEPLOYMENT_ID, AppDeploymentStatus.FAILED, null, "boom"))
                .thenReturn(deployment(AppDeploymentStatus.FAILED));

        assertThat(synchronizerWithoutRegistry.fail(APP_DEPLOYMENT_ID, "boom")).isPresent();
        assertThat(totalCount()).isZero();
    }

    @Test
    void testAFailingMeterRegistryLeavesTheWriteAndItsAuditIntactInAndOutsideATransaction() {
        MeterRegistry throwingRegistry = new SimpleMeterRegistry() {

            @Override
            protected Counter newCounter(Meter.Id id) {
                throw new IllegalStateException("registration clash");
            }
        };

        AppDeploymentSynchronizer synchronizerWithThrowingRegistry = createSynchronizer(throwingRegistry);

        stubStored(AppDeploymentStatus.QUEUED);

        when(appDeploymentService.updateStatus(
            APP_DEPLOYMENT_ID, AppDeploymentStatus.FAILED, null, "boom"))
                .thenReturn(deployment(AppDeploymentStatus.FAILED));

        assertThat(synchronizerWithThrowingRegistry.fail(APP_DEPLOYMENT_ID, "boom")).isPresent();

        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_DEPLOYMENT_FAILED), eq(APP_ID), any());

        TransactionSynchronizationManager.initSynchronization();

        assertThat(synchronizerWithThrowingRegistry.fail(APP_DEPLOYMENT_ID, "boom")).isPresent();

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            assertThatCode(synchronization::afterCommit).doesNotThrowAnyException();
        }
    }

    private AppDeploymentSynchronizer createSynchronizer(@Nullable MeterRegistry registry) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        return new AppDeploymentSynchronizer(
            appAuditPublisher, beanFactory.getBeanProvider(AppDeploymentProvider.class), appDeploymentService,
            appConnectionService, appInstanceService, appVersionService, provider(registry));
    }

    private void stubStored(AppDeploymentStatus status) {
        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(deployment(status));
    }

    private double count(String provider, String status) {
        Counter counter = meterRegistry.find(COUNTER_NAME)
            .tag("provider", provider)
            .tag("status", status)
            .counter();

        return counter == null ? 0.0 : counter.count();
    }

    private double totalCount() {
        return meterRegistry.find(COUNTER_NAME)
            .counters()
            .stream()
            .mapToDouble(Counter::count)
            .sum();
    }

    private static AppDeployment deployment(AppDeploymentStatus status) {
        AppDeployment appDeployment = new AppDeployment();

        appDeployment.setId(APP_DEPLOYMENT_ID);
        appDeployment.setAppInstanceId(APP_INSTANCE_ID);
        appDeployment.setAppVersionId(APP_VERSION_ID);
        appDeployment.setProvider(AppDeploymentProviderType.VERCEL);
        appDeployment.setProviderDeploymentId("dpl_1");
        appDeployment.setStatus(status);

        return appDeployment;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(@Nullable T bean) {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(bean);

        return objectProvider;
    }
}
