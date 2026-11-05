/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import org.quartz.impl.jdbcjobstore.DriverDelegate;
import org.quartz.impl.jdbcjobstore.NoSuchDelegateException;
import org.quartz.impl.jdbcjobstore.Semaphore;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiTenantLocalDataSourceJobStore extends LocalDataSourceJobStore {

    private MultiTenantDriverDelegate multiTenantDriverDelegate;
    private MultiTenantSemaphore multiTenantSemaphore;

    @Override
    protected DriverDelegate getDelegate() throws NoSuchDelegateException {
        synchronized (this) {
            DriverDelegate driverDelegate = super.getDelegate();

            if (multiTenantDriverDelegate == null || !multiTenantDriverDelegate.wraps(driverDelegate)) {
                multiTenantDriverDelegate = new MultiTenantDriverDelegate(driverDelegate);
            }

            return multiTenantDriverDelegate;
        }
    }

    @Override
    protected Semaphore getLockHandler() {
        synchronized (this) {
            Semaphore semaphore = super.getLockHandler();

            if (semaphore == null) {
                return null;
            }

            if (multiTenantSemaphore == null || !multiTenantSemaphore.wraps(semaphore)) {
                multiTenantSemaphore = new MultiTenantSemaphore(semaphore);
            }

            return multiTenantSemaphore;
        }
    }
}
