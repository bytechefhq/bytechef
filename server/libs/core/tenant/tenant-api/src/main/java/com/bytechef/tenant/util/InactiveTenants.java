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

package com.bytechef.tenant.util;

import com.bytechef.tenant.service.TenantDeletionService;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Single entry for the "is this tenant pending deletion" check. An absent {@link TenantDeletionService} bean
 * (single-tenant and distributed apps) means every tenant is active.
 *
 * @author Ivica Cardic
 */
public final class InactiveTenants {

    private InactiveTenants() {
    }

    public static boolean isInactive(ObjectProvider<TenantDeletionService> provider, String tenantId) {
        TenantDeletionService tenantDeletionService = provider.getIfAvailable();

        return tenantDeletionService != null && tenantDeletionService.isInactive(tenantId);
    }
}
