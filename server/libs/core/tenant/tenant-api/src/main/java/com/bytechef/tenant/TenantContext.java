/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.tenant.constant.TenantConstants;
import java.util.Objects;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class TenantContext {

    private static final String DEFAULT_TENANT_ID = "public";

    private static final ThreadLocal<String> currentTenant = ThreadLocal.withInitial(() -> DEFAULT_TENANT_ID);

    public static String getCurrentDatabaseSchema() {
        return Objects.equals(getCurrentTenantId(), DEFAULT_TENANT_ID)
            ? DEFAULT_TENANT_ID
            : TenantConstants.TENANT_PREFIX + "_" + getCurrentTenantId();
    }

    public static String getCurrentTenantId() {
        return currentTenant.get();
    }

    public static void resetCurrentTenantId() {
        setCurrentTenantId(DEFAULT_TENANT_ID);
    }

    public static void setCurrentTenantId(String tenantId) {
        Assert.notNull(tenantId, "tenantId must not be null");

        currentTenant.set(tenantId);
    }
}
