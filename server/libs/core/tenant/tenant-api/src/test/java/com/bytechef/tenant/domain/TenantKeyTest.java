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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.EncodingUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
class TenantKeyTest {

    @Test
    void testParseReadsTheTenantIdOfAGeneratedKey() {
        TenantKey tenantKey = TenantKey.of("000005");

        TenantKey parsedTenantKey = TenantKey.parse(tenantKey.getKey());

        assertThat(parsedTenantKey.getTenantId()).isEqualTo("000005");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "a b", "x;y", "bad'id", ""
    })
    void testParseRejectsAnInvalidTenantId(String tenantId) {
        String tenantKeyString = EncodingUtils.base64EncodeToString(tenantId + ":c2VjcmV0");

        assertThatThrownBy(() -> TenantKey.parse(tenantKeyString))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
