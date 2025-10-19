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

    @Override
    protected DriverDelegate getDelegate() throws NoSuchDelegateException {
        return new MultiTenantDriverDelegate(super.getDelegate());
    }

    @Override
    protected Semaphore getLockHandler() {
        Semaphore semaphore = super.getLockHandler();

        if (semaphore == null) {
            return null;
        }

        return new MultiTenantSemaphore(semaphore);
    }
}
