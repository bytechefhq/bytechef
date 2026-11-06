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

package com.bytechef.platform.scheduler.db.task;

import com.bytechef.tenant.TenantContext;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class ContextBindingTest {

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRunBindsTenantAndSystemAuthentication() {
        AtomicReference<String> observedTenantId = new AtomicReference<>();
        AtomicReference<Object> observedAuthentication = new AtomicReference<>();

        ContextBinding.run("000042", () -> {
            observedTenantId.set(TenantContext.getCurrentTenantId());
            observedAuthentication.set(SecurityContextHolder.getContext()
                .getAuthentication());
        });

        Assertions.assertThat(observedTenantId.get())
            .isEqualTo("000042");
        Assertions.assertThat(observedAuthentication.get())
            .isNotNull();
    }

    @Test
    void testRunRestoresTenantAfterCompletion() {
        ContextBinding.run("000042", () -> {});

        Assertions.assertThat(TenantContext.getCurrentTenantId())
            .isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testRunRestoresTenantWhenRunnableThrows() {
        Assertions.assertThatThrownBy(() -> ContextBinding.run("000042", () -> {
            throw new IllegalStateException("boom");
        }))
            .hasRootCauseInstanceOf(IllegalStateException.class);

        Assertions.assertThat(TenantContext.getCurrentTenantId())
            .isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        Assertions.assertThat(SecurityContextHolder.getContext()
            .getAuthentication())
            .isNull();
    }

    @Test
    void testCallReturnsSupplierValue() {
        Integer result = ContextBinding.call("000042", () -> 7);

        Assertions.assertThat(result)
            .isEqualTo(7);
    }
}
