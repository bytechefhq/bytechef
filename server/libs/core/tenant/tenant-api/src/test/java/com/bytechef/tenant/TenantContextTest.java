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

package com.bytechef.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
class TenantContextTest {

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testDefaultTenantIdIsValid() {
        TenantContext.setCurrentTenantId(TenantContext.DEFAULT_TENANT_ID);

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        assertThat(TenantContext.getCurrentDatabaseSchema()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testSetCurrentTenantIdAcceptsAValidTenantId() {
        TenantContext.setCurrentTenantId("000004");

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("000004");
        assertThat(TenantContext.getCurrentDatabaseSchema()).isEqualTo("bytechef_000004");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "a b", "x;y", "bad'id", "a\"b"
    })
    void testSetCurrentTenantIdRejectsAnInvalidTenantId(String tenantId) {
        TenantContext.setCurrentTenantId("000004");

        assertThatThrownBy(() -> TenantContext.setCurrentTenantId(tenantId))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("000004");
    }

    @Test
    void testSetCurrentTenantIdRejectsANullTenantId() {
        assertThatThrownBy(() -> TenantContext.setCurrentTenantId(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCallWithTenantIdRejectsAnInvalidTenantIdWithoutCalling() {
        AtomicBoolean called = new AtomicBoolean();

        assertThatThrownBy(() -> TenantContext.callWithTenantId("x;y", () -> called.getAndSet(true)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(called).isFalse();
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testRunWithTenantIdRejectsAnInvalidTenantIdWithoutRunning() {
        AtomicBoolean called = new AtomicBoolean();

        assertThatThrownBy(() -> TenantContext.runWithTenantId("bad'id", () -> called.set(true)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(called).isFalse();
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testCallWithTenantIdSwitchesAndRestoresTheTenant() {
        String tenantId = TenantContext.callWithTenantId("000002", TenantContext::getCurrentTenantId);

        assertThat(tenantId).isEqualTo("000002");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }
}
