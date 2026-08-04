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

package com.bytechef.tenant.single;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.bytechef.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins the tenant-enumeration contract every per-tenant scheduled sweep relies on ({@code JobTimeoutMonitor},
 * {@code JobRetentionMonitor}, the approval monitors, {@code AssetFileOrphanBlobCleaner}, ...): in single-tenant mode
 * the deployment has exactly one tenant — the default one — so {@code getTenantIds()} must return it rather than throw.
 * Throwing here broke every sweep on every single-tenant deployment (the default mode) with
 * {@code UnsupportedOperationException} each cycle.
 *
 * @author Ivica Cardic
 */
class SingleTenantServiceTest {

    private final SingleTenantService singleTenantService = new SingleTenantService();

    @Test
    void testGetTenantIdsReturnsTheDefaultTenant() {
        assertEquals(List.of(TenantContext.DEFAULT_TENANT_ID), singleTenantService.getTenantIds());
    }

    @Test
    void testMultiTenantIsDisabled() {
        assertFalse(singleTenantService.isMultiTenantEnabled());
    }
}
