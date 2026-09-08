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

package com.bytechef.tenant.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.tenant.TenantContext;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TenantTaskDecoratorTest {

    private final TenantTaskDecorator tenantTaskDecorator = new TenantTaskDecorator();

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testRunsUnderSubmitterTenantAndRestoresExecutorTenant() {
        AtomicReference<String> tenantIdSeenByTask = new AtomicReference<>();

        TenantContext.setCurrentTenantId("submitter");

        Runnable decoratedRunnable = tenantTaskDecorator.decorate(
            () -> tenantIdSeenByTask.set(TenantContext.getCurrentTenantId()));

        TenantContext.setCurrentTenantId("executor");

        decoratedRunnable.run();

        assertEquals("submitter", tenantIdSeenByTask.get());
        assertEquals("executor", TenantContext.getCurrentTenantId());
    }

    @Test
    void testRestoresExecutorTenantWhenTaskThrows() {
        TenantContext.setCurrentTenantId("submitter");

        Runnable decoratedRunnable = tenantTaskDecorator.decorate(() -> {
            throw new IllegalStateException("boom");
        });

        TenantContext.setCurrentTenantId("executor");

        try {
            decoratedRunnable.run();
        } catch (IllegalStateException illegalStateException) {
            assertEquals("boom", illegalStateException.getMessage());
        }

        assertEquals("executor", TenantContext.getCurrentTenantId());
    }
}
