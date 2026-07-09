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

import io.micrometer.context.ThreadLocalAccessor;

/**
 * Bridges the thread-bound {@link TenantContext} into the Micrometer {@code ContextRegistry} so Reactor's automatic
 * context propagation carries the tenant across reactive thread hops (e.g. Spring AI advisor chains scheduled on
 * {@code Schedulers.boundedElastic()}, where tenant-scoped lookups such as AI provider/API-key resolution run). Without
 * this, those hops fall back to {@link TenantContext#DEFAULT_TENANT_ID}, resolving the wrong tenant's data.
 *
 * @author Ivica Cardic
 */
public class TenantContextThreadLocalAccessor implements ThreadLocalAccessor<String> {

    public static final String KEY = "bytechef.tenant";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public String getValue() {
        return TenantContext.getCurrentTenantId();
    }

    @Override
    public void setValue(String value) {
        TenantContext.setCurrentTenantId(value);
    }

    @Override
    public void setValue() {
        TenantContext.resetCurrentTenantId();
    }
}
