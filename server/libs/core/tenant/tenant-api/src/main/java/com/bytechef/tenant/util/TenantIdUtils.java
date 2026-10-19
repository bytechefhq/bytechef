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

import com.bytechef.tenant.TenantContext;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 */
public final class TenantIdUtils {

    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private TenantIdUtils() {
    }

    public static String requireValid(String tenantId) {
        if (tenantId == null || !TENANT_ID_PATTERN.matcher(tenantId)
            .matches() || TenantContext.DEFAULT_TENANT_ID.equalsIgnoreCase(tenantId)) {

            throw new IllegalArgumentException("Invalid tenant id");
        }

        return tenantId;
    }
}
