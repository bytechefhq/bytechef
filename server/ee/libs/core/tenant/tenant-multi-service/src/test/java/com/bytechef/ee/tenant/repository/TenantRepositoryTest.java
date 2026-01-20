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

package com.bytechef.ee.tenant.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TenantRepositoryTest {

    private TenantRepository tenantRepository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = mock(DataSource.class);

        tenantRepository = new TenantRepository(dataSource);
    }

    @Test
    void testCreateTenantWithValidId() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant123"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("null"); // Expected: DB operation fails due to mock
    }

    @Test
    void testCreateTenantWithAlphanumericId() {
        assertThatThrownBy(() -> tenantRepository.createTenant("Tenant123ABC"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("null");
    }

    @Test
    void testCreateTenantWithUnderscoreId() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant_123"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("null");
    }

    @Test
    void testCreateTenantWithHyphenId() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant-123"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("null");
    }

    @Test
    void testCreateTenantWithSqlInjection() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant'; DROP SCHEMA bytechef_000001; --"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }

    @Test
    void testCreateTenantWithSqlInjectionInDropSchema() {
        assertThatThrownBy(() -> tenantRepository.createTenant("000001; DROP TABLE users; --"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }

    @Test
    void testCreateTenantWithSpaces() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant 123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }

    @Test
    void testCreateTenantWithSpecialCharacters() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant$123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }

    @Test
    void testCreateTenantWithQuotes() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant'123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }

    @Test
    void testCreateTenantWithDoubleQuotes() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant\"123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }

    @Test
    void testCreateTenantWithNull() {
        assertThatThrownBy(() -> tenantRepository.createTenant(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }

    @Test
    void testCreateTenantWithEmpty() {
        assertThatThrownBy(() -> tenantRepository.createTenant(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }

    @Test
    void testDeleteTenantWithValidId() {
        assertThatThrownBy(() -> tenantRepository.deleteTenant("tenant123"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("null");
    }

    @Test
    void testDeleteTenantWithSqlInjection() {
        assertThatThrownBy(() -> tenantRepository.deleteTenant("tenant'; DROP DATABASE bytechef; --"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }

    @Test
    void testDeleteTenantWithSemicolon() {
        assertThatThrownBy(() -> tenantRepository.deleteTenant("tenant123;"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }

    @Test
    void testDeleteTenantWithParentheses() {
        assertThatThrownBy(() -> tenantRepository.deleteTenant("tenant()"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }

    @Test
    void testDeleteTenantWithNull() {
        assertThatThrownBy(() -> tenantRepository.deleteTenant(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }
}
