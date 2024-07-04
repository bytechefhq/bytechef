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

package com.bytechef.platform.user.jwt;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.tenant.TenantContext;

/**
 * @author Ivica Cardic
 */
public class JwtKeyId {

    private final long signingKeyId;
    private final String tenantId;

    private JwtKeyId(long signingKeyId, String tenantId) {
        this.signingKeyId = signingKeyId;
        this.tenantId = tenantId;
    }

    public static JwtKeyId of(long signingKeyId) {
        return new JwtKeyId(signingKeyId, TenantContext.getCurrentTenantId());
    }

    public static JwtKeyId parse(String id) {
        id = EncodingUtils.decodeBase64ToString(id);

        String[] items = id.split(":");

        return new JwtKeyId(Long.parseLong(items[0]), items[1]);
    }

    @Override
    public String toString() {
        return EncodingUtils.encodeBase64ToString(tenantId + signingKeyId);
    }
}
