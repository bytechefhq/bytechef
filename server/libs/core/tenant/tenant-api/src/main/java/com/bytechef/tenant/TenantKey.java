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

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.RandomUtils;

public class TenantKey {

    private final String tenantId;

    private TenantKey(String tenantId) {
        this.tenantId = tenantId;
    }

    public static TenantKey of() {
        return new TenantKey(TenantContext.getCurrentTenantId());
    }

    public static TenantKey parse(String secretKey) {
        secretKey = EncodingUtils.base64DecodeToString(secretKey);

        String[] items = secretKey.split(":");

        return new TenantKey(items[0]);
    }

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String toString() {
        return EncodingUtils.base64EncodeToString(
            tenantId + ":" + EncodingUtils.base64EncodeToString(RandomUtils.nextBytes(24)));
    }
}
