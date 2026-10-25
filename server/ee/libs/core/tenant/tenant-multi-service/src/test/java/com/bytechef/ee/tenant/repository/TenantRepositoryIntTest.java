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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.bytechef.tenant.domain.Tenant;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class TenantRepositoryIntTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    private PGSimpleDataSource dataSource;
    private TenantRepository tenantRepository;

    @BeforeEach
    void beforeEach() throws SQLException {
        dataSource = new PGSimpleDataSource();

        dataSource.setURL(postgreSQLContainer.getJdbcUrl());
        dataSource.setUser(postgreSQLContainer.getUsername());
        dataSource.setPassword(postgreSQLContainer.getPassword());

        execute(
            "CREATE SCHEMA bytechef_000001",
            "CREATE TABLE bytechef_000001.\"user\" (email VARCHAR, login VARCHAR, activation_key VARCHAR, " +
                "reset_key VARCHAR)",
            "INSERT INTO bytechef_000001.\"user\" VALUES ('admin@example.com', 'admin', 'activation', 'reset')",
            "CREATE TABLE bytechef_000001.organization (name VARCHAR)",
            "INSERT INTO bytechef_000001.organization VALUES ('Acme')",
            "CREATE TABLE bytechef_000001.company (id VARCHAR, name VARCHAR)",
            "INSERT INTO bytechef_000001.company VALUES ('1', 'Acme')",
            "CREATE SCHEMA bytechef_000002");

        tenantRepository = new TenantRepository(dataSource);
    }

    @AfterEach
    void afterEach() throws SQLException {
        execute("DROP SCHEMA bytechef_000001 CASCADE", "DROP SCHEMA bytechef_000002 CASCADE");
    }

    @Test
    void testUserLookupsSkipATenantWhoseMigrationsHaveNotCreatedTheUserTable() {
        assertThat(tenantRepository.findTenantIdsByUserEmail("ADMIN@example.com")).containsExactly("000001");
        assertThat(tenantRepository.findTenantIdsByUserLogin("admin")).containsExactly("000001");
        assertThat(tenantRepository.findTenantIdByUserActivationKey("activation")).isEqualTo("000001");
        assertThat(tenantRepository.findTenantIdByUserResetKey("reset")).isEqualTo("000001");
    }

    @Test
    void testOrganizationAndCompanyLookupsSkipATenantWhoseMigrationsHaveNotCreatedTheirTables() {
        assertThat(tenantRepository.findTenantIdByOrganizationName("acme")).isEqualTo("000001");
        assertThat(tenantRepository.findTenants())
            .extracting(Tenant::getTenantId, Tenant::getCompanyName)
            .containsExactly(tuple("000001", "Acme"));
    }

    @Test
    void testFindTenantIdsStillListsATenantWhoseMigrationsAreRunning() {
        assertThat(tenantRepository.findTenantIds()).containsExactlyInAnyOrder("000001", "000002");
    }

    private void execute(String... sqls) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : sqls) {
                statement.execute(sql);
            }
        }
    }
}
