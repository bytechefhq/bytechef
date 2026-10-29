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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SingleTenantServiceTest {

    private final SingleTenantService singleTenantService = new SingleTenantService();

    @Test
    void testGetTenantIdsReturnsTheDefaultTenant() {
        assertThat(singleTenantService.getTenantIds()).containsExactly(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testIsMultiTenantEnabled() {
        assertThat(singleTenantService.isMultiTenantEnabled()).isFalse();
    }
}
