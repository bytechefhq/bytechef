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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.tenant.TenantContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
class TenantIdValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
        TenantContext.DEFAULT_TENANT_ID, "000001", "test_tenant", "tenant-b", "Tenant1"
    })
    void testIsValidAcceptsAValidTenantId(String tenantId) {
        assertThat(TenantIdValidator.isValid(tenantId)).isTrue();
        assertThat(TenantIdValidator.validate(tenantId)).isEqualTo(tenantId);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "a b", "x;y", "bad'id", "a\"b", "a.b", "a:b", "a,b", "tenant\n"
    })
    void testValidateRejectsAnInvalidTenantId(String tenantId) {
        assertThat(TenantIdValidator.isValid(tenantId)).isFalse();

        assertThatThrownBy(() -> TenantIdValidator.validate(tenantId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {
        64, 200
    })
    void testValidateRejectsATooLongTenantId(int length) {
        String tenantId = "1".repeat(length);

        assertThatThrownBy(() -> TenantIdValidator.validate(tenantId))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
