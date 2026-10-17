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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TenantRepositoryTest {

    private final Connection connection = mock(Connection.class);
    private TenantRepository tenantRepository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = mock(DataSource.class);

        tenantRepository = new TenantRepository(dataSource);
    }

    @Test
    void testCreateTenantWithValidId() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant123"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testCreateTenantWithAlphanumericId() {
        assertThatThrownBy(() -> tenantRepository.createTenant("Tenant123ABC"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testCreateTenantWithUnderscoreId() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant_123"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testCreateTenantWithHyphenId() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant-123"))
            .isInstanceOf(RuntimeException.class);
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
            .isInstanceOf(RuntimeException.class);
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
    void testFindTenantIdsByUserEmailSkipsASchemaWhoseChangelogHasNotCreatedTheUserTable() throws SQLException {
        mockTenantSchemas(true);
        mockUserQuery("bytechef_000001", "000001");
        mockFailingUserQuery("bytechef_000002", new SQLException("relation does not exist", "42P01"));
        mockUserQuery("bytechef_000003", "000003");

        List<String> tenantIds = createTenantRepository().findTenantIdsByUserEmail("user@localhost.com");

        assertThat(tenantIds).containsExactly("000001", "000003");
        verify(connection, never()).setSavepoint();
    }

    @Test
    void testFindTenantIdsByUserLoginSkipsASchemaDroppedDuringTheLookup() throws SQLException {
        mockTenantSchemas(true);
        mockUserQuery("bytechef_000001", "000001");
        mockFailingUserQuery("bytechef_000002", new SQLException("schema does not exist", "3F000"));
        mockUserQuery("bytechef_000003", "000003");

        List<String> tenantIds = createTenantRepository().findTenantIdsByUserLogin("user");

        assertThat(tenantIds).containsExactly("000001", "000003");
    }

    @Test
    void testFindTenantIdsByUserEmailRollsBackToASavepointInATransaction() throws SQLException {
        Savepoint savepoint = mock(Savepoint.class);

        when(connection.setSavepoint()).thenReturn(savepoint);

        mockTenantSchemas(false);
        mockUserQuery("bytechef_000001", "000001");
        mockFailingUserQuery("bytechef_000002", new SQLException("relation does not exist", "42P01"));
        mockUserQuery("bytechef_000003", "000003");

        List<String> tenantIds = createTenantRepository().findTenantIdsByUserEmail("user@localhost.com");

        assertThat(tenantIds).containsExactly("000001", "000003");
        verify(connection).rollback(savepoint);
    }

    @Test
    void testFindTenantIdsByUserEmailFailsOnAnyOtherError() throws SQLException {
        mockTenantSchemas(true);
        mockUserQuery("bytechef_000001", "000001");
        mockFailingUserQuery("bytechef_000002", new SQLException("connection lost", "08006"));
        mockUserQuery("bytechef_000003", "000003");

        TenantRepository repository = createTenantRepository();

        assertThatThrownBy(() -> repository.findTenantIdsByUserEmail("user@localhost.com"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("connection lost");
    }

    @Test
    void testDeleteTenantWithNull() {
        assertThatThrownBy(() -> tenantRepository.deleteTenant(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");
    }

    private TenantRepository createTenantRepository() throws SQLException {
        DataSource dataSource = mock(DataSource.class);

        when(dataSource.getConnection()).thenReturn(connection);

        return new TenantRepository(dataSource);
    }

    @SuppressFBWarnings({
        "ODR_OPEN_DATABASE_RESOURCE", "OBL_UNSATISFIED_OBLIGATION", "SQL_INJECTION_JDBC"
    })
    @SuppressWarnings("PMD.CheckResultSet")
    private void mockFailingUserQuery(String schema, SQLException sqlException) throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        when(connection.prepareStatement(contains(schema + ".user"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(sqlException);
    }

    @SuppressFBWarnings({
        "ODR_OPEN_DATABASE_RESOURCE", "OBL_UNSATISFIED_OBLIGATION", "SQL_INJECTION_JDBC"
    })
    @SuppressWarnings("PMD.CheckResultSet")
    private void mockTenantSchemas(boolean autoCommit) throws SQLException {
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.createStatement()).thenReturn(statement);
        when(connection.getAutoCommit()).thenReturn(autoCommit);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(resultSet.getString(1)).thenReturn("bytechef_000001", "bytechef_000002", "bytechef_000003");
    }

    @SuppressFBWarnings({
        "ODR_OPEN_DATABASE_RESOURCE", "OBL_UNSATISFIED_OBLIGATION", "SQL_INJECTION_JDBC"
    })
    @SuppressWarnings("PMD.CheckResultSet")
    private void mockUserQuery(String schema, String tenantId) throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(contains(schema + ".user"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString(1)).thenReturn(tenantId);
    }
}
