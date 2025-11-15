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

package com.bytechef.tenant.domain;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.RandomUtils;
import com.bytechef.tenant.TenantContext;

public class TenantKey {

    private final String key;
    private final String tenantId;

    private TenantKey(String key, String tenantId) {
        this.key = key;
        this.tenantId = tenantId;
    }

    public static TenantKey of() {
        String tenantId = TenantContext.getCurrentTenantId();

        return new TenantKey(
            EncodingUtils.base64EncodeToString(
                tenantId + ":" + EncodingUtils.base64EncodeToString(RandomUtils.nextBytes(24))),
            tenantId);
    }

    public static TenantKey parse(String tenantKeyString) {
        tenantKeyString = EncodingUtils.base64DecodeToString(tenantKeyString);

        String[] items = tenantKeyString.split(":");

        return new TenantKey(tenantKeyString, items[0]);
    }

    public String getKey() {
        return key;
    }

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String toString() {
        return key;
    }
}
