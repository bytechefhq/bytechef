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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TenantIdValidatorTest {

    @Test
    void testValidTenantIds() {
        assertThat(TenantIdValidator.isValid("public")).isTrue();
        assertThat(TenantIdValidator.isValid("000001")).isTrue();
        assertThat(TenantIdValidator.isValid("tenant_1")).isTrue();
    }

    @Test
    void testInvalidTenantIds() {
        assertThat(TenantIdValidator.isValid("public\"; DROP TABLE users; --")).isFalse();
        assertThat(TenantIdValidator.isValid("a b")).isFalse();
        assertThat(TenantIdValidator.isValid("a;b")).isFalse();
        assertThat(TenantIdValidator.isValid("a-b")).isFalse();
        assertThat(TenantIdValidator.isValid("")).isFalse();
        assertThat(TenantIdValidator.isValid(null)).isFalse();
    }

    @Test
    void testValidateThrowsOnInjection() {
        assertThatThrownBy(() -> TenantIdValidator.validate("public\"; DROP TABLE users; --"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testValidatePassesForValid() {
        assertThatCode(() -> TenantIdValidator.validate("000001")).doesNotThrowAnyException();
    }

    @Test
    void testValidateDatabaseSchema() {
        assertThatCode(() -> TenantIdValidator.validateDatabaseSchema("bytechef_000001")).doesNotThrowAnyException();
        assertThatCode(() -> TenantIdValidator.validateDatabaseSchema("public")).doesNotThrowAnyException();
        assertThatThrownBy(() -> TenantIdValidator.validateDatabaseSchema("bytechef_x\"; DROP"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
