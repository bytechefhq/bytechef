/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.scheduler.tenant;

import org.quartz.impl.jdbcjobstore.DriverDelegate;
import org.quartz.impl.jdbcjobstore.NoSuchDelegateException;
import org.quartz.impl.jdbcjobstore.Semaphore;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;

/**
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
