/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.quartz.impl.jdbcjobstore.DriverDelegate;
import org.quartz.impl.jdbcjobstore.JobStoreSupport;
import org.quartz.impl.jdbcjobstore.Semaphore;

/**
 * @version ee
 */
class MultiTenantLocalDataSourceJobStoreTest {

    private final MultiTenantLocalDataSourceJobStore multiTenantLocalDataSourceJobStore =
        new MultiTenantLocalDataSourceJobStore();

    @Test
    void testGetDelegateReturnsSameWrapperOnEveryCall() throws Exception {
        DriverDelegate driverDelegate = multiTenantLocalDataSourceJobStore.getDelegate();

        assertThat(driverDelegate).isInstanceOf(MultiTenantDriverDelegate.class);
        assertThat(multiTenantLocalDataSourceJobStore.getDelegate()).isSameAs(driverDelegate);
        assertThat(multiTenantLocalDataSourceJobStore.getDelegate()).isSameAs(driverDelegate);
    }

    @Test
    void testGetDelegateWrapsCurrentUnderlyingDelegateAfterItIsReplaced() throws Exception {
        DriverDelegate firstDriverDelegate = multiTenantLocalDataSourceJobStore.getDelegate();

        DriverDelegate replacementDriverDelegate = mock(DriverDelegate.class);

        when(replacementDriverDelegate.isUsingEnhancedStatements()).thenReturn(true);

        setUnderlyingDelegate(replacementDriverDelegate);

        DriverDelegate secondDriverDelegate = multiTenantLocalDataSourceJobStore.getDelegate();

        assertThat(secondDriverDelegate)
            .isInstanceOf(MultiTenantDriverDelegate.class)
            .isNotSameAs(firstDriverDelegate);
        assertThat(secondDriverDelegate.isUsingEnhancedStatements()).isTrue();
        assertThat(multiTenantLocalDataSourceJobStore.getDelegate()).isSameAs(secondDriverDelegate);

        verify(replacementDriverDelegate).isUsingEnhancedStatements();
    }

    @Test
    void testGetDelegateWrapsNewlyCreatedUnderlyingDelegateAfterItIsReset() throws Exception {
        DriverDelegate firstDriverDelegate = multiTenantLocalDataSourceJobStore.getDelegate();

        setUnderlyingDelegate(null);

        DriverDelegate secondDriverDelegate = multiTenantLocalDataSourceJobStore.getDelegate();

        assertThat(secondDriverDelegate)
            .isInstanceOf(MultiTenantDriverDelegate.class)
            .isNotSameAs(firstDriverDelegate);
        assertThat(multiTenantLocalDataSourceJobStore.getDelegate()).isSameAs(secondDriverDelegate);
    }

    @Test
    void testConcurrentGetDelegateCallsShareOneWrapper() throws Exception {
        int threadCount = 8;

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        try {
            List<Future<DriverDelegate>> futures = new ArrayList<>();
            Callable<DriverDelegate> getDelegateCall = () -> {
                assertThat(startLatch.await(10, TimeUnit.SECONDS)).isTrue();

                return multiTenantLocalDataSourceJobStore.getDelegate();
            };

            for (int index = 0; index < threadCount; index++) {
                futures.add(executorService.submit(getDelegateCall));
            }

            startLatch.countDown();

            DriverDelegate expectedDriverDelegate = futures.getFirst()
                .get(10, TimeUnit.SECONDS);

            for (Future<DriverDelegate> future : futures) {
                assertThat(future.get(10, TimeUnit.SECONDS)).isSameAs(expectedDriverDelegate);
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void testGetLockHandlerReturnsSameWrapperUntilLockHandlerIsReplaced() throws Exception {
        assertThat(multiTenantLocalDataSourceJobStore.getLockHandler()).isNull();

        Semaphore firstSemaphore = mock(Semaphore.class);

        multiTenantLocalDataSourceJobStore.setLockHandler(firstSemaphore);

        Semaphore firstLockHandler = multiTenantLocalDataSourceJobStore.getLockHandler();

        assertThat(firstLockHandler).isInstanceOf(MultiTenantSemaphore.class);
        assertThat(multiTenantLocalDataSourceJobStore.getLockHandler()).isSameAs(firstLockHandler);

        Semaphore secondSemaphore = mock(Semaphore.class);

        when(secondSemaphore.requiresConnection()).thenReturn(true);

        multiTenantLocalDataSourceJobStore.setLockHandler(secondSemaphore);

        Semaphore secondLockHandler = multiTenantLocalDataSourceJobStore.getLockHandler();

        assertThat(secondLockHandler)
            .isInstanceOf(MultiTenantSemaphore.class)
            .isNotSameAs(firstLockHandler);
        assertThat(secondLockHandler.requiresConnection()).isTrue();
        assertThat(multiTenantLocalDataSourceJobStore.getLockHandler()).isSameAs(secondLockHandler);

        multiTenantLocalDataSourceJobStore.setLockHandler(null);

        assertThat(multiTenantLocalDataSourceJobStore.getLockHandler()).isNull();
    }

    /**
     * Replaces the delegate {@code JobStoreSupport} created, as a re-initialization of the job store would.
     */
    private void setUnderlyingDelegate(DriverDelegate driverDelegate) throws ReflectiveOperationException {
        Field delegateField = JobStoreSupport.class.getDeclaredField("delegate");

        delegateField.setAccessible(true);

        delegateField.set(multiTenantLocalDataSourceJobStore, driverDelegate);
    }
}
